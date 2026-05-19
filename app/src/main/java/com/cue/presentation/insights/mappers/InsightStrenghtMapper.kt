package com.cue.presentation.insights.model

fun Float.toInsightsStrength() : SignalStrength{
    return if (this >= 0.7f) {
        SignalStrength.STRONG
    } else if (this >= 0.6f) {
        SignalStrength.MODERATE
    } else {
        SignalStrength.EMERGING
    }
}