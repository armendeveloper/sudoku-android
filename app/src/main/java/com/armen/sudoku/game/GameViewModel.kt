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
    private val settingsStore = SettingsStore(app)
    private val statsStore = StatsStore(app)
    val settings: StateFlow<Settings> = settingsStore.settings
    val stats: StateFlow<Stats> = statsStore.stats

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    private val _events = MutableSharedFlow<GameEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<GameEvent> = _events

    private class Snapshot(val values: IntArray, val notes: IntArray, val selected: Int?)
    private val undoStack = ArrayDeque<Snapshot>()
    private val redoStack = ArrayDeque<Snapshot>()
    private var appVisible = true
    private var gameScreenVisible = true

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
                    if (g != null && s.interactive && appVisible && gameScreenVisible) {
                        s.copy(game = g.copy(elapsedSeconds = g.elapsedSeconds + 1))
                    } else {
                        s
                    }
                }
            }
        }
    }

    fun newGame(difficulty: Difficulty) = start(difficulty, dailyDay = null)

    /** Starts (or returns to) today's daily challenge. Does nothing once today's is complete. */
    fun playDaily() {
        val day = Daily.today()
        val current = _ui.value.game
        if (current?.dailyDay == day && current.status == GameStatus.PLAYING) return
        if (day in stats.value.dailyDays) return
        start(Daily.difficultyFor(day), dailyDay = day)
    }

    private fun start(difficulty: Difficulty, dailyDay: Long?) {
        if (_ui.value.generating) return
        _ui.update { it.copy(generating = true, hint = null, notice = null) }
        viewModelScope.launch {
            val puzzle = withContext(Dispatchers.Default) {
                if (dailyDay != null) Daily.puzzle(dailyDay) else Generator().generate(difficulty)
            }
            // Leaving a game the player had started breaks their win streak.
            _ui.value.game?.let { old ->
                if (old.status == GameStatus.PLAYING && old.counted) statsStore.update { it.lost(old.difficulty) }
            }
            undoStack.clear()
            redoStack.clear()
            _ui.value = UiState(
                game = GameState(
                    givens = puzzle.givens,
                    solution = puzzle.solution,
                    values = puzzle.givens.copyOf(),
                    notes = IntArray(Sudoku.CELLS),
                    difficulty = puzzle.difficulty,
                    dailyDay = dailyDay,
                ),
            )
            save()
        }
    }

    fun restart() {
        val s = _ui.value
        val g = s.game ?: return
        if (s.generating || s.autoSolving) return
        undoStack.clear()
        redoStack.clear()
        // Restarting a game in progress is the same attempt; replaying a finished one is a new attempt.
        val counted = g.counted && g.status == GameStatus.PLAYING
        _ui.value = UiState(
            game = g.copy(
                values = g.givens.copyOf(),
                notes = IntArray(Sudoku.CELLS),
                mistakes = 0,
                hintsUsed = 0,
                elapsedSeconds = 0,
                status = GameStatus.PLAYING,
                counted = counted,
            ),
        )
        save()
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

    fun dismissNotice() = _ui.update { it.copy(notice = null) }

    fun enterDigit(digit: Int) {
        val s = _ui.value
        val g = s.game ?: return
        val cell = s.selected ?: return
        if (!s.interactive || g.isGiven(cell)) return
        val set = settings.value

        if (s.notesMode) {
            if (g.values[cell] != 0) return
            pushUndo()
            val notes = g.notes.copyOf().also { it[cell] = it[cell] xor (1 shl digit) }
            val next = g.tracked().copy(notes = notes)
            _ui.update { it.copy(game = next, hint = null, notice = null) }
            return
        }

        if (g.values[cell] == digit) return
        pushUndo()
        val correct = digit == g.solution[cell]
        // With mistakes hidden, treat every entry the same so nothing gives the answer away.
        val looksCorrect = correct || !set.showMistakes
        val values = g.values.copyOf().also { it[cell] = digit }
        val notes = g.notes.copyOf().also { it[cell] = 0 }
        if (looksCorrect && set.autoRemoveNotes) clearNoteFromPeers(notes, cell, digit)
        val mistakes = g.mistakes + if (looksCorrect) 0 else 1
        val lost = !looksCorrect && set.mistakeLimitActive && mistakes >= Settings.MISTAKE_LIMIT
        commit(
            g.tracked().copy(values = values, notes = notes, mistakes = mistakes, status = if (lost) GameStatus.LOST else g.status),
            origin = cell,
        )
        if (!lost) _events.tryEmit(if (looksCorrect) GameEvent.Correct else GameEvent.Wrong)
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
        _ui.update { it.copy(game = g.copy(values = values, notes = notes), hint = null, notice = null) }
    }

    fun undo() = step(from = undoStack, to = redoStack)

    fun redo() = step(from = redoStack, to = undoStack)

    private fun step(from: ArrayDeque<Snapshot>, to: ArrayDeque<Snapshot>) {
        val s = _ui.value
        val g = s.game ?: return
        if (!s.interactive) return
        val snap = from.removeLastOrNull() ?: return
        to.addLast(Snapshot(g.values, g.notes, s.selected))
        _ui.update {
            it.copy(
                game = g.copy(values = snap.values, notes = snap.notes),
                selected = snap.selected,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty(),
                hint = null,
                notice = null,
            )
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
        // The hint's eliminations are certain, so they can also clean up the player's notes.
        for (step in hint.steps) {
            for (code in step.eliminations) notes[code / 10] = notes[code / 10] and (1 shl (code % 10)).inv()
        }
        if (settings.value.autoRemoveNotes) clearNoteFromPeers(notes, hint.cell, hint.value)
        _ui.update { it.copy(selected = hint.cell, hint = hint) }
        commit(g.tracked().copy(values = values, notes = notes, hintsUsed = g.hintsUsed + 1), origin = hint.cell, keepHint = true)
    }

    /** Pencils in every legal candidate for every empty cell. */
    fun fillAllNotes() {
        val s = _ui.value
        val g = s.game ?: return
        if (!s.interactive) return
        pushUndo()
        // Ignore wrong entries so they don't hide the real candidates, unless mistakes are hidden:
        // then ignoring them would give them away.
        val ignoreWrong = settings.value.showMistakes
        val basis = IntArray(Sudoku.CELLS) { if (ignoreWrong && g.isWrong(it)) 0 else g.values[it] }
        val notes = IntArray(Sudoku.CELLS) { if (g.values[it] != 0) 0 else Sudoku.candidates(basis, it) }
        val next = g.tracked().copy(notes = notes)
        _ui.update { it.copy(game = next, hint = null, notice = null) }
    }

    fun clearAllNotes() {
        val s = _ui.value
        val g = s.game ?: return
        if (!s.interactive || g.notes.all { it == 0 }) return
        pushUndo()
        _ui.update { it.copy(game = g.copy(notes = IntArray(Sudoku.CELLS)), hint = null, notice = null) }
    }

    /** Fills the remaining cells one by one so the player can watch the solution appear. */
    fun autoSolve() {
        val s = _ui.value
        val start = s.game ?: return
        if (start.status != GameStatus.PLAYING || s.autoSolving || s.generating) return
        _ui.update { it.copy(autoSolving = true, paused = false, selected = null, hint = null, notesMode = false, notice = null) }
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
            redoStack.clear()
            if (start.counted) statsStore.update { it.lost(start.difficulty) }
            _ui.update {
                it.copy(
                    game = it.game?.copy(status = GameStatus.AUTO_SOLVED),
                    autoSolving = false,
                    selected = null,
                    canUndo = false,
                    canRedo = false,
                )
            }
            save()
        }
    }

    fun updateSettings(transform: (Settings) -> Settings) = settingsStore.update(transform)

    fun resetStats() = statsStore.reset()

    fun onAppVisible(visible: Boolean) {
        appVisible = visible
        if (!visible) save()
    }

    /** The timer only runs while the board is on screen (not under Settings or Statistics). */
    fun onGameScreenVisible(visible: Boolean) {
        gameScreenVisible = visible
    }

    override fun onCleared() = save()

    /** The first move of a game counts it as played. */
    private fun GameState.tracked(): GameState {
        if (counted) return this
        statsStore.update { it.started(difficulty) }
        return copy(counted = true)
    }

    private fun commit(game: GameState, origin: Int, keepHint: Boolean = false) {
        val won = game.isComplete
        val next = if (won) game.copy(status = GameStatus.WON) else game
        val ended = next.status != GameStatus.PLAYING
        val notice = if (!ended && !settings.value.showMistakes && next.values.none { it == 0 }) Notice.BOARD_FULL_WRONG else null

        var summary: WinSummary? = null
        if (won) {
            val (stats, win) = statsStore.stats.value.won(
                next.difficulty, next.elapsedSeconds, next.mistakes, next.hintsUsed, next.dailyDay, Daily.today(),
            )
            statsStore.update { stats }
            summary = win
        } else if (next.status == GameStatus.LOST) {
            statsStore.update { it.lost(next.difficulty) }
        }
        if (ended) {
            undoStack.clear()
            redoStack.clear()
        }
        _ui.update {
            it.copy(
                game = next,
                hint = if (keepHint) it.hint else null,
                canUndo = !ended && undoStack.isNotEmpty(),
                canRedo = !ended && redoStack.isNotEmpty(),
                notice = notice,
                winSummary = summary,
            )
        }
        when {
            won -> _events.tryEmit(GameEvent.Won(origin))
            next.status == GameStatus.LOST -> _events.tryEmit(GameEvent.Lost)
            else -> completedUnits(next, origin)?.let { _events.tryEmit(GameEvent.Completed(it, origin)) }
        }
        save()
    }

    /** Every cell of the rows, columns and boxes through [origin] that are now correctly filled. */
    private fun completedUnits(game: GameState, origin: Int): Set<Int>? {
        if (game.values[origin] != game.solution[origin]) return null
        val units = listOf(
            Sudoku.ROWS[Sudoku.row(origin)],
            Sudoku.COLS[Sudoku.col(origin)],
            Sudoku.BOXES[Sudoku.box(origin)],
        )
        val cells = units.filter { u -> u.all { game.values[it] == game.solution[it] } }.flatMap { it.toList() }.toSet()
        return cells.ifEmpty { null }
    }

    private fun pushUndo() {
        val s = _ui.value
        val g = s.game ?: return
        undoStack.addLast(Snapshot(g.values, g.notes, s.selected))
        if (undoStack.size > MAX_UNDO) undoStack.removeFirst()
        redoStack.clear()
        _ui.update { it.copy(canUndo = true, canRedo = false) }
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
