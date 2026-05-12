package com.hbde.courseschedule.data.repository

import com.hbde.courseschedule.data.local.entity.EventEntity
import kotlinx.coroutines.flow.Flow

interface EventRepository {
    fun getAllEvents(): Flow<List<EventEntity>>
    fun getEventById(id: Int): Flow<EventEntity?>
    fun getEventsByType(type: String): Flow<List<EventEntity>>
    fun getEventsByTimeRange(startTime: Long, endTime: Long): Flow<List<EventEntity>>
    suspend fun insertEvent(event: EventEntity)
    suspend fun updateEvent(event: EventEntity)
    suspend fun deleteEvent(event: EventEntity)
}
