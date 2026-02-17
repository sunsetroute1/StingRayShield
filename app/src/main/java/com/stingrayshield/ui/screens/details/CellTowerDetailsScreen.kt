package com.stingrayshield.ui.screens.details

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.SignalCellular4Bar
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.DisposableEffect
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import com.stingrayshield.domain.model.CellTower
import com.stingrayshield.domain.model.DetectionEvent
import com.stingrayshield.domain.model.ThreatLevel
import com.stingrayshield.domain.model.getTitle
import com.stingrayshield.ui.theme.statusAlert
import com.stingrayshield.ui.theme.statusDanger
import com.stingrayshield.ui.theme.statusNormal
import com.stingrayshield.ui.theme.statusWarning
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Screen showing detailed information about a specific cell tower
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CellTowerDetailsScreen(
    cellId: Int,
    onNavigateBack: () -> Unit,
    viewModel: CellTowerDetailsViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    
    val tabs = listOf("Details", "History", "Events")
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cell Tower Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (uiState.cellTower != null) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Cell Tower header
                    CellTowerHeader(
                        cellTower = uiState.cellTower!!,
                        onMarkAsTrusted = { viewModel.markAsTrusted() },
                        onMarkAsSuspicious = { viewModel.markAsSuspicious() }
                    )
                    
                    // Tabs
                    TabRow(selectedTabIndex = selectedTabIndex) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTabIndex == index,
                                onClick = { selectedTabIndex = index },
                                text = { Text(title) }
                            )
                        }
                    }
                    
                    // Tab content
                    when (selectedTabIndex) {
                        0 -> CellTowerDetailsTab(uiState.cellTower!!)
                        1 -> CellTowerHistoryTab(uiState.history, uiState.historyError)
                        2 -> CellTowerEventsTab(uiState.relatedEvents, uiState.eventsError)
                    }
                }
            } else if (uiState.error != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = uiState.error ?: "Unknown error",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(onClick = onNavigateBack) {
                        Text("Go Back")
                    }
                }
            }
        }
    }
}

/**
 * Header section showing cell tower basic info and status
 */
@Composable
fun CellTowerHeader(
    cellTower: CellTower,
    onMarkAsTrusted: () -> Unit,
    onMarkAsSuspicious: () -> Unit
) {
    val signalQuality = cellTower.getSignalQualityNormalized()
    val signalColor = when (signalQuality) {
        in 0.0..0.3 -> statusDanger
        in 0.3..0.5 -> statusAlert
        in 0.5..0.7 -> statusWarning
        else -> statusNormal
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.SignalCellular4Bar,
                    contentDescription = null,
                    tint = signalColor,
                    modifier = Modifier.size(36.dp)
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Cell ID: ${cellTower.cellId}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = "MCC: ${cellTower.mcc}, MNC: ${cellTower.mnc}, LAC: ${cellTower.lac}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (cellTower.isTrusted) {
                    StatusChip(
                        text = "Trusted",
                        backgroundColor = statusNormal.copy(alpha = 0.2f),
                        textColor = statusNormal,
                        iconVector = Icons.Default.CheckCircle
                    )
                }
                
                if (cellTower.isSuspicious) {
                    StatusChip(
                        text = "Suspicious",
                        backgroundColor = statusDanger.copy(alpha = 0.2f),
                        textColor = statusDanger,
                        iconVector = Icons.Default.Warning
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onMarkAsTrusted,
                    modifier = Modifier.weight(1f),
                    enabled = !cellTower.isTrusted
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text("Trust")
                }
                
                Button(
                    onClick = onMarkAsSuspicious,
                    modifier = Modifier.weight(1f),
                    enabled = !cellTower.isSuspicious
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text("Mark Suspicious")
                }
            }
        }
    }
}

/**
 * Tab showing cell tower details
 */
