package com.stingrayshield.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SettingsApplications
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SignalCellular4Bar
import androidx.compose.material.icons.filled.SlowMotionVideo
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.ui.platform.LocalUriHandler
import com.stingrayshield.util.DebugLog

/**
 * Settings screen allowing user to configure detector and app behavior
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToStingrayDevices: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Detection settings section
        SettingsSection(
            title = "Detection Settings",
            icon = Icons.Default.Security
        ) {
            SettingsSwitch(
                title = "Signal Strength Anomalies",
                description = "Detect sudden changes in signal strength",
                checked = uiState.detectSignalStrengthAnomalies,
                onCheckedChange = { viewModel.updateSetting(SettingsViewModel.KEY_DETECT_SIGNAL_STRENGTH, it) }
            )
            
            SettingsSwitch(
                title = "Tower Info Consistency",
                description = "Detect inconsistencies in cell tower information",
                checked = uiState.detectTowerInfoConsistency,
                onCheckedChange = { viewModel.updateSetting(SettingsViewModel.KEY_DETECT_TOWER_INFO, it) }
            )
            
            SettingsSwitch(
                title = "Neighboring Cell Info",
                description = "Analyze neighboring cell information for anomalies",
                checked = uiState.detectNeighborCellInfo,
                onCheckedChange = { viewModel.updateSetting(SettingsViewModel.KEY_DETECT_NEIGHBOR_CELLS, it) }
            )
            
            SettingsSwitch(
                title = "Silent SMS Detection",
                description = "Detect silent SMS used for tracking",
                checked = uiState.detectSilentSms,
                onCheckedChange = { viewModel.updateSetting(SettingsViewModel.KEY_DETECT_SILENT_SMS, it) }
            )
            
            SettingsSwitch(
                title = "Femtocell Detection",
                description = "Detect suspicious femtocells",
                checked = uiState.detectFemtocell,
                onCheckedChange = { viewModel.updateSetting(SettingsViewModel.KEY_DETECT_FEMTOCELL, it) }
            )
            
            SettingsSwitch(
                title = "Encryption Downgrade",
                description = "Detect 2G/3G/4G/5G downgrade attacks (SnoopSnitch C1-style). Alerts when connected to weaker RAT while better available.",
                checked = uiState.detectEncryptionDowngrade,
                onCheckedChange = { viewModel.updateSetting(SettingsViewModel.KEY_DETECT_ENCRYPTION, it) }
            )
            
            SettingsSwitch(
                title = "Location Anomalies",
                description = "Detect location-based anomalies in cell towers",
                checked = uiState.detectLocationAnomaly,
                onCheckedChange = { viewModel.updateSetting(SettingsViewModel.KEY_DETECT_LOCATION, it) }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Notification settings section
        SettingsSection(
            title = "Notification Settings",
            icon = Icons.Default.Notifications
        ) {
            SettingsSwitch(
                title = "High Threat Notifications",
                description = "Notify on high threat level detections",
                checked = uiState.notifyOnHighThreats,
                onCheckedChange = { viewModel.updateSetting(SettingsViewModel.KEY_NOTIFY_HIGH_THREATS, it) }
            )
            
            SettingsSwitch(
                title = "Medium Threat Notifications",
                description = "Notify on medium threat level detections",
                checked = uiState.notifyOnMediumThreats,
                onCheckedChange = { viewModel.updateSetting(SettingsViewModel.KEY_NOTIFY_MEDIUM_THREATS, it) }
            )
            
            SettingsSwitch(
                title = "Low Threat Notifications",
                description = "Notify on low threat level detections",
                checked = uiState.notifyOnLowThreats,
                onCheckedChange = { viewModel.updateSetting(SettingsViewModel.KEY_NOTIFY_LOW_THREATS, it) }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // App behavior settings
        SettingsSection(
            title = "App Behavior",
            icon = Icons.Default.SettingsApplications
        ) {
            SettingsSwitch(
                title = "Start on Boot",
                description = "Automatically start detector service when device boots",
                checked = uiState.startOnBoot,
                onCheckedChange = { viewModel.updateSetting(SettingsViewModel.KEY_START_ON_BOOT, it) }
            )
            
            SettingsSwitch(
                title = "Dark Theme",
                description = "Use dark theme for the app",
                checked = uiState.darkTheme,
                icon = Icons.Default.Brightness6,
                onCheckedChange = { viewModel.updateSetting(SettingsViewModel.KEY_DARK_THEME, it) }
            )
            
            SettingsSwitch(
                title = "Keep Screen On",
                description = "Prevent screen from turning off while app is in foreground",
                checked = uiState.keepScreenOn,
                icon = Icons.Default.PowerSettingsNew,
                onCheckedChange = { viewModel.updateSetting(SettingsViewModel.KEY_KEEP_SCREEN_ON, it) }
            )
            
            ScanIntervalSetting(
                currentValue = uiState.backgroundScanInterval,
                onValueChange = { viewModel.updateScanInterval(it) }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Stingray Devices section
        SettingsSection(
            title = "Stingray Device Information",
            icon = Icons.Default.CellTower
        ) {
            Button(
                onClick = onNavigateToStingrayDevices,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.CellTower,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text("View Identified Stingray Devices")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Data retention settings
        SettingsSection(
            title = "Data Settings",
            icon = Icons.Default.Storage
        ) {
            DataRetentionSetting(
                currentValue = uiState.dataRetentionPeriod,
                onValueChange = { viewModel.updateDataRetentionPeriod(it) }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = { /* Implement data clear functionality */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text("Clear All Detection Data")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // API Settings section for cell tower location database
        SettingsSection(
            title = "Cell Tower Database",
            icon = Icons.Default.CellTower
        ) {
            val uriHandler = LocalUriHandler.current
            var apiKeyInput by remember(uiState.openCellIdApiKey) { mutableStateOf(uiState.openCellIdApiKey) }
            var showApiKeyDialog by remember { mutableStateOf(false) }

            Text(
                text = "An OpenCellID API token is required to show real cell tower locations on the map. " +
                       "This free service provides a global database of cell tower positions.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Status and action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "OpenCellID API Token",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    if (uiState.openCellIdApiKey.isNotBlank()) {
                        Text(
                            text = "Configured: ${uiState.openCellIdApiKey.take(12)}...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Text(
                            text = "Not configured - map will not show towers",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                Button(onClick = { showApiKeyDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.VpnKey,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text(if (uiState.openCellIdApiKey.isNotBlank()) "Change" else "Set Token")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Clickable link to get an API key
            OutlinedButton(
                onClick = { uriHandler.openUri("https://my.opencellid.org/register") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text("Get a FREE API Token at my.opencellid.org")
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Tower data refresh",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Re-download US tower CSV files every ${uiState.csvRefreshIntervalDays} days (first run downloads once; 2/day limit).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Slider(
                    value = uiState.csvRefreshIntervalDays.coerceIn(7, 90).toFloat(),
                    onValueChange = { viewModel.updateSetting(SettingsViewModel.KEY_CSV_REFRESH_INTERVAL_DAYS, it.toInt().coerceIn(7, 90)) },
                    valueRange = 7f..90f,
                    steps = 82  // 83 positions: 7, 8, 9, … 90 (one day per step)
                )
                Text(
                    text = "${uiState.csvRefreshIntervalDays} days",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.width(64.dp)
                )
            }

            if (showApiKeyDialog) {
                AlertDialog(
                    onDismissRequest = { showApiKeyDialog = false },
                    title = { Text("OpenCellID API Token") },
                    text = {
                        Column {
                            Text(
                                "Enter your API token from OpenCellID. This is used to fetch " +
                                "real cell tower locations for the map display.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Don't have one yet? Tap the link below to register for free.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            androidx.compose.material3.OutlinedTextField(
                                value = apiKeyInput,
                                onValueChange = { apiKeyInput = it },
                                label = { Text("API Token") },
                                placeholder = { Text("pk.xxxxxxxxxxxxx") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Register at my.opencellid.org/register",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable {
                                    uriHandler.openUri("https://my.opencellid.org/register")
                                }
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.updateOpenCellIdApiKey(apiKeyInput.trim())
                                showApiKeyDialog = false
                            },
                            enabled = apiKeyInput.isNotBlank()
                        ) {
                            Text("Save")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            apiKeyInput = uiState.openCellIdApiKey
                            showApiKeyDialog = false
                        }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Theme settings
        SettingsSection(
            title = "Appearance",
            icon = Icons.Default.Brightness6
        ) {
            SettingsSwitch(
                title = "Use System Theme",
                description = "Automatically switch between light and dark themes based on system settings",
                checked = uiState.useSystemTheme,
                onCheckedChange = { viewModel.updateUseSystemTheme(it) }
            )

            if (!uiState.useSystemTheme) {
                SettingsSwitch(
                    title = "Dark Theme",
                    description = "Use dark theme for better visibility in low light conditions",
                    checked = uiState.darkTheme,
                    onCheckedChange = { viewModel.updateDarkTheme(it) }
                )
            }

            SettingsSwitch(
                title = "Keep Screen On",
                description = "Prevent screen from turning off while the app is active",
                checked = uiState.keepScreenOn,
                onCheckedChange = { viewModel.updateSetting(SettingsViewModel.KEY_KEEP_SCREEN_ON, it) }
            )
        }

        // Advanced notification settings
        SettingsSection(
            title = "Notification Preferences",
            icon = Icons.Default.Notifications
        ) {
            // Existing notification toggles
            SettingsSwitch(
                title = "High Threat Notifications",
                description = "Notify on high threat level detections",
                checked = uiState.notifyOnHighThreats,
                onCheckedChange = { viewModel.updateSetting(SettingsViewModel.KEY_NOTIFY_HIGH_THREATS, it) }
            )

            SettingsSwitch(
                title = "Medium Threat Notifications",
                description = "Notify on medium threat level detections",
                checked = uiState.notifyOnMediumThreats,
                onCheckedChange = { viewModel.updateSetting(SettingsViewModel.KEY_NOTIFY_MEDIUM_THREATS, it) }
            )

            SettingsSwitch(
                title = "Low Threat Notifications",
                description = "Notify on low threat level detections",
                checked = uiState.notifyOnLowThreats,
                onCheckedChange = { viewModel.updateSetting(SettingsViewModel.KEY_NOTIFY_LOW_THREATS, it) }
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // New customizable notification settings
            SettingsSwitch(
                title = "Enable Vibration",
                description = "Vibrate device for notifications",
                checked = uiState.enableVibration,
                onCheckedChange = { viewModel.updateEnableVibration(it) }
            )

            if (uiState.enableVibration) {
                var showVibrationDialog by remember { mutableStateOf(false) }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showVibrationDialog = true }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Vibration Pattern",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = getVibrationPatternDescription(uiState.vibrationPattern),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.rotate(270f)
                    )
                }

                if (showVibrationDialog) {
                    AlertDialog(
                        onDismissRequest = { showVibrationDialog = false },
                        title = { Text("Select Vibration Pattern") },
                        text = {
                            Column {
                                val patterns = listOf(
                                    "default" to "Default pattern",
                                    "long" to "Long vibration",
                                    "short" to "Short vibration",
                                    "urgent" to "Urgent pattern"
                                )

                                patterns.forEach { (pattern, description) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.updateVibrationPattern(pattern)
                                                showVibrationDialog = false
                                            }
                                            .padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = uiState.vibrationPattern == pattern,
                                            onClick = {
                                                viewModel.updateVibrationPattern(pattern)
                                                showVibrationDialog = false
                                            }
                                        )
                                        Column(modifier = Modifier.padding(start = 8.dp)) {
                                            Text(text = description)
                                            if (pattern == uiState.vibrationPattern) {
                                                Text(
                                                    text = getVibrationPatternDescription(pattern),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showVibrationDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            SettingsSwitch(
                title = "Enable Sound",
                description = "Play notification sounds",
                checked = uiState.enableSound,
                onCheckedChange = { viewModel.updateEnableSound(it) }
            )

            if (uiState.enableSound) {
                var showSoundDialog by remember { mutableStateOf(false) }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showSoundDialog = true }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Notification Sound",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = getSoundDescription(uiState.notificationSound),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.rotate(270f)
                    )
                }

                if (showSoundDialog) {
                    AlertDialog(
                        onDismissRequest = { showSoundDialog = false },
                        title = { Text("Select Notification Sound") },
                        text = {
                            Column {
                                val sounds = listOf(
                                    "default" to "Default notification",
                                    "alarm" to "Alarm sound",
                                    "silent" to "Silent"
                                )

                                sounds.forEach { (sound, description) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.updateNotificationSound(sound)
                                                showSoundDialog = false
                                            }
                                            .padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = uiState.notificationSound == sound,
                                            onClick = {
                                                viewModel.updateNotificationSound(sound)
                                                showSoundDialog = false
                                            }
                                        )
                                        Text(
                                            text = description,
                                            modifier = Modifier.padding(start = 8.dp)
                                        )
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showSoundDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            SettingsSwitch(
                title = "Enable LED Lights",
                description = "Show notification LED lights",
                checked = uiState.enableLights,
                onCheckedChange = { viewModel.updateEnableLights(it) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Data export settings
        SettingsSection(
            title = "Data Export",
            icon = Icons.Default.Share
        ) {
            Text(
                text = "Export your detection data for analysis or sharing with security researchers.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.exportDetectionEvents() },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text("Export Events")
                }

                OutlinedButton(
                    onClick = { viewModel.exportCellTowers() },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.CellTower,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text("Export Towers")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.exportStingrayDevices() },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text("Export Devices")
                }

                Button(
                    onClick = { viewModel.exportAllData() },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text("Export All")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Files are saved to: Documents/StingrayShield/Exports/",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Backup and restore settings
        SettingsSection(
            title = "Backup & Restore",
            icon = Icons.Default.Security
        ) {
            Text(
                text = "Backup your detection data and settings for safekeeping, or restore from a previous backup.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.createFullBackup() },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text("Full Backup")
                }

                OutlinedButton(
                    onClick = { viewModel.createQuickBackup() },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.CellTower,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text("Quick Backup")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "⚠️ Restore will overwrite existing data. Make sure to backup first!",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Show available backups
            var availableBackups by remember { mutableStateOf<List<java.io.File>>(emptyList()) }
            
            // Load backups when this section is composed
            androidx.compose.runtime.LaunchedEffect(Unit) {
                viewModel.getAvailableBackups { paths ->
                    availableBackups = paths.map { java.io.File(it) }
                }
            }
            
            if (availableBackups.isNotEmpty()) {
                Text(
                    text = "Available backups:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                availableBackups.take(3).forEach { backup ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = backup.name,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1
                            )
                            Text(
                                text = "${backup.length() / 1024} KB • ${java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault()).format(java.util.Date(backup.lastModified()))}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(
                            onClick = { viewModel.restoreFromBackup(backup.absolutePath) },
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text("Restore")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Backups are saved to: Documents/StingrayShield/Backups/",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Reset to defaults button
        Button(
            onClick = { viewModel.resetToDefaults() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Restore,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text("Reset to Default Settings")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Debug Log section
        SettingsSection(
            title = "Debug Log",
            icon = Icons.Default.BugReport
        ) {
            val debugLogs by DebugLog.logs.collectAsState()
            
            Text(
                text = "Recent API and scan activity (${debugLogs.size} entries)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { DebugLog.clear() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Clear Logs")
                }
                
                Button(
                    onClick = { 
                        DebugLog.d("TEST", "Manual test - API key configured: ${uiState.openCellIdApiKey.isNotBlank()}")
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Test Log")
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Log display area
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
            ) {
                if (debugLogs.isEmpty()) {
                    Text(
                        text = "No logs yet. Go to Map to trigger a scan.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.padding(8.dp)
                    ) {
                        items(debugLogs) { log ->
                            val color = when (log.level) {
                                "E" -> MaterialTheme.colorScheme.error
                                "W" -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                            Text(
                                text = "[${log.timestamp}] ${log.tag}: ${log.message}",
                                style = MaterialTheme.typography.bodySmall,
                                color = color,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

/**
 * Section header with title and icon
 */
@Composable
fun SettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Divider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
            
            content()
        }
    }
}

// Helper functions for notification settings
private fun getVibrationPatternDescription(pattern: String): String {
    return when (pattern) {
        "default" -> "Standard vibration pattern"
        "long" -> "Long, continuous vibration"
        "short" -> "Quick, short vibration"
        "urgent" -> "Repeated urgent pattern"
        else -> "Unknown pattern"
    }
}

private fun getSoundDescription(sound: String): String {
    return when (sound) {
        "default" -> "Default notification sound"
        "alarm" -> "Loud alarm sound"
        "silent" -> "No sound"
        else -> "Unknown sound"
    }
}

/**
 * Setting item with a switch toggle
 */
@Composable
fun SettingsSwitch(
    title: String,
    description: String,
    checked: Boolean,
    icon: ImageVector? = null,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 16.dp)
            )
        }
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

/**
 * Scan interval setting with slider
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanIntervalSetting(
    currentValue: Int,
    onValueChange: (Int) -> Unit
) {
    var sliderValue by remember { mutableFloatStateOf(currentValue.toFloat()) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.SlowMotionVideo,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 16.dp)
            )
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Background Scan Interval",
                    style = MaterialTheme.typography.titleMedium
                )
                
                Text(
                    text = "Seconds between background scans: ${sliderValue.toInt()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            onValueChangeFinished = { onValueChange(sliderValue.toInt()) },
            valueRange = 10f..120f,
            steps = 11,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

/**
 * Data retention setting with slider
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataRetentionSetting(
    currentValue: Int,
    onValueChange: (Int) -> Unit
) {
    var sliderValue by remember { mutableFloatStateOf(currentValue.toFloat()) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Storage,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 16.dp)
            )
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Data Retention Period",
                    style = MaterialTheme.typography.titleMedium
                )
                
                val displayText = when {
                    sliderValue.toInt() == 1 -> "1 day"
                    sliderValue.toInt() == 365 -> "1 year"
                    sliderValue.toInt() > 365 -> "Forever"
                    else -> "${sliderValue.toInt()} days"
                }
                
                Text(
                    text = "Keep detection data for: $displayText",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            onValueChangeFinished = { onValueChange(sliderValue.toInt()) },
            valueRange = 1f..366f,
            steps = 12,  // Approximately 30-day steps
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
