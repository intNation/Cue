package com.cue.domain.repository

import com.cue.data.local.dao.StudySessionDao
import com.cue.data.local.entity.StudySessionEntity
import com.cue.data.local.entity.toDomain
import com.cue.domain.model.SessionStatus
import com.cue.domain.model.StudySession

/**
 * Implementation of the [StudySessionRepository] interface.
 * Responsible for managing study sessions in the database.
 * gets study sessions from the database and maps them to the domain model.
 * inserts a study session  entity to the database using the dao
 */
class StudySessionRepositoryImpl(val dao: StudySessionDao): StudySessionRepository {

    /**
     * Starts a new study session.
     * @param startTime The start time of the study session.
     * @return The ID of the newly created study session.
     */
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

    /**
     * Gets the active study session.
     * @return The active study session, or null if no active session is found.
     */
    override suspend fun getActiveSession(): StudySession? {
        return dao.getActiveStudySessions()?.toDomain()
    }

    /**
     * Gets all study sessions.
     * @return List of study sessions in the database as study session models
     */
    override suspend fun getAllSessions(): List<StudySession> {
        return dao.getAllSessions().map { it.toDomain() }
    }


}