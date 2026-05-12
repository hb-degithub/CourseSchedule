package com.hbde.courseschedule.data.model

/**
 * 教室模型
 */
data class Classroom(
    val id: String,
    val name: String,
    val building: String,
    val floor: Int,
    val capacity: Int,
    val hasProjector: Boolean = true,
    val hasAirConditioner: Boolean = true
)

/**
 * 教室占用状态
 */
data class ClassroomStatus(
    val classroom: Classroom,
    val isOccupied: Boolean,
    val occupiedNodes: List<Int> = emptyList()
)
