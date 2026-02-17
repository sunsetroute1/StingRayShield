package com.stingrayshield.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Represents the overall system status of the stingray detection
 */
enum class DetectionStatus {
    NORMAL,     // No suspicious activity detected
    WARNING,    // Potential unusual network activity
    ALERT,      // High probability of stingray detected
    DANGER,     // Stingray likely in use
    UNKNOWN     // Insufficient data to determine status
}

/**
 * Represents the current state of the detection system
 */
@Parcelize
data class SystemStatus(
    val detectionStatus: DetectionStatus = DetectionStatus.UNKNOWN,
    val isServiceRunning: Boolean = false,
    val activeDetectors: List<String> = emptyList(),
    val lastScanTime: Long = 0,
    val activeScanCount: Int = 0,
    val detectedEvents: Int = 0,
    val currentCellId: Int? = null,
    val currentSignalStrength: Int? = null,
    val currentNetworkType: String? = null,
    val isEncryptionSecure: Boolean = true,
    // Traffic stats
    val totalSentBytes: Long = 0,
    val totalReceivedBytes: Long = 0,
    val sessionSentBytes: Long = 0,
    val sessionReceivedBytes: Long = 0,
    val trafficDataSource: String = "Unknown"
) : Parcelable {
    /**
     * Get a summary of the current system status
     */
    fun getStatusSummary(): String {
        return when (detectionStatus) {
            DetectionStatus.NORMAL -> "All systems normal. No suspicious activity detected."
            DetectionStatus.WARNING -> "Warning: Some unusual network activity detected. Monitoring..."
            DetectionStatus.ALERT -> "Alert: High probability of stingray device detected!"
            DetectionStatus.DANGER -> "DANGER: Stingray device likely active in your area!"
            DetectionStatus.UNKNOWN -> "System initializing or insufficient data available."
        }
    }
    
    /**
     * Get a recommendation based on the current status
     */
    fun getStatusRecommendation(): String {
        return when (detectionStatus) {
            DetectionStatus.NORMAL -> "Continue using your device normally."
            DetectionStatus.WARNING -> "Consider disabling mobile data if privacy is a concern."
            DetectionStatus.ALERT -> "Enable airplane mode or power off your device if you require privacy."
            DetectionStatus.DANGER -> "Power off your device immediately or remove battery if possible."
            DetectionStatus.UNKNOWN -> "Allow the system to gather more data."
        }
    }
}
