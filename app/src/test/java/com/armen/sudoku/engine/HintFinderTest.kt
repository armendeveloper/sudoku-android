package com.armen.sudoku.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HintFinderTest {

    private val solution = grid(
        "534678912672195348198342567859761423426853791713924856961537284287419635345286179",
    )

    @Test
    fun pointsOutMistakeFirst() {
        val current = solution.copyOf().also { it[0] = 0; it[10] = 1 } // cell 10 should be 7
        val hint = HintFinder.find(current, solution)!!
        assertEquals(HintKind.FIX_MISTAKE, hint.kind)
        assertEquals(10, hint.cell)
        assertEquals(7, hint.value)
    }

    @Test
    fun findsNakedSingleInSelectedCell() {
        val current = solution.copyOf().also { it[40] = 0 }
        val hint = HintFinder.find(current, solution, selected = 40)!!
        assertEquals(HintKind.NAKED_SINGLE, hint.kind)
        assertEquals(40, hint.cell)
        assertEquals(solution[40], hint.value)
    }

    @Test
    fun hintValueAlwaysMatchesSolution() {
        val current = grid("530070000600195000098000060800060003400803001700020006060000280000419005000080079")
        repeat(51) {
            val hint = HintFinder.find(current, solution)!!
            assertEquals(solution[hint.cell], hint.value)
            current[hint.cell] = hint.value
        }
        assertNull(HintFinder.find(current, solution))
    }

    @Test
    fun hintsSolveHardPuzzlesAndExplainEliminations() {
        val random = kotlin.random.Random(5)
        var explained = 0
        repeat(30) {
            val (puzzle, answer) = minimalPuzzle(random)
            val current = puzzle.copyOf()
            while (true) {
                val hint = HintFinder.find(current, answer) ?: break
                assertEquals(answer[hint.cell], hint.value)
                assertEquals(0, current[hint.cell], "hints only fill empty cells")
                if (hint.steps.isNotEmpty()) {
                    explained++
                    assertTrue(hint.patternCells.isNotEmpty())
                }
                current[hint.cell] = hint.value
            }
            assertTrue(current.contentEquals(answer))
        }
        assertTrue(explained > 0, "some hints should need eliminations")
    }
}
