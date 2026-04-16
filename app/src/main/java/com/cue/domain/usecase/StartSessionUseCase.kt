package com.cue.domain.usecase

import com.cue.domain.repository.ContextEngine
import com.cue.domain.repository.ContextSnapShotRepository
import com.cue.domain.repository.StudySessionRepository

/**
 * Use case for starting a new study session.
 * Collects a context snapshot at the start of the session.
 */
class StartSessionUseCase(
    private val studyRepository: StudySessionRepository,
    private val contextEngine: ContextEngine,
    private val snapshotRepository: ContextSnapShotRepository
) {
    /**
     * Starts a new study session and captures context.
     * @return The ID of the newly created study session.
     */
    suspend operator fun invoke(): Long {
        val startTime = System.currentTimeMillis()
        val sessionId = studyRepository.startSession(startTime)
        
        // Capture and save snapshot
        val snapshot = contextEngine.captureSnapshot(sessionId)
        snapshotRepository.insertSnapshot(snapshot)
        
        return sessionId
    }
}
