package com.cue.domain.usecase

import com.cue.domain.model.*
import com.cue.domain.repository.*
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.*

class GenerateInsightsUseCaseTest {

    private lateinit var userRepository: UserRepository
    private val sessionRepository = mockk<StudySessionRepository>()
    private val checkinRepository = mockk<DailyCheckinRepository>()
    private val snapshotRepository = mockk<ContextSnapShotRepository>()
    private val insightRepository = mockk<InsightRepository>()
    
    private lateinit var generateInsightsUseCase: GenerateInsightsUseCase

    @Before
    fun setup() {
        userRepository = mockk()
        generateInsightsUseCase = GenerateInsightsUseCase(
            userRepository,
            sessionRepository,
            checkinRepository,
            snapshotRepository,
            insightRepository
        )
    }

    @Test
    fun `when user checked in as No and phone usage was High, generate phone usage insight`() = runTest {
        // Arrange
        val userId = 1L
        val user = User(id = userId)
        val timestamp = System.currentTimeMillis()
        
        coEvery { userRepository.getCurrentUser() } returns user
        coEvery { checkinRepository.getAllCheckIns() } returns listOf(
            DailyCheckIn(id = 1, timestamp = timestamp, didStudy = false)
        )
        coEvery { snapshotRepository.getAllSnapshots() } returns listOf(
            ContextSnapshot(
                sessionId = 100L,
                phoneUsage = "High",
                connectivity = "WiFi",
                sleep = 8,
                weather = "Sunny",
                confidenceScore = 0.9f,
                timestamp = timestamp - 1000 // Close to checkin
            )
        )
        coEvery { sessionRepository.getAllSessions() } returns emptyList()
        coEvery { insightRepository.getUserInsights(userId) } returns emptyList()
        coEvery { insightRepository.insertInsight(any()) } returns 1L

        // Act
        generateInsightsUseCase()

        // Assert
        coVerify { 
            insightRepository.insertInsight(match { 
                it.type == InsightType.PHONE_USAGE && 
                it.message.contains("high phone usage", ignoreCase = true) 
            }) 
        }
    }

    @Test
    fun `when scheduled session was missed and Ghost Snapshot shows low sleep, generate sleep insight`() = runTest {
        // Arrange
        val userId = 1L
        val now = Calendar.getInstance()
        val dayOfWeek = when(now.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            Calendar.SUNDAY -> 7
            else -> 1
        }
        
        val user = User(
            id = userId,
            weeklySchedule = listOf(
                DaySchedule(dayOfWeek = dayOfWeek, startTime = "18:00", endTime = "20:00")
            )
        )
        
        coEvery { userRepository.getCurrentUser() } returns user
        coEvery { checkinRepository.getAllCheckIns() } returns emptyList()
        coEvery { sessionRepository.getAllSessions() } returns emptyList() // Missed session
        
        val snapshotTimestamp = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 18)
            set(Calendar.MINUTE, 0)
        }.timeInMillis

        coEvery { snapshotRepository.getAllSnapshots() } returns listOf(
            ContextSnapshot(
                sessionId = null, // Ghost Snapshot
                phoneUsage = "Low",
                connectivity = "WiFi",
                sleep = 4, // Low sleep
                weather = "Rainy",
                confidenceScore = 0.8f,
                timestamp = snapshotTimestamp
            )
        )
        coEvery { insightRepository.getUserInsights(userId) } returns emptyList()
        coEvery { insightRepository.insertInsight(any()) } returns 1L

        // Act
        generateInsightsUseCase()

        // Assert
        coVerify { 
            insightRepository.insertInsight(match { 
                it.type == InsightType.SLEEP && 
                it.message.contains("poor sleep", ignoreCase = true) 
            }) 
        }
    }
}
