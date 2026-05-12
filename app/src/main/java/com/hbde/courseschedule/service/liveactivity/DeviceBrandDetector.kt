package com.hbde.courseschedule.service.liveactivity

import android.os.Build

/**
 * 设备品牌检测工具
 * 用于检测当前设备品牌，以路由到对应的实时活动实现
 */
object DeviceBrandDetector {

    /**
     * 设备品牌枚举
     */
    enum class DeviceBrand {
        XIAOMI,
        OPPO,
        VIVO,
        HONOR,
        HUAWEI,
        OTHER
    }

    /**
     * 检测当前设备品牌
     * 使用 Build.MANUFACTURER 和 Build.BRAND 判断
     */
    fun detectBrand(): DeviceBrand {
        val manufacturer = Build.MANUFACTURER?.lowercase() ?: ""
        val brand = Build.BRAND?.lowercase() ?: ""

        return when {
            isXiaomi(manufacturer, brand) -> DeviceBrand.XIAOMI
            isOppo(manufacturer, brand) -> DeviceBrand.OPPO
            isVivo(manufacturer, brand) -> DeviceBrand.VIVO
            isHonor(manufacturer, brand) -> DeviceBrand.HONOR
            isHuawei(manufacturer, brand) -> DeviceBrand.HUAWEI
            else -> DeviceBrand.OTHER
        }
    }

    private fun isXiaomi(manufacturer: String, brand: String): Boolean {
        return manufacturer.contains("xiaomi") ||
                brand.contains("xiaomi") ||
                manufacturer.contains("redmi") ||
                brand.contains("redmi") ||
                manufacturer.contains("poco") ||
                brand.contains("poco")
    }

    private fun isOppo(manufacturer: String, brand: String): Boolean {
        return manufacturer.contains("oppo") ||
                brand.contains("oppo") ||
                manufacturer.contains("realme") ||
                brand.contains("realme") ||
                manufacturer.contains("oneplus") ||
                brand.contains("oneplus")
    }

    private fun isVivo(manufacturer: String, brand: String): Boolean {
        return manufacturer.contains("vivo") ||
                brand.contains("vivo") ||
                manufacturer.contains("iqoo") ||
                brand.contains("iqoo")
    }

    private fun isHonor(manufacturer: String, brand: String): Boolean {
        return manufacturer.contains("honor") ||
                brand.contains("honor") ||
                manufacturer.contains("荣耀") ||
                brand.contains("荣耀")
    }

    private fun isHuawei(manufacturer: String, brand: String): Boolean {
        return manufacturer.contains("huawei") ||
                brand.contains("huawei") ||
                manufacturer.contains("华为") ||
                brand.contains("华为")
    }
}
