package com.hbde.courseschedule.data.repository

import com.hbde.courseschedule.data.local.entity.ThemeConfigEntity
import kotlinx.coroutines.flow.Flow

interface ThemeConfigRepository {
    fun getThemeConfig(): Flow<ThemeConfigEntity>
    suspend fun updateThemeConfig(config: ThemeConfigEntity)
    fun getPresetThemes(): List<ThemeConfigEntity>
}
