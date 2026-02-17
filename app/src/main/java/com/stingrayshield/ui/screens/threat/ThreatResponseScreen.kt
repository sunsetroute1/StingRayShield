package com.stingrayshield.ui.screens.threat

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stingrayshield.domain.model.DetectionEvent
import com.stingrayshield.domain.model.ThreatLevel
import com.stingrayshield.ui.theme.statusAlert
import com.stingrayshield.ui.theme.statusDanger
import com.stingrayshield.util.CellTowerLocationEstimator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Stingray/IMSI Catcher database lookup links
 */
object StingrayDatabases {
    val databases = listOf(
        DatabaseLink(
            name = "IMSI Catcher Wiki",
            description = "Community database of known IMSI catcher signatures",
            url = "https://en.wikipedia.org/wiki/IMSI-catcher",
            searchUrl = null
        ),
        DatabaseLink(
            name = "EFF Surveillance Self-Defense",
            description = "Electronic Frontier Foundation's guide to cell-site simulators",
            url = "https://ssd.eff.org/module/problem-mobile-phones",
            searchUrl = null
        ),
        DatabaseLink(
            name = "OpenCellID",
            description = "Open database of cell tower locations worldwide",
            url = "https://opencellid.org",
            searchUrl = "https://opencellid.org/#zoom=16&lat={LAT}&lon={LNG}"
        ),
        DatabaseLink(
            name = "Mozilla Location Service",
            description = "Mozilla's cell tower and wifi location database",
            url = "https://location.services.mozilla.com",
            searchUrl = null
        ),
        DatabaseLink(
            name = "CellMapper",
            description = "Crowdsourced cellular tower mapping",
            url = "https://www.cellmapper.net",
            searchUrl = "https://www.cellmapper.net/map?MCC={MCC}&MNC={MNC}&type=LTE"
        ),
        DatabaseLink(
            name = "AIMSICD Project",
            description = "Android IMSI Catcher Detector research and signatures",
            url = "https://github.com/CellularPrivacy/Android-IMSI-Catcher-Detector",
            searchUrl = null
        ),
        DatabaseLink(
            name = "SnoopSnitch",
            description = "Mobile network security analysis tool data",
            url = "https://opensource.srlabs.de/projects/snoopsnitch",
            searchUrl = null
        )
    )
}

data class DatabaseLink(
    val name: String,
    val description: String,
    val url: String,
    val searchUrl: String?
)

