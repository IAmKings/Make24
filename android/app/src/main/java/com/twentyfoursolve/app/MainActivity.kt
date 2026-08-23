package com.twentyfoursolve.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.twentyfoursolve.app.navigation.BottomNavBar
import com.twentyfoursolve.app.theme.LocalStringProvider
import com.twentyfoursolve.app.theme.Solve24Theme
import com.twentyfoursolve.app.ui.game.GameScreen
import com.twentyfoursolve.app.ui.home.HomeScreen
import com.twentyfoursolve.app.ui.practice.PracticeScreen
import com.twentyfoursolve.app.ui.rules.RulesScreen
import com.twentyfoursolve.app.ui.settings.SettingsScreen
import com.twentyfoursolve.app.ui.stats.StatsScreen
import com.twentyfoursolve.core.model.Language
import com.twentyfoursolve.core.model.UserSettings
import com.twentyfoursolve.data.repository.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by settingsRepository.settings.collectAsState(
                initial = UserSettings()
            )
            Solve24Theme(language = settings.language) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Solve24AppContent()
                }
            }
        }
    }
}

sealed class Screen {
    data object Home : Screen()
    data class Game(val difficulty: String, val isPractice: Boolean = false, val numberRange: String = "Mixed") : Screen()
    data object Practice : Screen()
    data object Rules : Screen()
    data object Stats : Screen()
    data object Settings : Screen()
}

@Composable
fun Solve24AppContent() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    var selectedTab by rememberSaveable { mutableStateOf("home") }
    var showExitDialog by remember { mutableStateOf(false) }
    val showBottomBar = currentScreen !is Screen.Game
    val strings = LocalStringProvider.current

    fun exitGame(s: Screen.Game) {
        if (s.isPractice) {
            selectedTab = "practice"
            currentScreen = Screen.Practice
        } else {
            selectedTab = "home"
            currentScreen = Screen.Home
        }
    }

    // 系统返回键：所有二级页面正确回到上一级；游戏页先弹确认，Home 由系统默认处理（退出应用）
    BackHandler(enabled = currentScreen != Screen.Home) {
        val s = currentScreen
        if (s is Screen.Game) {
            showExitDialog = true
        } else {
            selectedTab = "home"
            currentScreen = Screen.Home
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(
                    currentTab = selectedTab,
                    onTabSelected = { tab ->
                        selectedTab = tab
                        currentScreen = when (tab) {
                            "home" -> Screen.Home
                            "practice" -> Screen.Practice
                            "rules" -> Screen.Rules
                            "stats" -> Screen.Stats
                            else -> Screen.Home
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        val modifier = Modifier.padding(innerPadding)

        when (val screen = currentScreen) {
            is Screen.Home -> HomeScreen(
                onStartGame = {
                    currentScreen = Screen.Game(difficulty = "medium", isPractice = false)
                },
                onPracticeMode = {
                    selectedTab = "practice"
                    currentScreen = Screen.Practice
                },
                onNavigate = { tab ->
                    selectedTab = tab
                    currentScreen = when (tab) {
                        "practice" -> Screen.Practice
                        "rules" -> Screen.Rules
                        "stats" -> Screen.Stats
                        "settings" -> Screen.Settings
                        else -> Screen.Home
                    }
                },
                modifier = modifier
            )

            is Screen.Game -> GameScreen(
                difficulty = screen.difficulty,
                isPractice = screen.isPractice,
                numberRange = screen.numberRange,
                onExit = { exitGame(screen) },
                onRequestExit = { showExitDialog = true },
                modifier = modifier
            )

            is Screen.Practice -> PracticeScreen(
                onBack = {
                    selectedTab = "home"
                    currentScreen = Screen.Home
                },
                onStartPractice = { diff, range ->
                    currentScreen = Screen.Game(
                        difficulty = diff,
                        isPractice = true,
                        numberRange = range
                    )
                },
                modifier = modifier
            )

            is Screen.Rules -> RulesScreen(
                onBack = {
                    selectedTab = "home"
                    currentScreen = Screen.Home
                },
                modifier = modifier
            )

            is Screen.Stats -> StatsScreen(modifier = modifier)

            is Screen.Settings -> SettingsScreen(
                onBack = {
                    selectedTab = "home"
                    currentScreen = Screen.Home
                },
                modifier = modifier
            )
        }
    }

    // 游戏页退出确认（系统返回键 / HUD 返回按钮触发），防止误触丢失进度
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text(strings["exitConfirmTitle"] ?: "Exit Game") },
            text = { Text(strings["exitConfirmText"] ?: "Are you sure you want to exit? Current progress will be lost.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExitDialog = false
                        val s = currentScreen
                        if (s is Screen.Game) exitGame(s)
                    }
                ) {
                    Text(strings["exitConfirm"] ?: "Exit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text(strings["cancel"] ?: "Cancel")
                }
            }
        )
    }
}