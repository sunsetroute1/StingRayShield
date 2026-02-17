package com.stingrayshield.ui.screens.devices

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.stingrayshield.domain.model.ThreatLevel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Screen displaying all identified stingray devices with hardware/software information
 */
@Composable
fun StingrayDevicesScreen(
    onNavigateBack: () -> Unit,
    viewModel: StingrayDevicesViewModel = hiltViewModel()
) {
    val devices by viewModel.devices.collectAsState(initial = emptyList())
    val isLoading by viewModel.isLoading.collectAsState(initial = false)
    
    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Identified Stingray Devices") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (devices.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No stingray devices identified yet",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(devices) { device ->
                    StingrayDeviceCard(
                        device = device,
                        onDelete = { viewModel.deleteDevice(device.id) },
                        onConfirm = { viewModel.confirmDevice(device.id) },
                        onMarkFalsePositive = { viewModel.markAsFalsePositive(device.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun StingrayDeviceCard(
    device: com.stingrayshield.domain.model.StingrayDevice,
    onDelete: () -> Unit,
    onConfirm: () -> Unit,
    onMarkFalsePositive: () -> Unit
) {
    val threatColor = when (device.threatLevel) {
        ThreatLevel.CRITICAL -> Color(0xFFD32F2F)
        ThreatLevel.HIGH -> Color(0xFFF57C00)
        ThreatLevel.MEDIUM -> Color(0xFFFFA000)
        ThreatLevel.LOW -> Color(0xFF1976D2)
        ThreatLevel.NONE -> Color(0xFF388E3C)
    }
    
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault())
    val dateString = dateFormat.format(Date(device.detectionTimestamp))
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with threat level
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = device.getDeviceIdentifier(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Threat Level: ${device.threatLevel.name}",
                        color = threatColor,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Detected: $dateString",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Divider(modifier = Modifier.padding(vertical = 12.dp))
            
            // Network Information
            Text(
                text = "Network Information",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            InfoRow("Network Type", device.getNetworkGeneration())
            device.mobileCountryCode?.let { InfoRow("MCC", it) }
            device.mobileNetworkCode?.let { InfoRow("MNC", it) }
            device.cellId?.let { InfoRow("Cell ID", it.toString()) }
            device.locationAreaCode?.let { InfoRow("LAC/TAC", it.toString()) }
            
            // Signal Characteristics
            if (device.signalStrength != null || device.rsrp != null || device.rsrq != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Signal Characteristics",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                device.signalStrength?.let { InfoRow("Signal Strength", "${it} dBm") }
                device.rsrp?.let { InfoRow("RSRP (LTE/5G)", "${it} dBm") }
                device.rsrq?.let { InfoRow("RSRQ (LTE/5G)", "${it} dB") }
                device.sinr?.let { InfoRow("SINR", "${it} dB") }
                device.rssi?.let { InfoRow("RSSI (2G/3G)", "${it} dBm") }
                device.arfcn?.let { InfoRow("ARFCN (2G)", it.toString()) }
                device.uarfcn?.let { InfoRow("UARFCN (3G)", it.toString()) }
                device.psc?.let { InfoRow("PSC (3G)", it.toString()) }
                device.pci?.let { InfoRow("PCI (LTE)", it.toString()) }
                device.nci?.let { InfoRow("NCI (5G)", it.toString()) }
            }
            
            // Hardware/Software Identification
            if (device.hardwareVendor != null || device.hardwareModel != null || 
                device.softwareVersion != null || device.firmwareVersion != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Hardware/Software Identification",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                device.hardwareVendor?.let { InfoRow("Hardware Vendor", it) }
                device.hardwareModel?.let { InfoRow("Hardware Model", it) }
                device.firmwareVersion?.let { InfoRow("Firmware Version", it) }
                device.softwareVersion?.let { InfoRow("Software Version", it) }
                device.protocolVersion?.let { InfoRow("Protocol Version", it) }
                device.encryptionType?.let { InfoRow("Encryption Type", it) }
            }
            
            // Device Fingerprint
            device.deviceFingerprint?.let { fingerprint ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Device Fingerprint",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = fingerprint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Behavior Pattern
            device.behaviorPattern?.let { behavior ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Behavior Pattern",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = behavior,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            // Location
            if (device.latitude != null && device.longitude != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Location",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                InfoRow("Latitude", String.format("%.6f", device.latitude))
                InfoRow("Longitude", String.format("%.6f", device.longitude))
                device.locationAccuracy?.let { 
                    InfoRow("Accuracy", "${it}m")
                }
            }
            
            // Action Buttons
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!device.isConfirmed) {
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Confirm")
                    }
                }
                
                if (!device.isFalsePositive) {
                    OutlinedButton(
                        onClick = onMarkFalsePositive,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("False Positive")
                    }
                }
                
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.align(Alignment.CenterVertically)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

