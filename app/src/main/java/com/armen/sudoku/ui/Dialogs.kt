package com.armen.sudoku.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.armen.sudoku.engine.Difficulty
import com.armen.sudoku.game.GameState
import com.armen.sudoku.game.GameStatus
import com.armen.sudoku.R
import androidx.compose.ui.res.stringResource

@Composable
fun NewGameDialog(current: Difficulty, onStart: (Difficulty) -> Unit, onDismiss: () -> Unit) {
    var choice by rememberSaveable { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.new_game)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Difficulty.entries.forEach { d ->
                    val selected = d == choice
                    Surface(
                        onClick = { choice = d },
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                RoundedCornerShape(12.dp),
                            ),
                    ) {
                        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = selected, onClick = { choice = d })
                            Column {
                                Text(difficultyLabel(d), style = MaterialTheme.typography.titleMedium)
                                Text(difficultyBlurb(d), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { onStart(choice) }) { Text(stringResource(R.string.start)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
fun ConfirmSolveDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.AutoFixHigh, contentDescription = null) },
        title = { Text(stringResource(R.string.solve_title)) },
        text = { Text(stringResource(R.string.solve_text)) },
        confirmButton = { Button(onClick = onConfirm) { Text(stringResource(R.string.solve_confirm)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.keep_playing)) } },
    )
}

@Composable
fun GameOverDialog(game: GameState, onNewGame: () -> Unit, onDismiss: () -> Unit) {
    val won = game.status == GameStatus.WON
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                if (won) Icons.Outlined.EmojiEvents else Icons.Outlined.AutoFixHigh,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
            )
        },
        title = { Text(stringResource(if (won) R.string.won_title else R.string.solved_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    if (won) stringResource(R.string.won_text, difficultyLabel(game.difficulty)) else stringResource(R.string.solved_text),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Stat(stringResource(R.string.stat_time), formatTime(game.elapsedSeconds))
                Stat(stringResource(R.string.stat_mistakes), game.mistakes.toString())
                Stat(stringResource(R.string.stat_hints), game.hintsUsed.toString())
            }
        },
        confirmButton = { Button(onClick = onNewGame) { Text(stringResource(R.string.new_game)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.view_board)) } },
    )
}

@Composable
private fun Stat(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun LanguageDialog(current: AppLanguage, onSelect: (AppLanguage) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.Language, contentDescription = null) },
        title = { Text(stringResource(R.string.language)) },
        text = {
            Column {
                AppLanguage.entries.forEach { lang ->
                    Surface(
                        onClick = { onSelect(lang) },
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Transparent,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = lang == current, onClick = { onSelect(lang) })
                            Text(
                                lang.nativeName ?: stringResource(R.string.language_system),
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
