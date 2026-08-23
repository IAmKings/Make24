package com.twentyfoursolve.app

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.twentyfoursolve.app.navigation.BottomNavBar
import com.twentyfoursolve.app.navigation.Routes
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
                    Solve24NavHost()
                }
            }
        }
    }
}

@Composable
fun Solve24NavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // 游戏页（普通局/练习局）隐藏底部导航栏
    val showBottomBar = currentRoute != Routes.GAME && currentRoute != Routes.GAME_PRACTICE

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(
                    currentTab = currentRoute ?: Routes.HOME,
                    onTabSelected = { tab -> navigateToTab(navController, tab) }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onStartGame = { navController.navigate(Routes.game("medium")) },
                    onPracticeMode = { navController.navigate(Routes.PRACTICE_CONFIG) },
                    onNavigate = { tab ->
                        when (tab) {
                            "practice" -> navController.navigate(Routes.PRACTICE_CONFIG)
                            else -> navController.navigate(tab)
                        }
                    }
                )
            }

            composable(
                route = Routes.GAME,
                arguments = listOf(navArgument("difficulty") { type = NavType.StringType; defaultValue = "medium" })
            ) { entry ->
                val difficulty = entry.arguments?.getString("difficulty") ?: "medium"
                GameScreen(
                    difficulty = difficulty,
                    isPractice = false,
                    onExit = { navController.popBackStack() }
                )
            }

            composable(
                route = Routes.GAME_PRACTICE,
                arguments = listOf(
                    navArgument("difficulty") { type = NavType.StringType; defaultValue = "easy" },
                    navArgument("range") { type = NavType.StringType; defaultValue = "Mixed" }
                )
            ) { entry ->
                val difficulty = entry.arguments?.getString("difficulty") ?: "easy"
                val range = Uri.decode(entry.arguments?.getString("range") ?: "Mixed")
                GameScreen(
                    difficulty = difficulty,
                    isPractice = true,
                    numberRange = range,
                    onExit = { navController.popBackStack() }
                )
            }

            composable(Routes.PRACTICE_CONFIG) {
                PracticeScreen(
                    onBack = { navController.popBackStack() },
                    onStartPractice = { diff, range ->
                        navController.navigate(Routes.gamePractice(diff, Uri.encode(range)))
                    }
                )
            }

            composable(Routes.RULES) {
                RulesScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.STATS) {
                StatsScreen()
            }

            composable(Routes.SETTINGS) {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

/** 底部导航切换：单顶入栈 + 保存/恢复各 tab 状态（官方推荐模式）。 */
private fun navigateToTab(navController: NavHostController, route: String) {
    navController.navigate(route) {
        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
