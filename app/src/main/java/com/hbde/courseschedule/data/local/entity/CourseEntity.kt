package com.hbde.courseschedule.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "courses")
data class CourseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val classroom: String? = null,
    val teacher: String? = null,
    val dayOfWeek: Int,
    val startNode: Int,
    val endNode: Int,
    val startWeek: Int,
    val endWeek: Int,
    val weekType: String,
    val color: Int? = null,
    val notes: String? = null
)

/**
 * 判断课程在某周是否可见（周范围 + 单双周判断）
 */
fun CourseEntity.isVisibleInWeek(week: Int): Boolean {
    if (week < startWeek || week > endWeek) return false
    return when (weekType.uppercase()) {
        "ODD" -> week % 2 == 1
        "EVEN" -> week % 2 == 0
        else -> true
    }
}
