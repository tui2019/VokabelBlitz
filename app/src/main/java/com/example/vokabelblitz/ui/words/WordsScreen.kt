package com.example.vokabelblitz.ui.words

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.ui.layout.layout
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.animation.core.tween
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.abs
import com.example.vokabelblitz.R
import com.example.vokabelblitz.data.Word
import com.example.vokabelblitz.ui.WordViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
fun WordsScreen(
    viewModel: WordViewModel,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 0.dp
) {
    val words by viewModel.allWords.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var currentSnackbarJob by remember { mutableStateOf<Job?>(null) }
    val restoredGenerations = remember { mutableStateMapOf<Int, Int>() }

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = bottomPadding + 8.dp)
            )
        },
        containerColor = Color.Transparent,
        modifier = modifier.fillMaxSize()
    ) { localPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(localPadding)
        ) {
            // Header (Premium M3E Roboto Flex Typography to match VokabelBlitz)
            val RobotoFlexFamily = FontFamily(
                Font(R.font.robotoflex, FontWeight.W900)
            )
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                        append("Meine")
                    }
                    withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.tertiary)) {
                        append("Wörter")
                    }
                },
                style = MaterialTheme.typography.displayMedium.copy(
                    fontFamily = RobotoFlexFamily,
                    fontWeight = FontWeight.W900,
                    textGeometricTransform = TextGeometricTransform(scaleX = 1.4f)
                ),
                letterSpacing = (-3).sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${words.size} Wörter in deinem Wortschatz",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                textAlign = TextAlign.Center
            )

            if (words.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = bottomPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Outlined.LibraryBooks,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Noch keine Wörter",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Gehe zum Reiter 'Lernen', um dein erstes Wort hinzuzufügen!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = PaddingValues(bottom = bottomPadding + 16.dp)
                ) {
                    items(
                        items = words,
                        key = { it.id }
                    ) { word ->
                        key(word.id, restoredGenerations[word.id] ?: 0) {
                            WordCard(
                                word = word,
                                onDelete = {
                                    viewModel.deleteWord(word)
                                    currentSnackbarJob?.cancel()
                                    currentSnackbarJob = scope.launch {
                                        snackbarHostState.currentSnackbarData?.dismiss()
                                        val result = snackbarHostState.showSnackbar(
                                            message = "Wort '${word.germanWord}' gelöscht",
                                            actionLabel = "Rückgängig",
                                            duration = SnackbarDuration.Short
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            restoredGenerations[word.id] = (restoredGenerations[word.id] ?: 0) + 1
                                            viewModel.restoreWord(word)
                                        }
                                    }
                                },
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WordCard(
    word: Word,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { totalDistance -> totalDistance * 0.75f }
    )

    // Track whether the user's finger is currently touching the card
    var isTouching by remember { mutableStateOf(false) }
    // Track whether we are in the vertical collapse phase
    var isShrinking by remember { mutableStateOf(false) }

    // Start vertical shrink after the user releases their finger AND the swipe settled at a dismissed state
    LaunchedEffect(dismissState.currentValue, isTouching) {
        if (!isTouching &&
            (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart ||
             dismissState.currentValue == SwipeToDismissBoxValue.StartToEnd)) {
            isShrinking = true
        }
    }

    // Animate the height of the item from 100% to 0% during the shrink phase
    val shrinkHeightFactor by animateFloatAsState(
        targetValue = if (isShrinking) 0f else 1f,
        animationSpec = tween(durationMillis = 280),
        finishedListener = {
            if (isShrinking) {
                onDelete()
            }
        },
        label = "shrink_height"
    )

    val direction = dismissState.dismissDirection
    val isThresholdReached = dismissState.targetValue != SwipeToDismissBoxValue.Settled

    // Safely retrieve raw pixel offset from dismissState, defaulting to 0f if uninitialized
    val rawOffset = try {
        dismissState.requireOffset()
    } catch (e: Exception) {
        0f
    }
    
    // Convert current pixel offset to Dp
    val swipeOffsetDp = with(LocalDensity.current) { abs(rawOffset).toDp() }
    
    // Dynamically expand capsule width with absolute offset, keeping a visual 4.dp margin
    val capsuleWidth = (swipeOffsetDp - 4.dp).coerceAtLeast(0.dp)

    // Dynamic scale spring animation for the delete icon
    val iconScale by animateFloatAsState(
        targetValue = if (isThresholdReached) 1.4f else if (capsuleWidth < 48.dp) 0f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "trash_scale"
    )

    // Observe touch events without consuming them to track finger up/down
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                val currentHeight = (placeable.height * shrinkHeightFactor).toInt()
                layout(placeable.width, currentHeight) {
                    placeable.placeRelative(0, 0)
                }
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitPointerEvent(pass = PointerEventPass.Initial)
                    isTouching = true
                    do {
                        val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                    } while (event.changes.any { it.pressed })
                    isTouching = false
                }
            }
    ) {
    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val showBackground = direction != SwipeToDismissBoxValue.Settled || isShrinking
            if (showBackground) {
                val isDismissed = dismissState.currentValue == SwipeToDismissBoxValue.EndToStart ||
                                 dismissState.currentValue == SwipeToDismissBoxValue.StartToEnd ||
                                 isShrinking

                val alignment = when {
                    isShrinking -> Alignment.Center
                    direction == SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                    direction == SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                    else -> Alignment.Center
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = alignment
                ) {
                    val modifierForRed = if (isDismissed) {
                        Modifier
                            .fillMaxHeight()
                            .fillMaxWidth()
                            .background(
                                color = Color(0xFFFF7878), // Coral-red hex
                                shape = RoundedCornerShape(12.dp) // matches the card corner radius
                            )
                    } else {
                        Modifier
                            .fillMaxHeight()
                            .width(capsuleWidth)
                            .background(
                                color = Color(0xFFFF7878),
                                shape = RoundedCornerShape(24.dp) // fully rounded pill
                            )
                    }

                    Box(
                        modifier = modifierForRed,
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_delete_material_symbols),
                            contentDescription = "Löschen",
                            tint = Color(0xFF2C0B0E), // High contrast dark tone for delete icon
                            modifier = Modifier
                                .scale(iconScale * shrinkHeightFactor)
                                .size(28.dp)
                        )
                    }
                }
            }
        },
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        modifier = Modifier.fillMaxWidth()
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = word.germanWord,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "→",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = word.englishTranslation,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "\"${word.usageExample}\"",
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatDate(word.addedAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
    } // Box pointer observer
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
