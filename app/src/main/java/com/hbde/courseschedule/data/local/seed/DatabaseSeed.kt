package com.hbde.courseschedule.data.local.seed

import com.hbde.courseschedule.data.local.entity.CourseEntity

object DatabaseSeed {

    val sampleCourses: List<CourseEntity> = listOf(
        CourseEntity(
            id = 0,
            name = "高等数学",
            classroom = "A101",
            teacher = "张教授",
            dayOfWeek = 1,      // 周一
            startNode = 1,      // 第 1 节
            endNode = 2,        // 第 2 节
            startWeek = 1,
            endWeek = 16,
            weekType = "all",
            color = -0x1a1a1a,  // 深青色
            notes = "带上教材和作业本"
        ),
        CourseEntity(
            id = 0,
            name = "大学英语",
            classroom = "B203",
            teacher = "李老师",
            dayOfWeek = 3,      // 周三
            startNode = 3,      // 第 3 节
            endNode = 4,        // 第 4 节
            startWeek = 1,
            endWeek = 16,
            weekType = "all",
            color = -0x5b5b5b,  // 深紫色
            notes = "记得带听力耳机"
        ),
        CourseEntity(
            id = 0,
            name = "程序设计",
            classroom = "C305",
            teacher = "王工程师",
            dayOfWeek = 5,      // 周五
            startNode = 5,      // 第 5 节
            endNode = 6,        // 第 6 节
            startWeek = 1,
            endWeek = 16,
            weekType = "all",
            color = -0x8b8b8b,  // 深棕色
            notes = "上机课，带笔记本电脑"
        )
    )
}
