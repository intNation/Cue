package com.cue.domain.repository

import com.cue.domain.model.ContextSnapshot

interface ContextSnapShotRepository {

    abstract suspend fun getSnapshotBySessionId(sessionId: Long): ContextSnapshot?
    abstract suspend fun insertSnapshot(snapshot: ContextSnapshot): Long
    abstract suspend fun getAllSnapshots(): List<ContextSnapshot>
}