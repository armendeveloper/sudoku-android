package com.armen.sudoku.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.outlined.ClearAll
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Replay
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.armen.sudoku.engine.Difficulty
import com.armen.sudoku.game.GameEvent
import com.armen.sudoku.game.GameStatus
import com.armen.sudoku.game.GameViewModel
import com.armen.sudoku.game.UiState
import com.armen.sudoku.R
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SudokuApp(vm: GameViewModel = viewModel()) {
    val state by vm.ui.collectAsStateWithLifecycle()
    val game = state.game
    val haptics = LocalHapticFeedback.current

    var showNewGame by rememberSaveable { mutableStateOf(false) }
    var showSolveConfirm by rememberSaveable { mutableStateOf(false) }
    var gameOverDismissed by rememberSaveable(game?.givens?.contentHashCode()) { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var showLanguage by rememberSaveable { mutableStateOf(false) }

    LifecycleEventEffect(Lifecycle.Event.ON_START) { vm.onAppVisible(true) }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { vm.onAppVisible(false) }
    LaunchedEffect(vm) {
        vm.events.collect { event ->
            haptics.performHapticFeedback(
                when (event) {
                    GameEvent.CORRECT -> HapticFeedbackType.TextHandleMove
                    GameEvent.WRONG -> HapticFeedbackType.Reject
                    GameEvent.WON -> HapticFeedbackType.Confirm
                },
            )
        }
    }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                navigationIcon = {
                    IconButton(onClick = { showNewGame = true }, enabled = !state.generating && !state.autoSolving) {
                        Icon(Icons.Rounded.Add, stringResource(R.string.new_game))
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { menuOpen = true }) { Icon(Icons.Rounded.MoreVert, stringResource(R.string.more_options)) }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
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
                                text = { Text(stringResource(R.string.language)) },
                                leadingIcon = { Icon(Icons.Outlined.Language, null) },
                                onClick = { menuOpen = false; showLanguage = true },
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
                    if (e.type != KeyEventType.KeyDown) return@onKeyEvent false
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

            val board = @Composable {
                BoardArea(state, boardSize, onTap = vm::select, onResume = vm::togglePause)
            }
            val controls = @Composable {
                ControlsColumn(
                    showStatus = landscape,
                    state = state,
                    onPause = vm::togglePause,
                    onDigit = vm::enterDigit,
                    onUndo = vm::undo,
                    onErase = vm::erase,
                    onNotes = vm::toggleNotesMode,
                    onHint = vm::hint,
                    onSolve = { showSolveConfirm = true },
                    onDismissHint = vm::dismissHint,
                    onNewGame = { showNewGame = true },
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
                    StatusRow(game, state.paused, canPause = state.game?.status == GameStatus.PLAYING && !state.autoSolving, onPause = vm::togglePause)
                    Spacer(Modifier.height(8.dp))
                    board()
                    Spacer(Modifier.height(12.dp))
                    controls()
                }
            }
        }
    }

    if (showNewGame) {
        NewGameDialog(
            current = game?.difficulty ?: Difficulty.EASY,
            onStart = { showNewGame = false; vm.newGame(it) },
            onDismiss = { showNewGame = false },
        )
    }
    if (showLanguage) {
        LanguageDialog(
            current = currentAppLanguage(),
            onSelect = { showLanguage = false; setAppLanguage(it) },
            onDismiss = { showLanguage = false },
        )
    }
    if (showSolveConfirm) {
        ConfirmSolveDialog(
            onConfirm = { showSolveConfirm = false; vm.autoSolve() },
            onDismiss = { showSolveConfirm = false },
        )
    }
    if (game != null && game.status != GameStatus.PLAYING && !state.autoSolving && !gameOverDismissed && !state.generating) {
        GameOverDialog(
            game = game,
            onNewGame = { gameOverDismissed = true; showNewGame = true },
            onDismiss = { gameOverDismissed = true },
        )
    }
}

@Composable
private fun BoardArea(state: UiState, size: Dp, onTap: (Int) -> Unit, onResume: () -> Unit) {
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        SudokuBoard(state, onCellTap = onTap, modifier = Modifier.fillMaxSize())
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
    onPause: () -> Unit,
    onDigit: (Int) -> Unit,
    onUndo: () -> Unit,
    onErase: () -> Unit,
    onNotes: () -> Unit,
    onHint: () -> Unit,
    onSolve: () -> Unit,
    onDismissHint: () -> Unit,
    onNewGame: () -> Unit,
) {
    val game = state.game
    val gameOver = game != null && game.status != GameStatus.PLAYING && !state.autoSolving
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        // In portrait the status row sits above the board; in landscape it lives here.
        if (showStatus) {
            StatusRow(game, state.paused, canPause = game?.status == GameStatus.PLAYING && !state.autoSolving, onPause = onPause)
            Spacer(Modifier.height(8.dp))
        }
        AnimatedVisibility(
            visible = state.hint != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            state.hint?.let { HintBanner(it, onDismissHint, Modifier.padding(bottom = 10.dp)) }
        }
        if (gameOver) {
            FilledTonalButton(onClick = onNewGame, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Icon(Icons.Rounded.Add, null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.start_new_game), style = MaterialTheme.typography.titleMedium)
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
