package com.twentyfoursolve.data.repository

import com.twentyfoursolve.core.model.GameRecord
import com.twentyfoursolve.data.local.dao.GameRecordDao
import com.twentyfoursolve.data.local.entity.GameRecordEntity
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class GameRepositoryTest {

    private lateinit var fakeDao: FakeGameRecordDao
    private lateinit var repository: LocalGameRepository

    @Before
    fun setup() {
        fakeDao = FakeGameRecordDao()
        repository = LocalGameRepository(fakeDao)
    }

    private fun createRecord(
        isSuccess: Boolean = true,
        score: Int = 1250,
        timeTaken: Int = 45,
        difficulty: String = "medium",
        mode: String = "timed"
    ) = GameRecord(
        isSuccess = isSuccess,
        score = score,
        timeTaken = timeTaken,
        difficulty = difficulty,
        mode = mode
    )

    @Test
    fun `saveGameRecord returns positive id`() = runTest {
        val id = repository.saveGameRecord(createRecord())
        assert(id > 0)
    }

    @Test
    fun `getAllRecords returns saved records`() = runTest {
        repository.saveGameRecord(createRecord(isSuccess = true))
        repository.saveGameRecord(createRecord(isSuccess = false))

        val records = repository.getAllRecords()
        assertEquals(2, records.size)
    }

    @Test
    fun `getWinRate calculates correctly`() = runTest {
        repository.saveGameRecord(createRecord(isSuccess = true))
        repository.saveGameRecord(createRecord(isSuccess = true))
        repository.saveGameRecord(createRecord(isSuccess = false))

        val winRate = repository.getWinRate()
        assertEquals(2f / 3f, winRate)
    }

    @Test
    fun `getWinRate returns 0 when no games`() = runTest {
        val winRate = repository.getWinRate()
        assertEquals(0f, winRate)
    }

    @Test
    fun `getTotalTimedGames counts only timed mode`() = runTest {
        repository.saveGameRecord(createRecord(mode = "timed"))
        repository.saveGameRecord(createRecord(mode = "timed"))
        repository.saveGameRecord(createRecord(mode = "practice"))

        val count = repository.getTotalTimedGames()
        assertEquals(2, count)
    }

    @Test
    fun `getAverageTime calculates correctly`() = runTest {
        repository.saveGameRecord(createRecord(timeTaken = 30, isSuccess = true))
        repository.saveGameRecord(createRecord(timeTaken = 60, isSuccess = true))

        val avg = repository.getAverageTime()
        assertEquals(45.0, avg!!)
    }

    @Test
    fun `getCurrentStreak counts consecutive wins from end`() = runTest {
        repository.saveGameRecord(createRecord(isSuccess = true))
        repository.saveGameRecord(createRecord(isSuccess = true))
        repository.saveGameRecord(createRecord(isSuccess = true))
        repository.saveGameRecord(createRecord(isSuccess = false))
        repository.saveGameRecord(createRecord(isSuccess = true))

        val streak = repository.getCurrentStreak()
        assertEquals(1, streak)
    }

    @Test
    fun `getTopSolves returns limited results`() = runTest {
        repository.saveGameRecord(createRecord(score = 1000))
        repository.saveGameRecord(createRecord(score = 2000))
        repository.saveGameRecord(createRecord(score = 1500))

        val top = repository.getTopSolves(2)
        assertEquals(2, top.size)
    }

    @Test
    fun `difficulty stats count only that tier timed games`() = runTest {
        repository.saveGameRecord(createRecord(isSuccess = true, timeTaken = 10, difficulty = "easy"))
        repository.saveGameRecord(createRecord(isSuccess = false, timeTaken = 80, difficulty = "easy"))
        repository.saveGameRecord(createRecord(isSuccess = true, timeTaken = 40, difficulty = "medium"))
        repository.saveGameRecord(
            createRecord(isSuccess = true, timeTaken = 5, difficulty = "medium", mode = "practice")
        )

        val stats = repository.getDifficultyStats()
        val easy = stats.first { it.difficulty == com.twentyfoursolve.core.model.Difficulty.EASY }
        val medium = stats.first { it.difficulty == com.twentyfoursolve.core.model.Difficulty.MEDIUM }
        val hard = stats.first { it.difficulty == com.twentyfoursolve.core.model.Difficulty.HARD }

        assertEquals(4, stats.size)
        assertEquals(2, easy.games)
        assertEquals(1, easy.wins)
        assertEquals(10.0, easy.avgTimeSeconds)
        assertEquals(1, medium.games)
        assertEquals(40.0, medium.avgTimeSeconds)
        assertEquals(0, hard.games)
        assertEquals(null, hard.avgTimeSeconds)
    }

    @Test
    fun `getDailyActivity returns activity data`() = runTest {
        repository.saveGameRecord(createRecord(isSuccess = true))
        repository.saveGameRecord(createRecord(isSuccess = false))

        val sevenDaysAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
        val activity = repository.getDailyActivity(sevenDaysAgo)
        assert(activity.isNotEmpty())
    }
}

