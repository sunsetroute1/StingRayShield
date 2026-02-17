package com.stingrayshield.data.api

import android.content.Context
import android.content.SharedPreferences
import com.stingrayshield.util.DebugLog
import com.stingrayshield.util.OpenCellIdPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.io.SequenceInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.GZIPInputStream
import javax.inject.Inject
import javax.inject.Singleton

private val Context.towerLocationCache: DataStore<Preferences> by preferencesDataStore(name = "tower_location_cache")

/**
 * Service for fetching real cell tower locations from public databases.
 *
 * OpenCellID is the single source for the API key: it is read from app Settings
 * ([OpenCellIdPreferences.KEY_OPENCELLID_API_KEY]) and used for:
 * - Single-cell API: cell/get
 * - Area API: cell/getInArea
 * - CSV downloads: ocid/downloads?token=... (US MCC files 310–314)
 * When the user changes the API key in Settings, all of these use the new key on the next request.
 *
 * Supports:
 * - OpenCellID (primary) - Free API with registration
 * - UnwiredLabs (fallback) - Free tier available
 * - Local caching to minimize API calls
 *
 * API Keys:
 * - OpenCellID: Register at https://opencellid.org/register (FREE)
 * - UnwiredLabs: Register at https://unwiredlabs.com/
 *
 * For privacy, this service caches all lookups locally so the same tower
 * is never looked up twice.
 */
