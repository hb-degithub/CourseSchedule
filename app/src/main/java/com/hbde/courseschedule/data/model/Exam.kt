package com.hbde.courseschedule.data.model

import java.time.LocalDateTime

data class Exam(
    val event: Event,
    val examSpecificFields: ExamSpecificFields = ExamSpecificFields()
)

data class ExamSpecificFields(
    val seatNumber: String? = null,
    val examDurationMinutes: Int = 120,
    val invigilator: String? = null,
    val allowedMaterials: List<String> = emptyList()
)
