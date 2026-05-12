package com.hbde.courseschedule.data.model

import java.time.LocalDateTime

data class Task(
    val event: Event,
    val taskSpecificFields: TaskSpecificFields = TaskSpecificFields()
)

data class TaskSpecificFields(
    val subject: String? = null,
    val progressPercentage: Int = 0,
    val attachmentUrls: List<String> = emptyList()
)
