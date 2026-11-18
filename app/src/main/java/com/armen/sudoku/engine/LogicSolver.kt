package com.armen.sudoku.engine

/**
 * Solving techniques a person can use, from simplest to hardest. [tier] groups them for grading:
 * 0 = singles, 1 = intermediate, 2 = advanced.
 */
enum class Technique(val tier: Int) {
    NAKED_SINGLE(0),
    HIDDEN_SINGLE(0),
    POINTING(1),
    CLAIMING(1),
    NAKED_PAIR(1),
    HIDDEN_PAIR(1),
    NAKED_TRIPLE(2),
    HIDDEN_TRIPLE(2),
    X_WING(2),
    XY_WING(2),
    SWORDFISH(2),
}

enum class HintUnit { ROW, COLUMN, BOX }

/** A row, column or box. [index] is 0-based. */
data class House(val type: HintUnit, val index: Int) {
    val cells: IntArray
        get() = when (type) {
            HintUnit.ROW -> Sudoku.ROWS[index]
            HintUnit.COLUMN -> Sudoku.COLS[index]
            HintUnit.BOX -> Sudoku.BOXES[index]
        }

    operator fun contains(cell: Int): Boolean = when (type) {
        HintUnit.ROW -> Sudoku.row(cell) == index
        HintUnit.COLUMN -> Sudoku.col(cell) == index
        HintUnit.BOX -> Sudoku.box(cell) == index
    }
}

/**
 * One logical deduction.
 *
 * Singles place [value] in [cell]. Every other technique only removes candidates, listed in
 * [eliminations] (each encoded as `cell * 10 + digit`).
 *
 * [cells] are the cells that form the pattern, [digits] the numbers it is about. [houses] are where
 * the pattern is found and [targets] are where candidates get removed. For example, a pointing pair
 * has the box in [houses] and the row or column in [targets]. An XY-Wing lists its pivot first in
 * [cells], and [digits] holds x, y and z, where the pivot is x/y, the pincers are x/z and y/z, and
 * z is removed.
 */
data class Step(
    val technique: Technique,
    val digits: List<Int>,
    val cells: List<Int>,
    val houses: List<House> = emptyList(),
    val targets: List<House> = emptyList(),
    val eliminations: List<Int> = emptyList(),
    val cell: Int = -1,
    val value: Int = 0,
) {
    val isPlacement get() = technique == Technique.NAKED_SINGLE || technique == Technique.HIDDEN_SINGLE

    /**
     * True when this step only holds because [digit] was already ruled out of [cell].
     * Used to drop deductions a hint does not need.
     */
    internal fun dependsOn(cell: Int, digit: Int): Boolean = when (technique) {
        Technique.NAKED_SINGLE -> cell == this.cell && digit != value
        Technique.HIDDEN_SINGLE -> digit == value && cell != this.cell && cell in houses[0]
        Technique.NAKED_PAIR, Technique.NAKED_TRIPLE, Technique.XY_WING -> cell in cells
        Technique.HIDDEN_PAIR, Technique.HIDDEN_TRIPLE ->
            digit in digits && cell !in cells && cell in houses[0]
        Technique.POINTING, Technique.CLAIMING, Technique.X_WING, Technique.SWORDFISH ->
            digit == digits[0] && houses.any { cell in it } && targets.none { cell in it }
    }
}

/** A placement a person can find next, and the eliminations they need to make first (often none). */
class Deduction(val placement: Step, val steps: List<Step>)

/**
 * Solves the way a person would: always uses the simplest technique that makes progress.
 * Every step is a sound deduction, so a puzzle this solves completely has exactly one solution.
 */
object LogicSolver {

    const val MAX_TIER = 2

    /** Rows, columns and boxes; boxes first because people scan them first. */
    val HOUSES: List<House> =
        List(9) { House(HintUnit.BOX, it) } + List(9) { House(HintUnit.ROW, it) } + List(9) { House(HintUnit.COLUMN, it) }

    private val LINES = HOUSES.filter { it.type != HintUnit.BOX }

