package com.stingrayshield.ui.screens.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stingrayshield.data.repository.CellTowerRepository
import com.stingrayshield.data.repository.DetectionEventRepository
import com.stingrayshield.domain.model.DetectionEvent
import com.stingrayshield.domain.model.ThreatLevel
import com.stingrayshield.util.NetworkStatsHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * ViewModel for the dashboard screen
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cellTowerRepository: CellTowerRepository,
    private val detectionEventRepository: DetectionEventRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    
    // Recent detection events
    val recentEvents: Flow<List<DetectionEvent>> = detectionEventRepository.getUnarchivedEvents()
    
    // Network stats helper
    private val networkStatsHelper = NetworkStatsHelper(context)

    init {
        networkStatsHelper.startSession()
        loadDashboardData()
        startTrafficStatsUpdates()
    }
    
    /**
     * Load data for the dashboard
     */
    private fun loadDashboardData() {
        viewModelScope.launch {
            // Get statistics for the last 24 hours
            val yesterday = Instant.now().minus(24, ChronoUnit.HOURS).toEpochMilli()
            
            // Get tower count
            cellTowerRepository.getAllCellTowers().collect { towers ->
                val uniqueTowerCount = towers.map { it.cellId }.distinct().size
                
                _uiState.update { currentState ->
                    currentState.copy(
                        uniqueTowerCount = uniqueTowerCount,
                        totalTowerObservations = towers.size
                    )
                }
            }
            
            // Get threat statistics
            val highThreats = detectionEventRepository.getRecentEventCountByThreatLevel(
                ThreatLevel.HIGH, yesterday
            )
            
            val mediumThreats = detectionEventRepository.getRecentEventCountByThreatLevel(
                ThreatLevel.MEDIUM, yesterday
            )
            
            val lowThreats = detectionEventRepository.getRecentEventCountByThreatLevel(
                ThreatLevel.LOW, yesterday
            )
            
            _uiState.update { currentState ->
                currentState.copy(
                    highThreatCount = highThreats,
                    mediumThreatCount = mediumThreats,
                    lowThreatCount = lowThreats
                )
            }
        }
    }
    
    /**
     * Start periodic updates for traffic stats
     */
    private fun startTrafficStatsUpdates() {
        viewModelScope.launch {
            while (true) {
                updateTrafficStats()
                delay(5000) // Update every 5 seconds
            }
        }
    }
    
    /**
     * Update traffic statistics
     */
    private suspend fun updateTrafficStats() {
        try {
            val trafficData = networkStatsHelper.getTrafficStats()
            _uiState.update { currentState ->
                currentState.copy(
                    totalSentBytes = trafficData.totalSentBytes,
                    totalReceivedBytes = trafficData.totalReceivedBytes,
                    sessionSentBytes = trafficData.sessionSentBytes,
                    sessionReceivedBytes = trafficData.sessionReceivedBytes,
                    trafficDataSource = trafficData.dataSource,
                    mobileNetworkAvailable = trafficData.mobileNetworkAvailable
                )
            }
        } catch (e: Exception) {
            // Log error but don't crash
            android.util.Log.e("DashboardViewModel", "Error updating traffic stats: ${e.message}")
        }
    }
    
    /**
     * Refresh the dashboard data
     */
    fun refreshData() {
        loadDashboardData()
        viewModelScope.launch {
            updateTrafficStats()
        }
    }
    
    /**
     * Reset session traffic counters
     */
    fun resetSessionStats() {
        networkStatsHelper.startSession()
        viewModelScope.launch {
            updateTrafficStats()
        }
    }
    
    /**
     * Archive a detection event
     */
    fun archiveEvent(eventId: Long) {
        viewModelScope.launch {
            detectionEventRepository.archiveEvent(eventId)
        }
    }
}

/**
 * UI state for the dashboard screen
 */
data class DashboardUiState(
    val uniqueTowerCount: Int = 0,
    val totalTowerObservations: Int = 0,
    val highThreatCount: Int = 0,
    val mediumThreatCount: Int = 0,
    val lowThreatCount: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
    // Traffic stats
    val totalSentBytes: Long = 0,
    val totalReceivedBytes: Long = 0,
    val sessionSentBytes: Long = 0,
    val sessionReceivedBytes: Long = 0,
    val trafficDataSource: String = "Unknown",
    val mobileNetworkAvailable: Boolean = false
) {
    // Formatted traffic values
    val totalSentFormatted: String get() = formatBytes(totalSentBytes)
    val totalReceivedFormatted: String get() = formatBytes(totalReceivedBytes)
    val sessionSentFormatted: String get() = formatBytes(sessionSentBytes)
    val sessionReceivedFormatted: String get() = formatBytes(sessionReceivedBytes)
    
    companion object {
        fun formatBytes(bytes: Long): String {
            if (bytes < 0) return "N/A"
            return when {
                bytes < 1024 -> "$bytes B"
                bytes < 1024 * 1024 -> String.format("%.2f KB", bytes / 1024.0)
                bytes < 1024 * 1024 * 1024 -> String.format("%.2f MB", bytes / (1024.0 * 1024.0))
                else -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
            }
        }
    }
}
