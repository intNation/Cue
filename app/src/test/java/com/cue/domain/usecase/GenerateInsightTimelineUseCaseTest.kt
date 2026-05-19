package com.cue.domain.usecase

import com.cue.domain.model.Insight
import com.cue.domain.model.InsightTimeLineEventType
import com.cue.domain.model.InsightType
import com.cue.domain.model.User
import com.cue.domain.repository.InsightRepository
import com.cue.domain.repository.UserRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GenerateInsightTimelineUseCaseTest {

    private lateinit var userRepo: UserRepository
    private lateinit var insightRepo: InsightRepository
    private lateinit var useCase: GenerateInsightTimelineUseCase

    @Before
    fun setUp() {
        userRepo = mockk()
        insightRepo = mockk()
        useCase = GenerateInsightTimelineUseCase(userRepo, insightRepo)
    }

    @Test
    fun `User authentication check`() = runBlocking {
        coEvery { userRepo.getCurrentUser() } returns null

        val result = useCase.invoke()

        assertTrue(result.isEmpty())
        coVerify(exactly = 0) { insightRepo.getUserInsights(any()) }
    }

    @Test
    fun `Null insight repository response handling`() = runBlocking {
        val user = testUser()
        coEvery { userRepo.getCurrentUser() } returns user
        coEvery { insightRepo.getUserInsights(user.id) } returns null

        val result = useCase.invoke()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `Fourteen day filtering boundary   older items`() = runBlocking {
        val user = testUser()
        coEvery { userRepo.getCurrentUser() } returns user
        coEvery { insightRepo.getUserInsights(user.id) } returns listOf(
            insight(message = "Expired pattern", timestamp = daysAgo(15))
        )

        val result = useCase.invoke()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `Fourteen day filtering boundary   inclusive items`() = runBlocking {
        val user = testUser()
        val recentTimestamp = System.currentTimeMillis() - (14L * DAY_MS) + ONE_MINUTE_MS
        coEvery { userRepo.getCurrentUser() } returns user
        coEvery { insightRepo.getUserInsights(user.id) } returns listOf(
            insight(message = "Boundary pattern", timestamp = recentTimestamp),
            insight(message = "Clearly recent pattern", timestamp = daysAgo(1))
        )

        val result = useCase.invoke()

        assertEquals(2, result.size)
        assertTrue(result.any { it.message == "Boundary pattern" })
        assertTrue(result.any { it.message == "Clearly recent pattern" })
    }

    @Test
    fun `Chronological state determination for NEWLY DETECTED`() = runBlocking {
        val user = testUser()
        coEvery { userRepo.getCurrentUser() } returns user
        coEvery { insightRepo.getUserInsights(user.id) } returns listOf(
            insight(message = "Unique pattern", timestamp = daysAgo(2))
        )

        val result = useCase.invoke()

        assertEquals(InsightTimeLineEventType.NEWLY_DETECTED, result.single().eventType)
    }

    @Test
    fun `State transition to REINFORCED for recurring messages`() = runBlocking {
        val user = testUser()
        val olderTimestamp = daysAgo(5)
        val newerTimestamp = daysAgo(1)
        coEvery { userRepo.getCurrentUser() } returns user
        coEvery { insightRepo.getUserInsights(user.id) } returns listOf(
            insight(message = "Repeated pattern", timestamp = newerTimestamp),
            insight(message = "Repeated pattern", timestamp = olderTimestamp)
        )

        val result = useCase.invoke()

        assertEquals(2, result.size)
        assertEquals(
            InsightTimeLineEventType.REINFORCED,
            result.first { it.timestamp == newerTimestamp }.eventType
        )
        assertEquals(
            InsightTimeLineEventType.NEWLY_DETECTED,
            result.first { it.timestamp == olderTimestamp }.eventType
        )
    }

    @Test
    fun `Multiple REINFORCED occurrences for a single message`() = runBlocking {
        val user = testUser()
        val oldestTimestamp = daysAgo(6)
        val middleTimestamp = daysAgo(3)
        val newestTimestamp = daysAgo(1)
        coEvery { userRepo.getCurrentUser() } returns user
        coEvery { insightRepo.getUserInsights(user.id) } returns listOf(
            insight(message = "Repeated pattern", timestamp = newestTimestamp),
            insight(message = "Repeated pattern", timestamp = oldestTimestamp),
            insight(message = "Repeated pattern", timestamp = middleTimestamp)
        )

        val result = useCase.invoke()

        assertEquals(
            InsightTimeLineEventType.NEWLY_DETECTED,
            result.first { it.timestamp == oldestTimestamp }.eventType
        )
        assertEquals(
            InsightTimeLineEventType.REINFORCED,
            result.first { it.timestamp == middleTimestamp }.eventType
        )
        assertEquals(
            InsightTimeLineEventType.REINFORCED,
            result.first { it.timestamp == newestTimestamp }.eventType
        )
    }

    @Test
    fun `Internal sort order stability for state calculation`() = runBlocking {
        val user = testUser()
        val oldestTimestamp = daysAgo(4)
        val newestTimestamp = daysAgo(1)
        coEvery { userRepo.getCurrentUser() } returns user
        coEvery { insightRepo.getUserInsights(user.id) } returns listOf(
            insight(id = 2L, message = "Out of order pattern", timestamp = newestTimestamp),
            insight(id = 1L, message = "Out of order pattern", timestamp = oldestTimestamp)
        )

        val result = useCase.invoke()

        assertEquals(
            InsightTimeLineEventType.NEWLY_DETECTED,
            result.first { it.id == 1L }.eventType
        )
        assertEquals(
            InsightTimeLineEventType.REINFORCED,
            result.first { it.id == 2L }.eventType
        )
    }

    @Test
    fun `Final output display sorting`() = runBlocking {
        val user = testUser()
        coEvery { userRepo.getCurrentUser() } returns user
        coEvery { insightRepo.getUserInsights(user.id) } returns listOf(
            insight(message = "Oldest", timestamp = daysAgo(5)),
            insight(message = "Newest", timestamp = daysAgo(1)),
            insight(message = "Middle", timestamp = daysAgo(3))
        )

        val result = useCase.invoke()

        assertEquals(listOf("Newest", "Middle", "Oldest"), result.map { it.message })
    }

    @Test
    fun `Pagination and item limit enforcement`() = runBlocking {
        val user = testUser()
        val insights = (1..25).map { index ->
            insight(
                id = index.toLong(),
                message = "Pattern $index",
                timestamp = System.currentTimeMillis() - (index * ONE_MINUTE_MS)
            )
        }
        coEvery { userRepo.getCurrentUser() } returns user
        coEvery { insightRepo.getUserInsights(user.id) } returns insights

        val result = useCase.invoke()

        assertEquals(20, result.size)
        assertEquals((1L..20L).toList(), result.map { it.id })
    }

    @Test
    fun `Case sensitivity in message duplication logic`() = runBlocking {
        val user = testUser()
        coEvery { userRepo.getCurrentUser() } returns user
        coEvery { insightRepo.getUserInsights(user.id) } returns listOf(
            insight(message = "Warning", timestamp = daysAgo(2)),
            insight(message = "warning", timestamp = daysAgo(1))
        )

        val result = useCase.invoke()

        assertEquals(2, result.size)
        assertTrue(result.all { it.eventType == InsightTimeLineEventType.NEWLY_DETECTED })
    }

    @Test
    fun `Empty insight repository response`() = runBlocking {
        val user = testUser()
        coEvery { userRepo.getCurrentUser() } returns user
        coEvery { insightRepo.getUserInsights(user.id) } returns emptyList()

        val result = useCase.invoke()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `Handling of identical timestamps for different messages`() = runBlocking {
        val user = testUser()
        val timestamp = daysAgo(1)
        coEvery { userRepo.getCurrentUser() } returns user
        coEvery { insightRepo.getUserInsights(user.id) } returns listOf(
            insight(message = "Pattern A", timestamp = timestamp),
            insight(message = "Pattern B", timestamp = timestamp)
        )

        val result = useCase.invoke()

        assertEquals(2, result.size)
        assertTrue(result.any { it.message == "Pattern A" })
        assertTrue(result.any { it.message == "Pattern B" })
        assertTrue(result.all { it.eventType == InsightTimeLineEventType.NEWLY_DETECTED })
    }

    @Test
    fun `Data integrity during mapping`() = runBlocking {
        val user = testUser()
        val timestamp = daysAgo(1)
        coEvery { userRepo.getCurrentUser() } returns user
        coEvery { insightRepo.getUserInsights(user.id) } returns listOf(
            insight(
                id = 44L,
                message = "Mapped pattern",
                type = InsightType.SLEEP,
                timestamp = timestamp,
                confidenceScore = 0.83f
            )
        )

        val result = useCase.invoke().single()

        assertEquals(44L, result.id)
        assertEquals(InsightType.SLEEP, result.insightType)
        assertEquals("Mapped pattern", result.message)
        assertEquals(0.83f, result.confidence, 0.0001f)
        assertEquals(timestamp, result.timestamp)
        assertEquals(InsightTimeLineEventType.NEWLY_DETECTED, result.eventType)
    }

    @Test
    fun `Filtering all insights as expired`() = runBlocking {
        val user = testUser()
        coEvery { userRepo.getCurrentUser() } returns user
        coEvery { insightRepo.getUserInsights(user.id) } returns listOf(
            insight(message = "Expired A", timestamp = daysAgo(15)),
            insight(message = "Expired B", timestamp = daysAgo(20))
        )

        val result = useCase.invoke()

        assertTrue(result.isEmpty())
    }

    private fun testUser(): User {
        return User(id = USER_ID, email = "test@example.com", firstName = "Test")
    }

    private fun insight(
        id: Long = 0L,
        userId: Long = USER_ID,
        message: String,
        type: InsightType = InsightType.PHONE_USAGE,
        timestamp: Long = daysAgo(1),
        confidenceScore: Float = 0.7f
    ): Insight {
        return Insight(
            id = id,
            userId = userId,
            message = message,
            type = type,
            timestamp = timestamp,
            confidenceScore = confidenceScore
        )
    }

    private companion object {
        const val USER_ID = 123888438L
        const val DAY_MS = 24L * 60 * 60 * 1000
        const val ONE_MINUTE_MS = 60L * 1000

        fun daysAgo(days: Int): Long {
            return System.currentTimeMillis() - (days * DAY_MS)
        }
    }
}
