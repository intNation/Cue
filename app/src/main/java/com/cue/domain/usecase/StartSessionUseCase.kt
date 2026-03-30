package com.cue.domain.usecase

import com.cue.domain.repository.StudySessionRepository

/**
 * Use case for starting a new study session.
 * @param repo The study session repository.
 * @constructor Creates a new StartSessionUseCase object.
 */
class StartSessionUseCase (val repo : StudySessionRepository){
    /**
     * Starts a new study session.
     * @return The ID of the newly created study session.
     */
    suspend fun invoke() : Long {
        return repo.startSession(System.currentTimeMillis())
    }

}