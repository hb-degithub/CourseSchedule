package com.hbde.courseschedule.importer.parser

import com.hbde.courseschedule.importer.ImportResult

/**
 * HTML 源码粘贴导入器
 * 用户粘贴课表页面源码，离线解析
 */
class HtmlPasteImporter(
    private val parserRegistry: ParserRegistry
) {

    /**
     * 自动检测解析器并解析 HTML
     * @param html 用户粘贴的 HTML 源码
     * @return 导入结果
     */
    fun parse(html: String): ImportResult {
        if (html.isBlank()) {
            return ImportResult(
                success = false,
                courses = emptyList(),
                errors = listOf("HTML 内容为空"),
                totalCount = 0
            )
        }

        // 尝试自动检测解析器
        val detectedParser = parserRegistry.detectParser(html)

        return if (detectedParser != null) {
            parseWithParser(detectedParser, html)
        } else {
            // 如果自动检测失败，尝试所有解析器
            val allResults = parserRegistry.getAllParsers().map { parser ->
                parseWithParser(parser, html)
            }

            // 返回解析结果最多的那个
            val bestResult = allResults.maxByOrNull { it.courses.size }
                ?: return ImportResult(
                    success = false,
                    courses = emptyList(),
                    errors = listOf("无法识别课表格式，请确认粘贴的是课表页面完整源码"),
                    totalCount = 0
                )

            if (bestResult.courses.isEmpty()) {
                ImportResult(
                    success = false,
                    courses = emptyList(),
                    errors = listOf(
                        "无法解析课表内容。支持的系统：正方、强智、青果教务。" +
                        "请粘贴课表页面的完整 HTML 源码（右键页面 -> 查看网页源代码 -> 全选复制）"
                    ),
                    totalCount = 0
                )
            } else {
                bestResult
            }
        }
    }

    /**
     * 使用指定解析器解析 HTML
     */
    fun parseWithParser(parserName: String, html: String): ImportResult {
        val parser = parserRegistry.getParser(parserName)
            ?: return ImportResult(
                success = false,
                courses = emptyList(),
                errors = listOf("未知的解析器: $parserName"),
                totalCount = 0
            )
        return parseWithParser(parser, html)
    }

    private fun parseWithParser(parser: CourseParser, html: String): ImportResult {
        return try {
            val courses = parser.parse(html)
            val errors = mutableListOf<String>()

            if (courses.isEmpty()) {
                errors.add("解析器 ${parser.parserName} 未提取到任何课程信息")
            }

            ImportResult(
                success = courses.isNotEmpty(),
                courses = courses,
                errors = errors,
                totalCount = courses.size
            )
        } catch (e: Exception) {
            ImportResult(
                success = false,
                courses = emptyList(),
                errors = listOf("解析失败: ${e.message}"),
                totalCount = 0
            )
        }
    }

    /**
     * 获取所有可用的解析器信息
     */
    fun getAvailableParsers(): List<Pair<String, String>> {
        return parserRegistry.getAllParsers().map {
            it.systemType to it.parserName
        }
    }
}
