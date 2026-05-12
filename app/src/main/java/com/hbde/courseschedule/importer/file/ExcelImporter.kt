package com.hbde.courseschedule.importer.file

import android.net.Uri
import com.hbde.courseschedule.importer.ImportResult

/**
 * Excel 课表文件导入器（Apache POI）
 */
class ExcelImporter {

    /**
     * 解析 Excel 文件并返回导入结果
     */
    fun parse(fileUri: Uri): ImportResult {
        // TODO: 实现 Apache POI 解析 Excel 逻辑
        return ImportResult(
            success = false,
            courses = emptyList(),
            errors = listOf("Not implemented"),
            totalCount = 0
        )
    }
}
