package com.lumi.meds

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lumi.data.LumiDao
import com.lumi.data.Medication
import com.lumi.ui.BigButton
import com.lumi.ui.IconLabel
import com.lumi.ui.Screen
import com.lumi.ui.focusRing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun MedsScreen(dao: LumiDao) {
    val meds by dao.meds().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var toRemove by rememberSaveable { mutableStateOf<Long?>(null) }
    var adding by rememberSaveable { mutableStateOf(false) }
    var viewing by rememberSaveable { mutableStateOf<Long?>(null) }
    val withPhoto by dao.medsWithPrescription().collectAsState(initial = emptyList())

    if (adding) {
        MedForm(
            onSave = { med, photo ->
                scope.launch {
                    val jpeg = photo?.let { withContext(Dispatchers.Default) { compressPhoto(it) } }
                    dao.insertWithPrescription(med, jpeg)
                    photo?.delete()
                }
                adding = false
            },
            onCancel = { adding = false },
        )
        return
    }

    Screen("Meus remédios") {
        BigButton("+ Adicionar remédio") { adding = true }
        if (meds.isEmpty()) Text("Nenhum remédio ainda.", style = MaterialTheme.typography.titleMedium)
        meds.forEach { m ->
            MedCard(m, hasPhoto = m.id in withPhoto, onView = { viewing = m.id }, onRemove = { toRemove = m.id })
        }
    }

    viewing?.let { id -> PrescriptionViewer(dao, id, meds.find { it.id == id }?.name.orEmpty()) { viewing = null } }

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
private fun MedCard(m: Medication, hasPhoto: Boolean, onView: () -> Unit, onRemove: () -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(m.name, style = MaterialTheme.typography.titleLarge)
            Text(m.dose, style = MaterialTheme.typography.bodyLarge)
            Text("Horários: ${m.times.joinToString(", ")}", style = MaterialTheme.typography.bodyLarge)
            Row {
                if (hasPhoto) {
                    TextButton(onClick = onView, modifier = Modifier.focusRing()) {
                        IconLabel(Icons.Outlined.Description, "Ver receita")
                    }
                }
                TextButton(onClick = onRemove, modifier = Modifier.focusRing()) { Text("Remover") }
            }
        }
    }
}

/** Foto da receita em tela cheia; os bytes só são lidos do banco quando abre. */
@Composable
private fun PrescriptionViewer(dao: LumiDao, medId: Long, name: String, onClose: () -> Unit) {
    var image by remember { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(medId) {
        image = withContext(Dispatchers.Default) {
            dao.prescription(medId)?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() }
        }
    }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Receita: $name", style = MaterialTheme.typography.titleLarge)
                image?.let {
                    Image(
                        it, contentDescription = "Foto da receita de $name",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxWidth().weight(1f),
                    )
                } ?: Box(Modifier.weight(1f))
                BigButton("Fechar", onClose)
            }
        }
    }
}
