package com.lumi.caregiver

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.lumi.data.Caregiver
import com.lumi.data.DoseTaken
import com.lumi.data.Medication
import com.lumi.data.LumiDao
import com.lumi.ui.BackButton
import com.lumi.ui.BigButton
import com.lumi.ui.Screen
import com.lumi.ui.SectionTitle
import com.lumi.ui.focusRing
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.security.SecureRandom
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

const val CODE_TTL_MS = 10 * 60_000L

/** Código de acesso de uso único: 6 dígitos, vale 10 minutos. */
data class AccessCode(val code: String, val expiresAt: Long) : java.io.Serializable {
    fun valid(now: Long) = now < expiresAt
}

private val random = SecureRandom()

const val MISSED_TOLERANCE_MIN = 30L

/** Avisos para o responsável: dose não confirmada após a tolerância e estoque acabando. */
fun caregiverAlerts(meds: List<Medication>, taken: List<DoseTaken>, now: LocalDateTime): List<String> {
    val day = now.toLocalDate().toString()
    val missed = meds.flatMap { m ->
        m.times.filter { t ->
            now.isAfter(now.toLocalDate().atTime(LocalTime.parse(t)).plusMinutes(MISSED_TOLERANCE_MIN)) &&
                taken.none { it.day == day && it.medId == m.id && it.time == t }
        }.map { "Dose não confirmada: ${m.name} das $it" }
    }
    val low = meds.filter { it.lowStock }.map { "Estoque acabando: ${it.name} (${it.daysLeft} dia(s))" }
    return missed + low
}

/** Valida o código lido pelo responsável: tem que bater e estar no prazo. */
fun AccessCode?.accepts(input: String, now: Long) = this != null && code == input.trim() && valid(now)

fun newAccessCode(now: Long) = AccessCode("%06d".format(random.nextInt(1_000_000)), now + CODE_TTL_MS)

