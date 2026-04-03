package com.cue.data.context

import com.cue.domain.model.ContextSnapshot
import com.cue.domain.repository.ContextEngine

/**
 * Mock implementation of [ContextEngine] for V1.
 * Returns randomized dummy data to simulate signal collection.
 */
class MockContextEngine : ContextEngine {
    override suspend fun captureSnapshot(sessionId: Long): ContextSnapshot {
        val phoneUsages = listOf("High", "Medium", "Low")
        val connectivities = listOf("WiFi", "Cellular", "None")
        val weathers = listOf("Sunny", "Rainy", "Cloudy")
        val sleep = listOf(6,9,4)

        return ContextSnapshot(
            sessionId = sessionId,
            phoneUsage = phoneUsages.random(),
            connectivity = connectivities.random(),
            weather = weathers.random(),
            sleep = sleep.random(), // Mocked
            confidenceScore = 0.85f,// Static confidence for V1,
            timestamp = System.currentTimeMillis(),

        )
    }
}