    class Result(val solved: Boolean, val hardestTier: Int, val grid: IntArray, val steps: List<Step>)

    /**
     * Solves [grid] using techniques up to [maxTier]. [Result.hardestTier] is the tier of the hardest
     * technique needed (-1 if the grid was already full).
     */
    fun solve(grid: IntArray, maxTier: Int = MAX_TIER, recordSteps: Boolean = false): Result {
        val board = Board(grid)
        val steps = ArrayList<Step>()
        var hardest = -1
        while (board.emptyCount > 0) {
            val step = board.findStep(maxTier) ?: break
            board.apply(step)
            hardest = maxOf(hardest, step.technique.tier)
            if (recordSteps) steps += step
        }
        return Result(board.emptyCount == 0, hardest, board.values, steps)
    }

    /**
     * The next number a person can place on [grid], with only the eliminations that placement relies on.
     * Returns null when [grid] is full or needs techniques beyond [maxTier].
     */
    fun nextPlacement(grid: IntArray, maxTier: Int = MAX_TIER): Deduction? {
        val board = Board(grid)
        val eliminations = ArrayList<Step>()
        while (board.emptyCount > 0) {
            val step = board.findStep(maxTier) ?: return null
            if (step.isPlacement) return Deduction(step, relevant(eliminations, step))
            board.apply(step)
            eliminations += step
        }
        return null
    }

    /** Keeps only the eliminations that [placement] needs, directly or through other kept steps. */
    private fun relevant(steps: List<Step>, placement: Step): List<Step> {
        val needed = mutableListOf(placement)
        val keep = BooleanArray(steps.size)
        for (i in steps.indices.reversed()) {
            val step = steps[i]
            val used = step.eliminations.any { code -> needed.any { it.dependsOn(code / 10, code % 10) } }
            if (used) {
                keep[i] = true
                needed += step
            }
        }
        return steps.filterIndexed { i, _ -> keep[i] }
    }

    /** A grid plus a candidate bitmask per cell (bit d set = d still possible). */
    internal class Board(grid: IntArray) {
        val values = grid.copyOf()
        val cand = IntArray(Sudoku.CELLS) { Sudoku.candidates(values, it) }
        var emptyCount = values.count { it == 0 }
            private set

        fun apply(step: Step) {
            if (step.isPlacement) place(step.cell, step.value)
            for (code in step.eliminations) cand[code / 10] = cand[code / 10] and (1 shl (code % 10)).inv()
        }

        private fun place(cell: Int, digit: Int) {
            values[cell] = digit
            cand[cell] = 0
            emptyCount--
            val mask = (1 shl digit).inv()
            for (p in Sudoku.PEERS[cell]) cand[p] = cand[p] and mask
        }

        fun findStep(maxTier: Int): Step? {
            nakedSingle()?.let { return it }
            hiddenSingle()?.let { return it }
            if (maxTier < 1) return null
            pointing()?.let { return it }
            claiming()?.let { return it }
            nakedSubset(2)?.let { return it }
            hiddenSubset(2)?.let { return it }
            if (maxTier < 2) return null
            nakedSubset(3)?.let { return it }
            hiddenSubset(3)?.let { return it }
            fish(2)?.let { return it }
            xyWing()?.let { return it }
            fish(3)?.let { return it }
            return null
        }

        private fun has(cell: Int, digit: Int) = cand[cell] and (1 shl digit) != 0

        private fun nakedSingle(): Step? {
            for (cell in 0 until Sudoku.CELLS) {
                if (values[cell] == 0 && Integer.bitCount(cand[cell]) == 1) {
                    val d = Integer.numberOfTrailingZeros(cand[cell])
                    return Step(Technique.NAKED_SINGLE, listOf(d), listOf(cell), cell = cell, value = d)
                }
            }
            return null
        }

