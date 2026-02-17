package com.stingrayshield.data.repository

import com.stingrayshield.data.database.DetectionEventDao
import com.stingrayshield.domain.model.AnomalyType
import com.stingrayshield.domain.model.DetectionEvent
import com.stingrayshield.domain.model.ThreatLevel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing stingray detection events
 */
@Singleton
class DetectionEventRepository @Inject constructor(
    private val detectionEventDao: DetectionEventDao
) {
    /**
     * Add a new detection event to the database
     */
    suspend fun addDetectionEvent(event: DetectionEvent): Long {
        return detectionEventDao.insertDetectionEvent(event)
    }

    /**
     * Get all detection events as a Flow
     */
    fun getAllEvents(): Flow<List<DetectionEvent>> {
        return detectionEventDao.getAllEventsAsFlow()
    }

    /**
     * Get events filtered by threat level
     */
    fun getEventsByThreatLevel(threatLevels: List<ThreatLevel>): Flow<List<DetectionEvent>> {
        val threatLevelStrings = threatLevels.map { it.name }
        return detectionEventDao.getEventsByThreatLevel(threatLevelStrings)
    }

    /**
     * Get events filtered by anomaly type
     */
    fun getEventsByType(anomalyTypes: List<AnomalyType>): Flow<List<DetectionEvent>> {
        val anomalyTypeStrings = anomalyTypes.map { it.name }
        return detectionEventDao.getEventsByType(anomalyTypeStrings)
    }

    /**
     * Get events that haven't been archived yet
     */
    fun getUnarchivedEvents(): Flow<List<DetectionEvent>> {
        return detectionEventDao.getUnarchivedEvents()
    }

    /**
     * Get events within a specific time range
     */
    suspend fun getEventsInTimeRange(startTime: Long, endTime: Long): List<DetectionEvent> {
        return detectionEventDao.getEventsInTimeRange(startTime, endTime)
    }

    /**
     * Update an existing detection event
     */
    suspend fun updateEvent(event: DetectionEvent) {
        detectionEventDao.updateEvent(event)
    }

    /**
     * Archive a specific event
     */
    suspend fun archiveEvent(eventId: Long) {
        detectionEventDao.archiveEvent(eventId)
    }

    /**
     * Get count of recent events with a specific threat level
     */
    suspend fun getRecentEventCountByThreatLevel(
        threatLevel: ThreatLevel,
        sinceTime: Long
    ): Int {
        return detectionEventDao.getRecentEventCountByThreatLevel(threatLevel.name, sinceTime)
    }

    /**
     * Delete events older than a specific timestamp
     */
    suspend fun deleteOldEvents(timestamp: Long): Int {
        return detectionEventDao.deleteOldEvents(timestamp)
    }

    /**
     * Delete all detection events from the database
     */
    suspend fun clearAllData(): Int {
        return detectionEventDao.deleteAllEvents()
    }
    
    /**
     * Get events for a specific cell ID as a Flow
     */
    fun getEventsByCellId(cellId: Int): Flow<List<DetectionEvent>> {
        return detectionEventDao.getEventsByCellId(cellId)
    }
    
    /**
     * Get a single event by its ID
     */
    suspend fun getEventById(eventId: Long): DetectionEvent? {
        return detectionEventDao.getEventById(eventId)
    }
    
    /**
     * Mark an event as a false positive
     */
    suspend fun markEventAsFalsePositive(eventId: Long) {
        detectionEventDao.markAsFalsePositive(eventId)
    }
    
    /**
     * Get recent events (limit to specified count)
     */
    suspend fun getRecentEvents(limit: Int = 50): List<DetectionEvent> {
        return detectionEventDao.getRecentEvents(limit)
    }
}
