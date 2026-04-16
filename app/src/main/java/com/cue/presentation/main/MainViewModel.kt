package com.cue.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cue.domain.model.EndType
import com.cue.domain.model.StudySession
import com.cue.domain.usecase.CleanupStaleSessionsUseCase
import com.cue.domain.usecase.EndSessionUseCase
import com.cue.domain.usecase.GetActiveSessionUseCase
import com.cue.domain.usecase.StartSessionUseCase
import com.cue.domain.usecase.SubmitDailyCheckInUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Data class representing the UI state of the main screen.
 * @property activeSession The currently active study session, or null if no session is active.
 * @property isLoading A flag indicating whether the UI is currently loading data.
 * @property error An optional error message if an error occurred during data loading.
 */
data class MainUiState(
    val activeSession: StudySession? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * ViewModel for the main screen.
 * Manages the state and business logic for the main screen.
 * @param startSessionUseCase Use case for starting a new study session.
 * @param endSessionUseCase Use case for ending an active study session.
 * @param getActiveSessionUseCase Use case for getting the current active study session.
 * @param cleanupStaleSessionsUseCase Use case for cleaning up stale study sessions.
 * @param submitDailyCheckInUseCase Use case for submitting a daily check-in.
 */
class MainViewModel(
    private val startSessionUseCase: StartSessionUseCase,
    private val endSessionUseCase: EndSessionUseCase,
    private val getActiveSessionUseCase: GetActiveSessionUseCase,
    private val cleanupStaleSessionsUseCase: CleanupStaleSessionsUseCase,
    private val submitDailyCheckInUseCase: SubmitDailyCheckInUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    /**
     * Initialize the ViewModel by automatically refreshing the session status.
     * This ensures the initial state of the UI is up-to-date.
     * @see refreshSessionStatus
     */
    init {
        viewModelScope.launch {
            cleanupStaleSessionsUseCase() //close old sessions that may not be closed
            refreshSessionStatus()
        }
    }

    /**
     * Refreshes the session status by updating the UI state with the current active session.
     * @see MainUiState
     */
    fun refreshSessionStatus() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val active = getActiveSessionUseCase()
            _uiState.value = _uiState.value.copy(activeSession = active, isLoading = false)
        }
    }

    /**
     * Toggles the active study session.
     * If there is no active session, starts a new one.
     * If there is an active session, ends it manually.
     * @see StartSessionUseCase
     */
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

    /**
     *  Submits the daily check-in status indicating whether the user studied or not.
     *  This can be used to track daily study habits and provide feedback to the user.
     *  @param didStudy A boolean indicating whether the user studied today.
     *  @see SubmitDailyCheckInUseCase
     */
    fun submitCheckIn(didStudy: Boolean) {
        viewModelScope.launch {
            submitDailyCheckInUseCase(didStudy)
            // Feedback to UI?
        }
    }

}
