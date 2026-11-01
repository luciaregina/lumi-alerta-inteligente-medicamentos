package com.lumi.caregiver

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.lumi.ui.BackButton
import com.lumi.ui.BigButton
import com.lumi.ui.Screen
import kotlinx.coroutines.delay
import java.security.SecureRandom

const val CODE_TTL_MS = 10 * 60_000L

/** Código de acesso de uso único: 6 dígitos, vale 10 minutos. */
data class AccessCode(val code: String, val expiresAt: Long) : java.io.Serializable {
    fun valid(now: Long) = now < expiresAt
}

private val random = SecureRandom()

fun newAccessCode(now: Long) = AccessCode("%06d".format(random.nextInt(1_000_000)), now + CODE_TTL_MS)

@Composable
fun CaregiverScreen() {
    var code by rememberSaveable { mutableStateOf<AccessCode?>(null) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(code) {
        while (code != null) { now = System.currentTimeMillis(); delay(1000) }
    }

    Screen("Responsável") {
        Text("Um familiar ou cuidador pode acompanhar seus remédios e receber avisos.")
        val c = code?.takeIf { it.valid(now) }
        if (c == null) {
            if (code != null) Text("O código expirou. Gere um novo.", style = MaterialTheme.typography.titleMedium)
            BigButton("Gerar QR Code") { code = newAccessCode(System.currentTimeMillis()) }
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
            BackButton(onClick = { code = null }, text = "Cancelar código")
        }
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
