package com.hbde.courseschedule.importer

import com.hbde.courseschedule.importer.parser.RawCourse

data class ImportResult(
    val success: Boolean,
    val courses: List<RawCourse>,
    val errors: List<String>,
    val totalCount: Int
)
