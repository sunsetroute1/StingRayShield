package com.stingrayshield.util

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Simple in-app debug log for displaying on UI when Logcat isn't available.
 * Stores the most recent log entries in memory.
 */
object DebugLog {
    private const val MAX_ENTRIES = 50
    private val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.US)
    
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()
    
    data class LogEntry(
        val timestamp: String,
        val level: String,
        val tag: String,
        val message: String
    ) {
        override fun toString(): String = "[$timestamp] $level/$tag: $message"
    }
    
    private fun addLog(level: String, tag: String, message: String) {
        val entry = LogEntry(
            timestamp = dateFormat.format(Date()),
            level = level,
            tag = tag,
            message = message
        )
        
        val currentLogs = _logs.value.toMutableList()
        currentLogs.add(0, entry) // Add to front
        if (currentLogs.size > MAX_ENTRIES) {
            currentLogs.removeAt(currentLogs.lastIndex)
        }
        _logs.value = currentLogs
    }
    
    fun d(tag: String, message: String) {
        Log.d(tag, message)
        addLog("D", tag, message)
    }
    
    fun i(tag: String, message: String) {
        Log.i(tag, message)
        addLog("I", tag, message)
    }
    
    fun w(tag: String, message: String) {
        Log.w(tag, message)
        addLog("W", tag, message)
    }
    
    fun e(tag: String, message: String) {
        Log.e(tag, message)
        addLog("E", tag, message)
    }
    
    fun clear() {
        _logs.value = emptyList()
    }
}






