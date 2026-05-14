package com.cue.domain.usecase

import com.cue.domain.repository.InsightRepository
import com.cue.domain.repository.UserRepository
import com.cue.domain.model.Insight
import com.cue.domain.model.InsightType
import com.cue.domain.model.User
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
/**
 * Unit tests for [GenerateInsightSummaryUseCase].
 *
 * This test suite verifies the logic for aggregating, filtering, and sorting user insights
 * into summarized patterns. It covers business rules including:
 * - Authentication checks and repository error handling.
 * - Filtering insights based on a 14-day temporal window.
 * - Grouping insights by unique message content.
 * - Calculating average confidence scores and identifying the latest timestamps.
 * - Complex multi-criteria sorting (Recurrence, Confidence, and Recency).
 * - Enforcing the maximum limit of summarized results.
 */
class GenerateInsightSummaryUseCaseTest {

    private lateinit var userRepo: UserRepository
    private lateinit var insightRepo: InsightRepository
    private lateinit var useCase: GenerateInsightSummaryUseCase

    @Before
    fun setUp() {
        userRepo = mockk()
        insightRepo = mockk()
        useCase = GenerateInsightSummaryUseCase(insightRepo ,userRepo)
    }

    /**
     * Verifies that the use case returns an empty list immediately when there is no
     * authenticated user (i.e., `userRepo.getCurrentUser()` returns `null`).
     */
    @Test
    fun `User not logged in returns empty list`() = runBlocking {
        // Given: The user repository indicates no user is currently logged in.
        coEvery { userRepo.getCurrentUser() } returns null

        // When: The use case is executed.
        val result = useCase()

        // Then: An empty list is returned and the insight repository is never queried.
        assertTrue("Result should be an empty list when user is null", result.isEmpty())
        coVerify(exactly = 0) { insightRepo.getUserInsights(any()) }
    }

    /**
     * Verifies that the use case returns an empty list when the insight repository
     * returns no data (either `null` or an empty list) for the authenticated user.
     */
    @Test
    fun `Repository returns null or empty insights`() = runBlocking {
        // Given: A user is logged in.
        val user = User(id = 123888438, email = "test@example.com", firstName = "Test User")
        coEvery { userRepo.getCurrentUser() } returns user

        // Scenario 1: Repository returns null
        coEvery { insightRepo.getUserInsights(user.id) } returns null

        // When: Executing the use case
        val resultNull = useCase()

        // Then: Result should be empty
        assertTrue("Result should be empty when repository returns null", resultNull.isEmpty())

        // Scenario 2: Repository returns empty list
        coEvery { insightRepo.getUserInsights(user.id) } returns emptyList()

        // When: Executing the use case
        val resultEmpty = useCase()

        // Then: Result should be empty
        assertTrue("Result should be empty when repository returns empty list", resultEmpty.isEmpty())
    }

    /**
     * Verifies that the use case returns an empty list when all available insights
     * are older than the defined look-back window (e.g., 14 days).
     *
     * This test ensures that the filtering logic correctly excludes data that is no
     * longer considered relevant for the summary.
     */
    @Test
    fun `All insights outside the PATTERN WINDOW MS window`() = runBlocking {
        val user = testUser()
        coEvery { userRepo.getCurrentUser() } returns user
        coEvery { insightRepo.getUserInsights(user.id) } returns listOf(
            insight(message = "Old pattern A", timestamp = daysAgo(15)),
            insight(message = "Old pattern B", timestamp = daysAgo(20))
        )

        val result = useCase()

        assertTrue(result.isEmpty())
    }

    /**
     * Verifies that the use case correctly filters out insights that fall outside the
     * defined temporal window while retaining those that are within the window.
     *
     * This test ensures that the "recent" insight (1 day old) is included in the summary
     * while the "old" insight (15 days old) is excluded based on the 14-day policy.
     */
    @Test
    fun `Mix of recent and old insights filtering`() = runBlocking {
        val user = testUser()
        coEvery { userRepo.getCurrentUser() } returns user
        coEvery { insightRepo.getUserInsights(user.id) } returns listOf(
            insight(message = "Recent pattern", timestamp = daysAgo(1)),
            insight(message = "Old pattern", timestamp = daysAgo(15))
        )

        val result = useCase()

        assertEquals(1, result.size)
        assertEquals("Recent pattern", result.single().message)
    }

