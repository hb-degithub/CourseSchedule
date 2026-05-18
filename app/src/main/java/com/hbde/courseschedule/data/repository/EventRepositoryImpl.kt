package com.hbde.courseschedule.data.repository

import com.hbde.courseschedule.data.local.dao.EventDao
import com.hbde.courseschedule.data.local.entity.EventEntity
import com.hbde.courseschedule.data.mapper.toDomainModel
import com.hbde.courseschedule.data.mapper.toEntity
import com.hbde.courseschedule.data.model.Event
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventRepositoryImpl @Inject constructor(
    private val eventDao: EventDao
) : EventRepository {

    override fun getAllEvents(): Flow<List<Event>> = eventDao.getAllEvents().map { list ->
        list.map { it.toDomainModel() }
    }

    override fun getEventById(id: Int): Flow<Event?> = eventDao.getEventById(id).map { it?.toDomainModel() }

    override fun getEventsByType(type: String): Flow<List<Event>> =
        eventDao.getEventsByType(type).map { list -> list.map { it.toDomainModel() } }

    override fun getEventsByTimeRange(startTime: Long, endTime: Long): Flow<List<Event>> =
        eventDao.getEventsByTimeRange(startTime, endTime).map { list -> list.map { it.toDomainModel() } }

    override fun getEventsByCourseId(courseId: Int): Flow<List<Event>> =
        eventDao.getEventsByCourseId(courseId).map { list -> list.map { it.toDomainModel() } }

    override fun getUpcomingEvents(now: Long): Flow<List<Event>> =
        eventDao.getUpcomingEvents(now).map { list -> list.map { it.toDomainModel() } }

    override fun getUpcomingExams(now: Long): Flow<List<Event>> =
        eventDao.getUpcomingExams(now).map { list -> list.map { it.toDomainModel() } }

    override suspend fun insertEvent(event: Event): Long {
        return eventDao.insert(event.toEntity())
    }

    override suspend fun updateEvent(event: Event) {
        eventDao.update(event.toEntity())
    }

    override suspend fun deleteEvent(event: Event) {
        eventDao.delete(event.toEntity())
    }

    override suspend fun toggleEventCompletion(eventId: Int, completed: Boolean) {
        eventDao.updateCompletionStatus(eventId, completed)
    }

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
}
