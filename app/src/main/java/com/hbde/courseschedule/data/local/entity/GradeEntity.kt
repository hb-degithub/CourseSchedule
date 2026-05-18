package com.hbde.courseschedule.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 成绩实体
 */
@Entity(tableName = "grades")
data class GradeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val courseName: String,
    val credit: Float,
    val score: Float,
    val semester: String,
    val type: String, // "REQUIRED" or "ELECTIVE"
    val teacher: String? = null,
    val courseCode: String? = null,
    val gpa: Float? = null,
    val createdAt: Long = System.currentTimeMillis()
)
