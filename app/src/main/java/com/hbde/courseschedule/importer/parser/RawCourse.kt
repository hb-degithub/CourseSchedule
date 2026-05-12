package com.hbde.courseschedule.importer.parser

data class RawCourse(
    val name: String,
    val classroom: String,
    val teacher: String,
    val dayOfWeek: Int,
    val startNode: Int,
    val endNode: Int,
    val startWeek: Int,
    val endWeek: Int,
    val weekType: String
)
