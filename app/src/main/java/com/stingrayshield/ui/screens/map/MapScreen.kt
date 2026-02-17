package com.stingrayshield.ui.screens.map

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.stingrayshield.domain.model.CellTower
import com.stingrayshield.domain.model.StingrayDevice
import com.stingrayshield.domain.model.ThreatLevel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    navigateToEventDetails: (Long) -> Unit,
    navigateToCellTowerDetails: (Int) -> Unit,
    viewModel: MapViewModel
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(Unit) {
        Configuration.getInstance().userAgentValue = context.packageName
        viewModel.updateCurrentServingCells()
    }
    
    // Helper: sync map overlays with current tower list (run every time state has tower data)
    fun updateMapMarkers(
        mv: MapView,
        towers: List<CellTower>,
        stingrays: List<StingrayDevice>,
        currentServingCellIds: Set<Int>,
        ctx: Context = context
    ) {
        android.util.Log.d("MapScreen", "=== updateMapMarkers called with ${towers.size} towers ===")
        
        val existingMarkers = mv.overlays.filterIsInstance<Marker>()
        existingMarkers.forEach { mv.overlays.remove(it) }
        android.util.Log.d("MapScreen", "Removed ${existingMarkers.size} existing markers")
        
        var markersAdded = 0
        var firstValidTower: CellTower? = null
        
        towers.forEach { tower ->
            val lat = tower.getDisplayLatitude()
            val lng = tower.getDisplayLongitude()
            
            if (lat == 0.0 && lng == 0.0) {
                android.util.Log.d("MapScreen", "Skipping tower ${tower.cellId} - no coordinates")
                return@forEach
            }
            
            if (firstValidTower == null) firstValidTower = tower
            
            val isConnected = tower.isPrimary || tower.cellId in currentServingCellIds
            
            val marker = Marker(mv)
            marker.position = GeoPoint(lat, lng)
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            marker.title = if (isConnected) "🟢 ${tower.networkType} Tower #${tower.cellId} (connected)" else "${tower.networkType} Tower #${tower.cellId}"
            marker.snippet = "${tower.signalStrength} dBm"
            
            val colorInt = if (isConnected) {
                android.graphics.Color.GREEN
            } else {
                android.graphics.Color.BLUE
            }
            
            val drawable = ContextCompat.getDrawable(ctx, android.R.drawable.ic_menu_compass)?.mutate()
            if (drawable != null) {
                DrawableCompat.setTint(drawable, colorInt)
                marker.icon = drawable
            }
            
            marker.setOnMarkerClickListener { _, _ ->
                viewModel.selectTower(tower)
                true
            }
            
            mv.overlays.add(marker)
            markersAdded++
        }
        
        // Add stingray markers
        stingrays.forEach { device ->
            val lat = device.latitude ?: return@forEach
            val lng = device.longitude ?: return@forEach
            
            val marker = Marker(mv)
            marker.position = GeoPoint(lat, lng)
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            marker.title = "⚠️ STINGRAY DETECTED"
            marker.snippet = "Threat: ${device.threatLevel}"
            
            val drawable = ContextCompat.getDrawable(ctx, android.R.drawable.ic_dialog_alert)?.mutate()
            if (drawable != null) {
                DrawableCompat.setTint(drawable, android.graphics.Color.RED)
                marker.icon = drawable
            }
            
            marker.setOnMarkerClickListener { _, _ ->
                viewModel.selectStingray(device)
                true
            }
            
            mv.overlays.add(marker)
        }
        
        android.util.Log.d("MapScreen", "Added $markersAdded markers, total overlays: ${mv.overlays.size}")
        
        // Center on first tower if map is at default location
        firstValidTower?.let { tower ->
            val center = mv.mapCenter
            if (kotlin.math.abs(center.latitude - 39.8283) < 2.0) {
                android.util.Log.d("MapScreen", "Centering map on first tower")
                mv.controller.setCenter(GeoPoint(tower.getDisplayLatitude(), tower.getDisplayLongitude()))
                mv.controller.setZoom(15.0)
            }
        }
        
        mv.invalidate()
        mv.postInvalidate()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // OSM Map with update callback for markers
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                android.util.Log.d("MapScreen", "=== Creating MapView ===")
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(14.0)
                    controller.setCenter(GeoPoint(39.8283, -98.5795)) // US center default
                    // Load towers for visible center immediately (don't wait for GPS or scroll)
                    viewModel.loadTowersForRegion(39.8283, -98.5795)
                    // Load towers when user pans/zooms: immediate if moved enough, and when map settles (idle)
                    addMapListener(object : MapListener {
                        private var lastLoadedCenter: GeoPoint? = null
                        private val moveThreshold = 0.02
                        private var idleLoadRunnable: Runnable? = null
                        private val idleDelayMs = 400L
                        
                        private fun tryLoadForCenter(lat: Double, lon: Double) {
                            val lastCenter = lastLoadedCenter
                            if (lastCenter == null ||
                                kotlin.math.abs(lat - lastCenter.latitude) > moveThreshold ||
                                kotlin.math.abs(lon - lastCenter.longitude) > moveThreshold) {
                                lastLoadedCenter = GeoPoint(lat, lon)
                                viewModel.loadTowersForRegion(lat, lon)
                            }
                        }
                        
                        private fun scheduleIdleLoad() {
                            idleLoadRunnable?.let { removeCallbacks(it) }
                            idleLoadRunnable = Runnable {
                                val c = mapCenter
                                tryLoadForCenter(c.latitude, c.longitude)
                                idleLoadRunnable = null
                            }
                            postDelayed(idleLoadRunnable!!, idleDelayMs)
                        }
                        
                        override fun onScroll(event: ScrollEvent?): Boolean {
                            val center = mapCenter
                            tryLoadForCenter(center.latitude, center.longitude)
                            scheduleIdleLoad()
                            return true
                        }
                        
                        override fun onZoom(event: ZoomEvent?): Boolean {
                            val center = mapCenter
                            tryLoadForCenter(center.latitude, center.longitude)
                            scheduleIdleLoad()
                            return true
                        }
                    })
                    
                    // Add location overlay
                    try {
                        val locOverlay = MyLocationNewOverlay(GpsMyLocationProvider(ctx), this)
                        locOverlay.enableMyLocation()
                        locOverlay.enableFollowLocation()
                        overlays.add(locOverlay)
                        
                        locOverlay.runOnFirstFix {
                            post {
                                locOverlay.myLocation?.let { loc ->
                                    controller.animateTo(loc)
                                    viewModel.onLocationUpdate(loc.latitude, loc.longitude)
                                    // Load towers for user's location
                                    viewModel.loadTowersForRegion(loc.latitude, loc.longitude)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("MapScreen", "Location error: ${e.message}")
                    }
                }
            },
            update = { mv ->
                val towers = uiState.cellTowers
                val stingrays = uiState.stingrayDevices
                val servingIds = uiState.currentServingCellIds
                android.util.Log.d("MapScreen", "=== AndroidView update: ${towers.size} towers ===")
                updateMapMarkers(mv, towers, stingrays, servingIds)
            }
        )
        
        // Top bar with stats
        Card(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
            )
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                val totalTowers = uiState.cellTowers.size
                val towersWithRealLoc = uiState.cellTowers.count { it.hasRealLocation() }
                val towersWithValidCoords = uiState.cellTowers.count { 
                    it.getDisplayLatitude() != 0.0 || it.getDisplayLongitude() != 0.0 
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (uiState.isLoading) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 1.5.dp
                            )
                            Text(
                                "Loading ${uiState.loadingProgress}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Text(
                        "📡 $totalTowers Towers",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    if (uiState.stingrayDevices.isNotEmpty()) {
                        Text(
                            "🚨 ${uiState.stingrayDevices.size}",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                // Debug info
                Text(
                    "📍$towersWithRealLoc verified • 🗺️$towersWithValidCoords on map",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Error snackbar
        uiState.error?.let { error ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                Text(error)
            }
        }
        
        // Legend
        Card(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
            )
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                LegendItem(Color(0xFF4CAF50), "Primary/Serving")
                LegendItem(Color(0xFF2196F3), "Neighbor Cell")
                LegendItem(Color(0xFFFF9800), "Suspicious")
                LegendItem(Color(0xFFF44336), "Stingray")
            }
        }
        
        // Tower detail sheet
        uiState.selectedTower?.let { tower ->
            TowerDetailSheet(
                tower = tower,
                onDismiss = { viewModel.clearSelection() },
                onShare = { shareText(context, viewModel.getTowerShareText(tower)) },
                onCopy = { copyToClipboard(context, viewModel.getTowerShareText(tower)) },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
        
        // Stingray detail sheet
        uiState.selectedStingray?.let { device ->
            StingrayDetailSheet(
                device = device,
                onDismiss = { viewModel.clearSelection() },
                onShare = { shareText(context, viewModel.getStingrayShareText(device)) },
                onCopy = { copyToClipboard(context, viewModel.getStingrayShareText(device)) },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun TowerDetailSheet(
    tower: CellTower,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CellTower, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("${tower.networkType} Cell Tower", fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, "Close")
                }
            }
            
            if (tower.isSuspicious) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    Text("⚠️ SUSPICIOUS", modifier = Modifier.padding(8.dp), fontWeight = FontWeight.Bold)
                }
            }
            
            Divider(Modifier.padding(vertical = 8.dp))
            
            InfoRow("Carrier", tower.getCarrierName())
            InfoRow("Cell ID", tower.cellId.toString())
            InfoRow("LAC/TAC", tower.locationAreaCode.toString())
            InfoRow("MCC/MNC", "${tower.mobileCountryCode}/${tower.mobileNetworkCode}")
            InfoRow("Signal", "${tower.signalStrength} dBm (${tower.getSignalQualityDescription()})")
            InfoRow("Status", if (tower.isPrimary) "🟢 Primary" else "🔵 Neighbor")
            
            // Location info
            if (tower.hasRealLocation()) {
                InfoRow("Location", "📍 Real (${tower.locationSource ?: "database"})")
                InfoRow("Coordinates", String.format("%.6f, %.6f", tower.towerLatitude, tower.towerLongitude))
                tower.towerRange?.let { range ->
                    InfoRow("Range", "${range}m accuracy")
                }
            } else {
                InfoRow("Location", "📌 Observation point")
                InfoRow("Coordinates", String.format("%.6f, %.6f", tower.latitude, tower.longitude))
            }
            
            Spacer(Modifier.height(12.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onCopy, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.ContentCopy, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Copy")
                }
                Button(onClick = onShare, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Share, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Share")
                }
            }
        }
    }
}

