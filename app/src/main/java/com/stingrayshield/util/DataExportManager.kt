package com.stingrayshield.util

import android.content.Context
import android.os.Environment
import android.util.Log
import android.widget.Toast
import com.stingrayshield.domain.model.CellTower
import com.stingrayshield.domain.model.DetectionEvent
import com.stingrayshield.domain.model.StingrayDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for exporting application data in various formats
 */
@Singleton
class DataExportManager @Inject constructor(
    private val context: Context
) {
    
    companion object {
        private const val TAG = "DataExportManager"
        private const val EXPORT_DIR_NAME = "StingrayShield/Exports"
        private const val DATE_FORMAT = "yyyyMMdd_HHmmss"
    }
    
    /**
     * Export detection events to CSV
     */
    suspend fun exportDetectionEventsToCsv(events: List<DetectionEvent>): String? = withContext(Dispatchers.IO) {
        return@withContext try {
            val exportDir = getExportDirectory() ?: return@withContext null
            val timestamp = SimpleDateFormat(DATE_FORMAT, Locale.getDefault()).format(Date())
            val file = File(exportDir, "detection_events_$timestamp.csv")
            
            file.writeText(buildString {
                appendLine("id,anomalyType,threatLevel,timestamp,description,latitude,longitude,cellId")
                events.forEach { event ->
                    appendLine("${event.id},${event.anomalyType},${event.threatLevel},${event.timestamp},\"${event.description}\",${event.latitude},${event.longitude},${event.cellId}")
                }
            })
            
            Log.d(TAG, "Exported ${events.size} events to ${file.absolutePath}")
            file.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting events to CSV", e)
            null
        }
    }
    
    /**
     * Export cell towers to CSV
     */
    suspend fun exportCellTowersToCsv(towers: List<CellTower>): String? = withContext(Dispatchers.IO) {
        return@withContext try {
            val exportDir = getExportDirectory() ?: return@withContext null
            val timestamp = SimpleDateFormat(DATE_FORMAT, Locale.getDefault()).format(Date())
            val file = File(exportDir, "cell_towers_$timestamp.csv")
            
            file.writeText(buildString {
                appendLine("id,cellId,lac,mcc,mnc,networkType,signalStrength,latitude,longitude,isPrimary")
                towers.forEach { tower ->
                    appendLine("${tower.id},${tower.cellId},${tower.locationAreaCode},${tower.mobileCountryCode},${tower.mobileNetworkCode},${tower.networkType},${tower.signalStrength},${tower.latitude},${tower.longitude},${tower.isPrimary}")
                }
            })
            
            Log.d(TAG, "Exported ${towers.size} towers to ${file.absolutePath}")
            file.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting towers to CSV", e)
            null
        }
    }
    
    /**
     * Export stingray devices to CSV
     */
    suspend fun exportStingrayDevicesToCsv(devices: List<StingrayDevice>): String? = withContext(Dispatchers.IO) {
        return@withContext try {
            val exportDir = getExportDirectory() ?: return@withContext null
            val timestamp = SimpleDateFormat(DATE_FORMAT, Locale.getDefault()).format(Date())
            val file = File(exportDir, "stingray_devices_$timestamp.csv")
            
            file.writeText(buildString {
                appendLine("id,cellId,threatLevel,detectionTimestamp,latitude,longitude,networkType")
                devices.forEach { device ->
                    appendLine("${device.id},${device.cellId},${device.threatLevel},${device.detectionTimestamp},${device.latitude},${device.longitude},${device.networkType}")
                }
            })
            
            Log.d(TAG, "Exported ${devices.size} devices to ${file.absolutePath}")
            file.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting devices to CSV", e)
            null
        }
    }
    
    /**
     * Create a full export package with all data
     */
    suspend fun createFullExportPackage(
        events: List<DetectionEvent>,
        towers: List<CellTower>,
        devices: List<StingrayDevice>
    ): List<String> = withContext(Dispatchers.IO) {
        val filePaths = mutableListOf<String>()
        
        exportDetectionEventsToCsv(events)?.let { filePaths.add(it) }
        exportCellTowersToCsv(towers)?.let { filePaths.add(it) }
        exportStingrayDevicesToCsv(devices)?.let { filePaths.add(it) }
        
        return@withContext filePaths
    }
    
    /**
     * Show export result to user
     */
    fun showExportResult(context: Context, filePaths: List<String>) {
        val message = if (filePaths.isNotEmpty()) {
            "Exported ${filePaths.size} file(s) to Documents/$EXPORT_DIR_NAME"
        } else {
            "Export failed - no files created"
        }
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
    
    /**
     * Export detection events as CSV (no params version)
     */
    suspend fun exportEventsAsCsv(): String? {
        return try {
            Log.d(TAG, "Exporting events as CSV")
            // TODO: Implement CSV export
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting CSV", e)
            null
        }
    }
    
    /**
     * Export detection events as JSON
     */
    suspend fun exportEventsAsJson(): String? {
        return try {
            Log.d(TAG, "Exporting events as JSON")
            // TODO: Implement JSON export
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting JSON", e)
            null
        }
    }
    
    /**
     * Export cell tower data as CSV (no params version)
     */
    suspend fun exportTowersAsCsv(): String? {
        return try {
            Log.d(TAG, "Exporting towers as CSV")
            // TODO: Implement CSV export
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting towers CSV", e)
            null
        }
    }
    
    /**
     * Export cell tower data as JSON
     */
    suspend fun exportTowersAsJson(): String? {
        return try {
            Log.d(TAG, "Exporting towers as JSON")
            // TODO: Implement JSON export
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting towers JSON", e)
            null
        }
    }
    
    /**
     * Export all data as a zip archive
     */
    suspend fun exportAllAsZip(): String? {
        return try {
            Log.d(TAG, "Exporting all data as ZIP")
            // TODO: Implement ZIP export
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting ZIP", e)
            null
        }
    }
    
    /**
     * Get the export directory
     */
    private fun getExportDirectory(): File? {
        return try {
            val documentsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            documentsDir?.let {
                val exportDir = File(it, EXPORT_DIR_NAME)
                if (!exportDir.exists()) {
                    exportDir.mkdirs()
                }
                exportDir
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting export directory", e)
            null
        }
    }
}
