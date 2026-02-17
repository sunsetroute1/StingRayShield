package com.stingrayshield.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.telephony.CellInfo
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.stingrayshield.R
import com.stingrayshield.StingrayShieldApp.Companion.DETECTOR_CHANNEL_ID
import com.stingrayshield.domain.model.DetectionEvent
import com.stingrayshield.domain.model.DetectionStatus
import com.stingrayshield.domain.model.SystemStatus
import com.stingrayshield.domain.model.ThreatLevel
import com.stingrayshield.ui.MainActivity
import com.stingrayshield.detection.StingrayDetector
import com.stingrayshield.util.AlarmManager
import com.stingrayshield.util.DeviceControlManager
import com.stingrayshield.util.ThreatNotificationManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Background service that monitors cellular network activity for potential stingray devices.
 */
@AndroidEntryPoint
class DetectorService : Service() {

    companion object {
        const val ACTION_START_SERVICE = "com.stingrayshield.START_SERVICE"
        const val ACTION_STOP_SERVICE = "com.stingrayshield.STOP_SERVICE"
        const val ACTION_STATUS_UPDATE = "com.stingrayshield.STATUS_UPDATE"
        const val ACTION_DETECTION_EVENT = "com.stingrayshield.DETECTION_EVENT"
        const val ACTION_HIGH_THREAT_DETECTED = "com.stingrayshield.HIGH_THREAT_DETECTED"
        
        const val EXTRA_STATUS = "extra_status"
        const val EXTRA_EVENT = "extra_event"
        const val EXTRA_EVENT_ID = "extra_event_id"
        
        private const val NOTIFICATION_ID = 1001
        private const val SCAN_INTERVAL_MS = 30_000L // 30 seconds
    }
    
    @Inject lateinit var stingrayDetector: StingrayDetector
    @Inject lateinit var alarmManager: AlarmManager
    @Inject lateinit var deviceControlManager: DeviceControlManager
    @Inject lateinit var threatNotificationManager: ThreatNotificationManager
    
    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var telephonyManager: TelephonyManager? = null
    
    // Modern API (Android 12+)
    private var telephonyCallback: TelephonyCallback? = null
    
    // Legacy API (pre-Android 12)
    @Suppress("DEPRECATION")
    private var phoneStateListener: PhoneStateListener? = null
    
    private val _systemStatus = MutableStateFlow(SystemStatus())
    val systemStatus: StateFlow<SystemStatus> = _systemStatus.asStateFlow()
    
    private var scanJob: Job? = null
    private var lastKnownCellId: Int? = null
    private var lastSignalStrength: Int? = null
    private var isAlarmActive = false
    
    // Track recent threats to avoid duplicate notifications
    private val recentThreatCellIds = mutableSetOf<Int>()
    private var lastThreatNotificationTime = 0L
    private val THREAT_NOTIFICATION_COOLDOWN_MS = 60_000L // 1 minute cooldown
    
