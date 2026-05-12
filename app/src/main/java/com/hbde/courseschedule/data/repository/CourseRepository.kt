package com.hbde.courseschedule.data.repository

import com.hbde.courseschedule.data.local.entity.CourseEntity
import kotlinx.coroutines.flow.Flow

interface CourseRepository {
    fun getAllCourses(): Flow<List<CourseEntity>>
    fun getCourseById(id: Int): Flow<CourseEntity?>
    fun getCoursesByDayOfWeek(dayOfWeek: Int): Flow<List<CourseEntity>>
    fun getCoursesByWeek(week: Int): Flow<List<CourseEntity>>
    suspend fun insertCourse(course: CourseEntity)
    suspend fun updateCourse(course: CourseEntity)
    suspend fun deleteCourse(course: CourseEntity)
}
