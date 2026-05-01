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
    fun `explicit failed check-in with high phone usage creates phone usage insight`() = runTest {
        val timestamp = todayAt(hour = 20)
        val fixture = fixture(
            checkIns = listOf(DailyCheckIn(timestamp = timestamp, didStudy = false)),
            snapshots = listOf(snapshot(timestamp = timestamp, phoneUsage = "High"))
        )

        fixture.useCase()

        assertInsertedTypes(fixture.insightRepository, InsightType.PHONE_USAGE)
        assertInsertedMessageContains(fixture.insightRepository, "high phone usage")
    }

    @Test
    fun `explicit failed check-in with poor sleep creates sleep insight`() = runTest {
        val timestamp = todayAt(hour = 20)
        val fixture = fixture(
            checkIns = listOf(DailyCheckIn(timestamp = timestamp, didStudy = false)),
            snapshots = listOf(snapshot(timestamp = timestamp, sleep = 4))
        )

        fixture.useCase()

        assertInsertedTypes(fixture.insightRepository, InsightType.SLEEP)
        assertInsertedMessageContains(fixture.insightRepository, "poor sleep")
    }

    @Test
    fun `explicit failed check-in with no connectivity creates connectivity insight`() = runTest {
        val timestamp = todayAt(hour = 20)
        val fixture = fixture(
            checkIns = listOf(DailyCheckIn(timestamp = timestamp, didStudy = false)),
            snapshots = listOf(snapshot(timestamp = timestamp, connectivity = "None"))
        )

        fixture.useCase()

        assertInsertedTypes(fixture.insightRepository, InsightType.CONNECTIVITY)
        assertInsertedMessageContains(fixture.insightRepository, "internet connectivity")
    }

    @Test
    fun `single failed check-in can create multiple cause insights from same snapshot`() = runTest {
        val timestamp = todayAt(hour = 20)
        val fixture = fixture(
            checkIns = listOf(DailyCheckIn(timestamp = timestamp, didStudy = false)),
            snapshots = listOf(
                snapshot(
                    timestamp = timestamp,
                    phoneUsage = "High",
                    sleep = 5,
                    connectivity = "None"
                )
            )
        )

        fixture.useCase()

        assertInsertedTypes(
            fixture.insightRepository,
            InsightType.PHONE_USAGE,
            InsightType.SLEEP,
            InsightType.CONNECTIVITY
        )
    }

    @Test
    fun `silent missed scheduled session with ghost snapshot creates insight`() = runTest {
        val scheduledTime = todayAt(hour = 18)
        val fixture = fixture(
            user = User(
                id = USER_ID,
                weeklySchedule = listOf(
                    DaySchedule(
                        dayOfWeek = cueDayOfWeek(scheduledTime),
                        startTime = "18:00",
                        endTime = "20:00",
                        isFlexible = false
                    )
                )
            ),
            sessions = emptyList(),
            checkIns = emptyList(),
            snapshots = listOf(
                snapshot(
                    sessionId = null,
                    timestamp = scheduledTime,
                    phoneUsage = "High"
                )
            )
        )

        fixture.useCase()

        assertInsertedTypes(fixture.insightRepository, InsightType.PHONE_USAGE)
    }

    @Test
    fun `silent failure is ignored when a study session exists on scheduled day`() = runTest {
        val scheduledTime = todayAt(hour = 18)
        val fixture = fixture(
            user = User(
                id = USER_ID,
                weeklySchedule = listOf(
                    DaySchedule(
                        dayOfWeek = cueDayOfWeek(scheduledTime),
                        startTime = "18:00",
                        endTime = "20:00",
                        isFlexible = false
                    )
                )
            ),
            sessions = listOf(
                StudySession(
                    id = 10L,
                    startTime = todayAt(hour = 18, minute = 5),
                    endTime = todayAt(hour = 19),
                    status = SessionStatus.ENDED,
                    endType = EndType.MANUAL
                )
            ),
            checkIns = emptyList(),
            snapshots = listOf(
                snapshot(
                    sessionId = null,
                    timestamp = scheduledTime,
                    phoneUsage = "High",
                    sleep = 4,
                    connectivity = "None"
                )
            )
        )

        fixture.useCase()

        assertTrue(fixture.insightRepository.inserted.isEmpty())
    }

    @Test
    fun `positive check-ins do not create failure insights`() = runTest {
        val timestamp = todayAt(hour = 20)
        val fixture = fixture(
            checkIns = listOf(DailyCheckIn(timestamp = timestamp, didStudy = true)),
            snapshots = listOf(
                snapshot(
                    timestamp = timestamp,
                    phoneUsage = "High",
                    sleep = 4,
                    connectivity = "None"
                )
            )
        )

        fixture.useCase()

        assertTrue(fixture.insightRepository.inserted.isEmpty())
    }

    @Test
    fun `existing insight type is not inserted again`() = runTest {
        val timestamp = todayAt(hour = 20)
        val existingPhoneUsageInsight = Insight(
            userId = USER_ID,
            message = "Existing phone usage insight",
            type = InsightType.PHONE_USAGE,
            timestamp = timestamp - 1_000
        )
        val fixture = fixture(
            existingInsights = listOf(existingPhoneUsageInsight),
            checkIns = listOf(DailyCheckIn(timestamp = timestamp, didStudy = false)),
            snapshots = listOf(snapshot(timestamp = timestamp, phoneUsage = "High"))
        )

        fixture.useCase()

        assertTrue(fixture.insightRepository.inserted.isEmpty())
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

    private fun todayAt(hour: Int, minute: Int = 0): Long {
        return Calendar.getInstance().apply {
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
    }

    private companion object {
        const val USER_ID = 1L
    }
}
