package com.cue.presentation.onboarding

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cue.context.contracts.ProviderResult
import com.cue.context.contracts.UnavailableReason
import com.cue.context.provider.LocationProvider
import com.cue.core.util.ScheduleManager
import com.cue.domain.model.DaySchedule
import com.cue.domain.model.StudyLocation
import com.cue.domain.model.StudyPlace
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
    val studyPlaces: List<StudyPlace> = emptyList(),
    val weeklySchedule: List<DaySchedule> = emptyList(),
    val successMetric: SuccessMetric? = null,
    val locationEnabled: Boolean = false,
    val calendarEnabled: Boolean = false,
    val sleepEnabled: Boolean = false,
    val movementEnabled: Boolean = false,
    val isAnchoringPlace: Boolean = false,
    val anchorError: String? = null,
    val isSaving: Boolean = false,
    val onboardingCompleted: Boolean = false
)

/**
 * ViewModel for managing the 4-step onboarding flow.
 */
class OnboardingViewModel(
    application: Application,
    private val saveUserOnboardingUseCase: SaveUserOnboardingUseCase
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()
    private val locationProvider = LocationProvider(application)

    fun onAddStudyPlace(category: StudyLocation, label: String) {
        val trimmedLabel = label.trim()
        if (trimmedLabel.isBlank()) {
            _uiState.update { it.copy(anchorError = "Add a label before saving this anchor.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isAnchoringPlace = true, anchorError = null) }

            when (val result = locationProvider.getWhetherLocation()) {
                is ProviderResult.Available -> {
                    val newPlace = StudyPlace(
                        label = trimmedLabel,
                        category = category,
                        latitude = result.data.latitude,
                        longitude = result.data.longitude
                    )
                    _uiState.update {
                        it.copy(
                            studyPlaces = it.studyPlaces + newPlace,
                            isAnchoringPlace = false
                        )
                    }
                }

                is ProviderResult.Unavailable -> {
                    _uiState.update {
                        it.copy(
                            isAnchoringPlace = false,
                            anchorError = result.reason.toAnchorErrorMessage()
                        )
                    }
                }
            }
        }
    }

    fun onRemoveStudyPlace(place: StudyPlace) {
        _uiState.update { it.copy(studyPlaces = it.studyPlaces - place) }
    }

    fun clearAnchorError() {
        _uiState.update { it.copy(anchorError = null) }
    }

    fun onScheduleChange(schedule: List<DaySchedule>) {
        _uiState.update { it.copy(weeklySchedule = schedule) }
    }

    fun onSuccessMetricSelect(metric: SuccessMetric) {
        _uiState.update { it.copy(successMetric = metric) }
    }

    fun toggleLocationPermission(enabled: Boolean) {
        _uiState.update { it.copy(locationEnabled = enabled) }
    }

    fun toggleCalendarPermission(enabled: Boolean) {
        _uiState.update { it.copy(calendarEnabled = enabled) }
    }

    fun toggleSleepPermission(enabled: Boolean) {
        _uiState.update { it.copy(sleepEnabled = enabled) }
    }

    fun toggleMovementPermission(enabled: Boolean) {
        _uiState.update { it.copy(movementEnabled = enabled) }
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
                studyPlaces = state.studyPlaces,
                weeklySchedule = state.weeklySchedule,
                successMetric = metric,
                locationEnabled = state.locationEnabled,
                calendarEnabled = state.calendarEnabled,
                sleepEnabled = state.sleepEnabled,
                movementEnabled = state.movementEnabled
            )
            
            // Start background context polling
            ScheduleManager(getApplication()).updateSchedule(state.weeklySchedule)
            
            _uiState.update { it.copy(isSaving = false, onboardingCompleted = true) }
        }
    }
}

private fun UnavailableReason.toAnchorErrorMessage(): String {
    return when (this) {
        UnavailableReason.PERMISSION_DENIED ->
            "Location permission is required to anchor your current study place."
        UnavailableReason.SYSTEM_SETTING_DISABLED ->
            "Turn on device location services before anchoring this place."
        UnavailableReason.DATA_NOT_AVAILABLE ->
            "Cue could not get your current location. Try again where location is available."
        UnavailableReason.TIMEOUT ->
            "Location lookup took too long. Try again."
        UnavailableReason.TEMPORARY_ERROR ->
            "Cue could not anchor this place right now. Try again."
    }
}
