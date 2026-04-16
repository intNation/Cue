package com.cue.data.repository

import com.cue.data.local.dao.ContextSnapshotDao
import com.cue.data.local.entity.ContextSnapshotEntity
import com.cue.data.local.mappers.toDomain
import com.cue.domain.model.ContextSnapshot
import com.cue.domain.repository.ContextSnapShotRepository

/**
 * Implementation of the [com.cue.domain.repository.ContextSnapShotRepository] interface.
 */
class ContextSnapShotRepositoryImpl(val dao: ContextSnapshotDao) : ContextSnapShotRepository {

    override suspend fun getSnapshotBySessionId(sessionId: Long): ContextSnapshot? {
        return dao.getSnapshotBySessionId(sessionId)?.toDomain()
    }

    override suspend fun insertSnapshot(snapshot: ContextSnapshot): Long {
        val entity = ContextSnapshotEntity(
            sessionId = snapshot.sessionId,
            studyLocation = snapshot.studyLocation,
            phoneUsage = snapshot.phoneUsage,
            connectivity = snapshot.connectivity,
            sleep = snapshot.sleep,
            weather = snapshot.weather,
            confidenceScore = snapshot.confidenceScore,
            timestamp = snapshot.timestamp
        )
        return dao.insertSnapshot(entity)
    }

    override suspend fun getAllSnapshots(): List<ContextSnapshot> {
        return dao.getAllSnapshots().map { it.toDomain() }
    }
}
