package com.armen.sudoku.ui

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.core.os.LocaleListCompat
import com.armen.sudoku.R
import com.armen.sudoku.engine.Difficulty
import com.armen.sudoku.engine.Hint
import com.armen.sudoku.engine.HintKind
import com.armen.sudoku.engine.HintUnit

@Composable
fun difficultyLabel(d: Difficulty): String = stringResource(
    when (d) {
        Difficulty.EASY -> R.string.difficulty_easy
        Difficulty.MEDIUM -> R.string.difficulty_medium
        Difficulty.HARD -> R.string.difficulty_hard
        Difficulty.EXPERT -> R.string.difficulty_expert
    },
)

@Composable
fun difficultyBlurb(d: Difficulty): String = stringResource(
    when (d) {
        Difficulty.EASY -> R.string.difficulty_easy_blurb
        Difficulty.MEDIUM -> R.string.difficulty_medium_blurb
        Difficulty.HARD -> R.string.difficulty_hard_blurb
        Difficulty.EXPERT -> R.string.difficulty_expert_blurb
    },
)

@Composable
fun hintMessage(hint: Hint): String = when (hint.kind) {
    HintKind.FIX_MISTAKE -> stringResource(R.string.hint_fix_mistake, hint.wrongValue, hint.value)
    HintKind.NAKED_SINGLE -> stringResource(R.string.hint_naked_single, hint.value)
    HintKind.HIDDEN_SINGLE -> when (hint.unit) {
        HintUnit.ROW -> stringResource(R.string.hint_hidden_row, hint.value, hint.unitIndex + 1)
        HintUnit.COLUMN -> stringResource(R.string.hint_hidden_column, hint.value, hint.unitIndex + 1)
        HintUnit.BOX, null -> stringResource(R.string.hint_hidden_box, hint.value)
    }
    HintKind.REVEAL -> stringResource(R.string.hint_reveal, hint.value)
}

/** App languages offered in the picker. An empty tag means "follow the phone's language". */
enum class AppLanguage(val tag: String, val nativeName: String?) {
    SYSTEM("", null),
    ENGLISH("en", "English"),
    ARMENIAN("hy", "Հայերեն"),
    RUSSIAN("ru", "Русский"),
}

fun currentAppLanguage(): AppLanguage {
    val tag = AppCompatDelegate.getApplicationLocales().toLanguageTags()
    return AppLanguage.entries.firstOrNull { it.tag.isNotEmpty() && tag.startsWith(it.tag) } ?: AppLanguage.SYSTEM
}

/** Switches the app language; AppCompat recreates the activity and remembers the choice. */
fun setAppLanguage(language: AppLanguage) {
    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language.tag))
}
