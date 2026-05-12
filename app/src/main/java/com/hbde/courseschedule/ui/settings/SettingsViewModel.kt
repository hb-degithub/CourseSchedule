package com.hbde.courseschedule.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hbde.courseschedule.data.local.SettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    val reminderMinutes: StateFlow<Int> = settingsDataStore.reminderMinutes
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 15
        )

    val ttsEnabled: StateFlow<Boolean> = settingsDataStore.ttsEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val autoSilentEnabled: StateFlow<Boolean> = settingsDataStore.autoSilentEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val dailyOverviewHour: StateFlow<Int> = settingsDataStore.dailyOverviewHour
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 8
        )

    fun onTtsEnabledChange(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setTtsEnabled(enabled)
        }
    }

    fun onAutoSilentEnabledChange(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setAutoSilentEnabled(enabled)
        }
    }

    fun onReminderMinutesChange(minutes: Int) {
        viewModelScope.launch {
            settingsDataStore.setReminderMinutes(minutes)
        }
    }

    fun onDailyOverviewHourChange(hour: Int) {
        viewModelScope.launch {
            settingsDataStore.setDailyOverviewHour(hour)
        }
    }

    fun importSchedule() {
        // TODO: Navigate to import flow or trigger import
    }

    fun exportSchedule() {
        // TODO: Trigger export
    }
}
