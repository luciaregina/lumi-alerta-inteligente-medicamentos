package com.lumi.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lumi.LocalProfile
import com.lumi.Profile
import com.lumi.ThemeMode
import com.lumi.ui.BigButton
import com.lumi.ui.Screen
import com.lumi.ui.focusRing

/** Tamanho de letra em passos de 10%, entre 90% e 200%. */
fun stepFont(scale: Float, steps: Int): Float = ((Math.round(scale * 10) + steps).coerceIn(9, 20)) / 10f

@Composable
fun SettingsScreen(profile: Profile, onProfile: (Profile) -> Unit, onRedoOnboarding: () -> Unit) {
    Screen("Ajustes") {
        SectionTitle("Tamanho da letra")
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            val pct = Math.round(profile.fontScale * 100)
            FontButton("A−", "Diminuir letra", enabled = pct > 90) { onProfile(profile.copy(fontScale = stepFont(profile.fontScale, -1))) }
            Text(
                "$pct%",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f).semantics { liveRegion = LiveRegionMode.Polite; contentDescription = "Letra em $pct por cento" },
            )
            FontButton("A+", "Aumentar letra", enabled = pct < 200) { onProfile(profile.copy(fontScale = stepFont(profile.fontScale, 1))) }
        }

        SectionTitle("Acessibilidade")
        Toggle("Alto contraste", profile.highContrast) { onProfile(profile.copy(highContrast = it)) }
        Toggle("Alarme falado", profile.spokenAlarm) { onProfile(profile.copy(spokenAlarm = it)) }
        Toggle("Tela piscando no alarme", profile.flashAlarm) { onProfile(profile.copy(flashAlarm = it)) }
        Toggle("Vibração", profile.vibration) { onProfile(profile.copy(vibration = it)) }
        Toggle("Modo simples", profile.simpleMode) { onProfile(profile.copy(simpleMode = it)) }
        Toggle("Botões maiores", profile.bigButtons) { onProfile(profile.copy(bigButtons = it)) }

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
private fun FontButton(text: String, description: String, enabled: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(72.dp).focusRing().semantics { contentDescription = description },
    ) { Text(text, style = MaterialTheme.typography.titleLarge) }
}

@Composable
private fun Toggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = if (LocalProfile.current.bigButtons) 72.dp else 56.dp)
            .focusRing()
            .toggleable(checked, role = Role.Switch, onValueChange = onChange),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
internal fun SectionTitle(text: String) =
    Text(text, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
