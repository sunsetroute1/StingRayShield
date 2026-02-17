package com.stingrayshield.util

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.telephony.CellInfo
import android.telephony.CellLocation
import android.telephony.PhoneStateListener
import android.telephony.ServiceState
import android.telephony.SignalStrength
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Helper class for handling telephony operations across different Android versions
 * Including Android 15 and future Android 16
 */
class TelephonyHelper(private val context: Context) {
    
    private val telephonyManager: TelephonyManager by lazy {
        context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    }
    
    /**
     * Gets current cell information
     */
    @SuppressLint("MissingPermission")
    fun getAllCellInfo(): List<CellInfo>? {
        return try {
            telephonyManager.allCellInfo
        } catch (e: SecurityException) {
            null
        }
    }
    
    /**
     * Creates a Flow of cell info updates that works across Android versions
     */
    @RequiresPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
    fun cellInfoUpdates(): Flow<List<CellInfo>> = callbackFlow {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ uses TelephonyCallback
            val callback = createModernCellInfoCallback {
                trySend(it)
            }
            telephonyManager.registerTelephonyCallback(
                context.mainExecutor,
                callback
            )
            awaitClose {
                telephonyManager.unregisterTelephonyCallback(callback)
            }
        } else {
            // Older versions use PhoneStateListener
            val listener = createLegacyPhoneStateListener {
                trySend(it)
            }
            telephonyManager.listen(
                listener,
                PhoneStateListener.LISTEN_CELL_INFO
            )
            awaitClose {
                telephonyManager.listen(listener, PhoneStateListener.LISTEN_NONE)
            }
        }
    }
    
    /**
     * Creates a signal strength flow that works across all Android versions
     */
    @RequiresPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
    fun signalStrengthUpdates(): Flow<SignalStrength> = callbackFlow {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ uses TelephonyCallback
            val callback = createModernSignalStrengthCallback {
                trySend(it)
            }
            telephonyManager.registerTelephonyCallback(
                context.mainExecutor,
                callback
            )
            awaitClose {
                telephonyManager.unregisterTelephonyCallback(callback)
            }
        } else {
            // Older versions use PhoneStateListener
            val listener = createLegacySignalStrengthListener {
                trySend(it)
            }
            telephonyManager.listen(
                listener,
                PhoneStateListener.LISTEN_SIGNAL_STRENGTHS
            )
            awaitClose {
                telephonyManager.listen(listener, PhoneStateListener.LISTEN_NONE)
            }
        }
    }
    
    /**
     * Creates a service state flow that works across all Android versions
     * Particularly important for detecting cell changes
     */
    @RequiresPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
    fun serviceStateUpdates(): Flow<ServiceState> = callbackFlow {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ uses TelephonyCallback
            val callback = createModernServiceStateCallback {
                trySend(it)
            }
            telephonyManager.registerTelephonyCallback(
                context.mainExecutor,
                callback
            )
            awaitClose {
                telephonyManager.unregisterTelephonyCallback(callback)
            }
        } else {
            // Older versions use PhoneStateListener
            val listener = createLegacyServiceStateListener {
                trySend(it)
            }
            telephonyManager.listen(
                listener,
                PhoneStateListener.LISTEN_SERVICE_STATE
            )
            awaitClose {
                telephonyManager.listen(listener, PhoneStateListener.LISTEN_NONE)
            }
        }
    }
    
    @RequiresApi(Build.VERSION_CODES.S)
    private fun createModernCellInfoCallback(
        onCellInfoChanged: (List<CellInfo>) -> Unit
    ): TelephonyCallback {
        return object : TelephonyCallback(), TelephonyCallback.CellInfoListener {
            override fun onCellInfoChanged(cellInfo: List<CellInfo>) {
                onCellInfoChanged(cellInfo)
            }
        }
    }
    
    @RequiresApi(Build.VERSION_CODES.S)
    private fun createModernSignalStrengthCallback(
        onSignalStrengthsChanged: (SignalStrength) -> Unit
    ): TelephonyCallback {
        return object : TelephonyCallback(), TelephonyCallback.SignalStrengthsListener {
            override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
                onSignalStrengthsChanged(signalStrength)
            }
        }
    }
    
    @RequiresApi(Build.VERSION_CODES.S)
    private fun createModernServiceStateCallback(
        onServiceStateChanged: (ServiceState) -> Unit
    ): TelephonyCallback {
        return object : TelephonyCallback(), TelephonyCallback.ServiceStateListener {
            override fun onServiceStateChanged(serviceState: ServiceState) {
                onServiceStateChanged(serviceState)
            }
        }
    }
    
    private fun createLegacyPhoneStateListener(
        onCellInfoChanged: (List<CellInfo>) -> Unit
    ): PhoneStateListener {
        return object : PhoneStateListener() {
            @Suppress("DEPRECATION")
            override fun onCellInfoChanged(cellInfo: List<CellInfo>) {
                onCellInfoChanged(cellInfo)
            }
        }
    }
    
    private fun createLegacySignalStrengthListener(
        onSignalStrengthsChanged: (SignalStrength) -> Unit
    ): PhoneStateListener {
        return object : PhoneStateListener() {
            @Suppress("DEPRECATION")
            override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
                onSignalStrengthsChanged(signalStrength)
            }
        }
    }
    
    private fun createLegacyServiceStateListener(
        onServiceStateChanged: (ServiceState) -> Unit
    ): PhoneStateListener {
        return object : PhoneStateListener() {
            @Suppress("DEPRECATION")
            override fun onServiceStateChanged(serviceState: ServiceState) {
                onServiceStateChanged(serviceState)
            }
        }
    }
    
    /**
     * Check for potential IMSI catcher anomalies based on cell information
     * This replaces functionality we would have had from the alpha library
     */
    fun detectPotentialStingrayFromCellInfo(cellInfoList: List<CellInfo>): Boolean {
        // Implementation of stingray detection logic
        // This would analyze signal parameters, cell transitions, etc.
        return false // Placeholder return, actual implementation would be more complex
    }
}
