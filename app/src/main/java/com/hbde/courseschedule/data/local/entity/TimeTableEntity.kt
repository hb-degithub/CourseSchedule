package com.hbde.courseschedule.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "time_tables")
data class TimeTableEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val timeSlots: String
)

/**
 * 时间段数据类，用于解析 TimeTableEntity.timeSlots
 */
data class TimeSlot(
    val startTime: String,
    val endTime: String
)

/**
 * 将 TimeSlot 列表序列化为 JSON 字符串
 */
fun List<TimeSlot>.toJson(): String {
    return com.google.gson.Gson().toJson(this.map { listOf(it.startTime, it.endTime) })
}

/**
 * 从 JSON 字符串解析 TimeSlot 列表
 */
fun String.toTimeSlotList(): List<TimeSlot> {
    val type = object : com.google.gson.reflect.TypeToken<List<List<String>>>() {}.type
    val list: List<List<String>> = com.google.gson.Gson().fromJson(this, type) ?: emptyList()
    return list.map { TimeSlot(it[0], it[1]) }
}
