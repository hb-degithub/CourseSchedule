package com.hbde.courseschedule.data.model

/**
 * 成绩数据模型
 */
data class Grade(
    val id: Int = 0,
    val courseName: String,
    val credit: Float,
    val score: Float,
    val semester: String,
    val type: CourseType
)

enum class CourseType {
    REQUIRED,   // 必修
    ELECTIVE    // 选修
}

// GPA 相关类型和工具已迁移到 utils/GpaCalculator.kt
// 保留此处引用以保持兼容性
typealias GpaAlgorithm = com.hbde.courseschedule.utils.GpaAlgorithm
typealias GpaResult = com.hbde.courseschedule.utils.GpaResult

/**
 * GPA 计算器（委托到 utils/GpaCalculator）
 */
object GpaCalculator {

    fun standard40(score: Float): Float = com.hbde.courseschedule.utils.GpaCalculator.standard40(score)

    fun peking40(score: Float): Float = com.hbde.courseschedule.utils.GpaCalculator.peking40(score)

    fun wes(score: Float): Float = com.hbde.courseschedule.utils.GpaCalculator.wes(score)

    fun getGradePoint(score: Float, algorithm: GpaAlgorithm): Float =
        com.hbde.courseschedule.utils.GpaCalculator.getGradePoint(score, algorithm)

    fun calculateGpa(grades: List<Grade>, algorithm: GpaAlgorithm): GpaResult =
        com.hbde.courseschedule.utils.GpaCalculator.calculateGpa(grades, algorithm)

    fun getAlgorithmName(algorithm: GpaAlgorithm): String =
        com.hbde.courseschedule.utils.GpaCalculator.getAlgorithmName(algorithm)
}
