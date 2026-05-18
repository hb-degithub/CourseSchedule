package com.hbde.courseschedule.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.hbde.courseschedule.data.local.entity.EventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {

    @Insert
    suspend fun insert(event: EventEntity): Long

    @Update
    suspend fun update(event: EventEntity)

    @Delete
    suspend fun delete(event: EventEntity)

    @Query("SELECT * FROM events")
    fun getAllEvents(): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE type = :type")
    fun getEventsByType(type: String): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE startTime >= :startTime AND endTime <= :endTime")
    fun getEventsByTimeRange(startTime: Long, endTime: Long): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE id = :id")
    fun getEventById(id: Int): Flow<EventEntity?>

    @Query("SELECT * FROM events WHERE courseId = :courseId")
    fun getEventsByCourseId(courseId: Int): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE isCompleted = 0 AND startTime >= :now ORDER BY startTime ASC")
    fun getUpcomingEvents(now: Long): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE type = 'EXAM' AND isCompleted = 0 AND startTime >= :now ORDER BY startTime ASC")
    fun getUpcomingExams(now: Long): Flow<List<EventEntity>>

    @Query("UPDATE events SET isCompleted = :completed WHERE id = :eventId")
    suspend fun updateCompletionStatus(eventId: Int, completed: Boolean)
}