@Composable
fun CaregiverScreen(dao: LumiDao) {
    val caregiver by dao.caregiver().collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    var code by rememberSaveable { mutableStateOf<AccessCode?>(null) }
    var scanned by rememberSaveable { mutableStateOf(false) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(code) {
        while (code != null) { now = System.currentTimeMillis(); delay(1000) }
    }

    caregiver?.let { cg ->
        Screen("Responsável") {
            Text("✓ Responsável conectado", style = MaterialTheme.typography.titleLarge)
            Text("${cg.name} (${cg.relation})", style = MaterialTheme.typography.headlineSmall)
            Text("Essa pessoa acompanha seus remédios e recebe avisos.")

            val meds by dao.meds().collectAsState(initial = emptyList())
            val taken by dao.takenOn(LocalDate.now().toString()).collectAsState(initial = emptyList())
            val alerts = caregiverAlerts(meds, taken, LocalDateTime.now())
            SectionTitle("Avisos para ${cg.name}")
            // ponytail: simulação. Enviar como push (ex.: Firebase Cloud Messaging) quando houver backend.
            if (alerts.isEmpty()) Text("Nenhum aviso agora. Tudo em dia!")
            alerts.forEach { Text("• $it", style = MaterialTheme.typography.bodyLarge) }

            var confirm by rememberSaveable { mutableStateOf(false) }
            BackButton(onClick = { confirm = true }, text = "Remover acesso")
            if (confirm) {
                AlertDialog(
                    onDismissRequest = { confirm = false },
                    title = { Text("Remover o acesso de ${cg.name}?") },
                    text = { Text("${cg.name} deixa de ver seus remédios e de receber avisos na hora.") },
                    confirmButton = {
                        TextButton(onClick = { scope.launch { dao.removeCaregiver() }; confirm = false }) { Text("Remover") }
                    },
                    dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancelar") } },
                )
            }
        }
        return
    }

    if (scanned) {
        LinkForm(
            scannedCode = code?.code.orEmpty(),
            onLink = { name, relation, typed ->
                // Uso único: o código é descartado assim que o vínculo é criado.
                if (code.accepts(typed, System.currentTimeMillis())) {
                    scope.launch { dao.setCaregiver(Caregiver(name = name, relation = relation)) }
                    code = null
                    scanned = false
                    true
                } else false
            },
            onCancel = { scanned = false },
        )
        return
    }

    Screen("Responsável") {
        Text("Um familiar ou cuidador pode acompanhar seus remédios e receber avisos.")
        val c = code?.takeIf { it.valid(now) }
        if (c == null) {
            if (code != null) Text("O código expirou. Gere um novo.", style = MaterialTheme.typography.titleMedium)
            // LGPD: consentimento explícito antes de compartilhar dados de saúde.
            var consent by rememberSaveable { mutableStateOf(false) }
            Row(
                Modifier.fillMaxWidth().heightIn(min = 56.dp).focusRing()
                    .toggleable(consent, role = Role.Checkbox) { consent = it },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = consent, onCheckedChange = null)
                Text(
                    "Autorizo compartilhar meus remédios, horários e estoque com o responsável. Posso remover o acesso quando quiser.",
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
            if (consent) BigButton("Gerar QR Code") { code = newAccessCode(System.currentTimeMillis()) }
        } else {
            Text("Peça para o responsável escanear este código no app LUMI dele.")
            QrCode("lumi:${c.code}", Modifier.align(Alignment.CenterHorizontally))
            val left = (c.expiresAt - now) / 1000
            Text(
                "Código: ${c.code.chunked(3).joinToString(" ")}",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { contentDescription = "Código ${c.code.toList().joinToString(" ")}" },
            )
            Text("Vale por mais %d:%02d".format(left / 60, left % 60), style = MaterialTheme.typography.bodyLarge)
            // ponytail: simulação. No app do responsável: câmera lendo o QR (ex.: ML Kit Barcode) e API criando o vínculo.
            BigButton("Simular: responsável escaneou") { scanned = true }
            BackButton(onClick = { code = null }, text = "Cancelar código")
        }
    }
}

/** O que o responsável preenche no app dele ao escanear o QR. */
@Composable
private fun LinkForm(
    scannedCode: String,
    onLink: (name: String, relation: String, code: String) -> Boolean,
    onCancel: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var relation by rememberSaveable { mutableStateOf("") }
    var typed by rememberSaveable { mutableStateOf(scannedCode) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    Screen("Responsável escaneou o código", "Simulação do app do responsável") {
        OutlinedTextField(name, { name = it }, label = { Text("Nome do responsável") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(relation, { relation = it }, label = { Text("Parentesco (ex.: Filha)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(
            typed, { typed = it.filter(Char::isDigit).take(6) },
            label = { Text("Código lido do QR") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = error != null,
            supportingText = error?.let { { Text(it) } },
            singleLine = true, modifier = Modifier.fillMaxWidth(),
        )
        BigButton("Conectar") {
            error = when {
                name.isBlank() || relation.isBlank() -> "Preencha nome e parentesco."
                !onLink(name.trim(), relation.trim(), typed) -> "Código inválido ou expirado."
                else -> null
            }
        }
        BackButton(onCancel)
    }
}

@Composable
private fun QrCode(content: String, modifier: Modifier = Modifier) {
    val matrix = remember(content) { QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 0, 0) }
    // Fundo branco fixo: leitores de QR precisam de módulos escuros sobre claro, mesmo no tema escuro.
    Box(
        modifier
            .background(Color.White)
            .padding(16.dp)
            .semantics { contentDescription = "QR Code de acesso do responsável" },
    ) {
        Canvas(Modifier.size(220.dp)) {
            val cell = size.width / matrix.width
            for (y in 0 until matrix.height) for (x in 0 until matrix.width) {
                if (matrix[x, y]) drawRect(Color.Black, Offset(x * cell, y * cell), Size(cell + 0.5f, cell + 0.5f))
            }
        }
    }
}
