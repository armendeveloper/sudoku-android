package com.armen.sudoku.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.armen.sudoku.R
import com.armen.sudoku.engine.Difficulty
import com.armen.sudoku.game.Settings
import com.armen.sudoku.game.Stats
import com.armen.sudoku.game.ThemeMode

/** A full-screen page with a back arrow, used for Statistics and Settings. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Page(
    title: String,
    onBack: () -> Unit,
    actions: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    BackHandler(onBack = onBack)
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(title) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back)) }
                },
                actions = { actions() },
            )
        },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(Modifier.widthIn(max = 640.dp).fillMaxWidth().padding(horizontal = 16.dp)) {
                content()
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 20.dp, bottom = 8.dp, start = 4.dp),
    )
}

@Composable
fun StatsScreen(stats: Stats, today: Long, initialLevel: Difficulty, onReset: () -> Unit, onBack: () -> Unit) {
    var level by rememberSaveable { mutableStateOf(initialLevel) }
    var confirmReset by rememberSaveable { mutableStateOf(false) }
    Page(
        title = stringResource(R.string.statistics),
        onBack = onBack,
        actions = {
            IconButton(onClick = { confirmReset = true }) {
                Icon(Icons.Outlined.DeleteOutline, stringResource(R.string.reset_stats_title))
            }
        },
    ) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Difficulty.entries.forEach { d ->
                FilterChip(selected = d == level, onClick = { level = d }, label = { Text(difficultyLabel(d)) })
            }
        }
        Spacer(Modifier.height(8.dp))
        val s = stats[level]
        val none = stringResource(R.string.stats_none)
        TileGrid(
            listOf(
                stringResource(R.string.stats_played) to s.played.toString(),
                stringResource(R.string.stats_won) to s.won.toString(),
                stringResource(R.string.stats_win_rate) to (s.winRate?.let { "$it%" } ?: none),
                stringResource(R.string.stats_perfect) to s.perfect.toString(),
                stringResource(R.string.stats_best_time) to (s.bestTime?.let(::formatTime) ?: none),
                stringResource(R.string.stats_average_time) to (s.averageTime?.let(::formatTime) ?: none),
                stringResource(R.string.stats_current_streak) to s.currentStreak.toString(),
                stringResource(R.string.stats_best_streak) to s.bestStreak.toString(),
            ),
        )
        Text(
            stringResource(R.string.stats_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 10.dp, start = 4.dp, end = 4.dp),
        )

        SectionTitle(stringResource(R.string.daily_challenge))
        TileGrid(
            listOf(
                stringResource(R.string.stats_days_completed) to stats.dailyDays.size.toString(),
                stringResource(R.string.stats_daily_streak) to pluralStringResource(R.plurals.days, stats.dailyStreak(today), stats.dailyStreak(today)),
                stringResource(R.string.stats_best_daily_streak) to pluralStringResource(R.plurals.days, stats.bestDailyStreak, stats.bestDailyStreak),
            ),
        )
        Text(
            stringResource(R.string.daily_explainer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 10.dp, start = 4.dp, end = 4.dp),
        )
    }
    if (confirmReset) {
        ResetStatsDialog(
            onConfirm = { confirmReset = false; onReset() },
            onDismiss = { confirmReset = false },
        )
    }
}

/** Label/value tiles, two per row. */
@Composable
private fun TileGrid(items: List<Pair<String, String>>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { (label, value) ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, maxLines = 1)
                            Text(
                                label,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: Settings,
    onChange: ((Settings) -> Settings) -> Unit,
    onThemeChange: (ThemeMode) -> Unit,
    onBack: () -> Unit,
) {
    var showLanguage by rememberSaveable { mutableStateOf(false) }
    Page(title = stringResource(R.string.settings), onBack = onBack) {
        SectionTitle(stringResource(R.string.settings_appearance))
        Text(
            stringResource(R.string.theme),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )
        val modes = ThemeMode.entries
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            modes.forEachIndexed { i, mode ->
                SegmentedButton(
                    selected = settings.theme == mode,
                    onClick = { onThemeChange(mode) },
                    shape = SegmentedButtonDefaults.itemShape(i, modes.size),
                    icon = {},
                ) {
                    Text(
                        stringResource(
                            when (mode) {
                                ThemeMode.SYSTEM -> R.string.theme_system
                                ThemeMode.LIGHT -> R.string.theme_light
                                ThemeMode.DARK -> R.string.theme_dark
                            },
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        val language = currentAppLanguage()
        SettingRow(
            title = stringResource(R.string.language),
            description = language.nativeName ?: stringResource(R.string.language_system),
            onClick = { showLanguage = true },
        )

        SectionTitle(stringResource(R.string.settings_gameplay))
        SwitchRow(
            stringResource(R.string.setting_show_mistakes),
            stringResource(R.string.setting_show_mistakes_desc),
            settings.showMistakes,
        ) { v -> onChange { it.copy(showMistakes = v) } }
        SwitchRow(
            stringResource(R.string.setting_mistake_limit),
            if (settings.showMistakes) {
                pluralStringResource(R.plurals.setting_mistake_limit_desc, Settings.MISTAKE_LIMIT, Settings.MISTAKE_LIMIT)
            } else {
                stringResource(R.string.setting_mistake_limit_needs)
            },
            settings.mistakeLimit,
            enabled = settings.showMistakes,
        ) { v -> onChange { it.copy(mistakeLimit = v) } }
        SwitchRow(
            stringResource(R.string.setting_auto_remove_notes),
            stringResource(R.string.setting_auto_remove_notes_desc),
            settings.autoRemoveNotes,
        ) { v -> onChange { it.copy(autoRemoveNotes = v) } }
        SwitchRow(stringResource(R.string.setting_highlight_peers), null, settings.highlightPeers) { v ->
            onChange { it.copy(highlightPeers = v) }
        }
        SwitchRow(stringResource(R.string.setting_highlight_same), null, settings.highlightSameNumbers) { v ->
            onChange { it.copy(highlightSameNumbers = v) }
        }
        SwitchRow(stringResource(R.string.setting_show_timer), null, settings.showTimer) { v ->
            onChange { it.copy(showTimer = v) }
        }
        SwitchRow(stringResource(R.string.setting_haptics), null, settings.haptics) { v ->
            onChange { it.copy(haptics = v) }
        }
    }
    if (showLanguage) {
        LanguageDialog(
            current = currentAppLanguage(),
            onSelect = { showLanguage = false; setAppLanguage(it) },
            onDismiss = { showLanguage = false },
        )
    }
}

@Composable
private fun SettingRow(title: String, description: String, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    description: String?,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(horizontal = 4.dp, vertical = 10.dp)
            .alpha(if (enabled) 1f else 0.5f),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (description != null) {
                Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.padding(start = 12.dp))
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}
