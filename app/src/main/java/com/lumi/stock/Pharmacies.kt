package com.lumi.stock

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.lumi.ui.SectionTitle
import com.lumi.ui.focusRing

data class Pharmacy(val name: String, val distance: String, val hours: String)

// ponytail: dados fictícios. Trocar por GPS + API de lugares (Google Places ou OpenStreetMap) quando o time escolher.
val SAMPLE_PHARMACIES = listOf(
    Pharmacy("Drogaria São João", "350 m", "Aberta 24 horas"),
    Pharmacy("Farmácia Popular", "800 m", "Aberta até 22:00"),
    Pharmacy("Drogasil", "1,2 km", "Aberta até 20:00"),
)

/** Abre o app de mapas do celular buscando o lugar (sem chave de API). */
fun openMap(context: Context, query: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(query))))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "Nenhum app de mapas instalado.", Toast.LENGTH_LONG).show()
    }
}

@Composable
fun WhereToBuy() {
    val context = LocalContext.current
    SectionTitle("Onde comprar")
    Text("Farmácias perto de você (exemplo)", style = MaterialTheme.typography.bodyLarge)
    SAMPLE_PHARMACIES.forEach { p ->
        Card(Modifier.fillMaxWidth()) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(p.name, style = MaterialTheme.typography.titleLarge)
                    Text("${p.distance} · ${p.hours}", style = MaterialTheme.typography.bodyLarge)
                }
                Button(
                    onClick = { openMap(context, p.name) },
                    modifier = Modifier.heightIn(min = 56.dp).focusRing()
                        .semantics { contentDescription = "Ir até ${p.name}" },
                ) { Text("Ir") }
            }
        }
    }
    OutlinedButton(
        onClick = { openMap(context, "farmácia") },
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).focusRing(),
    ) { Text("Ver farmácias no mapa") }
}
