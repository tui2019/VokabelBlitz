package com.example.vokabelblitz
 
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.vokabelblitz.ui.WordViewModel
import com.example.vokabelblitz.ui.home.HomeScreen
import com.example.vokabelblitz.ui.quiz.QuizScreen
import com.example.vokabelblitz.ui.words.WordsScreen
import androidx.compose.runtime.collectAsState
 
data class BottomNavItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)
 
@Composable
fun MainNavigation() {
    val viewModel: WordViewModel = viewModel()
    val isQuizActive by viewModel.isQuizActive.collectAsState()
 
    if (isQuizActive) {
        // Intercept back gesture inside Quiz Screen to return to Home screen
        BackHandler {
            viewModel.endQuiz()
        }
        QuizScreen(
            viewModel = viewModel,
            onExit = { viewModel.endQuiz() },
            modifier = Modifier.safeDrawingPadding()
        )
    } else {
        MainScaffold(viewModel = viewModel)
    }
}
 
@Composable
private fun MainScaffold(viewModel: WordViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
 
    // Intercept back gesture on the Words tab to return to the Learn (Home) tab
    if (selectedTab != 0) {
        BackHandler {
            selectedTab = 0
        }
    }
 
    val navItems = remember {
        listOf(
            BottomNavItem("Learn", Icons.Filled.Home, Icons.Outlined.Home),
            BottomNavItem("Words", Icons.Filled.LibraryBooks, Icons.Outlined.LibraryBooks)
        )
    }
 
    Scaffold(
        bottomBar = {
            NavigationBar {
                navItems.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                if (selectedTab == index) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label
                            )
                        },
                        label = { Text(item.label) },
                        selected = selectedTab == index,
                        onClick = { selectedTab = index }
                    )
                }
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            0 -> HomeScreen(
                viewModel = viewModel,
                onStartQuiz = { viewModel.startQuiz() },
                modifier = Modifier.padding(innerPadding)
            )
            1 -> WordsScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}
