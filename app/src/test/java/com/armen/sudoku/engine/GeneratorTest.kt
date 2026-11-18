package com.armen.sudoku.engine

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GeneratorTest {

    @Test
    fun everyDifficultyProducesAUniqueLogicallySolvablePuzzle() {
        for (d in Difficulty.entries) {
            repeat(3) { seed ->
                val p = Generator(Random(seed * 31 + d.ordinal)).generate(d)
                assertTrue(isValidSolution(p.solution), "$d solution is valid")
                assertEquals(1, Solver.countSolutions(p.givens), "$d has a unique solution")
                for (i in 0 until 81) {
                    if (p.givens[i] != 0) assertEquals(p.solution[i], p.givens[i], "$d givens match solution")
                }
                val clues = p.givens.count { it != 0 }
                // Symmetric pairs remove two cells at a time, so the count can end one below the floor.
                assertTrue(clues >= d.minClues - 1, "$d has $clues clues")

                val result = LogicSolver.solve(p.givens, d.maxTier)
                assertTrue(result.solved, "$d can be solved with techniques up to tier ${d.maxTier}")
                assertContentEquals(p.solution, result.grid)
                assertEquals(d.minTier, result.hardestTier, "$d (seed $seed) needs a tier-${d.minTier} technique")
            }
        }
    }

    @Test
    fun sameSeedGivesSamePuzzle() {
        for (d in Difficulty.entries) {
            val a = Generator(Random(20261118)).generate(d)
            val b = Generator(Random(20261118)).generate(d)
            assertContentEquals(a.givens, b.givens, "$d is deterministic")
        }
    }

    @Test
    fun easyHasMoreCluesThanMedium() {
        repeat(5) { seed ->
            val easy = Generator(Random(seed)).generate(Difficulty.EASY).givens.count { it != 0 }
            val medium = Generator(Random(seed)).generate(Difficulty.MEDIUM).givens.count { it != 0 }
            assertTrue(easy > medium, "seed $seed: easy $easy vs medium $medium")
        }
    }
}
