package com.armen.sudoku.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.armen.sudoku.engine.Sudoku
import com.armen.sudoku.game.UiState
import com.armen.sudoku.R
import androidx.compose.ui.res.stringResource

private val BoardShape = RoundedCornerShape(14.dp)

/**
 * The 9x9 grid, drawn on a single canvas. Highlights, from strongest to weakest:
 * hinted cell, selected cell, wrong entries, same digit as the selection, and the
 * selection's row/column/box.
 */
@Composable
fun SudokuBoard(state: UiState, onCellTap: (Int) -> Unit, modifier: Modifier = Modifier) {
    val game = state.game
    val colors = MaterialTheme.colorScheme
    val textMeasurer = rememberTextMeasurer(cacheSize = 32)
    val selected = state.selected
    val selectedValue = selected?.let { game?.values?.get(it) }?.takeIf { it != 0 }
    val hintCell = state.hint?.cell
    val thickLine = colors.onSurface.copy(alpha = 0.78f)
    val currentOnCellTap by rememberUpdatedState(onCellTap)
    val description = stringResource(R.string.board_description)

    Spacer(
        modifier
            .aspectRatio(1f)
            .clip(BoardShape)
            .border(2.dp, thickLine, BoardShape)
            .semantics { contentDescription = description }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val cellSize = size.width / 9f
                    val col = (offset.x / cellSize).toInt().coerceIn(0, 8)
                    val row = (offset.y / cellSize).toInt().coerceIn(0, 8)
                    currentOnCellTap(Sudoku.cellAt(row, col))
                }
            }
            .drawWithCache {
                val cs = size.width / 9f
                fun measure(text: String, sizeFactor: Float, weight: FontWeight): TextLayoutResult =
                    textMeasurer.measure(text, TextStyle(fontSize = (cs * sizeFactor).toSp(), fontWeight = weight))

                val givenDigits = (0..9).map { measure(it.toString(), 0.56f, FontWeight.Bold) }
                val userDigits = (0..9).map { measure(it.toString(), 0.56f, FontWeight.Medium) }
                val noteDigits = (0..9).map { measure(it.toString(), 0.23f, FontWeight.SemiBold) }
                val thin = 1.dp.toPx()
                val thick = 2.dp.toPx()

                onDrawBehind {
                    drawRect(colors.surface)
                    if (game == null) return@onDrawBehind

                    for (cell in 0 until Sudoku.CELLS) {
                        val bg = when {
                            cell == hintCell -> colors.tertiaryContainer
                            cell == selected -> colors.primary.copy(alpha = 0.34f)
                            game.isWrong(cell) -> colors.errorContainer.copy(alpha = 0.75f)
                            selectedValue != null && game.values[cell] == selectedValue -> colors.primary.copy(alpha = 0.2f)
                            selected != null && isPeer(selected, cell) -> colors.primary.copy(alpha = 0.075f)
                            else -> null
                        } ?: continue
                        drawRect(bg, Offset(Sudoku.col(cell) * cs, Sudoku.row(cell) * cs), Size(cs, cs))
                    }

                    for (i in 1..8) {
                        if (i % 3 == 0) continue
                        val p = i * cs
                        drawLine(colors.outlineVariant, Offset(p, 0f), Offset(p, size.height), thin)
                        drawLine(colors.outlineVariant, Offset(0f, p), Offset(size.width, p), thin)
                    }
                    for (i in intArrayOf(3, 6)) {
                        val p = i * cs
                        drawLine(thickLine, Offset(p, 0f), Offset(p, size.height), thick)
                        drawLine(thickLine, Offset(0f, p), Offset(size.width, p), thick)
                    }

                    for (cell in 0 until Sudoku.CELLS) {
                        val x = Sudoku.col(cell) * cs
                        val y = Sudoku.row(cell) * cs
                        val v = game.values[cell]
                        if (v != 0) {
                            val layout = if (game.isGiven(cell)) givenDigits[v] else userDigits[v]
                            val color = when {
                                game.isGiven(cell) -> colors.onSurface
                                game.isWrong(cell) -> colors.error
                                else -> colors.primary
                            }
                            drawText(
                                layout, color,
                                Offset(x + (cs - layout.size.width) / 2f, y + (cs - layout.size.height) / 2f),
                            )
                        } else if (game.notes[cell] != 0) {
                            val sub = cs / 3f
                            for (d in 1..9) {
                                if (game.notes[cell] and (1 shl d) == 0) continue
                                val nx = x + ((d - 1) % 3) * sub
                                val ny = y + ((d - 1) / 3) * sub
                                val match = d == selectedValue
                                if (match) {
                                    drawCircle(colors.primary.copy(alpha = 0.22f), sub * 0.42f, Offset(nx + sub / 2f, ny + sub / 2f))
                                }
                                val layout = noteDigits[d]
                                drawText(
                                    layout,
                                    if (match) colors.primary else colors.onSurfaceVariant,
                                    Offset(nx + (sub - layout.size.width) / 2f, ny + (sub - layout.size.height) / 2f),
                                )
                            }
                        }
                    }
                }
            },
    )
}

private fun isPeer(a: Int, b: Int) =
    Sudoku.row(a) == Sudoku.row(b) || Sudoku.col(a) == Sudoku.col(b) || Sudoku.box(a) == Sudoku.box(b)
