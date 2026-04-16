package com.cue.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cue.data.local.entity.DailyCheckInEntity

/**
 * Data access object (DAO) for the DailyCheckInEntity.
 * Provides methods for inserting and retrieving daily check-in entities.
 */
@Dao
interface DailyCheckInDao {

    /**
     * Inserts a daily check-in entity into the database.
     * if the daily check-in exists, it will be replaced
     * @param checkIn The daily check-in entity to insert.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCheckIn(checkIn: DailyCheckInEntity): Long

    /**
     * Retrieves all daily check-in entities from the database.
     * @return A list of daily check-in entities.
     */
    @Query("SELECT * FROM DailyCheckIn ORDER BY timestamp DESC")
    suspend fun getAllCheckIns(): List<DailyCheckInEntity>

    /**
     * Retrieves the most recent daily check-in entity from the database.
     * @param startOfDay The start of the day in milliseconds.
     */
    @Query("SELECT * FROM DailyCheckIn WHERE timestamp >= :startOfDay")
    suspend fun getRecentCheckIn(startOfDay: Long): DailyCheckInEntity?
}
