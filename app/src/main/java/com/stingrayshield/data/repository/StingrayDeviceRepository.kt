package com.stingrayshield.data.repository

import com.stingrayshield.data.database.StingrayDeviceDao
import com.stingrayshield.domain.model.StingrayDevice
import com.stingrayshield.domain.model.ThreatLevel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing StingrayDevice data
 */
@Singleton
class StingrayDeviceRepository @Inject constructor(
    private val stingrayDeviceDao: StingrayDeviceDao
) {
    
    fun getAllDevices(): Flow<List<StingrayDevice>> = stingrayDeviceDao.getAllDevices()
    
    suspend fun getDeviceById(id: Long): StingrayDevice? = stingrayDeviceDao.getDeviceById(id)
    
    fun getDevicesByThreatLevel(threatLevel: ThreatLevel): Flow<List<StingrayDevice>> = 
        stingrayDeviceDao.getDevicesByThreatLevel(threatLevel)
    
    suspend fun getDevicesByCellId(cellId: Int): List<StingrayDevice> = 
        stingrayDeviceDao.getDevicesByCellId(cellId)
    
    suspend fun getDevicesByFingerprint(fingerprint: String): List<StingrayDevice> = 
        stingrayDeviceDao.getDevicesByFingerprint(fingerprint)
    
    fun getConfirmedDevices(): Flow<List<StingrayDevice>> = stingrayDeviceDao.getConfirmedDevices()
    
    suspend fun insertDevice(device: StingrayDevice): Long = stingrayDeviceDao.insertDevice(device)
    
    suspend fun insertDevices(devices: List<StingrayDevice>) = stingrayDeviceDao.insertDevices(devices)
    
    suspend fun updateDevice(device: StingrayDevice) = stingrayDeviceDao.updateDevice(device)
    
    suspend fun deleteDevice(device: StingrayDevice) = stingrayDeviceDao.deleteDevice(device)
    
    suspend fun deleteDeviceById(id: Long) = stingrayDeviceDao.deleteDeviceById(id)
    
    suspend fun getDeviceCount(): Int = stingrayDeviceDao.getDeviceCount()
    
    suspend fun getDeviceCountByThreatLevel(threatLevel: ThreatLevel): Int = 
        stingrayDeviceDao.getDeviceCountByThreatLevel(threatLevel)
    
    suspend fun clearAllData(): Int = stingrayDeviceDao.deleteAllDevices()
}

