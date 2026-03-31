package com.cue.domain.usecase

import com.cue.core.constants.SessionConstants
import com.cue.domain.model.EndType
import com.cue.domain.model.SessionStatus
import com.cue.domain.repository.StudySessionRepository

/**
 * Use case to check and close any active sessions that have exceeded the maximum allowed duration.
 * This ensures data integrity by preventing "infinite" sessions if a user forgets to manually end them.
 */
class CleanupStaleSessionsUseCase(
    private val repository: StudySessionRepository
) {

    /**
     * Checks and closes any active sessions that have exceeded the maximum allowed duration.
     */
    suspend operator fun invoke() {
        val activeSession = repository.getActiveSession() ?: return // get the current  active session from the repository
        
        val currentTime = System.currentTimeMillis() //get the current time
        val duration = currentTime - activeSession.startTime

        //if study duration is above the max duration, close the session automatically
        if (duration > SessionConstants.MAX_SESSION_DURATION_MS) {
            val staleSession = activeSession.copy(
                endTime = activeSession.startTime + SessionConstants.MAX_SESSION_DURATION_MS,
                status = SessionStatus.ENDED,
                endType = EndType.AUTO
            )
            repository.updateSession(staleSession)
        }
    }
}
