package com.lumi.stock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.lumi.data.LumiDao
import com.lumi.data.Medication
import com.lumi.ui.Screen
import com.lumi.ui.SectionTitle
import com.lumi.ui.focusRing
import kotlinx.coroutines.launch

/** Selo sempre com texto (não só cor). */
fun stockStatus(daysLeft: Int) = when {
    daysLeft <= 3 -> "Acabando!"
    daysLeft <= 7 -> "Atenção"
    else -> "Tudo certo"
}

@Composable
fun StockScreen(dao: LumiDao) {
    val meds by dao.meds().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var refill by rememberSaveable { mutableStateOf<Long?>(null) }

    Screen("Estoque") {
        if (meds.isEmpty()) Text("Nenhum remédio ainda.", style = MaterialTheme.typography.titleMedium)
        meds.forEach { m -> StockCard(m) { refill = m.id } }
        WhereToBuy()
    }

    refill?.let { id ->
        var units by rememberSaveable { mutableStateOf("30") }
        val n = units.toIntOrNull()?.takeIf { it > 0 }
        AlertDialog(
            onDismissRequest = { refill = null },
            title = { Text("Reposição de ${meds.find { it.id == id }?.name.orEmpty()}") },
            text = {
                OutlinedTextField(
                    value = units, onValueChange = { units = it.filter(Char::isDigit).take(4) },
                    label = { Text("Quantas unidades chegaram?") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(enabled = n != null, onClick = {
                    scope.launch { dao.addStock(id, n!!) }
                    refill = null
                }) { Text("Adicionar") }
            },
            dismissButton = { TextButton(onClick = { refill = null }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun StockCard(m: Medication, onRefill: () -> Unit) {
    val days = m.daysLeft
    val status = stockStatus(days)
    val color = when (status) {
        "Acabando!" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(m.name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                Text(status, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
            }
            val daysText = if (m.dosesPerDay == 0) "" else " · dura $days dia(s)"
            Text("${m.stock} unidade(s)$daysText", style = MaterialTheme.typography.bodyLarge)
            // Barra decorativa: o leitor de tela já lê os dias e o selo acima.
            LinearProgressIndicator(
                progress = { (days / 30f).coerceIn(0f, 1f) },
                color = color,
                modifier = Modifier.fillMaxWidth().clearAndSetSemantics {},
            )
            OutlinedButton(onClick = onRefill, modifier = Modifier.heightIn(min = 48.dp).focusRing()) {
                Text("+ Reposição")
            }
        }
    }
}
