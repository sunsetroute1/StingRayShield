package com.stingrayshield.ui.viewmodel

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.stingrayshield.data.repository.CellTowerRepository
import com.stingrayshield.data.repository.DetectionEventRepository
import com.stingrayshield.domain.model.DetectionEvent
import com.stingrayshield.domain.model.SystemStatus
import com.stingrayshield.service.DetectorService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Main ViewModel for the StingrayShield app
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cellTowerRepository: CellTowerRepository,
    private val detectionEventRepository: DetectionEventRepository
) : ViewModel() {

    // Permission state
    private val _permissionState = MutableStateFlow(PermissionState())
    val permissionState: StateFlow<PermissionState> = _permissionState.asStateFlow()

    // System status
    private val _systemStatus = MutableStateFlow(SystemStatus())
    val systemStatus: StateFlow<SystemStatus> = _systemStatus.asStateFlow()

    // Recent detection events
    private val _recentEvents = MutableStateFlow<List<DetectionEvent>>(emptyList())
    val recentEvents: StateFlow<List<DetectionEvent>> = _recentEvents.asStateFlow()

    // Event count by type
    private val _eventsByType = MutableStateFlow<Map<String, Int>>(emptyMap())
    val eventsByType: StateFlow<Map<String, Int>> = _eventsByType.asStateFlow()

    // Broadcast receiver for service status updates
    private val statusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                DetectorService.ACTION_STATUS_UPDATE -> {
                    val status = intent.getParcelableExtra<SystemStatus>(DetectorService.EXTRA_STATUS)
                    status?.let { updateSystemStatus(it) }
                }
                DetectorService.ACTION_DETECTION_EVENT -> {
                    val event = intent.getParcelableExtra<DetectionEvent>(DetectorService.EXTRA_EVENT)
                    event?.let { handleNewDetectionEvent(it) }
                }
            }
        }
    }

    init {
        // Register broadcast receivers
        val intentFilter = IntentFilter().apply {
            addAction(DetectorService.ACTION_STATUS_UPDATE)
            addAction(DetectorService.ACTION_DETECTION_EVENT)
        }
        LocalBroadcastManager.getInstance(context)
            .registerReceiver(statusReceiver, intentFilter)

        // Load recent events
        loadRecentEvents()
        
        // Calculate event statistics
        calculateEventStatistics()
    }

    /**
     * Update the app's permission state
     */
    fun updatePermissionState(
        locationGranted: Boolean,
        phoneStateGranted: Boolean,
        notificationsGranted: Boolean
    ) {
        _permissionState.update { currentState ->
            currentState.copy(
                locationGranted = locationGranted,
                phoneStateGranted = phoneStateGranted,
                notificationsGranted = notificationsGranted,
                allRequiredPermissionsGranted = locationGranted && phoneStateGranted
            )
        }
    }

    /**
     * Update the system status
     */
    private fun updateSystemStatus(newStatus: SystemStatus) {
        _systemStatus.update { newStatus }
    }

    /**
     * Update the service running state
     */
    fun updateServiceRunningState(isRunning: Boolean) {
        _systemStatus.update { currentStatus ->
            currentStatus.copy(isServiceRunning = isRunning)
        }
    }

    /**
     * Handle a new detection event
     */
    private fun handleNewDetectionEvent(event: DetectionEvent) {
        viewModelScope.launch {
            // Save to database
            detectionEventRepository.addDetectionEvent(event)
            
            // Reload recent events
            loadRecentEvents()
            
            // Recalculate statistics
            calculateEventStatistics()
        }
    }

    /**
     * Load recent detection events
     */
    private fun loadRecentEvents() {
        viewModelScope.launch {
            detectionEventRepository.getUnarchivedEvents().collect { events ->
                _recentEvents.update { events.sortedByDescending { it.timestamp }.take(10) }
            }
        }
    }

    /**
     * Calculate event statistics by type
     */
    private fun calculateEventStatistics() {
        viewModelScope.launch {
            detectionEventRepository.getAllEvents().collect { events ->
                val countByType = events.groupingBy { it.anomalyType.name }
                    .eachCount()
                _eventsByType.update { countByType }
            }
        }
    }

    /**
     * Archive a detection event
     */
    fun archiveEvent(eventId: Long) {
        viewModelScope.launch {
            detectionEventRepository.archiveEvent(eventId)
            loadRecentEvents() // Reload to reflect changes
        }
    }

    override fun onCleared() {
        super.onCleared()
        LocalBroadcastManager.getInstance(context).unregisterReceiver(statusReceiver)
    }
    
    /**
     * Check and update permission state
     */
    fun checkPermissions(context: Context) {
        val locationGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        val phoneStateGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
        
        val notificationsGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true
        
        val backgroundLocationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else true
        
        updatePermissionState(
            locationGranted = locationGranted,
            phoneStateGranted = phoneStateGranted,
            notificationsGranted = notificationsGranted
        )
        
        _permissionState.update { it.copy(backgroundLocationGranted = backgroundLocationGranted) }
    }

    /**
     * Class representing the current permission state
     */
    data class PermissionState(
        val locationGranted: Boolean = false,
        val phoneStateGranted: Boolean = false,
        val notificationsGranted: Boolean = false,
        val backgroundLocationGranted: Boolean = false,
        val allRequiredPermissionsGranted: Boolean = false
    )
}
