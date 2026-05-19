package com.cue.domain.usecase

import com.cue.domain.model.ContextSnapshot
import com.cue.domain.model.InsightPatternSummary
import com.cue.domain.model.InsightType
import com.cue.domain.repository.ContextSnapShotRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * Unit tests for [GenerateTodayLikeHintUseCase].
 *
 * This test suite verifies the logic for generating contextual "Today-like" hints by comparing
 * the most recent [ContextSnapshot] with historical [InsightPatternSummary] data.
 *
 * Key behaviors tested include:
 * - Handling of empty repositories and empty pattern summaries.
 * - Filtering of insights based on confidence score thresholds.
 * - Logic for selecting the most recent snapshot for comparison.
 * - Validation of matching logic for various [InsightType]s (Sleep, Phone Usage, Weather, Connectivity).
 * - Selection priority and tie-breaking when multiple patterns match the current context.
 * - Graceful handling of repository and internal use case exceptions.
 * - Validation of the final generated hint message and data integrity.
 */
class GenerateTodayLikeHintUseCaseTest {

    private val contextRepo = mockk<ContextSnapShotRepository>()
    private val patternSummaryUseCase = mockk<GenerateInsightSummaryUseCase>()
    private val useCase = GenerateTodayLikeHintUseCase(
        contextRepo = contextRepo,
        patternSummaryUseCase = patternSummaryUseCase
    )

    @Test
    fun `Empty repository snapshot handling`() = runBlocking {
        coEvery { contextRepo.getAllSnapshots() } returns emptyList()
        coEvery { patternSummaryUseCase() } returns listOf(summary(InsightType.PHONE_USAGE))

        val result = useCase()

        assertNull(result)
    }

    @Test
    fun `Empty pattern summary handling`() = runBlocking {
        coEvery { contextRepo.getAllSnapshots() } returns listOf(snapshot(phoneUsage = "High"))
        coEvery { patternSummaryUseCase() } returns emptyList()

        val result = useCase()

        assertNull(result)
    }

    @Test
    fun `Confidence threshold filtering`() = runBlocking {
        coEvery { contextRepo.getAllSnapshots() } returns listOf(snapshot(phoneUsage = "High"))
        coEvery { patternSummaryUseCase() } returns listOf(
            summary(InsightType.PHONE_USAGE, averageConfidence = 0.69f)
        )

        val result = useCase()

        assertNull(result)
    }

    @Test
    fun `Latest snapshot selection logic`() = runBlocking {
        val olderMatchingSnapshot = snapshot(timestamp = 1_000L, phoneUsage = "High")
        val latestNonMatchingSnapshot = snapshot(timestamp = 2_000L, phoneUsage = "Low")
        coEvery { contextRepo.getAllSnapshots() } returns listOf(
            olderMatchingSnapshot,
            latestNonMatchingSnapshot
        )
        coEvery { patternSummaryUseCase() } returns listOf(summary(InsightType.PHONE_USAGE))

        val result = useCase()

        assertNull(result)
    }

    @Test
    fun `No matching signal for any pattern`() = runBlocking {
        coEvery { contextRepo.getAllSnapshots() } returns listOf(
            snapshot(phoneUsage = "Low", sleep = 8, weather = "Sunny", studyLocation = "Library")
        )
        coEvery { patternSummaryUseCase() } returns listOf(
            summary(InsightType.PHONE_USAGE),
            summary(InsightType.SLEEP),
            summary(InsightType.WEATHER),
            summary(InsightType.CONNECTIVITY)
        )

        val result = useCase()

        assertNull(result)
    }

    @Test
    fun `Sleep insight type positive match`() = runBlocking {
        coEvery { contextRepo.getAllSnapshots() } returns listOf(snapshot(sleep = 4))
        coEvery { patternSummaryUseCase() } returns listOf(summary(InsightType.SLEEP))

        val result = useCase()

        assertNotNull(result)
        assertEquals(InsightType.SLEEP, result?.insightType)
    }

    @Test
    fun `Sleep insight type boundary low`() = runBlocking {
        coEvery { contextRepo.getAllSnapshots() } returns listOf(snapshot(sleep = 0))
        coEvery { patternSummaryUseCase() } returns listOf(summary(InsightType.SLEEP))

        val result = useCase()

        assertNull(result)
    }

    @Test
    fun `Sleep insight type boundary high`() = runBlocking {
        coEvery { contextRepo.getAllSnapshots() } returns listOf(snapshot(sleep = 5))
        coEvery { patternSummaryUseCase() } returns listOf(summary(InsightType.SLEEP))

        val result = useCase()

        assertNull(result)
    }

    @Test
    fun `Phone usage positive match`() = runBlocking {
        coEvery { contextRepo.getAllSnapshots() } returns listOf(snapshot(phoneUsage = "High"))
        coEvery { patternSummaryUseCase() } returns listOf(summary(InsightType.PHONE_USAGE))

        val result = useCase()

        assertNotNull(result)
        assertEquals(InsightType.PHONE_USAGE, result?.insightType)
    }

    @Test
    fun `Phone usage mismatch`() = runBlocking {
        coEvery { contextRepo.getAllSnapshots() } returns listOf(snapshot(phoneUsage = "Medium"))
        coEvery { patternSummaryUseCase() } returns listOf(summary(InsightType.PHONE_USAGE))

        val result = useCase()

        assertNull(result)
    }

