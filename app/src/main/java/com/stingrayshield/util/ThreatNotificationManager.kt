package com.stingrayshield.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.stingrayshield.R
import com.stingrayshield.domain.model.DetectionEvent
import com.stingrayshield.domain.model.ThreatLevel
import com.stingrayshield.ui.MainActivity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages threat notifications and alerts for stingray detection
 */
@Singleton
class ThreatNotificationManager @Inject constructor(
    private val context: Context
) {
    // Notification preferences (will be injected or passed from settings)
    private var enableVibration = true
    private var vibrationPattern = "default"
    private var enableSound = true
    private var notificationSound = "default"
    private var enableLights = true

    /**
     * Update notification preferences from settings
     */
    fun updateNotificationPreferences(
        enableVibration: Boolean,
        vibrationPattern: String,
        enableSound: Boolean,
        notificationSound: String,
        enableLights: Boolean
    ) {
        this.enableVibration = enableVibration
        this.vibrationPattern = vibrationPattern
        this.enableSound = enableSound
        this.notificationSound = notificationSound
        this.enableLights = enableLights

        // Recreate notification channels with new preferences
        createNotificationChannels()
    }

    companion object {
        const val THREAT_CHANNEL_ID = "stingray_threat_channel"
        const val THREAT_CHANNEL_NAME = "Stingray Threat Alerts"
        const val CRITICAL_CHANNEL_ID = "stingray_critical_channel"
        const val CRITICAL_CHANNEL_NAME = "Critical Stingray Alerts"
        
        const val NOTIFICATION_ID_THREAT = 2001
        const val NOTIFICATION_ID_CRITICAL = 2002
        
        const val ACTION_OPEN_THREAT_DETAILS = "com.stingrayshield.OPEN_THREAT_DETAILS"
        const val ACTION_ENABLE_AIRPLANE_MODE = "com.stingrayshield.ENABLE_AIRPLANE_MODE"
        const val ACTION_DISMISS_THREAT = "com.stingrayshield.DISMISS_THREAT"
        
        const val EXTRA_THREAT_EVENT_ID = "threat_event_id"
        const val EXTRA_THREAT_CELL_ID = "threat_cell_id"
        const val EXTRA_THREAT_LEVEL = "threat_level"
    }
    
    private val notificationManager = NotificationManagerCompat.from(context)
    
    init {
        createNotificationChannels()
    }
    
    /**
     * Create notification channels for different threat levels
     */
    private fun createNotificationChannels() {
        // High threat channel
        val threatChannel = NotificationChannel(
            THREAT_CHANNEL_ID,
            THREAT_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alerts when potential stingray devices are detected"

            // Apply vibration settings
            enableVibration(enableVibration)
            if (enableVibration) {
                vibrationPattern = getVibrationPattern(this@ThreatNotificationManager.vibrationPattern)
            }

            // Apply sound settings
            if (enableSound) {
                setSound(
                    getSoundUri(notificationSound),
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
            } else {
                setSound(null, null)
            }

            // Apply light settings
            enableLights(enableLights)
            if (enableLights) {
                lightColor = android.graphics.Color.YELLOW
            }
        }
        
        // Critical threat channel - maximum priority
        val criticalChannel = NotificationChannel(
            CRITICAL_CHANNEL_ID,
            CRITICAL_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Critical alerts for confirmed stingray devices"

            // Apply vibration settings (more urgent for critical)
            enableVibration(enableVibration)
            if (enableVibration) {
                vibrationPattern = getVibrationPattern(this@ThreatNotificationManager.vibrationPattern, true) // true for critical
            }

            // Apply sound settings
            if (enableSound) {
                setSound(
                    getSoundUri(notificationSound, true), // true for critical
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
            } else {
                setSound(null, null)
            }

            // Apply light settings
            enableLights(enableLights)
            if (enableLights) {
                lightColor = android.graphics.Color.RED
            }
        }
        
        val systemNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        systemNotificationManager.createNotificationChannel(threatChannel)
        systemNotificationManager.createNotificationChannel(criticalChannel)
    }
    
    /**
     * Show a threat notification for a detection event
     */
    fun showThreatNotification(event: DetectionEvent) {
        if (!hasNotificationPermission()) return
        
        val isCritical = event.threatLevel == ThreatLevel.CRITICAL
        val isHigh = event.threatLevel == ThreatLevel.HIGH
        
        if (!isCritical && !isHigh) return
        
        // Create intent to open threat details
        val detailsIntent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_OPEN_THREAT_DETAILS
            putExtra(EXTRA_THREAT_EVENT_ID, event.id)
            putExtra(EXTRA_THREAT_CELL_ID, event.cellId)
            putExtra(EXTRA_THREAT_LEVEL, event.threatLevel.name)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        val detailsPendingIntent = PendingIntent.getActivity(
            context,
            0,
            detailsIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Create airplane mode action intent
        val airplaneModeIntent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_ENABLE_AIRPLANE_MODE
            putExtra(EXTRA_THREAT_EVENT_ID, event.id)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        val airplaneModePendingIntent = PendingIntent.getActivity(
            context,
            1,
            airplaneModeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val channelId = if (isCritical) CRITICAL_CHANNEL_ID else THREAT_CHANNEL_ID
        val notificationId = if (isCritical) NOTIFICATION_ID_CRITICAL else NOTIFICATION_ID_THREAT
        
        val title = if (isCritical) {
            "🚨 CRITICAL: Stingray Device Detected!"
        } else {
            "⚠️ HIGH THREAT: Suspicious Cell Tower"
        }
        
        val contentText = buildString {
            append(event.description.take(100))
            if (event.description.length > 100) append("...")
        }
        
        val bigText = buildString {
            appendLine(event.description)
            appendLine()
            event.cellId?.let { appendLine("Cell ID: $it") }
            event.signalStrength?.let { appendLine("Signal: $it dBm") }
            appendLine()
            appendLine("Tap to view details and take action")
        }
        
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_app_icon)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(if (isCritical) NotificationCompat.PRIORITY_MAX else NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(false)
            .setOngoing(isCritical) // Critical alerts stay until dismissed
            .setContentIntent(detailsPendingIntent)
            .addAction(
                R.drawable.ic_app_icon,
                "View Details",
                detailsPendingIntent
            )
            .addAction(
                R.drawable.ic_app_icon,
                "🛡️ Disconnect",
                airplaneModePendingIntent
            )
            .setColor(if (isCritical) android.graphics.Color.RED else android.graphics.Color.rgb(255, 140, 0))
            .build()
        
        try {
            notificationManager.notify(notificationId, notification)
        } catch (e: SecurityException) {
            // Permission not granted
        }
        
        // Trigger vibration for critical threats
        if (isCritical) {
            triggerCriticalVibration()
        }
    }
    
    /**
     * Trigger a strong vibration pattern for critical threats
     */
    private fun triggerCriticalVibration() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        
        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // SOS-like pattern for critical threats
                val pattern = longArrayOf(0, 200, 100, 200, 100, 200, 300, 500, 300, 500, 300, 500, 300, 200, 100, 200, 100, 200)
                it.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(longArrayOf(0, 200, 100, 200, 100, 200, 300, 500, 300, 500, 300, 500, 300, 200, 100, 200, 100, 200), -1)
            }
        }
    }
    
    /**
     * Dismiss threat notifications
     */
    fun dismissThreatNotifications() {
        notificationManager.cancel(NOTIFICATION_ID_THREAT)
        notificationManager.cancel(NOTIFICATION_ID_CRITICAL)
    }
    
    /**
     * Check if notification permission is granted
     */
    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * Get vibration pattern based on preference
     */
    private fun getVibrationPattern(pattern: String, isCritical: Boolean = false): LongArray {
        val basePattern = when (pattern) {
            "long" -> longArrayOf(0, 1000, 200, 1000)
            "short" -> longArrayOf(0, 200, 100, 200)
            "urgent" -> longArrayOf(0, 300, 150, 300, 150, 300)
            else -> longArrayOf(0, 500, 200, 500) // default
        }

        // Make critical notifications more intense
        return if (isCritical) {
            basePattern.map { it * 2 }.toLongArray()
        } else {
            basePattern
        }
    }

    /**
     * Get sound URI based on preference
     */
    private fun getSoundUri(sound: String, isCritical: Boolean = false): android.net.Uri? {
        return when (sound) {
            "alarm" -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            "silent" -> null
            else -> { // default
                if (isCritical) {
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                } else {
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                }
            }
        }
    }
}

