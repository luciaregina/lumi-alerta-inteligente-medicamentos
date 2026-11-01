package com.lumi.meds

import android.content.ActivityNotFoundException
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.lumi.data.Medication
import com.lumi.ui.BackButton
import com.lumi.ui.BigButton
import com.lumi.ui.IconLabel
import com.lumi.ui.Screen
import com.lumi.ui.focusRing
import java.io.File

val DOSES = listOf("1 comprimido", "½ comprimido", "2 comprimidos", "1 cápsula", "5 ml", "10 gotas", "1 unidade")

private val TIME = Regex("""(\d{1,2}):(\d{2})""")

/** "8:00, 20:00" -> ["08:00", "20:00"]; null se algum horário for inválido ou se não houver nenhum. */
fun parseTimes(text: String): List<String>? {
    val parts = text.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    if (parts.isEmpty()) return null
    return parts.map { p ->
        val (h, m) = TIME.matchEntire(p)?.destructured ?: return null
        if (h.toInt() > 23 || m.toInt() > 59) return null
        "%02d:%s".format(h.toInt(), m)
    }.distinct().sorted()
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MedForm(initial: Medication? = null, onSave: (Medication, photo: File?) -> Unit, onCancel: () -> Unit) {
    var name by rememberSaveable { mutableStateOf(initial?.name.orEmpty()) }
    var dose by rememberSaveable { mutableStateOf(initial?.dose ?: DOSES.first()) }
    var times by rememberSaveable { mutableStateOf(initial?.times?.joinToString(", ").orEmpty()) }
    var stock by rememberSaveable { mutableStateOf(initial?.stock?.toString().orEmpty()) }
    var doctor by rememberSaveable { mutableStateOf(initial?.doctor.orEmpty()) }
    var showErrors by rememberSaveable { mutableStateOf(false) }
    // A foto fica num arquivo temporário; só vira bytes no banco ao salvar.
    var hasPhoto by rememberSaveable { mutableStateOf(false) }
    var photoVersion by rememberSaveable { mutableIntStateOf(0) }
    val context = LocalContext.current
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) { hasPhoto = true; photoVersion++ }
    }
    val takePhoto = {
        try {
            camera.launch(photoUri(context))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "Nenhum app de câmera encontrado.", Toast.LENGTH_LONG).show()
        }
    }

    BackHandler(onBack = onCancel)
    val parsed = parseTimes(times)
    val nameError = name.isBlank()

    Screen("Novo remédio") {
        if (hasPhoto) {
            val preview = remember(photoVersion) {
                val path = photoFile(context).path
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }.also { BitmapFactory.decodeFile(path, it) }
                val opts = BitmapFactory.Options().apply { inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, maxSide = 800) }
                BitmapFactory.decodeFile(path, opts)?.asImageBitmap()
            }
            preview?.let {
                Image(
                    it, contentDescription = "Foto da receita",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = takePhoto, modifier = Modifier.heightIn(min = 48.dp).focusRing()) { Text("Tirar outra") }
                TextButton(onClick = { hasPhoto = false }, modifier = Modifier.heightIn(min = 48.dp).focusRing()) { Text("Remover foto") }
            }
        } else {
            OutlinedButton(
                onClick = takePhoto,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).focusRing(),
            ) { IconLabel(Icons.Outlined.PhotoCamera, "Fotografar receita") }
        }
        OutlinedTextField(
            value = name, onValueChange = { name = it },
            label = { Text("Nome do remédio") },
            isError = showErrors && nameError,
            supportingText = if (showErrors && nameError) {{ Text("Digite o nome do remédio.") }} else null,
            singleLine = true, modifier = Modifier.fillMaxWidth(),
        )
        Text("Dose", style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DOSES.forEach { d ->
                FilterChip(selected = dose == d, onClick = { dose = d }, label = { Text(d) }, modifier = Modifier.focusRing())
            }
        }
        OutlinedTextField(
            value = times, onValueChange = { times = it },
            label = { Text("Horários (ex.: 08:00, 20:00)") },
            isError = showErrors && parsed == null,
            supportingText = {
                Text(if (showErrors && parsed == null) "Use o formato HH:MM, separados por vírgula." else "${parsed?.size ?: 0} dose(s) por dia")
            },
            singleLine = true, modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = stock, onValueChange = { stock = it.filter(Char::isDigit).take(4) },
            label = { Text("Quantidade na caixa") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true, modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = doctor, onValueChange = { doctor = it },
            label = { Text("Médico / receita (opcional)") },
            singleLine = true, modifier = Modifier.fillMaxWidth(),
        )
        BigButton("Salvar") {
            if (nameError || parsed == null) showErrors = true
            else onSave(
                Medication(initial?.id ?: 0, name.trim(), dose, parsed, stock.toIntOrNull() ?: 0, doctor.trim()),
                photoFile(context).takeIf { hasPhoto },
            )
        }
        BackButton(onCancel)
    }
}
