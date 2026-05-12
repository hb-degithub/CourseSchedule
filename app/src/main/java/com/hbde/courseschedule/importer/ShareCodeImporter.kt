package com.hbde.courseschedule.importer

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hbde.courseschedule.data.local.entity.CourseEntity
import com.hbde.courseschedule.importer.parser.RawCourse
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import android.util.Base64

/**
 * 分享码导入/导出器
 * 用于生成和解析课程分享码（JSON + Gzip + Base64URL）
 */
class ShareCodeImporter {

    private val gson = Gson()

    /**
     * 将课程列表生成为分享码字符串
     * 序列化流程：List<CourseEntity> -> JSON -> Gzip 压缩 -> Base64URL 编码
     */
    fun generateShareCode(courses: List<CourseEntity>): String {
        if (courses.isEmpty()) return ""

        return try {
            // 转换为 RawCourse 列表进行序列化（避免 Room 相关字段）
            val rawCourses = courses.map { entity ->
                RawCourse(
                    name = entity.name,
                    classroom = entity.classroom ?: "",
                    teacher = entity.teacher ?: "",
                    dayOfWeek = entity.dayOfWeek,
                    startNode = entity.startNode,
                    endNode = entity.endNode,
                    startWeek = entity.startWeek,
                    endWeek = entity.endWeek,
                    weekType = entity.weekType
                )
            }

            val json = gson.toJson(rawCourses)
            val compressed = gzipCompress(json)
            val base64 = Base64.encodeToString(compressed, Base64.URL_SAFE or Base64.NO_WRAP)
            base64
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * 生成短分享码（取前 8 位）
     */
    fun generateShortShareCode(courses: List<CourseEntity>): String {
        val fullCode = generateShareCode(courses)
        return if (fullCode.length >= 8) fullCode.take(8) else fullCode
    }

    /**
     * 解析分享码并还原为课程列表
     * 反序列化流程：Base64URL 解码 -> Gzip 解压 -> JSON -> List<RawCourse>
     */
    fun parseShareCode(code: String): List<RawCourse> {
        if (code.isBlank()) return emptyList()

        return try {
            val decoded = Base64.decode(code, Base64.URL_SAFE or Base64.NO_WRAP)
            val json = gzipDecompress(decoded)
            val type = object : TypeToken<List<RawCourse>>() {}.type
            gson.fromJson<List<RawCourse>>(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 将 RawCourse 列表转换为 CourseEntity 列表
     */
    fun rawCoursesToEntities(rawCourses: List<RawCourse>): List<CourseEntity> {
        return rawCourses.map { raw ->
            CourseEntity(
                name = raw.name,
                classroom = raw.classroom.takeIf { it.isNotBlank() },
                teacher = raw.teacher.takeIf { it.isNotBlank() },
                dayOfWeek = raw.dayOfWeek,
                startNode = raw.startNode,
                endNode = raw.endNode,
                startWeek = raw.startWeek,
                endWeek = raw.endWeek,
                weekType = raw.weekType
            )
        }
    }

    private fun gzipCompress(data: String): ByteArray {
        val outputStream = ByteArrayOutputStream()
        GZIPOutputStream(outputStream).use { gzip ->
            gzip.write(data.toByteArray(Charsets.UTF_8))
        }
        return outputStream.toByteArray()
    }

    private fun gzipDecompress(data: ByteArray): String {
        val inputStream = ByteArrayInputStream(data)
        GZIPInputStream(inputStream).use { gzip ->
            return gzip.readBytes().toString(Charsets.UTF_8)
        }
    }
}
