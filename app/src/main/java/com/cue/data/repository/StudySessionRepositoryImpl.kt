package com.cue.data.repository

import com.cue.data.local.dao.StudySessionDao
import com.cue.data.local.entity.StudySessionEntity
import com.cue.data.local.mappers.toDomain
import com.cue.data.local.mappers.toEntity
import com.cue.domain.model.SessionStatus
import com.cue.domain.model.StudySession
import com.cue.domain.repository.StudySessionRepository

/**
 * Implementation of the [com.cue.domain.repository.StudySessionRepository] interface.
 */
class StudySessionRepositoryImpl(private val dao: StudySessionDao) : StudySessionRepository {

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
        return dao.getActiveSession()?.toDomain()
    }

    override suspend fun getAllSessions(): List<StudySession> {
        return dao.getAllSessions().map { it.toDomain() }
    }

    override suspend fun updateSession(session: StudySession) {
        dao.updateSession(session.toEntity())
    }
}
