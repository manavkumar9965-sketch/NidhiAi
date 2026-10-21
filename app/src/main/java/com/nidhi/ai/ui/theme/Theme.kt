package com.nidhi.ai.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.nidhi.ai.data.settings.ThemeMode

object NidhiColors {
    val Purple = Color(0xFF9D4EDD)
    val Cyan = Color(0xFF00E5FF)
    val Blue = Color(0xFF2979FF)
    val Night = Color(0xFF0A0A14)
    val NightSurface = Color(0xFF14142A)
}

private val DarkColors = darkColorScheme(
    primary = NidhiColors.Cyan,
    onPrimary = Color(0xFF00363D),
    secondary = NidhiColors.Purple,
    onSecondary = Color.White,
    tertiary = NidhiColors.Blue,
    onTertiary = Color.White,
    background = NidhiColors.Night,
    onBackground = Color(0xFFE6E6F5),
    surface = NidhiColors.NightSurface,
    onSurface = Color(0xFFE6E6F5),
    surfaceVariant = Color(0xFF1E1E3A),
    onSurfaceVariant = Color(0xFFB8B8D8),
    outline = Color(0xFF4A4A7A),
    error = Color(0xFFFF6B8A),
    errorContainer = Color(0xFF4A1425),
    onErrorContainer = Color(0xFFFFD9E1)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF6A1B9A),
    onPrimary = Color.White,
    secondary = Color(0xFF00838F),
    onSecondary = Color.White,
    tertiary = Color(0xFF1565C0),
    onTertiary = Color.White,
    background = Color(0xFFF7F5FF),
    onBackground = Color(0xFF1B1B2F),
    surface = Color.White,
    onSurface = Color(0xFF1B1B2F),
    surfaceVariant = Color(0xFFEDE7F6),
    onSurfaceVariant = Color(0xFF4A4A68),
    outline = Color(0xFF8A8AA8),
    error = Color(0xFFB3261E),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

@Composable
fun ThemeMode.isDark(): Boolean = when (this) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.DARK -> true
    ThemeMode.LIGHT -> false
}

@Composable
fun NidhiTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (mode.isDark()) DarkColors else LightColors,
        content = content
    )
}
