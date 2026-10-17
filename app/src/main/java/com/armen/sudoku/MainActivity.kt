package com.armen.sudoku

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.armen.sudoku.ui.SudokuApp
import com.armen.sudoku.ui.theme.SudokuTheme

// AppCompatActivity so AppCompatDelegate can switch the app language at runtime.
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SudokuTheme {
                SudokuApp()
            }
        }
    }
}
