package com.hbde.courseschedule.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val type: String,
    val location: String? = null,
    val startTime: Long,
    val endTime: Long,
    val reminderMinutes: Int = 0,
    val priority: Int = 0,
    val notes: String? = null,
    val isCompleted: Boolean = false
)
