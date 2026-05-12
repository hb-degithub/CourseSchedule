package com.hbde.courseschedule.ui.campus

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class CampusUiState(
    val isLoading: Boolean = false
)

@HiltViewModel
class CampusViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(CampusUiState())
    val uiState: StateFlow<CampusUiState> = _uiState.asStateFlow()
}
