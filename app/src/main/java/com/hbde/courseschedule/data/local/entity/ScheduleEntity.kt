package com.hbde.courseschedule.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedules")
data class ScheduleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val startDate: Long,
    val currentScheduleId: Int,
    val currentTimeTableId: Int
)
