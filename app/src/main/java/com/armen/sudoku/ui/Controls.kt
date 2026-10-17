package com.armen.sudoku.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.armen.sudoku.engine.Hint
import com.armen.sudoku.game.GameState
import com.armen.sudoku.R
import androidx.compose.ui.res.stringResource

@Composable
fun StatusRow(game: GameState?, paused: Boolean, canPause: Boolean, onPause: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.secondaryContainer) {
            Text(
                game?.difficulty?.let { difficultyLabel(it) } ?: "…",
                Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            stringResource(R.string.mistakes_count, game?.mistakes ?: 0),
            style = MaterialTheme.typography.bodyMedium,
            color = if ((game?.mistakes ?: 0) > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.weight(1f))
        Text(
            formatTime(game?.elapsedSeconds ?: 0),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
        )
        IconButton(onClick = onPause, enabled = canPause) {
            Icon(if (paused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause, stringResource(if (paused) R.string.resume else R.string.pause))
        }
    }
}

@Composable
fun ActionBar(
    canUndo: Boolean,
    notesMode: Boolean,
    enabled: Boolean,
    onUndo: () -> Unit,
    onErase: () -> Unit,
    onNotes: () -> Unit,
    onHint: () -> Unit,
    onSolve: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        ActionButton(Icons.AutoMirrored.Rounded.Undo, stringResource(R.string.action_undo), onUndo, enabled && canUndo)
        ActionButton(Icons.AutoMirrored.Outlined.Backspace, stringResource(R.string.action_erase), onErase, enabled)
        ActionButton(Icons.Outlined.EditNote, stringResource(if (notesMode) R.string.action_notes_on else R.string.action_notes), onNotes, enabled, active = notesMode)
        ActionButton(Icons.Outlined.Lightbulb, stringResource(R.string.action_hint), onHint, enabled)
        ActionButton(Icons.Outlined.AutoFixHigh, stringResource(R.string.action_solve), onSolve, enabled)
    }
}

@Composable
private fun ActionButton(icon: ImageVector, label: String, onClick: () -> Unit, enabled: Boolean, active: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    val container by animateColorAsState(if (active) colors.primary else colors.surfaceContainerHigh, label = "actionBg")
    val content by animateColorAsState(if (active) colors.onPrimary else colors.onSurface, label = "actionFg")
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.alpha(if (enabled) 1f else 0.38f),
    ) {
        Surface(
            onClick = onClick,
            enabled = enabled,
            shape = CircleShape,
            color = container,
            contentColor = content,
            modifier = Modifier.size(52.dp),
        ) {
            Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = label) }
        }
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
    }
}

/** Digits 1–9, each showing how many are still left to place. */
@Composable
fun NumberPad(game: GameState?, notesMode: Boolean, enabled: Boolean, onDigit: (Int) -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        for (d in 1..9) {
            val remaining = 9 - (game?.placedCount(d) ?: 0)
            val usable = enabled && (notesMode || remaining > 0)
            Surface(
                onClick = { onDigit(d) },
                enabled = usable,
                shape = RoundedCornerShape(12.dp),
                color = if (notesMode) colors.secondaryContainer else colors.surfaceContainerHigh,
                modifier = Modifier
                    .weight(1f)
                    .height(66.dp)
                    .alpha(if (usable) 1f else 0.35f),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text(
                        d.toString(),
                        fontSize = if (notesMode) 22.sp else 28.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (notesMode) colors.onSecondaryContainer else colors.primary,
                    )
                    Text(
                        if (remaining > 0) remaining.toString() else "✓",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
fun HintBanner(hint: Hint, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(start = 14.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Lightbulb, contentDescription = null)
            Spacer(Modifier.width(10.dp))
            Text(hintMessage(hint), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            IconButton(onClick = onDismiss) { Icon(Icons.Outlined.Close, stringResource(R.string.dismiss_hint)) }
        }
    }
}

fun formatTime(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}
