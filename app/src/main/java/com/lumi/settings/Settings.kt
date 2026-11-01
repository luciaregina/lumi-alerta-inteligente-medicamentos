package com.lumi.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.lumi.Profile
import com.lumi.ThemeMode
import com.lumi.ui.BigButton
import com.lumi.ui.Screen
import com.lumi.ui.focusRing

@Composable
fun SettingsScreen(profile: Profile, onProfile: (Profile) -> Unit, onRedoOnboarding: () -> Unit) {
    Screen("Ajustes") {
        SectionTitle("Tema")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            ThemeMode.entries.forEach { mode ->
                FilterChip(
                    selected = profile.theme == mode,
                    onClick = { onProfile(profile.copy(theme = mode)) },
                    label = { Text(mode.label) },
                    modifier = Modifier.focusRing(),
                )
            }
        }
        SectionTitle("Cadastro")
        Text("Quer responder o cadastro de novo e reajustar o app?")
        BigButton("Refazer cadastro", onRedoOnboarding)
    }
}

@Composable
internal fun SectionTitle(text: String) =
    Text(text, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
