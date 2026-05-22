package com.example.vokabelblitz

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.vokabelblitz.ai.GeminiTranslator
import com.example.vokabelblitz.ai.ModelStatus
import com.example.vokabelblitz.data.AppDatabase
import com.example.vokabelblitz.data.Word
import com.example.vokabelblitz.theme.VokabelBlitzTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class QuickEntryActivity : ComponentActivity() {
    private lateinit var translator: GeminiTranslator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        translator = GeminiTranslator(application)

        setContent {
            VokabelBlitzTheme {
                QuickEntryScreen(
                    onDismiss = { finish() },
                    onSaveWord = { wordToTranslate, onStateChange ->
                        lifecycleScope.launch {
                            onStateChange(QuickEntryState.Translating)
                            val status = translator.initialize()
                            if (status is ModelStatus.Available) {
                                val result = translator.translate(wordToTranslate)
                                if (result != null) {
                                    val newWord = Word(
                                        germanWord = result.germanWord,
                                        englishTranslation = result.englishTranslation,
                                        usageExample = result.usageExample
                                    )
                                    AppDatabase.getDatabase(applicationContext).wordDao().insert(newWord)
                                    onStateChange(QuickEntryState.Success(newWord))
                                    delay(1000)
                                    finish()
                                } else {
                                    onStateChange(QuickEntryState.Error("Ungültiges Wort oder Übersetzungsfehler."))
                                }
                            } else {
                                onStateChange(QuickEntryState.Error("KI-Modell nicht verfügbar."))
                            }
                        }
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        translator.close()
    }
}

sealed class QuickEntryState {
    object Idle : QuickEntryState()
    object Translating : QuickEntryState()
    data class Success(val word: Word) : QuickEntryState()
    data class Error(val message: String) : QuickEntryState()
}

@Composable
fun QuickEntryScreen(
    onDismiss: () -> Unit,
    onSaveWord: (String, (QuickEntryState) -> Unit) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var state by remember { mutableStateOf<QuickEntryState>(QuickEntryState.Idle) }
    val focusRequester = remember { FocusRequester() }

    // Request focus and open keyboard immediately when UI is ready
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // Outer box that dismisses when tapped (representing transparent backdrop click)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        // Floating dialog card
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight()
                .clickable(enabled = false) {}, // Consume clicks inside the card
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Schnelleingabe",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Schließen")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                when (state) {
                    is QuickEntryState.Success -> {
                        val word = (state as QuickEntryState.Success).word
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Erfolgreich hinzugefügt!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "${word.germanWord} = ${word.englishTranslation}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    else -> {
                        // Input Area
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextField(
                                value = text,
                                onValueChange = { text = it },
                                placeholder = { Text("Wort eingeben…") },
                                singleLine = true,
                                shape = CircleShape,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    cursorColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                                    .focusRequester(focusRequester)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            val isTranslating = state is QuickEntryState.Translating

                            FilledIconButton(
                                onClick = {
                                    if (text.isNotBlank()) {
                                        onSaveWord(text) { state = it }
                                    }
                                },
                                enabled = text.isNotBlank() && !isTranslating,
                                shape = CircleShape,
                                modifier = Modifier.size(56.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                if (isTranslating) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        Icons.AutoMirrored.Rounded.Send,
                                        contentDescription = "Senden",
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        // Error State
                        if (state is QuickEntryState.Error) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = (state as QuickEntryState.Error).message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Start
                            )
                        }
                    }
                }
            }
        }
    }
}
