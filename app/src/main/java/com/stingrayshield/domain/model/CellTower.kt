package com.stingrayshield.domain.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import com.stingrayshield.util.CellTowerLocationEstimator
import kotlinx.parcelize.Parcelize
import java.time.Instant

/**
 * Represents a cell tower with its identifying information and signal characteristics.
 */
@Parcelize
@Entity(
    tableName = "cell_towers",
    indices = [
        Index("cellId"),
        Index("isPrimary"),
        Index("timestamp"),
        Index("isPrimary", "cellId"),
        Index("isPrimary", "timestamp")
    ]
)
data class CellTower(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val cellId: Int, // Unique identifier for the cell
    val locationAreaCode: Int, // LAC (Location Area Code)
    val mobileCountryCode: String, // MCC
    val mobileNetworkCode: String, // MNC
    val networkType: String = "Unknown", // 2G/3G/4G/5G as string
    val signalStrength: Int, // in dBm
    val timestamp: Long = Instant.now().toEpochMilli(),
    val latitude: Double = 0.0, // User's location when tower was observed
    val longitude: Double = 0.0, // User's location when tower was observed
    // Actual tower location from public database (OpenCellID, etc.)
    val towerLatitude: Double? = null, // Real tower location
    val towerLongitude: Double? = null, // Real tower location
    val towerRange: Int? = null, // Tower range/accuracy in meters
    val locationSource: String? = null, // Source of location data: "opencellid", "mozilla", "cached", etc.
    val isPrimary: Boolean = true, // Whether this is the primary connected tower
    val isServingCell: Boolean = true, // Whether this cell is currently serving the device
    val isTrusted: Boolean = false, // User marked as trusted
    val isSuspicious: Boolean = false, // Flagged as suspicious
    val psc: Int = 0, // Primary Scrambling Code (3G)
    val pci: Int = 0, // Physical Cell Identity (4G/5G)
    val firstSeen: Long = Instant.now().toEpochMilli(),
    val lastSeen: Long = Instant.now().toEpochMilli(),
    val observationCount: Int = 1
) : Parcelable {

    // Convenience properties for shorter names
    val mcc: String get() = mobileCountryCode
    val mnc: String get() = mobileNetworkCode
    val lac: Int get() = locationAreaCode

    /**
     * Get the cell identity string (MCC+MNC+LAC+CID)
     */
    fun getCellIdentity(): String {
        return "$mobileCountryCode-$mobileNetworkCode-$locationAreaCode-$cellId"
    }
    
    /**
     * Get a normalized signal quality value between 0.0 and 1.0
     */
    fun getSignalQualityNormalized(): Double {
        // Signal quality as a normalized value between 0.0 and 1.0
        // Typical signal strength range is from -50 dBm (excellent) to -120 dBm (poor)
        val min = -120.0
        val max = -50.0
        return kotlin.math.min(1.0, kotlin.math.max(0.0, (signalStrength - min) / (max - min)))
    }
    
    /**
     * Get a human-readable description of the signal strength
     */
    fun getSignalQualityDescription(): String {
        return when {
            signalStrength >= -70 -> "Excellent"
            signalStrength >= -85 -> "Good"
            signalStrength >= -100 -> "Fair"
            signalStrength >= -110 -> "Poor"
            else -> "Very Poor"
        }
    }
    
    /**
     * Get the carrier/network provider name based on MCC/MNC
     */
    fun getCarrierName(): String {
        return CellTowerLocationEstimator.getCarrierName(mobileCountryCode, mobileNetworkCode)
    }
    
    /**
     * Check if this tower has real location data from a public database
     */
    fun hasRealLocation(): Boolean {
        return towerLatitude != null && towerLongitude != null &&
               towerLatitude != 0.0 && towerLongitude != 0.0
    }
    
    /**
     * Get the best available location for display (real location if available, otherwise observation location)
     */
    fun getDisplayLatitude(): Double {
        return if (hasRealLocation()) towerLatitude!! else latitude
    }
    
    /**
     * Get the best available location for display (real location if available, otherwise observation location)
     */
    fun getDisplayLongitude(): Double {
        return if (hasRealLocation()) towerLongitude!! else longitude
    }
}
