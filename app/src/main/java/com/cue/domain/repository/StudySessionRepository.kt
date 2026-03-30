package com.cue.domain.repository

import com.cue.domain.model.StudySession

interface StudySessionRepository {

    abstract suspend fun startSession(startTime: Long) : Long
    abstract suspend fun getActiveSession(): StudySession?
    abstract suspend fun getAllSessions(): List<StudySession>

}