    @Test
    fun `Weather positive match`() = runBlocking {
        coEvery { contextRepo.getAllSnapshots() } returns listOf(snapshot(weather = "Rainy"))
        coEvery { patternSummaryUseCase() } returns listOf(summary(InsightType.WEATHER))

        val result = useCase()

        assertNotNull(result)
        assertEquals(InsightType.WEATHER, result?.insightType)
    }

    @Test
    fun `Connectivity positive match`() = runBlocking {
        coEvery { contextRepo.getAllSnapshots() } returns listOf(snapshot(connectivity = "None"))
        coEvery { patternSummaryUseCase() } returns listOf(summary(InsightType.CONNECTIVITY))

        val result = useCase()

        assertNotNull(result)
        assertEquals(InsightType.CONNECTIVITY, result?.insightType)
    }

    @Test
    fun `Multiple matching patterns selection`() = runBlocking {
        coEvery { contextRepo.getAllSnapshots() } returns listOf(
            snapshot(phoneUsage = "High", sleep = 3)
        )
        coEvery { patternSummaryUseCase() } returns listOf(
            summary(InsightType.PHONE_USAGE, averageConfidence = 0.72f),
            summary(InsightType.SLEEP, averageConfidence = 0.91f)
        )

        val result = useCase()

        assertNotNull(result)
        assertEquals(InsightType.SLEEP, result?.insightType)
        assertEquals(0.91f, result?.confidence ?: 0f, 0.0001f)
    }

    @Test
    fun `Tie breaking on confidence using timestamp`() = runBlocking {
        val latestSnapshotTimestamp = 4_000L
        coEvery { contextRepo.getAllSnapshots() } returns listOf(
            snapshot(timestamp = latestSnapshotTimestamp, phoneUsage = "High", sleep = 3)
        )
        coEvery { patternSummaryUseCase() } returns listOf(
            summary(InsightType.PHONE_USAGE, averageConfidence = 0.8f),
            summary(InsightType.SLEEP, averageConfidence = 0.8f)
        )

        val result = useCase()

        assertNotNull(result)
        assertEquals(0.8f, result?.confidence ?: 0f, 0.0001f)
        assertEquals(latestSnapshotTimestamp, result?.matchedSnapshotTimeStamp)
    }

    @Test
    fun `Repository exception handling`() {
        coEvery { contextRepo.getAllSnapshots() } throws RuntimeException("Snapshot repository failed")

        assertThrows(RuntimeException::class.java) {
            runBlocking {
                useCase()
            }
        }
    }

    @Test
    fun `Summary use case exception handling`() {
        coEvery { contextRepo.getAllSnapshots() } returns listOf(snapshot(phoneUsage = "High"))
        coEvery { patternSummaryUseCase() } throws RuntimeException("Summary use case failed")

        assertThrows(RuntimeException::class.java) {
            runBlocking {
                useCase()
            }
        }
    }

    @Test
    fun `Hint message content validation`() = runBlocking {
        coEvery { contextRepo.getAllSnapshots() } returns listOf(snapshot(phoneUsage = "High"))
        coEvery { patternSummaryUseCase() } returns listOf(summary(InsightType.PHONE_USAGE))

        val result = useCase()

        assertEquals(
            "On days like today with high phone usage, you tend to miss/delay a study sessions.",
            result?.message
        )
    }

    @Test
    fun `Data integrity check`() = runBlocking {
        val timestamp = 9_000L
        val patternMessage = "Stored phone usage pattern"
        coEvery { contextRepo.getAllSnapshots() } returns listOf(
            snapshot(timestamp = timestamp, phoneUsage = "High")
        )
        coEvery { patternSummaryUseCase() } returns listOf(
            summary(
                insightType = InsightType.PHONE_USAGE,
                message = patternMessage,
                averageConfidence = 0.84f
            )
        )

        val result = useCase()

        assertNotNull(result)
        assertEquals(InsightType.PHONE_USAGE, result?.insightType)
        assertEquals(patternMessage, result?.matchedPatternMessage)
        assertEquals(0.84f, result?.confidence ?: 0f, 0.0001f)
        assertEquals(timestamp, result?.matchedSnapshotTimeStamp)
    }

    private fun summary(
        insightType: InsightType,
        message: String = "Pattern for $insightType",
        averageConfidence: Float = 0.8f,
        recurrenceCount: Int = 2,
        latestTimestamp: Long = 1_000L
    ): InsightPatternSummary {
        return InsightPatternSummary(
            insightType = insightType,
            message = message,
            averageConfidence = averageConfidence,
            recurrenceCount = recurrenceCount,
            latestTimestamp = latestTimestamp
        )
    }

    private fun snapshot(
        id: Long = 0L,
        sessionId: Long? = null,
        studyLocation: String = "Library",
        phoneUsage: String = "Low",
        connectivity: String = "WiFi",
        sleep: Int = 8,
        weather: String = "Sunny",
        confidenceScore: Float = 0.9f,
        timestamp: Long = 1_000L
    ): ContextSnapshot {
        return ContextSnapshot(
            id = id,
            sessionId = sessionId,
            studyLocation = studyLocation,
            phoneUsage = phoneUsage,
            connectivity = connectivity,
            sleep = sleep,
            weather = weather,
            confidenceScore = confidenceScore,
            timestamp = timestamp
        )
    }
}
