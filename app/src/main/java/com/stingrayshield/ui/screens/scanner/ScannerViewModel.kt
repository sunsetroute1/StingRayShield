package com.stingrayshield.ui.screens.scanner

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.stingrayshield.data.repository.CellTowerRepository
import com.stingrayshield.detection.StingrayDetector
import com.stingrayshield.domain.model.CellTower
import com.stingrayshield.util.TelephonyHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlin.coroutines.resume

/**
 * ViewModel for the scanner screen
 */
@HiltViewModel
class ScannerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cellTowerRepository: CellTowerRepository,
    private val stingrayDetector: StingrayDetector,
    private val telephonyHelper: TelephonyHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()
    
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    // Current cell towers from the repository
    val currentCellTowers: StateFlow<List<CellTower>> = cellTowerRepository
        .getCurrentCellTowers()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    // Neighboring cell towers
    val neighboringCellTowers: StateFlow<List<CellTower>> = cellTowerRepository
        .getNeighboringCellTowers()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    // Recent cell tower history (from the last hour)
    val recentCellTowers: StateFlow<List<CellTower>> = cellTowerRepository
        .getAllCellTowers()
        .map { towers ->
            val oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS).toEpochMilli()
            towers.filter { it.lastSeen >= oneHourAgo }
                  .sortedByDescending { it.lastSeen }
                  .distinctBy { it.getCellIdentity() }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        refreshScannerData()
    }

    /**
     * Refresh the scanner data by performing an immediate cell tower scan
     */
    @SuppressLint("MissingPermission")
    fun refreshScannerData() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }

                // Get current location first
                val location = getCurrentLocation()
                
                // Get current cell info from telephony
                val cellInfoList = telephonyHelper.getAllCellInfo()
                
                if (cellInfoList != null && cellInfoList.isNotEmpty()) {
                    // Process cell info through the detector with location (which will store the towers)
                    stingrayDetector.processCellInfo(cellInfoList, location)
                    
                    _uiState.update { 
                        it.copy(
                            lastScanTimeMillis = System.currentTimeMillis(),
                            isLoading = false,
                            error = null
                        )
                    }
                } else {
                    _uiState.update { 
                        it.copy(
                            lastScanTimeMillis = System.currentTimeMillis(),
                            isLoading = false,
                            error = "No cell towers detected. Check permissions and cellular connection."
                        )
                    }
                }
            } catch (e: SecurityException) {
                _uiState.update { 
                    it.copy(
                        error = "Permission denied: Please grant location and phone state permissions.",
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        error = "Failed to refresh scanner data: ${e.message}",
                        isLoading = false
                    )
                }
            }
        }
    }
    
    /**
     * Get current device location
     */
    @SuppressLint("MissingPermission")
    private suspend fun getCurrentLocation(): Location? {
        return try {
            suspendCancellableCoroutine { continuation ->
                val cancellationToken = CancellationTokenSource()
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cancellationToken.token
                ).addOnSuccessListener { location ->
                    continuation.resume(location)
                }.addOnFailureListener {
                    continuation.resume(null)
                }
                
                continuation.invokeOnCancellation {
                    cancellationToken.cancel()
                }
            }
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * UI state for the scanner screen
 */
data class ScannerUiState(
    val lastScanTimeMillis: Long = 0L,
    val isLoading: Boolean = false,
    val error: String? = null
)
