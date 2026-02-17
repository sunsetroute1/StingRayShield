package com.stingrayshield.detection

import android.location.Location
import android.telephony.CellInfo
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellInfoWcdma
import android.os.Build
import com.stingrayshield.domain.model.AnomalyType
import com.stingrayshield.domain.model.DetectionEvent
import com.stingrayshield.domain.model.StingrayDevice
import com.stingrayshield.domain.model.ThreatLevel

/**
 * Extracts hardware and software identification metrics from detected stingray devices
 */
object DeviceIdentifier {
    
    /**
     * Extract device identification from a detection event and cell information
     */
    fun extractDeviceIdentification(
        detectionEvent: DetectionEvent,
        cellInfo: CellInfo?,
        location: Location? = null
    ): StingrayDevice {
        val device = StingrayDevice(
            detectionTimestamp = detectionEvent.timestamp,
            threatLevel = detectionEvent.threatLevel,
            detectionEventId = detectionEvent.id,
            cellId = detectionEvent.cellId,
            locationAreaCode = detectionEvent.locationAreaCode,
            signalStrength = detectionEvent.signalStrength,
            latitude = detectionEvent.latitude ?: location?.latitude,
            longitude = detectionEvent.longitude ?: location?.longitude,
            locationAccuracy = location?.accuracy,
            anomalyTypes = detectionEvent.anomalyType.name,
            behaviorPattern = detectionEvent.description
        )
        
        // Extract network-specific identification
        cellInfo?.let { extractNetworkSpecificInfo(it, device) }
        
        // Generate device fingerprint
        val fingerprint = device.generateFingerprint()
        
        // Infer hardware/software characteristics from signal patterns
        inferDeviceCharacteristics(device, detectionEvent)
        
        return device.copy(deviceFingerprint = fingerprint)
    }
    
    /**
     * Extract network-specific identification information
     */
    private fun extractNetworkSpecificInfo(cellInfo: CellInfo, device: StingrayDevice): StingrayDevice {
        return when {
            // 2G GSM
            cellInfo is CellInfoGsm -> {
                val identity = cellInfo.cellIdentity
                val signal = cellInfo.cellSignalStrength
                
                device.copy(
                    networkType = "2G",
                    mobileCountryCode = identity.mccString,
                    mobileNetworkCode = identity.mncString,
                    locationAreaCode = identity.lac.takeIf { it != Int.MAX_VALUE && it > 0 },
                    rssi = signal.dbm,
                    arfcn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        identity.arfcn.takeIf { it != Int.MAX_VALUE }
                    } else null
                )
            }
            
            // 3G WCDMA/UMTS
            cellInfo is CellInfoWcdma -> {
                val identity = cellInfo.cellIdentity
                val signal = cellInfo.cellSignalStrength
                
                device.copy(
                    networkType = "3G",
                    mobileCountryCode = identity.mccString,
                    mobileNetworkCode = identity.mncString,
                    locationAreaCode = identity.lac.takeIf { it != Int.MAX_VALUE && it > 0 },
                    rssi = signal.dbm,
                    uarfcn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        identity.uarfcn.takeIf { it != Int.MAX_VALUE }
                    } else null,
                    psc = identity.psc.takeIf { it != Int.MAX_VALUE && it > 0 }
                )
            }
            
