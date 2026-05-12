package com.hbde.courseschedule.importer.parser

import org.jsoup.Jsoup
import org.jsoup.nodes.Element

/**
 * 青果教务系统解析器
 *
 * 青果教务系统课表通常包含在 id="timetable" 的表格中
 * 表格结构：第一行为星期标题，第一列为节次，单元格内使用特定格式展示课程信息
 */
class QingguoParser : CourseParser {

    override fun parse(html: String): List<RawCourse> {
        val document = Jsoup.parse(html)
        val courses = mutableListOf<RawCourse>()

        // 青果系统常见的课表表格选择器
        val table = document.selectFirst("#timetable, table#timetable, .timetable, table.timetable")
            ?: document.selectFirst("table[border='1']")
            ?: return emptyList()

        val rows = table.select("tr")
        if (rows.isEmpty()) return emptyList()

        // 分析表头，确定星期几对应的列索引
        val headerRow = rows.first()
        val dayColumnMap = parseHeaderRow(headerRow)

        // 从第二行开始解析（跳过表头）
        for (rowIndex in 1 until rows.size) {
            val row = rows[rowIndex]
            val cells = row.select("td, th")
            if (cells.isEmpty()) continue

            // 第一列通常是节次信息
            val nodeInfo = cells.first()?.text()?.trim() ?: ""
            val (startNode, endNode) = parseNodeRange(nodeInfo, rowIndex)

            // 从第二列开始，每列对应一天
            for (cellIndex in 1 until cells.size) {
                val cell = cells[cellIndex]
                val dayOfWeek = dayColumnMap[cellIndex] ?: cellIndex

                val cellCourses = parseCell(cell, dayOfWeek, startNode, endNode)
                courses.addAll(cellCourses)
            }
        }

        return courses
    }

    /**
     * 解析表头行，返回列索引到星期几的映射
     */
    private fun parseHeaderRow(headerRow: Element?): Map<Int, Int> {
        if (headerRow == null) return emptyMap()

        val headers = headerRow.select("td, th")
        val map = mutableMapOf<Int, Int>()

        // 从第二列开始（第一列是节次/时间）
        for (i in 1 until headers.size) {
            val text = headers[i].text().trim()
            val dayOfWeek = when {
                text.contains("周一") || text.contains("星期一") || text.contains("Mon") -> 1
                text.contains("周二") || text.contains("星期二") || text.contains("Tue") -> 2
                text.contains("周三") || text.contains("星期三") || text.contains("Wed") -> 3
                text.contains("周四") || text.contains("星期四") || text.contains("Thu") -> 4
                text.contains("周五") || text.contains("星期五") || text.contains("Fri") -> 5
                text.contains("周六") || text.contains("星期六") || text.contains("Sat") -> 6
                text.contains("周日") || text.contains("星期日") || text.contains("Sun") -> 7
                else -> i // 默认按列索引
            }
            map[i] = dayOfWeek
        }

        return map
    }

    /**
     * 解析单元格内的课程信息
     * 青果系统通常用 <div> 或 <p> 包裹课程信息，也可能直接用 <br> 分隔
     */
    private fun parseCell(cell: Element, dayOfWeek: Int, startNode: Int, endNode: Int): List<RawCourse> {
        // 青果系统常见结构：单元格内有 class="course" 或 class="lesson" 的 div
        val courseDivs = cell.select("div.course, div.lesson, p.course, p.lesson")
        if (courseDivs.isNotEmpty()) {
            return courseDivs.flatMap { div ->
                parseCourseBlock(div.text(), dayOfWeek, startNode, endNode)
            }
        }

        // 也可能是用 <br> 分隔
        val html = cell.html()
        val blocks = html.split(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE))
            .filter { it.isNotBlank() }

        if (blocks.size > 1) {
            return blocks.flatMap { block ->
                val cleanText = Jsoup.parse(block).text()
                parseCourseBlock(cleanText, dayOfWeek, startNode, endNode)
            }
        }

