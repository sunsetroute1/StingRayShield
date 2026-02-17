package com.stingrayshield.domain.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import java.time.Instant

/**
 * Represents an identified stingray device with hardware and software identification metrics
 */
@Parcelize
@Entity(tableName = "stingray_devices")
data class StingrayDevice(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // Detection information
    val detectionTimestamp: Long = Instant.now().toEpochMilli(),
    val threatLevel: ThreatLevel,
    val detectionEventId: Long? = null, // Link to the detection event that identified this device
    
    // Cell tower identification
    val cellId: Int? = null,
    val locationAreaCode: Int? = null,
    val mobileCountryCode: String? = null,
    val mobileNetworkCode: String? = null,
    val networkType: String? = null, // 2G, 3G, 4G, 5G
    
    // Signal characteristics
    val signalStrength: Int? = null,
    val rsrp: Int? = null, // Reference Signal Received Power (LTE/5G)
    val rsrq: Int? = null, // Reference Signal Received Quality (LTE/5G)
    val sinr: Int? = null, // Signal to Interference plus Noise Ratio (LTE/5G)
    val rssi: Int? = null, // Received Signal Strength Indicator (2G/3G)
    val arfcn: Int? = null, // Absolute Radio Frequency Channel Number (2G)
    val uarfcn: Int? = null, // UTRA Absolute Radio Frequency Channel Number (3G)
    val psc: Int? = null, // Primary Scrambling Code (3G)
    val pci: Int? = null, // Physical Cell ID (LTE)
    val nci: Long? = null, // NR Cell Identity (5G)
    val tac: Int? = null, // Tracking Area Code (LTE/5G)
    
    // Hardware identification metrics
    val hardwareVendor: String? = null, // Inferred from signal patterns
    val hardwareModel: String? = null, // Inferred from signal patterns
    val firmwareVersion: String? = null, // Inferred from behavior patterns
    val deviceFingerprint: String? = null, // Unique combination of identifiers
    
    // Software identification metrics
    val softwareVersion: String? = null,
    val protocolVersion: String? = null,
    val encryptionType: String? = null,
    val cipheringIndicator: Int? = null,
    
    // Behavioral characteristics
    val behaviorPattern: String? = null, // Description of detected behavior
    val anomalyTypes: String? = null, // Comma-separated list of anomaly types detected
    
    // Location information
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationAccuracy: Float? = null,
    
    // Additional metadata
    val notes: String? = null,
    val isConfirmed: Boolean = false, // User confirmed this is a stingray
    val isFalsePositive: Boolean = false
) : Parcelable {
    
    /**
     * Generate a unique fingerprint from available identifiers
     */
    fun generateFingerprint(): String {
        val components = mutableListOf<String>()
        
        cellId?.let { components.add("CID:$it") }
        locationAreaCode?.let { components.add("LAC:$it") }
        mobileCountryCode?.let { components.add("MCC:$it") }
        mobileNetworkCode?.let { components.add("MNC:$it") }
        pci?.let { components.add("PCI:$it") }
        psc?.let { components.add("PSC:$it") }
        arfcn?.let { components.add("ARFCN:$it") }
        uarfcn?.let { components.add("UARFCN:$it") }
        
        return components.joinToString("|")
    }
    
    /**
     * Get a human-readable device identifier
     */
    fun getDeviceIdentifier(): String {
        return when {
            hardwareVendor != null && hardwareModel != null -> "$hardwareVendor $hardwareModel"
            deviceFingerprint != null -> "Device ${deviceFingerprint.take(16)}"
            cellId != null -> "Cell ID: $cellId"
            else -> "Unknown Device"
        }
    }
    
    /**
     * Get network generation as string
     */
    fun getNetworkGeneration(): String {
        return networkType ?: "Unknown"
    }
    
    /**
     * Check if device has sufficient identification data
     */
    fun hasIdentificationData(): Boolean {
        return cellId != null || 
               (mobileCountryCode != null && mobileNetworkCode != null) ||
               deviceFingerprint != null
    }
}

