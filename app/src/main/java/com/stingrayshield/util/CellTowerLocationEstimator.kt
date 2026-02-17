package com.stingrayshield.util

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Utility class to estimate cell tower locations based on signal strength
 * and provide carrier information from MCC/MNC codes.
 * 
 * Since Android doesn't provide actual tower coordinates, we estimate positions
 * based on signal strength (stronger signal = closer tower).
 */
object CellTowerLocationEstimator {

    /**
     * Estimate tower distance from signal strength in dBm
     * Uses free-space path loss model approximation
     * 
     * @param signalStrength Signal strength in dBm
     * @param networkType Network type (2G, 3G, 4G, 5G)
     * @return Estimated distance in meters
     */
    fun estimateDistanceFromSignal(signalStrength: Int, networkType: String): Double {
        // Reference values for path loss calculation
        // These are approximations based on typical cellular frequencies
        val referenceDistance = 1.0 // 1 meter
        val referenceSignal = when (networkType) {
            "5G" -> -40.0  // 5G at 1m (mmWave can be stronger close up)
            "4G" -> -45.0  // LTE at 1m
            "3G" -> -50.0  // UMTS at 1m
            "2G" -> -55.0  // GSM at 1m
            "CDMA" -> -50.0
            else -> -50.0
        }
        
        // Path loss exponent (varies by environment)
        // 2.0 = free space, 2.7-3.5 = urban, 3.0-5.0 = indoor
        val pathLossExponent = when (networkType) {
            "5G" -> 2.5  // 5G has more line-of-sight characteristics
            "4G" -> 3.0
            "3G" -> 3.2
            "2G" -> 3.5
            else -> 3.0
        }
        
        // Calculate distance using path loss formula
        // signalStrength = referenceSignal - 10 * n * log10(d/d0)
        // Solving for d: d = d0 * 10^((referenceSignal - signalStrength) / (10 * n))
        val signalDiff = referenceSignal - signalStrength
        val distance = referenceDistance * 10.0.pow(signalDiff / (10.0 * pathLossExponent))
        
        // Clamp to reasonable values (50m to 35km for cell towers)
        return distance.coerceIn(50.0, 35000.0)
    }

    /**
     * Generate an estimated position for a cell tower based on user location and signal strength.
     * Places the tower at the estimated distance in a direction based on cell ID (for consistency).
     * 
     * @param userLat User's latitude
     * @param userLng User's longitude
     * @param signalStrength Signal strength in dBm
     * @param networkType Network type string
     * @param cellId Cell ID (used to determine consistent direction)
     * @return Pair of (latitude, longitude) for estimated tower position
     */
    fun estimateTowerPosition(
        userLat: Double,
        userLng: Double,
        signalStrength: Int,
        networkType: String,
        cellId: Int
    ): Pair<Double, Double> {
        if (userLat == 0.0 && userLng == 0.0) return Pair(0.0, 0.0)
        
        val distance = estimateDistanceFromSignal(signalStrength, networkType)
        
        // Use cell ID to determine a consistent direction (so same tower always appears in same spot)
        val angle = (cellId % 360) * (PI / 180.0)
        
        // Convert distance to degrees (approximate)
        // 1 degree latitude ≈ 111,320 meters
        // 1 degree longitude ≈ 111,320 * cos(latitude) meters
        val latOffset = (distance * cos(angle)) / 111320.0
        val lngOffset = (distance * sin(angle)) / (111320.0 * cos(userLat * PI / 180.0))
        
        return Pair(userLat + latOffset, userLng + lngOffset)
    }

