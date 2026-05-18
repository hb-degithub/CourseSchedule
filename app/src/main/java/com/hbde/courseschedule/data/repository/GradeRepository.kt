package com.hbde.courseschedule.data.repository

import com.hbde.courseschedule.data.local.entity.GradeEntity
import kotlinx.coroutines.flow.Flow

interface GradeRepository {
    fun getAllGrades(): Flow<List<GradeEntity>>
    fun getGradesBySemester(semester: String): Flow<List<GradeEntity>>
    fun getAllSemesters(): Flow<List<String>>
    suspend fun insertGrade(grade: GradeEntity)
    suspend fun updateGrade(grade: GradeEntity)
    suspend fun deleteGrade(grade: GradeEntity)
    suspend fun deleteAllGrades()
    suspend fun deleteGradesBySemester(semester: String)
}
