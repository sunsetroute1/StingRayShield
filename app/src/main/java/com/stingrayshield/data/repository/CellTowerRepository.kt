package com.stingrayshield.data.repository

import com.stingrayshield.data.database.CellTowerDao
import com.stingrayshield.domain.model.CellTower
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing cell tower data
 */
@Singleton
class CellTowerRepository @Inject constructor(
    private val cellTowerDao: CellTowerDao
) {
    /**
     * Add a new cell tower observation to the database
     */
    suspend fun addCellTower(cellTower: CellTower): Long {
        return cellTowerDao.insertCellTower(cellTower)
    }

    /**
     * Add multiple cell tower observations at once
     */
    suspend fun addCellTowers(cellTowers: List<CellTower>): List<Long> {
        return cellTowerDao.insertCellTowers(cellTowers)
    }

    /**
     * Get all cell tower observations as a Flow
     */
    fun getAllCellTowers(): Flow<List<CellTower>> {
        return cellTowerDao.getAllCellTowersAsFlow()
    }

    /**
     * Get cell tower observations within a specific time range
     */
    suspend fun getCellTowersInTimeRange(startTime: Long, endTime: Long): List<CellTower> {
        return cellTowerDao.getCellTowersInTimeRange(startTime, endTime)
    }

    /**
     * Get all observations for a specific cell ID
     */
    suspend fun getCellTowersByCellId(cellId: Int): List<CellTower> {
        return cellTowerDao.getCellTowersByCellId(cellId)
    }

    /**
     * Get the most recent cell tower observation
     */
    suspend fun getMostRecentCellTower(): CellTower? {
        return cellTowerDao.getMostRecentCellTower()
    }

    /**
     * Get a list of all unique cell IDs that have been observed
     */
    suspend fun getAllUniqueCellIds(): List<Int> {
        return cellTowerDao.getAllUniqueCellIds()
    }

    /**
     * Get the average signal strength for a specific cell ID
     */
    suspend fun getAverageSignalStrengthForCell(cellId: Int): Double {
        return cellTowerDao.getAverageSignalStrengthForCell(cellId)
    }

    /**
     * Delete cell tower observations older than a specific timestamp
     */
    suspend fun deleteOldCellTowers(timestamp: Long): Int {
        return cellTowerDao.deleteOldCellTowers(timestamp)
    }

    /**
     * Get the number of times a specific cell ID has been observed
     */
    suspend fun getCellTowerCount(cellId: Int): Int {
        return cellTowerDao.getCellTowerCount(cellId)
    }

    /**
     * Delete all cell tower records from the database
     */
    suspend fun clearAllData(): Int {
        return cellTowerDao.deleteAllCellTowers()
    }
    
    /**
     * Get a cell tower by its cell ID (returns the most recent observation)
     */
    suspend fun getCellTowerById(cellId: Int): CellTower? {
        return cellTowerDao.getCellTowersByCellId(cellId).maxByOrNull { it.lastSeen }
    }
    
    /**
     * Get history of observations for a specific cell ID as a Flow
     */
    fun getCellTowerHistoryById(cellId: Int): Flow<List<CellTower>> {
        return cellTowerDao.getCellTowerHistoryByCellIdAsFlow(cellId)
    }
    
    /**
     * Update a cell tower record
     */
    suspend fun updateCellTower(cellTower: CellTower) {
        cellTowerDao.updateCellTower(cellTower)
    }
    
    /**
     * Get current (primary) cell towers as a Flow
     */
    fun getCurrentCellTowers(): Flow<List<CellTower>> {
        return cellTowerDao.getCurrentCellTowers()
    }
    
    /**
     * Get neighboring cell towers as a Flow
     */
    fun getNeighboringCellTowers(): Flow<List<CellTower>> {
        return cellTowerDao.getNeighboringCellTowers()
    }
    
    /**
     * Get cell towers with valid location coordinates
     */
    fun getCellTowersWithLocation(): Flow<List<CellTower>> {
        return cellTowerDao.getCellTowersWithLocation()
    }
    
    /**
     * Get cell towers without location coordinates
     */
    fun getCellTowersWithoutLocation(): Flow<List<CellTower>> {
        return cellTowerDao.getCellTowersWithoutLocation()
    }
    
    /**
     * Get count of towers with location
     */
    suspend fun getCountWithLocation(): Int {
        return cellTowerDao.getCountWithLocation()
    }
    
    /**
     * Get count of towers without location
     */
    suspend fun getCountWithoutLocation(): Int {
        return cellTowerDao.getCountWithoutLocation()
    }
    
    /**
     * Get unique cell towers (one per cell ID, most recent observation)
     */
    fun getUniqueCellTowers(): Flow<List<CellTower>> {
        return cellTowerDao.getUniqueCellTowers()
    }
    
    /**
     * Get cell towers within a geographic bounding box
     */
    suspend fun getCellTowersInBounds(minLat: Double, maxLat: Double, minLng: Double, maxLng: Double): List<CellTower> {
        return cellTowerDao.getCellTowersInBounds(minLat, maxLat, minLng, maxLng)
    }
}
