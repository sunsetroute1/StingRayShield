package com.stingrayshield.util

import android.content.Context
import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast

/**
 * Utility class for handling compatibility across different Android versions,
 * specifically focused on Android 15 (API 35) and Android 16 features.
 */
object AndroidVersionCompatibility {
    
    // Android 15 / API 35
    const val API_ANDROID_15 = 35
    
    // Future Android 16 (estimated API 36)
    const val API_ANDROID_16_ESTIMATE = 36
    
    /**
     * Checks if the device is running Android 15 or higher
     */
    @ChecksSdkIntAtLeast(api = API_ANDROID_15)
    fun isAndroid15OrHigher(): Boolean = Build.VERSION.SDK_INT >= API_ANDROID_15
    
    /**
     * Checks if the device might be running Android 16 or higher (future-proofing)
     */
    fun isPotentialAndroid16OrHigher(): Boolean = Build.VERSION.SDK_INT >= API_ANDROID_16_ESTIMATE
    
    /**
     * Apply appropriate configurations based on Android version
     */
    fun applyVersionSpecificConfigurations(context: Context) {
        when {
            isPotentialAndroid16OrHigher() -> {
                // Apply Android 16+ specific configurations
                // This will be implemented when Android 16 specs are known
                applyAndroid16Configurations(context)
            }
            isAndroid15OrHigher() -> {
                // Apply Android 15 specific configurations
                applyAndroid15Configurations(context)
            }
        }
    }
    
    private fun applyAndroid15Configurations(context: Context) {
        // Android 15 specific logic
        // For example, handling specific telephony API changes in Android 15
        // or OneUI 7.0 compatibility adjustments
    }
    
    private fun applyAndroid16Configurations(context: Context) {
        // First apply Android 15 configurations as a baseline
        applyAndroid15Configurations(context)
        
        // Apply additional Android 16 specific logic when available
        // This is a placeholder for future implementation
    }
}
