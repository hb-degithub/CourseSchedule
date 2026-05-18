package com.hbde.courseschedule.data.repository

import com.hbde.courseschedule.data.local.dao.GradeDao
import com.hbde.courseschedule.data.local.entity.GradeEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GradeRepositoryImpl @Inject constructor(
    private val gradeDao: GradeDao
) : GradeRepository {

    override fun getAllGrades(): Flow<List<GradeEntity>> = gradeDao.getAllGrades()

    override fun getGradesBySemester(semester: String): Flow<List<GradeEntity>> =
        gradeDao.getGradesBySemester(semester)

    override fun getAllSemesters(): Flow<List<String>> = gradeDao.getAllSemesters()

    override suspend fun insertGrade(grade: GradeEntity) {
        gradeDao.insert(grade)
    }

    override suspend fun updateGrade(grade: GradeEntity) {
        gradeDao.update(grade)
    }

    override suspend fun deleteGrade(grade: GradeEntity) {
        gradeDao.delete(grade)
    }

    override suspend fun deleteAllGrades() {
        gradeDao.deleteAllGrades()
    }

    override suspend fun deleteGradesBySemester(semester: String) {
        gradeDao.deleteGradesBySemester(semester)
    }
}
