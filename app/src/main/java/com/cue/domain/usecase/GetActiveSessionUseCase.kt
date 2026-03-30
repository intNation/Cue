package com.cue.domain.usecase

import com.cue.domain.model.StudySession
import com.cue.domain.repository.StudySessionRepository

/**
 * Use case for getting the currently active study session.
 */
class GetActiveSessionUseCase(
    private val repository: StudySessionRepository
) {
    suspend operator fun invoke(): StudySession? {
        return repository.getActiveSession()
    }
}
