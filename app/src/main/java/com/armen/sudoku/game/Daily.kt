package com.armen.sudoku.game

import com.armen.sudoku.engine.Difficulty
import com.armen.sudoku.engine.Generator
import com.armen.sudoku.engine.Puzzle
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.random.Random

/**
 * The daily challenge: one puzzle per calendar day, built from a seed of the date, so every
 * player with the same app version gets the same puzzle. It gets harder through the week.
 */
object Daily {

    fun today(): Long = LocalDate.now().toEpochDay()

    fun difficultyFor(day: Long): Difficulty = when (LocalDate.ofEpochDay(day).dayOfWeek!!) {
        DayOfWeek.MONDAY, DayOfWeek.TUESDAY -> Difficulty.EASY
        DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY -> Difficulty.MEDIUM
        DayOfWeek.FRIDAY, DayOfWeek.SATURDAY -> Difficulty.HARD
        DayOfWeek.SUNDAY -> Difficulty.EXPERT
    }

    fun puzzle(day: Long): Puzzle = Generator(Random(day * 1_000_003L + 0x5EED)).generate(difficultyFor(day))
}
