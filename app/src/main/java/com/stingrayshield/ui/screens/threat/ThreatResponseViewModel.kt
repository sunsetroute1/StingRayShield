package com.stingrayshield.ui.screens.threat

import android.annotation.SuppressLint
import android.content.Context
import android.telephony.CellInfo
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellInfoWcdma
import android.telephony.CellInfoGsm
import android.telephony.CellInfoCdma
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stingrayshield.data.repository.DetectionEventRepository
import com.stingrayshield.detection.DetectionAlgorithms
import com.stingrayshield.domain.model.DetectionEvent
import com.stingrayshield.util.CellTowerLocationEstimator
import com.stingrayshield.util.TelephonyHelper
import com.stingrayshield.util.ThreatNotificationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ThreatResponseViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val detectionEventRepository: DetectionEventRepository,
    private val telephonyHelper: TelephonyHelper,
    private val threatNotificationManager: ThreatNotificationManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _currentEvent = MutableStateFlow<DetectionEvent?>(null)
    val currentEvent: StateFlow<DetectionEvent?> = _currentEvent.asStateFlow()

    private val _collectedThreatData = MutableStateFlow<ThreatData?>(null)
    val collectedThreatData: StateFlow<ThreatData?> = _collectedThreatData.asStateFlow()

    private val _isCollectingData = MutableStateFlow(false)
    val isCollectingData: StateFlow<Boolean> = _isCollectingData.asStateFlow()

    init {
        // Check for event ID passed via navigation or intent
        savedStateHandle.get<Long>("eventId")?.let { eventId ->
            loadEvent(eventId)
        }
    }

    /**
     * Load a detection event by ID
     */
    fun loadEvent(eventId: Long) {
        viewModelScope.launch {
            try {
                val event = detectionEventRepository.getEventById(eventId)
                _currentEvent.value = event
                
                if (event != null) {
                    collectThreatData(event)
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    /**
     * Set the current event directly
     */
    fun setEvent(event: DetectionEvent) {
        _currentEvent.value = event
        collectThreatData(event)
    }

    /**
     * Collect additional threat data from current cell info
     */
    @SuppressLint("MissingPermission")
    private fun collectThreatData(event: DetectionEvent) {
        _isCollectingData.value = true
        
        viewModelScope.launch {
            try {
                val cellInfoList = telephonyHelper.getAllCellInfo()
                
                // Find the cell that matches the threat
                val matchingCell = cellInfoList?.find { cellInfo ->
                    val cellId = DetectionAlgorithms.getCellIdFromCellInfo(cellInfo)
                    cellId == event.cellId
                }
                
                // If no match, use the registered cell
                val targetCell = matchingCell ?: cellInfoList?.find { it.isRegistered }
                
                if (targetCell != null) {
                    val threatData = extractThreatData(targetCell)
                    _collectedThreatData.value = threatData
                }
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isCollectingData.value = false
            }
        }
    }

    /**
     * Extract threat data from CellInfo
     */
    private fun extractThreatData(cellInfo: CellInfo): ThreatData {
        val mcc = DetectionAlgorithms.getMccFromCellInfo(cellInfo)?.toString()
        val mnc = DetectionAlgorithms.getMncFromCellInfo(cellInfo)?.toString()
        val networkType = DetectionAlgorithms.getNetworkTypeFromCellInfo(cellInfo)
        val carrierName = CellTowerLocationEstimator.getCarrierName(mcc, mnc)
        
        var pci: Int? = null
        var arfcn: Int? = null
        var bandwidthKhz: Int? = null
        var timingAdvance: Int? = null
        
        when (cellInfo) {
            is CellInfoLte -> {
                pci = cellInfo.cellIdentity.pci.takeIf { it != Int.MAX_VALUE && it >= 0 }
                arfcn = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    cellInfo.cellIdentity.earfcn.takeIf { it != Int.MAX_VALUE && it >= 0 }
                } else null
                bandwidthKhz = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    cellInfo.cellIdentity.bandwidth.takeIf { it != Int.MAX_VALUE && it > 0 }
                } else null
                timingAdvance = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    cellInfo.cellSignalStrength.timingAdvance.takeIf { it != Int.MAX_VALUE && it >= 0 }
                } else null
            }
            is CellInfoWcdma -> {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    arfcn = cellInfo.cellIdentity.uarfcn.takeIf { it != Int.MAX_VALUE && it >= 0 }
                }
            }
            is CellInfoGsm -> {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    arfcn = cellInfo.cellIdentity.arfcn.takeIf { it != Int.MAX_VALUE && it >= 0 }
                }
                timingAdvance = cellInfo.cellSignalStrength.timingAdvance.takeIf { it != Int.MAX_VALUE && it >= 0 }
            }
        }
        
        // Handle 5G NR
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q && cellInfo is CellInfoNr) {
            val identity = cellInfo.cellIdentity as? android.telephony.CellIdentityNr
            pci = identity?.pci?.takeIf { it != Int.MAX_VALUE && it >= 0 }
            arfcn = identity?.nrarfcn?.takeIf { it != Int.MAX_VALUE && it >= 0 }
        }
        
        return ThreatData(
            mcc = mcc,
            mnc = mnc,
            carrierName = carrierName,
            networkType = networkType,
            pci = pci,
            arfcn = arfcn,
            bandwidthKhz = bandwidthKhz,
            timingAdvance = timingAdvance
        )
    }

    /**
     * Dismiss the threat notification
     */
    fun dismissNotification() {
        threatNotificationManager.dismissThreatNotifications()
    }

    /**
     * Refresh threat data collection
     */
    fun refreshThreatData() {
        _currentEvent.value?.let { event ->
            collectThreatData(event)
        }
    }
}

