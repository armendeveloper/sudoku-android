package com.armen.sudoku.engine

import kotlin.random.Random

enum class Difficulty(val targetClues: Int) {
    EASY(40),
    MEDIUM(33),
    HARD(28),
    EXPERT(24),
}

class Puzzle(val givens: IntArray, val solution: IntArray, val difficulty: Difficulty)

/**
 * Builds a random full solution, then removes cells in symmetric pairs as long as the
 * puzzle keeps exactly one solution, until the difficulty's clue count is reached.
 */
class Generator(private val random: Random = Random.Default) {

    fun generate(difficulty: Difficulty): Puzzle {
        val solution = Solver.solve(IntArray(Sudoku.CELLS), random)
            ?: error("An empty grid always has a solution")
        val puzzle = solution.copyOf()
        var clues = Sudoku.CELLS

        // Cells 0..40 paired with their 180° rotation (cell 40 is the centre and pairs with itself).
        for (cell in (0..40).shuffled(random)) {
            if (clues <= difficulty.targetClues) break
            val mirror = Sudoku.CELLS - 1 - cell
            val a = puzzle[cell]
            val b = puzzle[mirror]
            puzzle[cell] = 0
            puzzle[mirror] = 0
            if (Solver.countSolutions(puzzle, limit = 2) == 1) {
                clues -= if (cell == mirror) 1 else 2
            } else {
                puzzle[cell] = a
                puzzle[mirror] = b
            }
        }
        return Puzzle(puzzle, solution, difficulty)
    }
}
