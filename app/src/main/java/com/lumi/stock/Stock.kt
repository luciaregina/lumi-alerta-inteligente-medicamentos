package com.lumi.stock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lumi.data.LumiDao
import com.lumi.data.Medication
import com.lumi.ui.Screen

/** Selo sempre com texto (não só cor). */
fun stockStatus(daysLeft: Int) = when {
    daysLeft <= 3 -> "Acabando!"
    daysLeft <= 7 -> "Atenção"
    else -> "Tudo certo"
}

@Composable
fun StockScreen(dao: LumiDao) {
    val meds by dao.meds().collectAsState(initial = emptyList())
    Screen("Estoque") {
        if (meds.isEmpty()) Text("Nenhum remédio ainda.", style = MaterialTheme.typography.titleMedium)
        meds.forEach { StockCard(it) }
    }
}

@Composable
private fun StockCard(m: Medication) {
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
        }
    }
}
