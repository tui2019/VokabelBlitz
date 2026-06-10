package de.vokabelblitz.ui.quiz

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.ui.res.painterResource
import de.vokabelblitz.R
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.vokabelblitz.ui.WordViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    viewModel: WordViewModel,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val quizState by viewModel.quizState.collectAsStateWithLifecycle()

    androidx.compose.runtime.LaunchedEffect(quizState) {
        android.util.Log.d("QuizScreen", "quizState updated: words=${quizState.words.size}, index=${quizState.currentIndex}, isFinished=${quizState.isFinished}")
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    if (!quizState.isFinished) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${quizState.currentIndex + 1} / ${quizState.totalWords}", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = if (quizState.isReversed) "Englisch ➔ Deutsch" else "Deutsch ➔ Englisch",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Text("Ergebnisse")
                    }
                },
                navigationIcon = {
                    androidx.compose.material3.FilledTonalIconButton(
                        onClick = {
                            onExit()
                        },
                        modifier = Modifier.padding(start = 8.dp),
                        colors = androidx.compose.material3.IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
                actions = {
                    if (!quizState.isFinished) {
                        IconButton(onClick = { viewModel.toggleQuizLanguage() }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_swap_languages),
                                contentDescription = "Sprachrichtung umkehren",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (quizState.isFinished) {
                // Results screen
                QuizResults(
                    knownCount = quizState.knownCount,
                    learningCount = quizState.learningCount,
                    totalWords = quizState.totalWords,
                    onRestart = { viewModel.startQuiz() },
                    onExit = onExit
                )
            } else if (quizState.words.isEmpty()) {
                // Premium Centered Loading Screen
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 4.dp
                    )
                }
            } else {
                val currentWord = quizState.currentWord ?: return@Scaffold

                // Progress bar
                val animatedProgress by animateFloatAsState(
                    targetValue = quizState.progress,
                    animationSpec = tween(300),
                    label = "progress"
                )
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(32.dp))

                // 3D Card Flip Animation State
                val rotation by animateFloatAsState(
                    targetValue = if (quizState.isRevealed) 180f else 0f,
                    animationSpec = tween(
                        durationMillis = 500,
                        easing = androidx.compose.animation.core.FastOutSlowInEasing
                    ),
                    label = "cardFlip"
                )

                // Track current and previous words to prevent translation peeking during flip-back
                var previousWord by remember { mutableStateOf(currentWord) }
                var lastWord by remember { mutableStateOf(currentWord) }

                if (quizState.currentIndex == 0) {
                    previousWord = currentWord
                    lastWord = currentWord
                } else if (currentWord != lastWord) {
                    previousWord = lastWord
                    lastWord = currentWord
                }

                val displayedBackWord = if (quizState.isRevealed) currentWord else previousWord

                // Flashcard
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (rotation > 90f)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .graphicsLayer {
                            rotationY = rotation
                            cameraDistance = 12f * density
                        }
                        .clickable(enabled = !quizState.isRevealed) {
                            viewModel.revealAnswer()
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                            .graphicsLayer {
                                if (rotation > 90f) {
                                    rotationY = 180f
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (rotation <= 90f) {
                            // Front: show word based on direction
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (quizState.isReversed) currentWord.englishTranslation else currentWord.germanWord,
                                    style = MaterialTheme.typography.displaySmall,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Tippen zum Aufdecken",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        } else {
                            // Back: show translation + example
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (quizState.isReversed) displayedBackWord.englishTranslation else displayedBackWord.germanWord,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "=",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (quizState.isReversed) displayedBackWord.germanWord else displayedBackWord.englishTranslation,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "\"${displayedBackWord.usageExample}\"",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontStyle = FontStyle.Italic,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Premium Button Morphing & Splitting Animation States - Fluidly Synchronized
                // Physical Split (gap, corners) and Background Colors all transition in parallel (t = 0 to 300ms)
                // Texts/icons fade in with a tiny delay (t = 100 to 300ms) to ensure text renders inside a split button
                val gapSpacing by animateDpAsState(
                    targetValue = if (quizState.isRevealed) 6.dp else 0.dp,
                    animationSpec = tween(
                        durationMillis = 300,
                        easing = androidx.compose.animation.core.FastOutSlowInEasing
                    ),
                    label = "gapSpacing"
                )

                // Corner radius of touching inner edges.
                // Unrevealed: 0.dp (completely flat touching seam).
                // Revealed: 24.dp (squircles separating).
                val innerCornerRadius by animateDpAsState(
                    targetValue = if (quizState.isRevealed) 24.dp else 0.dp,
                    animationSpec = tween(
                        durationMillis = 300,
                        easing = androidx.compose.animation.core.FastOutSlowInEasing
                    ),
                    label = "innerCornerRadius"
                )

                // Weights: Constant 1:2 split so the physical seam is pre-allocated at the 1/3 and 2/3 mark.
                // This makes the cell-split transition significantly smoother as no layout resizing or weight
                // recalculations occur—the button simply parts organically exactly where it stands.
                val leftWeight = 1f
                val rightWeight = 2f

                // Background & Content Colors for the Left Button
                val leftContainerColor by animateColorAsState(
                    targetValue = if (quizState.isRevealed)
                        MaterialTheme.colorScheme.onTertiary
                    else
                        MaterialTheme.colorScheme.secondaryContainer,
                    animationSpec = tween(durationMillis = 300),
                    label = "leftContainerColor"
                )
                val leftContentColor by animateColorAsState(
                    targetValue = if (quizState.isRevealed)
                        MaterialTheme.colorScheme.tertiary
                    else
                        MaterialTheme.colorScheme.onSecondaryContainer,
                    animationSpec = tween(durationMillis = 300),
                    label = "leftContentColor"
                )

                // Background & Content Colors for the Right Button
                val rightContainerColor by animateColorAsState(
                    targetValue = if (quizState.isRevealed)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.secondaryContainer,
                    animationSpec = tween(durationMillis = 300),
                    label = "rightContainerColor"
                )
                val rightContentColor by animateColorAsState(
                    targetValue = if (quizState.isRevealed)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onSecondaryContainer,
                    animationSpec = tween(durationMillis = 300),
                    label = "rightContentColor"
                )

                // Content Alpha for "Antwort anzeigen" Overlay
                val revealAlpha by animateFloatAsState(
                    targetValue = if (quizState.isRevealed) 0f else 1f,
                    animationSpec = tween(
                        durationMillis = 150,
                        delayMillis = if (quizState.isRevealed) 0 else 150
                    ),
                    label = "revealAlpha"
                )

                // Content Alphas for "Lerne noch" and "Kann ich!"
                val leftContentAlpha by animateFloatAsState(
                    targetValue = if (quizState.isRevealed) 1f else 0f,
                    animationSpec = tween(
                        durationMillis = 200,
                        delayMillis = if (quizState.isRevealed) 100 else 0
                    ),
                    label = "leftContentAlpha"
                )
                val rightContentAlpha by animateFloatAsState(
                    targetValue = if (quizState.isRevealed) 1f else 0f,
                    animationSpec = tween(
                        durationMillis = 200,
                        delayMillis = if (quizState.isRevealed) 100 else 0
                    ),
                    label = "rightContentAlpha"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(112.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(gapSpacing)
                    ) {
                        // Left Button (Lerne noch / Left half of Antwort anzeigen)
                        Button(
                            onClick = {
                                if (quizState.isRevealed) {
                                    viewModel.markLearning()
                                } else {
                                    viewModel.revealAnswer()
                                }
                            },
                            shape = RoundedCornerShape(
                                topStart = 24.dp,
                                bottomStart = 24.dp,
                                topEnd = innerCornerRadius,
                                bottomEnd = innerCornerRadius
                            ),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = leftContainerColor,
                                contentColor = leftContentColor
                            ),
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier
                                .weight(leftWeight)
                                .fillMaxHeight()
                        ) {
                            if (leftContentAlpha > 0.01f) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.graphicsLayer { alpha = leftContentAlpha }
                                ) {
                                    Icon(
                                        Icons.Default.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Lerne noch",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        // Right Button (Kann ich! / Right half of Antwort anzeigen)
                        Button(
                            onClick = {
                                if (quizState.isRevealed) {
                                    viewModel.markKnown()
                                } else {
                                    viewModel.revealAnswer()
                                }
                            },
                            shape = RoundedCornerShape(
                                topStart = innerCornerRadius,
                                bottomStart = innerCornerRadius,
                                topEnd = 24.dp,
                                bottomEnd = 24.dp
                            ),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = rightContainerColor,
                                contentColor = rightContentColor
                            ),
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier
                                .weight(rightWeight)
                                .fillMaxHeight()
                        ) {
                            if (rightContentAlpha > 0.01f) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.graphicsLayer { alpha = rightContentAlpha }
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Kann ich!",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    // Floating Centered Overlay: "Antwort anzeigen"
                    if (revealAlpha > 0.01f) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .graphicsLayer { alpha = revealAlpha }
                        ) {
                            Icon(
                                Icons.Default.Visibility,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Antwort anzeigen",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuizResults(
    knownCount: Int,
    learningCount: Int,
    totalWords: Int,
    onRestart: () -> Unit,
    onExit: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Quiz abgeschlossen!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(24.dp))

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Gewusst", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "$knownCount / $totalWords",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Lerne noch", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "$learningCount / $totalWords",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onRestart,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
        ) {
            Icon(
                Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Quiz wiederholen",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onExit,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                text = "Zurück zu Start",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
