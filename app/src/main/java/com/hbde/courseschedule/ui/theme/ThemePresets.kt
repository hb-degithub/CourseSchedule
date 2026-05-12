package com.hbde.courseschedule.ui.theme

import com.hbde.courseschedule.data.local.entity.ThemeConfigEntity

/**
 * 内置主题预设
 *
 * 每套主题包含：
 * - primaryColor: 主题主色 (Int Color)
 * - backgroundImage: 背景值（纯色时为色值字符串，图片时为图片 URI 字符串）
 * - opacity: 背景透明度 0.0-1.0
 * - cornerRadius: 课程卡片圆角半径 dp
 * - fontSize: 字体大小基准 sp（通过 scale 计算实际大小）
 */
object ThemePresets {

    /**
     * 默认 Material 3 蓝色系
     */
    val DEFAULT = ThemeConfigEntity(
        id = 0,
        primaryColor = 0xFF2196F3.toInt(),
        backgroundImage = "#FFF5F5F5",
        opacity = 1.0f,
        cornerRadius = 4,
        fontSize = 14
    )

    /**
     * 深色学术风：深蓝 + 金色强调
     */
    val DARK_ACADEMIC = ThemeConfigEntity(
        id = 0,
        primaryColor = 0xFF1A237E.toInt(),
        backgroundImage = "#FF0D1B2A",
        opacity = 1.0f,
        cornerRadius = 6,
        fontSize = 14
    )

    /**
     * 清新绿白配色
     */
    val FRESH_GREEN = ThemeConfigEntity(
        id = 0,
        primaryColor = 0xFF43A047.toInt(),
        backgroundImage = "#FFF1F8E9",
        opacity = 1.0f,
        cornerRadius = 8,
        fontSize = 14
    )

    /**
     * 暖橙校园风
     */
    val WARM_CAMPUS = ThemeConfigEntity(
        id = 0,
        primaryColor = 0xFFFF6F00.toInt(),
        backgroundImage = "#FFFFF3E0",
        opacity = 1.0f,
        cornerRadius = 10,
        fontSize = 14
    )
}
