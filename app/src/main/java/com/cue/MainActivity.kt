package com.cue

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.cue.data.local.CueDatabase
import com.cue.data.local.entity.StudySessionEntity
import com.cue.presentation.theme.CueTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = Room.databaseBuilder(applicationContext, CueDatabase::class.java, "cue.db")
            .build()

        val sessionDao = db.studySessionDao()



        enableEdgeToEdge()

        lifecycleScope.launch{
            val id  = sessionDao.insertSession(
                 StudySessionEntity(
                        startTime = System.currentTimeMillis(),
                        endTime = null,
                        endType = "Auto"
                )
            )
            Log.d("TEST_DB", "Created session with ID: $id")
        }

        setContent {
            CueTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Text(text = "Cue is running!", modifier = Modifier.padding(innerPadding))

                }
            }
        }
    }
}