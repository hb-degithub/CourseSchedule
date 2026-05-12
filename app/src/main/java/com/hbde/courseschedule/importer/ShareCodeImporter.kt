package com.hbde.courseschedule.importer

import com.hbde.courseschedule.importer.parser.RawCourse

/**
 * 分享码导入/导出器
 * 用于生成和解析课程分享码（Base64 编码或压缩）
 */
class ShareCodeImporter {

    /**
     * 将课程列表生成为分享码字符串
     */
    fun generateShareCode(courses: List<RawCourse>): String {
        // TODO: 实现课程列表序列化 + Base64/压缩编码逻辑
        return ""
    }

    /**
     * 解析分享码并还原为课程列表
     */
    fun parseShareCode(code: String): List<RawCourse> {
        // TODO: 实现分享码解码 + 反序列化逻辑
        return emptyList()
    }
}
