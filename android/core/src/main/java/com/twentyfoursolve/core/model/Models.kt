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
    // 精确分数表示：合并结果以此为准（value 仅用于兼容显示）
    val numerator: Long = value.toLong(),
    val denominator: Long = 1L,
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
 * 统一难度阶梯：三档按数字范围递增（简单/中等/困难），难度即数值范围，
 * 不再单独提供"目标数值集合"维度，避免两个选择语义重复。
 */
enum class Difficulty(val range: IntRange, val label: String) {
    EASY(1..6, "Easy"),
    MEDIUM(1..10, "Medium"),
    HARD(1..13, "Hard");

    companion object {
        fun fromName(name: String): Difficulty = when (name.lowercase()) {
            "easy" -> EASY
            "hard" -> HARD
            else -> MEDIUM
        }
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
    val history: List<List<Card>> = emptyList(),
    /** 提示：当前牌面的一个解法表达式（null 表示未请求提示）。 */
    val hint: String? = null,
    /** 可解性检查结果：null=未检查，true=有解，false=无解。 */
    val solvable: Boolean? = null
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