    /**
     * Verifies that multiple insight records sharing the exact same message content
     * are aggregated into a single summary entry, and that the recurrence count
     * accurately reflects the number of identical messages found.
     */
    @Test
    fun `Grouping by identical message content`() = runBlocking {
        val user = testUser()
        coEvery { userRepo.getCurrentUser() } returns user
        coEvery { insightRepo.getUserInsights(user.id) } returns listOf(
            insight(message = "Same pattern", timestamp = daysAgo(1)),
            insight(message = "Same pattern", timestamp = daysAgo(2)),
            insight(message = "Different pattern", timestamp = daysAgo(1))
        )

        val result = useCase()

        assertEquals(2, result.size)
        assertTrue(result.any { it.message == "Same pattern" && it.recurrenceCount == 2 })
        assertTrue(result.any { it.message == "Different pattern" && it.recurrenceCount == 1 })
    }

    /**
     * Verifies that the average confidence score is correctly calculated by averaging the
     * individual confidence scores of all insights within a grouped pattern.
     */
    @Test
    fun `Calculation of averageConfidence for grouped insights`() = runBlocking {
        val user = testUser()
        coEvery { userRepo.getCurrentUser() } returns user
        coEvery { insightRepo.getUserInsights(user.id) } returns listOf(
            insight(message = "Confidence pattern", confidenceScore = 0.6f),
            insight(message = "Confidence pattern", confidenceScore = 0.8f),
            insight(message = "Confidence pattern", confidenceScore = 1.0f)
        )

        val result = useCase()

        assertEquals(0.8f, result.single().averageConfidence, 0.0001f)
    }

    /**
     * Verifies that the [GenerateInsightSummaryUseCase] correctly calculates the
     * `recurrenceCount` for grouped insights, ensuring the count matches the
     * number of original insights sharing the same message.
     */
    @Test
    fun `RecurrenceCount reflects group size`() = runBlocking {
        val user = testUser()
        coEvery { userRepo.getCurrentUser() } returns user
        coEvery { insightRepo.getUserInsights(user.id) } returns listOf(
            insight(message = "Repeated pattern"),
            insight(message = "Repeated pattern"),
            insight(message = "Repeated pattern")
        )

        val result = useCase()

        assertEquals(3, result.single().recurrenceCount)
    }

    /**
     * Verifies that when insights are grouped by message content, the resulting summary
     * correctly identifies and stores the most recent timestamp and its associated
     * [InsightType] from the group.
     */
    @Test
    fun `LatestTimestamp selection within a group`() = runBlocking {
        val user = testUser()
        val latestTimestamp = daysAgo(1)
        coEvery { userRepo.getCurrentUser() } returns user
        coEvery { insightRepo.getUserInsights(user.id) } returns listOf(
            insight(message = "Timestamp pattern", type = InsightType.SLEEP, timestamp = daysAgo(3)),
            insight(message = "Timestamp pattern", type = InsightType.PHONE_USAGE, timestamp = latestTimestamp)
        )

        val result = useCase()

        assertEquals(latestTimestamp, result.single().latestTimestamp)
        assertEquals(InsightType.PHONE_USAGE, result.single().insightType)
    }

    /**
     * Verifies that the resulting summary list is primary-sorted by the frequency of occurrence
     * in descending order, ensuring that the most frequently occurring patterns appear first.
     */
    @Test
    fun `Sorting by recurrenceCount descending`() = runBlocking {
        val user = testUser()
        coEvery { userRepo.getCurrentUser() } returns user
        coEvery { insightRepo.getUserInsights(user.id) } returns listOf(
            insight(message = "Low recurrence"),
            insight(message = "High recurrence"),
            insight(message = "High recurrence"),
            insight(message = "High recurrence")
        )

        val result = useCase()

        assertEquals("High recurrence", result.first().message)
        assertEquals(3, result.first().recurrenceCount)
    }

    /**
     * Verifies that when multiple insight groups have the same recurrence count, the
     * results are secondary-sorted by their average confidence score in descending order.
     *
     * This ensures that patterns with higher confidence scores are prioritized in the
     * final list when frequency is equal.
     */
    @Test
    fun `Secondary sorting by averageConfidence descending`() = runBlocking {
        val user = testUser()
        coEvery { userRepo.getCurrentUser() } returns user
        coEvery { insightRepo.getUserInsights(user.id) } returns listOf(
            insight(message = "Higher confidence", confidenceScore = 0.9f),
            insight(message = "Higher confidence", confidenceScore = 0.9f),
            insight(message = "Lower confidence", confidenceScore = 0.6f),
            insight(message = "Lower confidence", confidenceScore = 0.6f)
        )

        val result = useCase()

        assertEquals("Higher confidence", result.first().message)
    }

