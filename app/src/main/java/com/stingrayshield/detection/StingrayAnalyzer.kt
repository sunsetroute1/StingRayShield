package com.stingrayshield.detection

import android.content.Context
import android.telephony.CellInfo
import android.telephony.CellInfoCdma
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellInfoWcdma
import android.telephony.TelephonyManager
import com.stingrayshield.domain.model.CellTower
import com.stingrayshield.domain.model.ThreatLevel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stingray/IMSI Catcher Detection Engine
 *
 * Uses state-of-the-art methods aligned with SDR scanners and open-source cellular monitors:
 * - **SnoopSnitch (SRLabs)**: LAC inconsistency (A2), encryption downgrade (C1). C2 (cipher
 *   mode delay) requires Qualcomm/root and is handled in the detector pipeline when available.
 * - **AIMSICD / Android IMSI-Catcher Detector**: Neighbor isolation, tower consistency, silent SMS.
 * - **3G/4G/5G**: Encryption downgrade (2G/3G/4G/5G), TAC/LAC consistency, signal anomalies,
 *   same LAC across cells, rapid handoffs.
 *
 * Detection methods:
 * 1. Signal strength anomalies (abnormally strong = suspicious)
 * 2. Encryption/network downgrade (2G when 4G/5G available; 3G when 4G; 4G when 5G)
 * 3. Cell ID validity and consistency
 * 4. LAC/TAC consistency (single tower and across serving vs neighbors)
 * 5. Neighbor cell isolation (no/few neighbors = strong indicator)
 * 6. Rapid tower/handoff changes
 * 7. MCC/MNC validity for region
 * 8. New or changed tower baselines
 */
