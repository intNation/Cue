package com.cue.data.local.dao

import com.cue.data.local.entity.UserEntity
import com.cue.data.local.entity.StudyLocationEntity
import com.cue.data.local.entity.WeeklyScheduleEntity
import com.cue.data.local.model.UserWithDetails
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocations(locations: List<StudyLocationEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedules(schedules: List<WeeklyScheduleEntity>)

    @Query("DELETE FROM StudyLocation WHERE user_id = :userId")
    suspend fun deleteLocationsForUser(userId: Long)

    @Query("DELETE FROM WeeklySchedule WHERE user_id = :userId")
    suspend fun deleteSchedulesForUser(userId: Long)

    @Transaction
    @Query("SELECT * FROM User WHERE id = :userId")
    suspend fun getUserWithDetails(userId: Long): UserWithDetails?

    @Query("SELECT * FROM User LIMIT 1")
    suspend fun getCurrentUser(): UserEntity?
}