    /**
     * Get carrier name from MCC (Mobile Country Code) and MNC (Mobile Network Code)
     * Focuses on US carriers but includes major international ones
     */
    fun getCarrierName(mcc: String?, mnc: String?): String {
        if (mcc == null || mnc == null) return "Unknown Carrier"
        
        val mccInt = mcc.toIntOrNull() ?: return "Unknown Carrier"
        val mncInt = mnc.toIntOrNull() ?: return "Unknown Carrier"
        
        return when (mccInt) {
            // United States (MCC 310, 311, 312, 313, 316)
            310 -> getUSCarrier310(mncInt)
            311 -> getUSCarrier311(mncInt)
            312 -> "US Regional (312-$mncInt)"
            313 -> "US Carrier (313-$mncInt)"
            316 -> "US Carrier (316-$mncInt)"
            
            // Canada (MCC 302)
            302 -> getCanadaCarrier(mncInt)
            
            // Mexico (MCC 334)
            334 -> getMexicoCarrier(mncInt)
            
            // UK (MCC 234, 235)
            234, 235 -> getUKCarrier(mncInt)
            
            // Germany (MCC 262)
            262 -> getGermanyCarrier(mncInt)
            
            // France (MCC 208)
            208 -> getFranceCarrier(mncInt)
            
            // Japan (MCC 440, 441)
            440, 441 -> getJapanCarrier(mncInt)
            
            // South Korea (MCC 450)
            450 -> getKoreaCarrier(mncInt)
            
            // China (MCC 460)
            460 -> getChinaCarrier(mncInt)
            
            // Australia (MCC 505)
            505 -> getAustraliaCarrier(mncInt)
            
            // India (MCC 404, 405)
            404, 405 -> "India Carrier"
            
            else -> "International ($mccInt-$mncInt)"
        }
    }

    private fun getUSCarrier310(mnc: Int): String = when (mnc) {
        // Verizon
        4, 5, 6, 10, 12, 13, 350, 590, 820, 890, 910 -> "Verizon"
        // AT&T
        7, 16, 17, 30, 38, 70, 80, 90, 150, 170, 280, 380, 410, 560, 680, 950, 980 -> "AT&T"
        // T-Mobile
        20, 21, 22, 23, 24, 25, 26, 160, 200, 210, 220, 230, 240, 250, 260, 270, 310, 330, 490, 580, 660, 800 -> "T-Mobile"
        // Sprint (now T-Mobile)
        120, 830 -> "Sprint (T-Mobile)"
        // US Cellular
        730 -> "US Cellular"
        // Cricket (AT&T)
        180 -> "Cricket (AT&T)"
        // Metro (T-Mobile)
        320 -> "Metro by T-Mobile"
        // Dish Network
        750, 760 -> "Dish Network"
        // Google Fi
        850 -> "Google Fi"
        else -> "US Carrier (310-$mnc)"
    }

    private fun getUSCarrier311(mnc: Int): String = when (mnc) {
        // Verizon
        12, 110, 270, 271, 272, 273, 274, 275, 276, 277, 278, 279, 280, 281, 282, 
        283, 284, 285, 286, 287, 288, 289, 480, 481, 482, 483, 484, 485, 486, 
        487, 488, 489 -> "Verizon"
        // AT&T
        30, 70, 90, 180, 190 -> "AT&T"
        // T-Mobile
        490, 660, 882, 490 -> "T-Mobile"
        // Sprint (now T-Mobile)
        490, 870, 880 -> "Sprint (T-Mobile)"
        // US Cellular
        220, 221, 222, 223, 224, 225, 226, 227, 228, 229, 580 -> "US Cellular"
        // C Spire
        230, 231 -> "C Spire"
        else -> "US Carrier (311-$mnc)"
    }

    private fun getCanadaCarrier(mnc: Int): String = when (mnc) {
        220, 221, 222, 223 -> "Telus"
        370, 610, 630, 640 -> "Bell"
        490, 500, 510, 520, 530, 540, 590 -> "Wind/Freedom"
        660, 680 -> "Bell MTS"
        720, 730 -> "Rogers"
        else -> "Canada ($mnc)"
    }

    private fun getMexicoCarrier(mnc: Int): String = when (mnc) {
        1, 2, 3, 4 -> "Telcel"
        20 -> "AT&T Mexico"
        50 -> "Movistar"
        else -> "Mexico ($mnc)"
    }

    private fun getUKCarrier(mnc: Int): String = when (mnc) {
        10, 11, 12 -> "O2"
        15, 16 -> "Vodafone"
        20, 30 -> "Three"
        33, 34 -> "EE"
        50, 51 -> "BT"
        else -> "UK ($mnc)"
    }

    private fun getGermanyCarrier(mnc: Int): String = when (mnc) {
        1, 6 -> "Telekom"
        2, 4, 9 -> "Vodafone"
        3, 5, 7, 8, 77 -> "O2"
        else -> "Germany ($mnc)"
    }

