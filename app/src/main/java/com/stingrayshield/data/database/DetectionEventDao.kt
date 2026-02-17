package com.stingrayshield.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.stingrayshield.domain.model.AnomalyType
import com.stingrayshield.domain.model.DetectionEvent
import com.stingrayshield.domain.model.ThreatLevel
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for the detection events table
 */
@Dao
interface DetectionEventDao {
    
    /**
     * Insert a new detection event
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDetectionEvent(event: DetectionEvent): Long
    
    /**
     * Get all detection events as a Flow
     */
    @Query("SELECT * FROM detection_events ORDER BY timestamp DESC")
    fun getAllEventsAsFlow(): Flow<List<DetectionEvent>>
    
    /**
     * Get events with a minimum threat level
     */
    @Query("SELECT * FROM detection_events WHERE threatLevel IN (:threatLevels) ORDER BY timestamp DESC")
    fun getEventsByThreatLevel(threatLevels: List<String>): Flow<List<DetectionEvent>>
    
    /**
     * Get events of specific types
     */
    @Query("SELECT * FROM detection_events WHERE anomalyType IN (:anomalyTypes) ORDER BY timestamp DESC")
    fun getEventsByType(anomalyTypes: List<String>): Flow<List<DetectionEvent>>
    
    /**
     * Get unarchived events
     */
    @Query("SELECT * FROM detection_events WHERE isArchived = 0 ORDER BY timestamp DESC")
    fun getUnarchivedEvents(): Flow<List<DetectionEvent>>
    
    /**
     * Get events within a time range
     */
    @Query("SELECT * FROM detection_events WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    suspend fun getEventsInTimeRange(startTime: Long, endTime: Long): List<DetectionEvent>
    
    /**
     * Update a detection event (e.g., to archive it)
     */
    @Update
    suspend fun updateEvent(event: DetectionEvent)
    
    /**
     * Mark an event as archived
     */
    @Query("UPDATE detection_events SET isArchived = 1 WHERE id = :eventId")
    suspend fun archiveEvent(eventId: Long)
    
    /**
     * Get count of events by threat level in the last X milliseconds
     */
    @Query("SELECT COUNT(*) FROM detection_events WHERE threatLevel = :threatLevel AND timestamp >= :sinceTime")
    suspend fun getRecentEventCountByThreatLevel(threatLevel: String, sinceTime: Long): Int
    
    /**
     * Delete all events older than a specific time
     */
    @Query("DELETE FROM detection_events WHERE timestamp < :timestamp")
    suspend fun deleteOldEvents(timestamp: Long): Int
    
    /**
     * Delete all events
     */
    @Query("DELETE FROM detection_events")
    suspend fun deleteAllEvents(): Int
    
    /**
     * Get events for a specific cell ID as a Flow
     */
    @Query("SELECT * FROM detection_events WHERE cellId = :cellId ORDER BY timestamp DESC")
    fun getEventsByCellId(cellId: Int): Flow<List<DetectionEvent>>
    
    /**
     * Get a single event by its ID
     */
    @Query("SELECT * FROM detection_events WHERE id = :eventId")
    suspend fun getEventById(eventId: Long): DetectionEvent?
    
    /**
     * Mark an event as a false positive
     */
    @Query("UPDATE detection_events SET isFalsePositive = 1 WHERE id = :eventId")
    suspend fun markAsFalsePositive(eventId: Long)
    
    /**
     * Get recent events (limited by count)
     */
    @Query("SELECT * FROM detection_events ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentEvents(limit: Int): List<DetectionEvent>
}
