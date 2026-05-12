package com.hbde.courseschedule.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
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
}
