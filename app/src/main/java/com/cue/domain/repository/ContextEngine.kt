package com.cue.domain.repository

import com.cue.domain.model.ContextSnapshot

/**
 * Interface for the Context Engine responsible for signal collection.
 */
interface ContextEngine {
    /**
     * Captures a snapshot of the current context for a given session.
     */
    suspend fun captureSnapshot(sessionId: Long): ContextSnapshot
}
