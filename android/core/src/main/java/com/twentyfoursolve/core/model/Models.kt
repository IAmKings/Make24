package com.twentyfoursolve.core.model

/**
 * Card suit, matching the web demo's Suit type.
 */
enum class Suit {
    HEARTS,
    DIAMONDS,
    CLUBS,
    SPADES
}

/**
 * Playing card for the 24 game.
 * Reserves characterId and skinId for future extensibility (Card role/skin system).
 */
data class Card(
    val id: String,
    val value: Double,
    val label: String,
    val suit: Suit,
    val isUsed: Boolean = false,
    // Extension fields reserved for future card role/skin system
    val characterId: String? = null,
    val skinId: String? = null
)

/**
 * Arithmetic operators available in the game.
 */
enum class Operator(val symbol: String) {
    PLUS("+"),
    MINUS("-"),
    MULTIPLY("×"),
    DIVIDE("÷");

    fun apply(a: Double, b: Double): Double = when (this) {
        PLUS -> a + b
        MINUS -> a - b
        MULTIPLY -> a * b
        DIVIDE -> if (b != 0.0) a / b else Double.NaN
    }
}

/**
 * Game difficulty levels.
 */
enum class Difficulty(val range: IntRange, val label: String) {
    EASY(1..10, "Easy"),
    MEDIUM(1..13, "Medium"),
    HARD(1..20, "Hard");

    companion object {
        fun fromName(name: String): Difficulty = when (name.lowercase()) {
            "easy" -> EASY
            "hard" -> HARD
            else -> MEDIUM
        }
    }
}

/**
 * Number range for practice mode.
 */
enum class NumberRange(val label: String, val range: IntRange) {
    RANGE_1_5("1-5", 1..5),
    RANGE_6_10("6-10", 6..10),
    RANGE_11_15("11-15", 11..15),
    MIXED("Mixed", 1..20);

    companion object {
        fun fromLabel(label: String): NumberRange = entries.find { it.label == label } ?: MIXED
    }
}

/**
 * Language support.
 */
enum class Language(val code: String, val displayName: String) {
    EN("en", "English"),
    ZH("zh", "中文");

    companion object {
        fun fromCode(code: String): Language = if (code == "zh") ZH else EN
    }
}

/**
 * Current game state (MVI state).
 */
data class GameState(
    val cards: List<Card> = emptyList(),
    val selectedCardIndices: Set<Int> = emptySet(),
    val currentOperator: Operator? = null,
    val score: Int = 0,
    val timeRemaining: Int = 120, // seconds
    val difficulty: Difficulty = Difficulty.MEDIUM,
    val isGameOver: Boolean = false,
    val isSuccess: Boolean = false,
    val history: List<List<Card>> = emptyList()
)

/**
 * A saved game record for persistence.
 */
data class GameRecord(
    val id: Long = 0,
    val date: Long = System.currentTimeMillis(),
    val isSuccess: Boolean,
    val score: Int,
    val timeTaken: Int, // seconds
    val difficulty: String,
    val mode: String // "timed" or "practice"
)

/**
 * User settings.
 */
data class UserSettings(
    val soundEnabled: Boolean = true,
    val difficultyPreference: Difficulty = Difficulty.MEDIUM,
    val language: Language = Language.ZH
)