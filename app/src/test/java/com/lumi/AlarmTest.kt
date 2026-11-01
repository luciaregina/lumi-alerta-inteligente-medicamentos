package com.lumi

import com.lumi.alarm.alarmSpeech
import com.lumi.data.Medication
import org.junit.Assert.assertEquals
import org.junit.Test

class AlarmTest {
    @Test
    fun proximoDisparoHojeOuAmanha() {
        val now = java.time.LocalDateTime.of(2026, 11, 1, 9, 30)
        assertEquals(java.time.LocalDateTime.of(2026, 11, 1, 20, 0), com.lumi.alarm.nextTrigger("20:00", now))
        assertEquals(java.time.LocalDateTime.of(2026, 11, 2, 8, 0), com.lumi.alarm.nextTrigger("08:00", now))
        assertEquals(java.time.LocalDateTime.of(2026, 11, 2, 9, 30), com.lumi.alarm.nextTrigger("09:30", now))
    }

    @Test
    fun textoFalado() {
        val m = Medication(name = "Losartana", dose = "1 comprimido", times = listOf("08:00"), stock = 10)
        assertEquals("Hora do remédio. Losartana. 1 comprimido. 08:00", alarmSpeech(m, "08:00"))
    }
}
