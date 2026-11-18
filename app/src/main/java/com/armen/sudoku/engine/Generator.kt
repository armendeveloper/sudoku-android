package com.armen.sudoku.engine

import kotlin.random.Random

/**
 * Difficulty is set by the hardest technique a puzzle needs (see [Technique.tier]), not just by
 * how many clues it has. [minClues] is where the generator stops removing clues.
 */
enum class Difficulty(val minClues: Int, val minTier: Int, val maxTier: Int) {
    /** Singles only, with plenty of clues. */
    EASY(minClues = 38, minTier = 0, maxTier = 0),

    /** Singles only, but fewer clues, so they are harder to spot. */
    MEDIUM(minClues = 30, minTier = 0, maxTier = 0),

    /** Needs at least one pointing, claiming, naked pair or hidden pair. */
    HARD(minClues = 24, minTier = 1, maxTier = 1),

    /** Needs at least one triple, X-Wing, XY-Wing or Swordfish. */
    EXPERT(minClues = 22, minTier = 2, maxTier = 2),
}

class Puzzle(val givens: IntArray, val solution: IntArray, val difficulty: Difficulty)

/**
 * Builds a random full solution, then removes cells in symmetric pairs as long as the puzzle can
 * still be solved by logic alone using techniques up to the difficulty's tier. Solving by logic
 * also proves the solution is unique. If the result doesn't need a hard enough technique, it tries
 * again with a new solution.
 *
 * The output depends only on [random], so the same seed always gives the same puzzle.
 */
class Generator(private val random: Random = Random.Default) {

    fun generate(difficulty: Difficulty): Puzzle {
        var best: Puzzle? = null
        var bestTier = -2
        repeat(MAX_ATTEMPTS) {
            val (puzzle, tier) = attempt(difficulty)
            if (tier >= difficulty.minTier) return puzzle
            if (tier > bestTier) {
                best = puzzle
                bestTier = tier
            }
        }
        // Rare: no attempt needed the target technique. Hand back the hardest one found.
        return best!!
    }

    private fun attempt(difficulty: Difficulty): Pair<Puzzle, Int> {
        val solution = Solver.solve(IntArray(Sudoku.CELLS), random)
            ?: error("An empty grid always has a solution")
        val puzzle = solution.copyOf()
        var clues = Sudoku.CELLS

        // Cells 0..40 paired with their 180° rotation (cell 40 is the centre and pairs with itself).
        for (cell in (0..40).shuffled(random)) {
            if (clues <= difficulty.minClues) break
            val mirror = Sudoku.CELLS - 1 - cell
            val a = puzzle[cell]
            val b = puzzle[mirror]
            puzzle[cell] = 0
            puzzle[mirror] = 0
            if (LogicSolver.solve(puzzle, difficulty.maxTier).solved) {
                clues -= if (cell == mirror) 1 else 2
            } else {
                puzzle[cell] = a
                puzzle[mirror] = b
            }
        }
        val tier = LogicSolver.solve(puzzle, difficulty.maxTier).hardestTier
        return Puzzle(puzzle, solution, difficulty) to tier
    }

    private companion object {
        const val MAX_ATTEMPTS = 60
    }
}
