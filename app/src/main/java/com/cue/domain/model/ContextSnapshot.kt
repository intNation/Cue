package com.cue.domain.model

data class ContextSnapshot(
    val id: Long = 0,
    val sessionId: Long,
    val phoneUsage: String, // Dummy: "High", "Medium", "Low"
    val connectivity: String, // Dummy: "WiFi", "Cellular", "None"
    val weather: String, // Dummy: "Sunny", "Rainy", "Cloudy"
    val confidenceScore: Float
)