@Singleton
class CellTowerLocationService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "CellTowerLocationService"
        
        // OpenCellID API
        private const val OPENCELLID_BASE_URL = "https://opencellid.org/"
        
        // OpenCellID CSV downloads (country/MCC files) - same token as API
        // Format: https://opencellid.org/ocid/downloads?token=TOKEN&type=mcc&file=310.csv.gz
        private const val OPENCELLID_CSV_DOWNLOAD_BASE = "https://opencellid.org/ocid/downloads"
        /** US MCCs per OpenCelliD: 310, 311, 312, 313, 314 */
        private val US_MCC_FILES = listOf("310", "311", "312", "313", "314")
        
        /** CSV cache dir; files named e.g. 310.csv.gz */
        private const val CSV_CACHE_DIR = "opencellid_csv"
        /** Default: re-download CSV after this many days (overridden by settings). */
        private const val DEFAULT_CSV_REFRESH_DAYS = 30
        /** Max towers to return from CSV per region (lower = faster load). */
        private const val CSV_MAX_TOWERS_PER_REGION = 2000
        /** OpenCellID allows 2 CSV downloads per user per day; we enforce that. */
        private const val OPENCELLID_CSV_DOWNLOADS_PER_DAY = 2
        private const val PREF_CSV_DOWNLOAD_DATE = "opencellid_csv_download_date"
        private const val PREF_CSV_DOWNLOAD_COUNT = "opencellid_csv_download_count"
        /** Assets path for bundled US MCC CSVs (310, 311, 312, 313, 314). */
        private const val ASSETS_US_CSV_DIR = "opencellid_us"
        /** Binary tower cache: faster than CSV. File name e.g. 310.towers.bin */
        private const val TOWERS_BIN_SUFFIX = ".towers.bin"
        /** Magic for our binary format (4 bytes). */
        private val BINARY_MAGIC = "STWR".toByteArray(Charsets.US_ASCII)
        private const val BINARY_RECORD_SIZE = 32
        private const val LAT_LON_SCALE = 1_000_000.0
        
        // UnwiredLabs API (fallback)
        private const val UNWIREDLABS_BASE_URL = "https://us1.unwiredlabs.com/"
        private const val UNWIREDLABS_TOKEN = "your_token_here"
        
        // Cache key prefix
        private const val CACHE_PREFIX = "tower_"
    }
    
    private val preferences: SharedPreferences = context.getSharedPreferences(
        OpenCellIdPreferences.PREFERENCES_NAME, Context.MODE_PRIVATE
    )
    
    /**
     * Get the OpenCellID API key from Settings. Used for all OpenCellID calls:
     * single-cell API (cell/get), area API (cell/getInArea), and CSV downloads (ocid/downloads).
     * When the user changes the key in Settings, the next call uses the new key.
     */
    private fun getOpenCellIdApiKey(): String {
        return preferences.getString(OpenCellIdPreferences.KEY_OPENCELLID_API_KEY, "") ?: ""
    }
    
    private val gson = Gson()
    
    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor { message ->
            DebugLog.d(TAG, "HTTP: $message")
        }.apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    
    /** Longer timeouts for CSV .gz downloads (files can be 9MB+). */
    private val csvDownloadClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }
    
    private val openCellIdApi: CellTowerLocationApi by lazy {
        Retrofit.Builder()
            .baseUrl(OPENCELLID_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CellTowerLocationApi::class.java)
    }
    
    private val unwiredLabsApi: CellTowerLocationApi by lazy {
        Retrofit.Builder()
            .baseUrl(UNWIREDLABS_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CellTowerLocationApi::class.java)
    }
    
    /**
     * Get the location of a cell tower.
     * 
     * @param mcc Mobile Country Code
     * @param mnc Mobile Network Code
     * @param lac Location Area Code
     * @param cellId Cell ID
     * @return CellTowerLocation if found, null otherwise
     */
    suspend fun getTowerLocation(
        mcc: Int,
        mnc: Int,
        lac: Int,
        cellId: Int
    ): CellTowerLocation? {
        val cacheKey = "$mcc-$mnc-$lac-$cellId"
        
        // Check cache first
        val cached = getCachedLocation(cacheKey)
        if (cached != null) {
            DebugLog.d(TAG, "Cache hit for tower $cacheKey")
            return cached.copy(source = "cached")
        }
        
        // Try OpenCellID first
        try {
            val location = fetchFromOpenCellId(mcc, mnc, lac, cellId)
            if (location != null) {
                cacheLocation(cacheKey, location)
                DebugLog.d(TAG, "OpenCellID: Found tower $cacheKey at ${location.latitude}, ${location.longitude}")
                return location
            }
        } catch (e: Exception) {
            DebugLog.w(TAG, "OpenCellID lookup failed: ${e.message}")
        }
        
        // Fallback to UnwiredLabs
        try {
            val location = fetchFromUnwiredLabs(mcc, mnc, lac, cellId)
            if (location != null) {
                cacheLocation(cacheKey, location)
                DebugLog.d(TAG, "UnwiredLabs: Found tower $cacheKey at ${location.latitude}, ${location.longitude}")
                return location
            }
        } catch (e: Exception) {
            DebugLog.w(TAG, "UnwiredLabs lookup failed: ${e.message}")
        }
        
        DebugLog.d(TAG, "No location found for tower $cacheKey")
        return null
    }
    
    /**
     * Batch lookup for multiple towers
     */
    suspend fun getTowerLocations(
        towers: List<TowerIdentifier>
    ): Map<TowerIdentifier, CellTowerLocation> {
        val results = mutableMapOf<TowerIdentifier, CellTowerLocation>()
        
        for (tower in towers) {
            val location = getTowerLocation(
                mcc = tower.mcc,
                mnc = tower.mnc,
                lac = tower.lac,
                cellId = tower.cellId
            )
            if (location != null) {
                results[tower] = location
            }
        }
        
        return results
    }
    
    private suspend fun fetchFromOpenCellId(
        mcc: Int,
        mnc: Int,
        lac: Int,
        cellId: Int
    ): CellTowerLocation? {
        val apiKey = getOpenCellIdApiKey()
        
        // Skip if no API key configured
        if (apiKey.isBlank()) {
            DebugLog.d(TAG, "OpenCellID API key not configured - set it in Settings")
            return null
        }
        
        DebugLog.d(TAG, "Fetching location from OpenCellID for MCC=$mcc MNC=$mnc LAC=$lac CID=$cellId with key=${apiKey.take(8)}...")
        
        try {
            val response = openCellIdApi.getOpenCellIdLocation(
                apiKey = apiKey,
                mcc = mcc,
                mnc = mnc,
                lac = lac,
                cellId = cellId
            )
            
            DebugLog.d(TAG, "OpenCellID HTTP response code: ${response.code()}")
            
            if (response.isSuccessful) {
                val body = response.body()
                DebugLog.d(TAG, "OpenCellID response body: $body")
                DebugLog.d(TAG, "OpenCellID response: status=${body?.status}, lat=${body?.lat}, lon=${body?.lon}, error=${body?.message}")
                
                // OpenCellID returns data directly when successful (no status field), 
                // or status="error" with message when failed
                if (body != null) {
                    if (body.lat != null && body.lon != null) {
                        // Success - we have coordinates
                        return CellTowerLocation.fromOpenCellId(body)
                    } else if (body.status == "ok" && body.lat != null && body.lon != null) {
                        // Alternative success format
                        return CellTowerLocation.fromOpenCellId(body)
                    } else {
                        DebugLog.w(TAG, "OpenCellID: No coordinates in response. Error: ${body.message}")
                    }
                }
            } else {
                val errorBody = response.errorBody()?.string()
                DebugLog.w(TAG, "OpenCellID HTTP error: ${response.code()}, body: $errorBody")
            }
        } catch (e: Exception) {
            DebugLog.e(TAG, "OpenCellID exception: ${e.javaClass.simpleName}: ${e.message}")
            e.printStackTrace()
        }
        
        return null
    }
    
    private suspend fun fetchFromUnwiredLabs(
        mcc: Int,
        mnc: Int,
        lac: Int,
        cellId: Int
    ): CellTowerLocation? {
        // Skip if no token configured
        if (UNWIREDLABS_TOKEN == "your_token_here") {
            DebugLog.d(TAG, "UnwiredLabs token not configured")
            return null
        }
        
        val response = unwiredLabsApi.getUnwiredLabsLocation(
            token = UNWIREDLABS_TOKEN,
            mcc = mcc,
            mnc = mnc,
            lac = lac,
            cellId = cellId
        )
        
        if (response.isSuccessful) {
            val body = response.body()
            if (body != null && body.status == "ok") {
                return CellTowerLocation.fromUnwiredLabs(body)
            }
        }
        
        return null
    }
    
    private suspend fun getCachedLocation(key: String): CellTowerLocation? {
        val prefKey = stringPreferencesKey("$CACHE_PREFIX$key")
        val json = context.towerLocationCache.data
            .map { prefs -> prefs[prefKey] }
            .first()
        
        return if (json != null) {
            try {
                gson.fromJson(json, CellTowerLocation::class.java)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }
    
    private suspend fun cacheLocation(key: String, location: CellTowerLocation) {
        val prefKey = stringPreferencesKey("$CACHE_PREFIX$key")
        val json = gson.toJson(location)
        
        context.towerLocationCache.edit { prefs ->
            prefs[prefKey] = json
        }
    }
    
    /**
     * Clear the location cache
     */
    suspend fun clearCache() {
        context.towerLocationCache.edit { it.clear() }
        DebugLog.d(TAG, "Tower location cache cleared")
    }
    
    /**
     * Fetch all cell towers in a geographic area from OpenCellID.
     * Tries API first; if that returns nothing, falls back to US CSV downloads (MCC 310–314).
     *
     * @param centerLat Center latitude of the area
     * @param centerLon Center longitude of the area
     * @param radiusKm Radius in kilometers (default 10km)
     * @return List of cell towers in the area, or empty list if failed
     */
    suspend fun getTowersInArea(
        centerLat: Double,
        centerLon: Double,
        radiusKm: Double = 10.0,
        onProgress: ((Int) -> Unit)? = null
    ): List<RegionCellTower> {
        onProgress?.invoke(0)
        val latOffset = radiusKm / 111.0
        val lonOffset = radiusKm / (111.0 * kotlin.math.cos(Math.toRadians(centerLat)))
        val latMin = centerLat - latOffset
        val latMax = centerLat + latOffset
        val lonMin = centerLon - lonOffset
        val lonMax = centerLon + lonOffset
        val bbox = "$latMin,$lonMin,$latMax,$lonMax"
        DebugLog.d(TAG, "Fetching towers in area: center=($centerLat, $centerLon), radius=${radiusKm}km, bbox=$bbox")
        
        onProgress?.invoke(15)
        val apiKey = getOpenCellIdApiKey()
        // 1) If we already have local data (binary, cached CSV, or bundled), use it and skip API/download to avoid rate limits
        if (hasLocalTowerData()) {
            DebugLog.d(TAG, "Using local tower data (binary/cache/bundled); skipping Area API")
            onProgress?.invoke(40)
            return getTowersInAreaFromCsv(latMin, latMax, lonMin, lonMax, onProgress)
        }
        // 2) Try OpenCellID area API only when we don't have local data (when API key is set)
        if (apiKey.isNotBlank()) {
            try {
                val response = openCellIdApi.getCellsInArea(
                    apiKey = apiKey,
                    bbox = bbox,
                    limit = 1000
                )
                DebugLog.d(TAG, "Area API response code: ${response.code()}")
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.status.equals("Error", ignoreCase = true) && body?.message?.contains("RATE_LIMITED", ignoreCase = true) == true) {
                        DebugLog.w(TAG, "Area API rate limited (2 requests/day); falling back to CSV with 2 downloads/day limit")
                    } else {
                        DebugLog.d(TAG, "Area API returned ${body?.count ?: 0} towers")
                    }
                    if (body?.cells != null && body.cells.isNotEmpty()) {
                        val towers = body.cells.mapNotNull { cell ->
                            if (cell.lat != null && cell.lon != null && cell.cellid != null) {
                                RegionCellTower(
                                    cellId = cell.cellid,
                                    lac = cell.lac ?: 0,
                                    mcc = cell.mcc ?: 0,
                                    mnc = cell.mnc ?: 0,
                                    latitude = cell.lat,
                                    longitude = cell.lon,
                                    range = cell.range,
                                    samples = cell.samples,
                                    radio = cell.radio ?: "UNKNOWN",
                                    averageSignalStrength = cell.averageSignalStrength
                                )
                            } else null
                        }
                        DebugLog.d(TAG, "Parsed ${towers.size} valid towers from API")
                        onProgress?.invoke(100)
                        return towers
                    }
                    DebugLog.w(TAG, "Area API returned no cells: ${body?.message}")
                } else {
                    val errorBody = response.errorBody()?.string()
                    DebugLog.w(TAG, "Area API HTTP error: ${response.code()}, body: $errorBody")
                }
            } catch (e: Exception) {
                DebugLog.e(TAG, "Area API exception: ${e.javaClass.simpleName}: ${e.message}")
            }
        } else {
            DebugLog.d(TAG, "No API key - using bundled/cached US CSV only")
        }
        
        onProgress?.invoke(40)
        // 3) US CSV: bundled assets first, then download only if needed (respects refresh interval + 2/day)
        return getTowersInAreaFromCsv(latMin, latMax, lonMin, lonMax, onProgress)
    }
    
    private fun getCsvCacheDir(): File {
        val dir = File(context.filesDir, CSV_CACHE_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }
    
    /** True if we have local tower data (binary or cached CSV) so we can skip Area API and CSV download. */
    private fun hasLocalTowerData(): Boolean {
        val cacheDir = getCsvCacheDir()
        val refreshMs = getCsvRefreshIntervalDays() * 24L * 60 * 60 * 1000
        for (mcc in US_MCC_FILES) {
            val bin = File(cacheDir, "$mcc$TOWERS_BIN_SUFFIX")
            if (bin.exists()) return true
            val csv = File(cacheDir, "$mcc.csv.gz")
            if (csv.exists() && (System.currentTimeMillis() - csv.lastModified()) < refreshMs) return true
        }
        for (mcc in US_MCC_FILES) {
            for (suffix in listOf(".bin", ".csv.gz", ".csv")) {
                try {
                    context.assets.open("$ASSETS_US_CSV_DIR/$mcc$suffix").close()
                    return true
                } catch (_: Exception) { }
            }
        }
        return false
    }
    
    /** Today's date string (YYYY-MM-DD) for rate-limit tracking. */
    private fun todayDateString(): String {
        val c = java.util.Calendar.getInstance()
        return "%04d-%02d-%02d".format(c.get(java.util.Calendar.YEAR), c.get(java.util.Calendar.MONTH) + 1, c.get(java.util.Calendar.DAY_OF_MONTH))
    }
    
    /** CSV refresh interval in days (from settings; default monthly). */
    private fun getCsvRefreshIntervalDays(): Int {
        val days = preferences.getInt(OpenCellIdPreferences.KEY_CSV_REFRESH_INTERVAL_DAYS, DEFAULT_CSV_REFRESH_DAYS)
        return days.coerceIn(1, 365)
    }
    
    /** True if we can perform another CSV download today (OpenCellID allows 2 per day). */
    private fun canDownloadCsvToday(): Boolean {
        val today = todayDateString()
        val savedDate = preferences.getString(PREF_CSV_DOWNLOAD_DATE, "") ?: ""
        val count = preferences.getInt(PREF_CSV_DOWNLOAD_COUNT, 0)
        if (savedDate != today) return true
        return count < OPENCELLID_CSV_DOWNLOADS_PER_DAY
    }
    
    /** Record one successful CSV download for today. */
    private fun recordCsvDownload() {
        val today = todayDateString()
        val savedDate = preferences.getString(PREF_CSV_DOWNLOAD_DATE, "") ?: ""
        val count = preferences.getInt(PREF_CSV_DOWNLOAD_COUNT, 0)
        val newCount = if (savedDate != today) 1 else (count + 1).coerceAtMost(OPENCELLID_CSV_DOWNLOADS_PER_DAY)
        preferences.edit()
            .putString(PREF_CSV_DOWNLOAD_DATE, today)
            .putInt(PREF_CSV_DOWNLOAD_COUNT, newCount)
            .apply()
        DebugLog.d(TAG, "CSV downloads today: $newCount/$OPENCELLID_CSV_DOWNLOADS_PER_DAY")
    }
    
    /**
     * Download OpenCelliD MCC CSV .gz if missing or stale.
     * Respects OpenCellID limit: 2 downloads per user per day.
     * URL: https://opencellid.org/ocid/downloads?token=TOKEN&type=mcc&file=310.csv.gz
     */
    /**
     * Download if: first run (no file) or file older than refresh interval (e.g. 30 days).
     * Skips download on RATE_LIMIT; respects 2 downloads/day.
     */
    private fun downloadCsvIfNeeded(mcc: String): Boolean {
        val apiKey = getOpenCellIdApiKey()
        if (apiKey.isBlank()) return false
        val file = File(getCsvCacheDir(), "$mcc.csv.gz")
        val refreshMs = getCsvRefreshIntervalDays() * 24L * 60 * 60 * 1000
        val fileAge = if (file.exists()) System.currentTimeMillis() - file.lastModified() else Long.MAX_VALUE
        if (file.exists() && fileAge < refreshMs) {
            DebugLog.d(TAG, "CSV cache hit for MCC $mcc (age ${fileAge / (24 * 60 * 60 * 1000)}d < ${getCsvRefreshIntervalDays()}d)")
            return true
        }
        if (!canDownloadCsvToday()) {
            DebugLog.w(TAG, "CSV download limit reached for today (2/day). Using cache only for MCC $mcc")
            return file.exists()
        }
        val url = "$OPENCELLID_CSV_DOWNLOAD_BASE?token=${java.net.URLEncoder.encode(apiKey, "UTF-8")}&type=mcc&file=$mcc.csv.gz"
        DebugLog.d(TAG, "Downloading CSV for MCC $mcc from OpenCelliD...")
        return try {
            val request = Request.Builder()
                .url(url)
                .addHeader("Accept-Encoding", "identity")
                .build()
            csvDownloadClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val body = response.body?.string()?.take(200)
                    DebugLog.w(TAG, "CSV download failed for $mcc: HTTP ${response.code}, body: $body")
                    return false
                }
                response.body?.byteStream()?.use { input ->
                    val peek = ByteArray(1024)
                    val n = input.read(peek)
                    if (n > 0) {
                        val head = String(peek, 0, n, Charsets.UTF_8)
                        when {
                            peek[0] == '<'.code.toByte() -> {
                                DebugLog.w(TAG, "CSV download for $mcc returned HTML. Sample: ${head.take(120)}")
                                return false
                            }
                            peek[0] == '{'.code.toByte() && head.contains("RATE_LIMITED", ignoreCase = true) -> {
                                DebugLog.w(TAG, "CSV download for $mcc: OpenCellID rate limit (2/day). Use cache or try tomorrow.")
                                return false
                            }
                        }
                    }
                    FileOutputStream(file).use { output ->
                        output.write(peek, 0, n)
                        input.copyTo(output)
                    }
                }
                recordCsvDownload()
                DebugLog.d(TAG, "CSV downloaded for MCC $mcc: ${file.length() / 1024}KB")
                true
            }
        } catch (e: Exception) {
            DebugLog.e(TAG, "CSV download exception for $mcc: ${e.message}")
            false
        }
    }
    
    /**
     * Open bundled US MCC CSV from assets. Tries .bin (gzip), .csv.gz, then .csv
     * to avoid merge conflict (merger can treat .csv.gz and .csv as same resource).
     */
    private fun openBundledCsvReader(mcc: String): BufferedReader? {
        val paths = listOf("$ASSETS_US_CSV_DIR/$mcc.bin", "$ASSETS_US_CSV_DIR/$mcc.csv.gz", "$ASSETS_US_CSV_DIR/$mcc.csv")
        for (path in paths) {
            try {
                val raw = context.assets.open(path)
                val magic = ByteArray(2)
                if (raw.read(magic) != 2) {
                    raw.close()
                    continue
                }
                val fullStream: InputStream = SequenceInputStream(ByteArrayInputStream(magic), raw)
                val readerStream = if (magic[0] == GZIP_MAGIC[0] && magic[1] == GZIP_MAGIC[1]) {
                    GZIPInputStream(fullStream)
                } else {
                    fullStream
                }
                DebugLog.d(TAG, "Using bundled CSV: $path")
                return BufferedReader(InputStreamReader(readerStream))
            } catch (e: Exception) {
                if (e is java.io.FileNotFoundException || e.message?.contains("NotFoundException") == true) continue
                DebugLog.e(TAG, "Bundled CSV $path: ${e.message}")
            }
        }
        return null
    }
    
    /**
     * Stream-parse OpenCelliD CSV from file (gzip or plain).
     *
     * Column layout per https://wiki.opencellid.org/wiki/Database_format "Columns present in database":
     * Index | Parameter     | Data type | Description
     * ------|---------------|-----------|----------------------------------------------------------
     *   0   | radio         | string    | Network type: GSM, UMTS, LTE or CDMA
     *   1   | mcc           | integer   | Mobile Country Code (e.g. 260 for Poland)
     *   2   | net           | integer   | Mobile Network Code (MNC); for CDMA = SID
     *   3   | area          | integer   | LAC (GSM/UMTS), TAC (LTE), or NID (CDMA)
     *   4   | cell          | integer   | Cell ID (CID) / UTRAN Cell ID / BID (CDMA)
     *   5   | unit          | integer   | PSC (UMTS), PCI (LTE), or empty (GSM/CDMA)
     *   6   | lon           | double    | Longitude -180.0 to 180.0  (column 7 — primary for map)
     *   7   | lat           | double    | Latitude -90.0 to 90.0     (column 8 — primary for map)
     *   8   | range         | integer   | Cell range estimate in meters
     *   9   | samples       | integer   | Number of measurements for this cell
     *  10   | changeable    | integer   | 1=calculated from measurements, 0=exact GPS
     *  11   | created       | integer   | Unix timestamp first seen
     *  12   | updated       | integer   | Unix timestamp last update
     *  13   | averageSignal | integer   | Average signal strength (dBm or TS 27.007 8.5)
     *
     * Filter by bbox and cap at CSV_MAX_TOWERS_PER_REGION.
     */
    /** Parse CSV lines from reader into result list (same column layout as parseCsvInBbox). */
    private fun parseCsvFromReader(
        reader: BufferedReader,
        latMin: Double,
        latMax: Double,
        lonMin: Double,
        lonMax: Double,
        limit: Int,
        result: MutableList<RegionCellTower>
    ) {
        var line = reader.readLine() ?: return
        if (isHeaderRow(line)) line = reader.readLine() ?: return
        parseRow(line, latMin, latMax, lonMin, lonMax, result, limit)
        while (result.size < limit) {
            val row = reader.readLine() ?: break
            if (isHeaderRow(row)) continue
            parseRow(row, latMin, latMax, lonMin, lonMax, result, limit)
        }
    }
    
    private fun parseCsvInBbox(
        file: File,
        latMin: Double,
        latMax: Double,
        lonMin: Double,
        lonMax: Double,
        limit: Int
    ): List<RegionCellTower> {
        if (!file.exists()) return emptyList()
        val result = mutableListOf<RegionCellTower>()
        try {
            val reader = openCsvReader(file)
                ?: run {
                    DebugLog.e(TAG, "Parse CSV: could not open ${file.name} (not gzip, not plain CSV)")
                    return emptyList()
                }
            reader.use { parseCsvFromReader(it, latMin, latMax, lonMin, lonMax, limit, result) }
        } catch (e: Exception) {
            DebugLog.e(TAG, "Parse CSV error ${file.name}: ${e.message}")
        }
        if (result.isEmpty()) {
            val firstLine = try {
                openCsvReader(file)?.use { it.readLine()?.take(150) } ?: "(could not open)"
            } catch (_: Exception) { "(read error)" }
            DebugLog.w(TAG, "Parse CSV ${file.name}: 0 towers in bbox. First line sample: $firstLine")
        }
        return result
    }

    /** True if line looks like OpenCelliD header (radio,mcc,net,...). MCC exports have no header. */
    private fun isHeaderRow(line: String): Boolean {
        val first = line.split(",").getOrNull(0)?.trim() ?: return false
        return first.equals("radio", ignoreCase = true) || first.equals("lat", ignoreCase = true)
    }

    /** Parse one CSV row using column indices from wiki (Parameter = column, Data type = parse as). */
    private fun parseRow(
        row: String,
        latMin: Double,
        latMax: Double,
        lonMin: Double,
        lonMax: Double,
        result: MutableList<RegionCellTower>,
        limit: Int
    ) {
        if (result.size >= limit) return
        val cols = row.split(",")
        if (cols.size < 9) return
        val lon = cols.getOrNull(6)?.toDoubleOrNull() ?: return   // lon (double)
        val lat = cols.getOrNull(7)?.toDoubleOrNull() ?: return   // lat (double)
        if (lat < latMin || lat > latMax || lon < lonMin || lon > lonMax) return
        val mcc = cols.getOrNull(1)?.toIntOrNull() ?: return      // mcc (integer)
        val net = cols.getOrNull(2)?.toIntOrNull() ?: 0           // net / MNC (integer)
        val area = cols.getOrNull(3)?.toIntOrNull() ?: 0          // area / LAC (integer)
        val cell = cols.getOrNull(4)?.toIntOrNull() ?: return      // cell (integer)
        val range = cols.getOrNull(8)?.toIntOrNull()              // range (integer)
        val samples = cols.getOrNull(9)?.toIntOrNull()            // samples (integer)
        val radio = cols.getOrNull(0)?.takeIf { it.isNotBlank() } ?: "UNKNOWN"  // radio (string)
        val averageSignal = cols.getOrNull(13)?.toIntOrNull()     // averageSignal (integer)
        result.add(
            RegionCellTower(
                cellId = cell,
                lac = area,
                mcc = mcc,
                mnc = net,
                latitude = lat,
                longitude = lon,
                range = range,
                samples = samples,
                radio = radio,
                averageSignalStrength = averageSignal
            )
        )
    }

    /** Parse one CSV row into RegionCellTower (no bbox filter). Used by CSV→binary converter. */
    private fun parseRowToTower(row: String): RegionCellTower? {
        val cols = row.split(",")
        if (cols.size < 9) return null
        val lon = cols.getOrNull(6)?.toDoubleOrNull() ?: return null
        val lat = cols.getOrNull(7)?.toDoubleOrNull() ?: return null
        val mcc = cols.getOrNull(1)?.toIntOrNull() ?: return null
        val net = cols.getOrNull(2)?.toIntOrNull() ?: 0
        val area = cols.getOrNull(3)?.toIntOrNull() ?: 0
        val cell = cols.getOrNull(4)?.toIntOrNull() ?: return null
        val range = cols.getOrNull(8)?.toIntOrNull()
        val samples = cols.getOrNull(9)?.toIntOrNull()
        val radio = cols.getOrNull(0)?.takeIf { it.isNotBlank() } ?: "UNKNOWN"
        val averageSignal = cols.getOrNull(13)?.toIntOrNull()
        return RegionCellTower(
            cellId = cell,
            lac = area,
            mcc = mcc,
            mnc = net,
            latitude = lat,
            longitude = lon,
            range = range,
            samples = samples,
            radio = radio,
            averageSignalStrength = averageSignal
        )
    }

    private fun radioToByte(radio: String): Int = when (radio.uppercase()) {
        "GSM" -> 0
        "UMTS", "WCDMA" -> 1
        "LTE" -> 2
        "CDMA" -> 3
        "NR", "5G" -> 4
        else -> 0
    }

    private fun byteToRadio(b: Int): String = when (b) {
        1 -> "UMTS"
        2 -> "LTE"
        3 -> "CDMA"
        4 -> "NR"
        else -> "GSM"
    }

    /**
     * Convert OpenCellID CSV (from reader) to binary format for faster loading.
     * Writes STWR magic + fixed 32-byte records per tower.
     */
    private fun convertCsvStreamToBinary(reader: BufferedReader, outputBinaryFile: File) {
        var line = reader.readLine() ?: return
        if (isHeaderRow(line)) line = reader.readLine() ?: return
        FileOutputStream(outputBinaryFile).use { out ->
            out.write(BINARY_MAGIC)
            val buf = ByteBuffer.allocate(BINARY_RECORD_SIZE).order(ByteOrder.LITTLE_ENDIAN)
            do {
                val tower = parseRowToTower(line) ?: continue
                buf.clear()
                buf.putInt((tower.latitude * LAT_LON_SCALE).toInt())
                buf.putInt((tower.longitude * LAT_LON_SCALE).toInt())
                buf.putInt(tower.cellId)
                buf.putInt(tower.lac)
                buf.putInt(tower.mcc)
                buf.putInt(tower.mnc)
                buf.putShort((tower.range ?: -1).toShort())
                buf.putShort((tower.samples ?: 0).toShort())
                buf.put(radioToByte(tower.radio).toByte())
                buf.put(0); buf.put(0); buf.put(0)
                out.write(buf.array())
            } while (reader.readLine()?.also { line = it } != null)
        }
    }

    /** Convert CSV file to binary (for downloaded cache). After success, deletes the source CSV to save space. */
    private fun convertCsvToBinary(csvFile: File, outputBinaryFile: File) {
        val reader = openCsvReader(csvFile) ?: return
        reader.use { convertCsvStreamToBinary(it, outputBinaryFile) }
        if (csvFile.delete()) {
            DebugLog.d(TAG, "Converted CSV to binary and removed source: ${csvFile.name}")
        } else {
            DebugLog.d(TAG, "Converted CSV to binary: ${outputBinaryFile.name} (source kept)")
        }
    }

    /**
     * Load towers from binary file (bbox filter, up to limit).
     * Much faster than CSV: fixed 32-byte records, no string parsing.
     */
    private fun loadTowersFromBinary(
        file: File,
        latMin: Double,
        latMax: Double,
        lonMin: Double,
        lonMax: Double,
        limit: Int
    ): List<RegionCellTower> {
        if (!file.exists() || file.length() < BINARY_MAGIC.size + BINARY_RECORD_SIZE) return emptyList()
        val result = mutableListOf<RegionCellTower>()
        FileInputStream(file).use { fis ->
            val magic = ByteArray(BINARY_MAGIC.size)
            if (fis.read(magic) != magic.size || !magic.contentEquals(BINARY_MAGIC)) return emptyList()
            val buf = ByteArray(BINARY_RECORD_SIZE)
            while (result.size < limit) {
                if (fis.read(buf) != BINARY_RECORD_SIZE) break
                val bb = ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN)
                val lat = bb.getInt() / LAT_LON_SCALE
                val lon = bb.getInt() / LAT_LON_SCALE
                if (lat < latMin || lat > latMax || lon < lonMin || lon > lonMax) continue
                val cellId = bb.getInt()
                val lac = bb.getInt()
                val mcc = bb.getInt()
                val mnc = bb.getInt()
                val rangeShort = bb.short
                val samplesShort = bb.short
                val radioByte = bb.get().toInt().and(0xFF)
                result.add(
                    RegionCellTower(
                        cellId = cellId,
                        lac = lac,
                        mcc = mcc,
                        mnc = mnc,
                        latitude = lat,
                        longitude = lon,
                        range = rangeShort.toInt().takeIf { it >= 0 },
                        samples = samplesShort.toInt().takeIf { it >= 0 },
                        radio = byteToRadio(radioByte),
                        averageSignalStrength = null
                    )
                )
            }
        }
        return result
    }

    /** GZIP magic: 1f 8b. */
    private val GZIP_MAGIC = byteArrayOf(0x1f.toByte(), 0x8b.toByte())
    /** ZIP magic: PK (50 4B). */
    private val ZIP_MAGIC = byteArrayOf(0x50.toByte(), 0x4b.toByte())

    /** Tar magic "ustar" at offset 257 in a 512-byte tar header. */
    private val TAR_USTAR = "ustar".toByteArray()

    /**
     * Open file as CSV. The file name IS the data identifier (e.g. 310.csv.gz = MCC 310).
     * Handles: ZIP (one entry = the CSV file), gzip-of-CSV, gzip-of-tar, or plain CSV.
     */
    private fun openCsvReader(file: File): BufferedReader? {
        FileInputStream(file).use { fis ->
            val magic = ByteArray(2)
            if (fis.read(magic) != 2) return null
            return when {
                magic[0] == ZIP_MAGIC[0] && magic[1] == ZIP_MAGIC[1] -> {
                    DebugLog.d(TAG, "CSV ${file.name}: file is ZIP (entries are the data files)")
                    openZipAsCsvReader(file)
                }
                magic[0] == GZIP_MAGIC[0] && magic[1] == GZIP_MAGIC[1] -> {
                    DebugLog.d(TAG, "CSV ${file.name}: parsing as gzip")
                    openGzipAsCsvReader(file)
                }
                magic[0] == '<'.code.toByte() -> {
                    DebugLog.w(TAG, "CSV ${file.name}: file looks like HTML (server error?) - skip")
                    null
                }
                else -> {
                    DebugLog.d(TAG, "CSV ${file.name}: parsing as plain text (no gzip magic)")
                    BufferedReader(InputStreamReader(FileInputStream(file)))
                }
            }
        }
    }

    /**
     * ZIP where each entry IS the data file (filename = data identifier, e.g. 310.csv).
     * Find entry matching our MCC from file name "310.csv.gz" -> "310.csv" or "310.csv.gz".
     */
    private fun openZipAsCsvReader(file: File): BufferedReader? {
        val mccFromName = file.name.removeSuffix(".csv.gz").removeSuffix(".zip").takeIf { it.all(Char::isDigit) } ?: return null
        return try {
            java.util.zip.ZipFile(file).use { zip ->
                val entry = zip.entries().toList().find { e ->
                    !e.isDirectory && (e.name == "$mccFromName.csv" || e.name == "$mccFromName.csv.gz" || e.name.endsWith(".csv"))
                } ?: zip.entries().toList().firstOrNull { !it.isDirectory } ?: return null
                DebugLog.d(TAG, "ZIP entry for MCC $mccFromName: ${entry.name}")
                val bytes = zip.getInputStream(entry).readBytes()
                val csvBytes = if (bytes.size >= 2 && bytes[0] == GZIP_MAGIC[0] && bytes[1] == GZIP_MAGIC[1]) {
                    GZIPInputStream(ByteArrayInputStream(bytes)).readBytes()
                } else {
                    bytes
                }
                BufferedReader(InputStreamReader(ByteArrayInputStream(csvBytes)))
            }
        } catch (e: Exception) {
            DebugLog.e(TAG, "openZipAsCsvReader ${file.name}: ${e.message}")
            null
        }
    }

    /**
     * Gzip content can be (1) raw CSV or (2) tar where the first entry is the CSV "file".
     * Read first 512 bytes; if tar magic "ustar" at 257, extract first entry and parse as CSV.
     */
    private fun openGzipAsCsvReader(file: File): BufferedReader? {
        return try {
            GZIPInputStream(FileInputStream(file)).use { gzip ->
                val header = ByteArray(512)
                val n = gzip.read(header)
                if (n < 512) {
                    val rest = gzip.readBytes()
                    BufferedReader(InputStreamReader(ByteArrayInputStream(header.copyOf(n).plus(rest))))
                } else {
                    val isTar = 262 <= header.size && (0..4).all { header[257 + it] == TAR_USTAR[it] }
                    if (isTar) {
                        val sizeStr = header.copyOfRange(124, 136).toString(Charsets.US_ASCII).trim().trimStart('0').ifEmpty { "0" }
                        val size = sizeStr.toLongOrNull(8) ?: 0L
                        DebugLog.d(TAG, "CSV ${file.name}: gzip contains tar, first entry size=$size")
                        val maxSize = 512 * 1024 * 1024
                        val content = ByteArray(size.toInt().coerceIn(0, maxSize))
                        var off = 0
                        while (off < content.size) {
                            val read = gzip.read(content, off, content.size - off)
                            if (read <= 0) break
                            off += read
                        }
                        BufferedReader(InputStreamReader(ByteArrayInputStream(content.copyOf(off))))
                    } else {
                        BufferedReader(InputStreamReader(SequenceInputStream(ByteArrayInputStream(header), gzip)))
                    }
                }
            }
        } catch (e: Exception) {
            DebugLog.e(TAG, "openGzipAsCsvReader ${file.name}: ${e.message}")
            null
        }
    }
    
    /**
     * Load towers in bbox from US OpenCelliD (310–314).
     * Prefers binary cache (.towers.bin) for speed; falls back to CSV (bundled or downloaded).
     * After loading from downloaded CSV, converts to binary in background for next time.
     */
    private suspend fun getTowersInAreaFromCsv(
        latMin: Double,
        latMax: Double,
        lonMin: Double,
        lonMax: Double,
        onProgress: ((Int) -> Unit)? = null
    ): List<RegionCellTower> = coroutineScope {
        onProgress?.invoke(50)
        val perFile = (CSV_MAX_TOWERS_PER_REGION / US_MCC_FILES.size).coerceAtLeast(400)
        val cacheDir = getCsvCacheDir()
        val completed = AtomicInteger(0)
        val batches = US_MCC_FILES.map { mcc ->
            async {
                var batch: List<RegionCellTower>? = null
                val binaryFile = File(cacheDir, "$mcc$TOWERS_BIN_SUFFIX")
                if (binaryFile.exists()) {
                    batch = loadTowersFromBinary(binaryFile, latMin, latMax, lonMin, lonMax, perFile)
                    if (batch.isNotEmpty()) DebugLog.d(TAG, "Loaded ${batch.size} towers from binary $mcc")
                }
                if (batch == null || batch.isEmpty()) {
                    var fromBundled = false
                    openBundledCsvReader(mcc)?.use { reader ->
                        val fromBundledList = mutableListOf<RegionCellTower>()
                        try {
                            parseCsvFromReader(reader, latMin, latMax, lonMin, lonMax, perFile, fromBundledList)
                            batch = fromBundledList
                            fromBundled = true
                        } catch (e: Exception) {
                            DebugLog.e(TAG, "Bundled CSV $mcc parse error: ${e.message}")
                        }
                    }
                    if (fromBundled && !File(cacheDir, "$mcc$TOWERS_BIN_SUFFIX").exists()) {
                        Thread {
                            try {
                                openBundledCsvReader(mcc)?.use { r ->
                                    convertCsvStreamToBinary(r, File(cacheDir, "$mcc$TOWERS_BIN_SUFFIX"))
                                }
                            } catch (e: Exception) { DebugLog.e(TAG, "Bundled→binary $mcc: ${e.message}") }
                        }.start()
                    }
                }
                if ((batch == null || batch.isEmpty())) {
                    val apiKey = getOpenCellIdApiKey()
                    if (apiKey.isNotBlank() && downloadCsvIfNeeded(mcc)) {
                        val csvFile = File(cacheDir, "$mcc.csv.gz")
                        batch = parseCsvInBbox(csvFile, latMin, latMax, lonMin, lonMax, perFile)
                        if (batch.isNotEmpty()) {
                            val outBin = File(cacheDir, "$mcc$TOWERS_BIN_SUFFIX")
                            Thread {
                                try { convertCsvToBinary(csvFile, outBin) } catch (e: Exception) {
                                    DebugLog.e(TAG, "CSV→binary convert $mcc: ${e.message}")
                                }
                            }.start()
                        }
                    }
                }
                val n = completed.incrementAndGet()
                onProgress?.invoke((50 + n * 10).coerceAtMost(90))
                batch ?: emptyList()
            }
        }
        val all = batches.awaitAll().flatten().take(CSV_MAX_TOWERS_PER_REGION)
        onProgress?.invoke(100)
        DebugLog.d(TAG, "Towers loaded (binary/CSV): ${all.size}")
        all
    }
}

/**
 * Cell tower data from region/area query
 */
data class RegionCellTower(
    val cellId: Int,
    val lac: Int,
    val mcc: Int,
    val mnc: Int,
    val latitude: Double,
    val longitude: Double,
    val range: Int?,
    val samples: Int?,
    val radio: String,
    val averageSignalStrength: Int?
)

/**
 * Identifier for a cell tower (used for batch lookups)
 */
data class TowerIdentifier(
    val mcc: Int,
    val mnc: Int,
    val lac: Int,
    val cellId: Int
) {
    override fun toString(): String = "$mcc-$mnc-$lac-$cellId"
}