class FakeGameRecordDao : GameRecordDao {
    private val records = mutableListOf<GameRecordEntity>()
    private var nextId = 1L
    private var nextTimestamp = System.currentTimeMillis()

    override suspend fun insert(record: GameRecordEntity): Long {
        val id = nextId++
        records.add(record.copy(id = id, date = nextTimestamp++))
        return id
    }

    override suspend fun getAllRecords(): List<GameRecordEntity> {
        return records.sortedByDescending { it.date }
    }

    override suspend fun getTimedGameCount(): Int {
        return records.count { it.mode == "timed" }
    }

    override suspend fun getWinCount(): Int {
        return records.count { it.isSuccess && it.mode == "timed" }
    }

    override suspend fun getAverageTime(): Double? {
        val timedWins = records.filter { it.isSuccess && it.mode == "timed" }
        return if (timedWins.isEmpty()) null
        else timedWins.map { it.timeTaken }.average()
    }

    override suspend fun getTopSolves(limit: Int): List<GameRecordEntity> {
        return records
            .filter { it.isSuccess && it.mode == "timed" }
            .sortedByDescending { it.score }
            .take(limit)
    }

    override suspend fun getRecentWinCount(sinceTimestamp: Long): Int {
        return records.count { it.isSuccess && it.mode == "timed" && it.date >= sinceTimestamp }
    }

    override suspend fun getCurrentStreak(): Int? {
        val timedRecords = records
            .filter { it.mode == "timed" }
            .sortedByDescending { it.date }
        if (timedRecords.isEmpty()) return null

        var streak = 0
        for (record in timedRecords) {
            if (record.isSuccess) {
                streak++
            } else {
                break
            }
        }
        return streak
    }

    override suspend fun getTimedStatsByDifficulty(): List<GameRecordDao.DifficultyStatRow> {
        return records
            .filter { it.mode == "timed" }
            .groupBy { it.difficulty }
            .map { (difficulty, rows) ->
                val wins = rows.filter { it.isSuccess }
                GameRecordDao.DifficultyStatRow(
                    difficulty = difficulty,
                    games = rows.size,
                    wins = wins.size,
                    avgTime = wins.map { it.timeTaken.toDouble() }.average().takeIf { wins.isNotEmpty() },
                )
            }
    }

    override suspend fun getDailyActivity(sinceTimestamp: Long): List<GameRecordDao.DailyActivity> {
        return records
            .filter { it.mode == "timed" && it.date >= sinceTimestamp }
            .groupBy { it.date / 86400000 }
            .map { (day, dayRecords) ->
                GameRecordDao.DailyActivity(
                    date = day * 86400000,
                    win_count = dayRecords.count { it.isSuccess }
                )
            }
            .sortedBy { it.date }
    }
}
