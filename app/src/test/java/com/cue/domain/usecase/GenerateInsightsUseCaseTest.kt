package com.cue.domain.usecase

import com.cue.domain.model.ContextSnapshot
import com.cue.domain.model.DailyCheckIn
import com.cue.domain.model.DaySchedule
import com.cue.domain.model.EndType
import com.cue.domain.model.Insight
import com.cue.domain.model.InsightType
import com.cue.domain.model.SessionStatus
import com.cue.domain.model.StudySession
import com.cue.domain.model.User
import com.cue.domain.repository.ContextSnapShotRepository
import com.cue.domain.repository.DailyCheckinRepository
import com.cue.domain.repository.InsightRepository
import com.cue.domain.repository.StudySessionRepository
import com.cue.domain.repository.UserRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class GenerateInsightsUseCaseTest {

    @Test
    fun `one failed check-in does not create insight because V4 requires repeated patterns`() = runTest {
        val timestamp = dayOffsetAt(daysAgo = 1, hour = 20)
        val fixture = fixture(
            checkIns = listOf(DailyCheckIn(timestamp = timestamp, didStudy = false)),
            snapshots = listOf(snapshot(timestamp = timestamp, phoneUsage = "High"))
        )

        fixture.useCase()

        assertTrue(fixture.insightRepository.inserted.isEmpty())
    }

    @Test
    fun `three repeated high phone usage failures create phone usage insight`() = runTest {
        val failures = failureSeries(hour = 20, count = 3)
        val fixture = fixture(
            checkIns = failures.map { DailyCheckIn(timestamp = it, didStudy = false) },
            snapshots = failures.map { snapshot(timestamp = it, phoneUsage = "High") }
        )

        fixture.useCase()

        assertInsertedTypes(fixture.insightRepository, InsightType.PHONE_USAGE)
        assertInsertedMessageContains(fixture.insightRepository, "high phone usage")
        assertTrue(fixture.insightRepository.inserted.single().confidenceScore >= 0.6f)
    }

    @Test
    fun `pattern below sixty percent frequency is filtered out`() = runTest {
        val failures = failureSeries(hour = 20, count = 5)
        val snapshots = failures.mapIndexed { index, timestamp ->
            snapshot(
                timestamp = timestamp,
                phoneUsage = if (index < 2) "High" else "Low"
            )
        }
        val fixture = fixture(
            checkIns = failures.map { DailyCheckIn(timestamp = it, didStudy = false) },
            snapshots = snapshots
        )

        fixture.useCase()

        assertTrue(fixture.insightRepository.inserted.isEmpty())
    }

    @Test
    fun `unknown signal values are ignored for occurrence counting`() = runTest {
        val failures = failureSeries(hour = 20, count = 3)
        val fixture = fixture(
            checkIns = failures.map { DailyCheckIn(timestamp = it, didStudy = false) },
            snapshots = failures.map { snapshot(timestamp = it, phoneUsage = "UNKNOWN") }
        )

        fixture.useCase()

        assertTrue(fixture.insightRepository.inserted.isEmpty())
    }

    @Test
    fun `snapshots outside correlation window are ignored`() = runTest {
        val failures = failureSeries(hour = 20, count = 3)
        val fixture = fixture(
            checkIns = failures.map { DailyCheckIn(timestamp = it, didStudy = false) },
            snapshots = failures.map {
                snapshot(timestamp = it - 4 * 60 * 60 * 1000L, phoneUsage = "High")
            }
        )

        fixture.useCase()

        assertTrue(fixture.insightRepository.inserted.isEmpty())
    }

    @Test
    fun `time bucket is included in generated message`() = runTest {
        val failures = failureSeries(hour = 9, count = 3)
        val fixture = fixture(
            checkIns = failures.map { DailyCheckIn(timestamp = it, didStudy = false) },
            snapshots = failures.map { snapshot(timestamp = it, sleep = 4) }
        )

        fixture.useCase()

        assertInsertedTypes(fixture.insightRepository, InsightType.SLEEP)
        assertInsertedMessageContains(fixture.insightRepository, "Morning")
    }

    @Test
    fun `twenty three hundred is treated as evening`() = runTest {
        val failures = failureSeries(hour = 23, count = 3)
        val fixture = fixture(
            checkIns = failures.map { DailyCheckIn(timestamp = it, didStudy = false) },
            snapshots = failures.map { snapshot(timestamp = it, phoneUsage = "High") }
        )

        fixture.useCase()

        assertInsertedTypes(fixture.insightRepository, InsightType.PHONE_USAGE)
        assertInsertedMessageContains(fixture.insightRepository, "Evening")
    }

    @Test
    fun `silent failures are analyzed through V4 frequency thresholds`() = runTest {
        val expectedFailureTimes = failureSeries(hour = 18, count = 3)
        val schedule = expectedFailureTimes.map {
            DaySchedule(dayOfWeek = cueDayOfWeek(it), startTime = "18:00", endTime = "20:00")
        }
        val fixture = fixture(
            user = User(id = USER_ID, weeklySchedule = schedule),
            sessions = emptyList(),
            checkIns = emptyList(),
            snapshots = expectedFailureTimes.map {
                snapshot(sessionId = null, timestamp = it, connectivity = "None")
            }
        )

        fixture.useCase()

        assertInsertedTypes(fixture.insightRepository, InsightType.CONNECTIVITY)
    }

    @Test
    fun `real sessions prevent scheduled days from counting as silent failures`() = runTest {
        val expectedFailureTimes = failureSeries(hour = 18, count = 3)
        val schedule = expectedFailureTimes.map {
            DaySchedule(dayOfWeek = cueDayOfWeek(it), startTime = "18:00", endTime = "20:00")
        }
        val fixture = fixture(
            user = User(id = USER_ID, weeklySchedule = schedule),
            sessions = expectedFailureTimes.mapIndexed { index, timestamp ->
                StudySession(
                    id = index + 1L,
                    startTime = timestamp + 5 * 60 * 1000,
                    endTime = timestamp + 65 * 60 * 1000,
                    status = SessionStatus.ENDED,
                    endType = EndType.MANUAL
                )
            },
            checkIns = emptyList(),
            snapshots = expectedFailureTimes.map {
                snapshot(sessionId = null, timestamp = it, connectivity = "None")
            }
        )

        fixture.useCase()

        assertTrue(fixture.insightRepository.inserted.isEmpty())
    }

    @Test
    fun `only top three insights are inserted by priority`() = runTest {
        val failures = failureSeries(hour = 20, count = 3)
        val fixture = fixture(
            checkIns = failures.map { DailyCheckIn(timestamp = it, didStudy = false) },
            snapshots = failures.map {
                snapshot(
                    timestamp = it,
                    phoneUsage = "High",
                    sleep = 4,
                    connectivity = "None",
                    weather = "Rainy"
                )
            }
        )

        fixture.useCase()

        assertEquals(3, fixture.insightRepository.inserted.size)
        assertInsertedMessageContains(fixture.insightRepository, "both high phone usage and less than 5 hours of sleep")
        assertInsertedMessageContains(fixture.insightRepository, "both high phone usage and no internet connectivity")
        assertInsertedMessageContains(fixture.insightRepository, "high phone usage over 1 hour")
    }

    @Test
    fun `two signal pattern creates explainable combined insight`() = runTest {
        val failures = failureSeries(hour = 20, count = 3)
        val fixture = fixture(
            checkIns = failures.map { DailyCheckIn(timestamp = it, didStudy = false) },
            snapshots = failures.map {
                snapshot(
                    timestamp = it,
                    phoneUsage = "High",
                    sleep = 4
                )
            }
        )

        fixture.useCase()

        assertInsertedTypes(
            fixture.insightRepository,
            InsightType.PHONE_USAGE,
            InsightType.PHONE_USAGE,
            InsightType.SLEEP
        )
        assertInsertedMessageContains(fixture.insightRepository, "both high phone usage and less than 5 hours of sleep")
    }

    @Test
    fun `recent matching insight suppresses duplicate history insert`() = runTest {
        val failures = failureSeries(hour = 20, count = 3)
        val existing = Insight(
            id = 44L,
            userId = USER_ID,
            message = "You tend to miss study sessions after high phone usage over 1 hour before scheduled study sessions in the Evening.",
            type = InsightType.PHONE_USAGE,
            timestamp = System.currentTimeMillis(),
            confidenceScore = 0.6f
        )
        val fixture = fixture(
            existingInsights = listOf(existing),
            checkIns = failures.map { DailyCheckIn(timestamp = it, didStudy = false) },
            snapshots = failures.map { snapshot(timestamp = it, phoneUsage = "High") }
        )

        fixture.useCase()

        assertTrue(fixture.insightRepository.inserted.isEmpty())
    }

    @Test
    fun `older matching insight creates new history row without reusing existing id`() = runTest {
        val failures = failureSeries(hour = 20, count = 3)
        val existing = Insight(
            id = 44L,
            userId = USER_ID,
            message = "You tend to miss study sessions after high phone usage over 1 hour before scheduled study sessions in the Evening.",
            type = InsightType.PHONE_USAGE,
            timestamp = System.currentTimeMillis() - (4 * 24 * 60 * 60 * 1000L),
            confidenceScore = 0.6f
        )
        val fixture = fixture(
            existingInsights = listOf(existing),
            checkIns = failures.map { DailyCheckIn(timestamp = it, didStudy = false) },
            snapshots = failures.map { snapshot(timestamp = it, phoneUsage = "High") }
        )

        fixture.useCase()

        assertInsertedTypes(fixture.insightRepository, InsightType.PHONE_USAGE)
        assertEquals(0L, fixture.insightRepository.inserted.single().id)
    }

    @Test
    fun `same insight type in a different time bucket is inserted as a distinct pattern`() = runTest {
        val failures = failureSeries(hour = 20, count = 3)
        val existingMorningInsight = Insight(
            id = 44L,
            userId = USER_ID,
            message = "You tend to miss study sessions after high phone usage over 1 hour before scheduled study sessions in the Morning.",
            type = InsightType.PHONE_USAGE,
            timestamp = System.currentTimeMillis(),
            confidenceScore = 0.7f
        )
        val fixture = fixture(
            existingInsights = listOf(existingMorningInsight),
            checkIns = failures.map { DailyCheckIn(timestamp = it, didStudy = false) },
            snapshots = failures.map { snapshot(timestamp = it, phoneUsage = "High") }
        )

        fixture.useCase()

        assertInsertedTypes(fixture.insightRepository, InsightType.PHONE_USAGE)
        assertInsertedMessageContains(fixture.insightRepository, "Evening")
    }

    @Test
    fun `no current user exits without touching dependent repositories`() = runTest {
        val userRepository = FakeUserRepository(user = null)
        val sessionRepository = FakeStudySessionRepository()
        val checkInRepository = FakeDailyCheckinRepository()
        val snapshotRepository = FakeContextSnapshotRepository()
        val insightRepository = FakeInsightRepository()
        val useCase = GenerateInsightsUseCase(
            userRepository,
            sessionRepository,
            checkInRepository,
            snapshotRepository,
            insightRepository
        )

        useCase()

        assertEquals(0, checkInRepository.getAllCheckInsCalls)
        assertEquals(0, snapshotRepository.getAllSnapshotsCalls)
        assertEquals(0, sessionRepository.getAllSessionsCalls)
        assertTrue(insightRepository.inserted.isEmpty())
    }

    private fun fixture(
        user: User = User(id = USER_ID),
        sessions: List<StudySession> = emptyList(),
        checkIns: List<DailyCheckIn> = emptyList(),
        snapshots: List<ContextSnapshot> = emptyList(),
        existingInsights: List<Insight> = emptyList()
    ): Fixture {
        val insightRepository = FakeInsightRepository(existingInsights)
        return Fixture(
            useCase = GenerateInsightsUseCase(
                userRepository = FakeUserRepository(user),
                sessionRepository = FakeStudySessionRepository(sessions),
                checkinRepository = FakeDailyCheckinRepository(checkIns),
                snapshotRepository = FakeContextSnapshotRepository(snapshots),
                insightRepository = insightRepository
            ),
            insightRepository = insightRepository
        )
    }

    private fun snapshot(
        sessionId: Long? = 100L,
        timestamp: Long,
        studyLocation: String = "UNKNOWN",
        phoneUsage: String = "Low",
        sleep: Int = 8,
        connectivity: String = "WiFi",
        weather: String = "Sunny",
        confidenceScore: Float = 0.9f
    ) = ContextSnapshot(
        sessionId = sessionId,
        studyLocation = studyLocation,
        phoneUsage = phoneUsage,
        sleep = sleep,
        connectivity = connectivity,
        weather = weather,
        confidenceScore = confidenceScore,
        timestamp = timestamp
    )

    private fun failureSeries(hour: Int, count: Int): List<Long> {
        return (1..count).map { dayOffsetAt(daysAgo = it, hour = hour) }
    }

    private fun dayOffsetAt(daysAgo: Int, hour: Int, minute: Int = 0): Long {
        return Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -daysAgo)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun cueDayOfWeek(timestamp: Long): Int {
        return when (Calendar.getInstance().apply { timeInMillis = timestamp }.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            Calendar.SUNDAY -> 7
            else -> error("Unsupported day of week")
        }
    }

    private fun assertInsertedTypes(
        insightRepository: FakeInsightRepository,
        vararg expectedTypes: InsightType
    ) {
        assertEquals(expectedTypes.toList(), insightRepository.inserted.map { it.type })
    }

    private fun assertInsertedMessageContains(
        insightRepository: FakeInsightRepository,
        expectedText: String
    ) {
        assertTrue(
            insightRepository.inserted.any { insight ->
                insight.message.contains(expectedText, ignoreCase = true)
            }
        )
    }

    private data class Fixture(
        val useCase: GenerateInsightsUseCase,
        val insightRepository: FakeInsightRepository
    )

    private class FakeUserRepository(
        private val user: User?
    ) : UserRepository {
        override suspend fun saveUser(user: User): Long = user.id
        override suspend fun getUser(userId: Long): User? = user?.takeIf { it.id == userId }
        override suspend fun getCurrentUser(): User? = user
    }

    private class FakeStudySessionRepository(
        private val sessions: List<StudySession> = emptyList()
    ) : StudySessionRepository {
        var getAllSessionsCalls = 0

        override suspend fun startSession(startTime: Long): Long = error("Not used")
        override suspend fun getActiveSession(): StudySession? = error("Not used")
        override suspend fun getAllSessions(): List<StudySession> {
            getAllSessionsCalls++
            return sessions
        }
        override suspend fun updateSession(session: StudySession) = error("Not used")
    }

    private class FakeDailyCheckinRepository(
        private val checkIns: List<DailyCheckIn> = emptyList()
    ) : DailyCheckinRepository {
        var getAllCheckInsCalls = 0

        override suspend fun insertCheckIn(checkIn: DailyCheckIn): Long = error("Not used")
        override suspend fun getAllCheckIns(): List<DailyCheckIn> {
            getAllCheckInsCalls++
            return checkIns
        }
        override suspend fun getRecentCheckIn(startOfDay: Long): DailyCheckIn? = error("Not used")
    }

    private class FakeContextSnapshotRepository(
        private val snapshots: List<ContextSnapshot> = emptyList()
    ) : ContextSnapShotRepository {
        var getAllSnapshotsCalls = 0

        override suspend fun getSnapshotBySessionId(sessionId: Long): ContextSnapshot? = error("Not used")
        override suspend fun insertSnapshot(snapshot: ContextSnapshot): Long = error("Not used")
        override suspend fun getAllSnapshots(): List<ContextSnapshot> {
            getAllSnapshotsCalls++
            return snapshots
        }
    }

    private class FakeInsightRepository(
        initialInsights: List<Insight> = emptyList()
    ) : InsightRepository {
        private val existing = initialInsights.toMutableList()
        val inserted = mutableListOf<Insight>()

        override suspend fun insertInsight(insight: Insight): Long {
            inserted += insight
            existing += insight
            return inserted.size.toLong()
        }

        override suspend fun getUserInsights(userId: Long): List<Insight> {
            return existing.filter { it.userId == userId }
        }

        override suspend fun getInsightById(insightId: Long): Insight? {
            return existing.firstOrNull { it.id == insightId }
        }

        override suspend fun getInsightByType(userId: Long, insightType: String): List<Insight> {
            return existing.filter { it.userId == userId && it.type.name == insightType }
        }
    }

    private companion object {
        const val USER_ID = 1L
    }
}
