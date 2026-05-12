package com.hbde.courseschedule.importer.parser

interface CourseParser {
    fun parse(html: String): List<RawCourse>
}
