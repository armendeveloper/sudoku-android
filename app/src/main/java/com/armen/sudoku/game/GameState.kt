package com.armen.sudoku.game

import androidx.compose.runtime.Immutable
import com.armen.sudoku.engine.Difficulty
import com.armen.sudoku.engine.Hint

enum class GameStatus { PLAYING, WON, AUTO_SOLVED }

/**
 * One puzzle in progress. Arrays are never mutated after construction — every change
 * produces a new GameState with fresh copies, so Compose can treat it as immutable.
 */
@Immutable
@Suppress("ArrayInDataClass")
data class GameState(
    val givens: IntArray,
    val solution: IntArray,
    val values: IntArray,
    /** Pencil marks per cell as a bitmask (bit d set = note d). */
    val notes: IntArray,
    val difficulty: Difficulty,
    val mistakes: Int = 0,
    val hintsUsed: Int = 0,
    val elapsedSeconds: Long = 0,
    val status: GameStatus = GameStatus.PLAYING,
) {
    fun isGiven(cell: Int) = givens[cell] != 0
    fun isWrong(cell: Int) = values[cell] != 0 && values[cell] != solution[cell]
    fun placedCount(digit: Int) = values.count { it == digit }
    val isComplete get() = values.contentEquals(solution)
}

@Immutable
data class UiState(
    val game: GameState? = null,
    val selected: Int? = null,
    val notesMode: Boolean = false,
    val paused: Boolean = false,
    val generating: Boolean = false,
    val autoSolving: Boolean = false,
    val hint: Hint? = null,
    val canUndo: Boolean = false,
) {
    /** Board input is allowed only while a game is actively being played. */
    val interactive get() = game?.status == GameStatus.PLAYING && !paused && !generating && !autoSolving
}

enum class GameEvent { CORRECT, WRONG, WON }
