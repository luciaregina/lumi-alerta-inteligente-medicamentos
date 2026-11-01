package com.lumi

import com.lumi.alarm.alarmSpeech
import com.lumi.data.Medication
import org.junit.Assert.assertEquals
import org.junit.Test

class AlarmTest {
    @Test
    fun textoFalado() {
        val m = Medication(name = "Losartana", dose = "1 comprimido", times = listOf("08:00"), stock = 10)
        assertEquals("Hora do remédio. Losartana. 1 comprimido. 08:00", alarmSpeech(m, "08:00"))
    }
}