        // 直接解析文本
        return parseCourseBlock(cell.text(), dayOfWeek, startNode, endNode)
    }

    /**
     * 从课程文本块中解析出 RawCourse
     *
     * 青果系统常见的课程文本格式：
     * - 课程名\n教师\n教室\n周次
     * - 课程名(教师)[教室]{周次}
     */
    private fun parseCourseBlock(text: String, dayOfWeek: Int, startNode: Int, endNode: Int): List<RawCourse> {
        val cleanText = text.trim()
        if (cleanText.isBlank() || cleanText == "&nbsp;" || cleanText == " ") {
            return emptyList()
        }

        // 尝试提取周次信息
        val weekInfo = extractWeekInfo(cleanText)
        val textWithoutWeek = weekInfo?.second ?: cleanText

        // 按行分割
        val lines = textWithoutWeek.split("\n", "\r\n").map { it.trim() }.filter { it.isNotBlank() }

        var name = ""
        var teacher = ""
        var classroom = ""

        if (lines.isNotEmpty()) {
            name = lines[0]
        }

        // 尝试从剩余文本中提取教师和教室
        for (i in 1 until lines.size) {
            val line = lines[i]
            when {
                line.contains("教室") || line.contains("楼") || line.contains("馆") ||
                    line.matches(Regex(".*[A-Za-z]\\d+.*")) ||
                    line.matches(Regex("^\\d+[#-]\\d+")) -> {
                    classroom = line
                }
                line.contains("老师") || line.contains("教授") || line.contains("讲师") ||
                    line.contains("教师") ||
                    (line.length in 2..5 && !line.matches(Regex(".*\\d+.*"))) -> {
                    if (teacher.isBlank()) teacher = line
                }
            }
        }

        if (name.isBlank()) return emptyList()

        return if (weekInfo != null) {
            val (startWeek, endWeek, weekType) = weekInfo.first
            listOf(
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
        } else {
            listOf(
                RawCourse(
                    name = name,
                    classroom = classroom,
                    teacher = teacher,
                    dayOfWeek = dayOfWeek,
                    startNode = startNode,
                    endNode = endNode,
                    startWeek = 1,
                    endWeek = 20,
                    weekType = "all"
                )
            )
        }
    }

    /**
     * 解析节次范围
     */
    private fun parseNodeRange(nodeText: String, rowIndex: Int): Pair<Int, Int> {
        // 尝试从文本解析，如 "第1-2节"、"1-2"、"1、2节"
        val match = Regex("(\\d+)[-~—](\\d+)").find(nodeText)
        if (match != null) {
            val start = match.groupValues[1].toIntOrNull() ?: rowIndex
            val end = match.groupValues[2].toIntOrNull() ?: start
            return Pair(start, end)
        }

        val singleMatch = Regex("(\\d+)").find(nodeText)
        if (singleMatch != null) {
            val node = singleMatch.groupValues[1].toIntOrNull() ?: rowIndex
            return Pair(node, node + 1)
        }

        // 默认：第 n 行对应第 n 节课（每节课 2 节）
        return Pair(rowIndex * 2 - 1, rowIndex * 2)
    }

    /**
     * 从文本中提取周次信息
     * 返回：Triple<startWeek, endWeek, weekType> 和去除周次后的文本
     */
    private fun extractWeekInfo(text: String): Pair<Triple<Int, Int, String>, String>? {
        // 匹配模式：[1-16周]、(1-16周)、{1-16周}、1-16周、第1-16周
        val patterns = listOf(
            Regex("([({\\[])(\\d+)[-~—](\\d+)周(.*?)([)}\\]])"),
            Regex("(\\d+)[-~—](\\d+)周"),
            Regex("(\\d+)[-~—](\\d+)\\s*周"),
            Regex("第(\\d+)[-~—](\\d+)周")
        )

        for (pattern in patterns) {
            val match = pattern.find(text) ?: continue
            val groups = match.groupValues

            val startWeek = groups[groups.size - 3].toIntOrNull() ?: 1
            val endWeek = groups[groups.size - 2].toIntOrNull() ?: 20

            // 判断单双周
            val weekType = when {
                text.contains("单") && !text.contains("双") -> "odd"
                text.contains("双") && !text.contains("单") -> "even"
                else -> "all"
            }

            val remainingText = text.replace(match.value, "").trim()
            return Pair(Triple(startWeek, endWeek, weekType), remainingText)
        }

        return null
    }
}
