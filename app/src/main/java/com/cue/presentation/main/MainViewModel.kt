package com.cue.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cue.domain.model.EndType
import com.cue.domain.model.StudySession
import com.cue.domain.usecase.EndSessionUseCase
import com.cue.domain.usecase.GetActiveSessionUseCase
import com.cue.domain.usecase.StartSessionUseCase
import com.cue.domain.usecase.SubmitDailyCheckInUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MainUiState(
    val activeSession: StudySession? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class MainViewModel(
    private val startSessionUseCase: StartSessionUseCase,
    private val endSessionUseCase: EndSessionUseCase,
    private val getActiveSessionUseCase: GetActiveSessionUseCase,
    private val submitDailyCheckInUseCase: SubmitDailyCheckInUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init { //runs when the viewmodel instance is created
        refreshSessionStatus()
    }

    fun refreshSessionStatus() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val active = getActiveSessionUseCase()
            _uiState.value = _uiState.value.copy(activeSession = active, isLoading = false)
        }
    }

    fun toggleSession() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val current = _uiState.value.activeSession
            if (current == null) {
                startSessionUseCase()
            } else {
                endSessionUseCase(EndType.MANUAL)
            }
            refreshSessionStatus()
        }
    }

    fun submitCheckIn(didStudy: Boolean) {
        viewModelScope.launch {
            submitDailyCheckInUseCase(didStudy)
            // Feedback to UI?
        }
    }
}