        private fun hiddenSingle(): Step? {
            for (house in HOUSES) {
                val cells = house.cells
                for (d in 1..9) {
                    var spot = -1
                    var count = 0
                    for (c in cells) if (has(c, d)) { spot = c; count++ }
                    if (count == 1) {
                        return Step(Technique.HIDDEN_SINGLE, listOf(d), listOf(spot), houses = listOf(house), cell = spot, value = d)
                    }
                }
            }
            return null
        }

        /** In a box, every spot for a digit lies on one line, so the digit leaves the rest of that line. */
        private fun pointing(): Step? {
            for (b in 0 until 9) {
                val box = House(HintUnit.BOX, b)
                for (d in 1..9) {
                    val spots = box.cells.filter { has(it, d) }
                    if (spots.size < 2) continue
                    val line = when {
                        spots.all { Sudoku.row(it) == Sudoku.row(spots[0]) } -> House(HintUnit.ROW, Sudoku.row(spots[0]))
                        spots.all { Sudoku.col(it) == Sudoku.col(spots[0]) } -> House(HintUnit.COLUMN, Sudoku.col(spots[0]))
                        else -> continue
                    }
                    val elims = line.cells.filter { it !in box && has(it, d) }.map { it * 10 + d }
                    if (elims.isNotEmpty()) {
                        return Step(Technique.POINTING, listOf(d), spots, listOf(box), listOf(line), elims)
                    }
                }
            }
            return null
        }

        /** On a line, every spot for a digit lies in one box, so the digit leaves the rest of that box. */
        private fun claiming(): Step? {
            for (line in LINES) {
                for (d in 1..9) {
                    val spots = line.cells.filter { has(it, d) }
                    if (spots.size < 2 || spots.any { Sudoku.box(it) != Sudoku.box(spots[0]) }) continue
                    val box = House(HintUnit.BOX, Sudoku.box(spots[0]))
                    val elims = box.cells.filter { it !in line && has(it, d) }.map { it * 10 + d }
                    if (elims.isNotEmpty()) {
                        return Step(Technique.CLAIMING, listOf(d), spots, listOf(line), listOf(box), elims)
                    }
                }
            }
            return null
        }

        /** [n] cells in a house that together allow only [n] digits: those digits leave the house's other cells. */
        private fun nakedSubset(n: Int): Step? {
            val technique = if (n == 2) Technique.NAKED_PAIR else Technique.NAKED_TRIPLE
            for (house in HOUSES) {
                val open = house.cells.filter { values[it] == 0 && Integer.bitCount(cand[it]) in 2..n }
                for (combo in combinations(open.size, n)) {
                    val cells = combo.map { open[it] }
                    val union = cells.fold(0) { acc, c -> acc or cand[c] }
                    if (Integer.bitCount(union) != n) continue
                    val elims = house.cells
                        .filter { it !in cells && values[it] == 0 }
                        .flatMap { c -> Sudoku.digitsOf(cand[c] and union).map { c * 10 + it } }
                    if (elims.isNotEmpty()) {
                        return Step(technique, Sudoku.digitsOf(union), cells, listOf(house), listOf(house), elims)
                    }
                }
            }
            return null
        }

        /** [n] digits that fit in only the same [n] cells of a house: those cells can't hold anything else. */
        private fun hiddenSubset(n: Int): Step? {
            val technique = if (n == 2) Technique.HIDDEN_PAIR else Technique.HIDDEN_TRIPLE
            for (house in HOUSES) {
                val cells = house.cells
                // Bitmask of positions (0..8 within the house) where each digit can go.
                val where = IntArray(10) { d ->
                    if (d == 0) 0 else cells.indices.fold(0) { acc, k -> if (has(cells[k], d)) acc or (1 shl k) else acc }
                }
                val digits = (1..9).filter { Integer.bitCount(where[it]) in 2..n }
                for (combo in combinations(digits.size, n)) {
                    val ds = combo.map { digits[it] }
                    val union = ds.fold(0) { acc, d -> acc or where[d] }
                    if (Integer.bitCount(union) != n) continue
                    val keepMask = ds.fold(0) { acc, d -> acc or (1 shl d) }
                    val spots = (0 until 9).filter { union and (1 shl it) != 0 }.map { cells[it] }
                    val elims = spots.flatMap { c -> Sudoku.digitsOf(cand[c] and keepMask.inv()).map { c * 10 + it } }
                    if (elims.isNotEmpty()) {
                        return Step(technique, ds, spots, listOf(house), listOf(house), elims)
                    }
                }
            }
            return null
        }

