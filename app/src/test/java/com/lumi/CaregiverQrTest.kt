package com.lumi

import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.lumi.caregiver.AccessCode
import com.lumi.caregiver.CaregiverReply
import com.lumi.caregiver.accepts
import com.lumi.caregiver.decodeQr
import com.lumi.caregiver.parsePatientQr
import com.lumi.caregiver.parseReplyQr
import com.lumi.caregiver.patientQr
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class CaregiverQrTest {
    @Test
    fun qrDoPaciente() {
        assertEquals("123456", parsePatientQr(patientQr("123456")))
        assertEquals("123456", parsePatientQr("  lumi:123456\n"))
        assertNull(parsePatientQr("lumi:12345"))
        assertNull(parsePatientQr("lumi:12345a"))
        assertNull(parsePatientQr("https://exemplo.com"))
        assertNull(parsePatientQr(CaregiverReply("123456", "Ana", "Filha").toQr()))
    }

    @Test
    fun respostaDoResponsavelIdaEVolta() {
        val r = CaregiverReply("042917", "Ana Lúcia: a filha", "Filha mais velha")
        assertEquals(r, parseReplyQr(r.toQr()))
    }

    @Test
    fun respostaInvalidaEhRecusada() {
        assertNull(parseReplyQr("lumi:123456"))
        assertNull(parseReplyQr("lumi-resp:123456:Ana"))
        assertNull(parseReplyQr("lumi-resp:12345:Ana:Filha"))
        assertNull(parseReplyQr("lumi-resp:123456:%20:Filha"))
        assertNull(parseReplyQr("lumi-resp:123456:Ana:Filha:extra"))
        assertNull(parseReplyQr("lumi-resp:123456:%ZZ:Filha"))
        assertNull(parseReplyQr("lumi-resp:123456:${"a".repeat(41)}:Filha"))
    }

    @Test
    fun respostaSoVinculaComCodigoCertoENoPrazo() {
        val code = AccessCode("123456", 2_000)
        val ok = parseReplyQr(CaregiverReply("123456", "Ana", "Filha").toQr())!!
        val wrong = parseReplyQr(CaregiverReply("654321", "Ana", "Filha").toQr())!!
        assert(code.accepts(ok.code, 1_000))
        assertFalse(code.accepts(wrong.code, 1_000))
        assertFalse(code.accepts(ok.code, 2_000))
    }

    @Test
    fun leQrDeUmaImagemEmTonsDeCinza() {
        val content = CaregiverReply("123456", "José", "Neto").toQr()
        val m = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 300, 300)
        val pixels = ByteArray(m.width * m.height) { i -> if (m[i % m.width, i / m.width]) 0 else -1 }
        assertEquals(content, decodeQr(pixels, m.width, m.height))
        assertNull(decodeQr(ByteArray(100 * 100) { -1 }, 100, 100))
    }
}