@Singleton
class StingrayAnalyzer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Known legitimate towers (built over time)
    private val knownTowers = mutableMapOf<String, TowerBaseline>()
    
    // Recent tower observations for pattern analysis
    private val recentObservations = mutableListOf<TowerObservation>()
    
    // Expected MCC for US carriers
    private val validUSMCCs = setOf("310", "311", "312", "313", "314", "316")
    
    // Known carrier MNCs (partial list)
    private val knownCarriers = mapOf(
        "310-260" to "T-Mobile",
        "310-410" to "AT&T",
        "311-480" to "Verizon",
        "310-120" to "Sprint",
        "311-490" to "T-Mobile",
        "310-150" to "AT&T",
        "310-380" to "AT&T",
        "310-280" to "AT&T",
        "310-170" to "AT&T",
        "312-530" to "Sprint",
        "310-004" to "Verizon",
        "310-010" to "Verizon",
        "310-012" to "Verizon"
    )
    
    /**
     * Analyze a cell tower for stingray indicators
     * Returns a detailed threat assessment
     */
    fun analyzeTower(tower: CellTower, allCellInfo: List<CellInfo>): ThreatAssessment {
        val indicators = mutableListOf<ThreatIndicator>()
        var totalScore = 0
        
        // 1. Signal Strength Analysis
        val signalResult = analyzeSignalStrength(tower)
        if (signalResult.score > 0) {
            indicators.add(signalResult)
            totalScore += signalResult.score
        }
        
        // 2. Encryption/Network Type Analysis (C1-style downgrade)
        val encryptionResult = analyzeEncryption(tower, allCellInfo)
        if (encryptionResult.score > 0) {
            indicators.add(encryptionResult)
            totalScore += encryptionResult.score
        }
        // 2b. LAC/TAC inconsistency across cells (SnoopSnitch A2-style)
        val lacAcrossResult = analyzeLacAcrossCells(tower, allCellInfo)
        if (lacAcrossResult.score > 0) {
            indicators.add(lacAcrossResult)
            totalScore += lacAcrossResult.score
        }
        // 3. MCC/MNC Validity Check
        val mccMncResult = analyzeMccMnc(tower)
        if (mccMncResult.score > 0) {
            indicators.add(mccMncResult)
            totalScore += mccMncResult.score
        }
        
        // 4. Neighbor Cell Analysis
        val neighborResult = analyzeNeighborCells(tower, allCellInfo)
        if (neighborResult.score > 0) {
            indicators.add(neighborResult)
            totalScore += neighborResult.score
        }
        
        // 5. Historical Comparison
        val historyResult = analyzeHistory(tower)
        if (historyResult.score > 0) {
            indicators.add(historyResult)
            totalScore += historyResult.score
        }
        
        // 6. Cell ID Validity
        val cellIdResult = analyzeCellId(tower)
        if (cellIdResult.score > 0) {
            indicators.add(cellIdResult)
            totalScore += cellIdResult.score
        }
        
        // 7. LAC Consistency
        val lacResult = analyzeLac(tower)
        if (lacResult.score > 0) {
            indicators.add(lacResult)
            totalScore += lacResult.score
        }
        
        // 8. Rapid Change Detection
        val rapidChangeResult = analyzeRapidChanges(tower)
        if (rapidChangeResult.score > 0) {
            indicators.add(rapidChangeResult)
            totalScore += rapidChangeResult.score
        }
        
        // Record this observation
        recordObservation(tower)
        
        // Determine threat level based on total score
        val threatLevel = when {
            totalScore >= 80 -> ThreatLevel.CRITICAL
            totalScore >= 60 -> ThreatLevel.HIGH
            totalScore >= 40 -> ThreatLevel.MEDIUM
            totalScore >= 20 -> ThreatLevel.LOW
            else -> ThreatLevel.NONE
        }
        
        return ThreatAssessment(
            tower = tower,
            threatLevel = threatLevel,
            totalScore = totalScore,
            indicators = indicators,
            recommendation = getRecommendation(threatLevel, indicators)
        )
    }
    
    /**
     * Signal strength analysis
     * Stingrays often broadcast at unusually high power to force connections
     */
    private fun analyzeSignalStrength(tower: CellTower): ThreatIndicator {
        val signal = tower.signalStrength
        
        // Normal ranges by network type
        val (normalMin, normalMax, suspiciousThreshold) = when (tower.networkType) {
            "LTE", "4G" -> Triple(-120, -80, -60)
            "5G" -> Triple(-110, -70, -50)
            "3G", "WCDMA" -> Triple(-110, -75, -55)
            "2G", "GSM" -> Triple(-110, -70, -50)
            else -> Triple(-120, -70, -50)
        }
        
        return when {
            signal > suspiciousThreshold -> ThreatIndicator(
                name = "ABNORMALLY_STRONG_SIGNAL",
                description = "Signal strength $signal dBm is suspiciously high for ${tower.networkType}. " +
                        "Normal range: $normalMin to $normalMax dBm. Stingrays often broadcast at high power.",
                score = 30,
                severity = "HIGH"
            )
            signal > normalMax -> ThreatIndicator(
                name = "STRONG_SIGNAL",
                description = "Signal $signal dBm is stronger than typical. Could indicate proximity to tower OR a stingray.",
                score = 10,
                severity = "LOW"
            )
            else -> ThreatIndicator("SIGNAL_NORMAL", "Signal strength is within normal range", 0, "NONE")
        }
    }
    
    /**
     * Encryption/Network downgrade analysis
     * Stingrays often force 2G connections to intercept unencrypted traffic
     */
    private fun analyzeEncryption(tower: CellTower, allCells: List<CellInfo>): ThreatIndicator {
        // Check if we're on 2G when stronger 4G/5G is available
        if (tower.networkType in listOf("2G", "GSM") && tower.isServingCell) {
            val has4GNeighbor = allCells.any { it is CellInfoLte && !it.isRegistered }
            val has5GNeighbor = allCells.any { it is CellInfoNr && !it.isRegistered }
            
            if (has4GNeighbor || has5GNeighbor) {
                return ThreatIndicator(
                    name = "ENCRYPTION_DOWNGRADE",
                    description = "⚠️ CRITICAL: Connected to 2G (no encryption) while 4G/5G towers are available. " +
                            "This is a classic stingray attack pattern to intercept calls and texts.",
                    score = 50,
                    severity = "CRITICAL"
                )
            }
        }
        
        // Check for 3G when 4G available (less severe but still suspicious)
        if (tower.networkType in listOf("3G", "WCDMA") && tower.isServingCell) {
            val has4GNeighbor = allCells.any { it is CellInfoLte && !it.isRegistered }
            if (has4GNeighbor) {
                return ThreatIndicator(
                    name = "NETWORK_DOWNGRADE",
                    description = "Connected to 3G while 4G towers are visible. Could indicate a downgrade attack.",
                    score = 15,
                    severity = "MEDIUM"
                )
            }
        }
        
        // 4G when 5G available (downgrade to exploit 4G)
        if (tower.networkType in listOf("LTE", "4G") && tower.isServingCell) {
            val has5GNeighbor = allCells.any { it is CellInfoNr && !it.isRegistered }
            if (has5GNeighbor) {
                return ThreatIndicator(
                    name = "FOUR_G_WHEN_FIVE_G",
                    description = "Connected to 4G while 5G towers are visible. May indicate forced downgrade.",
                    score = 20,
                    severity = "MEDIUM"
                )
            }
        }
        
        return ThreatIndicator("ENCRYPTION_OK", "Network encryption appears normal", 0, "NONE")
    }
    
    /**
     * LAC/TAC inconsistency: serving cell LAC vs neighbor LACs (SnoopSnitch A2-style).
     * Legitimate cells in the same area typically share LAC; fake towers often don't.
     */
    private fun analyzeLacAcrossCells(tower: CellTower, allCells: List<CellInfo>): ThreatIndicator {
        if (!tower.isServingCell || allCells.size < 2) return ThreatIndicator("LAC_ACROSS_OK", "", 0, "NONE")
        val servingLac = tower.locationAreaCode
        val neighborLacs = allCells.filter { !it.isRegistered }.mapNotNull { info ->
            when (info) {
                is CellInfoLte -> if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) info.cellIdentity.tac.takeIf { it != Int.MAX_VALUE && it > 0 } else null
                is CellInfoGsm -> info.cellIdentity.lac.takeIf { it != Int.MAX_VALUE && it > 0 }
                is CellInfoWcdma -> info.cellIdentity.lac.takeIf { it != Int.MAX_VALUE && it > 0 }
                is CellInfoNr -> (info.cellIdentity as? android.telephony.CellIdentityNr)?.tac?.takeIf { it != Int.MAX_VALUE && it > 0 }
                else -> null
            }
        }.distinct()
        if (neighborLacs.isEmpty()) return ThreatIndicator("LAC_ACROSS_OK", "", 0, "NONE")
        if (servingLac in neighborLacs) return ThreatIndicator("LAC_ACROSS_OK", "", 0, "NONE")
        return ThreatIndicator(
            name = "LAC_INCONSISTENT",
            description = "Serving cell LAC $servingLac does not match any neighbor LAC ($neighborLacs). Legitimate cells in the same area share LAC. Strong indicator of fake cell.",
            score = 40,
            severity = "HIGH"
        )
    }
    
    /**
     * MCC/MNC validation
     * Invalid or mismatched codes indicate fake towers
     */
    private fun analyzeMccMnc(tower: CellTower): ThreatIndicator {
        val mcc = tower.mobileCountryCode
        val mnc = tower.mobileNetworkCode
        val combo = "$mcc-$mnc"
        
        // Check for empty/invalid MCC
        if (mcc.isBlank() || mcc == "0" || mcc == "Unknown") {
            return ThreatIndicator(
                name = "MISSING_MCC",
                description = "Tower has no Mobile Country Code. Legitimate towers always broadcast MCC.",
                score = 40,
                severity = "HIGH"
            )
        }
        
        // Check if MCC is valid for US
        if (mcc !in validUSMCCs && mcc.length == 3) {
            return ThreatIndicator(
                name = "FOREIGN_MCC",
                description = "MCC $mcc is not a US mobile country code. In the US, you should see 310-316.",
                score = 35,
                severity = "HIGH"
            )
        }
        
        // Check against known carriers
        if (combo !in knownCarriers && mnc.isNotBlank() && mnc != "Unknown") {
            return ThreatIndicator(
                name = "UNKNOWN_CARRIER",
                description = "MCC/MNC combination $combo is not a recognized US carrier.",
                score = 20,
                severity = "MEDIUM"
            )
        }
        
        return ThreatIndicator("MCC_MNC_VALID", "Carrier codes appear legitimate", 0, "NONE")
    }
    
    /**
     * Neighbor cell analysis
     * Legitimate towers report nearby cells; stingrays often don't
     */
    private fun analyzeNeighborCells(tower: CellTower, allCells: List<CellInfo>): ThreatIndicator {
        if (!tower.isServingCell) return ThreatIndicator("NOT_SERVING", "", 0, "NONE")
        
        val neighborCount = allCells.count { !it.isRegistered }
        
        return when {
            neighborCount == 0 -> ThreatIndicator(
                name = "NO_NEIGHBORS",
                description = "⚠️ This tower reports NO neighboring cells. Legitimate towers always see neighbors. " +
                        "This is a strong indicator of a stingray.",
                score = 35,
                severity = "HIGH"
            )
            neighborCount == 1 -> ThreatIndicator(
                name = "FEW_NEIGHBORS",
                description = "Only 1 neighbor cell visible. Urban areas typically see 5-15 neighbors.",
                score = 15,
                severity = "MEDIUM"
            )
            neighborCount < 3 -> ThreatIndicator(
                name = "LOW_NEIGHBORS",
                description = "Only $neighborCount neighbors visible. This is lower than typical.",
                score = 5,
                severity = "LOW"
            )
            else -> ThreatIndicator("NEIGHBORS_OK", "$neighborCount neighbors detected - normal", 0, "NONE")
        }
    }
    
    /**
     * Historical comparison
     * Detect new towers or towers that changed characteristics
     */
    private fun analyzeHistory(tower: CellTower): ThreatIndicator {
        val towerKey = "${tower.mobileCountryCode}-${tower.mobileNetworkCode}-${tower.locationAreaCode}-${tower.cellId}"
        val baseline = knownTowers[towerKey]
        
        if (baseline == null) {
            // New tower - record it but flag if serving
            knownTowers[towerKey] = TowerBaseline(
                cellId = tower.cellId,
                lac = tower.locationAreaCode,
                mcc = tower.mobileCountryCode,
                mnc = tower.mobileNetworkCode,
                networkType = tower.networkType,
                firstSeen = System.currentTimeMillis(),
                signalRange = tower.signalStrength..tower.signalStrength
            )
            
            return if (tower.isServingCell) {
                ThreatIndicator(
                    name = "NEW_TOWER_SERVING",
                    description = "⚠️ FIRST TIME seeing this tower and it's your serving cell. " +
                            "Could be a new legitimate tower OR a stingray. Monitor closely.",
                    score = 25,
                    severity = "MEDIUM"
                )
            } else {
                ThreatIndicator(
                    name = "NEW_TOWER",
                    description = "First observation of this tower. Recording baseline.",
                    score = 5,
                    severity = "LOW"
                )
            }
        } else {
            // Check for characteristic changes
            val issues = mutableListOf<String>()
            
            if (baseline.networkType != tower.networkType) {
                issues.add("Network type changed from ${baseline.networkType} to ${tower.networkType}")
            }
            
            // Signal significantly stronger than ever seen
            if (tower.signalStrength > baseline.signalRange.last + 20) {
                issues.add("Signal ${tower.signalStrength}dBm is much stronger than historical max ${baseline.signalRange.last}dBm")
            }
            
            if (issues.isNotEmpty()) {
                return ThreatIndicator(
                    name = "TOWER_CHANGED",
                    description = "⚠️ Tower characteristics changed: ${issues.joinToString("; ")}",
                    score = 30,
                    severity = "HIGH"
                )
            }
            
            // Update baseline
            val newMin = minOf(baseline.signalRange.first, tower.signalStrength)
            val newMax = maxOf(baseline.signalRange.last, tower.signalStrength)
            knownTowers[towerKey] = baseline.copy(
                signalRange = newMin..newMax,
                lastSeen = System.currentTimeMillis()
            )
        }
        
        return ThreatIndicator("HISTORY_OK", "Tower matches historical baseline", 0, "NONE")
    }
    
    /**
     * Cell ID validity check
     */
    private fun analyzeCellId(tower: CellTower): ThreatIndicator {
        val cellId = tower.cellId
        
        // LTE cell IDs (eNB ID + sector) should be in reasonable range
        if (tower.networkType == "LTE") {
            val enbId = cellId shr 8  // Extract eNB ID
            if (enbId <= 0 || enbId > 1048575) {  // Valid range for eNB ID
                return ThreatIndicator(
                    name = "INVALID_CELL_ID",
                    description = "Cell ID $cellId has invalid eNB ID component. This is suspicious.",
                    score = 25,
                    severity = "MEDIUM"
                )
            }
        }
        
        // Check for obviously fake cell IDs
        if (cellId <= 0) {
            return ThreatIndicator(
                name = "ZERO_CELL_ID",
                description = "Cell ID is zero or negative. Invalid for legitimate towers.",
                score = 40,
                severity = "HIGH"
            )
        }
        
        return ThreatIndicator("CELL_ID_OK", "Cell ID appears valid", 0, "NONE")
    }
    
    /**
     * Location Area Code consistency check
     */
    private fun analyzeLac(tower: CellTower): ThreatIndicator {
        val lac = tower.locationAreaCode
        
        if (lac <= 0) {
            return ThreatIndicator(
                name = "INVALID_LAC",
                description = "Location Area Code is zero or invalid. Legitimate towers have valid LACs.",
                score = 30,
                severity = "HIGH"
            )
        }
        
        // Check against recent observations for LAC consistency in the area
        val recentLacs = recentObservations
            .filter { System.currentTimeMillis() - it.timestamp < 300000 } // Last 5 min
            .map { it.lac }
            .distinct()
        
        if (recentLacs.isNotEmpty() && lac !in recentLacs && tower.isServingCell) {
            return ThreatIndicator(
                name = "LAC_MISMATCH",
                description = "LAC $lac doesn't match other towers in area ($recentLacs). Possible spoofing.",
                score = 20,
                severity = "MEDIUM"
            )
        }
        
        return ThreatIndicator("LAC_OK", "Location Area Code is consistent", 0, "NONE")
    }
    
    /**
     * Rapid tower change detection
     * Frequent handoffs to different cells can indicate a mobile stingray
     */
    private fun analyzeRapidChanges(tower: CellTower): ThreatIndicator {
        val recent = recentObservations
            .filter { it.isServing && System.currentTimeMillis() - it.timestamp < 60000 } // Last minute
        
        val uniqueServingCells = recent.map { it.cellId }.distinct().size
        
        return when {
            uniqueServingCells >= 5 -> ThreatIndicator(
                name = "RAPID_HANDOFFS",
                description = "⚠️ $uniqueServingCells different serving cells in the last minute. " +
                        "This could indicate a mobile stingray forcing reconnections.",
                score = 35,
                severity = "HIGH"
            )
            uniqueServingCells >= 3 -> ThreatIndicator(
                name = "FREQUENT_HANDOFFS",
                description = "$uniqueServingCells serving cell changes in a minute. Unusual unless moving fast.",
                score = 15,
                severity = "MEDIUM"
            )
            else -> ThreatIndicator("HANDOFFS_OK", "Tower changes are within normal range", 0, "NONE")
        }
    }
    
    private fun recordObservation(tower: CellTower) {
        recentObservations.add(TowerObservation(
            cellId = tower.cellId,
            lac = tower.locationAreaCode,
            isServing = tower.isServingCell,
            timestamp = System.currentTimeMillis()
        ))
        
        // Keep only last 100 observations
        if (recentObservations.size > 100) {
            recentObservations.removeAt(0)
        }
    }
    
    private fun getRecommendation(level: ThreatLevel, indicators: List<ThreatIndicator>): String {
        return when (level) {
            ThreatLevel.CRITICAL -> "🚨 HIGH RISK! Consider: 1) Enable airplane mode 2) Move to different location " +
                    "3) Avoid sensitive communications 4) Report this location"
            ThreatLevel.HIGH -> "⚠️ SUSPICIOUS ACTIVITY: Avoid sensitive calls/texts. Consider moving locations. " +
                    "Continue monitoring."
            ThreatLevel.MEDIUM -> "Monitor situation. Some anomalies detected but could be legitimate. " +
                    "Avoid sending sensitive information until threat clears."
            ThreatLevel.LOW -> "Minor anomalies detected. Likely normal but worth noting."
            ThreatLevel.NONE -> "No threats detected. Cell tower appears legitimate."
        }
    }
}

data class ThreatAssessment(
    val tower: CellTower,
    val threatLevel: ThreatLevel,
    val totalScore: Int,
    val indicators: List<ThreatIndicator>,
    val recommendation: String
)

data class ThreatIndicator(
    val name: String,
    val description: String,
    val score: Int,
    val severity: String
)

data class TowerBaseline(
    val cellId: Int,
    val lac: Int,
    val mcc: String,
    val mnc: String,
    val networkType: String,
    val firstSeen: Long,
    val lastSeen: Long = System.currentTimeMillis(),
    val signalRange: IntRange
)

data class TowerObservation(
    val cellId: Int,
    val lac: Int,
    val isServing: Boolean,
    val timestamp: Long
)

