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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.HeartBroken
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.runtime.Immutable
import androidx.compose.ui.res.pluralStringResource
import com.armen.sudoku.game.WinSummary

/** What the New Game dialog shows about today's daily challenge. */
@Immutable
data class DailyInfo(
    val day: Long,
    val difficulty: Difficulty,
    val completed: Boolean,
    val inProgress: Boolean,
    val streak: Int,
)

@Composable
fun NewGameDialog(
    current: Difficulty,
    daily: DailyInfo,
    onStart: (Difficulty) -> Unit,
    onDaily: () -> Unit,
    onDismiss: () -> Unit,
) {
    var choice by rememberSaveable { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.new_game)) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DailyCard(daily, onDaily)
                Text(
                    stringResource(R.string.or_pick_level),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp, start = 4.dp),
                )
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
private fun DailyCard(daily: DailyInfo, onDaily: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onDaily,
        enabled = !daily.completed,
        shape = RoundedCornerShape(16.dp),
        color = if (daily.completed) colors.surfaceContainerHigh else colors.tertiaryContainer,
        contentColor = if (daily.completed) colors.onSurface else colors.onTertiaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Today, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.daily_challenge), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.daily_subtitle, dailyDateLabel(daily.day), difficultyLabel(daily.difficulty)),
                    style = MaterialTheme.typography.bodySmall,
                )
                if (daily.inProgress) {
                    Text(stringResource(R.string.daily_continue), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
                if (daily.streak > 0) {
                    Text(
                        pluralStringResource(R.plurals.daily_streak, daily.streak, daily.streak),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            // Icon only, so long titles in other languages keep the full width.
            if (daily.completed) {
                Icon(Icons.Outlined.CheckCircle, stringResource(R.string.daily_done), tint = colors.primary)
            } else {
                Icon(
                    Icons.Rounded.PlayArrow,
                    stringResource(if (daily.inProgress) R.string.daily_continue else R.string.daily_play),
                )
            }
        }
    }
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
fun GameOverDialog(
    game: GameState,
    summary: WinSummary?,
    onNewGame: () -> Unit,
    onTryAgain: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (game.status == GameStatus.LOST) {
        AlertDialog(
            onDismissRequest = onDismiss,
            icon = { Icon(Icons.Outlined.HeartBroken, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(stringResource(R.string.lost_title)) },
            text = { Text(pluralStringResource(R.plurals.lost_text, game.mistakes, game.mistakes), style = MaterialTheme.typography.bodyLarge) },
            confirmButton = { Button(onClick = onTryAgain) { Text(stringResource(R.string.try_again)) } },
            dismissButton = { TextButton(onClick = onNewGame) { Text(stringResource(R.string.new_game)) } },
        )
        return
    }
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
                    when {
                        won && game.isDaily -> stringResource(R.string.daily_won_text, difficultyLabel(game.difficulty))
                        won -> stringResource(R.string.won_text, difficultyLabel(game.difficulty))
                        else -> stringResource(R.string.solved_text)
                    },
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (won && summary?.newBestTime == true) {
                    Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.tertiaryContainer) {
                        Text(
                            stringResource(R.string.new_best_time),
                            Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                    }
                }
                Stat(stringResource(R.string.stat_time), formatTime(game.elapsedSeconds))
                Stat(stringResource(R.string.stat_mistakes), game.mistakes.toString())
                Stat(stringResource(R.string.stat_hints), game.hintsUsed.toString())
                if (won && summary != null) {
                    Stat(stringResource(R.string.stats_current_streak), summary.streak.toString())
                    summary.dailyStreak?.let { Stat(stringResource(R.string.stats_daily_streak), pluralStringResource(R.plurals.days, it, it)) }
                }
            }
        },
        confirmButton = { Button(onClick = onNewGame) { Text(stringResource(R.string.new_game)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.view_board)) } },
    )
}

@Composable
fun ResetStatsDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.DeleteOutline, contentDescription = null) },
        title = { Text(stringResource(R.string.reset_stats_title)) },
        text = { Text(stringResource(R.string.reset_stats_text)) },
        confirmButton = { Button(onClick = onConfirm) { Text(stringResource(R.string.reset)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
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
