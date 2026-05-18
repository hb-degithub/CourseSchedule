package com.hbde.courseschedule.importer.parser

/**
 * 解析器注册表
 * 插件化架构：新增学校只需实现 CourseParser 并在此注册
 */
class ParserRegistry {

    private val parsers = mutableMapOf<String, CourseParser>()

    init {
        register("qiangzhi", QiangzhiParser())
        register("zhengfang", ZhengfangParser())
        register("qingguo", QingguoParser())
    }

    fun register(name: String, parser: CourseParser) {
        parsers[name] = parser
    }

    fun getParser(name: String): CourseParser? {
        return parsers[name]
    }

    fun getAvailableParsers(): List<String> {
        return parsers.keys.toList()
    }

    /**
     * 自动检测 HTML 适用的解析器
     */
    fun detectParser(html: String): CourseParser? {
        return parsers.values.firstOrNull { it.canParse(html) }
    }

    /**
     * 获取所有解析器信息
     */
    fun getAllParsers(): List<CourseParser> {
        return parsers.values.toList()
    }
}