        /**
         * X-Wing (n = 2) and Swordfish (n = 3): in [n] rows a digit fits only in the same [n] columns,
         * so it leaves the rest of those columns (and the same with rows and columns swapped).
         */
        private fun fish(n: Int): Step? {
            val technique = if (n == 2) Technique.X_WING else Technique.SWORDFISH
            for (byRows in listOf(true, false)) {
                val baseType = if (byRows) HintUnit.ROW else HintUnit.COLUMN
                val coverType = if (byRows) HintUnit.COLUMN else HintUnit.ROW
                for (d in 1..9) {
                    // For each base line, a bitmask of the cross positions where d fits.
                    val where = IntArray(9) { line ->
                        val cells = House(baseType, line).cells
                        cells.indices.fold(0) { acc, k -> if (has(cells[k], d)) acc or (1 shl k) else acc }
                    }
                    val lines = (0 until 9).filter { Integer.bitCount(where[it]) in 2..n }
                    for (combo in combinations(lines.size, n)) {
                        val base = combo.map { lines[it] }
                        val union = base.fold(0) { acc, l -> acc or where[l] }
                        if (Integer.bitCount(union) != n) continue
                        val cover = (0 until 9).filter { union and (1 shl it) != 0 }
                        val baseHouses = base.map { House(baseType, it) }
                        val coverHouses = cover.map { House(coverType, it) }
                        val elims = coverHouses.flatMap { h -> h.cells.filter { c -> baseHouses.none { c in it } && has(c, d) } }
                            .map { it * 10 + d }
                        if (elims.isNotEmpty()) {
                            val spots = baseHouses.flatMap { h -> h.cells.filter { has(it, d) } }
                            return Step(technique, listOf(d), spots, baseHouses, coverHouses, elims)
                        }
                    }
                }
            }
            return null
        }

        /**
         * XY-Wing: a pivot with x/y sees one cell with x/z and another with y/z. Whichever the pivot
         * turns out to be, one of those two cells is z, so z leaves every cell that sees both.
         */
        private fun xyWing(): Step? {
            for (pivot in 0 until Sudoku.CELLS) {
                if (values[pivot] != 0 || Integer.bitCount(cand[pivot]) != 2) continue
                val (x, y) = Sudoku.digitsOf(cand[pivot])
                val wings = Sudoku.PEERS[pivot].filter { values[it] == 0 && Integer.bitCount(cand[it]) == 2 }
                for (a in wings) {
                    if (!has(a, x) || has(a, y)) continue
                    val z = Integer.numberOfTrailingZeros(cand[a] and (1 shl x).inv())
                    for (b in wings) {
                        if (cand[b] != (1 shl y) or (1 shl z)) continue
                        val elims = Sudoku.PEERS[a]
                            .filter { it != b && it != pivot && it in Sudoku.PEERS[b] && has(it, z) }
                            .map { it * 10 + z }
                        if (elims.isNotEmpty()) {
                            return Step(Technique.XY_WING, listOf(x, y, z), listOf(pivot, a, b), eliminations = elims)
                        }
                    }
                }
            }
            return null
        }
    }

    /** All ways to choose [k] (2 or 3) of [n] indices, in increasing order. */
    private fun combinations(n: Int, k: Int): Sequence<IntArray> = sequence {
        if (k == 2) {
            for (i in 0 until n) for (j in i + 1 until n) yield(intArrayOf(i, j))
        } else {
            for (i in 0 until n) for (j in i + 1 until n) for (l in j + 1 until n) yield(intArrayOf(i, j, l))
        }
    }
}
