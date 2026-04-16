package com.cue.domain.repository

import com.cue.domain.model.StudySession

/**
 * Interface for the study session repository.
 * Responsible for managing study sessions.
 */
interface StudySessionRepository {
    suspend fun startSession(startTime: Long): Long
    suspend fun getActiveSession(): StudySession?
    suspend fun getAllSessions(): List<StudySession>
    suspend fun updateSession(session: StudySession)
}
