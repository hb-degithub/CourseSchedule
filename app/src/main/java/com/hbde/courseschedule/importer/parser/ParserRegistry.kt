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
}
