package com.lumi.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.lumi.LocalProfile
import com.lumi.Profile
import com.lumi.ThemeMode

// Tokens de cor. Texto principal sobre fundo passa WCAG AA (4,5:1); no alto contraste, AAA (7:1).
private val Teal = Color(0xFF00585E)     // 8,2:1 sobre branco
private val TealLight = Color(0xFF7FD3DA) // 10,9:1 sobre #121212
private val Yellow = Color(0xFFFFEB3B)    // 17:1 sobre preto

private val Light = lightColorScheme(
    primary = Teal, onPrimary = Color.White,
    background = Color.White, onBackground = Color(0xFF1A1C1C),
    surface = Color.White, onSurface = Color(0xFF1A1C1C),
    error = Color(0xFFB3261E), onError = Color.White,
)

private val Dark = darkColorScheme(
    primary = TealLight, onPrimary = Color(0xFF00363A),
    background = Color(0xFF121212), onBackground = Color(0xFFE2E3E3),
    surface = Color(0xFF121212), onSurface = Color(0xFFE2E3E3),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
)

private val HighContrast = darkColorScheme(
    primary = Yellow, onPrimary = Color.Black,
    secondary = Yellow, onSecondary = Color.Black,
    secondaryContainer = Yellow, onSecondaryContainer = Color.Black,
    background = Color.Black, onBackground = Yellow,
    surface = Color.Black, onSurface = Yellow,
    surfaceVariant = Color.Black, onSurfaceVariant = Yellow,
    surfaceContainer = Color.Black, surfaceContainerHigh = Color.Black, surfaceContainerHighest = Color.Black,
    outline = Yellow, error = Yellow, onError = Color.Black,
)

@Composable
fun LumiTheme(profile: Profile = Profile(), content: @Composable () -> Unit) {
    val dark = when (profile.theme) {
        ThemeMode.AUTO -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colors = when {
        profile.highContrast -> HighContrast
        dark -> Dark
        else -> Light
    }
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalProfile provides profile,
        // Soma a escala do perfil à escala de fonte já escolhida no sistema.
        LocalDensity provides Density(density.density, density.fontScale * profile.fontScale),
    ) {
        MaterialTheme(colorScheme = colors, content = content)
    }
}
