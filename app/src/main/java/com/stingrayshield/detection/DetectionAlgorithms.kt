package com.stingrayshield.detection

import android.telephony.CellInfo
import android.telephony.CellInfoCdma
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellInfoTdscdma
import android.telephony.CellInfoWcdma
import android.telephony.CellSignalStrengthCdma
import android.telephony.CellSignalStrengthGsm
import android.telephony.CellSignalStrengthLte
import android.telephony.CellSignalStrengthNr
import android.telephony.CellSignalStrengthTdscdma
import android.telephony.CellSignalStrengthWcdma
import android.telephony.TelephonyManager
import android.os.Build
import androidx.annotation.RequiresApi
import com.stingrayshield.domain.model.AnomalyType
import com.stingrayshield.domain.model.CellTower
import com.stingrayshield.domain.model.DetectionEvent
import com.stingrayshield.domain.model.ThreatLevel
import kotlin.math.abs

/**
 * Advanced detection algorithms for identifying IMSI-catchers (Stingray/IMSI catchers).
 *
 * Aligned with methods used by SDR scanners and open-source cellular monitors:
 * - **SnoopSnitch** (SRLabs): IMSI Catcher Score – LAC inconsistency (A2), encryption
 *   downgrade (C1), cipher mode complete delays (C2). C2 requires Qualcomm/root modem access.
 * - **AIMSICD** / **Android IMSI-Catcher Detector**: Silent SMS, tower consistency, neighbor analysis.
 * - **Research**: 3GPP-based checks, encryption downgrade (2G/3G/4G/5G), TAC/LAC consistency,
 *   signal anomalies, isolation from neighbors.
 *
 * Supports 2G (GSM), 3G (WCDMA/UMTS), 4G (LTE), 5G (NR), and CDMA. Some indicators (e.g. raw
 * cipher mode timing) require modem-level access not available on standard Android API.
 */
object DetectionAlgorithms {

    /**
     * Constants and thresholds for detection algorithms
     */
    private const val SIGNAL_STRENGTH_JUMP_THRESHOLD_DBM = 15
    private const val SIGNAL_STRENGTH_ANOMALY_THRESHOLD_DBM = 20
    private const val TIMING_ADVANCE_THRESHOLD = 5
    private const val RSRP_SUSPICIOUS_THRESHOLD = -70 // Suspiciously strong RSRP for LTE
    private const val RSRQ_SUSPICIOUS_THRESHOLD = -5  // Suspiciously high RSRQ for LTE
    private const val SINR_SUSPICIOUS_THRESHOLD = 20  // Suspiciously high SINR for LTE/NR
    private const val KNOWN_TOWER_DISTANCE_THRESHOLD = 300 // meters
    private const val CIPHERING_INDICATOR_DOWNGRADE_MS = 2000 // milliseconds

    /**
     * Analyze signal strength for sudden changes that might indicate stingray device
     * More sophisticated than the simple version in the service
     */
    fun detectSignalStrengthAnomalies(
        currentCellInfo: CellInfo?,
        previousCellInfo: CellInfo?,
        historicalData: List<CellTower>
    ): DetectionEvent? {
        if (currentCellInfo == null || previousCellInfo == null) return null
        
        // Get current and previous signal strengths
        val currentSignal = getSignalStrengthFromCellInfo(currentCellInfo)
        val previousSignal = getSignalStrengthFromCellInfo(previousCellInfo)
        
        if (currentSignal != null && previousSignal != null) {
            val currentCellId = getCellIdFromCellInfo(currentCellInfo)
            val previousCellId = getCellIdFromCellInfo(previousCellInfo)
            
            // If we're on the same cell and signal jumped significantly
            if (currentCellId == previousCellId && currentCellId != null) {
                val signalDelta = abs(currentSignal - previousSignal)
                
                if (signalDelta > SIGNAL_STRENGTH_JUMP_THRESHOLD_DBM) {
                    // Level 1: Basic sudden signal strength change
                    return DetectionEvent(
                        anomalyType = AnomalyType.SIGNAL_STRENGTH,
                        threatLevel = ThreatLevel.LOW,
                        description = "Sudden signal strength change detected: ${previousSignal}dBm to ${currentSignal}dBm (delta: ${signalDelta}dBm)",
                        cellId = currentCellId,
                        signalStrength = currentSignal
                    )
                }
                
                // Level 2: Stronger detection with historical context
                val historicalAverage = historicalData
                    .filter { it.cellId == currentCellId }
                    .map { it.signalStrength }
                    .takeIf { it.isNotEmpty() }
                    ?.average()
                
                if (historicalAverage != null) {
                    val deltaFromAverage = abs(currentSignal - historicalAverage)
                    if (deltaFromAverage > SIGNAL_STRENGTH_ANOMALY_THRESHOLD_DBM) {
                    return DetectionEvent(
                        anomalyType = AnomalyType.SIGNAL_STRENGTH,
                        threatLevel = ThreatLevel.MEDIUM,
                        description = "Abnormal signal strength: ${currentSignal}dBm differs from historical average (${historicalAverage.toInt()}dBm) by ${deltaFromAverage.toInt()}dBm. This could indicate a signal boosting attack typical of IMSI-catchers.",
                        cellId = currentCellId,
                        signalStrength = currentSignal
                    )
                    }
                }
            }
        }
        
        return null
    }

