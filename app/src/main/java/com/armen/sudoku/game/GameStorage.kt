package com.armen.sudoku.game

import android.content.Context
import androidx.core.content.edit
import com.armen.sudoku.engine.Difficulty

/** Saves the current game so the player can close the app and continue later. */
class GameStorage(context: Context) {
    private val prefs = context.getSharedPreferences("sudoku_game", Context.MODE_PRIVATE)

    fun save(game: GameState) = prefs.edit {
        putString(KEY_GIVENS, game.givens.encode())
        putString(KEY_SOLUTION, game.solution.encode())
        putString(KEY_VALUES, game.values.encode())
        putString(KEY_NOTES, game.notes.joinToString(","))
        putString(KEY_DIFFICULTY, game.difficulty.name)
        putInt(KEY_MISTAKES, game.mistakes)
        putInt(KEY_HINTS, game.hintsUsed)
        putLong(KEY_ELAPSED, game.elapsedSeconds)
        putString(KEY_STATUS, game.status.name)
        putLong(KEY_DAILY, game.dailyDay ?: NO_DAILY)
        putBoolean(KEY_COUNTED, game.counted)
    }

    fun load(): GameState? = runCatching {
        GameState(
            givens = prefs.getString(KEY_GIVENS, null)!!.decode(),
            solution = prefs.getString(KEY_SOLUTION, null)!!.decode(),
            values = prefs.getString(KEY_VALUES, null)!!.decode(),
            notes = prefs.getString(KEY_NOTES, null)!!.split(",").map(String::toInt).toIntArray()
                .also { require(it.size == 81) },
            difficulty = Difficulty.valueOf(prefs.getString(KEY_DIFFICULTY, null)!!),
            mistakes = prefs.getInt(KEY_MISTAKES, 0),
            hintsUsed = prefs.getInt(KEY_HINTS, 0),
            elapsedSeconds = prefs.getLong(KEY_ELAPSED, 0),
            status = GameStatus.valueOf(prefs.getString(KEY_STATUS, null)!!),
            // Saves from version 1.0 have neither key, and default to a regular, uncounted game.
            dailyDay = prefs.getLong(KEY_DAILY, NO_DAILY).takeIf { it != NO_DAILY },
            counted = prefs.getBoolean(KEY_COUNTED, false),
        )
    }.getOrNull()

    private fun IntArray.encode() = joinToString("")
    private fun String.decode() = map { it.digitToInt() }.toIntArray().also { require(it.size == 81) }

    private companion object {
        const val KEY_GIVENS = "givens"
        const val KEY_SOLUTION = "solution"
        const val KEY_VALUES = "values"
        const val KEY_NOTES = "notes"
        const val KEY_DIFFICULTY = "difficulty"
        const val KEY_MISTAKES = "mistakes"
        const val KEY_HINTS = "hints"
        const val KEY_ELAPSED = "elapsed"
        const val KEY_STATUS = "status"
        const val KEY_DAILY = "daily_day"
        const val KEY_COUNTED = "counted"
        const val NO_DAILY = Long.MIN_VALUE
    }
}
