package com.hbde.courseschedule.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val dataStore = context.dataStore

    companion object {
        val REMINDER_MINUTES = intPreferencesKey("reminder_minutes")
        val TTS_ENABLED = booleanPreferencesKey("tts_enabled")
        val AUTO_SILENT_ENABLED = booleanPreferencesKey("auto_silent_enabled")
        val DAILY_OVERVIEW_HOUR = intPreferencesKey("daily_overview_hour")

        // TTS settings
        val TTS_SPEED = floatPreferencesKey("tts_speed")
        val TTS_VOLUME = floatPreferencesKey("tts_volume")

        // Appearance settings
        val BACKGROUND_TYPE = stringPreferencesKey("background_type") // "color" or "image"
        val BACKGROUND_COLOR = stringPreferencesKey("background_color") // hex string
        val BACKGROUND_IMAGE_URI = stringPreferencesKey("background_image_uri")
        val BACKGROUND_OPACITY = floatPreferencesKey("background_opacity")
        val BORDER_WIDTH = intPreferencesKey("border_width") // dp
        val COURSE_NAME_FONT_SIZE = intPreferencesKey("course_name_font_size") // sp
        val CLASSROOM_FONT_SIZE = intPreferencesKey("classroom_font_size") // sp

        // Calendar sync
        val CALENDAR_SYNC_ENABLED = booleanPreferencesKey("calendar_sync_enabled")

        // Onboarding
        val HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("has_completed_onboarding")
    }

    val reminderMinutes: Flow<Int> = dataStore.data
        .map { preferences ->
            preferences[REMINDER_MINUTES] ?: 15
        }

    val ttsEnabled: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[TTS_ENABLED] ?: false
        }

    val autoSilentEnabled: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[AUTO_SILENT_ENABLED] ?: false
        }

    val dailyOverviewHour: Flow<Int> = dataStore.data
        .map { preferences ->
            preferences[DAILY_OVERVIEW_HOUR] ?: 8
        }

    // TTS settings flows
    val ttsSpeed: Flow<Float> = dataStore.data
        .map { preferences ->
            preferences[TTS_SPEED] ?: 1.0f
        }

    val ttsVolume: Flow<Float> = dataStore.data
        .map { preferences ->
            preferences[TTS_VOLUME] ?: 1.0f
        }

    // Appearance settings flows
    val backgroundType: Flow<String> = dataStore.data
        .map { preferences ->
            preferences[BACKGROUND_TYPE] ?: "color"
        }

    val backgroundColor: Flow<String> = dataStore.data
        .map { preferences ->
            preferences[BACKGROUND_COLOR] ?: "#FFF5F5F5"
        }

    val backgroundImageUri: Flow<String?> = dataStore.data
        .map { preferences ->
            preferences[BACKGROUND_IMAGE_URI]
        }

    val backgroundOpacity: Flow<Float> = dataStore.data
        .map { preferences ->
            preferences[BACKGROUND_OPACITY] ?: 1.0f
        }

    val borderWidth: Flow<Int> = dataStore.data
        .map { preferences ->
            preferences[BORDER_WIDTH] ?: 0
        }

    val courseNameFontSize: Flow<Int> = dataStore.data
        .map { preferences ->
            preferences[COURSE_NAME_FONT_SIZE] ?: 14
        }

    val classroomFontSize: Flow<Int> = dataStore.data
        .map { preferences ->
            preferences[CLASSROOM_FONT_SIZE] ?: 12
        }

    suspend fun setReminderMinutes(minutes: Int) {
        dataStore.edit { preferences ->
            preferences[REMINDER_MINUTES] = minutes
        }
    }

    suspend fun setTtsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[TTS_ENABLED] = enabled
        }
    }

    suspend fun setAutoSilentEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[AUTO_SILENT_ENABLED] = enabled
        }
    }

    suspend fun setDailyOverviewHour(hour: Int) {
        dataStore.edit { preferences ->
            preferences[DAILY_OVERVIEW_HOUR] = hour
        }
    }

    // TTS settings setters
    suspend fun setTtsSpeed(speed: Float) {
        dataStore.edit { preferences ->
            preferences[TTS_SPEED] = speed.coerceIn(0.5f, 2.0f)
        }
    }

    suspend fun setTtsVolume(volume: Float) {
        dataStore.edit { preferences ->
            preferences[TTS_VOLUME] = volume.coerceIn(0f, 1f)
        }
    }

    // Appearance settings setters
    suspend fun setBackgroundType(type: String) {
        dataStore.edit { preferences ->
            preferences[BACKGROUND_TYPE] = type
        }
    }

    suspend fun setBackgroundColor(color: String) {
        dataStore.edit { preferences ->
            preferences[BACKGROUND_COLOR] = color
        }
    }

    suspend fun setBackgroundImageUri(uri: String?) {
        dataStore.edit { preferences ->
            if (uri != null) {
                preferences[BACKGROUND_IMAGE_URI] = uri
            } else {
                preferences.remove(BACKGROUND_IMAGE_URI)
            }
        }
    }

    suspend fun setBackgroundOpacity(opacity: Float) {
        dataStore.edit { preferences ->
            preferences[BACKGROUND_OPACITY] = opacity.coerceIn(0f, 1f)
        }
    }

    suspend fun setBorderWidth(width: Int) {
        dataStore.edit { preferences ->
            preferences[BORDER_WIDTH] = width.coerceIn(0, 2)
        }
    }

    suspend fun setCourseNameFontSize(size: Int) {
        dataStore.edit { preferences ->
            preferences[COURSE_NAME_FONT_SIZE] = size.coerceIn(12, 20)
        }
    }

    suspend fun setClassroomFontSize(size: Int) {
        dataStore.edit { preferences ->
            preferences[CLASSROOM_FONT_SIZE] = size.coerceIn(10, 16)
        }
    }

    // Calendar sync
    val calendarSyncEnabled: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[CALENDAR_SYNC_ENABLED] ?: false
        }

    suspend fun setCalendarSyncEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[CALENDAR_SYNC_ENABLED] = enabled
        }
    }

    // Onboarding
    val hasCompletedOnboarding: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[HAS_COMPLETED_ONBOARDING] ?: false
        }

    suspend fun setHasCompletedOnboarding(completed: Boolean) {
        dataStore.edit { preferences ->
            preferences[HAS_COMPLETED_ONBOARDING] = completed
        }
    }
}
