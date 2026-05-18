package com.hbde.courseschedule.ui.campus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hbde.courseschedule.data.local.SettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class CampusUiState(
    val isLoading: Boolean = false
)

@HiltViewModel
class CampusViewModel @Inject constructor(
    settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(CampusUiState())
    val uiState: StateFlow<CampusUiState> = _uiState.asStateFlow()

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
}
