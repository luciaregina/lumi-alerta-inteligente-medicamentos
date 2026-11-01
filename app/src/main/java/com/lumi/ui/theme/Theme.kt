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

private val HighContrast = darkColorScheme(
    primary = Color(0xFFFFEB3B), onPrimary = Color.Black,
    background = Color.Black, onBackground = Color(0xFFFFEB3B),
    surface = Color.Black, onSurface = Color(0xFFFFEB3B),
    onSurfaceVariant = Color(0xFFFFEB3B), outline = Color(0xFFFFEB3B),
)

@Composable
fun LumiTheme(profile: Profile = Profile(), content: @Composable () -> Unit) {
    val colors = when {
        profile.highContrast -> HighContrast
        isSystemInDarkTheme() -> darkColorScheme()
        else -> lightColorScheme()
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
