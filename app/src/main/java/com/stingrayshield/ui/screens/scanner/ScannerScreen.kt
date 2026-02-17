package com.stingrayshield.ui.screens.scanner

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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SignalCellular4Bar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stingrayshield.R
import com.stingrayshield.domain.model.CellTower
import com.stingrayshield.ui.theme.statusAlert
import com.stingrayshield.ui.theme.statusDanger
import com.stingrayshield.ui.theme.statusNormal
import com.stingrayshield.ui.theme.statusWarning
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Scanner screen showing current and nearby cell towers
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    navigateToCellTowerDetails: (Int) -> Unit,
    viewModel: ScannerViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentCellTowers by viewModel.currentCellTowers.collectAsState()
    val neighboringCellTowers by viewModel.neighboringCellTowers.collectAsState()
    val recentCellTowers by viewModel.recentCellTowers.collectAsState()
    
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    
    val tabs = listOf(
        "Current" to currentCellTowers, 
        "Neighboring" to neighboringCellTowers,
        "History" to recentCellTowers
    )
    
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.refreshScannerData() },
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh Scanner"
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Header section with last scan time
            ScannerHeader(uiState.lastScanTimeMillis)
            
            // Tab row for different tower lists
            TabRow(
                selectedTabIndex = selectedTabIndex
            ) {
                tabs.forEachIndexed { index, (title, _) ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    )
                }
            }
            
            // Main content with cell tower list
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    val cellTowers = tabs[selectedTabIndex].second
                    
                    if (cellTowers.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No ${tabs[selectedTabIndex].first.lowercase()} cell towers detected",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(
                                top = 16.dp,
                                bottom = 88.dp,
                                start = 16.dp,
                                end = 16.dp
                            )
                        ) {
                            items(cellTowers) { cellTower ->
                                CellTowerCard(
                                    cellTower = cellTower,
                                    onClick = { navigateToCellTowerDetails(cellTower.cellId) }
                                )
                            }
                        }
                    }
                }
                
                if (uiState.error != null) {
                    Card(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp)
                            .fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = uiState.error ?: "",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}

/**
 * Header section showing last scan time
 */
@Composable
fun ScannerHeader(lastScanTimeMillis: Long) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    val lastScanTime = if (lastScanTimeMillis > 0) {
        dateFormat.format(Date(lastScanTimeMillis))
    } else {
        "Never"
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Cell Tower Scanner",
            style = MaterialTheme.typography.headlineSmall
        )
        
        Text(
            text = "Last scan: $lastScanTime",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Card displaying cell tower information
 */
@Composable
fun CellTowerCard(cellTower: CellTower, onClick: () -> Unit) {
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
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Signal strength indicator
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .border(2.dp, signalColor, CircleShape)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SignalCellular4Bar,
                        contentDescription = null,
                        tint = signalColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // Cell tower info
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Cell ID: ${cellTower.cellId}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = "MCC: ${cellTower.mcc}, MNC: ${cellTower.mnc}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Text(
                        text = "LAC: ${cellTower.lac}, Network Type: ${cellTower.networkType}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                // Signal strength in dBm
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "${cellTower.signalStrength} dBm",
                        style = MaterialTheme.typography.titleMedium,
                        color = signalColor,
                        fontWeight = FontWeight.Bold
                    )
                    
                    SignalMeter(
                        signalQuality = signalQuality,
                        color = signalColor
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Divider()
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Location info if available
            if (cellTower.latitude != 0.0 && cellTower.longitude != 0.0) {
                Text(
                    text = "Location: ${cellTower.latitude}, ${cellTower.longitude}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            // Last seen time
            val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            val lastSeenTime = dateFormat.format(Date(cellTower.lastSeen))
            
            Text(
                text = "Last seen: $lastSeenTime",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Visual meter showing signal quality
 */
@Composable
fun SignalMeter(signalQuality: Double, color: Color) {
    val segments = 5
    val filledSegments = (signalQuality * segments).toInt().coerceIn(0, segments)
    
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        for (i in 1..segments) {
            val height = (3 + (i * 2)).dp
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(height)
                    .background(
                        color = if (i <= filledSegments) color else Color.Gray.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(1.dp)
                    )
            )
        }
    }
}
