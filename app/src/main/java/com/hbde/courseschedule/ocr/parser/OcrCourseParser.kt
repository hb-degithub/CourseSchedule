package com.hbde.courseschedule.ocr.parser

import com.google.mlkit.vision.text.Text
import com.hbde.courseschedule.importer.parser.RawCourse

/**
 * 从 OCR 识别结果中解析出 RawCourse
 */
class OcrCourseParser {

    /**
     * 将 ML Kit 识别到的 Text 解析为课程列表
     */
    fun parse(text: Text): List<RawCourse> {
        // TODO: 实现从 OCR Text 解析 RawCourse 的逻辑
        return emptyList()
    }
}
