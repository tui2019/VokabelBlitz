package de.vokabelblitz

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import de.vokabelblitz.theme.VokabelBlitzTheme

class MainActivity : ComponentActivity() {
    private val startQuizState = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        startQuizState.value = intent?.getStringExtra("action") == "start_quiz"

        enableEdgeToEdge()
        setContent {
            VokabelBlitzTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainNavigation(
                        startQuizImmediately = startQuizState.value,
                        onQuizStarted = { startQuizState.value = false }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val action = intent.getStringExtra("action")
        if (action == "start_quiz") {
            startQuizState.value = true
        }
    }
}
