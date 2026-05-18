package com.hbde.courseschedule.data.mapper

import com.hbde.courseschedule.data.local.entity.EventEntity
import com.hbde.courseschedule.data.model.Event
import com.hbde.courseschedule.data.model.EventPriority
import com.hbde.courseschedule.data.model.EventType
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

fun EventEntity.toDomainModel(): Event {
    return Event(
        id = id,
        title = title,
        type = EventType.valueOf(type),
        location = location,
        startTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(startTime), ZoneId.systemDefault()),
        endTime = if (endTime > 0) LocalDateTime.ofInstant(Instant.ofEpochMilli(endTime), ZoneId.systemDefault()) else null,
        reminderMinutes = reminderMinutes,
        priority = EventPriority.entries.getOrElse(priority) { EventPriority.MEDIUM },
        notes = notes,
        isCompleted = isCompleted,
        courseId = courseId
    )
}

fun Event.toEntity(): EventEntity {
    return EventEntity(
        id = id,
        title = title,
        type = type.name,
        location = location,
        startTime = startTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        endTime = endTime?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli() ?: 0L,
        reminderMinutes = reminderMinutes,
        priority = priority.ordinal,
        notes = notes,
        isCompleted = isCompleted,
        courseId = courseId
    )
}
