package com.lumi

import com.lumi.data.Converters
import com.lumi.data.Medication
import org.junit.Assert.assertEquals
import org.junit.Test

class MedicationTest {
    @Test
    fun calculaDiasDeEstoqueEConverteHorarios() {
        val m = Medication(name = "X", dose = "1 comprimido", times = listOf("08:00", "20:00"), stock = 9)
        assertEquals(2, m.dosesPerDay)
        assertEquals(4, m.daysLeft)
        val c = Converters()
        assertEquals(listOf("08:00", "20:00"), c.toList(c.fromList(m.times)))
        assertEquals(emptyList<String>(), c.toList(c.fromList(emptyList())))
    }
}
