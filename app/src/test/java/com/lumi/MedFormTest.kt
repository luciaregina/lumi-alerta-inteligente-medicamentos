package com.lumi

import com.lumi.meds.parseTimes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MedFormTest {
    @Test
    fun validaHorarios() {
        assertEquals(listOf("08:00", "20:00"), parseTimes("20:00, 8:00"))
        assertEquals(listOf("08:00"), parseTimes("08:00, 08:00,"))
        assertNull(parseTimes(""))
        assertNull(parseTimes("8h"))
        assertNull(parseTimes("08:00, 24:00"))
        assertNull(parseTimes("08:60"))
    }
}
