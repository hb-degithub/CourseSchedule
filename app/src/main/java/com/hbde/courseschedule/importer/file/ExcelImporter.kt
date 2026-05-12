package com.hbde.courseschedule.importer.file

import android.content.Context
import android.net.Uri
import com.hbde.courseschedule.importer.ImportResult
import com.hbde.courseschedule.importer.parser.RawCourse
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.InputStream

/**
 * Excel 课表文件导入器（Apache POI）
 */
class ExcelImporter(private val context: Context) {

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
     * 解析 Excel 文件并返回导入结果
     */
    fun parse(fileUri: Uri): ImportResult {
        return try {
            context.contentResolver.openInputStream(fileUri)?.use { stream ->
                val workbook = WorkbookFactory.create(stream)
                val sheet = workbook.getSheetAt(0)
                if (sheet.physicalNumberOfRows == 0) {
                    return ImportResult(
                        success = false,
                        courses = emptyList(),
                        errors = listOf("Excel 文件为空"),
                        totalCount = 0
                    )
                }

                // 读取表头（第一行）
                val headerRow = sheet.getRow(0) ?: return ImportResult(
                    success = false,
                    courses = emptyList(),
                    errors = listOf("Excel 文件缺少表头"),
                    totalCount = 0
                )

                val columnMap = mutableMapOf<String, Int>()
                for (cell in headerRow) {
                    val rawHeader = getCellStringValue(cell).trim().lowercase()
                    val mapped = HEADER_MAPPINGS[rawHeader]
                    if (mapped != null && !columnMap.containsKey(mapped)) {
                        columnMap[mapped] = cell.columnIndex
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

                for (rowIndex in 1 until sheet.physicalNumberOfRows) {
                    val row = sheet.getRow(rowIndex) ?: continue
                    try {
                        val name = getCellStringValue(row.getCell(columnMap["name"] ?: -1)).trim()
                        if (name.isBlank()) continue

                        val dayOfWeek = parseInt(
                            getCellStringValue(row.getCell(columnMap["dayOfWeek"] ?: -1)),
                            1
                        ).coerceIn(1, 7)

                        val startNode = parseInt(
                            getCellStringValue(row.getCell(columnMap["startNode"] ?: -1)),
                            1
                        ).coerceIn(1, 12)

                        val endNodeStr = getCellStringValue(row.getCell(columnMap["endNode"] ?: -1))
                        val endNode = if (endNodeStr.isNotBlank()) {
                            parseInt(endNodeStr, startNode).coerceIn(startNode, 12)
                        } else {
                            startNode
                        }

                        val startWeek = parseInt(
                            getCellStringValue(row.getCell(columnMap["startWeek"] ?: -1)),
                            1
                        ).coerceIn(1, 25)

                        val endWeekStr = getCellStringValue(row.getCell(columnMap["endWeek"] ?: -1))
                        val endWeek = if (endWeekStr.isNotBlank()) {
                            parseInt(endWeekStr, startWeek).coerceIn(startWeek, 25)
                        } else {
                            startWeek
                        }

                        val weekType = parseWeekType(
                            getCellStringValue(row.getCell(columnMap["weekType"] ?: -1))
                        )

                        val classroom = getCellStringValue(row.getCell(columnMap["classroom"] ?: -1)).trim()
                        val teacher = getCellStringValue(row.getCell(columnMap["teacher"] ?: -1)).trim()

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

                workbook.close()

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
                errors = listOf("Excel 解析失败: ${e.message}"),
                totalCount = 0
            )
        }
    }

    private fun getCellStringValue(cell: Cell?): String {
        if (cell == null) return ""
        return when (cell.cellType) {
            CellType.STRING -> cell.stringCellValue ?: ""
            CellType.NUMERIC -> {
                val num = cell.numericCellValue
                if (num == num.toInt().toDouble()) {
                    num.toInt().toString()
                } else {
                    num.toString()
                }
            }
            CellType.BOOLEAN -> cell.booleanCellValue.toString()
            CellType.FORMULA -> {
                try {
                    cell.stringCellValue ?: ""
                } catch (e: Exception) {
                    try {
                        cell.numericCellValue.toString()
                    } catch (e2: Exception) {
                        ""
                    }
                }
            }
            else -> ""
        }
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
