package com.stingrayshield.util

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.TrafficStats
import android.os.Build
import android.telephony.TelephonyManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * Helper class for tracking network traffic statistics
 * Uses multiple methods to get accurate data on different Android versions
 */
class NetworkStatsHelper(private val context: Context) {

    private val telephonyManager: TelephonyManager? by lazy {
        context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
    }

    private val networkStatsManager: NetworkStatsManager? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            context.getSystemService(Context.NETWORK_STATS_SERVICE) as? NetworkStatsManager
        } else null
    }

    // Store baseline values for session tracking
    private var baselineTxBytes: Long = 0
    private var baselineRxBytes: Long = 0
    private var sessionStartTime: Long = 0

    /**
     * Data class to hold network traffic statistics
     */
    data class TrafficData(
        val totalSentBytes: Long,
        val totalReceivedBytes: Long,
        val sessionSentBytes: Long,
        val sessionReceivedBytes: Long,
        val mobileNetworkAvailable: Boolean,
        val dataSource: String // Indicates which API provided the data
    ) {
        val totalSentFormatted: String get() = formatBytes(totalSentBytes)
        val totalReceivedFormatted: String get() = formatBytes(totalReceivedBytes)
        val sessionSentFormatted: String get() = formatBytes(sessionSentBytes)
        val sessionReceivedFormatted: String get() = formatBytes(sessionReceivedBytes)

        companion object {
            fun formatBytes(bytes: Long): String {
                if (bytes < 0) return "N/A"
                return when {
                    bytes < 1024 -> "$bytes B"
                    bytes < 1024 * 1024 -> String.format("%.2f KB", bytes / 1024.0)
                    bytes < 1024 * 1024 * 1024 -> String.format("%.2f MB", bytes / (1024.0 * 1024.0))
                    else -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
                }
            }
        }
    }

    /**
     * Start a new tracking session (resets session counters)
     */
    fun startSession() {
        sessionStartTime = System.currentTimeMillis()
        val stats = getTrafficStatsBasic()
        baselineTxBytes = stats.first
        baselineRxBytes = stats.second
    }

    /**
     * Get current traffic statistics using multiple methods for reliability
     */
    suspend fun getTrafficStats(): TrafficData = withContext(Dispatchers.IO) {
        // Try multiple methods to get the most accurate data
        
        // Method 1: Try TrafficStats API (most compatible but may return -1)
        val basicStats = getTrafficStatsBasic()
        
        // Method 2: Try NetworkStatsManager (Android M+, more reliable but needs permission)
        val managerStats = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getNetworkStatsManagerData()
        } else {
            null
        }
        
        // Method 3: Try UID-based stats as fallback
        val uidStats = getUidBasedStats()
        
        // Determine best available data source
        val (txBytes, rxBytes, source) = when {
            // Prefer NetworkStatsManager if available and valid
            managerStats != null && managerStats.first >= 0 && managerStats.second >= 0 -> {
                Triple(managerStats.first, managerStats.second, "NetworkStatsManager")
            }
            // Fall back to TrafficStats if both values are valid
            basicStats.first >= 0 && basicStats.second >= 0 -> {
                Triple(basicStats.first, basicStats.second, "TrafficStats")
            }
            // If only TX is valid from TrafficStats, use UID stats for RX
            basicStats.first >= 0 && uidStats.second >= 0 -> {
                Triple(basicStats.first, uidStats.second, "Mixed (TrafficStats TX, UID RX)")
            }
            // Use UID stats if available
            uidStats.first >= 0 && uidStats.second >= 0 -> {
                Triple(uidStats.first, uidStats.second, "UID Stats")
            }
            // Last resort: use whatever TrafficStats gives us
            else -> {
                Triple(
                    if (basicStats.first >= 0) basicStats.first else 0L,
                    if (basicStats.second >= 0) basicStats.second else 0L,
                    "Partial/Unavailable"
                )
            }
        }

        // Calculate session stats
        val sessionTx = if (baselineTxBytes > 0 && txBytes >= baselineTxBytes) {
            txBytes - baselineTxBytes
        } else {
            0L
        }
        
        val sessionRx = if (baselineRxBytes > 0 && rxBytes >= baselineRxBytes) {
            rxBytes - baselineRxBytes
        } else {
            0L
        }

        TrafficData(
            totalSentBytes = txBytes,
            totalReceivedBytes = rxBytes,
            sessionSentBytes = sessionTx,
            sessionReceivedBytes = sessionRx,
            mobileNetworkAvailable = isMobileNetworkAvailable(),
            dataSource = source
        )
    }

    /**
     * Get basic traffic stats from TrafficStats API
     * Returns Pair(txBytes, rxBytes), -1 if unavailable
     */
    private fun getTrafficStatsBasic(): Pair<Long, Long> {
        val txBytes = TrafficStats.getMobileTxBytes()
        val rxBytes = TrafficStats.getMobileRxBytes()
        
        // Also try total stats if mobile returns -1
        val totalTx = if (txBytes == TrafficStats.UNSUPPORTED.toLong()) {
            TrafficStats.getTotalTxBytes()
        } else txBytes
        
        val totalRx = if (rxBytes == TrafficStats.UNSUPPORTED.toLong()) {
            TrafficStats.getTotalRxBytes()
        } else rxBytes
        
        return Pair(totalTx, totalRx)
    }

    /**
     * Get traffic stats using NetworkStatsManager (Android M+)
     * This is more reliable but requires PACKAGE_USAGE_STATS permission
     */
    private fun getNetworkStatsManagerData(): Pair<Long, Long>? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return null
        
        return try {
            val manager = networkStatsManager ?: return null
            
            // Get stats for the current day
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            val startTime = calendar.timeInMillis
            val endTime = System.currentTimeMillis()

            // Try to get mobile network stats
            val subscriberId = getSubscriberId()
            
            var totalTx = 0L
            var totalRx = 0L

            if (subscriberId != null) {
                try {
                    val bucket = manager.querySummaryForDevice(
                        ConnectivityManager.TYPE_MOBILE,
                        subscriberId,
                        startTime,
                        endTime
                    )
                    totalTx = bucket.txBytes
                    totalRx = bucket.rxBytes
                } catch (e: Exception) {
                    // Permission might be denied or other error
                    return null
                }
            }

            if (totalTx > 0 || totalRx > 0) {
                Pair(totalTx, totalRx)
            } else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get UID-based traffic stats (per-application)
     * Uses the current app's UID to get more specific data
     */
    private fun getUidBasedStats(): Pair<Long, Long> {
        val uid = android.os.Process.myUid()
        val txBytes = TrafficStats.getUidTxBytes(uid)
        val rxBytes = TrafficStats.getUidRxBytes(uid)
        return Pair(txBytes, rxBytes)
    }

    /**
     * Get all UIDs traffic stats combined
     * This iterates through known UIDs to get total device traffic
     */
    private fun getAllAppsTrafficStats(): Pair<Long, Long> {
        var totalTx = 0L
        var totalRx = 0L
        
        // Get stats for common UID ranges (apps typically have UIDs >= 10000)
        for (uid in 0..99999) {
            val tx = TrafficStats.getUidTxBytes(uid)
            val rx = TrafficStats.getUidRxBytes(uid)
            if (tx > 0) totalTx += tx
            if (rx > 0) totalRx += rx
        }
        
        return Pair(totalTx, totalRx)
    }

    /**
     * Check if mobile network is currently available
     */
    private fun isMobileNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = connectivityManager?.activeNetwork
                val capabilities = connectivityManager?.getNetworkCapabilities(network)
                capabilities?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) == true
            } else {
                @Suppress("DEPRECATION")
                connectivityManager?.activeNetworkInfo?.type == ConnectivityManager.TYPE_MOBILE
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get subscriber ID for NetworkStatsManager queries
     */
    @Suppress("MissingPermission")
    private fun getSubscriberId(): String? {
        return try {
            telephonyManager?.subscriberId
        } catch (e: SecurityException) {
            null
        }
    }

    /**
     * Get detailed traffic breakdown by network type
     */
    suspend fun getDetailedTrafficStats(): Map<String, TrafficData> = withContext(Dispatchers.IO) {
        val results = mutableMapOf<String, TrafficData>()
        
        // Mobile data
        val mobileStats = getTrafficStats()
        results["Mobile"] = mobileStats
        
        // Total (includes WiFi)
        val totalTx = TrafficStats.getTotalTxBytes()
        val totalRx = TrafficStats.getTotalRxBytes()
        results["Total"] = TrafficData(
            totalSentBytes = totalTx,
            totalReceivedBytes = totalRx,
            sessionSentBytes = 0,
            sessionReceivedBytes = 0,
            mobileNetworkAvailable = true,
            dataSource = "TrafficStats Total"
        )
        
        // WiFi (calculated as total - mobile)
        if (mobileStats.totalSentBytes >= 0 && mobileStats.totalReceivedBytes >= 0 &&
            totalTx >= 0 && totalRx >= 0) {
            results["WiFi"] = TrafficData(
                totalSentBytes = maxOf(0, totalTx - mobileStats.totalSentBytes),
                totalReceivedBytes = maxOf(0, totalRx - mobileStats.totalReceivedBytes),
                sessionSentBytes = 0,
                sessionReceivedBytes = 0,
                mobileNetworkAvailable = false,
                dataSource = "Calculated"
            )
        }
        
        results
    }
}













