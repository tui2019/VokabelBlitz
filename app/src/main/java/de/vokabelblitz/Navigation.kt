package de.vokabelblitz
 
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import de.vokabelblitz.ui.WordViewModel
import de.vokabelblitz.ui.home.HomeScreen
import de.vokabelblitz.ui.quiz.QuizScreen
import de.vokabelblitz.ui.words.WordsScreen
 
data class BottomNavItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)
 
@Composable
fun MainNavigation(
    startQuizImmediately: Boolean = false,
    onQuizStarted: () -> Unit = {}
) {
    val viewModel: WordViewModel = viewModel()
    val navController = rememberNavController()

    // Add navigation trace logging
    DisposableEffect(navController) {
        val listener = androidx.navigation.NavController.OnDestinationChangedListener { _, destination, _ ->
            android.util.Log.d("Navigation", "Destination changed: ${destination.route}")
        }
        navController.addOnDestinationChangedListener(listener)
        onDispose {
            navController.removeOnDestinationChangedListener(listener)
        }
    }

    // LaunchedEffect to immediately start quiz when triggered from the widget
    androidx.compose.runtime.LaunchedEffect(startQuizImmediately) {
        if (startQuizImmediately) {
            val currentRoute = navController.currentDestination?.route
            if (currentRoute == "main") {
                android.util.Log.d("Navigation", "LaunchedEffect: startQuizImmediately triggered!")
                viewModel.startQuiz()
                navController.navigate("quiz")
                onQuizStarted()
            } else {
                android.util.Log.w("Navigation", "LaunchedEffect: Ignored startQuizImmediately because current destination is $currentRoute")
                onQuizStarted()
            }
        }
    }
 
    NavHost(navController = navController, startDestination = "main") {
        composable("main") {
            MainScaffold(
                viewModel = viewModel,
                onStartQuiz = {
                    // Guard against rapid double clicks or navigating while transitioning
                    val currentRoute = navController.currentDestination?.route
                    if (currentRoute == "main") {
                        android.util.Log.d("Navigation", "onStartQuiz: Starting quiz and navigating to quiz screen")
                        viewModel.startQuiz()
                        navController.navigate("quiz")
                    } else {
                        android.util.Log.w("Navigation", "onStartQuiz: Ignored click because current destination is $currentRoute")
                    }
                }
            )
        }
        composable("quiz") {
            var isExiting by remember { mutableStateOf(false) }

            BackHandler {
                if (!isExiting) {
                    isExiting = true
                    android.util.Log.d("Navigation", "BackHandler triggered exit from quiz screen")
                    viewModel.endQuiz()
                    navController.popBackStack("main", false)
                } else {
                    android.util.Log.w("Navigation", "BackHandler: Ignored duplicate exit attempt")
                }
            }

            QuizScreen(
                viewModel = viewModel,
                onExit = {
                    if (!isExiting) {
                        isExiting = true
                        android.util.Log.d("Navigation", "onExit triggered from quiz screen")
                        viewModel.endQuiz()
                        navController.popBackStack("main", false)
                    } else {
                        android.util.Log.w("Navigation", "onExit: Ignored duplicate exit attempt")
                    }
                },
                modifier = Modifier.safeDrawingPadding()
            )
        }
    }
}
 
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MainScaffold(viewModel: WordViewModel, onStartQuiz: () -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }

 
    // Intercept back gesture on the Words tab to return to the Learn (Home) tab
    if (selectedTab != 0) {
        BackHandler {
            selectedTab = 0
        }
    }
 
    val navItems = remember {
        listOf(
            BottomNavItem("Lernen", Icons.Filled.Home, Icons.Outlined.Home),
            BottomNavItem("Wörter", Icons.Filled.LibraryBooks, Icons.Outlined.LibraryBooks)
        )
    }
 
    Scaffold(
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                ) {
                    // Floating Navigation Toolbar (M3 Expressive)
                    HorizontalFloatingToolbar(
                        expanded = true,
                        colors = FloatingToolbarDefaults.standardFloatingToolbarColors(
                            toolbarContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier.wrapContentSize()
                    ) {
                        navItems.forEachIndexed { index, item ->
                            val isSelected = selectedTab == index
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.secondaryContainer 
                                        else Color.Transparent
                                    )
                                    .clickable { selectedTab = index }
                                    .animateContentSize()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = item.selectedIcon,
                                            contentDescription = item.label,
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    Text(
                                        text = item.label,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer 
                                                else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (index < navItems.size - 1) {
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                        }
                    }

                }
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            0 -> HomeScreen(
                viewModel = viewModel,
                onStartQuiz = onStartQuiz,
                modifier = Modifier.padding(innerPadding)
            )
            1 -> WordsScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(top = innerPadding.calculateTopPadding()),
                bottomPadding = innerPadding.calculateBottomPadding()
            )
        }
    }
}
