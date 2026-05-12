package com.hbde.courseschedule.service

import android.content.Context
import android.media.AudioManager
import android.os.Build
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.hbde.courseschedule.data.local.SettingsDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "silent_mode_prefs")

@Singleton
class SilentModeManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsDataStore: SettingsDataStore,
) {

    companion object {
        private val KEY_ORIGINAL_RINGER_MODE = intPreferencesKey("original_ringer_mode")
    }

    private val audioManager: AudioManager by lazy {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    /**
     * 进入静音/振动模式
     * @param mode AudioManager.RINGER_MODE_SILENT 或 RINGER_MODE_VIBRATE
     */
    suspend fun enterSilentMode(mode: Int) {
        val currentMode = audioManager.ringerMode

        // 如果当前已经是目标模式，不需要重复操作
        if (currentMode == mode) {
            return
        }

        // 保存原始铃声模式
        context.dataStore.edit { preferences ->
            preferences[KEY_ORIGINAL_RINGER_MODE] = currentMode
        }

        // 切换为目标模式
        audioManager.ringerMode = mode
    }

    /**
     * 恢复之前的铃声模式
     */
    suspend fun restoreRingerMode() {
        val originalMode = context.dataStore.data
            .map { preferences ->
                preferences[KEY_ORIGINAL_RINGER_MODE]
            }
            .first()

        if (originalMode != null) {
            audioManager.ringerMode = originalMode
            // 清除保存的模式
            context.dataStore.edit { preferences ->
                preferences.remove(KEY_ORIGINAL_RINGER_MODE)
            }
        }
    }

    /**
     * 获取当前铃声模式
     */
    fun getCurrentRingerMode(): Int = audioManager.ringerMode

    /**
     * 检查是否处于静音/振动模式
     */
    fun isSilentOrVibrate(): Boolean {
        return audioManager.ringerMode == AudioManager.RINGER_MODE_SILENT ||
                audioManager.ringerMode == AudioManager.RINGER_MODE_VIBRATE
    }

    /**
     * 从 SettingsDataStore 读取自动静音开关
     */
    suspend fun isSilentModeEnabled(): Boolean {
        return settingsDataStore.autoSilentEnabled.first()
    }
}
