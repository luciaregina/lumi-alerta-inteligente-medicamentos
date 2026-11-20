package com.lumi.caregiver

import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.ChecksumException
import com.google.zxing.DecodeHintType
import com.google.zxing.FormatException
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import java.net.URLDecoder
import java.net.URLEncoder

/*
 * Vínculo offline por troca de QR Codes (SCRUM-38), sem servidor:
 * 1. O paciente mostra "lumi:<código>".
 * 2. O responsável escaneia, preenche nome e parentesco e mostra "lumi-resp:<código>:<nome>:<parentesco>".
 * 3. O paciente escaneia a resposta, confere o código (prazo e uso único) e cria o vínculo.
 */

private const val PATIENT_PREFIX = "lumi:"
private const val REPLY_PREFIX = "lumi-resp:"
const val MAX_FIELD = 40
private val SIX_DIGITS = Regex("\\d{6}")

fun patientQr(code: String) = PATIENT_PREFIX + code

/** Código de 6 dígitos do QR do paciente, ou null se não for um QR do LUMI. */
fun parsePatientQr(text: String): String? =
    text.trim().takeIf { it.startsWith(PATIENT_PREFIX) }?.removePrefix(PATIENT_PREFIX)?.takeIf { it.matches(SIX_DIGITS) }

/** O que o celular do responsável devolve para o paciente. */
data class CaregiverReply(val code: String, val name: String, val relation: String) {
    fun toQr() = REPLY_PREFIX + listOf(code, name, relation).joinToString(":") { URLEncoder.encode(it, "UTF-8") }
}

/** Resposta do responsável, ou null se o QR não for uma resposta válida do LUMI. */
fun parseReplyQr(text: String): CaregiverReply? {
    val t = text.trim()
    if (!t.startsWith(REPLY_PREFIX)) return null
    val parts = t.removePrefix(REPLY_PREFIX).split(":")
    if (parts.size != 3) return null
    val (code, name, relation) = try {
        parts.map { URLDecoder.decode(it, "UTF-8").trim() }
    } catch (_: IllegalArgumentException) {
        return null
    }
    if (!code.matches(SIX_DIGITS)) return null
    if (name.isBlank() || relation.isBlank() || name.length > MAX_FIELD || relation.length > MAX_FIELD) return null
    return CaregiverReply(code, name, relation)
}

private val hints = mapOf(DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE))

/** Procura um QR Code numa imagem em tons de cinza (plano Y da câmera, 1 byte por pixel). */
fun decodeQr(luminance: ByteArray, width: Int, height: Int): String? = try {
    val source = PlanarYUVLuminanceSource(luminance, width, height, 0, 0, width, height, false)
    QRCodeReader().decode(BinaryBitmap(HybridBinarizer(source)), hints).text
} catch (_: NotFoundException) {
    null
} catch (_: ChecksumException) {
    null
} catch (_: FormatException) {
    null
}
