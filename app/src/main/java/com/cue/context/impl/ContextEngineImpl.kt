package com.cue.context.impl

import com.cue.context.contracts.ConnectivitySignal
import com.cue.context.contracts.PhoneUsageLevelProvider
import com.cue.context.contracts.PhoneUsageLevelSignal
import com.cue.context.contracts.ProviderResult
import com.cue.context.contracts.StudyLocationProvider
import com.cue.context.contracts.StudyLocationSignal
import com.cue.context.contracts.WhetherProvider
import com.cue.context.contracts.WhetherSignal
import com.cue.context.contracts.ConnectivityProvider
import com.cue.domain.model.ContextSnapshot
import com.cue.domain.repository.ContextEngine
import com.cue.domain.repository.UserRepository

/**
 * Production implementation of [ContextEngine] that orchestrates multiple signal providers.
 */
class ContextEngineImpl(
    private val userRepository: UserRepository,
    private val phoneProvider: PhoneUsageLevelProvider,
    private val connectivityProvider: ConnectivityProvider,
    private val locationProvider: StudyLocationProvider,
    private val weatherProvider: WhetherProvider
) : ContextEngine {

    override suspend fun captureSnapshot(sessionId: Long?): ContextSnapshot {
        val user = userRepository.getCurrentUser()
        val studyPlaces = user?.studyPlaces ?: emptyList()

        // 1. Collect all signals in parallel (or sequential for simplicity in V3)
        val phoneResult = phoneProvider.getPhoneUsageLevel()
        val connectivityResult = connectivityProvider.getConnectivitySignal()
        val studyLocationResult = locationProvider.getStudyLocationSignal(studyPlaces)
        
        // Weather depends on location
        val weatherLocationResult = locationProvider.getWhetherLocation()
        val weatherResult = if (weatherLocationResult is ProviderResult.Available) {
            weatherProvider.getWhetherSignal(weatherLocationResult.data)
        } else {
            ProviderResult.Unavailable((weatherLocationResult as? ProviderResult.Unavailable)?.reason 
                ?: com.cue.context.contracts.UnavailableReason.DATA_NOT_AVAILABLE)
        }

        // 2. Normalize results for the domain model
        val phoneUsage = when (phoneResult) {
            is ProviderResult.Available -> phoneResult.data.name
            is ProviderResult.Unavailable -> "UNKNOWN"
        }

        val connectivity = when (connectivityResult) {
            is ProviderResult.Available -> connectivityResult.data.name
            is ProviderResult.Unavailable -> "UNKNOWN"
        }

        val studyLocation = when (studyLocationResult) {
            is ProviderResult.Available -> studyLocationResult.data.name
            is ProviderResult.Unavailable -> "UNKNOWN"
        }

        val weather = when (weatherResult) {
            is ProviderResult.Available -> weatherResult.data.name
            is ProviderResult.Unavailable -> "UNKNOWN"
        }

        // 3. Calculate Confidence Score
        val availableSignals = listOf(
            phoneResult,
            connectivityResult,
            studyLocationResult,
            weatherResult
        ).count { it is ProviderResult.Available }
        
        val confidenceScore = availableSignals.toFloat() / 4.0f

        return ContextSnapshot(
            sessionId = sessionId,
            studyLocation = studyLocation,
            phoneUsage = phoneUsage,
            connectivity = connectivity,
            weather = weather,
            sleep = 0, // Sleep API integration deferred to later V3/V4
            confidenceScore = confidenceScore,
            timestamp = System.currentTimeMillis()
        )
    }
}
