package com.stingrayshield.ui.screens.map

import android.annotation.SuppressLint
import android.content.Context
import android.telephony.CellInfo
import android.telephony.CellInfoCdma
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellInfoWcdma
import android.telephony.TelephonyManager
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.stingrayshield.data.api.CellTowerLocationService
import com.stingrayshield.data.api.RegionCellTower
import com.stingrayshield.data.api.TowerIdentifier
import com.stingrayshield.util.DebugLog
import com.stingrayshield.data.repository.CellTowerRepository
import com.stingrayshield.data.repository.StingrayDeviceRepository
import com.stingrayshield.detection.DetectionAlgorithms
import com.stingrayshield.domain.model.CellTower
import com.stingrayshield.domain.model.StingrayDevice
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cellTowerRepository: CellTowerRepository,
    private val stingrayDeviceRepository: StingrayDeviceRepository,
    private val cellTowerLocationService: CellTowerLocationService
) : ViewModel() {
    
    private val telephonyManager: TelephonyManager by lazy {
        context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    }
    
    // Cache default MCC/MNC from SIM
    private val defaultMcc: String by lazy {
        try {
            val networkOperator = telephonyManager.networkOperator
            if (networkOperator.length >= 3) networkOperator.substring(0, 3) else ""
        } catch (e: Exception) { "" }
    }
    
    private val defaultMnc: String by lazy {
        try {
            val networkOperator = telephonyManager.networkOperator
            if (networkOperator.length >= 5) networkOperator.substring(3, 5) 
            else if (networkOperator.length > 3) networkOperator.substring(3)
            else ""
        } catch (e: Exception) { "" }
    }
    
    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    // Track which regions we've already loaded to avoid duplicate fetches
    private val loadedRegions = mutableSetOf<String>()
    
    init {
        DebugLog.d("MapViewModel", "=== MapViewModel initializing ===")
        loadStingrays()
        updateCurrentServingCells()
        // Load DB towers first so map shows something even when OpenCellID is rate limited
        loadHistoricalTowers()
        getLocationAndLoadRegion()
    }
    
    /**
     * Refresh which cell IDs the device is currently connected to (for map highlight).
     */
    @SuppressLint("MissingPermission")
    fun updateCurrentServingCells() {
        try {
            val cellInfoList = telephonyManager.allCellInfo ?: emptyList()
            val servingIds = cellInfoList
                .filter { it.isRegistered }
                .mapNotNull { DetectionAlgorithms.getCellIdFromCellInfo(it) }
                .toSet()
            _uiState.value = _uiState.value.copy(currentServingCellIds = servingIds)
            if (servingIds.isNotEmpty()) {
                DebugLog.d("MapViewModel", "Current serving cell IDs: $servingIds")
            }
        } catch (_: SecurityException) { }
        catch (e: Exception) {
            DebugLog.e("MapViewModel", "updateCurrentServingCells: ${e.message}")
        }
    }
    
    private fun loadStingrays() {
        viewModelScope.launch {
            try {
                val devices = stingrayDeviceRepository.getAllDevices().first()
                _uiState.value = _uiState.value.copy(stingrayDevices = devices)
            } catch (e: Exception) {
                DebugLog.e("MapViewModel", "Error loading stingrays: ${e.message}")
            }
        }
    }
    
    /**
     * Get user's location and load towers for that region from OpenCellID
     */
    @SuppressLint("MissingPermission")
    private fun getLocationAndLoadRegion() {
        DebugLog.d("MapViewModel", "=== getLocationAndLoadRegion() ===")
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, loadingProgress = 0)
                
                val location = fusedLocationClient.lastLocation.await()
                if (location != null) {
                    DebugLog.d("MapViewModel", "Got location: ${location.latitude}, ${location.longitude}")
                    _uiState.value = _uiState.value.copy(
                        userLatitude = location.latitude,
                        userLongitude = location.longitude
                    )
                    loadTowersForRegion(location.latitude, location.longitude)
                } else {
                    DebugLog.w("MapViewModel", "Location is null - loading towers for default map center")
                    // Still load towers for default US center so user sees something
                    val defaultLat = 39.8283
                    val defaultLon = -98.5795
                    _uiState.value = _uiState.value.copy(
                        userLatitude = defaultLat,
                        userLongitude = defaultLon,
                        error = null
                    )
                    loadTowersForRegion(defaultLat, defaultLon)
                }
            } catch (e: Exception) {
                DebugLog.e("MapViewModel", "Error getting location: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    loadingProgress = 0,
                    error = "Error: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Load cell towers for a geographic region from OpenCellID API.
     * Caches loaded regions to avoid duplicate API calls.
     * Region key uses ~2km grid so panning the map triggers new loads.
     */
    fun loadTowersForRegion(latitude: Double, longitude: Double, radiusKm: Double = 15.0) {
        val regionKey = "${(latitude * 50).toInt()}_${(longitude * 50).toInt()}"
        if (loadedRegions.contains(regionKey)) {
            DebugLog.d("MapViewModel", "Region $regionKey already loaded, skipping")
            return
        }
        
        DebugLog.d("MapViewModel", "=== Loading towers for region $regionKey (${latitude}, ${longitude}) ===")
        
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, loadingProgress = 0)
                val onProgress: (Int) -> Unit = { p ->
                    _uiState.value = _uiState.value.copy(loadingProgress = p.coerceIn(0, 100))
                }
                // Fetch towers from OpenCellID (API first, then US CSV fallback)
                val regionTowers = withContext(Dispatchers.IO) {
                    cellTowerLocationService.getTowersInArea(
                        centerLat = latitude,
                        centerLon = longitude,
                        radiusKm = radiusKm,
                        onProgress = onProgress
                    )
                }
                
                DebugLog.d("MapViewModel", "OpenCellID returned ${regionTowers.size} towers for region")
                
                if (regionTowers.isNotEmpty()) {
                    // Convert to CellTower objects
                    val cellTowers = regionTowers.map { rt ->
                        CellTower(
                            cellId = rt.cellId,
                            locationAreaCode = rt.lac,
                            mobileCountryCode = rt.mcc.toString(),
                            mobileNetworkCode = rt.mnc.toString(),
                            networkType = rt.radio,
                            signalStrength = rt.averageSignalStrength ?: -85,
                            latitude = rt.latitude,   // CSV column 8 (lat) — map position
                            longitude = rt.longitude, // CSV column 7 (lon) — map position
                            towerLatitude = rt.latitude,
                            towerLongitude = rt.longitude,
                            towerRange = rt.range,
                            locationSource = "opencellid",
                            isPrimary = false,
                            isServingCell = false,
                            observationCount = rt.samples ?: 1  // CSV samples for tap details
                        )
                    }
                    
                    // Merge with existing towers
                    val existingTowers = _uiState.value.cellTowers.associateBy { it.cellId }.toMutableMap()
                    cellTowers.forEach { tower -> existingTowers[tower.cellId] = tower }
                    
                    _uiState.value = _uiState.value.copy(
                        cellTowers = existingTowers.values.toList(),
                        isLoading = false,
                        loadingProgress = 0,
                        error = null
                    )
                    updateCurrentServingCells()
                    loadedRegions.add(regionKey)
                    DebugLog.d("MapViewModel", "✓ Loaded ${cellTowers.size} towers, total now: ${existingTowers.size}")
                } else {
                    DebugLog.w("MapViewModel", "No towers returned for region (API + US CSV fallback)")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        loadingProgress = 0,
                        error = if (_uiState.value.cellTowers.isEmpty())
                            "No towers here. OpenCellID allows 2 CSV downloads/day—use cached data or try tomorrow. Check API key in Settings."
                        else null
                    )
                }
            } catch (e: Exception) {
                DebugLog.e("MapViewModel", "Error loading region towers: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    loadingProgress = 0,
                    error = "Error: ${e.message}"
                )
            }
        }
    }
    
    private fun loadHistoricalTowers() {
        com.stingrayshield.util.DebugLog.d("MapViewModel", "loadHistoricalTowers() called")
        viewModelScope.launch {
            try {
                com.stingrayshield.util.DebugLog.d("MapViewModel", "Starting to collect towers from database...")
                // Subscribe to unique cell towers (one per cell ID, most recent observation)
                cellTowerRepository.getUniqueCellTowers().collect { towers ->
                    com.stingrayshield.util.DebugLog.d("MapViewModel", "=== DB Flow emitted: ${towers.size} unique towers ===")
                    
                    if (towers.isNotEmpty()) {
                        towers.take(3).forEach { t ->
                            com.stingrayshield.util.DebugLog.d("MapViewModel", "  Tower: id=${t.cellId}, lat=${t.latitude}, lng=${t.longitude}, real=${t.towerLatitude},${t.towerLongitude}")
                        }
                        
                        // Show towers immediately - MERGE with existing scanned towers
                        val existingTowers = _uiState.value.cellTowers.associateBy { it.cellId }.toMutableMap()
                        towers.forEach { tower -> 
                            // Only replace if the DB tower has location data and existing doesn't, 
                            // or if DB tower is newer
                            val existing = existingTowers[tower.cellId]
                            if (existing == null || tower.hasRealLocation() || tower.timestamp > existing.timestamp) {
                                existingTowers[tower.cellId] = tower
                            }
                        }
                        
                        _uiState.value = _uiState.value.copy(cellTowers = existingTowers.values.toList())
                        updateCurrentServingCells()
                        com.stingrayshield.util.DebugLog.d("MapViewModel", "UI state updated with ${existingTowers.size} towers total")
                        // Fetch real locations from OpenCellID in background
                        fetchRealTowerLocationsAsync(existingTowers.values.toList())
                    } else {
                        com.stingrayshield.util.DebugLog.d("MapViewModel", "No towers in database yet")
                    }
                }
            } catch (e: Exception) {
                com.stingrayshield.util.DebugLog.e("MapViewModel", "Error loading historical towers: ${e.message}")
            }
        }
    }
    
    /**
     * Fetch real tower locations from public database (OpenCellID) for towers
     * that don't already have location data. Runs in background without blocking UI.
     */
    private fun fetchRealTowerLocationsAsync(towers: List<CellTower>) {
        viewModelScope.launch {
            try {
                val towersNeedingLocation = towers.filter { !it.hasRealLocation() }
                
                if (towersNeedingLocation.isEmpty()) {
                    com.stingrayshield.util.DebugLog.d("MapViewModel", "All ${towers.size} towers already have real locations")
                    return@launch
                }
                
                com.stingrayshield.util.DebugLog.d("MapViewModel", "=== Fetching locations for ${towersNeedingLocation.size} of ${towers.size} towers ===")
                
                val updatedTowers = towers.toMutableList()
                var locationsFound = 0
                
                for (tower in towersNeedingLocation) {
                    try {
                        // Use tower's MCC/MNC, fallback to device's network operator
                        val mccStr = tower.mobileCountryCode.ifBlank { defaultMcc }
                        val mncStr = tower.mobileNetworkCode.ifBlank { defaultMnc }
                        val mcc = mccStr.toIntOrNull()
                        val mnc = mncStr.toIntOrNull()
                        
                        if (mcc == null || mnc == null || mcc == 0) {
                            com.stingrayshield.util.DebugLog.w("MapViewModel", "Tower ${tower.cellId}: Invalid MCC/MNC (${mccStr}/${mncStr}) - skipping API lookup")
                            continue
                        }
                        
                        com.stingrayshield.util.DebugLog.d("MapViewModel", "Fetching location for tower ${tower.cellId} (MCC=$mcc MNC=$mnc LAC=${tower.locationAreaCode})")
                        
                        val location = cellTowerLocationService.getTowerLocation(
                            mcc = mcc,
                            mnc = mnc,
                            lac = tower.locationAreaCode,
                            cellId = tower.cellId
                        )
                        
                        if (location != null) {
                            locationsFound++
                            val updatedTower = tower.copy(
                                towerLatitude = location.latitude,
                                towerLongitude = location.longitude,
                                towerRange = location.range,
                                locationSource = location.source
                            )
                            
                            // Update in our local list
                            val index = updatedTowers.indexOfFirst { it.cellId == tower.cellId }
                            if (index >= 0) {
                                updatedTowers[index] = updatedTower
                            }
                            
                            // Save to database
                            try {
                                cellTowerRepository.updateCellTower(updatedTower)
                            } catch (e: Exception) {
                                com.stingrayshield.util.DebugLog.w("MapViewModel", "Failed to save tower to DB: ${e.message}")
                            }
                            
                            com.stingrayshield.util.DebugLog.d("MapViewModel", 
                                "✓ Tower ${tower.cellId}: ${location.latitude}, ${location.longitude} (${location.source})")
                        } else {
                            com.stingrayshield.util.DebugLog.d("MapViewModel", "✗ Tower ${tower.cellId}: No location found")
                        }
                    } catch (e: Exception) {
                        com.stingrayshield.util.DebugLog.w("MapViewModel", "Failed to get location for tower ${tower.cellId}: ${e.message}")
                    }
                }
                
                com.stingrayshield.util.DebugLog.d("MapViewModel", "=== Location fetch complete: $locationsFound of ${towersNeedingLocation.size} found ===")
                
                // Update UI state directly with all updated towers
                if (locationsFound > 0) {
                    _uiState.value = _uiState.value.copy(cellTowers = updatedTowers)
                    com.stingrayshield.util.DebugLog.d("MapViewModel", "UI updated with $locationsFound new tower locations")
                }
            } catch (e: Exception) {
                com.stingrayshield.util.DebugLog.e("MapViewModel", "Background location fetch error: ${e.message}")
            }
        }
    }
    
    /**
     * Force refresh tower locations - clears cache and re-fetches from API
     */
    fun forceRefreshTowerLocations() {
        com.stingrayshield.util.DebugLog.d("MapViewModel", "=== Force refreshing tower locations ===")
        viewModelScope.launch {
            try {
                // Clear location cache
                cellTowerLocationService.clearCache()
                
                // Get current towers and re-fetch locations
                val towers = _uiState.value.cellTowers
                if (towers.isNotEmpty()) {
                    // Reset tower locations to force re-fetch
                    val resetTowers = towers.map { it.copy(towerLatitude = null, towerLongitude = null, locationSource = null) }
                    _uiState.value = _uiState.value.copy(cellTowers = resetTowers)
                    
                    // Fetch locations again
                    fetchRealTowerLocationsAsync(resetTowers)
                }
            } catch (e: Exception) {
                com.stingrayshield.util.DebugLog.e("MapViewModel", "Force refresh error: ${e.message}")
            }
        }
    }
    
    @SuppressLint("MissingPermission")
    private fun getLocationAndScan() {
        com.stingrayshield.util.DebugLog.d("MapViewModel", "getLocationAndScan() called")
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, loadingProgress = 0)
                
                // Get current location
                com.stingrayshield.util.DebugLog.d("MapViewModel", "Getting last known location...")
                val location = fusedLocationClient.lastLocation.await()
                if (location != null) {
                    com.stingrayshield.util.DebugLog.d("MapViewModel", "Got location: ${location.latitude}, ${location.longitude}")
                    _uiState.value = _uiState.value.copy(
                        userLatitude = location.latitude,
                        userLongitude = location.longitude
                    )
                    // Scan for towers at this location
                    scanCellTowers(location.latitude, location.longitude)
                } else {
                    com.stingrayshield.util.DebugLog.w("MapViewModel", "Location is null!")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        loadingProgress = 0,
                        error = "Could not get GPS location"
                    )
                }
            } catch (e: SecurityException) {
                com.stingrayshield.util.DebugLog.e("MapViewModel", "Security exception: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    loadingProgress = 0,
                    error = "Location permission required"
                )
            } catch (e: Exception) {
                com.stingrayshield.util.DebugLog.e("MapViewModel", "Exception: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    loadingProgress = 0,
                    error = "Error: ${e.message}"
                )
            }
        }
    }
    
    @SuppressLint("MissingPermission")
    private fun scanCellTowers(latitude: Double, longitude: Double) {
        com.stingrayshield.util.DebugLog.d("MapViewModel", "=== scanCellTowers() at $latitude, $longitude ===")
        try {
            val cellInfoList = telephonyManager.allCellInfo ?: emptyList()
            com.stingrayshield.util.DebugLog.d("MapViewModel", "TelephonyManager returned ${cellInfoList.size} cells")
            
            val towers = mutableListOf<CellTower>()
            val currentTime = System.currentTimeMillis()
            
            cellInfoList.forEachIndexed { index, cellInfo ->
                val tower = createCellTower(cellInfo, latitude, longitude, index, currentTime)
                if (tower != null) {
                    towers.add(tower)
                    com.stingrayshield.util.DebugLog.d("MapViewModel", "Tower: ${tower.networkType} ID=${tower.cellId} at ${tower.latitude},${tower.longitude}")
                }
            }
            
            // Add scanned towers to UI immediately
            if (towers.isNotEmpty()) {
                // Merge with existing towers (replace by cellId)
                val existingTowers = _uiState.value.cellTowers.associateBy { it.cellId }.toMutableMap()
                towers.forEach { tower -> existingTowers[tower.cellId] = tower }
                
                _uiState.value = _uiState.value.copy(
                    cellTowers = existingTowers.values.toList(),
                    isLoading = false,
                    loadingProgress = 0,
                    lastScanTime = currentTime,
                    error = null
                )
                com.stingrayshield.util.DebugLog.d("MapViewModel", "Updated UI with ${existingTowers.size} towers")
                
                // Also save to database in background
                viewModelScope.launch {
                    try {
                        cellTowerRepository.addCellTowers(towers)
                        com.stingrayshield.util.DebugLog.d("MapViewModel", "Saved ${towers.size} towers to database")
                    } catch (e: Exception) {
                        com.stingrayshield.util.DebugLog.e("MapViewModel", "Error saving towers: ${e.message}")
                    }
                }
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    loadingProgress = 0,
                    lastScanTime = currentTime,
                    error = null
                )
            }
            
            com.stingrayshield.util.DebugLog.d("MapViewModel", "Scan complete: ${towers.size} towers found")
            
        } catch (e: SecurityException) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                loadingProgress = 0,
                error = "Phone permission required"
            )
        } catch (e: Exception) {
            com.stingrayshield.util.DebugLog.e("MapViewModel", "Scan error: ${e.message}")
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                loadingProgress = 0,
                error = "Scan failed: ${e.message}"
            )
        }
    }
    
    /**
     * Add a small offset to prevent multiple towers at exact same location from overlapping.
     * Uses cell ID to create consistent offset per tower.
     */
    private fun addMarkerOffset(lat: Double, lng: Double, cellId: Int): Pair<Double, Double> {
        // Use cell ID to create a consistent angle for this tower
        val angle = (cellId % 360) * (Math.PI / 180.0)
        // Small offset ~20-30 meters to spread markers visually
        val offsetMeters = 20.0 + (cellId % 20)
        val latOffset = (offsetMeters * kotlin.math.cos(angle)) / 111320.0
        val lngOffset = (offsetMeters * kotlin.math.sin(angle)) / (111320.0 * kotlin.math.cos(lat * Math.PI / 180.0))
        return Pair(lat + latOffset, lng + lngOffset)
    }
    
    private fun createCellTower(
        cellInfo: CellInfo,
        latitude: Double,
        longitude: Double,
        index: Int,
        timestamp: Long
    ): CellTower? {
        return when (cellInfo) {
            is CellInfoLte -> {
                val id = cellInfo.cellIdentity
                val sig = cellInfo.cellSignalStrength
                val cellId = id.ci.takeIf { it != Int.MAX_VALUE && it > 0 } ?: return null
                // Store user's observation location with small offset to prevent overlap
                val (obsLat, obsLon) = addMarkerOffset(latitude, longitude, cellId)
                CellTower(
                    cellId = cellId,
                    locationAreaCode = id.tac.takeIf { it != Int.MAX_VALUE } ?: 0,
                    mobileCountryCode = id.mccString ?: "",
                    mobileNetworkCode = id.mncString ?: "",
                    networkType = "LTE",
                    signalStrength = sig.dbm,
                    latitude = obsLat,
                    longitude = obsLon,
                    timestamp = timestamp,
                    isPrimary = cellInfo.isRegistered,
                    isServingCell = cellInfo.isRegistered,
                    pci = id.pci.takeIf { it != Int.MAX_VALUE } ?: 0
                )
            }
            is CellInfoWcdma -> {
                val id = cellInfo.cellIdentity
                val sig = cellInfo.cellSignalStrength
                val cellId = id.cid.takeIf { it != Int.MAX_VALUE && it > 0 } ?: return null
                val (obsLat, obsLon) = addMarkerOffset(latitude, longitude, cellId)
                CellTower(
                    cellId = cellId,
                    locationAreaCode = id.lac.takeIf { it != Int.MAX_VALUE } ?: 0,
                    mobileCountryCode = id.mccString ?: "",
                    mobileNetworkCode = id.mncString ?: "",
                    networkType = "3G",
                    signalStrength = sig.dbm,
                    latitude = obsLat,
                    longitude = obsLon,
                    timestamp = timestamp,
                    isPrimary = cellInfo.isRegistered,
                    isServingCell = cellInfo.isRegistered,
                    psc = id.psc.takeIf { it != Int.MAX_VALUE } ?: 0
                )
            }
            is CellInfoGsm -> {
                val id = cellInfo.cellIdentity
                val sig = cellInfo.cellSignalStrength
                val cellId = id.cid.takeIf { it != Int.MAX_VALUE && it > 0 } ?: return null
                val (obsLat, obsLon) = addMarkerOffset(latitude, longitude, cellId)
                CellTower(
                    cellId = cellId,
                    locationAreaCode = id.lac.takeIf { it != Int.MAX_VALUE } ?: 0,
                    mobileCountryCode = id.mccString ?: "",
                    mobileNetworkCode = id.mncString ?: "",
                    networkType = "2G",
                    signalStrength = sig.dbm,
                    latitude = obsLat,
                    longitude = obsLon,
                    timestamp = timestamp,
                    isPrimary = cellInfo.isRegistered,
                    isServingCell = cellInfo.isRegistered
                )
            }
            is CellInfoCdma -> {
                val id = cellInfo.cellIdentity
                val sig = cellInfo.cellSignalStrength
                val (obsLat, obsLon) = addMarkerOffset(latitude, longitude, id.basestationId)
                CellTower(
                    cellId = id.basestationId,
                    locationAreaCode = id.networkId,
                    mobileCountryCode = "310",
                    mobileNetworkCode = id.systemId.toString(),
                    networkType = "CDMA",
                    signalStrength = sig.dbm,
                    latitude = obsLat,
                    longitude = obsLon,
                    timestamp = timestamp,
                    isPrimary = cellInfo.isRegistered,
                    isServingCell = cellInfo.isRegistered
                )
            }
            else -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && cellInfo is CellInfoNr) {
                    val id = cellInfo.cellIdentity as? android.telephony.CellIdentityNr ?: return null
                    val sig = cellInfo.cellSignalStrength as? android.telephony.CellSignalStrengthNr ?: return null
                    val nci = id.nci.takeIf { it != Long.MAX_VALUE && it > 0 } ?: return null
                    val (obsLat, obsLon) = addMarkerOffset(latitude, longitude, nci.toInt())
                    CellTower(
                        cellId = nci.toInt(),
                        locationAreaCode = id.tac.takeIf { it != Int.MAX_VALUE } ?: 0,
                        mobileCountryCode = id.mccString ?: "",
                        mobileNetworkCode = id.mncString ?: "",
                        networkType = "5G",
                        signalStrength = sig.dbm,
                        latitude = obsLat,
                        longitude = obsLon,
                        timestamp = timestamp,
                        isPrimary = cellInfo.isRegistered,
                        isServingCell = cellInfo.isRegistered,
                        pci = id.pci.takeIf { it != Int.MAX_VALUE } ?: 0
                    )
                } else null
            }
        }
    }
    
    // Called when GPS location updates
    fun onLocationUpdate(latitude: Double, longitude: Double) {
        if (latitude == 0.0 && longitude == 0.0) return
        
        val state = _uiState.value
        // Only rescan if moved significantly (>100m) or never scanned
        val shouldScan = state.lastScanTime == 0L || 
            distanceMeters(state.userLatitude, state.userLongitude, latitude, longitude) > 100
        
        _uiState.value = state.copy(userLatitude = latitude, userLongitude = longitude)
        
        if (shouldScan) {
            scanCellTowers(latitude, longitude)
        }
    }
    
    private fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
                kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
                kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)
        return r * 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
    }
    
    fun refresh() {
        com.stingrayshield.util.DebugLog.d("MapViewModel", "=== Manual refresh triggered ===")
        getLocationAndScan()
        loadStingrays()
        // Also fetch locations for any towers that need them
        viewModelScope.launch {
            kotlinx.coroutines.delay(1000) // Wait for scan to complete
            val towers = _uiState.value.cellTowers
            if (towers.any { !it.hasRealLocation() }) {
                fetchRealTowerLocationsAsync(towers)
            }
        }
    }
    
    /**
     * Load cell towers within the visible map bounds.
     * Called automatically when the user pans or zooms the map.
     */
    fun loadTowersInBounds(minLat: Double, maxLat: Double, minLng: Double, maxLng: Double) {
        // Create a hash of the bounds to avoid duplicate loads
        val boundsHash = (minLat * 1000).toInt() + (maxLat * 1000).toInt() + (minLng * 1000).toInt() + (maxLng * 1000).toInt()
        if (boundsHash == _uiState.value.lastBoundsHash) return
        
        com.stingrayshield.util.DebugLog.d("MapViewModel", "=== Loading towers in bounds: $minLat,$minLng to $maxLat,$maxLng ===")
        
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, loadingProgress = 0, lastBoundsHash = boundsHash)
                
                // Expand bounds slightly to include nearby towers
                val latPadding = (maxLat - minLat) * 0.1
                val lngPadding = (maxLng - minLng) * 0.1
                
                val towers = cellTowerRepository.getCellTowersInBounds(
                    minLat - latPadding,
                    maxLat + latPadding,
                    minLng - lngPadding,
                    maxLng + lngPadding
                )
                
                com.stingrayshield.util.DebugLog.d("MapViewModel", "Found ${towers.size} towers in bounds")
                
                // Merge with currently displayed towers
                val existingTowers = _uiState.value.cellTowers.associateBy { it.cellId }.toMutableMap()
                towers.forEach { tower -> existingTowers[tower.cellId] = tower }
                
                _uiState.value = _uiState.value.copy(
                    cellTowers = existingTowers.values.toList(),
                    isLoading = false,
                    loadingProgress = 0,
                    error = null
                )
                
                // Fetch real locations for any towers that need them
                val towersNeedingLocations = towers.filter { !it.hasRealLocation() }
                if (towersNeedingLocations.isNotEmpty()) {
                    fetchRealTowerLocationsAsync(towers)
                }
                
            } catch (e: Exception) {
                com.stingrayshield.util.DebugLog.e("MapViewModel", "Error loading towers in bounds: ${e.message}")
                _uiState.value = _uiState.value.copy(isLoading = false, loadingProgress = 0)
            }
        }
    }
    
    fun selectTower(tower: CellTower?) {
        _uiState.value = _uiState.value.copy(selectedTower = tower, selectedStingray = null)
    }
    
    fun selectStingray(device: StingrayDevice?) {
        _uiState.value = _uiState.value.copy(selectedStingray = device, selectedTower = null)
    }
    
    fun clearSelection() {
        _uiState.value = _uiState.value.copy(selectedTower = null, selectedStingray = null)
    }
    
    fun getTowerShareText(tower: CellTower): String {
        val locationInfo = if (tower.hasRealLocation()) {
            "Location: ${tower.towerLatitude}, ${tower.towerLongitude} (${tower.locationSource ?: "database"})"
        } else {
            "Observed at: ${tower.latitude}, ${tower.longitude}"
        }
        
        return """
            |📡 Cell Tower
            |Carrier: ${tower.getCarrierName()}
            |Network: ${tower.networkType}
            |Cell ID: ${tower.cellId}
            |LAC: ${tower.locationAreaCode}
            |MCC/MNC: ${tower.mobileCountryCode}/${tower.mobileNetworkCode}
            |Signal: ${tower.signalStrength} dBm
            |$locationInfo
            |${if (tower.isSuspicious) "⚠️ SUSPICIOUS" else "✅ Normal"}
        """.trimMargin()
    }
    
    fun getStingrayShareText(device: StingrayDevice): String {
        return """
            |🚨 STINGRAY DETECTED
            |Threat Level: ${device.threatLevel}
            |Cell ID: ${device.cellId ?: "Unknown"}
            |Signal: ${device.signalStrength ?: "?"} dBm
            |Location: ${device.latitude}, ${device.longitude}
        """.trimMargin()
    }
}

data class MapUiState(
    val isLoading: Boolean = false,
    /** Loading progress 0–100 when isLoading is true. */
    val loadingProgress: Int = 0,
    val error: String? = null,
    val cellTowers: List<CellTower> = emptyList(),
    val stingrayDevices: List<StingrayDevice> = emptyList(),
    val selectedTower: CellTower? = null,
    val selectedStingray: StingrayDevice? = null,
    val userLatitude: Double = 0.0,
    val userLongitude: Double = 0.0,
    val lastScanTime: Long = 0,
    val lastBoundsHash: Int = 0,
    /** Cell IDs the device is currently connected to (for map highlight). */
    val currentServingCellIds: Set<Int> = emptySet()
)
