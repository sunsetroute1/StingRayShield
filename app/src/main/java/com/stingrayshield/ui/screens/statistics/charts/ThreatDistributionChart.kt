package com.stingrayshield.ui.screens.statistics.charts

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stingrayshield.domain.model.DetectionEvent

/**
 * Composable for displaying threat distribution chart
 */
@Composable
fun ThreatDistributionChart(detectionEvents: List<DetectionEvent>, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        // Placeholder chart - in a real app, you would integrate a charting library like MPAndroidChart or Vico
        Text(
            text = "Threat Distribution Chart\n(${detectionEvents.size} events analyzed)",
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
