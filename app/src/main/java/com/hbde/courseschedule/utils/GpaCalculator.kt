package com.hbde.courseschedule.utils

import com.hbde.courseschedule.data.model.Grade

/**
 * GPA 算法类型
 */
enum class GpaAlgorithm {
    STANDARD_4_0,   // 标准 4.0
    PEKING_4_0,     // 北大 4.0
    WES             // WES
}

/**
 * GPA 计算结果
 */
data class GpaResult(
    val gpa: Float,
    val totalCredits: Float,
    val totalCourses: Int,
    val weightedSum: Float
)

/**
 * 学期绩点趋势数据
 */
data class SemesterGpa(
    val semester: String,
    val gpa: Float,
    val totalCredits: Float,
    val courseCount: Int
)

/**
 * GPA 计算器
 * 封装多种 GPA 算法
 */
object GpaCalculator {

    /**
     * 标准 4.0 算法
     * 90-100→4.0, 85-89→3.7, 82-84→3.3, 78-81→3.0, 75-77→2.7,
     * 72-74→2.3, 68-71→2.0, 64-67→1.5, 60-63→1.0, <60→0
     */
    fun standard40(score: Float): Float {
        return when {
            score >= 90 -> 4.0f
            score >= 85 -> 3.7f
            score >= 82 -> 3.3f
            score >= 78 -> 3.0f
            score >= 75 -> 2.7f
            score >= 72 -> 2.3f
            score >= 68 -> 2.0f
            score >= 64 -> 1.5f
            score >= 60 -> 1.0f
            else -> 0.0f
        }
    }

    /**
     * 北大 4.0 算法
     * 90-100→4.0, 85-89→3.7, 82-84→3.3, 78-81→3.0, 75-77→2.7,
     * 72-74→2.3, 68-71→2.0, 64-67→1.5, 60-63→1.0, <60→0
     */
    fun peking40(score: Float): Float {
        return standard40(score)
    }

    /**
     * WES 算法
     * 90-100→4.0, 80-89→3.0, 70-79→2.0, 60-69→1.0, <60→0
     */
    fun wes(score: Float): Float {
        return when {
            score >= 90 -> 4.0f
            score >= 80 -> 3.0f
            score >= 70 -> 2.0f
            score >= 60 -> 1.0f
            else -> 0.0f
        }
    }

    /**
     * 根据算法类型获取绩点
     */
    fun getGradePoint(score: Float, algorithm: GpaAlgorithm): Float {
        return when (algorithm) {
            GpaAlgorithm.STANDARD_4_0 -> standard40(score)
            GpaAlgorithm.PEKING_4_0 -> peking40(score)
            GpaAlgorithm.WES -> wes(score)
        }
    }

    /**
     * 计算 GPA
     * GPA = Σ(课程绩点 × 学分) / Σ(学分)
     */
    fun calculateGpa(grades: List<Grade>, algorithm: GpaAlgorithm): GpaResult {
        if (grades.isEmpty()) {
            return GpaResult(0f, 0f, 0, 0f)
        }

        var weightedSum = 0f
        var totalCredits = 0f

        grades.forEach { grade ->
            val gp = getGradePoint(grade.score, algorithm)
            weightedSum += gp * grade.credit
            totalCredits += grade.credit
        }

        val gpa = if (totalCredits > 0) weightedSum / totalCredits else 0f

        return GpaResult(
            gpa = gpa,
            totalCredits = totalCredits,
            totalCourses = grades.size,
            weightedSum = weightedSum
        )
    }

    /**
     * 计算各学期绩点趋势
     */
    fun calculateSemesterGpaTrend(
        grades: List<Grade>,
        algorithm: GpaAlgorithm
    ): List<SemesterGpa> {
        return grades.groupBy { it.semester }
            .map { (semester, semesterGrades) ->
                val result = calculateGpa(semesterGrades, algorithm)
                SemesterGpa(
                    semester = semester,
                    gpa = result.gpa,
                    totalCredits = result.totalCredits,
                    courseCount = result.totalCourses
                )
            }
            .sortedBy { it.semester }
    }

    /**
     * 获取算法中文名称
     */
    fun getAlgorithmName(algorithm: GpaAlgorithm): String {
        return when (algorithm) {
            GpaAlgorithm.STANDARD_4_0 -> "标准 4.0"
            GpaAlgorithm.PEKING_4_0 -> "北大 4.0"
            GpaAlgorithm.WES -> "WES"
        }
    }

    /**
     * 获取算法说明
     */
    fun getAlgorithmDescription(algorithm: GpaAlgorithm): String {
        return when (algorithm) {
            GpaAlgorithm.STANDARD_4_0 ->
                "90-100→4.0, 85-89→3.7, 82-84→3.3, 78-81→3.0, 75-77→2.7, 72-74→2.3, 68-71→2.0, 64-67→1.5, 60-63→1.0"
            GpaAlgorithm.PEKING_4_0 ->
                "与标准 4.0 算法相同，90-100→4.0, 85-89→3.7..."
            GpaAlgorithm.WES ->
                "90-100→4.0, 80-89→3.0, 70-79→2.0, 60-69→1.0, <60→0"
        }
    }
}
