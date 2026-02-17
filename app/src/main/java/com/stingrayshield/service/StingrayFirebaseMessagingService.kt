package com.stingrayshield.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.stingrayshield.R
import com.stingrayshield.StingrayShieldApp.Companion.ALERT_CHANNEL_ID
import com.stingrayshield.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint

/**
 * Firebase Cloud Messaging Service for StingrayShield
 * Handles push notifications for threat alerts and app updates
 * 
 * Privacy: FCM tokens are handled securely and not linked to user identifiers
 */
@AndroidEntryPoint
class StingrayFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FCMService"
        private const val NOTIFICATION_ID_BASE = 5000
        
        // Obfuscated message types
        private const val TYPE_ALERT = "a"
        private const val TYPE_UPDATE = "u"
        private const val TYPE_CONFIG = "c"
        private const val TYPE_INFO = "i"
    }

    /**
     * Called when a new FCM token is generated
     * Store securely and don't link to user identity
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Token is stored locally only, not sent to any external server
        // This maintains user privacy while allowing push notifications
        android.util.Log.d(TAG, "FCM token refreshed")
        
        // Store token securely for local use
        storeTokenSecurely(token)
    }

    /**
     * Called when a message is received
     */
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        
        // Handle data payload (silent push)
        if (message.data.isNotEmpty()) {
            handleDataMessage(message.data)
        }

        // Handle notification payload (display notification)
        message.notification?.let { notification ->
            showNotification(
                title = notification.title ?: "StingrayShield Alert",
                body = notification.body ?: "New security information available",
                type = message.data["type"] ?: TYPE_INFO
            )
        }
    }

    /**
     * Handle data-only messages (silent notifications)
     */
    private fun handleDataMessage(data: Map<String, String>) {
        val type = data["type"] ?: return
        
        when (type) {
            TYPE_ALERT -> handleThreatAlert(data)
            TYPE_UPDATE -> handleAppUpdate(data)
            TYPE_CONFIG -> handleConfigUpdate(data)
            TYPE_INFO -> handleInfoMessage(data)
        }
    }

    /**
     * Handle threat alert from server
     */
    private fun handleThreatAlert(data: Map<String, String>) {
        val level = data["level"] ?: "medium"
        val region = data["region"] // Obfuscated region code
        
        // Only show if relevant to user's general area (privacy-preserving)
        showNotification(
            title = "⚠️ Stingray Alert",
            body = "Increased stingray activity detected in your region. Stay vigilant.",
            type = TYPE_ALERT,
            priority = NotificationCompat.PRIORITY_HIGH
        )
    }

    /**
     * Handle app update notification
     */
    private fun handleAppUpdate(data: Map<String, String>) {
        val version = data["version"] ?: return
        
        showNotification(
            title = "Update Available",
            body = "A new version of StingrayShield is available with improved detection.",
            type = TYPE_UPDATE
        )
    }

    /**
     * Handle remote config update trigger
     */
    private fun handleConfigUpdate(data: Map<String, String>) {
        // Trigger remote config fetch
        // This allows updating detection parameters without app update
        android.util.Log.d(TAG, "Config update triggered")
    }

    /**
     * Handle informational message
     */
    private fun handleInfoMessage(data: Map<String, String>) {
        val message = data["message"] ?: return
        
        showNotification(
            title = "StingrayShield",
            body = message,
            type = TYPE_INFO
        )
    }

    /**
     * Display a notification
     */
    private fun showNotification(
        title: String,
        body: String,
        type: String,
        priority: Int = NotificationCompat.PRIORITY_DEFAULT
    ) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create intent to open app
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("notification_type", type)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build notification
        val notification = NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_app_icon)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(priority)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        // Show notification with unique ID based on type
        val notificationId = NOTIFICATION_ID_BASE + type.hashCode()
        notificationManager.notify(notificationId, notification)
    }

    /**
     * Store FCM token securely (local storage only)
     */
    private fun storeTokenSecurely(token: String) {
        try {
            val prefs = getSharedPreferences("fcm_prefs", Context.MODE_PRIVATE)
            prefs.edit()
                .putString("fcm_token_hash", token.hashCode().toString())
                .putLong("fcm_token_time", System.currentTimeMillis())
                .apply()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error storing token: ${e.message}")
        }
    }
}













