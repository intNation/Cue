package com.cue.domain.model

/**
 * Data class representing a context snapshot.
 * @param id The unique identifier of the context snapshot.
 * @param sessionId The ID of the associated study session.
 * @param studyLocation The study location status ("AT_USUAL_LOCATION", "AWAY_FROM_USUAL_LOCATION", "UNKNOWN").
 * @param phoneUsage The usage of the phone ("High", "Medium", "Low").
 * @param connectivity The connectivity status of the device ("WiFi", "Cellular", "None").
 * @param sleep The amount of sleep (hours).
 * @param weather The weather condition ("Sunny", "Rainy", "Cloudy").
 * @param confidenceScore The confidence score of the snapshot.
 * @param timestamp The timestamp when the snapshot was captured.
 */
data class ContextSnapshot(
    val id: Long = 0,
    val sessionId: Long?,
    val studyLocation: String,
    val phoneUsage: String,
    val connectivity: String,
    val sleep: Int,
    val weather: String,
    val confidenceScore: Float,
    val timestamp: Long
)
