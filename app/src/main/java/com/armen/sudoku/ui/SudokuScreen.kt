package com.armen.sudoku.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.outlined.ClearAll
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Leaderboard
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.armen.sudoku.R
import com.armen.sudoku.engine.Difficulty
import com.armen.sudoku.engine.Sudoku
import com.armen.sudoku.game.Daily
import com.armen.sudoku.game.GameEvent
import com.armen.sudoku.game.GameStatus
import com.armen.sudoku.game.GameViewModel
import com.armen.sudoku.game.Notice
import com.armen.sudoku.game.Settings
import com.armen.sudoku.game.UiState
import com.armen.sudoku.game.applyThemeMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class Screen { GAME, STATS, SETTINGS }

/** How long the completion waves last. The win dialog waits for the win wave to finish. */
private const val UNIT_FLASH_MS = 700
private const val WIN_FLASH_MS = 1100

/**
 * The game, with Statistics and Settings sliding in on top. The game stays composed underneath,
 * so its dialogs and scroll position survive a trip to another page.
 */
@Composable
fun SudokuApp(vm: GameViewModel = viewModel()) {
    var screen by rememberSaveable { mutableStateOf(Screen.GAME) }
    LaunchedEffect(screen) { vm.onGameScreenVisible(screen == Screen.GAME) }

    Box(Modifier.fillMaxSize()) {
        GameScreen(
            vm,
            active = screen == Screen.GAME,
            onOpenStats = { screen = Screen.STATS },
            onOpenSettings = { screen = Screen.SETTINGS },
            modifier = if (screen == Screen.GAME) Modifier else Modifier.clearAndSetSemantics { },
        )
        AnimatedVisibility(
            visible = screen == Screen.STATS,
            enter = slideInHorizontally { it } + fadeIn(),
            exit = slideOutHorizontally { it } + fadeOut(),
        ) {
            val stats by vm.stats.collectAsStateWithLifecycle()
            StatsScreen(
                stats,
                today = Daily.today(),
                initialLevel = vm.ui.value.game?.difficulty ?: Difficulty.EASY,
                onReset = vm::resetStats,
                onBack = { screen = Screen.GAME },
            )
        }
        AnimatedVisibility(
            visible = screen == Screen.SETTINGS,
            enter = slideInHorizontally { it } + fadeIn(),
            exit = slideOutHorizontally { it } + fadeOut(),
        ) {
            val settings by vm.settings.collectAsStateWithLifecycle()
            SettingsScreen(
                settings,
                onChange = vm::updateSettings,
                onThemeChange = { mode ->
                    vm.updateSettings { it.copy(theme = mode) }
                    applyThemeMode(mode)
                },
                onBack = { screen = Screen.GAME },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GameScreen(
    vm: GameViewModel,
    active: Boolean,
    onOpenStats: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by vm.ui.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val stats by vm.stats.collectAsStateWithLifecycle()
    val game = state.game
    val haptics = LocalHapticFeedback.current
    val currentSettings by rememberUpdatedState(settings)

    var showNewGame by rememberSaveable { mutableStateOf(false) }
    var showSolveConfirm by rememberSaveable { mutableStateOf(false) }
    // Keyed on status too, so replaying a finished puzzle and finishing again shows the dialog again.
    var gameOverDismissed by rememberSaveable(game?.givens?.contentHashCode(), game?.status) { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }

    // Completion waves. The win dialog waits until the win wave has played.
    var flash by remember { mutableStateOf<Flash?>(null) }
    val flashProgress = remember { Animatable(1f) }
    var celebrating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    fun startFlash(cells: Set<Int>, origin: Int, durationMs: Int) {
        flash = Flash(cells, origin, System.nanoTime())
        scope.launch {
            flashProgress.snapTo(0f)
            flashProgress.animateTo(1f, tween(durationMs, easing = LinearEasing))
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_START) { vm.onAppVisible(true) }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { vm.onAppVisible(false) }
    LaunchedEffect(vm) {
        vm.events.collect { event ->
            if (currentSettings.haptics) {
                val type = when (event) {
                    GameEvent.Correct -> HapticFeedbackType.TextHandleMove
                    GameEvent.Wrong, GameEvent.Lost -> HapticFeedbackType.Reject
                    is GameEvent.Won -> HapticFeedbackType.Confirm
                    is GameEvent.Completed -> null
                }
                type?.let(haptics::performHapticFeedback)
            }
            when (event) {
                is GameEvent.Completed -> startFlash(event.cells, event.origin, UNIT_FLASH_MS)
                is GameEvent.Won -> {
                    celebrating = true
                    startFlash((0 until Sudoku.CELLS).toSet(), event.origin, WIN_FLASH_MS)
                    scope.launch {
                        delay(WIN_FLASH_MS.toLong())
                        celebrating = false
                    }
                }
                else -> Unit
            }
        }
    }

    val focusRequester = remember { FocusRequester() }
    // Take keyboard focus back whenever the game is on screen again.
    LaunchedEffect(active) { if (active) focusRequester.requestFocus() }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                navigationIcon = {
                    IconButton(onClick = { showNewGame = true }, enabled = !state.generating && !state.autoSolving) {
                        Icon(Icons.Rounded.Add, stringResource(R.string.new_game))
                    }
                },
                actions = {
                    IconButton(onClick = onOpenStats) { Icon(Icons.Outlined.Leaderboard, stringResource(R.string.statistics)) }
                    Box {
                        IconButton(onClick = { menuOpen = true }) { Icon(Icons.Rounded.MoreVert, stringResource(R.string.more_options)) }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_redo)) },
                                leadingIcon = { Icon(Icons.AutoMirrored.Rounded.Redo, null) },
                                enabled = state.interactive && state.canRedo,
                                onClick = { menuOpen = false; vm.redo() },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.fill_all_notes)) },
                                leadingIcon = { Icon(Icons.Outlined.EditNote, null) },
                                enabled = state.interactive,
                                onClick = { menuOpen = false; vm.fillAllNotes() },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.clear_all_notes)) },
                                leadingIcon = { Icon(Icons.Outlined.ClearAll, null) },
                                enabled = state.interactive,
                                onClick = { menuOpen = false; vm.clearAllNotes() },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.restart_puzzle)) },
                                leadingIcon = { Icon(Icons.Outlined.Replay, null) },
                                enabled = game != null && !state.generating && !state.autoSolving,
                                onClick = { menuOpen = false; vm.restart() },
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.settings)) },
                                leadingIcon = { Icon(Icons.Outlined.Settings, null) },
                                onClick = { menuOpen = false; onOpenSettings() },
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .focusRequester(focusRequester)
                .focusable()
                .onKeyEvent { e ->
                    if (!active || e.type != KeyEventType.KeyDown) return@onKeyEvent false
                    keyDigit(e.key)?.let { vm.enterDigit(it); return@onKeyEvent true }
                    when (e.key) {
                        Key.DirectionUp -> vm.moveSelection(-1, 0)
                        Key.DirectionDown -> vm.moveSelection(1, 0)
                        Key.DirectionLeft -> vm.moveSelection(0, -1)
                        Key.DirectionRight -> vm.moveSelection(0, 1)
                        Key.Backspace, Key.Delete, Key.Zero, Key.NumPad0 -> vm.erase()
                        Key.N -> vm.toggleNotesMode()
                        Key.H -> vm.hint()
                        Key.Z -> vm.undo()
                        Key.Y -> vm.redo()
                        else -> return@onKeyEvent false
                    }
                    true
                },
        ) {
            val landscape = maxWidth > maxHeight
            // In portrait, leave room for the status row, hint banner, action bar and number pad.
            val boardSize = if (landscape) {
                min(maxHeight - 32.dp, maxWidth * 0.55f)
            } else {
                min(maxWidth - 32.dp, maxHeight - 305.dp).coerceAtLeast(240.dp)
            }
            val canPause = game?.status == GameStatus.PLAYING && !state.autoSolving

            val board = @Composable {
                BoardArea(
                    state, settings, boardSize,
                    onTap = vm::select,
                    onResume = vm::togglePause,
                    flash = flash,
                    flashProgress = { flashProgress.value },
                )
            }
            val controls = @Composable {
                ControlsColumn(
                    showStatus = landscape,
                    state = state,
                    settings = settings,
                    onPause = vm::togglePause,
                    onDigit = vm::enterDigit,
                    onUndo = vm::undo,
                    onErase = vm::erase,
                    onNotes = vm::toggleNotesMode,
                    onHint = vm::hint,
                    onSolve = { showSolveConfirm = true },
                    onDismissHint = vm::dismissHint,
                    onDismissNotice = vm::dismissNotice,
                    onNewGame = { showNewGame = true },
                    onTryAgain = vm::restart,
                )
            }

            if (landscape) {
                Row(
                    Modifier.fillMaxSize().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    board()
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) { controls() }
                }
            } else {
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    StatusRow(game, settings, state.paused, canPause = canPause, onPause = vm::togglePause)
                    Spacer(Modifier.height(8.dp))
                    board()
                    Spacer(Modifier.height(12.dp))
                    controls()
                }
            }
        }
    }

    if (showNewGame) {
        val today = Daily.today()
        NewGameDialog(
            current = game?.difficulty ?: Difficulty.EASY,
            daily = DailyInfo(
                day = today,
                difficulty = Daily.difficultyFor(today),
                completed = today in stats.dailyDays,
                inProgress = game?.dailyDay == today && game.status == GameStatus.PLAYING,
                streak = stats.dailyStreak(today),
            ),
            onStart = { showNewGame = false; vm.newGame(it) },
            onDaily = { showNewGame = false; vm.playDaily() },
            onDismiss = { showNewGame = false },
        )
    }
    if (showSolveConfirm) {
        ConfirmSolveDialog(
            onConfirm = { showSolveConfirm = false; vm.autoSolve() },
            onDismiss = { showSolveConfirm = false },
        )
    }
    if (game != null && game.status != GameStatus.PLAYING && !state.autoSolving && !gameOverDismissed &&
        !state.generating && !celebrating && active
    ) {
        GameOverDialog(
            game = game,
            summary = state.winSummary,
            onNewGame = { gameOverDismissed = true; showNewGame = true },
            onTryAgain = { gameOverDismissed = true; vm.restart() },
            onDismiss = { gameOverDismissed = true },
        )
    }
}

