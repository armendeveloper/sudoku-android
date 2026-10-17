package com.armen.sudoku.engine

/**
 * Grid geometry shared by the solver, generator and hint finder.
 * A grid is an IntArray of 81 cells in row-major order; 0 means empty.
 */
object Sudoku {
    const val CELLS = 81

    /** Bits 1..9 set: every digit is still a candidate. */
    const val ALL_DIGITS = 0b11_1111_1110

    fun row(cell: Int) = cell / 9
    fun col(cell: Int) = cell % 9
    fun box(cell: Int) = (cell / 27) * 3 + (cell % 9) / 3
    fun cellAt(row: Int, col: Int) = row * 9 + col

    /** The 20 cells that share a row, column or box with each cell. */
    val PEERS: Array<IntArray> = Array(CELLS) { i ->
        (0 until CELLS).filter { j ->
            j != i && (row(j) == row(i) || col(j) == col(i) || box(j) == box(i))
        }.toIntArray()
    }

    val ROWS: List<IntArray> = List(9) { r -> IntArray(9) { c -> cellAt(r, c) } }
    val COLS: List<IntArray> = List(9) { c -> IntArray(9) { r -> cellAt(r, c) } }
    val BOXES: List<IntArray> = List(9) { b ->
        IntArray(9) { k -> cellAt((b / 3) * 3 + k / 3, (b % 3) * 3 + k % 3) }
    }

    /** Bitmask of digits that can legally go in [cell], given the digits already on [grid]. */
    fun candidates(grid: IntArray, cell: Int): Int {
        if (grid[cell] != 0) return 0
        var used = 0
        for (p in PEERS[cell]) used = used or (1 shl grid[p])
        return ALL_DIGITS and used.inv()
    }

    fun digitsOf(mask: Int): List<Int> = (1..9).filter { mask and (1 shl it) != 0 }
}
