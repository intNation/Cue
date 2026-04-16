package com.cue.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cue.data.local.dao.UserDao
import com.cue.data.local.entity.UserEntity
import com.cue.data.local.mappers.toDomain
import com.cue.data.local.mappers.toEntity
import com.cue.data.local.mappers.toLocationEntities
import com.cue.data.local.mappers.toScheduleEntities
import com.cue.domain.model.DaySchedule
import com.cue.domain.model.StudyLocation
import com.cue.domain.model.SuccessMetric
import com.cue.domain.model.User
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UserPersistenceTest {

    private lateinit var db: CueDatabase
    private lateinit var userDao: UserDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, CueDatabase::class.java).build()
        userDao = db.userDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun writeUserAndReadWithDetails() = runBlocking {
        // Arrange
        val user = User(
            id = 0, // Room will generate ID
            firstName = "John",
            lastName = "Doe",
            email = "john@example.com",
            preferredLocations = listOf(StudyLocation.LIBRARY, StudyLocation.HOME),
            weeklySchedule = listOf(
                DaySchedule(dayOfWeek = 1, startTime = "09:00", endTime = "17:00"),
                DaySchedule(dayOfWeek = 2, isFlexible = true)
            ),
            successMetric = SuccessMetric.TIME_DURATION,
            isOnboardingCompleted = true
        )

        // Act - Manual insertion simulating UserRepositoryImpl.saveUser
        val userId = userDao.insertUser(user.toEntity())
        val userWithId = user.copy(id = userId)
        
        userDao.insertLocations(userWithId.toLocationEntities())
        userDao.insertSchedules(userWithId.toScheduleEntities())

        // Retrieve
        val loaded = userDao.getUserWithDetails(userId)

        // Assert
        assertNotNull(loaded)
        val domainUser = loaded!!.toDomain()
        
        assertEquals("John", domainUser.firstName)
        assertEquals(2, domainUser.preferredLocations.size)
        assertEquals(StudyLocation.LIBRARY, domainUser.preferredLocations[0])
        assertEquals(2, domainUser.weeklySchedule.size)
        assertEquals("09:00", domainUser.weeklySchedule.find { it.dayOfWeek == 1 }?.startTime)
        assertEquals(true, domainUser.weeklySchedule.find { it.dayOfWeek == 2 }?.isFlexible)
    }
}
