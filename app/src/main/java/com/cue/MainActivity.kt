package com.cue

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.cue.core.util.ScheduleManager
import com.cue.data.context.MockContextEngine
import com.cue.data.local.CueDatabase
import com.cue.data.repository.ContextSnapShotRepositoryImpl
import com.cue.data.repository.DailyCheckinRepositoryImpl
import com.cue.data.repository.InsightRepositoryImpl
import com.cue.data.repository.StudySessionRepositoryImpl
import com.cue.data.repository.UserRepositoryImpl
import com.cue.domain.usecase.CleanupStaleSessionsUseCase
import com.cue.domain.usecase.EndSessionUseCase
import com.cue.domain.usecase.GenerateInsightsUseCase
import com.cue.domain.usecase.GetActiveSessionUseCase
import com.cue.domain.usecase.SaveUserOnboardingUseCase
import com.cue.domain.usecase.StartSessionUseCase
import com.cue.domain.usecase.SubmitDailyCheckInUseCase
import com.cue.presentation.insights.InsightsScreen
import com.cue.presentation.insights.InsightsViewModel
import com.cue.presentation.main.MainViewModel
import com.cue.presentation.onboarding.OnboardingViewModel
import com.cue.presentation.onboarding.screens.PermissionsScreen
import com.cue.presentation.onboarding.screens.StudyLocationScreen
import com.cue.presentation.onboarding.screens.StudyScheduleScreen
import com.cue.presentation.onboarding.screens.SuccessMetricScreen
import com.cue.presentation.theme.CueTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val db by lazy { (application as CueApplication).database }

    private val viewModel: MainViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val sessionDao = db.studySessionDao()
                val snapshotDao = db.contextSnapshotDao()
                val checkInDao = db.dailyCheckInDao()

                val sessionRepo = StudySessionRepositoryImpl(sessionDao)
                val snapshotRepo = ContextSnapShotRepositoryImpl(snapshotDao)
                val checkInRepo = DailyCheckinRepositoryImpl(checkInDao)
                val contextEngine = MockContextEngine()

                return MainViewModel(
                    StartSessionUseCase(sessionRepo, contextEngine, snapshotRepo),
                    EndSessionUseCase(sessionRepo),
                    GetActiveSessionUseCase(sessionRepo),
                    CleanupStaleSessionsUseCase(sessionRepo),
                    SubmitDailyCheckInUseCase(checkInRepo)
                ) as T
            }
        }
    }

    private val onboardingViewModel: OnboardingViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val userRepo = UserRepositoryImpl(db, db.userDao())
                return OnboardingViewModel(
                    application,
                    SaveUserOnboardingUseCase(userRepo)
                ) as T
            }
        }
    }

    private val insightsViewModel: InsightsViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val userRepo = UserRepositoryImpl(db, db.userDao())
                val insightRepo = InsightRepositoryImpl(db.insightDao())
                val sessionRepo = StudySessionRepositoryImpl(db.studySessionDao())
                val checkinRepo = DailyCheckinRepositoryImpl(db.dailyCheckInDao())
                val snapshotRepo = ContextSnapShotRepositoryImpl(db.contextSnapshotDao())
                
                return InsightsViewModel(
                    userRepo,
                    insightRepo,
                    GenerateInsightsUseCase(userRepo, sessionRepo, checkinRepo, snapshotRepo, insightRepo)
                ) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Ensure background polling is active if onboarding is done
        lifecycleScope.launch {
            val userRepo = UserRepositoryImpl(db, db.userDao())
            userRepo.getCurrentUser()?.let { user ->
                if (user.isOnboardingCompleted) {
                    ScheduleManager(applicationContext).updateSchedule(user.weeklySchedule)
                }
            }
        }

        enableEdgeToEdge()
        setContent {
            CueTheme {
                val uiState by viewModel.uiState.collectAsState()
                val onboardingState by onboardingViewModel.uiState.collectAsState()
                val insightsState by insightsViewModel.uiState.collectAsState()
                
                var forceOnboarding by remember { mutableStateOf(true) }
                var currentTab by remember { mutableStateOf(0) } // 0 = Home, 1 = Insights

                if (forceOnboarding && !onboardingState.onboardingCompleted) {
                    when (onboardingState.currentStep) {
                        1 -> StudyLocationScreen(
                            selectedLocations = onboardingState.selectedLocations,
                            onLocationToggle = { onboardingViewModel.onLocationToggle(it) },
                            onContinue = { onboardingViewModel.nextStep() }
                        )
                        2 -> StudyScheduleScreen(
                            weeklySchedule = onboardingState.weeklySchedule,
                            onScheduleChange = { onboardingViewModel.onScheduleChange(it) },
                            onContinue = { onboardingViewModel.nextStep() },
                            onBack = { onboardingViewModel.previousStep() }
                        )
                        3 -> SuccessMetricScreen(
                            selectedMetric = onboardingState.successMetric,
                            onMetricSelect = { onboardingViewModel.onSuccessMetricSelect(it) },
                            onComplete = { onboardingViewModel.nextStep() },
                            onBack = { onboardingViewModel.previousStep() }
                        )
                        4 -> PermissionsScreen(
                            locationEnabled = onboardingState.locationEnabled,
                            calendarEnabled = onboardingState.calendarEnabled,
                            sleepEnabled = onboardingState.sleepEnabled,
                            movementEnabled = onboardingState.movementEnabled,
                            onLocationToggle = { onboardingViewModel.toggleLocationPermission(it) },
                            onCalendarToggle = { onboardingViewModel.toggleCalendarPermission(it) },
                            onSleepToggle = { onboardingViewModel.toggleSleepPermission(it) },
                            onMovementToggle = { onboardingViewModel.toggleMovementPermission(it) },
                            onComplete = { onboardingViewModel.completeOnboarding() },
                            onCustomizeLater = { onboardingViewModel.completeOnboarding() }
                        )
                    }
                } else {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        bottomBar = {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                contentColor = MaterialTheme.colorScheme.primary
                            ) {
                                NavigationBarItem(
                                    selected = currentTab == 0,
                                    onClick = { currentTab = 0 },
                                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                                    label = { Text("Focus") }
                                )
                                NavigationBarItem(
                                    selected = currentTab == 1,
                                    onClick = { 
                                        currentTab = 1
                                        insightsViewModel.loadInsights() 
                                    },
                                    icon = { Icon(Icons.Default.Info, contentDescription = null) },
                                    label = { Text("Insights") }
                                )
                            }
                        }
                    ) { innerPadding ->
                        if (currentTab == 0) {
                            MainScreen(
                                modifier = Modifier.padding(innerPadding),
                                activeSessionId = uiState.activeSession?.id,
                                onToggleSession = { viewModel.toggleSession() },
                                onCheckIn = { didStudy -> viewModel.submitCheckIn(didStudy) }
                            )
                        } else {
                            InsightsScreen(
                                uiState = insightsState,
                                onRefresh = { insightsViewModel.loadInsights() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    activeSessionId: Long?,
    onToggleSession: () -> Unit,
    onCheckIn: (Boolean) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Cue",
            style = MaterialTheme.typography.headlineLarge
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (activeSessionId != null) "Session Active (ID: $activeSessionId)" else "No Active Session",
                    style = MaterialTheme.typography.bodyLarge
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(onClick = onToggleSession) {
                    Text(text = if (activeSessionId != null) "End Session" else "Start Session")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(text = "Did you study today?", style = MaterialTheme.typography.titleMedium)
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = { onCheckIn(true) }) {
                Text("Yes")
            }
            Button(onClick = { onCheckIn(false) }) {
                Text("No")
            }
        }
    }
}
