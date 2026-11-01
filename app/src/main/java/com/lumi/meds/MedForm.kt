package com.lumi.meds

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.lumi.data.Medication
import com.lumi.ui.BackButton
import com.lumi.ui.BigButton
import com.lumi.ui.IconLabel
import com.lumi.ui.Screen
import com.lumi.ui.focusRing

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

/** O que o OCR devolveria de uma receita. */
fun simulatedPrescription() = Medication(0, "Losartana 50 mg", "1 comprimido", listOf("08:00", "20:00"), 30, "Dra. Ana Souza")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MedForm(initial: Medication? = null, onSave: (Medication) -> Unit, onCancel: () -> Unit) {
    var name by rememberSaveable { mutableStateOf(initial?.name.orEmpty()) }
    var dose by rememberSaveable { mutableStateOf(initial?.dose ?: DOSES.first()) }
    var times by rememberSaveable { mutableStateOf(initial?.times?.joinToString(", ").orEmpty()) }
    var stock by rememberSaveable { mutableStateOf(initial?.stock?.toString().orEmpty()) }
    var doctor by rememberSaveable { mutableStateOf(initial?.doctor.orEmpty()) }
    var showErrors by rememberSaveable { mutableStateOf(false) }
    var scanned by rememberSaveable { mutableStateOf(false) }

    BackHandler(onBack = onCancel)
    val parsed = parseTimes(times)
    val nameError = name.isBlank()

    Screen("Novo remédio") {
        OutlinedButton(
            onClick = {
                // ponytail: simulação. Trocar por câmera + OCR (ex.: ML Kit Text Recognition) quando o time escolher a solução.
                val r = simulatedPrescription()
                name = r.name; dose = r.dose; times = r.times.joinToString(", "); stock = r.stock.toString(); doctor = r.doctor
                scanned = true
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).focusRing(),
        ) { IconLabel(Icons.Outlined.PhotoCamera, "Escanear receita (simulação)") }
        if (scanned) {
            Text(
                "Dados lidos da receita. Confira tudo antes de salvar.",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
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
            else onSave(Medication(initial?.id ?: 0, name.trim(), dose, parsed, stock.toIntOrNull() ?: 0, doctor.trim()))
        }
        BackButton(onCancel)
    }
}