    override fun onCreate() {
        super.onCreate()
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        setupTelephonyMonitoring()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_SERVICE -> startDetectorService()
            ACTION_STOP_SERVICE -> stopSelf()
        }
        return START_STICKY
    }

    private fun startDetectorService() {
        val notification = createServiceNotification()
        startForeground(NOTIFICATION_ID, notification)
        
        _systemStatus.update { it.copy(isServiceRunning = true) }
        broadcastStatusUpdate()
        
        startPeriodicScan()
    }
    
    private fun startPeriodicScan() {
        scanJob?.cancel()
        scanJob = serviceScope.launch {
            while (true) {
                performScan()
                delay(SCAN_INTERVAL_MS)
            }
        }
    }
    
    private fun performScan() {
        serviceScope.launch {
            try {
                // Get current cell information
                val cellInfo = getCurrentCellInfo()
                
                if (cellInfo.isNotEmpty()) {
                    // Use StingrayDetector to process cell information
                    val detectedEvents = stingrayDetector.processCellInfo(cellInfo, null)
                    
                    // Check for high-threat events
                    detectedEvents.forEach { event ->
                        handleDetectionEvent(event)
                    }
                    
                    // Update status and broadcast
                    updateSystemStatus(cellInfo)
                    broadcastStatusUpdate()
                }
            } catch (e: Exception) {
                android.util.Log.e("DetectorService", "Error during scan", e)
            }
        }
    }
    
    /**
     * Handle a detection event - trigger notifications and alarms for high/critical threats
     */
    private fun handleDetectionEvent(event: DetectionEvent) {
        val isHighThreat = event.threatLevel == ThreatLevel.HIGH || event.threatLevel == ThreatLevel.CRITICAL
        
        if (isHighThreat) {
            val currentTime = System.currentTimeMillis()
            val cellId = event.cellId ?: 0
            
            // Check if we should send a notification (cooldown and deduplication)
            val shouldNotify = (currentTime - lastThreatNotificationTime > THREAT_NOTIFICATION_COOLDOWN_MS) ||
                               (cellId !in recentThreatCellIds)
            
            if (shouldNotify) {
                // Send threat notification
                threatNotificationManager.showThreatNotification(event)
                lastThreatNotificationTime = currentTime
                recentThreatCellIds.add(cellId)
                
                // Clear old cell IDs after some time
                if (recentThreatCellIds.size > 10) {
                    recentThreatCellIds.clear()
                    recentThreatCellIds.add(cellId)
                }
                
                // Start alarm for critical threats
                if (event.threatLevel == ThreatLevel.CRITICAL && !isAlarmActive) {
                    alarmManager.startAlarm()
                    isAlarmActive = true
                }
                
                // Broadcast the high threat event
                val intent = Intent(ACTION_HIGH_THREAT_DETECTED).apply {
                    putExtra(EXTRA_EVENT, event)
                    putExtra(EXTRA_EVENT_ID, event.id)
                    putExtra("alarm_triggered", event.threatLevel == ThreatLevel.CRITICAL)
                }
                LocalBroadcastManager.getInstance(this@DetectorService).sendBroadcast(intent)
                
                // Update threat level in system status
                updateThreatLevel(event)
            }
        }
        
        // Also broadcast regular detection events
        val intent = Intent(ACTION_DETECTION_EVENT).apply {
            putExtra(EXTRA_EVENT, event)
        }
        LocalBroadcastManager.getInstance(this@DetectorService).sendBroadcast(intent)
    }
    
    private fun getCurrentCellInfo(): List<CellInfo> {
        return try {
            telephonyManager?.allCellInfo ?: emptyList()
        } catch (e: SecurityException) {
            // Permission not granted
            emptyList()
        }
    }
    
    /**
     * Stop alarm (called from UI or when threat is resolved)
     */
    fun stopAlarm() {
        if (isAlarmActive) {
            alarmManager.stopAlarm()
            isAlarmActive = false
        }
    }
    
    /**
     * Dismiss threat notifications
     */
    fun dismissThreatNotifications() {
        threatNotificationManager.dismissThreatNotifications()
    }
    
    /**
     * Enable airplane mode (called from UI)
     */
    fun enableAirplaneMode(): Boolean {
        return deviceControlManager.enableAirplaneMode()
    }
    
    /**
     * Shutdown device (called from UI)
     */
    fun shutdownDevice(): Boolean {
        return deviceControlManager.requestDeviceShutdown()
    }
    
    private fun updateThreatLevel(event: DetectionEvent) {
        val currentStatus = _systemStatus.value.detectionStatus
        
        val newStatus = when (event.threatLevel) {
            ThreatLevel.CRITICAL -> DetectionStatus.DANGER
            ThreatLevel.HIGH -> DetectionStatus.ALERT
            ThreatLevel.MEDIUM -> DetectionStatus.WARNING
            ThreatLevel.LOW -> {
                // Only upgrade to WARNING if currently NORMAL
                if (currentStatus == DetectionStatus.NORMAL) 
                    DetectionStatus.WARNING else currentStatus
            }
            ThreatLevel.NONE -> currentStatus // No change
        }
        
        _systemStatus.update { 
            it.copy(
                detectionStatus = newStatus,
                detectedEvents = it.detectedEvents + 1
            )
        }
        
        // Update the notification to reflect the new status
        updateNotification()
    }
    
    private fun updateSystemStatus(cellInfoList: List<CellInfo>) {
        val primaryCell = cellInfoList.firstOrNull()
        val currentCellId = primaryCell?.let { com.stingrayshield.detection.DetectionAlgorithms.getCellIdFromCellInfo(it) }
        val signalStrength = primaryCell?.let { com.stingrayshield.detection.DetectionAlgorithms.getSignalStrengthFromCellInfo(it) }
        val networkType = telephonyManager?.dataNetworkType?.let { getNetworkTypeString(it) }
        
        _systemStatus.update { status ->
            status.copy(
                lastScanTime = System.currentTimeMillis(),
                activeScanCount = status.activeScanCount + 1,
                currentCellId = currentCellId ?: status.currentCellId,
                currentSignalStrength = signalStrength ?: status.currentSignalStrength,
                currentNetworkType = networkType ?: status.currentNetworkType
            )
        }
    }
    
    @Suppress("DEPRECATION")
    private fun getNetworkTypeString(type: Int): String {
        return when (type) {
            TelephonyManager.NETWORK_TYPE_GPRS,
            TelephonyManager.NETWORK_TYPE_EDGE,
            TelephonyManager.NETWORK_TYPE_CDMA,
            TelephonyManager.NETWORK_TYPE_1xRTT,
            TelephonyManager.NETWORK_TYPE_IDEN,
            TelephonyManager.NETWORK_TYPE_GSM -> "2G"
            
            TelephonyManager.NETWORK_TYPE_UMTS,
            TelephonyManager.NETWORK_TYPE_EVDO_0,
            TelephonyManager.NETWORK_TYPE_EVDO_A,
            TelephonyManager.NETWORK_TYPE_HSDPA,
            TelephonyManager.NETWORK_TYPE_HSUPA,
            TelephonyManager.NETWORK_TYPE_HSPA,
            TelephonyManager.NETWORK_TYPE_EVDO_B,
            TelephonyManager.NETWORK_TYPE_EHRPD,
            TelephonyManager.NETWORK_TYPE_HSPAP,
            TelephonyManager.NETWORK_TYPE_TD_SCDMA -> "3G"
            
            TelephonyManager.NETWORK_TYPE_LTE -> "4G"
            
            TelephonyManager.NETWORK_TYPE_NR -> "5G"
            
            else -> "Unknown"
        }
    }
    
    private fun broadcastStatusUpdate() {
        val intent = Intent(ACTION_STATUS_UPDATE).apply {
            putExtra(EXTRA_STATUS, systemStatus.value)
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }
    
    private fun createServiceNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val status = _systemStatus.value.detectionStatus
        val (title, text) = when (status) {
            DetectionStatus.DANGER -> "🚨 DANGER" to "Critical threat detected!"
            DetectionStatus.ALERT -> "⚠️ ALERT" to "High threat detected"
            DetectionStatus.WARNING -> "Warning" to "Suspicious activity detected"
            else -> "StingrayShield Active" to "Monitoring for stingray devices"
        }
        
        return NotificationCompat.Builder(this, DETECTOR_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_app_icon)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
    
    private fun updateNotification() {
        val notification = createServiceNotification()
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    private fun setupTelephonyMonitoring() {
        // This would implement different monitoring methods based on Android version
        // Simplified for now
    }
    
    @Suppress("DEPRECATION")
    override fun onDestroy() {
        super.onDestroy()
        scanJob?.cancel()
        
        // Clean up telephony monitoring
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            telephonyCallback?.let { callback ->
                try {
                    telephonyManager?.unregisterTelephonyCallback(callback)
                } catch (e: Exception) {
                    // Ignore
                }
            }
        } else {
            phoneStateListener?.let { listener ->
                try {
                    telephonyManager?.listen(listener, PhoneStateListener.LISTEN_NONE)
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }
        
        _systemStatus.update { it.copy(isServiceRunning = false) }
        broadcastStatusUpdate()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