            // 4G LTE
            cellInfo is CellInfoLte -> {
                val identity = cellInfo.cellIdentity
                val signal = cellInfo.cellSignalStrength
                
                device.copy(
                    networkType = "4G",
                    mobileCountryCode = identity.mccString,
                    mobileNetworkCode = identity.mncString,
                    tac = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        identity.tac.takeIf { it != Int.MAX_VALUE && it > 0 }
                    } else null,
                    rsrp = signal.rsrp,
                    rsrq = signal.rsrq,
                    sinr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        signal.rssnr.takeIf { it != Int.MAX_VALUE }
                    } else null,
                    pci = identity.pci.takeIf { it != Int.MAX_VALUE && it > 0 }
                )
            }
            
            // 5G NR
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && cellInfo is CellInfoNr -> {
                val identity = cellInfo.cellIdentity as? android.telephony.CellIdentityNr
                val signal = cellInfo.cellSignalStrength as? android.telephony.CellSignalStrengthNr
                
                if (identity != null && signal != null) {
                    device.copy(
                        networkType = "5G",
                        mobileCountryCode = identity.mccString,
                        mobileNetworkCode = identity.mncString,
                        tac = identity.tac.takeIf { it != Int.MAX_VALUE && it > 0 },
                        rsrp = signal.ssRsrp,
                        rsrq = signal.ssRsrq,
                        sinr = signal.ssSinr.takeIf { it != Int.MAX_VALUE },
                        pci = identity.pci.takeIf { it != Int.MAX_VALUE && it > 0 }
                    )
                } else {
                    device
                }
            }
            
            else -> device
        }
    }
    
    /**
     * Infer hardware and software characteristics from signal patterns and behavior
     */
    private fun inferDeviceCharacteristics(
        device: StingrayDevice,
        detectionEvent: DetectionEvent
    ): StingrayDevice {
        var updatedDevice = device
        val characteristics = mutableListOf<String>()
        
        // Analyze signal patterns to infer device type
        when {
            // Very strong signal with perfect quality metrics suggests commercial stingray
            device.rsrp != null && device.rsrp > -70 && device.rsrq != null && device.rsrq > -5 -> {
                characteristics.add("Commercial-grade")
                updatedDevice = updatedDevice.copy(
                    hardwareVendor = "Inferred: Commercial IMSI-Catcher",
                    hardwareModel = "High-power cell simulator"
                )
            }
            
            // Unusual ARFCN/UARFCN suggests custom or modified equipment
            device.arfcn != null && (device.arfcn < 0 || device.arfcn > 1023) -> {
                characteristics.add("Non-standard frequency")
                updatedDevice = updatedDevice.copy(
                    hardwareVendor = "Inferred: Custom/Modified",
                    hardwareModel = "Non-standard ARFCN"
                )
            }
            
            // Encryption downgrade suggests software-based attack
            detectionEvent.anomalyType == AnomalyType.ENCRYPTION_CHANGE -> {
                characteristics.add("Software-based attack")
                updatedDevice = updatedDevice.copy(
                    softwareVersion = "Inferred: Encryption manipulation capable",
                    protocolVersion = "Multi-protocol"
                )
            }
            
            // Multiple anomaly types suggest sophisticated device
            detectionEvent.threatLevel == ThreatLevel.HIGH || detectionEvent.threatLevel == ThreatLevel.CRITICAL -> {
                characteristics.add("Sophisticated device")
                updatedDevice = updatedDevice.copy(
                    hardwareVendor = "Inferred: Advanced IMSI-Catcher",
                    firmwareVersion = "Multi-vector attack capable"
                )
            }
        }
        
        // Update behavior pattern with characteristics
        val behaviorPattern = if (characteristics.isNotEmpty()) {
            "${device.behaviorPattern}\n\nInferred characteristics: ${characteristics.joinToString(", ")}"
        } else {
            device.behaviorPattern
        }
        
        return updatedDevice.copy(behaviorPattern = behaviorPattern)
    }
    
    /**
     * Combine multiple detection events to build a more complete device profile
     */
    fun combineDeviceProfiles(
        existingDevice: StingrayDevice,
        newDetectionEvent: DetectionEvent,
        cellInfo: CellInfo?,
        location: Location? = null
    ): StingrayDevice {
        val newDevice = extractDeviceIdentification(newDetectionEvent, cellInfo, location)
        
        // Merge anomaly types
        val existingAnomalies = existingDevice.anomalyTypes?.split(",")?.toSet() ?: emptySet()
        val newAnomalies = newDevice.anomalyTypes?.split(",")?.toSet() ?: emptySet()
        val combinedAnomalies = (existingAnomalies + newAnomalies).joinToString(",")
        
        // Use highest threat level
        val highestThreatLevel = when {
            existingDevice.threatLevel == ThreatLevel.CRITICAL || newDevice.threatLevel == ThreatLevel.CRITICAL -> ThreatLevel.CRITICAL
            existingDevice.threatLevel == ThreatLevel.HIGH || newDevice.threatLevel == ThreatLevel.HIGH -> ThreatLevel.HIGH
            existingDevice.threatLevel == ThreatLevel.MEDIUM || newDevice.threatLevel == ThreatLevel.MEDIUM -> ThreatLevel.MEDIUM
            else -> ThreatLevel.LOW
        }
        
        // Merge behavior patterns
        val combinedBehavior = buildString {
            append(existingDevice.behaviorPattern ?: "")
            if (newDevice.behaviorPattern != null) {
                if (isNotEmpty()) append("\n\n")
                append("Additional detection: ${newDevice.behaviorPattern}")
            }
        }
        
        return existingDevice.copy(
            threatLevel = highestThreatLevel,
            anomalyTypes = combinedAnomalies,
            behaviorPattern = combinedBehavior,
            // Update with most recent signal data
            signalStrength = newDevice.signalStrength ?: existingDevice.signalStrength,
            rsrp = newDevice.rsrp ?: existingDevice.rsrp,
            rsrq = newDevice.rsrq ?: existingDevice.rsrq,
            sinr = newDevice.sinr ?: existingDevice.sinr,
            // Update location if more accurate
            latitude = if (newDevice.locationAccuracy != null && 
                          (existingDevice.locationAccuracy == null || 
                           newDevice.locationAccuracy < existingDevice.locationAccuracy)) {
                newDevice.latitude
            } else existingDevice.latitude,
            longitude = if (newDevice.locationAccuracy != null && 
                           (existingDevice.locationAccuracy == null || 
                            newDevice.locationAccuracy < existingDevice.locationAccuracy)) {
                newDevice.longitude
            } else existingDevice.longitude,
            locationAccuracy = if (newDevice.locationAccuracy != null && 
                                 (existingDevice.locationAccuracy == null || 
                                  newDevice.locationAccuracy < existingDevice.locationAccuracy)) {
                newDevice.locationAccuracy
            } else existingDevice.locationAccuracy
        )
    }
}

