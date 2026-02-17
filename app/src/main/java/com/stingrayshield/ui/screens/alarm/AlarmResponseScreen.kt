package com.stingrayshield.ui.screens.alarm

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stingrayshield.domain.model.StingrayDevice
import com.stingrayshield.domain.model.ThreatLevel

/**
 * Emergency response screen shown when a high-threat stingray is detected
 * Provides quick actions: Stop Alarm, Enable Airplane Mode, Shutdown Device
 */
@Composable
fun AlarmResponseScreen(
    device: StingrayDevice,
    onStopAlarm: () -> Unit,
    onEnableAirplaneMode: () -> Unit,
    onShutdownDevice: () -> Unit
) {
    val threatColor = when (device.threatLevel) {
        ThreatLevel.CRITICAL -> Color(0xFFD32F2F)
        ThreatLevel.HIGH -> Color(0xFFF57C00)
        else -> Color(0xFFFFA000)
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Warning icon and title
        Text(
            text = "⚠️ STINGRAY DETECTED ⚠️",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = threatColor,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Text(
            text = "High-threat stingray device identified",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Text(
            text = device.getDeviceIdentifier(),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        // Device information card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Device Information",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                device.networkType?.let {
                    InfoRow("Network Type", it)
                }
                device.cellId?.let {
                    InfoRow("Cell ID", it.toString())
                }
                device.signalStrength?.let {
                    InfoRow("Signal Strength", "${it} dBm")
                }
                device.hardwareVendor?.let {
                    InfoRow("Hardware Vendor", it)
                }
                device.hardwareModel?.let {
                    InfoRow("Hardware Model", it)
                }
                device.softwareVersion?.let {
                    InfoRow("Software Version", it)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Action buttons
        Button(
            onClick = onStopAlarm,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary
            )
        ) {
            Icon(
                imageVector = Icons.Default.VolumeOff,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text("Stop Alarm")
        }
        
        Button(
            onClick = onEnableAirplaneMode,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFF57C00)
            )
        ) {
            Icon(
                imageVector = Icons.Default.AirplanemodeActive,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text("Enable Airplane Mode")
        }
        
        Button(
            onClick = onShutdownDevice,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFD32F2F)
            )
        ) {
            Icon(
                imageVector = Icons.Default.PowerSettingsNew,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text("Shutdown Device")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Warning: Shutting down will disconnect all network connections",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

