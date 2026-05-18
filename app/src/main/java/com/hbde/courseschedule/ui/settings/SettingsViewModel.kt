package com.hbde.courseschedule.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hbde.courseschedule.data.local.SettingsDataStore
import com.hbde.courseschedule.data.local.entity.ThemeConfigEntity
import com.hbde.courseschedule.data.repository.ThemeConfigRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val themeConfigRepository: ThemeConfigRepository
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

    val termStartDate: StateFlow<LocalDate?> = settingsDataStore.termStartDate
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // Appearance settings
    val backgroundType: StateFlow<String> = settingsDataStore.backgroundType
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "color"
        )

    val backgroundColor: StateFlow<String> = settingsDataStore.backgroundColor
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "#FFF5F5F5"
        )

    val backgroundImageUri: StateFlow<String?> = settingsDataStore.backgroundImageUri
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val backgroundOpacity: StateFlow<Float> = settingsDataStore.backgroundOpacity
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 1.0f
        )

    val borderWidth: StateFlow<Int> = settingsDataStore.borderWidth
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val courseNameFontSize: StateFlow<Int> = settingsDataStore.courseNameFontSize
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 14
        )

    val classroomFontSize: StateFlow<Int> = settingsDataStore.classroomFontSize
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 12
        )

    val calendarSyncEnabled: StateFlow<Boolean> = settingsDataStore.calendarSyncEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    // Custom URLs
    val gradeUrl: StateFlow<String?> = settingsDataStore.gradeUrl
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val selectedCoursesUrl: StateFlow<String?> = settingsDataStore.selectedCoursesUrl
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val portalUrl: StateFlow<String?> = settingsDataStore.portalUrl
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val themeConfig = themeConfigRepository.getThemeConfig()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ThemeConfigEntity(
                id = 0,
                primaryColor = 0xFF2196F3.toInt(),
                backgroundImage = "#FFF5F5F5",
                opacity = 1.0f,
                cornerRadius = 4,
                fontSize = 14
            )
        )

    val presetThemes = themeConfigRepository.getPresetThemes()

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

    fun onTermStartDateChange(date: LocalDate?) {
        viewModelScope.launch {
            settingsDataStore.setTermStartDate(date)
        }
    }

    // Appearance settings actions
    fun applyPresetTheme(preset: ThemeConfigEntity) {
        viewModelScope.launch {
            themeConfigRepository.updateThemeConfig(preset)
            settingsDataStore.setBackgroundColor(preset.backgroundImage ?: "#FFF5F5F5")
            settingsDataStore.setBackgroundType("color")
        }
    }

    fun onBackgroundTypeChange(type: String) {
        viewModelScope.launch {
            settingsDataStore.setBackgroundType(type)
        }
    }

    fun onBackgroundColorChange(color: String) {
        viewModelScope.launch {
            settingsDataStore.setBackgroundColor(color)
        }
    }

    fun onBackgroundImageUriChange(uri: String?) {
        viewModelScope.launch {
            settingsDataStore.setBackgroundImageUri(uri)
        }
    }

    fun onBackgroundOpacityChange(opacity: Float) {
        viewModelScope.launch {
            settingsDataStore.setBackgroundOpacity(opacity)
        }
    }

    fun onBorderWidthChange(width: Int) {
        viewModelScope.launch {
            settingsDataStore.setBorderWidth(width)
        }
    }

    fun onCourseNameFontSizeChange(size: Int) {
        viewModelScope.launch {
            settingsDataStore.setCourseNameFontSize(size)
        }
    }

    fun onClassroomFontSizeChange(size: Int) {
        viewModelScope.launch {
            settingsDataStore.setClassroomFontSize(size)
        }
    }

    fun onCalendarSyncEnabledChange(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setCalendarSyncEnabled(enabled)
        }
    }

    // Custom URL actions
    fun onGradeUrlChange(url: String?) {
        viewModelScope.launch {
            settingsDataStore.setGradeUrl(url)
        }
    }

    fun onSelectedCoursesUrlChange(url: String?) {
        viewModelScope.launch {
            settingsDataStore.setSelectedCoursesUrl(url)
        }
    }

    fun onPortalUrlChange(url: String?) {
        viewModelScope.launch {
            settingsDataStore.setPortalUrl(url)
        }
    }

    fun importSchedule() {
        // TODO: Navigate to import flow or trigger import
    }

    fun exportSchedule() {
        // TODO: Trigger export
    }
}
