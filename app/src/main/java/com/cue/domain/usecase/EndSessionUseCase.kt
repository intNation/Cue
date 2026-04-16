package com.cue.domain.usecase

import com.cue.domain.model.EndType
import com.cue.domain.model.SessionStatus
import com.cue.domain.repository.StudySessionRepository

/**
 * Use case for ending an active study session.
 */
class EndSessionUseCase(
    private val studyRepository: StudySessionRepository
) {
    /**
     * Ends the current active session if it exists.
     * @param endType The type of termination (MANUAL or AUTO).
     */
    suspend operator fun invoke(endType: EndType = EndType.MANUAL) {
        val activeSession = studyRepository.getActiveSession()
        activeSession?.let {
            val endedSession = it.copy(
                endTime = System.currentTimeMillis(),
                status = SessionStatus.ENDED,
                endType = endType
            )
            studyRepository.updateSession(endedSession)
        }
    }
}
