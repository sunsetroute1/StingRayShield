package com.stingrayshield.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.RequiresApi
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages alarm functionality for stingray detection alerts
 */
@Singleton
class AlarmManager @Inject constructor(
    private val context: Context
) {
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var isAlarmActive = false
    
    init {
        initializeVibrator()
    }
    
    private fun initializeVibrator() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }
    
    /**
     * Start alarm with sound and vibration
     */
    fun startAlarm() {
        if (isAlarmActive) return
        
        isAlarmActive = true
        playAlarmSound()
        startVibration()
    }
    
    /**
     * Stop alarm
     */
    fun stopAlarm() {
        isAlarmActive = false
        stopAlarmSound()
        stopVibration()
    }
    
    private fun playAlarmSound() {
        try {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .build()
                )
                setDataSource(context, alarmUri)
                isLooping = true
                setVolume(1.0f, 1.0f)
                prepare()
                start()
            }
        } catch (e: Exception) {
            android.util.Log.e("AlarmManager", "Error playing alarm sound", e)
        }
    }
    
    private fun stopAlarmSound() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
            mediaPlayer = null
        } catch (e: Exception) {
            android.util.Log.e("AlarmManager", "Error stopping alarm sound", e)
        }
    }
    
    private fun startVibration() {
        try {
            val pattern = longArrayOf(0, 500, 200, 500, 200, 500) // Vibrate pattern
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val vibrationEffect = VibrationEffect.createWaveform(pattern, 0) // Repeat from index 0
                vibrator?.vibrate(vibrationEffect)
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0) // Repeat from index 0
            }
        } catch (e: Exception) {
            android.util.Log.e("AlarmManager", "Error starting vibration", e)
        }
    }
    
    private fun stopVibration() {
        try {
            vibrator?.cancel()
        } catch (e: Exception) {
            android.util.Log.e("AlarmManager", "Error stopping vibration", e)
        }
    }
    
    /**
     * Check if alarm is currently active
     */
    fun isAlarmActive(): Boolean = isAlarmActive
}

