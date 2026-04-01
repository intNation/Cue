package com.cue.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cue.domain.model.DaySchedule
import com.cue.domain.model.StudyLocation
import com.cue.domain.model.SuccessMetric
import com.cue.domain.usecase.SaveUserOnboardingUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI State for the Onboarding process.
 */
data class OnboardingUiState(
    val currentStep: Int = 1,
    val selectedLocations: List<StudyLocation> = emptyList(),
    val weeklySchedule: List<DaySchedule> = emptyList(),
    val successMetric: SuccessMetric? = null,
    val isSaving: Boolean = false,
    val onboardingCompleted: Boolean = false
)

/**
 * ViewModel for managing the 3-step onboarding flow.
 */
class OnboardingViewModel(
    private val saveUserOnboardingUseCase: SaveUserOnboardingUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun onLocationToggle(location: StudyLocation) {
        _uiState.update { state ->
            val newList = if (state.selectedLocations.contains(location)) {
                state.selectedLocations - location
            } else {
                state.selectedLocations + location
            }
            state.copy(selectedLocations = newList)
        }
    }

    fun onScheduleChange(schedule: List<DaySchedule>) {
        _uiState.update { it.copy(weeklySchedule = schedule) }
    }

    fun onSuccessMetricSelect(metric: SuccessMetric) {
        _uiState.update { it.copy(successMetric = metric) }
    }

    fun nextStep() {
        _uiState.update { it.copy(currentStep = it.currentStep + 1) }
    }

    fun previousStep() {
        if (_uiState.value.currentStep > 1) {
            _uiState.update { it.copy(currentStep = it.currentStep - 1) }
        }
    }

    fun completeOnboarding() {
        val state = _uiState.value
        val metric = state.successMetric ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            saveUserOnboardingUseCase(
                preferredLocations = state.selectedLocations,
                weeklySchedule = state.weeklySchedule,
                successMetric = metric
            )
            _uiState.update { it.copy(isSaving = false, onboardingCompleted = true) }
        }
    }
}
