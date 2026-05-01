package com.cue.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cue.data.local.dao.UserDao
import com.cue.data.local.mappers.toDomain
import com.cue.data.local.mappers.toEntity
import com.cue.data.local.mappers.toScheduleEntities
import com.cue.data.local.mappers.toStudyPlaceEntities
import com.cue.domain.model.DaySchedule
import com.cue.domain.model.StudyLocation
import com.cue.domain.model.StudyPlace
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
    fun writeUserWithStudyPlacesAndReadBack() = runBlocking {
        // Arrange
        val user = User(
            id = 0,
            firstName = "Alice",
            studyPlaces = listOf(
                StudyPlace(label = "Main Library", category = StudyLocation.LIBRARY, latitude = -26.1, longitude = 28.0),
                StudyPlace(label = "Home Desk", category = StudyLocation.HOME, latitude = -26.2, longitude = 28.1)
            ),
            weeklySchedule = listOf(
                DaySchedule(dayOfWeek = 1, startTime = "18:00", endTime = "21:00")
            ),
            phoneUsageEnabled = true,
            locationEnabled = true,
            isOnboardingCompleted = true
        )

        // Act
        val userId = userDao.insertUser(user.toEntity())
        val userWithId = user.copy(id = userId)
        
        userDao.insertLocations(userWithId.toStudyPlaceEntities())
        userDao.insertSchedules(userWithId.toScheduleEntities())

        // Retrieve
        val loaded = userDao.getUserWithDetails(userId)

        // Assert
        assertNotNull(loaded)
        val domainUser = loaded!!.toDomain()
        
        assertEquals(2, domainUser.studyPlaces.size)
        assertEquals("Main Library", domainUser.studyPlaces[0].label)
        assertEquals(true, domainUser.phoneUsageEnabled)
        assertEquals(true, domainUser.locationEnabled)
        assertEquals(1, domainUser.weeklySchedule.size)
    }
}