@Composable
private fun StingrayDetailSheet(
    device: StingrayDevice,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(8.dp))
                    Text("🚨 STINGRAY DETECTED", fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, "Close")
                }
            }
            
            val threatColor = when (device.threatLevel) {
                ThreatLevel.CRITICAL -> Color(0xFFB71C1C)
                ThreatLevel.HIGH -> Color(0xFFF44336)
                ThreatLevel.MEDIUM -> Color(0xFFFF9800)
                ThreatLevel.LOW -> Color(0xFFFFC107)
                ThreatLevel.NONE -> Color.Gray
            }
            
            Card(
                colors = CardDefaults.cardColors(containerColor = threatColor),
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                Text(
                    "THREAT: ${device.threatLevel}",
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
            
            Divider(Modifier.padding(vertical = 8.dp))
            
            InfoRow("Cell ID", device.cellId?.toString() ?: "Unknown")
            InfoRow("Signal", "${device.signalStrength ?: "?"} dBm")
            device.deviceFingerprint?.let { InfoRow("Fingerprint", it) }
            
            Spacer(Modifier.height(12.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onCopy, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.ContentCopy, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Copy")
                }
                Button(
                    onClick = onShare, 
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Share, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Share")
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Medium)
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Cell Tower Info", text))
    Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show()
}

private fun shareText(context: Context, text: String) {
    context.startActivity(Intent.createChooser(
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        },
        "Share via"
    ))
}
