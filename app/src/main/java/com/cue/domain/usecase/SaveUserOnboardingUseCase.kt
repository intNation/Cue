package com.cue.domain.usecase

import com.cue.domain.model.DaySchedule
import com.cue.domain.model.StudyLocation
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
     * @param preferredLocations The list of study locations from Step 1.
     * @param weeklySchedule The list of day schedules from Step 2.
     * @param successMetric The chosen success metric from Step 3.
     */
    suspend operator fun invoke(
        preferredLocations: List<StudyLocation>,
        weeklySchedule: List<DaySchedule>,
        successMetric: SuccessMetric
    ): Long {
        // Retrieve existing user if any, or create a new one
        val existingUser = repository.getCurrentUser() ?: User()

        // Update user with their study locations, schedule, and success metric,and set the isOnboardingCompleted flag to true
        val updatedUser = existingUser.copy(
            preferredLocations = preferredLocations,
            weeklySchedule = weeklySchedule,
            successMetric = successMetric,
            isOnboardingCompleted = true
        )

        //save the updated user with their onboarding data to room database
        return repository.saveUser(updatedUser)
    }
}
