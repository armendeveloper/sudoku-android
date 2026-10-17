package com.armen.sudoku.engine

import kotlin.random.Random

/**
 * Backtracking solver using bitmasks and the "fewest candidates first" heuristic,
 * which solves any 9x9 puzzle in milliseconds.
 */
object Solver {

    /** Returns a solved copy of [grid], or null if it has no solution. Pass [random] to get a random solution. */
    fun solve(grid: IntArray, random: Random? = null): IntArray? {
        val state = State(grid)
        if (!state.valid) return null
        val found = ArrayList<IntArray>(1)
        search(state, random, limit = 1, found)
        return found.firstOrNull()
    }

    /** Counts solutions, stopping at [limit]. A proper puzzle returns exactly 1. */
    fun countSolutions(grid: IntArray, limit: Int = 2): Int {
        val state = State(grid)
        if (!state.valid) return 0
        val found = ArrayList<IntArray>(limit)
        search(state, null, limit, found)
        return found.size
    }

    /** Returns true when enough solutions have been found and the search should stop. */
    private fun search(s: State, random: Random?, limit: Int, found: MutableList<IntArray>): Boolean {
        var best = -1
        var bestCount = 10
        var bestMask = 0
        for (i in 0 until Sudoku.CELLS) {
            if (s.cells[i] != 0) continue
            val mask = s.candidates(i)
            val count = Integer.bitCount(mask)
            if (count == 0) return false
            if (count < bestCount) {
                best = i
                bestCount = count
                bestMask = mask
                if (count == 1) break
            }
        }
        if (best == -1) {
            found.add(s.cells.copyOf())
            return found.size >= limit
        }
        val digits = Sudoku.digitsOf(bestMask).let { if (random != null) it.shuffled(random) else it }
        for (d in digits) {
            s.place(best, d)
            val stop = search(s, random, limit, found)
            s.remove(best, d)
            if (stop) return true
        }
        return false
    }

    private class State(grid: IntArray) {
        val cells = grid.copyOf()
        private val rows = IntArray(9)
        private val cols = IntArray(9)
        private val boxes = IntArray(9)
        var valid = true
            private set

        init {
            require(grid.size == Sudoku.CELLS) { "Grid must have 81 cells" }
            for (i in 0 until Sudoku.CELLS) {
                val v = cells[i]
                if (v == 0) continue
                val bit = 1 shl v
                val used = rows[Sudoku.row(i)] or cols[Sudoku.col(i)] or boxes[Sudoku.box(i)]
                if (v !in 1..9 || used and bit != 0) valid = false
                setBits(i, v)
            }
        }

        fun candidates(i: Int): Int =
            Sudoku.ALL_DIGITS and (rows[Sudoku.row(i)] or cols[Sudoku.col(i)] or boxes[Sudoku.box(i)]).inv()

        fun place(i: Int, d: Int) {
            cells[i] = d
            setBits(i, d)
        }

        fun remove(i: Int, d: Int) {
            cells[i] = 0
            val mask = (1 shl d).inv()
            rows[Sudoku.row(i)] = rows[Sudoku.row(i)] and mask
            cols[Sudoku.col(i)] = cols[Sudoku.col(i)] and mask
            boxes[Sudoku.box(i)] = boxes[Sudoku.box(i)] and mask
        }

        private fun setBits(i: Int, d: Int) {
            val bit = 1 shl d
            rows[Sudoku.row(i)] = rows[Sudoku.row(i)] or bit
            cols[Sudoku.col(i)] = cols[Sudoku.col(i)] or bit
            boxes[Sudoku.box(i)] = boxes[Sudoku.box(i)] or bit
        }
    }
}
