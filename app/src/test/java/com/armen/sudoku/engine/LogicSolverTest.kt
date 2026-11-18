package com.armen.sudoku.engine

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LogicSolverTest {

    private val classic = grid("530070000600195000098000060800060003400803001700020006060000280000419005000080079")
    private val classicSolution = grid("534678912672195348198342567859761423426853791713924856961537284287419635345286179")

    @Test
    fun solvesClassicPuzzleWithSinglesOnly() {
        val result = LogicSolver.solve(classic, maxTier = 0)
        assertTrue(result.solved)
        assertEquals(0, result.hardestTier)
        assertContentEquals(classicSolution, result.grid)
    }

    @Test
    fun fullGridNeedsNothing() {
        val result = LogicSolver.solve(classicSolution)
        assertTrue(result.solved)
        assertEquals(-1, result.hardestTier)
    }

    /**
     * Runs the solver on many hard random puzzles and checks every single step against the real
     * solution: placements must be right and eliminations must never remove the right number.
     * Also checks that every technique was used at least once, so each one is covered.
     */
    @Test
    fun everyDeductionIsSoundAndEveryTechniqueIsUsed() {
        val used = HashSet<Technique>()
        val random = Random(2024)
        repeat(150) {
            val (puzzle, solution) = minimalPuzzle(random)
            val board = LogicSolver.Board(puzzle)
            while (board.emptyCount > 0) {
                val step = board.findStep(LogicSolver.MAX_TIER) ?: break
                used += step.technique
                if (step.isPlacement) {
                    assertEquals(solution[step.cell], step.value, "${step.technique} placed a wrong number")
                }
                for (code in step.eliminations) {
                    val cell = code / 10
                    val digit = code % 10
                    assertTrue(solution[cell] != digit, "${step.technique} removed the right answer $digit from cell $cell")
                    assertTrue(board.cand[cell] and (1 shl digit) != 0, "${step.technique} removed a candidate that was already gone")
                }
                assertTrue(step.isPlacement || step.eliminations.isNotEmpty(), "${step.technique} made no progress")
                board.apply(step)
            }
        }
        // Swordfish is rare in random puzzles, so it has its own test below.
        val expected = Technique.entries.toSet() - Technique.SWORDFISH
        assertTrue(used.containsAll(expected), "Techniques never used: ${expected - used}")
    }

    @Test
    fun findsSwordfish() {
        // Empty board where, in rows 1, 5 and 9 (1-based), 5 fits only in columns 2, 5 and 8. Other rows allow 5 anywhere.
        val board = LogicSolver.Board(IntArray(81))
        val bit = 1 shl 5
        fun only(row: Int, vararg cols: Int) {
            for (c in 0 until 9) {
                val cell = Sudoku.cellAt(row, c)
                board.cand[cell] = if (c in cols) board.cand[cell] or bit else board.cand[cell] and bit.inv()
            }
        }
        only(0, 1, 4)
        only(4, 4, 7)
        only(8, 1, 7)

        val step = assertNotNull(board.findStep(LogicSolver.MAX_TIER))
        assertEquals(Technique.SWORDFISH, step.technique)
        assertEquals(listOf(5), step.digits)
        assertEquals(listOf(0, 4, 8), step.houses.map { it.index })
        assertEquals(listOf(1, 4, 7), step.targets.map { it.index })
        val expected = listOf(1, 2, 3, 5, 6, 7).flatMap { r -> listOf(1, 4, 7).map { c -> Sudoku.cellAt(r, c) * 10 + 5 } }
        assertEquals(expected.sorted(), step.eliminations.sorted())
    }

    /**
     * For hints, the solver keeps only the eliminations a placement needs. Replaying just those must
     * still make every kept pattern valid and force the placement.
     */
    @Test
    fun hintStepsAreSelfContained() {
        val random = Random(77)
        var withSteps = 0
        repeat(80) {
            val (puzzle, solution) = minimalPuzzle(random)
            val current = puzzle.copyOf()
            while (current.any { it == 0 }) {
                val deduction = LogicSolver.nextPlacement(current) ?: break
                val board = LogicSolver.Board(current)
                for (step in deduction.steps) {
                    assertTrue(patternHolds(board, step), "${step.technique} pattern does not hold after pruning")
                    board.apply(step)
                }
                val p = deduction.placement
                assertEquals(solution[p.cell], p.value)
                val forced = when (p.technique) {
                    Technique.NAKED_SINGLE -> board.cand[p.cell] == 1 shl p.value
                    else -> p.houses[0].cells.filter { board.cand[it] and (1 shl p.value) != 0 } == listOf(p.cell)
                }
                assertTrue(forced, "Placement is not forced by the kept steps")
                if (deduction.steps.isNotEmpty()) withSteps++
                current[p.cell] = p.value
            }
        }
        assertTrue(withSteps > 20, "Expected many hints that need eliminations, got $withSteps")
    }

    private fun patternHolds(board: LogicSolver.Board, step: Step): Boolean {
        fun has(cell: Int, d: Int) = board.cand[cell] and (1 shl d) != 0
        val mask = step.digits.fold(0) { acc, d -> acc or (1 shl d) }
        return when (step.technique) {
            Technique.NAKED_SINGLE, Technique.HIDDEN_SINGLE -> true
            Technique.POINTING, Technique.CLAIMING, Technique.X_WING, Technique.SWORDFISH ->
                step.houses.all { h -> h.cells.filter { has(it, step.digits[0]) }.all { c -> step.targets.any { c in it } } }
            Technique.NAKED_PAIR, Technique.NAKED_TRIPLE ->
                step.cells.fold(0) { acc, c -> acc or board.cand[c] } == mask && step.cells.size == step.digits.size
            Technique.HIDDEN_PAIR, Technique.HIDDEN_TRIPLE ->
                step.digits.all { d -> step.houses[0].cells.filter { has(it, d) }.all { it in step.cells } }
            Technique.XY_WING -> {
                val (x, y, z) = step.digits
                val (pivot, a, b) = step.cells
                board.cand[pivot] == (1 shl x) or (1 shl y) &&
                    board.cand[a] == (1 shl x) or (1 shl z) &&
                    board.cand[b] == (1 shl y) or (1 shl z)
            }
        }
    }

    @Test
    fun nextPlacementOnClassicIsASingle() {
        val deduction = assertNotNull(LogicSolver.nextPlacement(classic))
        assertTrue(deduction.steps.isEmpty())
        assertEquals(classicSolution[deduction.placement.cell], deduction.placement.value)
    }
}

/**
 * A random puzzle with as few clues as possible while keeping a unique solution, ignoring how hard
 * it is. These often need advanced techniques, which makes them good test material.
 */
internal fun minimalPuzzle(random: Random): Pair<IntArray, IntArray> {
    val solution = Solver.solve(IntArray(81), random)!!
    val puzzle = solution.copyOf()
    for (cell in (0 until 81).shuffled(random)) {
        val v = puzzle[cell]
        puzzle[cell] = 0
        if (Solver.countSolutions(puzzle, 2) != 1) puzzle[cell] = v
    }
    return puzzle to solution
}
