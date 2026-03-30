package com.cue.domain.model

data class DailyCheckIn(
    val id: Long = 0,
    val timestamp: Long,
    val didStudy: Boolean
)
