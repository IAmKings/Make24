package com.twentyfoursolve.data.repository

import com.twentyfoursolve.data.local.dao.DailyResultDao
import com.twentyfoursolve.data.local.entity.DailyResultEntity
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DailyRepositoryTest {

    private lateinit var repository: LocalDailyRepository

    @Before
    fun setup() {
        repository = LocalDailyRepository(FakeDailyResultDao())
    }

    @Test
    fun `higher score replaces the day and a tie keeps the faster time`() = runTest {
        assertTrue(repository.saveIfBetter(10, score = 1000, timeTaken = 40))
        assertFalse(repository.saveIfBetter(10, score = 900, timeTaken = 10))
        assertEquals(1000, repository.get(10)?.score)

        assertTrue(repository.saveIfBetter(10, score = 1000, timeTaken = 20))
        assertEquals(20, repository.get(10)?.timeTaken)

        assertFalse(repository.saveIfBetter(10, score = 1000, timeTaken = 30))
        assertEquals(20, repository.get(10)?.timeTaken)
    }

    @Test
    fun `streak counts consecutive days and skips an unfinished today`() = runTest {
        repository.saveIfBetter(8, 1, 10)
        repository.saveIfBetter(9, 1, 10)
        assertEquals(2, repository.streakAsOf(10))
        repository.saveIfBetter(10, 1, 10)
        assertEquals(3, repository.streakAsOf(10))
        assertEquals(3, repository.streakAsOf(11))
        assertEquals(0, repository.streakAsOf(12))
    }
}

private class FakeDailyResultDao : DailyResultDao {
    private val rows = mutableMapOf<Long, DailyResultEntity>()

    override suspend fun get(epochDay: Long): DailyResultEntity? = rows[epochDay]

    override suspend fun upsert(result: DailyResultEntity) {
        rows[result.epochDay] = result
    }

    override suspend fun epochDaysAtOrBefore(epochDay: Long): List<Long> {
        return rows.keys.filter { it <= epochDay }.sortedDescending()
    }
}
