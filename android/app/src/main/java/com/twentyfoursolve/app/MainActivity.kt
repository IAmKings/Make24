package com.twentyfoursolve.app

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
import com.twentyfoursolve.core.model.Difficulty
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
                    Solve24NavHost(defaultDifficulty = settings.difficultyPreference)
                }
            }
        }
    }
}

@Composable
fun Solve24NavHost(defaultDifficulty: Difficulty) {
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
                    onStartGame = {
                        navController.navigate(Routes.game(defaultDifficulty.name.lowercase()))
                    },
                    onPracticeMode = { navigateToTab(navController, Routes.PRACTICE_CONFIG) },
                    onNavigate = { tab ->
                        when (tab) {
                            "practice" -> navigateToTab(navController, Routes.PRACTICE_CONFIG)
                            "rules", "stats" -> navigateToTab(navController, tab)
                            // settings 为二级页，普通导航（返回键回上一级）
                            else -> navController.navigate(Routes.SETTINGS)
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
                    navArgument("difficulty") { type = NavType.StringType; defaultValue = "easy" }
                )
            ) { entry ->
                val difficulty = entry.arguments?.getString("difficulty") ?: "easy"
                GameScreen(
                    difficulty = difficulty,
                    isPractice = true,
                    onExit = { navController.popBackStack() }
                )
            }

            composable(Routes.PRACTICE_CONFIG) {
                PracticeScreen(
                    onStartPractice = { diff ->
                        navController.navigate(Routes.gamePractice(diff))
                    }
                )
            }

            composable(Routes.RULES) {
                RulesScreen()
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

/**
 * 底部导航切换。
 *
 * 注意：不能使用 launchSingleTop —— 从 tab A 切到 tab B 时，popUpTo 会先把 A 弹出，
 * 若目标路由此时恰好位于栈顶，launchSingleTop 会拦截压栈，导致没有新导航事件，
 * currentBackStackEntry 不更新，UI 表现为"点击无响应"。
 * 改为：目标页已是当前页则直接忽略；否则弹回起始页（保留各 tab 状态）后压入目标页，
 * 保证每次切换都产生真实的导航事件。
 */
private fun navigateToTab(navController: NavHostController, route: String) {
    if (navController.currentDestination?.route == route) return
    navController.navigate(route) {
        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
        restoreState = true
    }
}
