package com.cue.domain.repository

import com.cue.data.local.dao.StudySessionDao
import com.cue.data.local.entity.StudySessionEntity
import com.cue.data.local.entity.toDomain
import com.cue.domain.model.SessionStatus
import com.cue.domain.model.StudySession

class StudySessionRepositoryImpl(val dao: StudySessionDao): StudySessionRepository {
    override suspend fun startSession(startTime: Long): Long {
        val entity = StudySessionEntity(
            startTime = startTime,
            endTime = null,
            status = SessionStatus.ACTIVE.name,
            endType = null,
            createdAt = System.currentTimeMillis()
        )
        return dao.insertSession(entity)
    }

    override suspend fun getActiveSession(): StudySession? {
        return dao.getActiveStudySessions()?.toDomain()
    }

    override suspend fun getAllSessions(): List<StudySession> {
        return dao.getAllSessions().map { it.toDomain() }
    }


}