    private fun getFranceCarrier(mnc: Int): String = when (mnc) {
        1, 2 -> "Orange"
        10, 11, 13 -> "SFR"
        15, 16 -> "Free"
        20, 21 -> "Bouygues"
        else -> "France ($mnc)"
    }

    private fun getJapanCarrier(mnc: Int): String = when (mnc) {
        0, 1, 2, 3, 4, 6, 7, 8, 9, 10, 49, 50, 51, 52, 53, 54, 55, 56, 58, 70, 
        71, 72, 73, 74, 75, 76, 78, 79, 88, 89, 90, 92, 93, 94, 95, 96, 97, 98, 99 -> "NTT Docomo"
        20, 21 -> "SoftBank"
        else -> "Japan ($mnc)"
    }

    private fun getKoreaCarrier(mnc: Int): String = when (mnc) {
        2, 4, 8 -> "KT"
        5, 11 -> "SK Telecom"
        6 -> "LG U+"
        else -> "Korea ($mnc)"
    }

    private fun getChinaCarrier(mnc: Int): String = when (mnc) {
        0, 2, 4, 7, 8 -> "China Mobile"
        1, 6, 9 -> "China Unicom"
        3, 5, 11 -> "China Telecom"
        else -> "China ($mnc)"
    }

    private fun getAustraliaCarrier(mnc: Int): String = when (mnc) {
        1, 71, 72 -> "Telstra"
        2, 90 -> "Optus"
        3 -> "Vodafone AU"
        else -> "Australia ($mnc)"
    }

    /**
     * Get network capabilities description based on network type
     */
    fun getNetworkCapabilities(networkType: String): String = when (networkType) {
        "5G" -> "5G NR • Ultra-fast speeds • Low latency • Enhanced capacity"
        "4G" -> "LTE • High-speed data • HD voice • Streaming capable"
        "3G" -> "UMTS/HSPA • Moderate speed • Basic video • Voice calls"
        "2G" -> "GSM/EDGE • Basic data • Voice calls • SMS"
        "CDMA" -> "CDMA • Legacy network • Voice optimized"
        else -> "Unknown network type"
    }

    /**
     * Get frequency band information (approximation based on network type)
     */
    fun getFrequencyInfo(networkType: String): String = when (networkType) {
        "5G" -> "Sub-6 GHz / mmWave (28-39 GHz)"
        "4G" -> "700-2600 MHz (Bands 2, 4, 5, 12, 13, 66, 71)"
        "3G" -> "850-2100 MHz (Bands 1, 2, 4, 5)"
        "2G" -> "850-1900 MHz (GSM 850/1900)"
        "CDMA" -> "800-1900 MHz"
        else -> "Unknown frequency"
    }

    /**
     * Get signal quality description
     */
    fun getSignalQuality(signalStrength: Int, networkType: String): String {
        val quality = when (networkType) {
            "5G" -> when {
                signalStrength >= -80 -> "Excellent"
                signalStrength >= -95 -> "Good"
                signalStrength >= -110 -> "Fair"
                else -> "Poor"
            }
            "4G" -> when {
                signalStrength >= -80 -> "Excellent"
                signalStrength >= -90 -> "Good"
                signalStrength >= -100 -> "Fair"
                signalStrength >= -110 -> "Poor"
                else -> "Very Poor"
            }
            else -> when {
                signalStrength >= -70 -> "Excellent"
                signalStrength >= -85 -> "Good"
                signalStrength >= -100 -> "Fair"
                signalStrength >= -110 -> "Poor"
                else -> "Very Poor"
            }
        }
        
        val estimatedSpeed = when (networkType) {
            "5G" -> when {
                signalStrength >= -80 -> "500+ Mbps"
                signalStrength >= -95 -> "100-500 Mbps"
                signalStrength >= -110 -> "50-100 Mbps"
                else -> "<50 Mbps"
            }
            "4G" -> when {
                signalStrength >= -80 -> "50-150 Mbps"
                signalStrength >= -90 -> "25-50 Mbps"
                signalStrength >= -100 -> "10-25 Mbps"
                else -> "<10 Mbps"
            }
            "3G" -> when {
                signalStrength >= -85 -> "5-20 Mbps"
                signalStrength >= -100 -> "1-5 Mbps"
                else -> "<1 Mbps"
            }
            else -> "<500 Kbps"
        }
        
        return "$quality • Est. speed: $estimatedSpeed"
    }
}

