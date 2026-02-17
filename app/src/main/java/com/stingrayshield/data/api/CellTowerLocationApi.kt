package com.stingrayshield.data.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * API interface for fetching cell tower locations from public databases.
 * 
 * Primary: OpenCellID (opencellid.org)
 * Fallback: UnwiredLabs (unwiredlabs.com)
 * 
 * These services provide real cell tower coordinates based on MCC, MNC, LAC, and Cell ID.
 */
interface CellTowerLocationApi {
    
    /**
     * OpenCellID API endpoint - get single cell
     * Requires API key (free registration at opencellid.org)
     * 
     * Example: https://opencellid.org/cell/get?key=API_KEY&mcc=310&mnc=4&lac=1234&cellid=5678
     */
    @GET("cell/get")
    suspend fun getOpenCellIdLocation(
        @Query("key") apiKey: String,
        @Query("mcc") mcc: Int,
        @Query("mnc") mnc: Int,
        @Query("lac") lac: Int,
        @Query("cellid") cellId: Int,
        @Query("format") format: String = "json"
    ): Response<OpenCellIdResponse>
    
    /**
     * OpenCellID API endpoint - get all cells in a bounding box
     * Returns all cell towers within the specified geographic area
     * 
     * Example: https://opencellid.org/cell/getInArea?key=API_KEY&BBOX=latmin,lonmin,latmax,lonmax&format=json
     * 
     * Note: Limited to 1000 cells per request
     */
    @GET("cell/getInArea")
    suspend fun getCellsInArea(
        @Query("key") apiKey: String,
        @Query("BBOX") bbox: String, // Format: "latmin,lonmin,latmax,lonmax"
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 1000
    ): Response<OpenCellIdAreaResponse>
    
    /**
     * UnwiredLabs Geolocation API (alternative/fallback)
     * Requires API key (free tier available at unwiredlabs.com)
     */
    @GET("v2/process.php")
    suspend fun getUnwiredLabsLocation(
        @Query("token") token: String,
        @Query("mcc") mcc: Int,
        @Query("mnc") mnc: Int,
        @Query("lac") lac: Int,
        @Query("cid") cellId: Int
    ): Response<UnwiredLabsResponse>
}

/**
 * Response from OpenCellID API - single cell
 */
data class OpenCellIdResponse(
    val status: String?,
    val lat: Double?,
    val lon: Double?,
    val range: Int?,
    val averageSignalStrength: Int?,
    val samples: Int?,
    val changeable: Int?,
    val radio: String?,
    val mcc: Int?,
    val mnc: Int?,
    val lac: Int?,
    val cellid: Int?,
    val unit: Int?,
    val message: String? // Error message if status != "ok"
)

/**
 * Response from OpenCellID API - cells in area
 */
data class OpenCellIdAreaResponse(
    val count: Int?,
    val cells: List<OpenCellIdCell>?,
    val status: String?,
    val message: String?
)

/**
 * Individual cell in area response
 */
data class OpenCellIdCell(
    val lat: Double?,
    val lon: Double?,
    val range: Int?,
    val samples: Int?,
    val changeable: Int?,
    val radio: String?,
    val mcc: Int?,
    val mnc: Int?,
    val lac: Int?,
    val cellid: Int?,
    val averageSignalStrength: Int?,
    val unit: Int?,
    val created: Long?,
    val updated: Long?
)

/**
 * Response from UnwiredLabs API
 */
data class UnwiredLabsResponse(
    val status: String?,
    val message: String?,
    val balance: Int?,
    val lat: Double?,
    val lon: Double?,
    val accuracy: Int?
)

/**
 * Unified cell tower location result
 */
data class CellTowerLocation(
    val latitude: Double,
    val longitude: Double,
    val range: Int?, // Accuracy/range in meters
    val source: String, // "opencellid", "unwiredlabs", "cached"
    val samples: Int? = null // Number of observations (if available)
) {
    companion object {
        fun fromOpenCellId(response: OpenCellIdResponse): CellTowerLocation? {
            if (response.lat == null || response.lon == null) return null
            return CellTowerLocation(
                latitude = response.lat,
                longitude = response.lon,
                range = response.range,
                source = "opencellid",
                samples = response.samples
            )
        }
        
        fun fromUnwiredLabs(response: UnwiredLabsResponse): CellTowerLocation? {
            if (response.lat == null || response.lon == null) return null
            return CellTowerLocation(
                latitude = response.lat,
                longitude = response.lon,
                range = response.accuracy,
                source = "unwiredlabs"
            )
        }
    }
}
