package com.stingrayshield.ui.screens.statistics

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SignalCellular4Bar
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.unit.dp
import com.stingrayshield.domain.model.AnomalyType
import com.stingrayshield.domain.model.DetectionEvent
import com.stingrayshield.domain.model.ThreatLevel
import com.stingrayshield.ui.screens.statistics.charts.DetectionPatternsChart
import com.stingrayshield.ui.screens.statistics.charts.SignalStrengthChart
import com.stingrayshield.ui.screens.statistics.charts.ThreatDistributionChart
import com.stingrayshield.ui.theme.statusAlert
import com.stingrayshield.ui.theme.statusDanger
import com.stingrayshield.ui.theme.statusNormal
import com.stingrayshield.ui.theme.statusWarning
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Statistics details screen showing comprehensive detection and tower statistics
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEvent: (Long) -> Unit,
    onNavigateToCellTower: (Int) -> Unit,
    viewModel: StatisticsViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    
    val tabs = listOf("Threats", "Cell Towers", "Breakdown", "Charts")
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detection Statistics") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab row
            TabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) }
                    )
                }
            }
            
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                when (selectedTabIndex) {
                    0 -> ThreatStatsTab(uiState, onNavigateToEvent)
                    1 -> CellTowerStatsTab(uiState, onNavigateToCellTower)
                    2 -> BreakdownTab(uiState)
                    3 -> ChartsTab(uiState)
                }
            }
        }
    }
}

/**
 * Tab showing threat statistics
 */
@Composable
fun ThreatStatsTab(uiState: StatisticsUiState, onNavigateToEvent: (Long) -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Summary header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Security,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val totalThreats24h = uiState.critical24h + uiState.high24h + uiState.medium24h + uiState.low24h
                    Text(
                        text = "$totalThreats24h",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Total Detections (24h)",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        
        // Time period cards
        item {
            Text(
                text = "Threat Detection by Time Period",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        item {
            TimePeriodCard(
                title = "Last 24 Hours",
                critical = uiState.critical24h,
                high = uiState.high24h,
                medium = uiState.medium24h,
                low = uiState.low24h
            )
        }
        
        item {
            TimePeriodCard(
                title = "Last 7 Days",
                critical = uiState.critical7d,
                high = uiState.high7d,
                medium = uiState.medium7d,
                low = uiState.low7d
            )
        }
        
        item {
            TimePeriodCard(
                title = "Last 30 Days",
                critical = uiState.critical30d,
                high = uiState.high30d,
                medium = uiState.medium30d,
                low = uiState.low30d
            )
        }
        
        // Recent high threat events
        if (uiState.recentHighThreatEvents.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Recent High Priority Events",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            items(uiState.recentHighThreatEvents) { event ->
                HighThreatEventCard(event = event, onClick = { onNavigateToEvent(event.id) })
            }
        }
    }
}

/**
 * Tab showing cell tower statistics
 */
@Composable
fun CellTowerStatsTab(uiState: StatisticsUiState, onNavigateToCellTower: (Int) -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Tower summary
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.CellTower,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${uiState.uniqueTowerCount}",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Unique Cell Towers Detected",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${uiState.totalObservations} total observations",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }
        
        // Tower counts by time period
        item {
            Text(
                text = "Towers Seen by Time Period",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    TowerTimePeriodRow("Last 24 Hours", uiState.towers24h, uiState.uniqueTowerCount)
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    TowerTimePeriodRow("Last 7 Days", uiState.towers7d, uiState.uniqueTowerCount)
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    TowerTimePeriodRow("Last 30 Days", uiState.towers30d, uiState.uniqueTowerCount)
                }
            }
        }
        
        // Network type breakdown
        if (uiState.networkTypeBreakdown.isNotEmpty()) {
            item {
                Text(
                    text = "Network Type Distribution",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            item {
                NetworkTypeCard(breakdown = uiState.networkTypeBreakdown)
            }
        }
        
        // Top towers
        if (uiState.topTowers.isNotEmpty()) {
            item {
                Text(
                    text = "Most Frequently Seen Towers",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            items(uiState.topTowers) { tower ->
                TopTowerCard(tower = tower, onClick = { onNavigateToCellTower(tower.cellId) })
            }
        }
    }
}

/**
 * Tab showing breakdown by anomaly type
 */
@Composable
fun BreakdownTab(uiState: StatisticsUiState) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Detection Types Breakdown",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        if (uiState.eventsByAnomalyType.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "No detection events recorded yet",
                        modifier = Modifier.padding(32.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        } else {
            val total = uiState.eventsByAnomalyType.values.sum()
            
            items(uiState.eventsByAnomalyType.entries.sortedByDescending { it.value }.toList()) { (type, count) ->
                AnomalyTypeCard(
                    anomalyType = type,
                    count = count,
                    total = total
                )
            }
        }
        
        // Legend
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Anomaly Type Descriptions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    AnomalyTypeDescription(
                        type = "Signal Strength",
                        description = "Unusual signal strength patterns that may indicate a cell simulator"
                    )
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    AnomalyTypeDescription(
                        type = "Tower Consistency",
                        description = "Cell tower parameters changed unexpectedly (MCC/MNC/LAC mismatch)"
                    )
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    AnomalyTypeDescription(
                        type = "Neighbor Cells",
                        description = "Sudden disappearance of neighboring cells, classic stingray indicator"
                    )
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    AnomalyTypeDescription(
                        type = "Silent SMS",
                        description = "Invisible SMS messages used for tracking and surveillance"
                    )
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    AnomalyTypeDescription(
                        type = "Encryption Change",
                        description = "Network encryption was downgraded, potential forced downgrade attack"
                    )
                }
            }
        }
    }
}

