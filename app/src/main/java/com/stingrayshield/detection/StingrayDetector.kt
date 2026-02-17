package com.stingrayshield.detection

import android.location.Location
import android.telephony.CellInfo
import com.stingrayshield.data.repository.CellTowerRepository
import com.stingrayshield.data.repository.DetectionEventRepository
import com.stingrayshield.data.repository.StingrayDeviceRepository
import com.stingrayshield.domain.model.AnomalyType
import com.stingrayshield.domain.model.CellTower
import com.stingrayshield.domain.model.DetectionEvent
import com.stingrayshield.domain.model.ThreatLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * Main detector class that integrates various detection algorithms
 * and manages the detection pipeline
 */
@Singleton
class StingrayDetector @Inject constructor(
    private val cellTowerRepository: CellTowerRepository,
    private val detectionEventRepository: DetectionEventRepository,
    private val stingrayDeviceRepository: StingrayDeviceRepository,
    private val coroutineScope: CoroutineScope
) {
    // Tracking cell and signal data
    private var lastCellInfo: CellInfo? = null
    private var lastNetworkType: String? = null
    private var previousNeighborCount = AtomicInteger(0)
    private var lastCipheringIndicator = 0
    private var lastCipheringTime = 0L
    private var silentSmsCount = AtomicInteger(0)
    
    // Settings for which detection methods to use
    private var detectSignalStrengthEnabled = true
    private var detectTowerInfoEnabled = true
    private var detectNeighborCellsEnabled = true
    private var detectSilentSmsEnabled = true
    private var detectFemtocellEnabled = true
    private var detectEncryptionEnabled = true
    private var detectLocationEnabled = true

    /**
     * Configure which detection methods to use
     */
    fun configureDetection(
        signalStrength: Boolean = true,
        towerInfo: Boolean = true,
        neighborCells: Boolean = true,
        silentSms: Boolean = true,
        femtocell: Boolean = true,
        encryption: Boolean = true,
        location: Boolean = true
    ) {
        this.detectSignalStrengthEnabled = signalStrength
        this.detectTowerInfoEnabled = towerInfo
        this.detectNeighborCellsEnabled = neighborCells
        this.detectSilentSmsEnabled = silentSms
        this.detectFemtocellEnabled = femtocell
        this.detectEncryptionEnabled = encryption
        this.detectLocationEnabled = location
    }

    /**
     * Process new cell information and run detection algorithms
     * Returns list of detected events
     * @param cellInfoList List of current cell information
     * @param location Optional location data for device identification
     */
    suspend fun processCellInfo(
        cellInfoList: List<CellInfo>,
        location: Location? = null
    ): List<DetectionEvent> = withContext(Dispatchers.Default) {
        if (cellInfoList.isEmpty()) return@withContext emptyList()
        
        val detectedEvents = mutableListOf<DetectionEvent>()
        val currentNeighborCount = cellInfoList.size
        val currentCell = cellInfoList.firstOrNull() ?: return@withContext emptyList()
        
        // Load known cell towers for comparison
        val knownTowers = cellTowerRepository.getAllCellTowers().first()
        
        // Store all detected cell towers in the database
        storeCellTowers(cellInfoList, location)
        
        // Run various detection algorithms based on settings
        try {
            // 1. Signal strength anomalies
            if (detectSignalStrengthEnabled && lastCellInfo != null) {
                DetectionAlgorithms.detectSignalStrengthAnomalies(
                    currentCell, 
                    lastCellInfo, 
                    knownTowers
                )?.let { detectedEvents.add(it) }
            }
            
            // 2. Tower information inconsistencies
            if (detectTowerInfoEnabled) {
                DetectionAlgorithms.detectTowerInfoInconsistencies(
                    currentCell,
                    knownTowers
                )?.let { detectedEvents.add(it) }
            }
            
            // 3. Neighbor cell anomalies
            if (detectNeighborCellsEnabled) {
                val prevCount = previousNeighborCount.get()
                DetectionAlgorithms.detectNeighborCellAnomalies(
                    cellInfoList, 
                    prevCount
                )?.let { detectedEvents.add(it) }
                previousNeighborCount.set(currentNeighborCount)
            }
            
            // 3b. LAC/TAC inconsistency (SnoopSnitch-style A2 – serving LAC vs neighbors)
            if (detectTowerInfoEnabled) {
                DetectionAlgorithms.detectLacInconsistency(cellInfoList)?.let { detectedEvents.add(it) }
            }
            
            // 3c. Network downgrade (5G→4G, 4G→3G, etc. – encryption C1-style)
            if (detectEncryptionEnabled) {
                val currentType = DetectionAlgorithms.getNetworkTypeFromCellInfo(currentCell)
                DetectionAlgorithms.detectNetworkDowngrade(lastNetworkType, currentCell)?.let { detectedEvents.add(it) }
                lastNetworkType = currentType
            }
            
            // 4. Silent SMS detection - if any have been reported
            if (detectSilentSmsEnabled) {
                val smsCount = silentSmsCount.getAndSet(0)
                if (smsCount > 0) {
                    DetectionAlgorithms.detectSilentSms(smsCount)?.let { 
                        detectedEvents.add(it)
                    }
                }
            }
            
            // 5. Suspicious femtocell detection
            if (detectFemtocellEnabled) {
                DetectionAlgorithms.detectSuspiciousFemtocell(
                    currentCell,
                    knownTowers
                )?.let { detectedEvents.add(it) }
            }
            
            // 6. 2G GSM-specific anomalies
            DetectionAlgorithms.detect2GAnomalies(
                currentCell,
                knownTowers
            )?.let { detectedEvents.add(it) }
            
            // 7. 3G WCDMA/UMTS-specific anomalies
            DetectionAlgorithms.detect3GAnomalies(
                currentCell,
                knownTowers
            )?.let { detectedEvents.add(it) }
            
            // 8. 4G LTE-specific anomalies
            DetectionAlgorithms.detectLteAnomalies(
                currentCell,
                knownTowers
            )?.let { detectedEvents.add(it) }
            
            // 9. 5G NR-specific anomalies (on supported devices)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                DetectionAlgorithms.detect5GAnomalies(
                    currentCell,
                    knownTowers
                )?.let { detectedEvents.add(it) }
            }
            
            // 10. CDMA-specific anomalies (Verizon, US Cellular, Sprint legacy)
            DetectionAlgorithms.detectCdmaAnomalies(
                currentCell,
                knownTowers
            )?.let { detectedEvents.add(it) }

            // 11. Machine Learning enhanced detection
            // Load recent events for behavioral analysis
            val recentEvents = detectionEventRepository.getRecentEvents(50)
            performMachineLearningDetection(
                currentCell,
                recentEvents,
                knownTowers,
                location
            )?.let { detectedEvents.add(it) }

            // Store events in repository and extract device identification
            if (detectedEvents.isNotEmpty()) {
                storeDetectionEvents(detectedEvents)
                
                // Extract and store device identification for high-threat events
                detectedEvents.filter { 
                    it.threatLevel == ThreatLevel.HIGH || it.threatLevel == ThreatLevel.CRITICAL 
                }.forEach { event ->
                    extractAndStoreDeviceIdentification(event, currentCell, location)
                }
            }
            
            // Update last cell info and network type for next comparison
            lastCellInfo = currentCell
            if (lastNetworkType == null) {
                lastNetworkType = DetectionAlgorithms.getNetworkTypeFromCellInfo(currentCell)
            }
            
        } catch (e: Exception) {
            // Log exception but don't crash the detector
            detectedEvents.add(
                DetectionEvent(
                    anomalyType = AnomalyType.UNKNOWN,
                    threatLevel = ThreatLevel.NONE,
                    description = "Error during detection: ${e.message}",
                    timestamp = System.currentTimeMillis()
                )
            )
        }
        
        return@withContext detectedEvents
    }

    /**
     * Report an encryption indicator change that might indicate a downgrade attack
     */
    fun reportCipheringIndicator(indicator: Int) {
        val now = System.currentTimeMillis()
        if (detectEncryptionEnabled && lastCipheringTime > 0 && lastCipheringIndicator > 0) {
            val timeDelta = now - lastCipheringTime
            
            coroutineScope.launch {
                DetectionAlgorithms.detectEncryptionDowngrade(
                    indicator,
                    lastCipheringIndicator, 
                    timeDelta
                )?.let { event ->
                    storeDetectionEvents(listOf(event))
                }
            }
        }
        
        lastCipheringIndicator = indicator
        lastCipheringTime = now
    }

    /**
     * Report silent SMS detection
     */
    fun reportSilentSms() {
        silentSmsCount.incrementAndGet()
    }

    /**
     * Store detected events in the repository
     */
    private suspend fun storeDetectionEvents(events: List<DetectionEvent>) {
        events.forEach { event ->
            detectionEventRepository.addDetectionEvent(event)
        }
    }
    
    /**
     * Store cell tower information from CellInfo objects
     * Supports all US network types: GSM, CDMA, WCDMA, TDSCDMA, LTE, and 5G NR
     */
    private suspend fun storeCellTowers(cellInfoList: List<CellInfo>, location: Location?) {
        val currentTime = System.currentTimeMillis()

        cellInfoList.forEachIndexed { index, cellInfo ->
            val cellId = DetectionAlgorithms.getCellIdFromCellInfo(cellInfo) ?: return@forEachIndexed
            val signalStrength = DetectionAlgorithms.getSignalStrengthFromCellInfo(cellInfo) ?: return@forEachIndexed
            val mcc = DetectionAlgorithms.getMccFromCellInfo(cellInfo)?.toString() ?: "Unknown"
            val mnc = DetectionAlgorithms.getMncFromCellInfo(cellInfo)?.toString() ?: "Unknown"
            val lac = DetectionAlgorithms.getLacFromCellInfo(cellInfo) ?: 0

            // Use the comprehensive network type detection
            val networkType = DetectionAlgorithms.getNetworkTypeFromCellInfo(cellInfo)

            val isPrimary = index == 0 // First cell in list is usually the primary/serving cell

            // Get PCI/PSC for additional identification
            val pci = getPciFromCellInfo(cellInfo)
            val psc = getPscFromCellInfo(cellInfo)

            val cellTower = CellTower(
                cellId = cellId,
                locationAreaCode = lac,
                mobileCountryCode = mcc,
                mobileNetworkCode = mnc,
                networkType = networkType,
                signalStrength = signalStrength,
                timestamp = currentTime,
                latitude = location?.latitude ?: 0.0,
                longitude = location?.longitude ?: 0.0,
                isPrimary = isPrimary,
                isServingCell = isPrimary,
                firstSeen = currentTime,
                lastSeen = currentTime,
                pci = pci,
                psc = psc
            )

            try {
                cellTowerRepository.addCellTower(cellTower)
            } catch (e: Exception) {
                android.util.Log.e("StingrayDetector", "Error storing cell tower: ${e.message}")
            }
        }
    }
    
    /**
     * Get Physical Cell Identity (PCI) for LTE/5G cells
     */
    private fun getPciFromCellInfo(cellInfo: CellInfo): Int {
        return when {
            cellInfo is android.telephony.CellInfoLte -> 
                cellInfo.cellIdentity.pci.takeIf { it != Int.MAX_VALUE && it >= 0 } ?: 0
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q && 
                cellInfo is android.telephony.CellInfoNr -> {
                val identity = cellInfo.cellIdentity as? android.telephony.CellIdentityNr
                identity?.pci?.takeIf { it != Int.MAX_VALUE && it >= 0 } ?: 0
            }
            else -> 0
        }
    }
    
    /**
     * Get Primary Scrambling Code (PSC) for WCDMA/3G cells
     */
    private fun getPscFromCellInfo(cellInfo: CellInfo): Int {
        return when {
            cellInfo is android.telephony.CellInfoWcdma -> 
                cellInfo.cellIdentity.psc.takeIf { it != Int.MAX_VALUE && it >= 0 } ?: 0
            else -> 0
        }
    }
    
    /**
     * Extract device identification and store in repository
     */
    private suspend fun extractAndStoreDeviceIdentification(
        event: DetectionEvent,
        cellInfo: CellInfo?,
        location: Location?
    ) {
        try {
            // Extract device identification
            val device = DeviceIdentifier.extractDeviceIdentification(event, cellInfo, location)
            
            // Check if we already have a device with this fingerprint
            val fingerprint = device.deviceFingerprint
            if (fingerprint != null) {
                val existingDevices = stingrayDeviceRepository.getDevicesByFingerprint(fingerprint)
                
                if (existingDevices.isNotEmpty()) {
                    // Combine with existing device profile
                    val existingDevice = existingDevices.first()
                    val combinedDevice = DeviceIdentifier.combineDeviceProfiles(
                        existingDevice,
                        event,
                        cellInfo,
                        location
                    )
                    stingrayDeviceRepository.updateDevice(combinedDevice)
                } else {
                    // New device, insert it
                    stingrayDeviceRepository.insertDevice(device)
                }
            } else {
                // No fingerprint, but still store if we have cell ID
                if (device.cellId != null) {
                    val existingDevices = stingrayDeviceRepository.getDevicesByCellId(device.cellId)
                    
                    if (existingDevices.isNotEmpty()) {
                        // Combine with existing device
                        val existingDevice = existingDevices.first()
                        val combinedDevice = DeviceIdentifier.combineDeviceProfiles(
                            existingDevice,
                            event,
                            cellInfo,
                            location
                        )
                        stingrayDeviceRepository.updateDevice(combinedDevice)
                    } else {
                        // New device
                        stingrayDeviceRepository.insertDevice(device)
                    }
                } else {
                    // Store anyway if threat level is critical
                    if (device.threatLevel == ThreatLevel.CRITICAL) {
                        stingrayDeviceRepository.insertDevice(device)
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("StingrayDetector", "Error extracting device identification", e)
        }
    }

    /**
     * Enhanced detection using machine learning algorithms
     * Performs statistical analysis and pattern recognition
     */
    fun performMachineLearningDetection(
        currentCellInfo: CellInfo?,
        recentEvents: List<DetectionEvent>,
        knownTowers: List<CellTower>,
        userLocation: Location?
    ): DetectionEvent? {
        if (currentCellInfo == null) return null

        val cellId = DetectionAlgorithms.getCellIdFromCellInfo(currentCellInfo) ?: return null

        try {
            // 1. Advanced statistical signal strength analysis
            val signalAnomaly = MachineLearningDetector.detectAdvancedSignalAnomalies(
                currentCellInfo, knownTowers, cellId
            )

            // 2. Behavioral pattern analysis
            val behavioralPattern = MachineLearningDetector.detectBehavioralPatterns(
                recentEvents, currentCellInfo, knownTowers
            )

            // 3. Tower location validation
            val locationValidation = if (userLocation != null) {
                MachineLearningDetector.validateTowerLocation(
                    currentCellInfo, userLocation, knownTowers
                )
            } else null

            // 4. Traditional tower consistency check (for ensemble)
            val towerConsistency = DetectionAlgorithms.detectTowerInfoInconsistencies(
                currentCellInfo, knownTowers
            )

            // 5. Ensemble detection combining multiple methods
            return MachineLearningDetector.ensembleDetection(
                signalAnomaly,
                towerConsistency,
                behavioralPattern,
                locationValidation
            ) ?: signalAnomaly ?: behavioralPattern ?: locationValidation

        } catch (e: Exception) {
            android.util.Log.e("StingrayDetector", "Error in ML detection", e)
            return null
        }
    }

    /**
     * Get detection confidence score based on historical accuracy
     * Returns a value between 0.0 and 1.0 indicating confidence in detection
     */
    fun calculateDetectionConfidence(
        event: DetectionEvent,
        historicalEvents: List<DetectionEvent>
    ): Double {
        // Simple confidence calculation based on historical patterns
        val similarEvents = historicalEvents.filter {
            it.anomalyType == event.anomalyType &&
            it.cellId == event.cellId
        }

        if (similarEvents.isEmpty()) return 0.5 // Neutral confidence for new patterns

        // Calculate confidence based on consistency of threat levels
        val avgThreatLevel = similarEvents.map { it.threatLevel.severity }.average()
        val eventSeverity = event.threatLevel.severity

        // Higher confidence if event matches historical pattern
        val consistencyScore = 1.0 - (abs(eventSeverity - avgThreatLevel) / ThreatLevel.CRITICAL.severity.toDouble())

        // Boost confidence with more historical data
        val sampleSizeBoost = minOf(1.0, similarEvents.size / 10.0)

        return (consistencyScore * 0.7 + sampleSizeBoost * 0.3).coerceIn(0.0, 1.0)
    }

    // Extension property for threat level severity
    private val ThreatLevel.severity: Int
        get() = when (this) {
            ThreatLevel.NONE -> 0
            ThreatLevel.LOW -> 1
            ThreatLevel.MEDIUM -> 2
            ThreatLevel.HIGH -> 3
            ThreatLevel.CRITICAL -> 4
        }
}