    /**
     * Detect 4G LTE-specific anomalies that may indicate stingray presence
     * Based on documented stingray capabilities and academic research
     */
    fun detectLteAnomalies(
        currentCellInfo: CellInfo?, 
        knownTowers: List<CellTower>
    ): DetectionEvent? {
        if (currentCellInfo !is CellInfoLte) return null
        
        val cellId = currentCellInfo.cellIdentity.ci
        val tac = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            currentCellInfo.cellIdentity.tac
        } else {
            0 // Not available on older Android versions
        }
        
        // Suspiciously strong signal with unusually high quality metrics
        // Often indicates a stingray positioned very close to target
        val rsrp = currentCellInfo.cellSignalStrength.rsrp
        val rsrq = currentCellInfo.cellSignalStrength.rsrq
        
        // Timing advance can reveal distance discrepancies 
        val timingAdvance = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            currentCellInfo.cellSignalStrength.timingAdvance
        } else {
            0 // Not available on older Android versions
        }
        
        // Known tower with significant timing advance discrepancy 
        // indicates possible cell tower spoofing
        // Note: Timing advance tracking would require extending CellTower model
        // For now, we detect based on signal strength and tower info mismatches
        
        // Suspiciously strong signal with perfect quality metrics
        // Stingrays often broadcast with artificially perfect signal parameters
        if (rsrp > RSRP_SUSPICIOUS_THRESHOLD && rsrq > RSRQ_SUSPICIOUS_THRESHOLD) {
            return DetectionEvent(
                anomalyType = AnomalyType.SIGNAL_STRENGTH,
                threatLevel = ThreatLevel.MEDIUM,
                description = "Suspiciously perfect signal quality detected. RSRP: $rsrp dBm, RSRQ: $rsrq dB. Unusually strong and clean signal may indicate a cell simulator.",
                cellId = cellId,
                signalStrength = getSignalStrengthFromCellInfo(currentCellInfo) ?: 0,
                locationAreaCode = tac ?: 0
            )
        }
        
        return null
    }
    
    /**
     * Detect 5G NR-specific anomalies
     * 5G stingrays are more advanced but still display certain patterns
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    fun detect5GAnomalies(
        currentCellInfo: CellInfo?,
        knownTowers: List<CellTower>
    ): DetectionEvent? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || currentCellInfo !is CellInfoNr) return null
        
        // Cast to access NR-specific properties
        val cellIdentityNr = currentCellInfo.cellIdentity as? android.telephony.CellIdentityNr
            ?: return null
        val signalStrengthNr = currentCellInfo.cellSignalStrength as? CellSignalStrengthNr
            ?: return null
        
        // The nci (NR Cell Identity) - use pci as fallback since nci may not be available
        val nrCellId = cellIdentityNr.pci.takeIf { it != Int.MAX_VALUE && it > 0 } ?: 0
        
        // Get TAC (Tracking Area Code)
        val tac = cellIdentityNr.tac.takeIf { it != Int.MAX_VALUE && it > 0 }
        
        // Get signal strength parameters
        val ssRsrp = signalStrengthNr.ssRsrp
        val ssRsrq = signalStrengthNr.ssRsrq
        val ssSinr = signalStrengthNr.ssSinr
        
        // 5G→4G forced downgrade is handled by detectNetworkDowngrade() in the detector pipeline
        // when lastNetworkType is tracked across scans.
        
        // Suspiciously perfect 5G signal 
        // 5G signals normally have more variability due to mmWave characteristics
        if (ssRsrp > -80 && ssRsrq > -10 && ssSinr > SINR_SUSPICIOUS_THRESHOLD) {
            return DetectionEvent(
                anomalyType = AnomalyType.SIGNAL_STRENGTH,
                threatLevel = ThreatLevel.MEDIUM,
                description = "Unusually perfect 5G signal quality detected. SS-RSRP: $ssRsrp dBm, SS-RSRQ: $ssRsrq dB, SS-SINR: $ssSinr dB. Unnaturally clean 5G signal may indicate a cell simulator.",
                cellId = nrCellId,
                signalStrength = ssRsrp
            )
        }
        
        return null
    }
    
    /**
     * Detect inconsistencies in cell tower information
     */
    fun detectTowerInfoInconsistencies(
        currentCellInfo: CellInfo?,
        knownTowers: List<CellTower>
    ): DetectionEvent? {
        if (currentCellInfo == null) return null
        
        val cellId = getCellIdFromCellInfo(currentCellInfo) ?: return null
        val mcc = getMccFromCellInfo(currentCellInfo) ?: return null
        val mnc = getMncFromCellInfo(currentCellInfo) ?: return null
        val lac = getLacFromCellInfo(currentCellInfo) ?: return null
        
        // Check for known tower with same cellId but different MCC/MNC/LAC
        // This indicates possible cell ID spoofing
        val matchingCellIdTowers = knownTowers.filter { it.cellId == cellId }
        for (tower in matchingCellIdTowers) {
            val mccMismatch = tower.mobileCountryCode != mcc.toString()
            val mncMismatch = tower.mobileNetworkCode != mnc.toString()
            val lacMismatch = tower.locationAreaCode != lac
            
            if (mccMismatch || mncMismatch || lacMismatch) {
                val description = buildString {
                    append("Cell tower info inconsistency detected: Cell ID $cellId has changed parameters:\n")
                    if (mccMismatch) append("MCC changed from ${tower.mobileCountryCode} to $mcc\n")
                    if (mncMismatch) append("MNC changed from ${tower.mobileNetworkCode} to $mnc\n") 
                    if (lacMismatch) append("LAC changed from ${tower.locationAreaCode} to $lac")
                }
                
                return DetectionEvent(
                    anomalyType = AnomalyType.TOWER_CONSISTENCY,
                    threatLevel = ThreatLevel.HIGH,
                    description = description,
                    cellId = cellId,
                    locationAreaCode = lac
                )
            }
        }
        
        return null
    }
    
    /**
     * Detect neighbor cell anomalies typical of stingray operation
     */
    fun detectNeighborCellAnomalies(
        currentCells: List<CellInfo>,
        previousNeighborCount: Int
    ): DetectionEvent? {
        // One key stingray indicator: sudden disappearance of neighbor cells
        // Stingrays often block or hide legitimate neighboring towers
        if (currentCells.size == 1 && previousNeighborCount > 2) {
            val cellId = getCellIdFromCellInfo(currentCells.first()) ?: 0
            
            return DetectionEvent(
                anomalyType = AnomalyType.NEIGHBOR_CELLS,
                threatLevel = ThreatLevel.HIGH,
                description = "Suspicious disappearance of neighboring cells. Device previously detected $previousNeighborCount neighboring cells, but now only sees one. This is a classic indicator of an IMSI-catcher taking over the connection.",
                cellId = cellId
            )
        }
        
        return null
    }
    
    /**
     * Detect silent SMS-based tracking
     * Stingrays often use silent SMS for tracking and triggering
     */
    fun detectSilentSms(recentSilentSmsCount: Int): DetectionEvent? {
        if (recentSilentSmsCount > 0) {
            return DetectionEvent(
                anomalyType = AnomalyType.SILENT_SMS,
                threatLevel = ThreatLevel.MEDIUM,
                description = "Silent SMS activity detected. Detected $recentSilentSmsCount silent SMS messages. These are invisible messages often used for device tracking and surveillance."
            )
        }
        return null
    }
    
    /**
     * Detect suspicious femtocells
     * Femtocells with unusual characteristics may be stingrays
     */
    fun detectSuspiciousFemtocell(
        cellInfo: CellInfo?,
        knownTowers: List<CellTower>
    ): DetectionEvent? {
        if (cellInfo == null) return null
        
        // In LTE networks, CSG (Closed Subscriber Group) ID can identify femtocells
        if (cellInfo is CellInfoLte && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val csgInfo = cellInfo.cellIdentity.closedSubscriberGroupInfo
            if (csgInfo != null) {
                val csgIndication = csgInfo.csgIndicator
                val signalStrength = getSignalStrengthFromCellInfo(cellInfo) ?: -100
                
                // Suspicious femtocell might have very strong signal but restrict access
                if (csgIndication && signalStrength > -70) {
                    return DetectionEvent(
                        anomalyType = AnomalyType.FEMTOCELL,
                        threatLevel = ThreatLevel.MEDIUM,
                        description = "Suspicious femtocell detected. Detected a femtocell with unusually strong signal (${signalStrength}dBm). This could be a legitimate femtocell or potentially a surveillance device.",
                        cellId = cellInfo.cellIdentity.ci
                    )
                }
            }
        }
        
        return null
    }
    
    /**
     * LAC/TAC inconsistency across visible cells (SnoopSnitch-style A2).
     * Serving cell LAC/TAC should be consistent with neighbors; inconsistency can indicate a fake tower.
     */
    fun detectLacInconsistency(allCells: List<CellInfo>): DetectionEvent? {
        if (allCells.size < 2) return null
        val serving = allCells.firstOrNull() ?: return null
        if (!serving.isRegistered) return null
        val servingLac = getLacFromCellInfo(serving) ?: return null
        val neighborLacs = allCells.filter { !it.isRegistered }.mapNotNull { getLacFromCellInfo(it) }.distinct()
        if (neighborLacs.isEmpty()) return null
        if (servingLac in neighborLacs) return null
        val cellId = getCellIdFromCellInfo(serving) ?: 0
        return DetectionEvent(
            anomalyType = AnomalyType.TOWER_CONSISTENCY,
            threatLevel = ThreatLevel.HIGH,
            description = "LAC/TAC inconsistency: serving cell LAC=$servingLac differs from all visible neighbors (${neighborLacs.joinToString()}). " +
                "Legitimate cells in the same area typically share LAC. This can indicate an IMSI-catcher (fake cell).",
            cellId = cellId,
            locationAreaCode = servingLac
        )
    }

    /**
     * Detect forced network downgrade (e.g. 5G→4G, 4G→3G) used to exploit weaker encryption.
     * Aligned with encryption-downgrade (C1) style detection used by SnoopSnitch/AIMSICD.
     */
    fun detectNetworkDowngrade(
        lastNetworkType: String?,
        currentCellInfo: CellInfo?
    ): DetectionEvent? {
        if (lastNetworkType.isNullOrBlank() || currentCellInfo == null) return null
        val currentType = getNetworkTypeFromCellInfo(currentCellInfo)
        val cellId = getCellIdFromCellInfo(currentCellInfo) ?: 0
        val downgrade = when {
            lastNetworkType == "5G" && currentType == "4G" ->
                "5G to 4G downgrade. Device was on 5G but connected to 4G. May indicate forced downgrade to exploit 4G vulnerabilities."
            lastNetworkType == "5G" && currentType in listOf("3G", "2G") ->
                "5G to $currentType downgrade. Strong indicator of IMSI-catcher forcing legacy connection."
            lastNetworkType == "4G" && currentType == "3G" ->
                "4G to 3G downgrade. Could indicate stingray forcing 3G for interception."
            lastNetworkType == "4G" && currentType in listOf("2G", "GSM") ->
                "4G to 2G downgrade. Critical: 2G has no encryption. Classic IMSI-catcher pattern."
            else -> null
        }
        return downgrade?.let {
            DetectionEvent(
                anomalyType = AnomalyType.ENCRYPTION_CHANGE,
                threatLevel = if (currentType in listOf("2G", "GSM")) ThreatLevel.CRITICAL else ThreatLevel.HIGH,
                description = "Possible forced network downgrade: $it",
                cellId = cellId
            )
        }
    }

    /**
     * Detect encryption downgrades in cellular connection (C1-style).
     * Cipher mode complete delay (C2) typically requires Qualcomm modem/root; not available on standard API.
     */
    fun detectEncryptionDowngrade(
        currentCipheringIndicator: Int,
        previousCipheringIndicator: Int,
        timeElapsedMs: Long
    ): DetectionEvent? {
        // Quick downgrade is suspicious
        if (previousCipheringIndicator > currentCipheringIndicator && 
            timeElapsedMs < CIPHERING_INDICATOR_DOWNGRADE_MS) {
            
            return DetectionEvent(
                anomalyType = AnomalyType.ENCRYPTION_CHANGE,
                threatLevel = ThreatLevel.HIGH,
                description = "Encryption downgrade detected. Cellular connection encryption was downgraded from level $previousCipheringIndicator to $currentCipheringIndicator in ${timeElapsedMs}ms. This may indicate a forced downgrade attack by an IMSI-catcher."
            )
        }
        
        return null
    }

    /**
     * Helper methods to extract information from CellInfo objects
     * Supports all US network types: GSM, CDMA, WCDMA, TDSCDMA, LTE, and 5G NR
     */
    fun getSignalStrengthFromCellInfo(cellInfo: CellInfo): Int? {
        return when {
            // Handle LTE cells (4G - AT&T, T-Mobile, Verizon, etc.)
            cellInfo is CellInfoLte -> cellInfo.cellSignalStrength.dbm
            
            // Handle 5G NR cells (Android Q+)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && cellInfo is CellInfoNr -> 
                cellInfo.cellSignalStrength.dbm
            
            // Handle CDMA cells (Verizon, US Cellular, Sprint legacy)
            cellInfo is CellInfoCdma -> cellInfo.cellSignalStrength.dbm
            
            // Handle WCDMA/UMTS cells (3G - AT&T, T-Mobile)
            cellInfo is CellInfoWcdma -> cellInfo.cellSignalStrength.dbm
            
            // Handle TD-SCDMA cells (3G variant, some international carriers)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && cellInfo is CellInfoTdscdma ->
                cellInfo.cellSignalStrength.dbm
            
            // Handle GSM cells (2G - fallback networks)
            cellInfo is CellInfoGsm -> cellInfo.cellSignalStrength.dbm
            
            // Other types
            else -> null
        }
    }

    fun getCellIdFromCellInfo(cellInfo: CellInfo): Int? {
        return when {
            // LTE Cell ID (eNB + sector)
            cellInfo is CellInfoLte -> cellInfo.cellIdentity.ci.takeIf { it != Int.MAX_VALUE && it > 0 }
            
            // 5G NR Cell ID (Android Q+) - use PCI since nci requires higher API
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && cellInfo is CellInfoNr -> {
                val identity = cellInfo.cellIdentity as? android.telephony.CellIdentityNr
                identity?.pci?.takeIf { it != Int.MAX_VALUE && it > 0 }
            }
            
            // CDMA Base Station ID (Verizon)
            cellInfo is CellInfoCdma -> cellInfo.cellIdentity.basestationId.takeIf { it != Int.MAX_VALUE && it > 0 }
            
            // WCDMA/UMTS Cell ID
            cellInfo is CellInfoWcdma -> cellInfo.cellIdentity.cid.takeIf { it != Int.MAX_VALUE && it > 0 }
            
            // TD-SCDMA Cell ID (Android Q+)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && cellInfo is CellInfoTdscdma ->
                cellInfo.cellIdentity.cid.takeIf { it != Int.MAX_VALUE && it > 0 }
            
            // GSM/2G Cell ID
            cellInfo is CellInfoGsm -> cellInfo.cellIdentity.cid.takeIf { it != Int.MAX_VALUE && it > 0 }
            
            // Other types
            else -> null
        }
    }

    fun getMccFromCellInfo(cellInfo: CellInfo): Int? {
        return when {
            // LTE MCC
            cellInfo is CellInfoLte -> cellInfo.cellIdentity.mccString?.toIntOrNull()
            
            // 5G NR MCC (Android Q+)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && cellInfo is CellInfoNr -> {
                val identity = cellInfo.cellIdentity as? android.telephony.CellIdentityNr
                identity?.mccString?.toIntOrNull()
            }
            
            // CDMA doesn't have MCC in the same way - derive from SID
            // US carriers use SID ranges that can be mapped to MCC 310/311
            cellInfo is CellInfoCdma -> 310 // Default to US MCC for CDMA
            
            // WCDMA/3G MCC
            cellInfo is CellInfoWcdma -> cellInfo.cellIdentity.mccString?.toIntOrNull()
            
            // TD-SCDMA MCC (Android Q+)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && cellInfo is CellInfoTdscdma ->
                cellInfo.cellIdentity.mccString?.toIntOrNull()
            
            // GSM/2G MCC
            cellInfo is CellInfoGsm -> cellInfo.cellIdentity.mccString?.toIntOrNull()
            
            // Other types
            else -> null
        }
    }

    fun getMncFromCellInfo(cellInfo: CellInfo): Int? {
        return when {
            // LTE MNC
            cellInfo is CellInfoLte -> cellInfo.cellIdentity.mncString?.toIntOrNull()
            
            // 5G NR MNC (Android Q+)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && cellInfo is CellInfoNr -> {
                val identity = cellInfo.cellIdentity as? android.telephony.CellIdentityNr
                identity?.mncString?.toIntOrNull()
            }
            
            // CDMA uses System ID (SID) instead of MNC
            cellInfo is CellInfoCdma -> cellInfo.cellIdentity.systemId.takeIf { it > 0 }
            
            // WCDMA/3G MNC
            cellInfo is CellInfoWcdma -> cellInfo.cellIdentity.mncString?.toIntOrNull()
            
            // TD-SCDMA MNC (Android Q+)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && cellInfo is CellInfoTdscdma ->
                cellInfo.cellIdentity.mncString?.toIntOrNull()
            
            // GSM/2G MNC
            cellInfo is CellInfoGsm -> cellInfo.cellIdentity.mncString?.toIntOrNull()
            
            // Other types
            else -> null
        }
    }

    fun getLacFromCellInfo(cellInfo: CellInfo): Int? {
        return when {
            // LTE TAC (Tracking Area Code - equivalent to LAC)
            cellInfo is CellInfoLte && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> 
                cellInfo.cellIdentity.tac.takeIf { it != Int.MAX_VALUE && it > 0 }
            
            // 5G NR TAC (Android Q+)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && cellInfo is CellInfoNr -> {
                val identity = cellInfo.cellIdentity as? android.telephony.CellIdentityNr
                identity?.tac?.takeIf { it != Int.MAX_VALUE && it > 0 }
            }
            
            // CDMA uses Network ID instead of LAC
            cellInfo is CellInfoCdma -> cellInfo.cellIdentity.networkId.takeIf { it > 0 }
            
            // WCDMA/3G LAC
            cellInfo is CellInfoWcdma -> cellInfo.cellIdentity.lac.takeIf { it != Int.MAX_VALUE && it > 0 }
            
            // TD-SCDMA LAC (Android Q+)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && cellInfo is CellInfoTdscdma ->
                cellInfo.cellIdentity.lac.takeIf { it != Int.MAX_VALUE && it > 0 }
            
            // GSM/2G LAC
            cellInfo is CellInfoGsm -> cellInfo.cellIdentity.lac.takeIf { it != Int.MAX_VALUE && it > 0 }
            
            // Other types
            else -> null
        }
    }
    
    /**
     * Get network type string from CellInfo
     * Returns a human-readable network generation (2G/3G/4G/5G)
     */
    fun getNetworkTypeFromCellInfo(cellInfo: CellInfo): String {
        return when {
            // 5G NR
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && cellInfo is CellInfoNr -> "5G"
            
            // 4G LTE
            cellInfo is CellInfoLte -> "4G"
            
            // 3G variants
            cellInfo is CellInfoWcdma -> "3G"
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && cellInfo is CellInfoTdscdma -> "3G"
            
            // CDMA (can be 2G or 3G depending on EVDO)
            cellInfo is CellInfoCdma -> "CDMA"
            
            // 2G GSM
            cellInfo is CellInfoGsm -> "2G"
            
            else -> "Unknown"
        }
    }
    
    /**
     * Get carrier name hint from MCC/MNC combination
     * Helps identify if the network is from a legitimate US carrier
     */
    fun getCarrierHint(mcc: Int?, mnc: Int?): String {
        if (mcc == null || mnc == null) return "Unknown"
        
        // US MCC codes are 310, 311, 312, 313, 316
        return when {
            mcc == 310 -> when (mnc) {
                12, 13, 450, 680, 980, 990 -> "Verizon"
                26, 160, 170, 260, 270, 310, 330, 560, 670, 800 -> "T-Mobile"
                150, 170, 380, 410, 560 -> "AT&T"
                120, 430 -> "Sprint"
                else -> "US Carrier ($mnc)"
            }
            mcc == 311 -> when (mnc) {
                12, 110, 270, 271, 272, 273, 274, 275, 276, 277, 278, 279, 280, 281, 282, 283, 284, 285, 286, 287, 288, 289, 480, 481, 482, 483, 484, 485, 486, 487, 488, 489 -> "Verizon"
                180 -> "AT&T"
                490 -> "T-Mobile"
                580 -> "US Cellular"
                else -> "US Carrier ($mnc)"
            }
            mcc == 312 -> "US Regional Carrier"
            mcc == 313 -> "US Carrier"
            mcc == 316 -> "US Carrier"
            else -> "International ($mcc)"
        }
    }
    
    /**
     * Detect 2G GSM-specific anomalies
     * 2G networks are particularly vulnerable to stingray attacks due to weak encryption
     */
    fun detect2GAnomalies(
        currentCellInfo: CellInfo?,
        knownTowers: List<CellTower>
    ): DetectionEvent? {
        if (currentCellInfo !is CellInfoGsm) return null
        
        val cellId = currentCellInfo.cellIdentity.cid.takeIf { it != Int.MAX_VALUE && it > 0 } ?: return null
        val lac = currentCellInfo.cellIdentity.lac.takeIf { it != Int.MAX_VALUE && it > 0 }
        val mcc = currentCellInfo.cellIdentity.mccString?.toIntOrNull()
        val mnc = currentCellInfo.cellIdentity.mncString?.toIntOrNull()
        
        // Get signal strength
        val signalStrength = currentCellInfo.cellSignalStrength.dbm
        
        // 2G-specific detection patterns:
        
        // 1. Suspiciously strong 2G signal when device should be on 3G/4G/5G
        // Stingrays often force devices to 2G and broadcast strong signals
        if (signalStrength > -70) {
                return DetectionEvent(
                    anomalyType = AnomalyType.SIGNAL_STRENGTH,
                    threatLevel = ThreatLevel.HIGH,
                    description = "Unusually strong 2G signal detected. Signal strength: ${signalStrength}dBm. Strong 2G signals are suspicious, especially if your device should be on a newer network generation. This may indicate a stingray forcing a 2G connection.",
                    cellId = cellId,
                    signalStrength = signalStrength,
                    locationAreaCode = lac ?: 0
                )
        }
        
        // 2. Check for known tower with different parameters (spoofing)
        val knownTower = knownTowers.find { it.cellId == cellId }
        if (knownTower != null) {
            val mccMismatch = mcc != null && knownTower.mobileCountryCode != mcc.toString()
            val mncMismatch = mnc != null && knownTower.mobileNetworkCode != mnc.toString()
            val lacMismatch = lac != null && knownTower.locationAreaCode != lac
            
            if (mccMismatch || mncMismatch || lacMismatch) {
                return DetectionEvent(
                    anomalyType = AnomalyType.TOWER_CONSISTENCY,
                    threatLevel = ThreatLevel.HIGH,
                    description = "2G cell tower parameter mismatch detected: Cell ID $cellId shows different MCC/MNC/LAC than previously recorded. This is a strong indicator of cell tower spoofing.",
                    cellId = cellId,
                    locationAreaCode = lac ?: 0
                )
            }
        }
        
        // 3. Check for ARFCN (Absolute Radio Frequency Channel Number) anomalies
        // Stingrays may use unusual ARFCN values
        val arfcn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            currentCellInfo.cellIdentity.arfcn
        } else {
            0
        }
        
        // ARFCN values outside normal ranges can be suspicious
        // Normal GSM ARFCN range is typically 0-1023 for 900MHz, 512-885 for 1800MHz
        if (arfcn > 0 && (arfcn > 1023 || (arfcn < 512 && arfcn > 0))) {
            return DetectionEvent(
                anomalyType = AnomalyType.TOWER_CONSISTENCY,
                threatLevel = ThreatLevel.MEDIUM,
                description = "Unusual 2G ARFCN detected. ARFCN value $arfcn is outside normal ranges. This could indicate a non-standard cell tower or stingray device.",
                cellId = cellId
            )
        }
        
        return null
    }
    
    /**
     * Detect 3G WCDMA/UMTS-specific anomalies
     * 3G networks have better security than 2G but are still vulnerable
     */
    fun detect3GAnomalies(
        currentCellInfo: CellInfo?,
        knownTowers: List<CellTower>
    ): DetectionEvent? {
        if (currentCellInfo !is CellInfoWcdma) return null
        
        val cellId = currentCellInfo.cellIdentity.cid.takeIf { it != Int.MAX_VALUE && it > 0 } ?: return null
        val lac = currentCellInfo.cellIdentity.lac.takeIf { it != Int.MAX_VALUE && it > 0 }
        val mcc = currentCellInfo.cellIdentity.mccString?.toIntOrNull()
        val mnc = currentCellInfo.cellIdentity.mncString?.toIntOrNull()
        
        // Get signal strength
        val signalStrength = currentCellInfo.cellSignalStrength.dbm
        
        // 3G-specific detection patterns:
        
        // 1. Suspiciously strong 3G signal when device should be on 4G/5G
        // Similar to 2G, stingrays may force downgrade to 3G
        if (signalStrength > -75) {
                return DetectionEvent(
                    anomalyType = AnomalyType.SIGNAL_STRENGTH,
                    threatLevel = ThreatLevel.MEDIUM,
                    description = "Unusually strong 3G signal detected. Signal strength: ${signalStrength}dBm. Strong 3G signals are suspicious if your device should be on 4G or 5G. This may indicate a stingray forcing a 3G connection.",
                    cellId = cellId,
                    signalStrength = signalStrength,
                    locationAreaCode = lac ?: 0
                )
        }
        
        // 2. Check for known tower with different parameters
        val knownTower = knownTowers.find { it.cellId == cellId }
        if (knownTower != null) {
            val mccMismatch = mcc != null && knownTower.mobileCountryCode != mcc.toString()
            val mncMismatch = mnc != null && knownTower.mobileNetworkCode != mnc.toString()
            val lacMismatch = lac != null && knownTower.locationAreaCode != lac
            
            if (mccMismatch || mncMismatch || lacMismatch) {
                return DetectionEvent(
                    anomalyType = AnomalyType.TOWER_CONSISTENCY,
                    threatLevel = ThreatLevel.HIGH,
                    description = "3G cell tower parameter mismatch detected: Cell ID $cellId shows different MCC/MNC/LAC than previously recorded. This indicates possible cell tower spoofing.",
                    cellId = cellId,
                    locationAreaCode = lac ?: 0
                )
            }
        }
        
        // 3. Check for UARFCN (UTRA Absolute Radio Frequency Channel Number) anomalies
        // Unusual UARFCN values can indicate non-standard equipment
        val uarfcn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            currentCellInfo.cellIdentity.uarfcn
        } else {
            0
        }
        
        // Normal UARFCN ranges: 900MHz band (2937-3088), 2100MHz band (10562-10838)
        // Values significantly outside these ranges are suspicious
        if (uarfcn > 0 && (uarfcn < 2937 || uarfcn > 10838)) {
            return DetectionEvent(
                anomalyType = AnomalyType.TOWER_CONSISTENCY,
                threatLevel = ThreatLevel.MEDIUM,
                description = "Unusual 3G UARFCN detected. UARFCN value $uarfcn is outside normal ranges. This could indicate a non-standard cell tower or stingray device.",
                cellId = cellId
            )
        }
        
        // 4. Check for PSC (Primary Scrambling Code) anomalies
        // PSC values that change unexpectedly can indicate spoofing
        // Note: PSC tracking would require extending CellTower model
        // For now, we detect based on signal strength and tower info mismatches
        
        return null
    }
    
    /**
     * Detect CDMA-specific anomalies (Verizon, US Cellular, Sprint legacy)
     * CDMA networks use different identifiers: SID (System ID), NID (Network ID), BSID (Base Station ID)
     */
    fun detectCdmaAnomalies(
        currentCellInfo: CellInfo?,
        knownTowers: List<CellTower>
    ): DetectionEvent? {
        if (currentCellInfo !is CellInfoCdma) return null
        
        val baseStationId = currentCellInfo.cellIdentity.basestationId.takeIf { it != Int.MAX_VALUE && it > 0 } ?: return null
        val systemId = currentCellInfo.cellIdentity.systemId.takeIf { it != Int.MAX_VALUE && it > 0 }
        val networkId = currentCellInfo.cellIdentity.networkId.takeIf { it != Int.MAX_VALUE && it > 0 }
        
        // Get signal strength (CDMA uses multiple signal indicators)
        val cdmaDbm = currentCellInfo.cellSignalStrength.cdmaDbm
        val evdoDbm = currentCellInfo.cellSignalStrength.evdoDbm
        val signalStrength = minOf(cdmaDbm, evdoDbm) // Use the stronger signal
        
        // CDMA-specific detection patterns:
        
        // 1. Suspiciously strong CDMA signal
        // Stingrays often broadcast very strong signals to force device connection
        if (signalStrength > -65) {
            return DetectionEvent(
                anomalyType = AnomalyType.SIGNAL_STRENGTH,
                threatLevel = ThreatLevel.HIGH,
                description = "Unusually strong CDMA signal detected. Signal: ${signalStrength}dBm (CDMA: ${cdmaDbm}dBm, EVDO: ${evdoDbm}dBm). Very strong CDMA signals may indicate a cell simulator targeting Verizon/CDMA devices.",
                cellId = baseStationId,
                signalStrength = signalStrength,
                locationAreaCode = networkId ?: 0
            )
        }
        
        // 2. Check for invalid or suspicious System ID (SID)
        // Valid US CDMA SIDs are typically in ranges assigned to specific carriers
        // Verizon SIDs: 2-6000+ range, Sprint: different ranges
        if (systemId != null && (systemId < 1 || systemId > 32767)) {
            return DetectionEvent(
                anomalyType = AnomalyType.TOWER_CONSISTENCY,
                threatLevel = ThreatLevel.HIGH,
                description = "Invalid CDMA System ID detected. SID: $systemId is outside valid ranges. This may indicate a spoofed CDMA cell tower.",
                cellId = baseStationId,
                locationAreaCode = networkId ?: 0
            )
        }
        
        // 3. Check for known tower with different parameters
        val knownTower = knownTowers.find { it.cellId == baseStationId }
        if (knownTower != null) {
            val sidMismatch = systemId != null && knownTower.mobileNetworkCode != systemId.toString()
            val nidMismatch = networkId != null && knownTower.locationAreaCode != networkId
            
            if (sidMismatch || nidMismatch) {
                return DetectionEvent(
                    anomalyType = AnomalyType.TOWER_CONSISTENCY,
                    threatLevel = ThreatLevel.HIGH,
                    description = "CDMA cell tower parameter mismatch. Base Station $baseStationId has different SID/NID than recorded. SID mismatch: $sidMismatch, NID mismatch: $nidMismatch. This indicates possible tower spoofing.",
                    cellId = baseStationId,
                    locationAreaCode = networkId ?: 0
                )
            }
        }
        
        // 4. Check for EVDO vs 1xRTT discrepancy
        // If CDMA signal is strong but EVDO is missing/weak, may indicate older stingray
        if (cdmaDbm > -80 && evdoDbm < -100) {
            return DetectionEvent(
                anomalyType = AnomalyType.SIGNAL_STRENGTH,
                threatLevel = ThreatLevel.MEDIUM,
                description = "CDMA/EVDO signal discrepancy detected. Strong 1xRTT (${cdmaDbm}dBm) but weak EVDO (${evdoDbm}dBm). Older stingray devices may not properly simulate EVDO signals.",
                cellId = baseStationId,
                signalStrength = signalStrength
            )
        }
        
        // 5. Check base station latitude/longitude if available
        // CDMA provides rough lat/long - large discrepancies from device location are suspicious
        val bsLat = currentCellInfo.cellIdentity.latitude
        val bsLong = currentCellInfo.cellIdentity.longitude
        if (bsLat != Int.MAX_VALUE && bsLong != Int.MAX_VALUE) {
            // Convert from CDMA format (degrees * 3600 * 4) to decimal degrees
            val latDegrees = bsLat / (3600.0 * 4.0)
            val longDegrees = bsLong / (3600.0 * 4.0)
            
            // If coordinates are clearly invalid (outside US continental bounds)
            if (latDegrees < 24 || latDegrees > 50 || longDegrees < -125 || longDegrees > -66) {
                return DetectionEvent(
                    anomalyType = AnomalyType.LOCATION_TRACKING,
                    threatLevel = ThreatLevel.MEDIUM,
                    description = "CDMA base station reports invalid location. Reported coordinates ($latDegrees, $longDegrees) are outside continental US bounds. This may indicate a misconfigured or spoofed tower.",
                    cellId = baseStationId
                )
            }
        }
        
        return null
    }
    
}
