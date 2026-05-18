package com.hbde.courseschedule.data.repository

import android.content.Context
import com.hbde.courseschedule.data.local.CourseConflictChecker
import com.hbde.courseschedule.data.local.dao.CourseDao
import com.hbde.courseschedule.data.local.entity.CourseEntity
import com.hbde.courseschedule.service.widget.updateAllWidgets
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CourseRepositoryImpl @Inject constructor(
    private val courseDao: CourseDao,
    @ApplicationContext private val context: Context
) : CourseRepository {

    override fun getAllCourses(): Flow<List<CourseEntity>> = courseDao.getAllCourses()

    override fun getCourseById(id: Int): Flow<CourseEntity?> = courseDao.getCourseById(id)

    override fun getCoursesByDayOfWeek(dayOfWeek: Int): Flow<List<CourseEntity>> =
        courseDao.getCoursesByDayOfWeek(dayOfWeek)

    override fun getCoursesByWeek(week: Int): Flow<List<CourseEntity>> =
        courseDao.getCoursesByWeek(week)

    /**
     * 获取指定周、指定星期几的课程，并按单双周过滤
     */
    fun getCoursesByWeek(week: Int, dayOfWeek: Int): Flow<List<CourseEntity>> {
        return courseDao.getCoursesByDayOfWeek(dayOfWeek).map { courses ->
            courses.filter { course ->
                // 周次范围匹配
                week in course.startWeek..course.endWeek &&
                // 单双周过滤
                when (course.weekType) {
                    "odd" -> week % 2 == 1
                    "even" -> week % 2 == 0
                    else -> true // "all" 或其他
                }
            }
        }
    }

    override suspend fun insertCourse(course: CourseEntity) {
        // 冲突检测：查询同一天的所有课程
        val sameDayCourses = courseDao.getCoursesByDayOfWeek(course.dayOfWeek).first()
        val hasConflict = CourseConflictChecker.checkConflict(course, sameDayCourses)
        if (hasConflict) {
            throw IllegalStateException(
                "课程时间冲突：${course.name} 与已有课程在时间段或周次上重叠"
            )
        }
        courseDao.insert(course)
        // Trigger widget update after course insertion
        updateAllWidgets(context)
    }

    override suspend fun updateCourse(course: CourseEntity) {
        // 冲突检测：排除自身后检查
        val sameDayCourses = courseDao.getCoursesByDayOfWeek(course.dayOfWeek)
            .first()
            .filter { it.id != course.id }
        val hasConflict = CourseConflictChecker.checkConflict(course, sameDayCourses)
        if (hasConflict) {
            throw IllegalStateException(
                "课程时间冲突：${course.name} 与已有课程在时间段或周次上重叠"
            )
        }
        courseDao.update(course)
        // Trigger widget update after course update
        updateAllWidgets(context)
    }

    override suspend fun deleteCourse(course: CourseEntity) {
        courseDao.delete(course)
        // Trigger widget update after course deletion
        updateAllWidgets(context)
    }
}
