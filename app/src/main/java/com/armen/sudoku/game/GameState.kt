package com.armen.sudoku.game

import androidx.compose.runtime.Immutable
import com.armen.sudoku.engine.Difficulty
import com.armen.sudoku.engine.Hint

/** LOST means the player hit the mistake limit. */
enum class GameStatus { PLAYING, WON, AUTO_SOLVED, LOST }

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
    /** Epoch day of the daily challenge this game is, or null for a regular game. */
    val dailyDay: Long? = null,
    /** True once the player has made a move, so the game counts toward statistics. */
    val counted: Boolean = false,
) {
    fun isGiven(cell: Int) = givens[cell] != 0
    fun isWrong(cell: Int) = values[cell] != 0 && values[cell] != solution[cell]
    fun placedCount(digit: Int) = values.count { it == digit }
    val isComplete get() = values.contentEquals(solution)
    val isDaily get() = dailyDay != null
}

/** Extra results shown in the win dialog. */
@Immutable
data class WinSummary(val newBestTime: Boolean, val streak: Int, val dailyStreak: Int?)

/** A one-off message shown above the controls. */
enum class Notice { BOARD_FULL_WRONG }

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
    val canRedo: Boolean = false,
    val notice: Notice? = null,
    val winSummary: WinSummary? = null,
) {
    /** Board input is allowed only while a game is actively being played. */
    val interactive get() = game?.status == GameStatus.PLAYING && !paused && !generating && !autoSolving
}

sealed interface GameEvent {
    data object Correct : GameEvent
    data object Wrong : GameEvent
    data object Lost : GameEvent

    /** Rows, columns or boxes just finished by a move in [origin]; [cells] is every cell in them. */
    data class Completed(val cells: Set<Int>, val origin: Int) : GameEvent

    data class Won(val origin: Int) : GameEvent
}
