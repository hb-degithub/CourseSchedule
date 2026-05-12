package com.hbde.courseschedule.service

import android.content.Context
import android.media.AudioManager
import android.os.Build
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "silent_mode_prefs")

@Singleton
class SilentModeManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    companion object {
        private val KEY_ORIGINAL_RINGER_MODE = intPreferencesKey("original_ringer_mode")
    }

    private val audioManager: AudioManager by lazy {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    /**
     * 启用静音模式（振动模式）
     * 保存原始铃声模式到 DataStore
     */
    suspend fun enableSilentMode() {
        val currentMode = audioManager.ringerMode

        // 如果当前已经是振动或静音，不需要重复操作
        if (currentMode == AudioManager.RINGER_MODE_VIBRATE ||
            currentMode == AudioManager.RINGER_MODE_SILENT
        ) {
            return
        }

        // 保存原始铃声模式
        context.dataStore.edit { preferences ->
            preferences[KEY_ORIGINAL_RINGER_MODE] = currentMode
        }

        // 设置为振动模式（比完全静音更安全，不会错过紧急来电）
        audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
    }

    /**
     * 禁用静音模式，恢复之前的铃声模式
     */
    suspend fun disableSilentMode() {
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
}
