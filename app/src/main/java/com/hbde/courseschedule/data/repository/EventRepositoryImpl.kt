package com.hbde.courseschedule.data.repository

import com.hbde.courseschedule.data.local.dao.EventDao
import com.hbde.courseschedule.data.local.entity.EventEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventRepositoryImpl @Inject constructor(
    private val eventDao: EventDao
) : EventRepository {

    override fun getAllEvents(): Flow<List<EventEntity>> = eventDao.getAllEvents()

    override fun getEventById(id: Int): Flow<EventEntity?> = eventDao.getEventById(id)

    override fun getEventsByType(type: String): Flow<List<EventEntity>> =
        eventDao.getEventsByType(type)

    override fun getEventsByTimeRange(startTime: Long, endTime: Long): Flow<List<EventEntity>> =
        eventDao.getEventsByTimeRange(startTime, endTime)

    /**
     * 获取指定时间范围内的所有事件（startTime 在 [start, end] 之间，或 endTime 在 [start, end] 之间）
     */
    fun getEventsBetween(start: Long, end: Long): Flow<List<EventEntity>> {
        return eventDao.getAllEvents().map { events ->
            events.filter { event ->
                // 事件与查询区间有重叠
                event.startTime in start..end ||
                event.endTime in start..end ||
                (event.startTime <= start && event.endTime >= end)
            }
        }
    }

    override suspend fun insertEvent(event: EventEntity) {
        eventDao.insert(event)
    }

    override suspend fun updateEvent(event: EventEntity) {
        eventDao.update(event)
    }

    override suspend fun deleteEvent(event: EventEntity) {
        eventDao.delete(event)
    }
}
