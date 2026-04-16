package com.cue.domain.repository

import com.cue.domain.model.User

/**
 * Repository interface for managing user data and onboarding progress.
 */
interface UserRepository {
    /**
     * Saves or updates a user's profile and onboarding details.
     */
    suspend fun saveUser(user: User): Long

    /**
     * Retrieves the current user with all details (locations, schedule).
     */
    suspend fun getUser(userId: Long): User?

    /**
     * Retrieves the primary user profile (if any).
     */
    suspend fun getCurrentUser(): User?
}
