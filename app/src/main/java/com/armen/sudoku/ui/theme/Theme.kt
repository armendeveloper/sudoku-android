package com.armen.sudoku.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val LightColors = lightColorScheme(
    primary = Color(0xFF3949AB),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDE1FF),
    onPrimaryContainer = Color(0xFF0F1A5C),
    secondary = Color(0xFF5C6BC0),
    secondaryContainer = Color(0xFFE3E6FA),
    onSecondaryContainer = Color(0xFF1A2160),
    tertiary = Color(0xFFB27A00),
    tertiaryContainer = Color(0xFFFFE8A8),
    onTertiaryContainer = Color(0xFF3D2A00),
    background = Color(0xFFF5F6FB),
    onBackground = Color(0xFF1A1C29),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1C29),
    surfaceVariant = Color(0xFFE8EAF6),
    onSurfaceVariant = Color(0xFF5A5F78),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF9F9FE),
    surfaceContainer = Color(0xFFF0F1F9),
    surfaceContainerHigh = Color(0xFFEBECF6),
    surfaceContainerHighest = Color(0xFFE4E6F3),
    outline = Color(0xFF9FA4C0),
    outlineVariant = Color(0xFFD5D8EA),
    error = Color(0xFFD32F2F),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF5F0E0E),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA3ADFF),
    onPrimary = Color(0xFF0E1A6B),
    primaryContainer = Color(0xFF2C3A8C),
    onPrimaryContainer = Color(0xFFDDE1FF),
    secondary = Color(0xFFBAC3FF),
    secondaryContainer = Color(0xFF2E3462),
    onSecondaryContainer = Color(0xFFDEE1FF),
    tertiary = Color(0xFFFFCA28),
    tertiaryContainer = Color(0xFF574200),
    onTertiaryContainer = Color(0xFFFFE8A8),
    background = Color(0xFF0F1220),
    onBackground = Color(0xFFE4E5F2),
    surface = Color(0xFF181B2C),
    onSurface = Color(0xFFE4E5F2),
    surfaceVariant = Color(0xFF242840),
    onSurfaceVariant = Color(0xFFB4B8D0),
    surfaceContainerLowest = Color(0xFF0B0D18),
    surfaceContainerLow = Color(0xFF151827),
    surfaceContainer = Color(0xFF1C1F31),
    surfaceContainerHigh = Color(0xFF23273B),
    surfaceContainerHighest = Color(0xFF2B2F45),
    outline = Color(0xFF6E7391),
    outlineVariant = Color(0xFF353A54),
    error = Color(0xFFFF8A80),
    errorContainer = Color(0xFF5C1D1D),
    onErrorContainer = Color(0xFFFFDAD6),
)

private val AppTypography = Typography().run {
    copy(
        titleLarge = titleLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
        labelMedium = labelMedium.copy(fontWeight = FontWeight.Medium),
        headlineSmall = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.SemiBold),
    )
}

@Composable
fun SudokuTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}
