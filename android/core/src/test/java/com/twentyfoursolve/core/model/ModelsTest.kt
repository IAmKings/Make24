package com.twentyfoursolve.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ModelsTest {

    // ─── Difficulty ──────────────────────────────────────────────────

    @Test
    fun `Difficulty fromName easy`() {
        assertEquals(Difficulty.EASY, Difficulty.fromName("easy"))
        assertEquals(Difficulty.EASY, Difficulty.fromName("EASY"))
    }

    @Test
    fun `Difficulty fromName medium`() {
        assertEquals(Difficulty.MEDIUM, Difficulty.fromName("medium"))
        assertEquals(Difficulty.MEDIUM, Difficulty.fromName("MEDIUM"))
    }

    @Test
    fun `Difficulty fromName hard`() {
        assertEquals(Difficulty.HARD, Difficulty.fromName("hard"))
        assertEquals(Difficulty.HARD, Difficulty.fromName("HARD"))
    }

    @Test
    fun `Difficulty fromName unknown defaults to medium`() {
        assertEquals(Difficulty.MEDIUM, Difficulty.fromName("unknown"))
    }

    @Test
    fun `Difficulty ranges are correct`() {
        assertEquals(1..6, Difficulty.EASY.range)
        assertEquals(1..10, Difficulty.MEDIUM.range)
        assertEquals(1..13, Difficulty.HARD.range)
    }


    // ─── Language ────────────────────────────────────────────────────

    @Test
    fun `Language fromCode zh`() {
        assertEquals(Language.ZH, Language.fromCode("zh"))
    }

    @Test
    fun `Language fromCode en`() {
        assertEquals(Language.EN, Language.fromCode("en"))
    }

    @Test
    fun `Language fromCode unknown defaults to EN`() {
        assertEquals(Language.EN, Language.fromCode("fr"))
    }

    // ─── Operator ────────────────────────────────────────────────────

    @Test
    fun `Operator symbols`() {
        assertEquals("+", Operator.PLUS.symbol)
        assertEquals("-", Operator.MINUS.symbol)
        assertEquals("×", Operator.MULTIPLY.symbol)
        assertEquals("÷", Operator.DIVIDE.symbol)
    }

    @Test
    fun `Operator apply`() {
        assertEquals(5.0, Operator.PLUS.apply(2.0, 3.0))
        assertEquals(1.0, Operator.MINUS.apply(4.0, 3.0))
        assertEquals(12.0, Operator.MULTIPLY.apply(3.0, 4.0))
        assertEquals(2.5, Operator.DIVIDE.apply(5.0, 2.0))
    }

    @Test
    fun `Operator DIVIDE by zero returns NaN`() {
        assertTrue(Operator.DIVIDE.apply(5.0, 0.0).isNaN())
    }

    // ─── Card ────────────────────────────────────────────────────────

    @Test
    fun `Card default values`() {
        val card = Card(
            id = "test",
            value = 7.0,
            label = "7",
            suit = Suit.HEARTS
        )
        assertEquals("test", card.id)
        assertEquals(7.0, card.value)
        assertEquals("7", card.label)
        assertEquals(Suit.HEARTS, card.suit)
        assertEquals(false, card.isUsed)
        assertEquals(null, card.characterId)
        assertEquals(null, card.skinId)
    }

    @Test
    fun `Card with extension fields`() {
        val card = Card(
            id = "test",
            value = 5.0,
            label = "5",
            suit = Suit.SPADES,
            characterId = "char1",
            skinId = "skin2"
        )
        assertEquals("char1", card.characterId)
        assertEquals("skin2", card.skinId)
    }

    // ─── Suit ────────────────────────────────────────────────────────

    @Test
    fun `Suit has 4 entries`() {
        assertEquals(4, Suit.entries.size)
    }

    // ─── GameState ───────────────────────────────────────────────────

    @Test
    fun `GameState default values`() {
        val state = GameState()
        assertTrue(state.cards.isEmpty())
        assertTrue(state.selectedCardIndices.isEmpty())
        assertEquals(null, state.currentOperator)
        assertEquals(0, state.score)
        assertEquals(120, state.timeRemaining)
        assertEquals(Difficulty.MEDIUM, state.difficulty)
        assertEquals(false, state.isGameOver)
        assertEquals(false, state.isSuccess)
        assertTrue(state.history.isEmpty())
    }

    // ─── GameRecord ──────────────────────────────────────────────────

    @Test
    fun `GameRecord default values`() {
        val record = GameRecord(
            isSuccess = true,
            score = 1250,
            timeTaken = 45,
            difficulty = "medium",
            mode = "timed"
        )
        assertEquals(0L, record.id)
        assertEquals(true, record.isSuccess)
        assertEquals(1250, record.score)
        assertEquals(45, record.timeTaken)
        assertEquals("medium", record.difficulty)
        assertEquals("timed", record.mode)
    }

    // ─── UserSettings ────────────────────────────────────────────────

    @Test
    fun `UserSettings default values`() {
        val settings = UserSettings()
        assertEquals(true, settings.soundEnabled)
        assertEquals(Difficulty.MEDIUM, settings.difficultyPreference)
        assertEquals(Language.ZH, settings.language)
    }
}
