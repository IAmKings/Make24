package com.twentyfoursolve.app.navigation

/**
 * Navigation routes for the app.
 */
object Routes {
    const val HOME = "home"
    const val GAME = "game/{difficulty}"
    const val PRACTICE_CONFIG = "practice_config"
    const val GAME_PRACTICE = "game_practice/{difficulty}"
    const val GAME_DAILY = "game_daily"
    const val RULES = "rules"
    const val STATS = "stats"
    const val SETTINGS = "settings"

    fun game(difficulty: String = "medium") = "game/$difficulty"
    fun gamePractice(difficulty: String = "easy") = "game_practice/$difficulty"
}