@Composable
fun TimePeriodCard(
    title: String,
    critical: Int,
    high: Int,
    medium: Int,
    low: Int
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ThreatCountItem("Critical", critical, statusDanger)
                ThreatCountItem("High", high, statusAlert)
                ThreatCountItem("Medium", medium, statusWarning)
                ThreatCountItem("Low", low, Color.Blue)
            }
        }
    }
}

@Composable
fun ThreatCountItem(label: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun TowerTimePeriodRow(label: String, count: Int, total: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = " / $total",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun NetworkTypeCard(breakdown: Map<String, Int>) {
    val total = breakdown.values.sum().toFloat()
    val colors = mapOf(
        "5G" to Color(0xFF4CAF50),
        "4G" to Color(0xFF2196F3),
        "3G" to Color(0xFFFFC107),
        "2G" to Color(0xFFFF5722),
        "Unknown" to Color.Gray
    )
    
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            breakdown.entries.sortedByDescending { it.value }.forEach { (type, count) ->
                val percentage = if (total > 0) (count / total) * 100 else 0f
                val color = colors[type] ?: Color.Gray
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = type,
                        modifier = Modifier.width(60.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    LinearProgressIndicator(
                        progress = percentage / 100f,
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = color,
                        trackColor = color.copy(alpha = 0.2f)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "$count (${percentage.toInt()}%)",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.width(80.dp),
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}

@Composable
fun TopTowerCard(tower: TowerStats, onClick: () -> Unit) {
    val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.SignalCellular4Bar,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Cell ID: ${tower.cellId}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${tower.networkType} • ${tower.avgSignalStrength} dBm avg",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    text = "Last seen: ${dateFormat.format(Date(tower.lastSeen))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${tower.observationCount}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "observations",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun HighThreatEventCard(event: DetectionEvent, onClick: () -> Unit) {
    val threatColor = if (event.threatLevel == ThreatLevel.CRITICAL) statusDanger else statusAlert
    val dateFormat = SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault())
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = threatColor.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = threatColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.getTitle(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = dateFormat.format(Date(event.timestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            Text(
                text = event.threatLevel.name,
                style = MaterialTheme.typography.labelMedium,
                color = threatColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun AnomalyTypeCard(anomalyType: AnomalyType, count: Int, total: Int) {
    val percentage = if (total > 0) (count.toFloat() / total) * 100 else 0f
    val color = when (anomalyType) {
        AnomalyType.SIGNAL_STRENGTH -> statusWarning
        AnomalyType.TOWER_CONSISTENCY -> statusDanger
        AnomalyType.NEIGHBOR_CELLS -> statusAlert
        AnomalyType.SILENT_SMS -> Color(0xFF9C27B0)
        AnomalyType.FEMTOCELL -> Color(0xFF00BCD4)
        AnomalyType.ENCRYPTION_CHANGE -> statusDanger
        AnomalyType.LOCATION_TRACKING -> Color(0xFF795548)
        AnomalyType.MULTIPLE_ANOMALIES -> Color(0xFFFF6F00)
        AnomalyType.UNKNOWN -> Color.Gray
    }
    
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = anomalyType.name.replace("_", " "),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                LinearProgressIndicator(
                    progress = percentage / 100f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = color,
                    trackColor = color.copy(alpha = 0.2f)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "${percentage.toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
fun AnomalyTypeDescription(type: String, description: String) {
    Column {
        Text(
            text = type,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

/**
 * Tab showing data visualization charts
 */
@Composable
fun ChartsTab(uiState: StatisticsUiState) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Signal strength over time chart
        item {
            SignalStrengthChart(
                cellTowers = uiState.cellTowers,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Threat distribution pie chart
        item {
            ThreatDistributionChart(
                detectionEvents = uiState.detectionEvents,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Detection patterns over time chart
        item {
            DetectionPatternsChart(
                detectionEvents = uiState.detectionEvents,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Summary statistics card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Data Summary",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatisticItem(
                            value = uiState.detectionEvents.size.toString(),
                            label = "Detections",
                            color = statusDanger
                        )

                        StatisticItem(
                            value = uiState.cellTowers.size.toString(),
                            label = "Towers",
                            color = statusNormal
                        )

                        StatisticItem(
                            value = uiState.detectionEvents.count {
                                it.threatLevel == ThreatLevel.HIGH || it.threatLevel == ThreatLevel.CRITICAL
                            }.toString(),
                            label = "High Threats",
                            color = statusAlert
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatisticItem(
    value: String,
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = color,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

