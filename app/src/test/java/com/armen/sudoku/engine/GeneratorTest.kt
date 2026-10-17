package com.armen.sudoku.engine

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GeneratorTest {

    @Test
    fun everyDifficultyProducesAUniqueSolvablePuzzle() {
        for (d in Difficulty.entries) {
            val p = Generator(Random(d.ordinal + 1)).generate(d)
            assertTrue(isValidSolution(p.solution), "$d solution is valid")
            assertEquals(1, Solver.countSolutions(p.givens), "$d has a unique solution")
            for (i in 0 until 81) {
                if (p.givens[i] != 0) assertEquals(p.solution[i], p.givens[i], "$d givens match solution")
            }
            val clues = p.givens.count { it != 0 }
            // Symmetric removal can stop a couple of clues above target when no pair can be removed.
            assertTrue(clues in d.targetClues - 1..d.targetClues + 8, "$d has $clues clues")
        }
    }

    @Test
    fun harderLevelsHaveFewerClues() {
        val counts = Difficulty.entries.map { d -> Generator(Random(42)).generate(d).givens.count { it != 0 } }
        assertEquals(counts.sortedDescending(), counts)
    }
}
