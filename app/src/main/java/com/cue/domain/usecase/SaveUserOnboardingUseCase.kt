package com.cue.domain.usecase

import com.cue.domain.model.DaySchedule
import com.cue.domain.model.StudyPlace
import com.cue.domain.model.SuccessMetric
import com.cue.domain.model.User
import com.cue.domain.repository.UserRepository

/**
 * Use case to save the user's complete onboarding profile.
 */
class SaveUserOnboardingUseCase(
    private val repository: UserRepository
) {
    /**
     * Completes the onboarding process by saving the user's profile and marking it as completed.
     * @param studyPlaces The list of anchored study places from Step 1.
     * @param weeklySchedule The list of day schedules from Step 2.
     * @param successMetric The chosen success metric from Step 3.
     * @param locationEnabled Whether location-based signals are enabled.
     * @param calendarEnabled Whether calendar-based signals are enabled.
     * @param sleepEnabled Whether sleep-based signals are enabled.
     * @param movementEnabled Whether movement-based signals are enabled.
     */
    suspend operator fun invoke(
        studyPlaces: List<StudyPlace>,
        weeklySchedule: List<DaySchedule>,
        successMetric: SuccessMetric,
        locationEnabled: Boolean,
        calendarEnabled: Boolean,
        sleepEnabled: Boolean,
        movementEnabled: Boolean
    ): Long {
        // Retrieve existing user if any, or create a new one
        val existingUser = repository.getCurrentUser() ?: User()

        // Update user with their study locations, schedule, success metric, and permissions
        val updatedUser = existingUser.copy(
            studyPlaces = studyPlaces,
            weeklySchedule = weeklySchedule,
            successMetric = successMetric,
            isOnboardingCompleted = true,
            locationEnabled = locationEnabled,
            calendarEnabled = calendarEnabled,
            sleepEnabled = sleepEnabled,
            movementEnabled = movementEnabled
        )

        //save the updated user with their onboarding data to room database
        return repository.saveUser(updatedUser)
    }
}
