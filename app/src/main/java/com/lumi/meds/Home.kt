package com.lumi.meds

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.lumi.alarm.Alarms
import com.lumi.data.LumiDao
import com.lumi.data.Medication
import com.lumi.ui.Screen
import com.lumi.ui.focusRing
import kotlinx.coroutines.launch
import java.time.LocalDate

data class Dose(val med: Medication, val time: String)

/** Todas as doses do dia, em ordem de horário. */
fun todayDoses(meds: List<Medication>): List<Dose> =
    meds.flatMap { m -> m.times.map { Dose(m, it) } }.sortedWith(compareBy({ it.time }, { it.med.name }))

@Composable
fun HomeScreen(name: String, dao: LumiDao, onOpenStock: () -> Unit) {
    val meds by dao.meds().collectAsState(initial = emptyList())
    val today = remember { LocalDate.now().toString() }
    val taken by dao.takenOn(today).collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    Screen("Olá, $name!") {
        val low = meds.filter { it.lowStock }
        if (low.isNotEmpty()) {
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("⚠ Estoque acabando", style = MaterialTheme.typography.titleLarge)
                    Text(low.joinToString { "${it.name} (${it.daysLeft} dia(s))" }, style = MaterialTheme.typography.bodyLarge)
                    TextButton(onClick = onOpenStock, modifier = Modifier.focusRing()) { Text("Ver estoque") }
                }
            }
        }
        Text("Seus remédios de hoje", style = MaterialTheme.typography.titleLarge)
        val doses = todayDoses(meds)
        val context = LocalContext.current
        doses.firstOrNull()?.let { first ->
            OutlinedButton(
                onClick = { context.startActivity(Alarms.screenIntent(context, first.med.id, first.time)) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).focusRing(),
            ) { Text("🔔 Testar alarme") }
        }
        if (doses.isEmpty()) Text("Nenhuma dose para hoje. Cadastre seus remédios na aba Remédios.")
        doses.forEach { d ->
            val done = taken.any { it.medId == d.med.id && it.time == d.time }
            DoseCard(d, done) { scope.launch { dao.markTaken(d.med.id, d.time) } }
        }
    }
}

@Composable
private fun DoseCard(d: Dose, done: Boolean, onTake: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(d.time, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(end = 16.dp))
            Column(Modifier.weight(1f)) {
                Text(d.med.name, style = MaterialTheme.typography.titleLarge)
                Text(d.med.dose, style = MaterialTheme.typography.bodyLarge)
            }
            if (done) {
                Text("✓ Tomado", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            } else {
                Button(
                    onClick = onTake,
                    modifier = Modifier.heightIn(min = 56.dp).focusRing()
                        .semantics { contentDescription = "Já tomei ${d.med.name} das ${d.time}" },
                ) { Text("Já tomei") }
            }
        }
    }
}
