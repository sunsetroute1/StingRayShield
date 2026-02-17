package com.stingrayshield

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.stingrayshield.util.AndroidVersionCompatibility
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class StingrayShieldApp : Application() {

    companion object {
        const val DETECTOR_CHANNEL_ID = "stingray_detector_channel"
        const val ALERT_CHANNEL_ID = "stingray_alert_channel"
    }

    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase with privacy settings
        initializeFirebase()
        
        createNotificationChannels()
        
        // Apply Android 15 and 16 compatibility configurations
        AndroidVersionCompatibility.applyVersionSpecificConfigurations(this)
    }
    
    /**
     * Initialize Firebase with privacy-first configuration
     * Analytics disabled by default, Crashlytics enabled for stability
     */
    private fun initializeFirebase() {
        try {
            // Initialize Firebase
            FirebaseApp.initializeApp(this)
            
            // Configure Crashlytics - enabled but privacy-conscious
            FirebaseCrashlytics.getInstance().apply {
                // Enable crash collection for app stability
                setCrashlyticsCollectionEnabled(true)
                
                // Set only non-identifying custom keys
                setCustomKey("app_flavor", "release")
                setCustomKey("obfuscated", "true")
            }
            
            // Analytics is configured in FirebaseManager and disabled by default
            
        } catch (e: Exception) {
            // Firebase init failed - app continues to work without it
            android.util.Log.w("StingrayShieldApp", "Firebase initialization skipped: ${e.message}")
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the detector service notification channel
            val serviceChannel = NotificationChannel(
                DETECTOR_CHANNEL_ID,
                "Stingray Detector Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Channel for the stingray detector background service"
                setShowBadge(false)
            }

            // Create the alert notification channel
            val alertChannel = NotificationChannel(
                ALERT_CHANNEL_ID,
                "Stingray Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for stingray detection alerts"
                setShowBadge(true)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(serviceChannel)
            notificationManager.createNotificationChannel(alertChannel)
        }
    }
}
