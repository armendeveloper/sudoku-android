package com.armen.sudoku.game

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Immutable
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode { SYSTEM, LIGHT, DARK }

@Immutable
data class Settings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    /** Mark wrong numbers right away and count them as mistakes. */
    val showMistakes: Boolean = true,
    /** End the game after [MISTAKE_LIMIT] mistakes. Only applies while [showMistakes] is on. */
    val mistakeLimit: Boolean = false,
    val autoRemoveNotes: Boolean = true,
    val highlightPeers: Boolean = true,
    val highlightSameNumbers: Boolean = true,
    val showTimer: Boolean = true,
    val haptics: Boolean = true,
) {
    val mistakeLimitActive get() = mistakeLimit && showMistakes

    companion object {
        const val MISTAKE_LIMIT = 3
    }
}

/** Keeps the player's settings in SharedPreferences. */
class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("sudoku_settings", Context.MODE_PRIVATE)
    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<Settings> = _settings.asStateFlow()

    fun update(transform: (Settings) -> Settings) {
        val next = transform(_settings.value)
        _settings.value = next
        prefs.edit {
            putString(KEY_THEME, next.theme.name)
            putBoolean(KEY_SHOW_MISTAKES, next.showMistakes)
            putBoolean(KEY_MISTAKE_LIMIT, next.mistakeLimit)
            putBoolean(KEY_AUTO_NOTES, next.autoRemoveNotes)
            putBoolean(KEY_HIGHLIGHT_PEERS, next.highlightPeers)
            putBoolean(KEY_HIGHLIGHT_SAME, next.highlightSameNumbers)
            putBoolean(KEY_SHOW_TIMER, next.showTimer)
            putBoolean(KEY_HAPTICS, next.haptics)
        }
    }

    private fun load(): Settings {
        val d = Settings()
        return Settings(
            theme = runCatching { ThemeMode.valueOf(prefs.getString(KEY_THEME, null)!!) }.getOrDefault(d.theme),
            showMistakes = prefs.getBoolean(KEY_SHOW_MISTAKES, d.showMistakes),
            mistakeLimit = prefs.getBoolean(KEY_MISTAKE_LIMIT, d.mistakeLimit),
            autoRemoveNotes = prefs.getBoolean(KEY_AUTO_NOTES, d.autoRemoveNotes),
            highlightPeers = prefs.getBoolean(KEY_HIGHLIGHT_PEERS, d.highlightPeers),
            highlightSameNumbers = prefs.getBoolean(KEY_HIGHLIGHT_SAME, d.highlightSameNumbers),
            showTimer = prefs.getBoolean(KEY_SHOW_TIMER, d.showTimer),
            haptics = prefs.getBoolean(KEY_HAPTICS, d.haptics),
        )
    }

    private companion object {
        const val KEY_THEME = "theme"
        const val KEY_SHOW_MISTAKES = "show_mistakes"
        const val KEY_MISTAKE_LIMIT = "mistake_limit"
        const val KEY_AUTO_NOTES = "auto_remove_notes"
        const val KEY_HIGHLIGHT_PEERS = "highlight_peers"
        const val KEY_HIGHLIGHT_SAME = "highlight_same"
        const val KEY_SHOW_TIMER = "show_timer"
        const val KEY_HAPTICS = "haptics"
    }
}

/**
 * Applies the light/dark choice app-wide. AppCompat recreates the activity when the mode changes,
 * which also updates the system bars. Call it again on every launch, since AppCompat doesn't store it.
 */
fun applyThemeMode(mode: ThemeMode) {
    AppCompatDelegate.setDefaultNightMode(
        when (mode) {
            ThemeMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            ThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            ThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
        },
    )
}
