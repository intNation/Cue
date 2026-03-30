package com.cue.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.cue.data.local.dao.StudySessionDao
import com.cue.data.local.dao.UserDao
import com.cue.data.local.entity.StudySessionEntity
import com.cue.data.local.entity.UserEntity

/**
 * Room database class for the Cue.
 * Provides access to the study session and user data.
 * @constructor Creates a new CueDatabase object.
 */
@Database(entities = [StudySessionEntity::class, UserEntity::class], version = 2)
abstract class CueDatabase : RoomDatabase() {
    abstract fun studySessionDao(): StudySessionDao
    abstract fun userDao() : UserDao
}