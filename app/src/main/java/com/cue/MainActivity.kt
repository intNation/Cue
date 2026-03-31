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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.cue.data.context.MockContextEngine
import com.cue.data.local.CueDatabase
import com.cue.data.repository.ContextSnapShotRepositoryImpl
import com.cue.data.repository.DailyCheckinRepositoryImpl
import com.cue.data.repository.StudySessionRepositoryImpl
import com.cue.domain.usecase.EndSessionUseCase
import com.cue.domain.usecase.GetActiveSessionUseCase
import com.cue.domain.usecase.StartSessionUseCase
import com.cue.domain.usecase.SubmitDailyCheckInUseCase
import com.cue.presentation.main.MainViewModel
import com.cue.presentation.theme.CueTheme

class MainActivity : ComponentActivity() {

    private val db by lazy {
        Room.databaseBuilder(applicationContext, CueDatabase::class.java, "cue.db")
            .build()
    }

    private val viewModel: MainViewModel by viewModels { //by keyword delegates the management of the mainviewmodel to the function viewModels()
        object : ViewModelProvider.Factory {

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
                    SubmitDailyCheckInUseCase(checkInRepo)
                ) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CueTheme {
                val uiState by viewModel.uiState.collectAsState()
                
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(
                        modifier = Modifier.padding(innerPadding),
                        activeSessionId = uiState.activeSession?.id,
                        onToggleSession = { viewModel.toggleSession() },
                        onCheckIn = { didStudy -> viewModel.submitCheckIn(didStudy) }
                    )
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
