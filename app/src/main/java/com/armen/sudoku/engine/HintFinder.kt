package com.armen.sudoku.engine

enum class HintKind { FIX_MISTAKE, NAKED_SINGLE, HIDDEN_SINGLE, REVEAL }

/**
 * A suggested move: put [value] in [cell]. The UI turns [kind] and the extra fields into a
 * localized explanation: [wrongValue] for FIX_MISTAKE, [unit] and [unitIndex] (0-based) for HIDDEN_SINGLE.
 * [steps] are the eliminations (pairs, pointing, X-Wing…) that had to happen before this move was possible.
 */
data class Hint(
    val cell: Int,
    val value: Int,
    val kind: HintKind,
    val wrongValue: Int = 0,
    val unit: HintUnit? = null,
    val unitIndex: Int = 0,
    val steps: List<Step> = emptyList(),
) {
    /** Cells that make up the patterns in [steps], for highlighting on the board. */
    val patternCells: Set<Int> get() = steps.flatMapTo(HashSet()) { it.cells }
}

/**
 * Finds the most helpful next move, in this order:
 * 1. a wrong entry the player should fix,
 * 2. a single (only one number fits a cell, or a number has only one spot) in the selected cell,
 * 3. the simplest logical move anywhere, including the eliminations it needs,
 * 4. otherwise, reveals the empty cell with the fewest options.
 */
object HintFinder {

    fun find(current: IntArray, solution: IntArray, selected: Int? = null): Hint? {
        val mistake = (0 until Sudoku.CELLS).firstOrNull { current[it] != 0 && current[it] != solution[it] }
        if (mistake != null) {
            return Hint(mistake, solution[mistake], HintKind.FIX_MISTAKE, wrongValue = current[mistake])
        }

        val empty = (0 until Sudoku.CELLS).filter { current[it] == 0 }
        if (empty.isEmpty()) return null

        selected?.takeIf { current[it] == 0 }?.let { sel -> singleAt(current, sel)?.let { return it } }

        LogicSolver.nextPlacement(current)?.let { d ->
            val p = d.placement
            val house = p.houses.firstOrNull()
            return Hint(
                cell = p.cell,
                value = p.value,
                kind = if (p.technique == Technique.NAKED_SINGLE) HintKind.NAKED_SINGLE else HintKind.HIDDEN_SINGLE,
                unit = house?.type,
                unitIndex = house?.index ?: 0,
                steps = d.steps,
            )
        }

        val cell = selected?.takeIf { current[it] == 0 }
            ?: empty.minBy { Integer.bitCount(Sudoku.candidates(current, it)) }
        return Hint(cell, solution[cell], HintKind.REVEAL)
    }

    /** A naked or hidden single in [cell] that needs no eliminations, if there is one. */
    private fun singleAt(current: IntArray, cell: Int): Hint? {
        val mask = Sudoku.candidates(current, cell)
        if (Integer.bitCount(mask) == 1) {
            return Hint(cell, Integer.numberOfTrailingZeros(mask), HintKind.NAKED_SINGLE)
        }
        val houses = listOf(
            House(HintUnit.ROW, Sudoku.row(cell)),
            House(HintUnit.COLUMN, Sudoku.col(cell)),
            House(HintUnit.BOX, Sudoku.box(cell)),
        )
        for (d in Sudoku.digitsOf(mask)) {
            val bit = 1 shl d
            for (house in houses) {
                if (house.cells.none { it != cell && Sudoku.candidates(current, it) and bit != 0 }) {
                    return Hint(cell, d, HintKind.HIDDEN_SINGLE, unit = house.type, unitIndex = house.index)
                }
            }
        }
        return null
    }
}