@Composable
private fun BoardArea(
    state: UiState,
    settings: Settings,
    size: Dp,
    onTap: (Int) -> Unit,
    onResume: () -> Unit,
    flash: Flash?,
    flashProgress: () -> Float,
) {
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        SudokuBoard(
            state, settings,
            onCellTap = onTap,
            modifier = Modifier.fillMaxSize(),
            flash = flash,
            flashProgress = flashProgress,
        )
        when {
            state.generating || state.game == null -> Overlay {
                CircularProgressIndicator()
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.creating_puzzle), style = MaterialTheme.typography.bodyLarge)
            }
            state.paused -> Overlay {
                Text(stringResource(R.string.paused), style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(12.dp))
                Button(onClick = onResume) {
                    Icon(Icons.Rounded.PlayArrow, null)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.resume))
                }
            }
        }
    }
}

@Composable
private fun Overlay(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f), RoundedCornerShape(14.dp)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) { content() }
}

@Composable
private fun ControlsColumn(
    showStatus: Boolean,
    state: UiState,
    settings: Settings,
    onPause: () -> Unit,
    onDigit: (Int) -> Unit,
    onUndo: () -> Unit,
    onErase: () -> Unit,
    onNotes: () -> Unit,
    onHint: () -> Unit,
    onSolve: () -> Unit,
    onDismissHint: () -> Unit,
    onDismissNotice: () -> Unit,
    onNewGame: () -> Unit,
    onTryAgain: () -> Unit,
) {
    val game = state.game
    val gameOver = game != null && game.status != GameStatus.PLAYING && !state.autoSolving
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        // In portrait the status row sits above the board; in landscape it lives here.
        if (showStatus) {
            StatusRow(game, settings, state.paused, canPause = game?.status == GameStatus.PLAYING && !state.autoSolving, onPause = onPause)
            Spacer(Modifier.height(8.dp))
        }
        AnimatedVisibility(
            visible = state.hint != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            state.hint?.let { HintBanner(it, onDismissHint, Modifier.padding(bottom = 10.dp)) }
        }
        AnimatedVisibility(
            visible = state.notice != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            if (state.notice == Notice.BOARD_FULL_WRONG) {
                NoticeBanner(stringResource(R.string.notice_board_full_wrong), onDismissNotice, Modifier.padding(bottom = 10.dp))
            }
        }
        if (gameOver) {
            FilledTonalButton(onClick = onNewGame, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Icon(Icons.Rounded.Add, null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.start_new_game), style = MaterialTheme.typography.titleMedium)
            }
            if (game.status == GameStatus.LOST) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onTryAgain, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                    Icon(Icons.Outlined.Replay, null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.try_again))
                }
            }
        } else {
            ActionBar(
                canUndo = state.canUndo,
                notesMode = state.notesMode,
                enabled = state.interactive,
                onUndo = onUndo,
                onErase = onErase,
                onNotes = onNotes,
                onHint = onHint,
                onSolve = onSolve,
            )
            Spacer(Modifier.height(16.dp))
            NumberPad(game, state.notesMode, enabled = state.interactive, onDigit = onDigit)
        }
        Spacer(Modifier.height(16.dp))
    }
}

private fun keyDigit(key: Key): Int? = when (key) {
    Key.One, Key.NumPad1 -> 1
    Key.Two, Key.NumPad2 -> 2
    Key.Three, Key.NumPad3 -> 3
    Key.Four, Key.NumPad4 -> 4
    Key.Five, Key.NumPad5 -> 5
    Key.Six, Key.NumPad6 -> 6
    Key.Seven, Key.NumPad7 -> 7
    Key.Eight, Key.NumPad8 -> 8
    Key.Nine, Key.NumPad9 -> 9
    else -> null
}
