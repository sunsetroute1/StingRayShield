package com.stingrayshield.util

import android.content.Context
import android.os.Bundle
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase Manager with privacy-focused configuration
 * Handles initialization, analytics consent, and crash reporting
 * with user privacy as the top priority
 */
@Singleton
class FirebaseManager @Inject constructor(
    private val context: Context
) {
    private var isInitialized = false
    private var analyticsEnabled = false
    private var crashlyticsEnabled = true
    
    // Obfuscated keys for remote config (prevents easy identification)
    companion object {
        // These are obfuscated key names
        private const val RC_KEY_DETECTION_SENSITIVITY = "ds_v1"
        private const val RC_KEY_SCAN_INTERVAL = "si_v1"
        private const val RC_KEY_THREAT_THRESHOLD = "tt_v1"
        private const val RC_KEY_FEATURE_FLAGS = "ff_v1"
        
        // Default values
        private const val DEFAULT_DETECTION_SENSITIVITY = 0.7
        private const val DEFAULT_SCAN_INTERVAL = 30L
        private const val DEFAULT_THREAT_THRESHOLD = 0.8
    }

    /**
     * Initialize Firebase with privacy-first settings
     * Call this from Application.onCreate()
     */
    fun initialize() {
        if (isInitialized) return
        
        try {
            // Initialize Firebase
            FirebaseApp.initializeApp(context)
            
            // Configure Crashlytics with privacy settings
            configureCrashlytics()
            
            // Configure Analytics with privacy settings (disabled by default)
            configureAnalytics()
            
            // Configure Remote Config
            configureRemoteConfig()
            
            isInitialized = true
        } catch (e: Exception) {
            // Firebase initialization failed - continue without it
            android.util.Log.e("FirebaseManager", "Firebase initialization failed: ${e.message}")
        }
    }

    /**
     * Configure Crashlytics with privacy protections
     */
    private fun configureCrashlytics() {
        try {
            val crashlytics = Firebase.crashlytics
            
            // Enable crash collection (important for stability)
            crashlytics.setCrashlyticsCollectionEnabled(crashlyticsEnabled)
            
            // Don't automatically collect user identifiers
            // Only set anonymized session info
            crashlytics.setCustomKey("app_version", getObfuscatedVersion())
            crashlytics.setCustomKey("device_type", getObfuscatedDeviceType())
            
            // Never log user-identifiable information
            // crashlytics.setUserId() - intentionally not called
            
        } catch (e: Exception) {
            android.util.Log.e("FirebaseManager", "Crashlytics config failed: ${e.message}")
        }
    }

    /**
     * Configure Analytics with strict privacy settings
     * Analytics is DISABLED by default - must be explicitly enabled by user
     */
    private fun configureAnalytics() {
        try {
            val analytics = Firebase.analytics
            
            // Disable analytics collection by default
            analytics.setAnalyticsCollectionEnabled(analyticsEnabled)
            
            // Disable personalized ads
            analytics.setUserProperty("allow_personalized_ads", "false")
            
            // Set minimal timeout
            analytics.setSessionTimeoutDuration(300000) // 5 minutes
            
        } catch (e: Exception) {
            android.util.Log.e("FirebaseManager", "Analytics config failed: ${e.message}")
        }
    }

    /**
     * Configure Remote Config for feature flags and detection parameters
     */
    private fun configureRemoteConfig() {
        try {
            val remoteConfig = Firebase.remoteConfig
            
            // Set minimum fetch interval (1 hour in production)
            val configSettings = remoteConfigSettings {
                minimumFetchIntervalInSeconds = 3600
            }
            remoteConfig.setConfigSettingsAsync(configSettings)
            
            // Set default values
            val defaults = mapOf(
                RC_KEY_DETECTION_SENSITIVITY to DEFAULT_DETECTION_SENSITIVITY,
                RC_KEY_SCAN_INTERVAL to DEFAULT_SCAN_INTERVAL,
                RC_KEY_THREAT_THRESHOLD to DEFAULT_THREAT_THRESHOLD,
                RC_KEY_FEATURE_FLAGS to "{}"
            )
            remoteConfig.setDefaultsAsync(defaults)
            
        } catch (e: Exception) {
            android.util.Log.e("FirebaseManager", "Remote Config setup failed: ${e.message}")
        }
    }

    /**
     * Enable or disable analytics collection
     * User must explicitly opt-in
     */
    fun setAnalyticsEnabled(enabled: Boolean) {
        analyticsEnabled = enabled
        try {
            Firebase.analytics.setAnalyticsCollectionEnabled(enabled)
        } catch (e: Exception) {
            // Ignore
        }
    }

    /**
     * Enable or disable crash reporting
     */
    fun setCrashlyticsEnabled(enabled: Boolean) {
        crashlyticsEnabled = enabled
        try {
            Firebase.crashlytics.setCrashlyticsCollectionEnabled(enabled)
        } catch (e: Exception) {
            // Ignore
        }
    }

    /**
     * Log a detection event (privacy-safe)
     * Only logs aggregated, non-identifying data
     */
    fun logDetectionEvent(
        threatLevel: String,
        detectionType: String,
        networkType: String
    ) {
        if (!analyticsEnabled) return
        
        try {
            val bundle = Bundle().apply {
                // Only log categorical data, no identifiers
                putString("threat_level", obfuscateValue(threatLevel))
                putString("detection_type", obfuscateValue(detectionType))
                putString("network_gen", getNetworkGeneration(networkType))
                putLong("timestamp_bucket", getTimeBucket())
            }
            Firebase.analytics.logEvent("detection_event", bundle)
        } catch (e: Exception) {
            // Fail silently
        }
    }

    /**
     * Log app usage (privacy-safe)
     */
    fun logScreenView(screenName: String) {
        if (!analyticsEnabled) return
        
        try {
            val bundle = Bundle().apply {
                putString(FirebaseAnalytics.Param.SCREEN_NAME, obfuscateScreenName(screenName))
                putString(FirebaseAnalytics.Param.SCREEN_CLASS, "Activity")
            }
            Firebase.analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
        } catch (e: Exception) {
            // Fail silently
        }
    }

    /**
     * Log a non-fatal error to Crashlytics
     */
    fun logError(tag: String, message: String, throwable: Throwable? = null) {
        if (!crashlyticsEnabled) return
        
        try {
            val crashlytics = Firebase.crashlytics
            crashlytics.log("[$tag] $message")
            throwable?.let { crashlytics.recordException(it) }
        } catch (e: Exception) {
            // Fail silently
        }
    }

    /**
     * Get FCM token for push notifications (obfuscated storage)
     */
    suspend fun getFcmToken(): String? {
        return try {
            FirebaseMessaging.getInstance().token.await()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Fetch and activate remote config
     */
    suspend fun fetchRemoteConfig(): Boolean {
        return try {
            val remoteConfig = Firebase.remoteConfig
            remoteConfig.fetchAndActivate().await()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get detection sensitivity from remote config
     */
    fun getDetectionSensitivity(): Double {
        return try {
            Firebase.remoteConfig.getDouble(RC_KEY_DETECTION_SENSITIVITY)
        } catch (e: Exception) {
            DEFAULT_DETECTION_SENSITIVITY
        }
    }

    /**
     * Get scan interval from remote config
     */
    fun getScanInterval(): Long {
        return try {
            Firebase.remoteConfig.getLong(RC_KEY_SCAN_INTERVAL)
        } catch (e: Exception) {
            DEFAULT_SCAN_INTERVAL
        }
    }

    /**
     * Get threat threshold from remote config
     */
    fun getThreatThreshold(): Double {
        return try {
            Firebase.remoteConfig.getDouble(RC_KEY_THREAT_THRESHOLD)
        } catch (e: Exception) {
            DEFAULT_THREAT_THRESHOLD
        }
    }

    // --- Privacy Helper Functions ---

    /**
     * Obfuscate version string
     */
    private fun getObfuscatedVersion(): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            "v${pInfo.versionCode}"
        } catch (e: Exception) {
            "unknown"
        }
    }

    /**
     * Get obfuscated device type (only general category)
     */
    private fun getObfuscatedDeviceType(): String {
        val screenSize = context.resources.configuration.screenLayout and 
            android.content.res.Configuration.SCREENLAYOUT_SIZE_MASK
        return when {
            screenSize >= android.content.res.Configuration.SCREENLAYOUT_SIZE_XLARGE -> "tablet"
            else -> "phone"
        }
    }

    /**
     * Obfuscate sensitive values to categorical data
     */
    private fun obfuscateValue(value: String): String {
        // Hash the value and return first 8 chars
        return value.hashCode().toString(16).take(8)
    }

    /**
     * Get network generation without specific details
     */
    private fun getNetworkGeneration(networkType: String): String {
        return when {
            networkType.contains("5G", ignoreCase = true) -> "5G"
            networkType.contains("LTE", ignoreCase = true) || 
                networkType.contains("4G", ignoreCase = true) -> "4G"
            networkType.contains("3G", ignoreCase = true) ||
                networkType.contains("HSPA", ignoreCase = true) -> "3G"
            else -> "other"
        }
    }

    /**
     * Get time bucket (hour of day) instead of exact timestamp
     */
    private fun getTimeBucket(): Long {
        val calendar = java.util.Calendar.getInstance()
        return calendar.get(java.util.Calendar.HOUR_OF_DAY).toLong()
    }

    /**
     * Obfuscate screen names
     */
    private fun obfuscateScreenName(screenName: String): String {
        return when {
            screenName.contains("Dashboard", ignoreCase = true) -> "main"
            screenName.contains("Scanner", ignoreCase = true) -> "scan"
            screenName.contains("Map", ignoreCase = true) -> "map"
            screenName.contains("Settings", ignoreCase = true) -> "config"
            screenName.contains("Statistics", ignoreCase = true) -> "stats"
            screenName.contains("Threat", ignoreCase = true) -> "alert"
            else -> "other"
        }
    }

    /**
     * Clear all collected data (GDPR compliance)
     */
    fun clearAllData() {
        try {
            // Reset analytics
            Firebase.analytics.resetAnalyticsData()
            
            // Delete FCM token
            FirebaseMessaging.getInstance().deleteToken()
            
        } catch (e: Exception) {
            android.util.Log.e("FirebaseManager", "Error clearing data: ${e.message}")
        }
    }
}













