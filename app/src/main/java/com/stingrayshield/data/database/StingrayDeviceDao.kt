package com.stingrayshield.data.database

import androidx.room.*
import com.stingrayshield.domain.model.StingrayDevice
import com.stingrayshield.domain.model.ThreatLevel
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for StingrayDevice entities
 */
@Dao
interface StingrayDeviceDao {
    
    @Query("SELECT * FROM stingray_devices ORDER BY detectionTimestamp DESC")
    fun getAllDevices(): Flow<List<StingrayDevice>>
    
    @Query("SELECT * FROM stingray_devices WHERE id = :id")
    suspend fun getDeviceById(id: Long): StingrayDevice?
    
    @Query("SELECT * FROM stingray_devices WHERE threatLevel = :threatLevel ORDER BY detectionTimestamp DESC")
    fun getDevicesByThreatLevel(threatLevel: ThreatLevel): Flow<List<StingrayDevice>>
    
    @Query("SELECT * FROM stingray_devices WHERE cellId = :cellId ORDER BY detectionTimestamp DESC")
    suspend fun getDevicesByCellId(cellId: Int): List<StingrayDevice>
    
    @Query("SELECT * FROM stingray_devices WHERE deviceFingerprint = :fingerprint ORDER BY detectionTimestamp DESC")
    suspend fun getDevicesByFingerprint(fingerprint: String): List<StingrayDevice>
    
    @Query("SELECT * FROM stingray_devices WHERE isConfirmed = 1 ORDER BY detectionTimestamp DESC")
    fun getConfirmedDevices(): Flow<List<StingrayDevice>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: StingrayDevice): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevices(devices: List<StingrayDevice>)
    
    @Update
    suspend fun updateDevice(device: StingrayDevice)
    
    @Delete
    suspend fun deleteDevice(device: StingrayDevice)
    
    @Query("DELETE FROM stingray_devices WHERE id = :id")
    suspend fun deleteDeviceById(id: Long)
    
    @Query("SELECT COUNT(*) FROM stingray_devices")
    suspend fun getDeviceCount(): Int
    
    @Query("SELECT COUNT(*) FROM stingray_devices WHERE threatLevel = :threatLevel")
    suspend fun getDeviceCountByThreatLevel(threatLevel: ThreatLevel): Int
    
    @Query("DELETE FROM stingray_devices")
    suspend fun deleteAllDevices(): Int
}