/**
 * Threat response screen showing full threat details and response options
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreatResponseScreen(
    event: DetectionEvent?,
    onNavigateBack: () -> Unit,
    viewModel: ThreatResponseViewModel
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()
    
    val collectedData by viewModel.collectedThreatData.collectAsState()
    var showDisconnectDialog by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    
    val isCritical = event?.threatLevel == ThreatLevel.CRITICAL
    val isHigh = event?.threatLevel == ThreatLevel.HIGH
    val threatColor = if (isCritical) statusDanger else statusAlert
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        if (isCritical) "🚨 CRITICAL THREAT" else "⚠️ HIGH THREAT",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = threatColor.copy(alpha = 0.1f)
                )
            )
        }
    ) { paddingValues ->
        if (event == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("No threat data available")
            }
            return@Scaffold
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            // Pulsing threat indicator
            ThreatIndicator(
                threatLevel = event.threatLevel,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Threat summary
            Text(
                text = event.description,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Quick Actions
            Text(
                text = "🛡️ QUICK ACTIONS",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActionButton(
                    text = "Disconnect",
                    icon = Icons.Default.AirplanemodeActive,
                    color = statusDanger,
                    modifier = Modifier.weight(1f),
                    onClick = { showDisconnectDialog = true }
                )
                
                ActionButton(
                    text = "Share",
                    icon = Icons.Default.Share,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                    onClick = { shareThreatReport(context, event, collectedData) }
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Collected threat data
            CollectedDataSection(
                event = event,
                collectedData = collectedData,
                onCopyData = { data ->
                    clipboardManager.setText(AnnotatedString(data))
                    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Hardware/Device identification section
            DeviceIdentificationSection(
                event = event,
                collectedData = collectedData,
                onOpenDatabase = { url ->
                    openUrl(context, url)
                }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Location information
            LocationSection(
                event = event,
                onOpenMap = {
                    if (event.latitude != 0.0 && event.longitude != 0.0) {
                        val mapUrl = "https://www.openstreetmap.org/?mlat=${event.latitude}&mlon=${event.longitude}&zoom=17"
                        openUrl(context, mapUrl)
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Database lookup section
            DatabaseLookupSection(
                event = event,
                onOpenDatabase = { url -> openUrl(context, url) }
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
        
        // Disconnect confirmation dialog
        if (showDisconnectDialog) {
            AlertDialog(
                onDismissRequest = { showDisconnectDialog = false },
                icon = { Icon(Icons.Default.AirplanemodeActive, contentDescription = null, tint = statusDanger) },
                title = { Text("Disconnect from Network?", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text("This will enable Airplane Mode to immediately disconnect from the suspicious cell tower.")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Note: On most devices, you'll need to manually enable Airplane Mode in Settings.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showDisconnectDialog = false
                            openAirplaneModeSettings(context)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = statusDanger)
                    ) {
                        Text("Open Settings")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDisconnectDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun ThreatIndicator(
    threatLevel: ThreatLevel,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    val color = when (threatLevel) {
        ThreatLevel.CRITICAL -> statusDanger
        ThreatLevel.HIGH -> statusAlert
        else -> Color.Yellow
    }
    
    Box(
        modifier = modifier
            .size((80 * scale).dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.2f))
            .border(4.dp, color, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = color
        )
    }
}

@Composable
fun ActionButton(
    text: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun CollectedDataSection(
    event: DetectionEvent,
    collectedData: ThreatData?,
    onCopyData: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📊 COLLECTED DATA",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = { onCopyData(formatThreatDataForCopy(event, collectedData)) }
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                }
            }
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Detection info
            DataRow("Detection Time", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(event.timestamp)))
            DataRow("Threat Level", event.threatLevel.name)
            DataRow("Anomaly Type", event.anomalyType.name)
            
            event.cellId?.let { DataRow("Cell ID", it.toString()) }
            event.signalStrength?.let { DataRow("Signal Strength", "$it dBm") }
            event.locationAreaCode?.let { DataRow("LAC/TAC", it.toString()) }
            
            // Additional collected data
            collectedData?.let { data ->
                Spacer(modifier = Modifier.height(8.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))
                
                data.mcc?.let { DataRow("MCC", it) }
                data.mnc?.let { DataRow("MNC", it) }
                data.carrierName?.let { DataRow("Carrier", it) }
                data.networkType?.let { DataRow("Network Type", it) }
                data.pci?.let { DataRow("PCI", it.toString()) }
                data.arfcn?.let { DataRow("ARFCN/EARFCN", it.toString()) }
                data.bandwidthKhz?.let { DataRow("Bandwidth", "${it / 1000} MHz") }
            }
        }
    }
}

@Composable
fun DataRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun DeviceIdentificationSection(
    event: DetectionEvent,
    collectedData: ThreatData?,
    onOpenDatabase: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = statusDanger.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Security,
                    contentDescription = null,
                    tint = statusDanger,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "DEVICE IDENTIFICATION",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = statusDanger
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Device fingerprint
            val fingerprint = generateDeviceFingerprint(event, collectedData)
            
            Text(
                text = "Device Fingerprint:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = fingerprint,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surface,
                        RoundedCornerShape(4.dp)
                    )
                    .padding(8.dp)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "Use this fingerprint to check against known stingray device databases:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Quick lookup button
            OutlinedButton(
                onClick = {
                    // Search for this fingerprint pattern
                    val searchQuery = "IMSI catcher ${collectedData?.networkType ?: "LTE"} cell simulator"
                    val searchUrl = "https://www.google.com/search?q=${Uri.encode(searchQuery)}"
                    onOpenDatabase(searchUrl)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Search, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Search for Similar Devices")
            }
        }
    }
}

@Composable
fun LocationSection(
    event: DetectionEvent,
    onOpenMap: () -> Unit
) {
    if (event.latitude == 0.0 && event.longitude == 0.0) return
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "LOCATION DATA",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            DataRow("Latitude", "%.6f".format(event.latitude))
            DataRow("Longitude", "%.6f".format(event.longitude))
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Button(
                onClick = onOpenMap,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.OpenInNew, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("View on Map")
            }
        }
    }
}

@Composable
fun DatabaseLookupSection(
    event: DetectionEvent,
    onOpenDatabase: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Search, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "DATABASE LOOKUPS",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "Check cell tower data against known databases:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            StingrayDatabases.databases.forEach { database ->
                DatabaseLinkCard(
                    database = database,
                    event = event,
                    onClick = {
                        val url = if (database.searchUrl != null && event.latitude != 0.0) {
                            database.searchUrl
                                .replace("{LAT}", event.latitude.toString())
                                .replace("{LNG}", event.longitude.toString())
                                .replace("{MCC}", "310") // Default US MCC
                                .replace("{MNC}", "260") // Default T-Mobile MNC
                        } else {
                            database.url
                        }
                        onOpenDatabase(url)
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun DatabaseLinkCard(
    database: DatabaseLink,
    event: DetectionEvent,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = database.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = database.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Default.OpenInNew,
                contentDescription = "Open",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// Helper functions
private fun generateDeviceFingerprint(event: DetectionEvent, data: ThreatData?): String {
    val parts = mutableListOf<String>()
    
    event.cellId?.let { parts.add("CID:$it") }
    event.locationAreaCode?.let { parts.add("LAC:$it") }
    data?.mcc?.let { parts.add("MCC:$it") }
    data?.mnc?.let { parts.add("MNC:$it") }
    data?.pci?.let { parts.add("PCI:$it") }
    data?.networkType?.let { parts.add("NET:$it") }
    event.signalStrength?.let { parts.add("SIG:${it}dBm") }
    
    return if (parts.isNotEmpty()) {
        parts.joinToString("-")
    } else {
        "UNKNOWN-${event.id}"
    }
}

private fun formatThreatDataForCopy(event: DetectionEvent, data: ThreatData?): String {
    return buildString {
        appendLine("=== STINGRAY THREAT REPORT ===")
        appendLine()
        appendLine("Detection Time: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(event.timestamp))}")
        appendLine("Threat Level: ${event.threatLevel}")
        appendLine("Anomaly Type: ${event.anomalyType}")
        appendLine()
        appendLine("--- Cell Tower Info ---")
        event.cellId?.let { appendLine("Cell ID: $it") }
        event.locationAreaCode?.let { appendLine("LAC/TAC: $it") }
        event.signalStrength?.let { appendLine("Signal: $it dBm") }
        data?.mcc?.let { appendLine("MCC: $it") }
        data?.mnc?.let { appendLine("MNC: $it") }
        data?.carrierName?.let { appendLine("Carrier: $it") }
        data?.networkType?.let { appendLine("Network: $it") }
        data?.pci?.let { appendLine("PCI: $it") }
        appendLine()
        appendLine("--- Location ---")
        if (event.latitude != 0.0 && event.longitude != 0.0) {
            appendLine("Lat: ${event.latitude}")
            appendLine("Lng: ${event.longitude}")
            appendLine("Map: https://www.openstreetmap.org/?mlat=${event.latitude}&mlon=${event.longitude}&zoom=17")
        }
        appendLine()
        appendLine("--- Description ---")
        appendLine(event.description)
        appendLine()
        appendLine("Device Fingerprint: ${generateDeviceFingerprint(event, data)}")
        appendLine()
        appendLine("Generated by StingrayShield")
    }
}

private fun shareThreatReport(context: Context, event: DetectionEvent, data: ThreatData?) {
    val shareText = formatThreatDataForCopy(event, data)
    
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "🚨 Stingray Threat Report - ${event.threatLevel}")
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share Threat Report"))
}

private fun openUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Cannot open URL", Toast.LENGTH_SHORT).show()
    }
}

private fun openAirplaneModeSettings(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS)
        context.startActivity(intent)
    } catch (e: Exception) {
        // Fallback to general settings
        try {
            val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS)
            context.startActivity(intent)
        } catch (e2: Exception) {
            Toast.makeText(context, "Please enable Airplane Mode in Settings", Toast.LENGTH_LONG).show()
        }
    }
}

/**
 * Data class holding collected threat information
 */
data class ThreatData(
    val mcc: String? = null,
    val mnc: String? = null,
    val carrierName: String? = null,
    val networkType: String? = null,
    val pci: Int? = null,
    val arfcn: Int? = null,
    val bandwidthKhz: Int? = null,
    val timingAdvance: Int? = null
)

