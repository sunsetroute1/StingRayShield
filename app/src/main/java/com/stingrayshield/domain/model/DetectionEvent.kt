package com.stingrayshield.domain.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import java.time.Instant

/**
 * Represents different types of anomalies that can be detected
 */
enum class AnomalyType {
    TOWER_CONSISTENCY, // Cell tower information inconsistency
    SIGNAL_STRENGTH,   // Abnormal signal strength
    NEIGHBOR_CELLS,    // Missing neighboring cells
    ENCRYPTION_CHANGE, // Encryption downgrade
    SILENT_SMS,        // Silent SMS detection
    FEMTOCELL,         // Femtocell detection
    LOCATION_TRACKING, // Location-based tracking
    MULTIPLE_ANOMALIES,// Multiple detection methods confirm threat
    UNKNOWN
}

/**
 * Threat level for detected anomalies
 */
enum class ThreatLevel {
    NONE,       // No threat detected
    LOW,        // Low probability - could be normal network behavior
    MEDIUM,     // Medium probability - suspicious network behavior
    HIGH,       // High probability - likely a stingray device
    CRITICAL    // Critical - stingray almost certainly in use
}

/**
 * Represents a detected anomaly event that might indicate a stingray device
 */
@Parcelize
@Entity(tableName = "detection_events")
data class DetectionEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val anomalyType: AnomalyType,
    val threatLevel: ThreatLevel,
    val timestamp: Long = Instant.now().toEpochMilli(),
    val description: String,
    val additionalInfo: String = "", // Additional details about the event
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val cellId: Int = 0,
    val locationAreaCode: Int = 0,
    val mobileCountryCode: Int = 0, // MCC
    val mobileNetworkCode: Int = 0, // MNC
    val signalStrength: Int = 0,
    val isArchived: Boolean = false,
    val isFalsePositive: Boolean = false
) : Parcelable {
    
    // Convenience properties for shorter names
    val mcc: Int get() = mobileCountryCode
    val mnc: Int get() = mobileNetworkCode
    val lac: Int get() = locationAreaCode

    /**
     * Get a human-readable title for this detection event
     */
    fun getTitle(): String {
        return when (anomalyType) {
            AnomalyType.TOWER_CONSISTENCY -> "Cell Tower Information Inconsistency"
            AnomalyType.SIGNAL_STRENGTH -> "Abnormal Signal Strength"
            AnomalyType.NEIGHBOR_CELLS -> "Missing Neighboring Cells"
            AnomalyType.ENCRYPTION_CHANGE -> "Encryption Downgrade Detected"
            AnomalyType.SILENT_SMS -> "Silent SMS Detected"
            AnomalyType.FEMTOCELL -> "Suspicious Femtocell Detected"
            AnomalyType.LOCATION_TRACKING -> "Location Tracking Detected"
            AnomalyType.MULTIPLE_ANOMALIES -> "Multiple Stingray Indicators"
            AnomalyType.UNKNOWN -> "Unknown Anomaly"
        }
    }

    /**
     * Get an icon resource ID for this detection event
     */
    fun getIconResId(): Int {
        // Note: These resource IDs will be created in the drawable resources
        // For now, return 0 as placeholder - icons can be added later
        return 0
    }
}
