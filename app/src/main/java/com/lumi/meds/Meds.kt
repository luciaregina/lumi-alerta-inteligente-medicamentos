package com.lumi.meds

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import com.lumi.data.LumiDao
import com.lumi.data.Medication
import com.lumi.ui.Screen
import com.lumi.ui.focusRing
import kotlinx.coroutines.launch

@Composable
fun MedsScreen(dao: LumiDao) {
    val meds by dao.meds().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var toRemove by rememberSaveable { mutableStateOf<Long?>(null) }

    Screen("Meus remédios") {
        if (meds.isEmpty()) Text("Nenhum remédio ainda.", style = MaterialTheme.typography.titleMedium)
        meds.forEach { MedCard(it, onRemove = { toRemove = it.id }) }
    }

    toRemove?.let { id ->
        AlertDialog(
            onDismissRequest = { toRemove = null },
            title = { Text("Remover este remédio?") },
            text = { Text(meds.find { it.id == id }?.name.orEmpty()) },
            confirmButton = {
                TextButton(onClick = { scope.launch { dao.deleteMed(id) }; toRemove = null }) { Text("Remover") }
            },
            dismissButton = { TextButton(onClick = { toRemove = null }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun MedCard(m: Medication, onRemove: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(m.name, style = MaterialTheme.typography.titleLarge)
                Text(m.dose, style = MaterialTheme.typography.bodyLarge)
                Text("Horários: ${m.times.joinToString(", ")}", style = MaterialTheme.typography.bodyLarge)
            }
            TextButton(onClick = onRemove, modifier = Modifier.focusRing()) { Text("Remover") }
        }
    }
}
