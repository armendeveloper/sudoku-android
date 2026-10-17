package com.armen.sudoku.game

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.armen.sudoku.engine.Difficulty
import com.armen.sudoku.engine.Generator
import com.armen.sudoku.engine.HintFinder
import com.armen.sudoku.engine.Sudoku
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GameViewModel(app: Application) : AndroidViewModel(app) {

    private val storage = GameStorage(app)
    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    private val _events = MutableSharedFlow<GameEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<GameEvent> = _events

    private class Snapshot(val values: IntArray, val notes: IntArray, val selected: Int?)
    private val undoStack = ArrayDeque<Snapshot>()
    private var appVisible = true

    init {
        val saved = storage.load()
        if (saved != null && saved.status == GameStatus.PLAYING) {
            _ui.value = UiState(game = saved)
        } else {
            newGame(saved?.difficulty ?: Difficulty.EASY)
        }
        viewModelScope.launch {
            while (true) {
                delay(1_000)
                _ui.update { s ->
                    val g = s.game
                    if (g != null && s.interactive && appVisible) s.copy(game = g.copy(elapsedSeconds = g.elapsedSeconds + 1)) else s
                }
            }
        }
    }

    fun newGame(difficulty: Difficulty) {
        if (_ui.value.generating) return
        _ui.update { it.copy(generating = true, hint = null) }
        viewModelScope.launch {
            val puzzle = withContext(Dispatchers.Default) { Generator().generate(difficulty) }
            undoStack.clear()
            _ui.value = UiState(
                game = GameState(
                    givens = puzzle.givens,
                    solution = puzzle.solution,
                    values = puzzle.givens.copyOf(),
                    notes = IntArray(Sudoku.CELLS),
                    difficulty = difficulty,
                ),
            )
            save()
        }
    }

    fun restart() {
        val g = _ui.value.game ?: return
        undoStack.clear()
        _ui.value = UiState(game = g.copy(values = g.givens.copyOf(), notes = IntArray(Sudoku.CELLS), mistakes = 0, hintsUsed = 0, elapsedSeconds = 0, status = GameStatus.PLAYING))
    }

    fun select(cell: Int) {
        if (!_ui.value.interactive) return
        _ui.update { it.copy(selected = if (it.selected == cell) null else cell) }
    }

    fun moveSelection(dRow: Int, dCol: Int) {
        if (!_ui.value.interactive) return
        _ui.update { s ->
            val cur = s.selected ?: 40
            val r = (Sudoku.row(cur) + dRow + 9) % 9
            val c = (Sudoku.col(cur) + dCol + 9) % 9
            s.copy(selected = Sudoku.cellAt(r, c))
        }
    }

    fun toggleNotesMode() = _ui.update { it.copy(notesMode = !it.notesMode) }

    fun togglePause() {
        val s = _ui.value
        if (s.game?.status != GameStatus.PLAYING || s.autoSolving) return
        _ui.update { it.copy(paused = !it.paused) }
    }

    fun dismissHint() = _ui.update { it.copy(hint = null) }

    fun enterDigit(digit: Int) {
        val s = _ui.value
        val g = s.game ?: return
        val cell = s.selected ?: return
        if (!s.interactive || g.isGiven(cell)) return

        if (s.notesMode) {
            if (g.values[cell] != 0) return
            pushUndo()
            val notes = g.notes.copyOf().also { it[cell] = it[cell] xor (1 shl digit) }
            _ui.update { it.copy(game = g.copy(notes = notes), hint = null) }
            return
        }

        if (g.values[cell] == digit) return
        pushUndo()
        val correct = digit == g.solution[cell]
        val values = g.values.copyOf().also { it[cell] = digit }
        val notes = g.notes.copyOf().also { it[cell] = 0 }
        if (correct) clearNoteFromPeers(notes, cell, digit)
        commit(g.copy(values = values, notes = notes, mistakes = g.mistakes + if (correct) 0 else 1))
        _events.tryEmit(if (correct) GameEvent.CORRECT else GameEvent.WRONG)
    }

    fun erase() {
        val s = _ui.value
        val g = s.game ?: return
        val cell = s.selected ?: return
        if (!s.interactive || g.isGiven(cell)) return
        if (g.values[cell] == 0 && g.notes[cell] == 0) return
        pushUndo()
        val values = g.values.copyOf().also { it[cell] = 0 }
        val notes = g.notes.copyOf().also { it[cell] = 0 }
        _ui.update { it.copy(game = g.copy(values = values, notes = notes), hint = null) }
    }

    fun undo() {
        val s = _ui.value
        val g = s.game ?: return
        if (!s.interactive) return
        val snap = undoStack.removeLastOrNull() ?: return
        _ui.update {
            it.copy(game = g.copy(values = snap.values, notes = snap.notes), selected = snap.selected, canUndo = undoStack.isNotEmpty(), hint = null)
        }
    }

    fun hint() {
        val s = _ui.value
        val g = s.game ?: return
        if (!s.interactive) return
        val hint = HintFinder.find(g.values, g.solution, s.selected) ?: return
        pushUndo()
        val values = g.values.copyOf().also { it[hint.cell] = hint.value }
        val notes = g.notes.copyOf().also { it[hint.cell] = 0 }
        clearNoteFromPeers(notes, hint.cell, hint.value)
        _ui.update { it.copy(selected = hint.cell, hint = hint) }
        commit(g.copy(values = values, notes = notes, hintsUsed = g.hintsUsed + 1), keepHint = true)
    }

    /** Pencils in every legal candidate for every empty cell. */
    fun fillAllNotes() {
        val s = _ui.value
        val g = s.game ?: return
        if (!s.interactive) return
        pushUndo()
        // Ignore wrong entries so they don't hide the real candidates.
        val correctOnly = IntArray(Sudoku.CELLS) { if (g.isWrong(it)) 0 else g.values[it] }
        val notes = IntArray(Sudoku.CELLS) { if (g.values[it] != 0) 0 else Sudoku.candidates(correctOnly, it) }
        _ui.update { it.copy(game = g.copy(notes = notes), hint = null) }
    }

    fun clearAllNotes() {
        val s = _ui.value
        val g = s.game ?: return
        if (!s.interactive || g.notes.all { it == 0 }) return
        pushUndo()
        _ui.update { it.copy(game = g.copy(notes = IntArray(Sudoku.CELLS)), hint = null) }
    }

    /** Fills the remaining cells one by one so the player can watch the solution appear. */
    fun autoSolve() {
        val s = _ui.value
        val start = s.game ?: return
        if (start.status != GameStatus.PLAYING || s.autoSolving || s.generating) return
        _ui.update { it.copy(autoSolving = true, paused = false, selected = null, hint = null, notesMode = false) }
        viewModelScope.launch {
            for (cell in 0 until Sudoku.CELLS) {
                val g = _ui.value.game ?: return@launch
                if (g.values[cell] == g.solution[cell]) continue
                val values = g.values.copyOf().also { it[cell] = g.solution[cell] }
                val notes = g.notes.copyOf().also { it[cell] = 0 }
                _ui.update { it.copy(game = g.copy(values = values, notes = notes), selected = cell) }
                delay(35)
            }
            undoStack.clear()
            _ui.update { it.copy(game = it.game?.copy(status = GameStatus.AUTO_SOLVED), autoSolving = false, selected = null, canUndo = false) }
            save()
        }
    }

    fun onAppVisible(visible: Boolean) {
        appVisible = visible
        if (!visible) save()
    }

    override fun onCleared() = save()

    private fun commit(game: GameState, keepHint: Boolean = false) {
        val won = game.isComplete
        val next = if (won) game.copy(status = GameStatus.WON) else game
        _ui.update { it.copy(game = next, hint = if (keepHint) it.hint else null, canUndo = !won && undoStack.isNotEmpty()) }
        if (won) {
            undoStack.clear()
            _events.tryEmit(GameEvent.WON)
        }
        save()
    }

    private fun pushUndo() {
        val s = _ui.value
        val g = s.game ?: return
        undoStack.addLast(Snapshot(g.values, g.notes, s.selected))
        if (undoStack.size > MAX_UNDO) undoStack.removeFirst()
        _ui.update { it.copy(canUndo = true) }
    }

    private fun clearNoteFromPeers(notes: IntArray, cell: Int, digit: Int) {
        val mask = (1 shl digit).inv()
        for (p in Sudoku.PEERS[cell]) notes[p] = notes[p] and mask
    }

    private fun save() {
        _ui.value.game?.let(storage::save)
    }

    private companion object {
        const val MAX_UNDO = 200
    }
}
