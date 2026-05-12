package com.hbde.courseschedule.data.model

import java.time.LocalDateTime

enum class EventType {
    EXAM, HOMEWORK, CUSTOM
}

enum class EventPriority {
    LOW, MEDIUM, HIGH
}

data class Event(
    val id: Int = 0,
    val title: String,
    val type: EventType,
    val location: String? = null,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime? = null,
    val reminderMinutes: Int = 0,
    val priority: EventPriority = EventPriority.MEDIUM,
    val notes: String? = null,
    val isCompleted: Boolean = false
)
