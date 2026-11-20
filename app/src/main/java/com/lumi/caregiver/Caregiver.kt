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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationImportant
import androidx.compose.material.icons.outlined.VerifiedUser
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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
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
import com.lumi.ui.IconLabel
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

/** Etapas da tela. As que começam com CG_ acontecem no celular do responsável. */
private enum class Step { HOME, SCAN_REPLY, TYPE_REPLY, CG_SCAN, CG_TYPE, CG_FORM, CG_REPLY }

@Composable
fun CaregiverScreen(dao: LumiDao) {
    val caregiver by dao.caregiver().collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    var code by rememberSaveable { mutableStateOf<AccessCode?>(null) }
    var step by rememberSaveable { mutableStateOf(Step.HOME) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    // Lado do responsável: código lido do paciente e QR de resposta.
    var patientCode by rememberSaveable { mutableStateOf("") }
    var replyQr by rememberSaveable { mutableStateOf("") }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(code) {
        while (code != null) { now = System.currentTimeMillis(); delay(1000) }
    }
    val go = { s: Step -> error = null; step = s }

    /** Cria o vínculo se o código da resposta bater e estiver no prazo. Uso único: descarta o código. */
    val link = { reply: CaregiverReply ->
        if (code.accepts(reply.code, System.currentTimeMillis())) {
            scope.launch { dao.setCaregiver(Caregiver(name = reply.name, relation = reply.relation)) }
            code = null
            go(Step.HOME)
        } else {
            error = "O código não confere ou já expirou. Gere um novo QR Code e tente de novo."
        }
    }

    when (step) {
        Step.SCAN_REPLY -> {
            Screen("Escanear resposta do responsável") {
                Text("Aponte a câmera para o QR Code que aparece no celular do responsável.")
                ErrorText(error)
                QrScanner { text ->
                    val reply = parseReplyQr(text)
                    if (reply != null) link(reply)
                    else error = if (parsePatientQr(text) != null) "Este é o seu próprio QR Code. Escaneie o do celular do responsável."
                    else "Este QR Code não é uma resposta do LUMI."
                }
                BackButton(onClick = { go(Step.TYPE_REPLY) }, text = "Digitar em vez de escanear")
                BackButton(onClick = { go(Step.HOME) })
            }
            return
        }
        Step.TYPE_REPLY -> {
            ReplyForm(
                title = "Dados do responsável",
                hint = "Digite o que aparece no celular do responsável.",
                askCode = true,
                error = error,
                onDone = link,
                onCancel = { go(Step.HOME) },
            )
            return
        }
        Step.CG_SCAN -> {
            Screen("Escanear QR do paciente", "Celular do responsável") {
                Text("Aponte a câmera para o QR Code que aparece no celular do paciente.")
                ErrorText(error)
                QrScanner { text ->
                    val c = parsePatientQr(text)
                    if (c != null) { patientCode = c; go(Step.CG_FORM) }
                    else error = "Este QR Code não é do LUMI. Escaneie o QR mostrado no celular do paciente."
                }
                BackButton(onClick = { go(Step.CG_TYPE) }, text = "Digitar o código")
                BackButton(onClick = { go(Step.HOME) })
            }
            return
        }
        Step.CG_TYPE, Step.CG_FORM -> {
            ReplyForm(
                title = "Seus dados",
                hint = "O paciente vai ver seu nome e parentesco.",
                askCode = step == Step.CG_TYPE,
                presetCode = patientCode,
                error = error,
                onDone = { r -> replyQr = r.toQr(); go(Step.CG_REPLY) },
                onCancel = { go(Step.HOME) },
            )
            return
        }
        Step.CG_REPLY -> {
            val r = parseReplyQr(replyQr)
            Screen("Mostre para o paciente", "Celular do responsável") {
                Text("No celular do paciente, toque em \"Escanear resposta do responsável\" e aponte para este QR Code.")
                if (r != null) {
                    QrCode(replyQr, "QR Code de resposta do responsável", Modifier.align(Alignment.CenterHorizontally))
                    Text("Se a câmera não funcionar, o paciente pode digitar:", style = MaterialTheme.typography.titleMedium)
                    CodeText(r.code)
                    Text("${r.name} (${r.relation})", style = MaterialTheme.typography.titleLarge)
                }
                BigButton("Pronto") { replyQr = ""; patientCode = ""; go(Step.HOME) }
            }
            return
        }
        Step.HOME -> Unit
    }

    caregiver?.let { cg ->
        Screen("Responsável") {
            IconLabel(Icons.Outlined.VerifiedUser, "Responsável conectado", MaterialTheme.typography.titleLarge)
            Text("${cg.name} (${cg.relation})", style = MaterialTheme.typography.headlineSmall)
            Text("Essa pessoa acompanha seus remédios e recebe avisos.")

            val meds by dao.meds().collectAsState(initial = emptyList())
            val taken by dao.takenOn(LocalDate.now().toString()).collectAsState(initial = emptyList())
            val alerts = caregiverAlerts(meds, taken, LocalDateTime.now())
            SectionTitle("Avisos para ${cg.name}")
            // ponytail: simulação. Enviar como push (ex.: Firebase Cloud Messaging) quando houver backend.
            if (alerts.isEmpty()) Text("Nenhum aviso agora. Tudo em dia!")
            alerts.forEach { IconLabel(Icons.Outlined.NotificationImportant, it, MaterialTheme.typography.bodyLarge) }

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

            SectionTitle("Você é o responsável?")
            Text("Use esta opção no seu celular para acompanhar um paciente.")
            BackButton(onClick = { go(Step.CG_SCAN) }, text = "Escanear QR do paciente")
        } else {
            Text("1. Peça para o responsável escanear este código no app LUMI dele.")
            QrCode(patientQr(c.code), "QR Code de acesso do responsável", Modifier.align(Alignment.CenterHorizontally))
            val left = (c.expiresAt - now) / 1000
            CodeText(c.code)
            Text("Vale por mais %d:%02d".format(left / 60, left % 60), style = MaterialTheme.typography.bodyLarge)
            Text("2. Depois, escaneie o QR Code que vai aparecer no celular dele.")
            BigButton("Escanear resposta do responsável") { go(Step.SCAN_REPLY) }
            BackButton(onClick = { code = null }, text = "Cancelar código")
        }
    }
}

/** Mensagem de erro anunciada pelo leitor de tela assim que aparece. */
@Composable
private fun ErrorText(error: String?) {
    error ?: return
    Text(
        error,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
    )
}

@Composable
private fun CodeText(code: String) = Text(
    "Código: ${code.chunked(3).joinToString(" ")}",
    style = MaterialTheme.typography.headlineMedium,
    modifier = Modifier.semantics { contentDescription = "Código ${code.toList().joinToString(" ")}" },
)

/** Nome, parentesco e (se [askCode]) o código de 6 dígitos. */
@Composable
private fun ReplyForm(
    title: String,
    hint: String,
    askCode: Boolean,
    error: String?,
    onDone: (CaregiverReply) -> Unit,
    onCancel: () -> Unit,
    presetCode: String = "",
) {
    var name by rememberSaveable { mutableStateOf("") }
    var relation by rememberSaveable { mutableStateOf("") }
    var typed by rememberSaveable { mutableStateOf("") }
    var formError by rememberSaveable { mutableStateOf<String?>(null) }
    Screen(title) {
        Text(hint)
        ErrorText(error)
        if (askCode) {
            OutlinedTextField(
                typed, { typed = it.filter(Char::isDigit).take(6) },
                label = { Text("Código de 6 dígitos") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
        }
        OutlinedTextField(name, { name = it.take(MAX_FIELD) }, label = { Text("Nome do responsável") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(relation, { relation = it.take(MAX_FIELD) }, label = { Text("Parentesco (ex.: Filha)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        ErrorText(formError)
        BigButton("Continuar") {
            val c = if (askCode) typed else presetCode
            formError = when {
                c.length != 6 -> "O código tem 6 dígitos."
                name.isBlank() || relation.isBlank() -> "Preencha nome e parentesco."
                else -> null
            }
            if (formError == null) onDone(CaregiverReply(c, name.trim(), relation.trim()))
        }
        BackButton(onCancel)
    }
}

@Composable
private fun QrCode(content: String, description: String, modifier: Modifier = Modifier) {
    val matrix = remember(content) { QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 0, 0) }
    // Fundo branco fixo: leitores de QR precisam de módulos escuros sobre claro, mesmo no tema escuro.
    Box(
        modifier
            .background(Color.White)
            .padding(16.dp)
            .semantics { contentDescription = description },
    ) {
        Canvas(Modifier.size(220.dp)) {
            val cell = size.width / matrix.width
            for (y in 0 until matrix.height) for (x in 0 until matrix.width) {
                if (matrix[x, y]) drawRect(Color.Black, Offset(x * cell, y * cell), Size(cell + 0.5f, cell + 0.5f))
            }
        }
    }
}
