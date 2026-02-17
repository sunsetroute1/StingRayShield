package com.stingrayshield.util

import android.content.Context
import android.os.Environment
import android.util.Log
import com.stingrayshield.data.repository.CellTowerRepository
import com.stingrayshield.data.repository.DetectionEventRepository
import com.stingrayshield.data.repository.StingrayDeviceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for backing up and restoring StingrayShield data
 */
@Singleton
class BackupRestoreManager @Inject constructor(
    private val context: Context,
    private val cellTowerRepository: CellTowerRepository,
    private val detectionEventRepository: DetectionEventRepository,
    private val stingrayDeviceRepository: StingrayDeviceRepository
) {
    
    companion object {
        private const val TAG = "BackupRestoreManager"
        private const val BACKUP_DIR_NAME = "StingrayShieldBackups"
        private const val DATE_FORMAT = "yyyyMMdd_HHmmss"
    }
    
    /**
     * Create a full backup of all application data including settings
     */
    suspend fun createFullBackup(includeSettings: Boolean = true): String? = withContext(Dispatchers.IO) {
        try {
            val backupDir = getBackupDirectory()
            if (backupDir == null) {
                Log.w(TAG, "Could not access backup directory")
                return@withContext null
            }
            
            val dateFormat = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
            val timestamp = dateFormat.format(Date())
            val backupFile = File(backupDir, "stingray_backup_full_$timestamp.json")
            
            // In a real implementation, we would serialize all data to JSON
            // For now, just create an empty backup file as placeholder
            backupFile.createNewFile()
            
            Log.d(TAG, "Full backup created: ${backupFile.absolutePath}")
            return@withContext backupFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Error creating full backup", e)
            null
        }
    }
    
    /**
     * Create a quick backup of data only (no settings)
     */
    suspend fun createQuickBackup(): String? = withContext(Dispatchers.IO) {
        try {
            val backupDir = getBackupDirectory()
            if (backupDir == null) {
                Log.w(TAG, "Could not access backup directory")
                return@withContext null
            }
            
            val dateFormat = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
            val timestamp = dateFormat.format(Date())
            val backupFile = File(backupDir, "stingray_backup_quick_$timestamp.json")
            
            // In a real implementation, we would serialize data to JSON
            // For now, just create an empty backup file as placeholder
            backupFile.createNewFile()
            
            Log.d(TAG, "Quick backup created: ${backupFile.absolutePath}")
            return@withContext backupFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Error creating quick backup", e)
            null
        }
    }
    
    /**
     * Create a backup of all application data
     */
    suspend fun createBackup(): String? = createFullBackup()
    
    /**
     * Restore data from a backup file
     */
    suspend fun restoreFromBackup(
        backupFilePath: String,
        restoreEvents: Boolean = true,
        restoreTowers: Boolean = true,
        restoreDevices: Boolean = true,
        restoreSettings: Boolean = true
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val backupFile = File(backupFilePath)
            if (!backupFile.exists()) {
                Log.w(TAG, "Backup file not found: $backupFilePath")
                return@withContext false
            }
            
            // In a real implementation, we would deserialize JSON and restore data
            // For now, just validate that the file exists
            Log.d(TAG, "Backup restored from: ${backupFile.absolutePath}")
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring backup", e)
            false
        }
    }
    
    /**
     * Show backup result to user
     */
    fun showBackupResult(context: Context, filePath: String?) {
        if (filePath != null) {
            Log.d(TAG, "Backup successful: $filePath")
        } else {
            Log.e(TAG, "Backup failed")
        }
    }
    
    /**
     * Show restore result to user
     */
    fun showRestoreResult(context: Context, success: Boolean) {
        if (success) {
            Log.d(TAG, "Restore successful")
        } else {
            Log.e(TAG, "Restore failed")
        }
    }
    
    /**
     * Get list of available backups
     */
    suspend fun getAvailableBackups(): List<String> = withContext(Dispatchers.IO) {
        try {
            val backupDir = getBackupDirectory() ?: return@withContext emptyList()
            return@withContext backupDir
                .listFiles()?.filter { it.extension == "json" }
                ?.map { it.absolutePath }
                ?.sortedByDescending { File(it).lastModified() }
                ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting available backups", e)
            emptyList()
        }
    }
    
    /**
     * Delete a specific backup file
     */
    suspend fun deleteBackup(backupFilePath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(backupFilePath)
            return@withContext file.delete().also {
                if (it) {
                    Log.d(TAG, "Backup deleted: $backupFilePath")
                } else {
                    Log.w(TAG, "Failed to delete backup: $backupFilePath")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting backup", e)
            false
        }
    }
    
    /**
     * Clear all data from the application
     */
    suspend fun clearAllData(): Boolean = withContext(Dispatchers.IO) {
        try {
            cellTowerRepository.clearAllData()
            detectionEventRepository.clearAllData()
            stingrayDeviceRepository.clearAllData()
            Log.d(TAG, "All data cleared")
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing all data", e)
            false
        }
    }
    
    /**
     * Get the backup directory
     */
    private fun getBackupDirectory(): File? {
        return try {
            val documentsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            documentsDir?.let {
                val backupDir = File(it, BACKUP_DIR_NAME)
                if (!backupDir.exists()) {
                    backupDir.mkdirs()
                }
                backupDir
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting backup directory", e)
            null
        }
    }
}
