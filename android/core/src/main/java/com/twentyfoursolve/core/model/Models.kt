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
enum class Difficulty(val range: IntRange, val label: String, val multiplier: Double) {
    EASY(1..6, "Easy", 1.0),
    MEDIUM(1..10, "Medium", 1.5),
    HARD(1..13, "Hard", 2.0);

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
    val solvable: Boolean? = null,
    /** 简单难度下合并被拒绝（合并后剩余牌面无解）。 */
    val mergeRejected: Boolean = false,
    /** 是否允许无解题（设置配置；决定无解按钮可用性）。 */
    val allowUnsolvable: Boolean = true,
    /** 提示结果为无解（当前牌面无法到 24，作为回答展示）。 */
    val hintUnsolvable: Boolean = false,
    /** 开局 4 张牌值（无解按钮仅回答开局是否无解）。 */
    val initialPuzzle: List<Int> = emptyList(),
    /** 无解回答猜错（牌面有解）已施加惩罚。 */
    val unsolvablePenalty: Boolean = false,
    /** 本关得分（成功时的新增分数，用于结算展示）。 */
    val roundScore: Int = 0
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
    val language: Language = Language.ZH,
    /** 是否允许开局出现无解题（中高难度；简单难度始终无无解）。 */
    val allowUnsolvable: Boolean = true
)