package com.cue.data.repository

import com.cue.data.local.dao.DailyCheckInDao
import com.cue.data.local.entity.DailyCheckInEntity
import com.cue.data.local.mappers.toDomain
import com.cue.domain.model.DailyCheckIn
import com.cue.domain.repository.DailyCheckinRepository

class DailyCheckinRepositoryImpl(val dao: DailyCheckInDao) : DailyCheckinRepository
{
    /**
     * Inserts a daily check-in entity into the database.
     * @param checkIn The daily check-in to insert.
     * @return The ID of the inserted check-in entity.
     */
    override suspend fun insertCheckIn(checkIn: DailyCheckIn): Long {
        val entity = DailyCheckInEntity(
            timestamp = checkIn.timestamp,
            didStudy = checkIn.didStudy,
        )
        return dao.insertCheckIn(entity)

    }

    /**
     * Gets all daily check-in entities from the database.
     * @return A list of daily check-in entities.
     */
    override suspend fun getAllCheckIns(): List<DailyCheckIn> {
        return dao.getAllCheckIns().map { it.toDomain() }
    }

    override suspend fun getRecentCheckIn(startOfDay: Long): DailyCheckIn? {
        return dao.getRecentCheckIn(startOfDay)?.toDomain()
    }

}