package com.hbde.courseschedule.importer.file

import android.content.Context
import android.net.Uri
import com.hbde.courseschedule.importer.ImportResult
import com.hbde.courseschedule.importer.parser.RawCourse
import com.opencsv.CSVReader
import java.io.InputStreamReader

/**
 * CSV 课表文件导入器（OpenCSV）
 */
class CsvImporter(private val context: Context) {

    companion object {
        private val HEADER_MAPPINGS = mapOf(
            // 课程名
            "name" to "name",
            "课程名" to "name",
            "课程名称" to "name",
            "coursename" to "name",
            // 教室
            "classroom" to "classroom",
            "教室" to "classroom",
            "上课地点" to "classroom",
            "location" to "classroom",
            // 教师
            "teacher" to "teacher",
            "教师" to "teacher",
            "任课教师" to "teacher",
            "instructor" to "teacher",
            // 星期
            "day" to "dayOfWeek",
            "星期" to "dayOfWeek",
            "星期几" to "dayOfWeek",
            "dayofweek" to "dayOfWeek",
            // 开始节
            "node" to "startNode",
            "节次" to "startNode",
            "开始节" to "startNode",
            "startnode" to "startNode",
            // 结束节
            "endnode" to "endNode",
            "结束节" to "endNode",
            // 开始周
            "startweek" to "startWeek",
            "开始周" to "startWeek",
            "起始周" to "startWeek",
            // 结束周
            "endweek" to "endWeek",
            "结束周" to "endWeek",
            // 周类型
            "weektype" to "weekType",
            "周类型" to "weekType",
            "单双周" to "weekType"
        )
    }

    /**
     * 解析 CSV 文件并返回导入结果
     */
    fun parse(fileUri: Uri): ImportResult {
        return try {
            context.contentResolver.openInputStream(fileUri)?.use { stream ->
                // 处理 UTF-8 BOM
                val bomHandledStream = if (stream.available() >= 3) {
                    val bom = ByteArray(3)
                    stream.mark(3)
                    stream.read(bom)
                    if (bom[0] == 0xEF.toByte() && bom[1] == 0xBB.toByte() && bom[2] == 0xBF.toByte()) {
                        stream
                    } else {
                        stream.reset()
                        stream
                    }
                } else {
                    stream
                }

                val reader = CSVReader(InputStreamReader(bomHandledStream, Charsets.UTF_8))
                val allRows = reader.readAll()
                reader.close()

                if (allRows.isEmpty()) {
                    return ImportResult(
                        success = false,
                        courses = emptyList(),
                        errors = listOf("CSV 文件为空"),
                        totalCount = 0
                    )
                }

                // 读取表头
                val headerRow = allRows[0]
                val columnMap = mutableMapOf<String, Int>()
                headerRow.forEachIndexed { index, header ->
                    val rawHeader = header.trim().lowercase()
                    val mapped = HEADER_MAPPINGS[rawHeader]
                    if (mapped != null && !columnMap.containsKey(mapped)) {
                        columnMap[mapped] = index
                    }
                }

                if (columnMap["name"] == null) {
                    return ImportResult(
                        success = false,
                        courses = emptyList(),
                        errors = listOf("未找到课程名列，请检查表头"),
                        totalCount = 0
                    )
                }

                val courses = mutableListOf<RawCourse>()
                val errors = mutableListOf<String>()

                for (rowIndex in 1 until allRows.size) {
                    val row = allRows[rowIndex]
                    try {
                        val nameCol = columnMap["name"] ?: -1
                        val name = if (nameCol in row.indices) row[nameCol].trim() else ""
                        if (name.isBlank()) continue

                        val dayOfWeek = parseInt(
                            getValue(row, columnMap["dayOfWeek"]),
                            1
                        ).coerceIn(1, 7)

                        val startNode = parseInt(
                            getValue(row, columnMap["startNode"]),
                            1
                        ).coerceIn(1, 12)

                        val endNodeStr = getValue(row, columnMap["endNode"])
                        val endNode = if (endNodeStr.isNotBlank()) {
                            parseInt(endNodeStr, startNode).coerceIn(startNode, 12)
                        } else {
                            startNode
                        }

                        val startWeek = parseInt(
                            getValue(row, columnMap["startWeek"]),
                            1
                        ).coerceIn(1, 25)

                        val endWeekStr = getValue(row, columnMap["endWeek"])
                        val endWeek = if (endWeekStr.isNotBlank()) {
                            parseInt(endWeekStr, startWeek).coerceIn(startWeek, 25)
                        } else {
                            startWeek
                        }

                        val weekType = parseWeekType(
                            getValue(row, columnMap["weekType"])
                        )

                        val classroom = getValue(row, columnMap["classroom"]).trim()
                        val teacher = getValue(row, columnMap["teacher"]).trim()

                        courses.add(
                            RawCourse(
                                name = name,
                                classroom = classroom,
                                teacher = teacher,
                                dayOfWeek = dayOfWeek,
                                startNode = startNode,
                                endNode = endNode,
                                startWeek = startWeek,
                                endWeek = endWeek,
                                weekType = weekType
                            )
                        )
                    } catch (e: Exception) {
                        errors.add("第 ${rowIndex + 1} 行解析失败: ${e.message}")
                    }
                }

                ImportResult(
                    success = courses.isNotEmpty(),
                    courses = courses,
                    errors = errors,
                    totalCount = courses.size
                )
            } ?: ImportResult(
                success = false,
                courses = emptyList(),
                errors = listOf("无法打开文件"),
                totalCount = 0
            )
        } catch (e: Exception) {
            ImportResult(
                success = false,
                courses = emptyList(),
                errors = listOf("CSV 解析失败: ${e.message}"),
                totalCount = 0
            )
        }
    }

    private fun getValue(row: Array<String>, columnIndex: Int?): String {
        if (columnIndex == null || columnIndex !in row.indices) return ""
        return row[columnIndex]
    }

    private fun parseInt(value: String, default: Int): Int {
        return value.trim().toIntOrNull() ?: default
    }

    private fun parseWeekType(value: String): String {
        return when (value.trim().lowercase()) {
            "odd", "单", "单周" -> "odd"
            "even", "双", "双周" -> "even"
            else -> "all"
        }
    }
}
