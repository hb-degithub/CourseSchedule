package com.hbde.courseschedule.data.repository

import com.hbde.courseschedule.data.local.dao.ThemeConfigDao
import com.hbde.courseschedule.data.local.entity.ThemeConfigEntity
import com.hbde.courseschedule.ui.theme.ThemePresets
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThemeConfigRepositoryImpl @Inject constructor(
    private val themeConfigDao: ThemeConfigDao
) : ThemeConfigRepository {

    override fun getThemeConfig(): Flow<ThemeConfigEntity> {
        return themeConfigDao.getThemeConfig().map { config ->
            config ?: ThemePresets.DEFAULT
        }
    }

    override suspend fun updateThemeConfig(config: ThemeConfigEntity) {
        val existing = themeConfigDao.getThemeConfigSync()
        if (existing != null) {
            themeConfigDao.update(config.copy(id = existing.id))
        } else {
            themeConfigDao.insert(config.copy(id = 0))
        }
    }

    override fun getPresetThemes(): List<ThemeConfigEntity> {
        return listOf(
            ThemePresets.DEFAULT,
            ThemePresets.DARK_ACADEMIC,
            ThemePresets.FRESH_GREEN,
            ThemePresets.WARM_CAMPUS
        )
    }
}
