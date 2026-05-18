package com.hbde.courseschedule.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.hbde.courseschedule.data.local.entity.GradeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GradeDao {

    @Insert
    suspend fun insert(grade: GradeEntity): Long

    @Update
    suspend fun update(grade: GradeEntity)

    @Delete
    suspend fun delete(grade: GradeEntity)

    @Query("SELECT * FROM grades ORDER BY semester DESC, courseName ASC")
    fun getAllGrades(): Flow<List<GradeEntity>>

    @Query("SELECT * FROM grades WHERE semester = :semester ORDER BY courseName ASC")
    fun getGradesBySemester(semester: String): Flow<List<GradeEntity>>

    @Query("SELECT DISTINCT semester FROM grades ORDER BY semester DESC")
    fun getAllSemesters(): Flow<List<String>>

    @Query("SELECT * FROM grades WHERE id = :id")
    fun getGradeById(id: Int): Flow<GradeEntity?>

    @Query("DELETE FROM grades")
    suspend fun deleteAllGrades()

    @Query("DELETE FROM grades WHERE semester = :semester")
    suspend fun deleteGradesBySemester(semester: String)
}
