package com.stingrayshield.ui.screens.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stingrayshield.data.repository.CellTowerRepository
import com.stingrayshield.data.repository.DetectionEventRepository
import com.stingrayshield.domain.model.CellTower
import com.stingrayshield.domain.model.DetectionEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the cell tower details screen
 */
@HiltViewModel
class CellTowerDetailsViewModel @Inject constructor(
    private val cellTowerRepository: CellTowerRepository,
    private val detectionEventRepository: DetectionEventRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Cell ID from navigation arguments
    private val cellId: Int = savedStateHandle.get<Int>("cellId") ?: 0

    // UI state
    private val _uiState = MutableStateFlow(CellTowerDetailsUiState())
    val uiState: StateFlow<CellTowerDetailsUiState> = _uiState.asStateFlow()

    init {
        if (cellId > 0) {
            loadCellTowerDetails(cellId)
            loadCellTowerHistory(cellId)
            loadRelatedEvents(cellId)
        }
    }

    /**
     * Load cell tower details by cell ID
     */
    private fun loadCellTowerDetails(id: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                cellTowerRepository.getCellTowerById(id)?.let { tower ->
                    _uiState.update { 
                        it.copy(
                            cellTower = tower,
                            isLoading = false
                        )
                    }
                } ?: run {
                    _uiState.update { 
                        it.copy(
                            error = "Cell tower not found",
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        error = "Error loading cell tower: ${e.message}",
                        isLoading = false
                    )
                }
            }
        }
    }

    /**
     * Load history of observations for this cell tower
     */
    private fun loadCellTowerHistory(id: Int) {
        viewModelScope.launch {
            try {
                cellTowerRepository.getCellTowerHistoryById(id).collect { history ->
                    _uiState.update { 
                        it.copy(
                            history = history.sortedByDescending { tower -> tower.lastSeen }
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        historyError = "Error loading history: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Load detection events related to this cell tower
     */
    private fun loadRelatedEvents(id: Int) {
        viewModelScope.launch {
            try {
                detectionEventRepository.getEventsByCellId(id).collect { events ->
                    _uiState.update { 
                        it.copy(
                            relatedEvents = events.sortedByDescending { event -> event.timestamp }
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        eventsError = "Error loading events: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Mark cell tower as trusted (e.g., your home cell tower)
     */
    fun markAsTrusted() {
        viewModelScope.launch {
            try {
                _uiState.value.cellTower?.let { tower ->
                    val updatedTower = tower.copy(isTrusted = true)
                    cellTowerRepository.updateCellTower(updatedTower)
                    _uiState.update { it.copy(cellTower = updatedTower) }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        error = "Error updating cell tower: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Mark cell tower as suspicious
     */
    fun markAsSuspicious() {
        viewModelScope.launch {
            try {
                _uiState.value.cellTower?.let { tower ->
                    val updatedTower = tower.copy(isSuspicious = true)
                    cellTowerRepository.updateCellTower(updatedTower)
                    _uiState.update { it.copy(cellTower = updatedTower) }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        error = "Error updating cell tower: ${e.message}"
                    )
                }
            }
        }
    }
}

/**
 * UI state for the cell tower details screen
 */
data class CellTowerDetailsUiState(
    val cellTower: CellTower? = null,
    val history: List<CellTower> = emptyList(),
    val relatedEvents: List<DetectionEvent> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val historyError: String? = null,
    val eventsError: String? = null
)
