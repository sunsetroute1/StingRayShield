package com.stingrayshield.ui.screens.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stingrayshield.data.repository.DetectionEventRepository
import com.stingrayshield.domain.model.DetectionEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the event details screen
 */
@HiltViewModel
class EventDetailsViewModel @Inject constructor(
    private val detectionEventRepository: DetectionEventRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Event ID from navigation arguments
    private val eventId: Long = savedStateHandle.get<Long>("eventId") ?: 0L

    // UI state
    private val _uiState = MutableStateFlow(EventDetailsUiState())
    val uiState: StateFlow<EventDetailsUiState> = _uiState.asStateFlow()

    init {
        if (eventId > 0) {
            loadEventDetails(eventId)
        }
    }

    /**
     * Load event details by ID
     */
    private fun loadEventDetails(id: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                detectionEventRepository.getEventById(id)?.let { event ->
                    _uiState.update { 
                        it.copy(
                            event = event,
                            isLoading = false
                        )
                    }
                } ?: run {
                    _uiState.update { 
                        it.copy(
                            error = "Event not found",
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        error = "Error loading event: ${e.message}",
                        isLoading = false
                    )
                }
            }
        }
    }

    /**
     * Archive this detection event
     */
    fun archiveEvent() {
        viewModelScope.launch {
            try {
                detectionEventRepository.archiveEvent(eventId)
                _uiState.update { it.copy(isArchived = true) }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        error = "Error archiving event: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Mark event as a false positive
     */
    fun markAsFalsePositive() {
        viewModelScope.launch {
            try {
                detectionEventRepository.markEventAsFalsePositive(eventId)
                _uiState.update { 
                    it.copy(
                        event = it.event?.copy(isFalsePositive = true)
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        error = "Error marking event: ${e.message}"
                    )
                }
            }
        }
    }
}

/**
 * UI state for the event details screen
 */
data class EventDetailsUiState(
    val event: DetectionEvent? = null,
    val isLoading: Boolean = false,
    val isArchived: Boolean = false,
    val error: String? = null
)
