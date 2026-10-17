package com.armen.sudoku.engine

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SolverTest {

    private val puzzle = grid(
        "530070000600195000098000060800060003400803001700020006060000280000419005000080079",
    )
    private val solution = grid(
        "534678912672195348198342567859761423426853791713924856961537284287419635345286179",
    )

    @Test
    fun solvesClassicPuzzle() {
        assertContentEquals(solution, Solver.solve(puzzle))
    }

    @Test
    fun classicPuzzleHasUniqueSolution() {
        assertEquals(1, Solver.countSolutions(puzzle))
    }

    @Test
    fun emptyGridHasManySolutions() {
        assertEquals(2, Solver.countSolutions(IntArray(81), limit = 2))
    }

    @Test
    fun randomSolutionOfEmptyGridIsValid() {
        assertTrue(isValidSolution(Solver.solve(IntArray(81), Random(7))!!))
    }

    @Test
    fun conflictingGridHasNoSolution() {
        val bad = puzzle.copyOf().also { it[2] = 5 } // second 5 in row 1
        assertNull(Solver.solve(bad))
        assertEquals(0, Solver.countSolutions(bad))
    }
}

internal fun grid(s: String) = s.map { it.digitToInt() }.toIntArray()

internal fun isValidSolution(g: IntArray): Boolean {
    val units = Sudoku.ROWS + Sudoku.COLS + Sudoku.BOXES
    return g.all { it in 1..9 } && units.all { unit -> unit.map { g[it] }.toSet().size == 9 }
}
