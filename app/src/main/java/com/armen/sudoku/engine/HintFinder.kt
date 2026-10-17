package com.armen.sudoku.engine

enum class HintKind { FIX_MISTAKE, NAKED_SINGLE, HIDDEN_SINGLE, REVEAL }

enum class HintUnit { ROW, COLUMN, BOX }

/**
 * A suggested move: put [value] in [cell]. The UI turns [kind] and the extra fields into a
 * localized explanation: [wrongValue] for FIX_MISTAKE, [unit] and [unitIndex] (0-based) for HIDDEN_SINGLE.
 */
data class Hint(
    val cell: Int,
    val value: Int,
    val kind: HintKind,
    val wrongValue: Int = 0,
    val unit: HintUnit? = null,
    val unitIndex: Int = 0,
)

/**
 * Finds the most helpful next move, in this order:
 * 1. a wrong entry the player should fix,
 * 2. a cell where only one number fits (naked single),
 * 3. a number that has only one possible spot in a row, column or box (hidden single),
 * 4. otherwise, reveals the empty cell with the fewest options.
 * The player's selected cell is preferred whenever it qualifies.
 */
object HintFinder {

    fun find(current: IntArray, solution: IntArray, selected: Int? = null): Hint? {
        val mistake = (0 until Sudoku.CELLS).firstOrNull { current[it] != 0 && current[it] != solution[it] }
        if (mistake != null) {
            return Hint(mistake, solution[mistake], HintKind.FIX_MISTAKE, wrongValue = current[mistake])
        }

        val empty = (0 until Sudoku.CELLS).filter { current[it] == 0 }
        if (empty.isEmpty()) return null
        val candidates = IntArray(Sudoku.CELLS) { Sudoku.candidates(current, it) }

        fun nakedSingle(cell: Int): Hint? {
            if (current[cell] != 0 || Integer.bitCount(candidates[cell]) != 1) return null
            return Hint(cell, Integer.numberOfTrailingZeros(candidates[cell]), HintKind.NAKED_SINGLE)
        }

        fun hiddenSingle(cell: Int): Hint? {
            if (current[cell] != 0) return null
            val units = listOf(
                Triple(HintUnit.ROW, Sudoku.row(cell), Sudoku.ROWS[Sudoku.row(cell)]),
                Triple(HintUnit.COLUMN, Sudoku.col(cell), Sudoku.COLS[Sudoku.col(cell)]),
                Triple(HintUnit.BOX, Sudoku.box(cell), Sudoku.BOXES[Sudoku.box(cell)]),
            )
            for (d in Sudoku.digitsOf(candidates[cell])) {
                val bit = 1 shl d
                for ((unit, index, cells) in units) {
                    if (cells.none { it != cell && candidates[it] and bit != 0 }) {
                        return Hint(cell, d, HintKind.HIDDEN_SINGLE, unit = unit, unitIndex = index)
                    }
                }
            }
            return null
        }

        selected?.let { sel -> (nakedSingle(sel) ?: hiddenSingle(sel))?.let { return it } }
        empty.firstNotNullOfOrNull(::nakedSingle)?.let { return it }
        empty.firstNotNullOfOrNull(::hiddenSingle)?.let { return it }

        val cell = selected?.takeIf { current[it] == 0 }
            ?: empty.minBy { Integer.bitCount(candidates[it]) }
        return Hint(cell, solution[cell], HintKind.REVEAL)
    }
}
