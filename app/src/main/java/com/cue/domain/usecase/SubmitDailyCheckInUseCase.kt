package com.cue.domain.usecase

import com.cue.domain.model.DailyCheckIn
import com.cue.domain.repository.DailyCheckinRepository

/**
 * Use case for submitting a daily check-in.
 */
class SubmitDailyCheckInUseCase(
    private val repository: DailyCheckinRepository
) {
    suspend operator fun invoke(didStudy: Boolean): Long {
        val checkIn = DailyCheckIn(
            timestamp = System.currentTimeMillis(),
            didStudy = didStudy
        )
        return repository.insertCheckIn(checkIn)
    }
}
