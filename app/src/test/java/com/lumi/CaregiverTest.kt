package com.lumi

import com.lumi.caregiver.AccessCode
import com.lumi.caregiver.CODE_TTL_MS
import com.lumi.caregiver.accepts
import com.lumi.caregiver.newAccessCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CaregiverTest {
    @Test
    fun codigoDeSeisDigitosComValidade() {
        repeat(50) {
            val c = newAccessCode(1_000)
            assertEquals(6, c.code.length)
            assertTrue(c.code.all(Char::isDigit))
            assertTrue(c.valid(1_000 + CODE_TTL_MS - 1))
            assertFalse(c.valid(1_000 + CODE_TTL_MS))
        }
    }

    @Test
    fun aceitaSoCodigoCertoENoPrazo() {
        val c = AccessCode("123456", 2_000)
        assertTrue(c.accepts(" 123456 ", 1_000))
        assertFalse(c.accepts("654321", 1_000))
        assertFalse(c.accepts("123456", 2_000))
        assertFalse((null as AccessCode?).accepts("123456", 1_000))
    }

    @Test
    fun avisosDeDoseNaoConfirmadaEEstoque() {
        val m = com.lumi.data.Medication(1, "Losartana", "1 comprimido", listOf("08:00", "20:00"), 4)
        val now = java.time.LocalDateTime.of(2026, 11, 1, 8, 31)
        assertEquals(
            listOf("Dose não confirmada: Losartana das 08:00", "Estoque acabando: Losartana (2 dia(s))"),
            com.lumi.caregiver.caregiverAlerts(listOf(m), emptyList(), now),
        )
        val taken = listOf(com.lumi.data.DoseTaken("2026-11-01", 1, "08:00"))
        assertEquals(1, com.lumi.caregiver.caregiverAlerts(listOf(m), taken, now).size)
        // Dentro da tolerância de 30 min ainda não avisa
        assertEquals(1, com.lumi.caregiver.caregiverAlerts(listOf(m), emptyList(), now.minusMinutes(1)).size)
    }
}
