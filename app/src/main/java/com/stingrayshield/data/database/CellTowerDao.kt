package com.stingrayshield.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.stingrayshield.domain.model.CellTower
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for the cell tower table
 */
@Dao
interface CellTowerDao {
    
    /**
     * Insert a new cell tower record
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCellTower(cellTower: CellTower): Long
    
    /**
     * Insert multiple cell tower records
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCellTowers(cellTowers: List<CellTower>): List<Long>
    
    /**
     * Get all cell towers as a Flow
     */
    @Query("SELECT * FROM cell_towers ORDER BY timestamp DESC")
    fun getAllCellTowersAsFlow(): Flow<List<CellTower>>
    
    /**
     * Get cell towers within a time range
     */
    @Query("SELECT * FROM cell_towers WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    suspend fun getCellTowersInTimeRange(startTime: Long, endTime: Long): List<CellTower>
    
    /**
     * Get cell towers for a specific cell ID
     */
    @Query("SELECT * FROM cell_towers WHERE cellId = :cellId ORDER BY timestamp DESC")
    suspend fun getCellTowersByCellId(cellId: Int): List<CellTower>
    
    /**
     * Get the most recent cell tower record
     */
    @Query("SELECT * FROM cell_towers ORDER BY timestamp DESC LIMIT 1")
    suspend fun getMostRecentCellTower(): CellTower?
    
    /**
     * Get all unique cell IDs observed
     */
    @Query("SELECT DISTINCT cellId FROM cell_towers")
    suspend fun getAllUniqueCellIds(): List<Int>
    
    /**
     * Get average signal strength for a specific cell ID
     */
    @Query("SELECT AVG(signalStrength) FROM cell_towers WHERE cellId = :cellId")
    suspend fun getAverageSignalStrengthForCell(cellId: Int): Double
    
    /**
     * Delete all cell tower records older than a specific time
     */
    @Query("DELETE FROM cell_towers WHERE timestamp < :timestamp")
    suspend fun deleteOldCellTowers(timestamp: Long): Int
    
    /**
     * Count how many times a specific cell ID has been seen
     */
    @Query("SELECT COUNT(*) FROM cell_towers WHERE cellId = :cellId")
    suspend fun getCellTowerCount(cellId: Int): Int
    
    /**
     * Delete all cell tower records
     */
    @Query("DELETE FROM cell_towers")
    suspend fun deleteAllCellTowers(): Int
    
    /**
     * Get cell tower history for a specific cell ID as a Flow
     */
    @Query("SELECT * FROM cell_towers WHERE cellId = :cellId ORDER BY lastSeen DESC")
    fun getCellTowerHistoryByCellIdAsFlow(cellId: Int): Flow<List<CellTower>>
    
    /**
     * Update a cell tower record
     */
    @Update
    suspend fun updateCellTower(cellTower: CellTower)
    
    /**
     * Get current (primary) cell towers as a Flow
     * Returns the most recent observation for each unique primary cell
     */
    @Query("""
        SELECT * FROM cell_towers 
        WHERE isPrimary = 1 
        AND timestamp = (
            SELECT MAX(timestamp) 
            FROM cell_towers t2 
            WHERE t2.cellId = cell_towers.cellId 
            AND t2.isPrimary = 1
        )
        ORDER BY timestamp DESC
    """)
    fun getCurrentCellTowers(): Flow<List<CellTower>>
    
    /**
     * Get neighboring cell towers as a Flow
     * Returns the most recent observation for each unique neighboring cell
     */
    @Query("""
        SELECT * FROM cell_towers 
        WHERE isPrimary = 0 
        AND timestamp = (
            SELECT MAX(timestamp) 
            FROM cell_towers t2 
            WHERE t2.cellId = cell_towers.cellId 
            AND t2.isPrimary = 0
        )
        ORDER BY timestamp DESC
        LIMIT 50
    """)
    fun getNeighboringCellTowers(): Flow<List<CellTower>>
    
    /**
     * Get cell towers that have valid location coordinates
     */
    @Query("SELECT * FROM cell_towers WHERE latitude != 0.0 AND longitude != 0.0 ORDER BY timestamp DESC")
    fun getCellTowersWithLocation(): Flow<List<CellTower>>
    
    /**
     * Get cell towers without location coordinates
     */
    @Query("SELECT * FROM cell_towers WHERE latitude = 0.0 OR longitude = 0.0 ORDER BY timestamp DESC")
    fun getCellTowersWithoutLocation(): Flow<List<CellTower>>
    
    /**
     * Get count of towers with location
     */
    @Query("SELECT COUNT(*) FROM cell_towers WHERE latitude != 0.0 AND longitude != 0.0")
    suspend fun getCountWithLocation(): Int
    
    /**
     * Get count of towers without location
     */
    @Query("SELECT COUNT(*) FROM cell_towers WHERE latitude = 0.0 OR longitude = 0.0")
    suspend fun getCountWithoutLocation(): Int
    
    /**
     * Get unique cell towers (most recent observation per cell ID)
     */
    @Query("""
        SELECT * FROM cell_towers t1
        WHERE timestamp = (
            SELECT MAX(timestamp) FROM cell_towers t2 WHERE t2.cellId = t1.cellId
        )
        ORDER BY timestamp DESC
    """)
    fun getUniqueCellTowers(): Flow<List<CellTower>>
    
    /**
     * Get cell towers within a geographic bounding box (using real tower location if available, otherwise observation location)
     */
    @Query("""
        SELECT * FROM cell_towers t1
        WHERE timestamp = (
            SELECT MAX(timestamp) FROM cell_towers t2 WHERE t2.cellId = t1.cellId
        )
        AND (
            (towerLatitude IS NOT NULL AND towerLatitude BETWEEN :minLat AND :maxLat AND towerLongitude BETWEEN :minLng AND :maxLng)
            OR (towerLatitude IS NULL AND latitude BETWEEN :minLat AND :maxLat AND longitude BETWEEN :minLng AND :maxLng)
        )
        ORDER BY timestamp DESC
    """)
    suspend fun getCellTowersInBounds(minLat: Double, maxLat: Double, minLng: Double, maxLng: Double): List<CellTower>
}
