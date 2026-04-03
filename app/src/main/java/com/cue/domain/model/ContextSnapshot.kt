package com.cue.domain.model

/**
 * Data class representing a context snapshot.
 * @param id The unique identifier of the context snapshot.
 * @param sessionId The ID of the associated study session.
 * @param phoneUsage The usage of the phone.
 * @param connectivity The connectivity status of the device.
 * @param weather The weather condition.
 * @param confidenceScore The confidence score of the snapshot.
 * @constructor Creates a new ContextSnapshot object.
 */
data class ContextSnapshot(
    val id: Long = 0,
    val sessionId: Long?, //we do not need a session to capture context snapshot
    val phoneUsage: String, // Dummy: "High", "Medium", "Low"
    val connectivity: String, // Dummy: "WiFi", "Cellular", "None"
    val sleep: Int, // Mocked
    val weather: String, // Dummy: "Sunny", "Rainy", "Cloudy"
    val confidenceScore: Float,
    //added a timestamp to track when signals were collected
    val timestamp: Long
)
