package com.stingrayshield.util

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages device control functions like airplane mode and device shutdown
 */
@Singleton
class DeviceControlManager @Inject constructor(
    private val context: Context
) {
    
    /**
     * Enable airplane mode
     * Note: Requires WRITE_SETTINGS permission and may require user interaction on newer Android versions
     */
    fun enableAirplaneMode(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                Settings.Global.putInt(
                    context.contentResolver,
                    Settings.Global.AIRPLANE_MODE_ON,
                    1
                )
                
                // Broadcast intent to notify system of airplane mode change
                val intent = Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED).apply {
                    putExtra("state", true)
                }
                context.sendBroadcast(intent)
                true
            } else {
                // For older Android versions, open settings
                openAirplaneModeSettings()
                false
            }
        } catch (e: SecurityException) {
            android.util.Log.e("DeviceControlManager", "Permission denied for airplane mode", e)
            openAirplaneModeSettings()
            false
        } catch (e: Exception) {
            android.util.Log.e("DeviceControlManager", "Error enabling airplane mode", e)
            openAirplaneModeSettings()
            false
        }
    }
    
    /**
     * Disable airplane mode
     */
    fun disableAirplaneMode(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                Settings.Global.putInt(
                    context.contentResolver,
                    Settings.Global.AIRPLANE_MODE_ON,
                    0
                )
                
                // Broadcast intent to notify system of airplane mode change
                val intent = Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED).apply {
                    putExtra("state", false)
                }
                context.sendBroadcast(intent)
                true
            } else {
                openAirplaneModeSettings()
                false
            }
        } catch (e: SecurityException) {
            android.util.Log.e("DeviceControlManager", "Permission denied for airplane mode", e)
            openAirplaneModeSettings()
            false
        } catch (e: Exception) {
            android.util.Log.e("DeviceControlManager", "Error disabling airplane mode", e)
            openAirplaneModeSettings()
            false
        }
    }
    
    /**
     * Check if airplane mode is currently enabled
     */
    fun isAirplaneModeEnabled(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                Settings.Global.getInt(
                    context.contentResolver,
                    Settings.Global.AIRPLANE_MODE_ON,
                    0
                ) != 0
            } else {
                false
            }
        } catch (e: Exception) {
            android.util.Log.e("DeviceControlManager", "Error checking airplane mode", e)
            false
        }
    }
    
    /**
     * Open airplane mode settings for manual toggle
     */
    private fun openAirplaneModeSettings() {
        try {
            val intent = Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        } catch (e: Exception) {
            android.util.Log.e("DeviceControlManager", "Error opening airplane mode settings", e)
        }
    }
    
    /**
     * Request device shutdown
     * Note: This requires root access or system-level permissions on most devices
     * On most consumer devices, this will just show instructions
     */
    fun requestDeviceShutdown(): Boolean {
        return try {
            // ACTION_REQUEST_SHUTDOWN is not available on standard Android devices
            // Instead, we'll try to open the power dialog or show instructions
            // On some devices, we can trigger the power dialog
            val intent = Intent("android.intent.action.ACTION_REQUEST_SHUTDOWN")
            intent.putExtra("android.intent.extra.KEY_CONFIRM", false)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            android.util.Log.e("DeviceControlManager", "Error requesting device shutdown", e)
            // Fallback: Show instructions for manual shutdown
            showShutdownInstructions()
            false
        }
    }
    
    /**
     * Show instructions for manual device shutdown
     */
    private fun showShutdownInstructions() {
        // This would typically show a dialog or notification
        // For now, we'll log it
        android.util.Log.i("DeviceControlManager", "Device shutdown requires manual action. Please use the device's power button.")
    }
}

