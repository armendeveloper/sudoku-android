package com.armen.sudoku

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.armen.sudoku.game.SettingsStore
import com.armen.sudoku.game.applyThemeMode
import com.armen.sudoku.ui.SudokuApp
import com.armen.sudoku.ui.theme.SudokuTheme

// AppCompatActivity so AppCompatDelegate can switch the app language and light/dark mode at runtime.
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Before super.onCreate, so the saved theme applies without recreating the activity.
        applyThemeMode(SettingsStore(this).settings.value.theme)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SudokuTheme {
                SudokuApp()
            }
        }
    }
}
