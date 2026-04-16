package com.cue.domain.usecase

import com.cue.domain.model.Insight
import com.cue.domain.model.InsightType
import com.cue.domain.repository.ContextSnapShotRepository
import com.cue.domain.repository.DailyCheckinRepository
import com.cue.domain.repository.InsightRepository
import com.cue.domain.repository.StudySessionRepository
import com.cue.domain.repository.UserRepository
import java.util.Calendar
import kotlin.math.abs

class GenerateInsightsUseCase(
    private val userRepository: UserRepository,
    private val sessionRepository: StudySessionRepository,
    private val checkinRepository: DailyCheckinRepository,
    private val snapshotRepository: ContextSnapShotRepository,
    private val insightRepository: InsightRepository
) {
    suspend operator fun invoke() {
        val user = userRepository.getCurrentUser() ?: return
        val checkins = checkinRepository.getAllCheckIns()
        val snapshots = snapshotRepository.getAllSnapshots()
        
        // Rule 1: High Phone Usage before Failure
        val negativeCheckins = checkins.filter { !it.didStudy }
        
        negativeCheckins.forEach { checkin ->
            val closestSnapshot = snapshots.minByOrNull { abs(it.timestamp - checkin.timestamp) }
            applyRules(user.id, closestSnapshot)
        }
        
        // Silent Failure Detection
        val sessions = sessionRepository.getAllSessions()
        detectSilentFailures(user.id, user.weeklySchedule, sessions, snapshots)
    }

    private suspend fun detectSilentFailures(
        userId: Long,
        schedule: List<com.cue.domain.model.DaySchedule>,
        sessions: List<com.cue.domain.model.StudySession>,
        snapshots: List<com.cue.domain.model.ContextSnapshot>
    ) {
        // Look at the last 7 days
        val now = Calendar.getInstance()
        for (i in 0 until 7) {
            val date = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
            val dayOfWeek = when(date.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> 1
                Calendar.TUESDAY -> 2
                Calendar.WEDNESDAY -> 3
                Calendar.THURSDAY -> 4
                Calendar.FRIDAY -> 5
                Calendar.SATURDAY -> 6
                Calendar.SUNDAY -> 7
                else -> 1
            }

            val scheduledDay = schedule.find { it.dayOfWeek == dayOfWeek } ?: continue
            
            // If we had a scheduled session but no study session was started that day
            val sessionsToday = sessions.filter { 
                val sessionDate = Calendar.getInstance().apply { timeInMillis = it.startTime }
                sessionDate.get(Calendar.DAY_OF_YEAR) == date.get(Calendar.DAY_OF_YEAR)
            }

            if (sessionsToday.isEmpty()) {
                // Find ghost snapshot for this day
                val ghostSnapshot = snapshots.find {
                    val snapDate = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                    snapDate.get(Calendar.DAY_OF_YEAR) == date.get(Calendar.DAY_OF_YEAR) && it.sessionId == null
                }
                
                ghostSnapshot?.let {
                    applyRules(userId, it)
                }
            }
        }
    }

    private suspend fun applyRules(userId: Long, snapshot: com.cue.domain.model.ContextSnapshot?) {
        if (snapshot == null) return

        if (snapshot.phoneUsage == "High") {
            createUniqueInsight(
                userId,
                "you tend to miss study sessions after high phone usage before study.",
                InsightType.PHONE_USAGE
            )
        }
        
        if (snapshot.sleep < 6) {
            createUniqueInsight(
                userId,
                "you tend to miss study sessions after poor sleep before study.",
                InsightType.SLEEP
            )
        }

        if (snapshot.connectivity == "None") {
            createUniqueInsight(
                userId,
                "Lack of internet connectivity often leads to study delays.",
                InsightType.CONNECTIVITY
            )
        }
    }

    private suspend fun createUniqueInsight(userId: Long, message: String, type: InsightType) {
        val existing = insightRepository.getUserInsights(userId)
        if (existing!!.none { it.type == type }) {
            insightRepository.insertInsight(
                Insight(
                    userId = userId,
                    message = message,
                    type = type,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }
}
