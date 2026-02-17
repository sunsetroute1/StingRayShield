package com.stingrayshield.domain.model

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Extension functions for domain models to enhance UI presentation
 */

/**
 * Gets a normalized signal quality value between 0.0 and 1.0
 * Assumes signal strength is in dBm and typical values are between -50 (excellent) and -120 (poor)
 */
fun CellTower.getSignalQuality(): Double {
    // Signal quality as a normalized value between 0.0 and 1.0
    // Typical signal strength range is from -50 dBm (excellent) to -120 dBm (poor)
    val min = -120.0
    val max = -50.0
    return min(1.0, max(0.0, (signalStrength - min) / (max - min)))
}

/**
 * Gets a readable title for a cell tower
 */
fun CellTower.getTitle(): String {
    return "Cell Tower: $cellId (${mobileCountryCode}-${mobileNetworkCode})"
}

/**
 * Gets a readable title for a detection event
 */
fun DetectionEvent.getTitle(): String {
    return when (anomalyType) {
        AnomalyType.SIGNAL_STRENGTH -> "Signal Strength Anomaly"
        AnomalyType.TOWER_CONSISTENCY -> "Tower Info Inconsistency"
        AnomalyType.NEIGHBOR_CELLS -> "Neighbor Cell Anomaly"
        AnomalyType.SILENT_SMS -> "Silent SMS Detected"
        AnomalyType.FEMTOCELL -> "Suspicious Femtocell"
        AnomalyType.ENCRYPTION_CHANGE -> "Encryption Downgrade"
        AnomalyType.LOCATION_TRACKING -> "Location Anomaly"
        AnomalyType.MULTIPLE_ANOMALIES -> "Multiple Anomalies Detected"
        AnomalyType.UNKNOWN -> "Unknown Anomaly"
    }
}

/**
 * Gets a color code based on threat level
 * Used to colorize UI elements according to threat levels
 */
fun DetectionEvent.getThreatColor(): Int {
    return when (threatLevel) {
        ThreatLevel.CRITICAL -> 0xFFD32F2F.toInt()
        ThreatLevel.HIGH -> 0xFFF57C00.toInt()
        ThreatLevel.MEDIUM -> 0xFFFFA000.toInt()
        ThreatLevel.LOW -> 0xFF1976D2.toInt()
        ThreatLevel.NONE -> 0xFF388E3C.toInt()
    }
}

/**
 * Checks if a detection event is high severity (critical or high threat)
 */
fun DetectionEvent.isHighSeverity(): Boolean {
    return threatLevel == ThreatLevel.CRITICAL || threatLevel == ThreatLevel.HIGH
}

/**
 * Formats a signal strength value for display
 */
fun CellTower.formatSignalStrength(): String {
    return "$signalStrength dBm"
}

/**
 * Calculates difference in dBm between two signal readings
 */
fun calculateSignalDifference(current: Int, previous: Int): Int {
    return current - previous
}

/**
 * Returns absolute difference in dBm between two signal readings
 */
fun calculateAbsoluteSignalDifference(current: Int, previous: Int): Int {
    return abs(current - previous)
}