    /**
     * Verifies that when multiple insight groups have identical recurrence counts and
     * identical average confidence scores, the results are tertiary-sorted by their
     * latest timestamp in ascending order.
     *
     * This test ensures that older patterns are prioritized in the resulting list
     * when frequency and confidence are otherwise equal.
     */
    @Test
    fun `Tertiary sorting by latestTimestamp ascending`() = runBlocking {
        val user = testUser()
        coEvery { userRepo.getCurrentUser() } returns user
        coEvery { insightRepo.getUserInsights(user.id) } returns listOf(
            insight(message = "Newer pattern", confidenceScore = 0.7f, timestamp = daysAgo(1)),
            insight(message = "Older pattern", confidenceScore = 0.7f, timestamp = daysAgo(3))
        )

        val result = useCase()

        assertEquals("Older pattern", result.first().message)
    }

    /**
     * Verifies that the resulting list of summarized patterns is truncated to the
     * predefined maximum limit (e.g., top 3 results), even when the number of
     * unique patterns exceeds that limit.
     */
    @Test
    fun `Maximum result limit enforcement`() = runBlocking {
        val user = testUser()
        coEvery { userRepo.getCurrentUser() } returns user
        coEvery { insightRepo.getUserInsights(user.id) } returns listOf(
            insight(message = "Pattern 1"),
            insight(message = "Pattern 2"),
            insight(message = "Pattern 3"),
            insight(message = "Pattern 4")
        )

        val result = useCase()

        assertEquals(3, result.size)
    }

    /**
     * Verifies that the aggregation logic remains stable and accurate when multiple
     * insights share the exact same timestamp.
     *
     * This ensures that grouping, recurrence counting, and average confidence
     * calculations are not negatively impacted by temporal collisions.
     */
    @Test
    fun `Handling insights with identical timestamps`() = runBlocking {
        val user = testUser()
        val timestamp = daysAgo(1)
        coEvery { userRepo.getCurrentUser() } returns user
        coEvery { insightRepo.getUserInsights(user.id) } returns listOf(
            insight(message = "Identical timestamp", timestamp = timestamp, confidenceScore = 0.6f),
            insight(message = "Identical timestamp", timestamp = timestamp, confidenceScore = 0.8f)
        )

        val result = useCase()

        assertEquals(1, result.size)
        assertEquals(timestamp, result.single().latestTimestamp)
        assertEquals(2, result.single().recurrenceCount)
        assertEquals(0.7f, result.single().averageConfidence, 0.0001f)
    }

    /**
     * Verifies that the calculation of the average confidence score maintains expected
     * numerical precision when dealing with non-trivial floating-point values.
     *
     * This test ensures that the aggregation logic correctly sums and divides
     * confidence scores without introducing significant rounding errors that would
     * fall outside the acceptable delta.
     */
    @Test
    fun `Floating point precision in average calculation`() = runBlocking {
        val user = testUser()
        coEvery { userRepo.getCurrentUser() } returns user
        coEvery { insightRepo.getUserInsights(user.id) } returns listOf(
            insight(message = "Precision pattern", confidenceScore = 0.61f),
            insight(message = "Precision pattern", confidenceScore = 0.72f),
            insight(message = "Precision pattern", confidenceScore = 0.83f)
        )

        val result = useCase()

        assertEquals(0.72f, result.single().averageConfidence, 0.0001f)
    }

    /**
     * Verifies that any unexpected exceptions thrown by the insight repository are
     * propagated correctly to the caller.
     *
     * This ensures that the use case does not silently swallow critical failures,
     * allowing the application to handle repository errors (e.g., database
     * connection issues) appropriately.
     */
    @Test
    fun `Repository exception handling`() {
        val user = testUser()
        coEvery { userRepo.getCurrentUser() } returns user
        coEvery { insightRepo.getUserInsights(user.id) } throws RuntimeException("Repository failed")

        assertThrows(RuntimeException::class.java) {
            runBlocking {
                useCase()
            }
        }
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

        fun daysAgo(days: Int): Long {
            return System.currentTimeMillis() - (days * DAY_MS)
        }
    }


}