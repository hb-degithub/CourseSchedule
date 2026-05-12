package com.hbde.courseschedule.importer.file

import android.net.Uri
import com.hbde.courseschedule.importer.ImportResult

/**
 * CSV 课表文件导入器（OpenCSV）
 */
class CsvImporter {

    /**
     * 解析 CSV 文件并返回导入结果
     */
    fun parse(fileUri: Uri): ImportResult {
        // TODO: 实现 OpenCSV 解析 CSV 逻辑
        return ImportResult(
            success = false,
            courses = emptyList(),
            errors = listOf("Not implemented"),
            totalCount = 0
        )
    }
}