@Composable
fun CellTowerDetailsTab(cellTower: CellTower) {
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Tower details section
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Network Information",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                DetailRow("Network Type (radio)", cellTower.networkType)
                DetailRow("Signal Strength", "${cellTower.signalStrength} dBm")
                DetailRow("Cell ID", cellTower.cellId.toString())
                DetailRow("LAC / area", cellTower.lac.toString())
                DetailRow("MCC", cellTower.mcc.toString())
                DetailRow("MNC / net", cellTower.mnc.toString())
                
                if (cellTower.psc > 0) DetailRow("PSC (unit)", cellTower.psc.toString())
                if (cellTower.pci > 0) DetailRow("PCI (unit)", cellTower.pci.toString())
            }
        }
        
        // Tower location (CSV columns 7 & 8: lon, lat) and OpenCellID/database info
        val displayLat = cellTower.getDisplayLatitude()
        val displayLon = cellTower.getDisplayLongitude()
        if (displayLat != 0.0 || displayLon != 0.0) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Tower Location (latitude & longitude)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    DetailRow("Latitude (column 8)", displayLat.toString())
                    DetailRow("Longitude (column 7)", displayLon.toString())
                    cellTower.towerRange?.let { DetailRow("Range (m)", it.toString()) }
                    cellTower.locationSource?.takeIf { it.isNotBlank() }?.let { DetailRow("Location source", it) }
                    if (cellTower.locationSource == "opencellid") {
                        DetailRow("Samples (measurements)", cellTower.observationCount.toString())
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Last seen / observation info
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Observation Information",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val firstSeen = dateFormat.format(Date(cellTower.firstSeen))
                val lastSeen = dateFormat.format(Date(cellTower.lastSeen))
                
                DetailRow("First Seen", firstSeen)
                DetailRow("Last Seen", lastSeen)
                DetailRow("Observation Count", cellTower.observationCount.toString())
            }
        }
        
        // Map uses display position (real tower lon/lat from CSV when available)
        if (displayLat != 0.0 || displayLon != 0.0) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Map",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
            )
            TowerLocationMap(
                latitude = displayLat,
                longitude = displayLon
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Coordinates: $displayLat, $displayLon",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Tab showing cell tower observation history
 */
@Composable
fun CellTowerHistoryTab(history: List<CellTower>, error: String?) {
    if (error != null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error
            )
        }
        return
    }
    
    if (history.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No history available for this cell tower",
                style = MaterialTheme.typography.bodyLarge
            )
        }
        return
    }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(history) { observation ->
            HistoryItemCard(observation)
        }
    }
}

/**
 * Tab showing detection events related to this cell tower
 */
@Composable
fun CellTowerEventsTab(events: List<DetectionEvent>, error: String?) {
    if (error != null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error
            )
        }
        return
    }
    
    if (events.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No detection events associated with this cell tower",
                style = MaterialTheme.typography.bodyLarge
            )
        }
        return
    }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(events) { event ->
            EventItemCard(event)
        }
    }
}

/**
 * Card showing a single history item for a cell tower
 */
@Composable
fun HistoryItemCard(cellTower: CellTower) {
    val signalQuality = cellTower.getSignalQualityNormalized()
    val signalColor = when (signalQuality) {
        in 0.0..0.3 -> statusDanger
        in 0.3..0.5 -> statusAlert
        in 0.5..0.7 -> statusWarning
        else -> statusNormal
    }
    
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    val observedTime = dateFormat.format(Date(cellTower.lastSeen))
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = observedTime,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                
                Text(
                    text = "${cellTower.signalStrength} dBm",
                    style = MaterialTheme.typography.bodyLarge,
                    color = signalColor,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "Network: ${cellTower.networkType}",
                style = MaterialTheme.typography.bodyMedium
            )
            
            if (cellTower.latitude != 0.0 && cellTower.longitude != 0.0) {
                Text(
                    text = "Location: ${cellTower.latitude}, ${cellTower.longitude}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

/**
 * Card showing a detection event related to this cell tower
 */
@Composable
fun EventItemCard(event: DetectionEvent) {
    val threatColor = when (event.threatLevel) {
        ThreatLevel.CRITICAL -> statusDanger
        ThreatLevel.HIGH -> statusAlert
        ThreatLevel.MEDIUM -> statusWarning
        ThreatLevel.LOW -> Color.Blue
        ThreatLevel.NONE -> statusNormal
    }
    
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    val eventTime = dateFormat.format(Date(event.timestamp))
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* Navigate to event details */ },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = threatColor,
                    modifier = Modifier.padding(end = 8.dp)
                )
                
                Text(
                    text = event.getTitle(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = eventTime,
                style = MaterialTheme.typography.bodySmall
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = event.description,
                style = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "Threat Level: ${event.threatLevel}",
                style = MaterialTheme.typography.bodyMedium,
                color = threatColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Status chip for displaying cell tower status
 */
@Composable
fun StatusChip(
    text: String,
    backgroundColor: Color,
    textColor: Color,
    iconVector: ImageVector
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = iconVector,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(16.dp)
            )
            
            Spacer(modifier = Modifier.width(4.dp))
            
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor
            )
        }
    }
}

/**
 * Map showing the cell tower location using OpenStreetMap
 */
@Composable
fun TowerLocationMap(latitude: Double, longitude: Double) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // Initialize OSMDroid configuration
    Configuration.getInstance().userAgentValue = context.packageName
    
    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(15.0)
            controller.setCenter(GeoPoint(latitude, longitude))
        }
    }
    
    // Handle lifecycle
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }
    
    // Add marker
    DisposableEffect(latitude, longitude) {
        mapView.overlays.clear()
        val marker = Marker(mapView).apply {
            position = GeoPoint(latitude, longitude)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = "Cell Tower Location"
            snippet = "Lat: $latitude, Lng: $longitude"
        }
        mapView.overlays.add(marker)
        mapView.invalidate()
        onDispose { }
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize()
        )
    }
}
