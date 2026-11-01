package com.lumi

import com.lumi.caregiver.CODE_TTL_MS
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
}
