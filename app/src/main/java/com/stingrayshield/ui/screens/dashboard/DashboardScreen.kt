package com.stingrayshield.ui.screens.dashboard

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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stingrayshield.R
import com.stingrayshield.domain.model.DetectionEvent
import com.stingrayshield.domain.model.DetectionStatus
import com.stingrayshield.domain.model.SystemStatus
import com.stingrayshield.domain.model.ThreatLevel
import com.stingrayshield.ui.theme.statusAlert
import com.stingrayshield.ui.theme.statusDanger
import com.stingrayshield.ui.theme.statusNormal
import com.stingrayshield.ui.theme.statusUnknown
import com.stingrayshield.ui.theme.statusWarning
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Main dashboard screen showing system status and recent events
 */
@Composable
fun DashboardScreen(
    navigateToEventDetails: (Long) -> Unit,
    navigateToStatistics: () -> Unit,
    viewModel: DashboardViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val recentEvents by viewModel.recentEvents.collectAsState(initial = emptyList())
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "StingrayShield",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        
        item {
            StatusCard(
                status = SystemStatus(
                    detectionStatus = DetectionStatus.NORMAL, // This would come from the actual system status
                    isServiceRunning = true
                )
            )
        }
        
        item {
            StatisticsCard(uiState = uiState, onClick = navigateToStatistics)
        }
        
        item {
            TrafficStatsCard(
                uiState = uiState,
                onResetSession = { viewModel.resetSessionStats() }
            )
        }
        
        item {
            Text(
                text = "Recent Detection Events",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = { viewModel.refreshData() }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh"
                    )
                }
            }
        }
        
        if (recentEvents.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Text(
                        text = "No detection events recorded yet",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        } else {
            items(recentEvents) { event ->
                EventCard(
                    event = event,
                    onEventClick = { navigateToEventDetails(event.id) },
                    onArchive = { viewModel.archiveEvent(event.id) }
                )
            }
        }
    }
}

/**
 * Card showing the current system status
 */
@Composable
fun StatusCard(status: SystemStatus) {
    val statusColor = when (status.detectionStatus) {
        DetectionStatus.NORMAL -> statusNormal
        DetectionStatus.WARNING -> statusWarning
        DetectionStatus.ALERT -> statusAlert
        DetectionStatus.DANGER -> statusDanger
        DetectionStatus.UNKNOWN -> statusUnknown
    }
    
    val statusText = when (status.detectionStatus) {
        DetectionStatus.NORMAL -> stringResource(R.string.status_normal)
        DetectionStatus.WARNING -> stringResource(R.string.status_warning)
        DetectionStatus.ALERT -> stringResource(R.string.status_alert)
        DetectionStatus.DANGER -> stringResource(R.string.status_danger)
        DetectionStatus.UNKNOWN -> stringResource(R.string.status_unknown)
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                
                Text(
                    text = "Status: ${status.detectionStatus.name}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyLarge
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = status.getStatusRecommendation(),
                style = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = if (status.isServiceRunning) 
                    stringResource(R.string.service_running)
                else 
                    stringResource(R.string.service_stopped),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (status.isServiceRunning) FontWeight.Normal else FontWeight.Bold,
                color = if (status.isServiceRunning) Color.Green else Color.Red
            )
        }
    }
}

/**
 * Card showing network traffic statistics
 */
@Composable
fun TrafficStatsCard(
    uiState: DashboardUiState,
    onResetSession: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.SignalCellularAlt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Traffic Stats",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                
                // Connection status indicator
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (uiState.mobileNetworkAvailable) statusNormal else Color.Gray
                            )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (uiState.mobileNetworkAvailable) "Connected" else "Disconnected",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Total stats section
            Text(
                text = "Total (Since Boot)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                // Upload/Sent
                TrafficStatItem(
                    label = "Sent",
                    value = uiState.totalSentFormatted,
                    icon = Icons.Default.ArrowUpward,
                    color = Color(0xFF4CAF50) // Green for upload
                )
                
                // Download/Received
                TrafficStatItem(
                    label = "Received",
                    value = uiState.totalReceivedFormatted,
                    icon = Icons.Default.ArrowDownward,
                    color = Color(0xFF2196F3) // Blue for download
                )
            }
            
            Divider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
            
            // Session stats section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Session",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                IconButton(
                    onClick = onResetSession,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset Session",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                TrafficStatItem(
                    label = "Sent",
                    value = uiState.sessionSentFormatted,
                    icon = Icons.Default.ArrowUpward,
                    color = Color(0xFF4CAF50)
                )
                
                TrafficStatItem(
                    label = "Received",
                    value = uiState.sessionReceivedFormatted,
                    icon = Icons.Default.ArrowDownward,
                    color = Color(0xFF2196F3)
                )
            }
            
            // Data source info (for debugging)
            if (uiState.trafficDataSource != "Unknown") {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Source: ${uiState.trafficDataSource}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End
                )
            }
            
            // Warning if received data is unavailable
            if (uiState.totalReceivedBytes <= 0 && uiState.totalSentBytes > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Text(
                        text = "⚠️ Received data may be unavailable on this device. Some Android devices don't report mobile RX bytes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}

/**
 * Individual traffic stat item with icon
 */
@Composable
fun TrafficStatItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Card showing statistics about detections and cell towers
 */
@Composable
fun StatisticsCard(uiState: DashboardUiState, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Detection Statistics (24h)",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Tap for details →",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StatItem(
                    value = uiState.highThreatCount.toString(),
                    label = "High Threats",
                    color = statusDanger
                )
                
                StatItem(
                    value = uiState.mediumThreatCount.toString(),
                    label = "Medium Threats",
                    color = statusAlert
                )
                
                StatItem(
                    value = uiState.lowThreatCount.toString(),
                    label = "Low Threats",
                    color = statusWarning
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Cell Tower Statistics",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StatItem(
                    value = uiState.uniqueTowerCount.toString(),
                    label = "Unique Towers",
                    color = MaterialTheme.colorScheme.primary
                )
                
                StatItem(
                    value = uiState.totalTowerObservations.toString(),
                    label = "Total Observations",
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

/**
 * Individual statistic item
 */
@Composable
fun StatItem(value: String, label: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = color,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Card showing a detection event
 */
@Composable
fun EventCard(event: DetectionEvent, onEventClick: () -> Unit, onArchive: () -> Unit) {
    val threatColor = when (event.threatLevel) {
        ThreatLevel.CRITICAL -> statusDanger
        ThreatLevel.HIGH -> statusAlert
        ThreatLevel.MEDIUM -> statusWarning
        ThreatLevel.LOW -> Color.Blue
        ThreatLevel.NONE -> statusNormal
    }
    
    val dateFormat = SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault())
    val dateString = dateFormat.format(Date(event.timestamp))
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = threatColor,
                    modifier = Modifier.size(24.dp)
                )
                
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = event.getTitle(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = dateString,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = event.description,
                style = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onEventClick,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(text = stringResource(R.string.action_view_details))
                }
                
                Button(
                    onClick = onArchive
                ) {
                    Text(text = stringResource(R.string.action_dismiss))
                }
            }
        }
    }
}
