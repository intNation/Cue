package com.cue.data.repository

import androidx.room.withTransaction
import com.cue.data.local.CueDatabase
import com.cue.data.local.dao.UserDao
import com.cue.data.local.mappers.toDomain
import com.cue.data.local.mappers.toEntity
import com.cue.data.local.mappers.toLocationEntities
import com.cue.data.local.mappers.toScheduleEntities
import com.cue.domain.model.User
import com.cue.domain.repository.UserRepository

/**
 * Implementation of [UserRepository] using Room database.
 */
class UserRepositoryImpl(
    private val db: CueDatabase,
    private val userDao: UserDao
) : UserRepository {


    override suspend fun saveUser(user: User): Long {
        //to maintain atomicity, db.withTransaction is used
        //either all operations of inserting a user profile are successful or none are
        return db.withTransaction {
            val userId = userDao.insertUser(user.toEntity())
            
            // To maintain normalization and avoid duplicates, 
            // we could clear existing locations/schedules here if updating,
            // but for onboarding (Step 1-3), we are typically inserting fresh or replacing.
            
            // Prepare entities with the returned userId
            val userWithId = user.copy(id = userId)
            
            userDao.insertLocations(userWithId.toLocationEntities())
            userDao.insertSchedules(userWithId.toScheduleEntities())
            
            userId
        }
    }

    override suspend fun getUser(userId: Long): User? {
        return userDao.getUserWithDetails(userId)?.toDomain()
    }

    override suspend fun getCurrentUser(): User? {
        val userEntity = userDao.getCurrentUser() ?: return null
        return getUser(userEntity.id)
    }
}
