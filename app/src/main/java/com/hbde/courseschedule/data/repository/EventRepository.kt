package com.hbde.courseschedule.data.repository

import com.hbde.courseschedule.data.local.entity.EventEntity
import com.hbde.courseschedule.data.model.Event
import kotlinx.coroutines.flow.Flow

interface EventRepository {
    fun getAllEvents(): Flow<List<Event>>
    fun getEventById(id: Int): Flow<Event?>
    fun getEventsByType(type: String): Flow<List<Event>>
    fun getEventsByTimeRange(startTime: Long, endTime: Long): Flow<List<Event>>
    fun getEventsByCourseId(courseId: Int): Flow<List<Event>>
    fun getUpcomingEvents(now: Long): Flow<List<Event>>
    fun getUpcomingExams(now: Long): Flow<List<Event>>
    suspend fun insertEvent(event: Event): Long
    suspend fun updateEvent(event: Event)
    suspend fun deleteEvent(event: Event)
    suspend fun toggleEventCompletion(eventId: Int, completed: Boolean)
}
