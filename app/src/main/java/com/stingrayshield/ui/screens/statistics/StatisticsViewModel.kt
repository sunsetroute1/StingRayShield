package com.stingrayshield.ui.screens.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stingrayshield.data.repository.CellTowerRepository
import com.stingrayshield.data.repository.DetectionEventRepository
import com.stingrayshield.domain.model.AnomalyType
import com.stingrayshield.domain.model.CellTower
import com.stingrayshield.domain.model.DetectionEvent
import com.stingrayshield.domain.model.ThreatLevel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * ViewModel for the statistics details screen
 */
@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val cellTowerRepository: CellTowerRepository,
    private val detectionEventRepository: DetectionEventRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    init {
        loadStatistics()
    }

    /**
     * Load all statistics data
     */
    private fun loadStatistics() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                // Time ranges
                val now = Instant.now().toEpochMilli()
                val oneDayAgo = Instant.now().minus(24, ChronoUnit.HOURS).toEpochMilli()
                val oneWeekAgo = Instant.now().minus(7, ChronoUnit.DAYS).toEpochMilli()
                val oneMonthAgo = Instant.now().minus(30, ChronoUnit.DAYS).toEpochMilli()

                // Load threat counts for different time periods
                val high24h = detectionEventRepository.getRecentEventCountByThreatLevel(ThreatLevel.HIGH, oneDayAgo)
                val medium24h = detectionEventRepository.getRecentEventCountByThreatLevel(ThreatLevel.MEDIUM, oneDayAgo)
                val low24h = detectionEventRepository.getRecentEventCountByThreatLevel(ThreatLevel.LOW, oneDayAgo)
                val critical24h = detectionEventRepository.getRecentEventCountByThreatLevel(ThreatLevel.CRITICAL, oneDayAgo)

                val high7d = detectionEventRepository.getRecentEventCountByThreatLevel(ThreatLevel.HIGH, oneWeekAgo)
                val medium7d = detectionEventRepository.getRecentEventCountByThreatLevel(ThreatLevel.MEDIUM, oneWeekAgo)
                val low7d = detectionEventRepository.getRecentEventCountByThreatLevel(ThreatLevel.LOW, oneWeekAgo)
                val critical7d = detectionEventRepository.getRecentEventCountByThreatLevel(ThreatLevel.CRITICAL, oneWeekAgo)

                val high30d = detectionEventRepository.getRecentEventCountByThreatLevel(ThreatLevel.HIGH, oneMonthAgo)
                val medium30d = detectionEventRepository.getRecentEventCountByThreatLevel(ThreatLevel.MEDIUM, oneMonthAgo)
                val low30d = detectionEventRepository.getRecentEventCountByThreatLevel(ThreatLevel.LOW, oneMonthAgo)
                val critical30d = detectionEventRepository.getRecentEventCountByThreatLevel(ThreatLevel.CRITICAL, oneMonthAgo)

                // Load cell tower data
                cellTowerRepository.getAllCellTowers().collect { towers ->
                    val uniqueTowers = towers.distinctBy { it.cellId }
                    val networkTypeBreakdown = towers.groupBy { it.networkType }.mapValues { it.value.size }
                    
                    // Calculate towers seen in different time periods
                    val towers24h = towers.filter { it.lastSeen >= oneDayAgo }.distinctBy { it.cellId }.size
                    val towers7d = towers.filter { it.lastSeen >= oneWeekAgo }.distinctBy { it.cellId }.size
                    val towers30d = towers.filter { it.lastSeen >= oneMonthAgo }.distinctBy { it.cellId }.size

                    // Find most frequently seen towers
                    val topTowers = towers
                        .groupBy { it.cellId }
                        .map { (cellId, observations) -> 
                            TowerStats(
                                cellId = cellId,
                                observationCount = observations.size,
                                networkType = observations.firstOrNull()?.networkType ?: "Unknown",
                                avgSignalStrength = observations.map { it.signalStrength }.average().toInt(),
                                lastSeen = observations.maxOfOrNull { it.lastSeen } ?: 0L
                            )
                        }
                        .sortedByDescending { it.observationCount }
                        .take(10)

                    _uiState.update { currentState ->
                        currentState.copy(
                            isLoading = false,
                            // 24h stats
                            critical24h = critical24h,
                            high24h = high24h,
                            medium24h = medium24h,
                            low24h = low24h,
                            // 7d stats
                            critical7d = critical7d,
                            high7d = high7d,
                            medium7d = medium7d,
                            low7d = low7d,
                            // 30d stats
                            critical30d = critical30d,
                            high30d = high30d,
                            medium30d = medium30d,
                            low30d = low30d,
                            // Tower stats
                            uniqueTowerCount = uniqueTowers.size,
                            totalObservations = towers.size,
                            towers24h = towers24h,
                            towers7d = towers7d,
                            towers30d = towers30d,
                            networkTypeBreakdown = networkTypeBreakdown,
                            topTowers = topTowers
                        )
                    }
                }

                // Load events by anomaly type
                detectionEventRepository.getAllEvents().collect { events ->
                    val eventsByType = events.groupBy { it.anomalyType }.mapValues { it.value.size }
                    val recentEvents = events
                        .filter { it.timestamp >= oneDayAgo }
                        .sortedByDescending { it.timestamp }
                        .take(20)

                    _uiState.update { currentState ->
                        currentState.copy(
                            eventsByAnomalyType = eventsByType,
                            recentHighThreatEvents = recentEvents.filter { 
                                it.threatLevel == ThreatLevel.HIGH || it.threatLevel == ThreatLevel.CRITICAL 
                            }
                        )
                    }
                }

            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "Failed to load statistics: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Refresh statistics data
     */
    fun refresh() {
        loadStatistics()
    }
}

/**
 * Statistics for a specific tower
 */
data class TowerStats(
    val cellId: Int,
    val observationCount: Int,
    val networkType: String,
    val avgSignalStrength: Int,
    val lastSeen: Long
)

/**
 * UI state for the statistics details screen
 */
data class StatisticsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    
    // 24h threat counts
    val critical24h: Int = 0,
    val high24h: Int = 0,
    val medium24h: Int = 0,
    val low24h: Int = 0,
    
    // 7d threat counts
    val critical7d: Int = 0,
    val high7d: Int = 0,
    val medium7d: Int = 0,
    val low7d: Int = 0,
    
    // 30d threat counts
    val critical30d: Int = 0,
    val high30d: Int = 0,
    val medium30d: Int = 0,
    val low30d: Int = 0,
    
    // Cell tower stats
    val uniqueTowerCount: Int = 0,
    val totalObservations: Int = 0,
    val towers24h: Int = 0,
    val towers7d: Int = 0,
    val towers30d: Int = 0,
    val networkTypeBreakdown: Map<String, Int> = emptyMap(),
    val topTowers: List<TowerStats> = emptyList(),
    
    // Event breakdown
    val eventsByAnomalyType: Map<AnomalyType, Int> = emptyMap(),
    val recentHighThreatEvents: List<DetectionEvent> = emptyList(),
    
    // Full data for charts
    val cellTowers: List<CellTower> = emptyList(),
    val detectionEvents: List<DetectionEvent> = emptyList()
)

