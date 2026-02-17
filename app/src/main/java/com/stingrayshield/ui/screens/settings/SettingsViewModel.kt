package com.stingrayshield.ui.screens.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stingrayshield.data.repository.CellTowerRepository
import com.stingrayshield.data.repository.DetectionEventRepository
import com.stingrayshield.data.repository.StingrayDeviceRepository
import com.stingrayshield.util.BackupRestoreManager
import com.stingrayshield.util.OpenCellIdPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the settings screen
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val detectionEventRepository: DetectionEventRepository,
    private val cellTowerRepository: CellTowerRepository,
    private val stingrayDeviceRepository: StingrayDeviceRepository,
    private val backupRestoreManager: BackupRestoreManager,
    private val dataExportManager: com.stingrayshield.util.DataExportManager
) : ViewModel() {

    // Shared preferences for settings (same name as OpenCellIdPreferences for API key)
    private val preferences: SharedPreferences = context.getSharedPreferences(
        OpenCellIdPreferences.PREFERENCES_NAME, Context.MODE_PRIVATE
    )

    // UI state
    private val _uiState = MutableStateFlow(loadSettings())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    /**
     * Load settings from SharedPreferences
     */
    private fun loadSettings(): SettingsUiState {
        return SettingsUiState(
            // Detection settings
            detectSignalStrengthAnomalies = preferences.getBoolean(KEY_DETECT_SIGNAL_STRENGTH, true),
            detectTowerInfoConsistency = preferences.getBoolean(KEY_DETECT_TOWER_INFO, true),
            detectNeighborCellInfo = preferences.getBoolean(KEY_DETECT_NEIGHBOR_CELLS, true),
            detectSilentSms = preferences.getBoolean(KEY_DETECT_SILENT_SMS, true),
            detectFemtocell = preferences.getBoolean(KEY_DETECT_FEMTOCELL, true),
            detectEncryptionDowngrade = preferences.getBoolean(KEY_DETECT_ENCRYPTION, true),
            detectLocationAnomaly = preferences.getBoolean(KEY_DETECT_LOCATION, true),
            
            // Notification settings
            notifyOnHighThreats = preferences.getBoolean(KEY_NOTIFY_HIGH_THREATS, true),
            notifyOnMediumThreats = preferences.getBoolean(KEY_NOTIFY_MEDIUM_THREATS, true),
            notifyOnLowThreats = preferences.getBoolean(KEY_NOTIFY_LOW_THREATS, false),
            enableVibration = preferences.getBoolean(KEY_ENABLE_VIBRATION, true),
            vibrationPattern = preferences.getString(KEY_VIBRATION_PATTERN, "default") ?: "default",
            enableSound = preferences.getBoolean(KEY_ENABLE_SOUND, true),
            notificationSound = preferences.getString(KEY_NOTIFICATION_SOUND, "default") ?: "default",
            enableLights = preferences.getBoolean(KEY_ENABLE_LIGHTS, true),
            
            // App behavior settings
            startOnBoot = preferences.getBoolean(KEY_START_ON_BOOT, false),
            darkTheme = preferences.getBoolean(KEY_DARK_THEME, false),
            useSystemTheme = preferences.getBoolean(KEY_USE_SYSTEM_THEME, true),
            keepScreenOn = preferences.getBoolean(KEY_KEEP_SCREEN_ON, false),
            backgroundScanInterval = preferences.getInt(KEY_SCAN_INTERVAL, 30), // in seconds
            
            // Data retention settings
            dataRetentionPeriod = preferences.getInt(KEY_DATA_RETENTION, 30), // in days
            
            // API settings
            openCellIdApiKey = preferences.getString(OpenCellIdPreferences.KEY_OPENCELLID_API_KEY, "") ?: "",
            csvRefreshIntervalDays = preferences.getInt(OpenCellIdPreferences.KEY_CSV_REFRESH_INTERVAL_DAYS, 30)
        )
    }

    /**
     * Update a setting value
     */
    fun updateSetting(key: String, value: Any) {
        viewModelScope.launch {
            with(preferences.edit()) {
                when (value) {
                    is Boolean -> putBoolean(key, value)
                    is Int -> putInt(key, value)
                    is String -> putString(key, value)
                    is Long -> putLong(key, value)
                    is Float -> putFloat(key, value)
                }
                apply()
            }
            
            // Update UI state based on the changed setting
            when (key) {
                KEY_DETECT_SIGNAL_STRENGTH -> _uiState.update { it.copy(detectSignalStrengthAnomalies = value as Boolean) }
                KEY_DETECT_TOWER_INFO -> _uiState.update { it.copy(detectTowerInfoConsistency = value as Boolean) }
                KEY_DETECT_NEIGHBOR_CELLS -> _uiState.update { it.copy(detectNeighborCellInfo = value as Boolean) }
                KEY_DETECT_SILENT_SMS -> _uiState.update { it.copy(detectSilentSms = value as Boolean) }
                KEY_DETECT_FEMTOCELL -> _uiState.update { it.copy(detectFemtocell = value as Boolean) }
                KEY_DETECT_ENCRYPTION -> _uiState.update { it.copy(detectEncryptionDowngrade = value as Boolean) }
                KEY_DETECT_LOCATION -> _uiState.update { it.copy(detectLocationAnomaly = value as Boolean) }
                
                KEY_NOTIFY_HIGH_THREATS -> _uiState.update { it.copy(notifyOnHighThreats = value as Boolean) }
                KEY_NOTIFY_MEDIUM_THREATS -> _uiState.update { it.copy(notifyOnMediumThreats = value as Boolean) }
                KEY_NOTIFY_LOW_THREATS -> _uiState.update { it.copy(notifyOnLowThreats = value as Boolean) }
                
                KEY_START_ON_BOOT -> _uiState.update { it.copy(startOnBoot = value as Boolean) }
                KEY_DARK_THEME -> _uiState.update { it.copy(darkTheme = value as Boolean) }
                KEY_KEEP_SCREEN_ON -> _uiState.update { it.copy(keepScreenOn = value as Boolean) }
                KEY_SCAN_INTERVAL -> _uiState.update { it.copy(backgroundScanInterval = value as Int) }
                KEY_DATA_RETENTION -> _uiState.update { it.copy(dataRetentionPeriod = value as Int) }
                OpenCellIdPreferences.KEY_OPENCELLID_API_KEY -> _uiState.update { it.copy(openCellIdApiKey = value as String) }
                KEY_CSV_REFRESH_INTERVAL_DAYS -> _uiState.update { it.copy(csvRefreshIntervalDays = value as Int) }
            }
        }
    }
    
    /**
     * Update OpenCellID API key
     */
    fun updateOpenCellIdApiKey(apiKey: String) {
        updateSetting(OpenCellIdPreferences.KEY_OPENCELLID_API_KEY, apiKey)
    }
    
    /**
     * Update scan interval
     */
    fun updateScanInterval(seconds: Int) {
        updateSetting(KEY_SCAN_INTERVAL, seconds)
    }
    
    /**
     * Update data retention period
     */
    fun updateDataRetentionPeriod(days: Int) {
        updateSetting(KEY_DATA_RETENTION, days)
    }

    fun updateDarkTheme(enabled: Boolean) {
        updateSetting(KEY_DARK_THEME, enabled)
    }

    fun updateUseSystemTheme(enabled: Boolean) {
        updateSetting(KEY_USE_SYSTEM_THEME, enabled)
    }

    fun updateEnableVibration(enabled: Boolean) {
        updateSetting(KEY_ENABLE_VIBRATION, enabled)
    }

    fun updateVibrationPattern(pattern: String) {
        updateSetting(KEY_VIBRATION_PATTERN, pattern)
    }

    fun updateEnableSound(enabled: Boolean) {
        updateSetting(KEY_ENABLE_SOUND, enabled)
    }

    fun updateNotificationSound(sound: String) {
        updateSetting(KEY_NOTIFICATION_SOUND, sound)
    }

    fun updateEnableLights(enabled: Boolean) {
        updateSetting(KEY_ENABLE_LIGHTS, enabled)
    }

    /**
     * Reset settings to defaults
     */
    fun resetToDefaults() {
        viewModelScope.launch {
            with(preferences.edit()) {
                // Detection settings
                putBoolean(KEY_DETECT_SIGNAL_STRENGTH, true)
                putBoolean(KEY_DETECT_TOWER_INFO, true)
                putBoolean(KEY_DETECT_NEIGHBOR_CELLS, true)
                putBoolean(KEY_DETECT_SILENT_SMS, true)
                putBoolean(KEY_DETECT_FEMTOCELL, true)
                putBoolean(KEY_DETECT_ENCRYPTION, true)
                putBoolean(KEY_DETECT_LOCATION, true)
                
                // Notification settings
                putBoolean(KEY_NOTIFY_HIGH_THREATS, true)
                putBoolean(KEY_NOTIFY_MEDIUM_THREATS, true)
                putBoolean(KEY_NOTIFY_LOW_THREATS, false)
                putBoolean(KEY_ENABLE_VIBRATION, true)
                putString(KEY_VIBRATION_PATTERN, "default")
                putBoolean(KEY_ENABLE_SOUND, true)
                putString(KEY_NOTIFICATION_SOUND, "default")
                putBoolean(KEY_ENABLE_LIGHTS, true)
                
                // App behavior settings
                putBoolean(KEY_START_ON_BOOT, false)
                putBoolean(KEY_DARK_THEME, false)
                putBoolean(KEY_USE_SYSTEM_THEME, true)
                putBoolean(KEY_KEEP_SCREEN_ON, false)
                putInt(KEY_SCAN_INTERVAL, 30)
                
                // Data retention settings
                putInt(KEY_DATA_RETENTION, 30)
                putInt(OpenCellIdPreferences.KEY_CSV_REFRESH_INTERVAL_DAYS, 30)
                
                apply()
            }
            
            // Reload settings
            _uiState.update { loadSettings() }
        }
    }

    /**
     * Export detection events to CSV
     */
    fun exportDetectionEvents() {
        viewModelScope.launch {
            try {
                val events = getDetectionEvents()
                val filePath = dataExportManager.exportDetectionEventsToCsv(events)
                if (filePath != null) {
                    dataExportManager.showExportResult(context, listOf(filePath))
                }
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Export failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Export cell towers to CSV
     */
    fun exportCellTowers() {
        viewModelScope.launch {
            try {
                val towers = getCellTowers()
                val filePath = dataExportManager.exportCellTowersToCsv(towers)
                if (filePath != null) {
                    dataExportManager.showExportResult(context, listOf(filePath))
                }
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Export failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Export stingray devices to CSV
     */
    fun exportStingrayDevices() {
        viewModelScope.launch {
            try {
                val devices = getStingrayDevices()
                val filePath = dataExportManager.exportStingrayDevicesToCsv(devices)
                if (filePath != null) {
                    dataExportManager.showExportResult(context, listOf(filePath))
                }
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Export failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Export all data (JSON + individual CSVs)
     */
    fun exportAllData() {
        viewModelScope.launch {
            try {
                val events = getDetectionEvents()
                val towers = getCellTowers()
                val devices = getStingrayDevices()

                val filePaths = dataExportManager.createFullExportPackage(events, towers, devices)
                dataExportManager.showExportResult(context, filePaths)
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Export failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Helper methods to get data from repositories
    private suspend fun getDetectionEvents(): List<com.stingrayshield.domain.model.DetectionEvent> {
        return detectionEventRepository.getAllEvents().first()
    }

    private suspend fun getCellTowers(): List<com.stingrayshield.domain.model.CellTower> {
        return cellTowerRepository.getAllCellTowers().first()
    }

    private suspend fun getStingrayDevices(): List<com.stingrayshield.domain.model.StingrayDevice> {
        return stingrayDeviceRepository.getAllDevices().first()
    }

    /**
     * Create a full backup of all app data
     */
    fun createFullBackup() {
        viewModelScope.launch {
            try {
                val filePath = backupRestoreManager.createFullBackup(includeSettings = true)
                backupRestoreManager.showBackupResult(context, filePath)
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Backup failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Create a quick backup (data only, no settings)
     */
    fun createQuickBackup() {
        viewModelScope.launch {
            try {
                val filePath = backupRestoreManager.createQuickBackup()
                backupRestoreManager.showBackupResult(context, filePath)
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Backup failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Restore data from a backup file (placeholder - would need file picker implementation)
     */
    fun restoreFromBackup(backupFilePath: String) {
        viewModelScope.launch {
            try {
                val success = backupRestoreManager.restoreFromBackup(
                    backupFilePath = backupFilePath,
                    restoreEvents = true,
                    restoreTowers = true,
                    restoreDevices = true,
                    restoreSettings = true
                )
                backupRestoreManager.showRestoreResult(context, success)
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Restore failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Get list of available backup files
     */
    fun getAvailableBackups(onResult: (List<String>) -> Unit) {
        viewModelScope.launch {
            try {
                val backups = backupRestoreManager.getAvailableBackups()
                onResult(backups)
            } catch (e: Exception) {
                android.util.Log.e("SettingsViewModel", "Error getting backups", e)
                onResult(emptyList())
            }
        }
    }

    companion object {
        /** Same as [OpenCellIdPreferences.PREFERENCES_NAME]; kept for backward compatibility. */
        const val PREFERENCES_NAME = OpenCellIdPreferences.PREFERENCES_NAME

        // Detection settings keys
        const val KEY_DETECT_SIGNAL_STRENGTH = "detect_signal_strength"
        const val KEY_DETECT_TOWER_INFO = "detect_tower_info"
        const val KEY_DETECT_NEIGHBOR_CELLS = "detect_neighbor_cells"
        const val KEY_DETECT_SILENT_SMS = "detect_silent_sms"
        const val KEY_DETECT_FEMTOCELL = "detect_femtocell"
        const val KEY_DETECT_ENCRYPTION = "detect_encryption"
        const val KEY_DETECT_LOCATION = "detect_location"
        
        // Notification settings keys
        const val KEY_NOTIFY_HIGH_THREATS = "notify_high_threats"
        const val KEY_NOTIFY_MEDIUM_THREATS = "notify_medium_threats"
        const val KEY_NOTIFY_LOW_THREATS = "notify_low_threats"
        const val KEY_ENABLE_VIBRATION = "enable_vibration"
        const val KEY_VIBRATION_PATTERN = "vibration_pattern"
        const val KEY_ENABLE_SOUND = "enable_sound"
        const val KEY_NOTIFICATION_SOUND = "notification_sound"
        const val KEY_ENABLE_LIGHTS = "enable_lights"
        
        // App behavior settings keys
        const val KEY_START_ON_BOOT = "start_on_boot"
        const val KEY_DARK_THEME = "dark_theme"
        const val KEY_USE_SYSTEM_THEME = "use_system_theme"
        const val KEY_KEEP_SCREEN_ON = "keep_screen_on"
        const val KEY_SCAN_INTERVAL = "scan_interval"
        
        // Data retention settings keys
        const val KEY_DATA_RETENTION = "data_retention"
        
        /** Same as [OpenCellIdPreferences.KEY_OPENCELLID_API_KEY]; kept for backward compatibility. */
        const val KEY_OPENCELLID_API_KEY = OpenCellIdPreferences.KEY_OPENCELLID_API_KEY
        const val KEY_CSV_REFRESH_INTERVAL_DAYS = OpenCellIdPreferences.KEY_CSV_REFRESH_INTERVAL_DAYS
    }
}

/**
 * UI state for the settings screen
 */
data class SettingsUiState(
    // Detection settings
    val detectSignalStrengthAnomalies: Boolean = true,
    val detectTowerInfoConsistency: Boolean = true,
    val detectNeighborCellInfo: Boolean = true,
    val detectSilentSms: Boolean = true,
    val detectFemtocell: Boolean = true,
    val detectEncryptionDowngrade: Boolean = true,
    val detectLocationAnomaly: Boolean = true,
    
    // Notification settings
    val notifyOnHighThreats: Boolean = true,
    val notifyOnMediumThreats: Boolean = true,
    val notifyOnLowThreats: Boolean = false,
    val enableVibration: Boolean = true,
    val vibrationPattern: String = "default", // "default", "long", "short", "urgent"
    val enableSound: Boolean = true,
    val notificationSound: String = "default", // "default", "alarm", "silent"
    val enableLights: Boolean = true,
    
    // App behavior settings
    val startOnBoot: Boolean = false,
    val darkTheme: Boolean = false,
    val useSystemTheme: Boolean = true,
    val keepScreenOn: Boolean = false,
    val backgroundScanInterval: Int = 30, // in seconds

    // Data retention settings
    val dataRetentionPeriod: Int = 30, // in days
    
    // API settings
    val openCellIdApiKey: String = "",
    val csvRefreshIntervalDays: Int = 30
)
