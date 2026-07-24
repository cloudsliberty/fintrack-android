package com.fintrack.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val FtDarkColors = darkColorScheme(
    primary = FtAccent,
    onPrimary = Color.White,
    secondary = FtAccent,
    background = FtBg1,
    onBackground = FtText,
    surface = FtBg2,
    onSurface = FtText,
    surfaceVariant = FtBg3,
    onSurfaceVariant = FtText2,
    outline = FtBorder,
    error = FtExpense,
)

private val FtLightColors = lightColorScheme(
    primary = FtAccent,
    onPrimary = Color.White,
    secondary = FtAccent,
    error = FtExpense,
)

@Composable
fun FinTrackTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    // The web app is dark-themed by design (finance app, low-glare habit),
    // so FinTrack defaults to dark regardless of system theme — same
    // reasoning as most banking/budgeting apps defaulting dark. A light
    // scheme is still wired up for anyone who prefers it via device settings
    // override, but isn't the default branded look.
    val colors = if (darkTheme) FtDarkColors else FtLightColors
    MaterialTheme(colorScheme = colors, typography = MaterialTheme.typography, content = content)
}
