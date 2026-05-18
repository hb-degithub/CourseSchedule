package com.hbde.courseschedule.service.audio

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

private val Context.audioModeDataStore: DataStore<Preferences> by preferencesDataStore(name = "audio_mode_prefs")

/**
 * 音频模式管理器
 * 封装 AudioManager，实现上课自动静音/下课恢复功能
 * 支持用户配置：静音模式（完全静音 / 振动）
 * 与 AlarmManager 联动（通过 AlarmReceiver 调用）
 */
@Singleton
class AudioModeManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsDataStore: SettingsDataStore,
) {

    companion object {
        private val KEY_ORIGINAL_RINGER_MODE = intPreferencesKey("original_ringer_mode")
    }

    private val audioManager: AudioManager by lazy {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    private val dataStore = context.audioModeDataStore

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
        dataStore.edit { preferences ->
            preferences[KEY_ORIGINAL_RINGER_MODE] = currentMode
        }

        // 切换为目标模式
        audioManager.ringerMode = mode
    }

    /**
     * 恢复之前的铃声模式
     */
    suspend fun restoreRingerMode() {
        val originalMode = dataStore.data
            .map { preferences ->
                preferences[KEY_ORIGINAL_RINGER_MODE]
            }
            .first()

        if (originalMode != null) {
            audioManager.ringerMode = originalMode
            // 清除保存的模式
            dataStore.edit { preferences ->
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
     * 检查是否拥有修改勿扰状态的权限（Android 6.0+ 需要）
     */
    fun hasNotificationPolicyAccess(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                    as android.app.NotificationManager
            notificationManager.isNotificationPolicyAccessGranted
        } else {
            true
        }
    }

    /**
     * 从 SettingsDataStore 读取用户偏好的静音模式类型
     * @return "vibrate" 或 "silent"
     */
    suspend fun getPreferredSilentModeType(): String {
        return settingsDataStore.silentModeType.first()
    }

    /**
     * 获取目标铃声模式（根据用户偏好）
     */
    suspend fun getTargetRingerMode(): Int {
        val type = getPreferredSilentModeType()
        return when (type) {
            "silent" -> AudioManager.RINGER_MODE_SILENT
            else -> AudioManager.RINGER_MODE_VIBRATE
        }
    }
}
