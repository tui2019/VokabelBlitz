package de.vokabelblitz

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Main : NavKey

// Tab destinations
@Serializable data object HomeTab : NavKey
@Serializable data object WordsTab : NavKey

// Full-screen destinations
@Serializable data object QuizDestination : NavKey
