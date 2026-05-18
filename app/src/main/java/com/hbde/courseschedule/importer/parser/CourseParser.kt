package com.hbde.courseschedule.importer.parser

/**
 * 课程解析器接口
 * 支持教务系统 HTML 源码解析和 HTML 粘贴模式
 */
interface CourseParser {

    /**
     * 解析 HTML 源码，提取课程列表
     * @param html 课表页面的 HTML 源码
     * @return 解析出的课程列表
     */
    fun parse(html: String): List<RawCourse>

    /**
     * 解析器名称，用于展示
     */
    val parserName: String

    /**
     * 支持的教务系统标识
     */
    val systemType: String

    /**
     * 检测 HTML 是否匹配该解析器
     * @param html HTML 源码
     * @return 是否匹配
     */
    fun canParse(html: String): Boolean
}
