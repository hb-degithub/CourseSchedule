package com.hbde.courseschedule.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.hbde.courseschedule.data.local.entity.CourseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseDao {

    @Insert
    suspend fun insert(course: CourseEntity): Long

    @Update
    suspend fun update(course: CourseEntity)

    @Delete
    suspend fun delete(course: CourseEntity)

    @Query("SELECT * FROM courses")
    fun getAllCourses(): Flow<List<CourseEntity>>

    @Query("SELECT * FROM courses WHERE dayOfWeek = :dayOfWeek")
    fun getCoursesByDayOfWeek(dayOfWeek: Int): Flow<List<CourseEntity>>

    @Query("SELECT * FROM courses WHERE startWeek <= :week AND endWeek >= :week")
    fun getCoursesByWeek(week: Int): Flow<List<CourseEntity>>

    @Query("SELECT * FROM courses WHERE id = :id")
    fun getCourseById(id: Int): Flow<CourseEntity?>
}
