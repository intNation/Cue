package com.cue.core.constants

import kotlin.time.Duration.Companion.hours

/**
 * Object containing session-related constants.
 * Contains the maximum duration of a study session in milliseconds.
 * @property MAX_SESSION_DURATION_MS The maximum duration of a study session in milliseconds.
 */
object SessionConstants {
    val MAX_SESSION_DURATION_MS = 12.hours.inWholeMilliseconds
}
