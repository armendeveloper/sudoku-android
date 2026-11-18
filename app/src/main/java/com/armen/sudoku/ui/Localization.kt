package com.armen.sudoku.ui

import android.text.format.DateFormat
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.core.os.LocaleListCompat
import com.armen.sudoku.R
import com.armen.sudoku.engine.Difficulty
import com.armen.sudoku.engine.Hint
import com.armen.sudoku.engine.HintKind
import com.armen.sudoku.engine.HintUnit
import com.armen.sudoku.engine.House
import com.armen.sudoku.engine.Step
import com.armen.sudoku.engine.Technique
import java.time.LocalDate
import java.time.format.DateTimeFormatter

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

/** The explanation for a hint that needs no eliminations first. */
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

/** The closing line of a hint that needed eliminations: what they leave for the hinted cell. */
@Composable
fun hintConclusion(hint: Hint): String =
    if (hint.kind == HintKind.HIDDEN_SINGLE && hint.unit != null) {
        stringResource(R.string.hint_then_hidden_single, hint.value, houseLabel(House(hint.unit, hint.unitIndex)))
    } else {
        stringResource(R.string.hint_then_naked_single, hint.value)
    }

@Composable
fun techniqueName(t: Technique): String = stringResource(
    when (t) {
        Technique.NAKED_SINGLE -> R.string.technique_naked_single
        Technique.HIDDEN_SINGLE -> R.string.technique_hidden_single
        Technique.POINTING -> R.string.technique_pointing
        Technique.CLAIMING -> R.string.technique_claiming
        Technique.NAKED_PAIR -> R.string.technique_naked_pair
        Technique.HIDDEN_PAIR -> R.string.technique_hidden_pair
        Technique.NAKED_TRIPLE -> R.string.technique_naked_triple
        Technique.HIDDEN_TRIPLE -> R.string.technique_hidden_triple
        Technique.X_WING -> R.string.technique_x_wing
        Technique.XY_WING -> R.string.technique_xy_wing
        Technique.SWORDFISH -> R.string.technique_swordfish
    },
)

/** "row 3", "column 5" or "box 2" (1-based), in the current language. */
@Composable
fun houseLabel(h: House): String = stringResource(
    when (h.type) {
        HintUnit.ROW -> R.string.house_row
        HintUnit.COLUMN -> R.string.house_column
        HintUnit.BOX -> R.string.house_box
    },
    h.index + 1,
)

/** Explains one elimination step in plain words. */
@Composable
fun stepMessage(step: Step): String {
    val d = step.digits
    val text = when (step.technique) {
        Technique.POINTING ->
            stringResource(R.string.step_pointing, houseLabel(step.houses[0]), d[0], houseLabel(step.targets[0]))
        Technique.CLAIMING ->
            stringResource(R.string.step_claiming, houseLabel(step.houses[0]), d[0], houseLabel(step.targets[0]))
        Technique.NAKED_PAIR -> stringResource(R.string.step_naked_pair, houseLabel(step.houses[0]), d[0], d[1])
        Technique.NAKED_TRIPLE -> stringResource(R.string.step_naked_triple, houseLabel(step.houses[0]), d[0], d[1], d[2])
        Technique.HIDDEN_PAIR -> stringResource(R.string.step_hidden_pair, houseLabel(step.houses[0]), d[0], d[1])
        Technique.HIDDEN_TRIPLE -> stringResource(R.string.step_hidden_triple, houseLabel(step.houses[0]), d[0], d[1], d[2])
        Technique.X_WING, Technique.SWORDFISH -> fishMessage(step)
        Technique.XY_WING -> stringResource(R.string.step_xy_wing, d[0], d[1], d[2])
        // Singles are explained by hintMessage/hintConclusion, never as an elimination step.
        Technique.NAKED_SINGLE, Technique.HIDDEN_SINGLE -> ""
    }
    return text.replaceFirstChar { it.uppercase() }
}

@Composable
private fun fishMessage(step: Step): String {
    val base = step.houses.map { it.index + 1 }
    val cover = step.targets.map { it.index + 1 }
    val digit = step.digits[0]
    val byRows = step.houses[0].type == HintUnit.ROW
    return if (step.technique == Technique.X_WING) {
        stringResource(
            if (byRows) R.string.step_x_wing_rows else R.string.step_x_wing_columns,
            base[0], base[1], digit, cover[0], cover[1],
        )
    } else {
        stringResource(
            if (byRows) R.string.step_swordfish_rows else R.string.step_swordfish_columns,
            base[0], base[1], base[2], digit, cover[0], cover[1], cover[2],
        )
    }
}

/** A short localized date with the weekday, such as "Wed, Nov 18". */
@Composable
fun dailyDateLabel(epochDay: Long): String {
    val locale = LocalConfiguration.current.locales[0]
    val pattern = DateFormat.getBestDateTimePattern(locale, "EEEMMMd")
    return LocalDate.ofEpochDay(epochDay).format(DateTimeFormatter.ofPattern(pattern, locale))
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
