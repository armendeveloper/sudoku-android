package com.armen.sudoku.game

import com.armen.sudoku.engine.Difficulty
import com.armen.sudoku.engine.LogicSolver
import com.armen.sudoku.engine.Solver
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StatsTest {

    private val easy = Difficulty.EASY

    @Test
    fun winsUpdateCountsTimesAndStreaks() {
        var stats = Stats().started(easy)
        val (afterFirst, first) = stats.won(easy, seconds = 300, mistakes = 0, hints = 0, dailyDay = null, today = 100)
        stats = afterFirst
        assertTrue(first.newBestTime)
        assertEquals(1, first.streak)
        assertNull(first.dailyStreak)

        stats = stats.started(easy)
        val (afterSecond, second) = stats.won(easy, seconds = 200, mistakes = 2, hints = 0, dailyDay = null, today = 100)
        stats = afterSecond
        assertTrue(second.newBestTime)
        assertEquals(2, second.streak)

        val s = stats[easy]
        assertEquals(2, s.played)
        assertEquals(2, s.won)
        assertEquals(1, s.perfect)
        assertEquals(200L, s.bestTime)
        assertEquals(250L, s.averageTime)
        assertEquals(100, s.winRate)
        assertEquals(2, s.bestStreak)
    }

    @Test
    fun hintsDoNotSetBestTime() {
        val (stats, win) = Stats().started(easy).won(easy, seconds = 10, mistakes = 0, hints = 3, dailyDay = null, today = 0)
        assertFalse(win.newBestTime)
        assertNull(stats[easy].bestTime)
        assertEquals(0, stats[easy].perfect)
        assertEquals(10L, stats[easy].averageTime)
    }

    @Test
    fun aLossResetsTheStreakButKeepsTheBest() {
        var stats = Stats()
        repeat(3) { stats = stats.started(easy).won(easy, 100, 0, 0, null, 0).first }
        stats = stats.started(easy).lost(easy)
        val s = stats[easy]
        assertEquals(0, s.currentStreak)
        assertEquals(3, s.bestStreak)
        assertEquals(4, s.played)
        assertEquals(75, s.winRate)
    }

    @Test
    fun difficultiesAreTrackedSeparately() {
        val stats = Stats().started(Difficulty.HARD).won(Difficulty.HARD, 500, 0, 0, null, 0).first
        assertEquals(1, stats[Difficulty.HARD].won)
        assertEquals(0, stats[easy].played)
        assertNull(stats[easy].winRate)
    }

    @Test
    fun dailyStreakCountsBackFromTodayOrYesterday() {
        val stats = Stats(dailyDays = setOf(10, 11, 12, 20, 21))
        assertEquals(2, stats.dailyStreak(today = 21))
        assertEquals(2, stats.dailyStreak(today = 22)) // today not played yet: the streak still stands
        assertEquals(0, stats.dailyStreak(today = 23)) // missed a day
        assertEquals(3, stats.bestDailyStreak)
        assertEquals(0, Stats().bestDailyStreak)
    }

    @Test
    fun winningTheDailyRecordsTheDay() {
        val stats = Stats(dailyDays = setOf(99))
        val (after, win) = stats.started(easy).won(easy, 100, 0, 0, dailyDay = 100, today = 100)
        assertTrue(100L in after.dailyDays)
        assertEquals(2, win.dailyStreak)
    }
}

class DailyTest {

    @Test
    fun difficultyRisesThroughTheWeek() {
        val monday = LocalDate.of(2026, 11, 16).toEpochDay()
        val expected = listOf(
            Difficulty.EASY, Difficulty.EASY, Difficulty.MEDIUM, Difficulty.MEDIUM,
            Difficulty.HARD, Difficulty.HARD, Difficulty.EXPERT,
        )
        assertEquals(expected, (0 until 7).map { Daily.difficultyFor(monday + it) })
    }

    @Test
    fun sameDayGivesSamePuzzleAndDaysDiffer() {
        val day = LocalDate.of(2026, 11, 18).toEpochDay()
        val a = Daily.puzzle(day)
        val b = Daily.puzzle(day)
        assertContentEquals(a.givens, b.givens)
        assertFalse(a.givens.contentEquals(Daily.puzzle(day + 7).givens))
        assertEquals(Daily.difficultyFor(day), a.difficulty)
        assertEquals(1, Solver.countSolutions(a.givens))
        assertTrue(LogicSolver.solve(a.givens).solved)
    }
}
