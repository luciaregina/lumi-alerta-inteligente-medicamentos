package com.lumi

import com.lumi.meds.sampleSize
import org.junit.Assert.assertEquals
import org.junit.Test

class PrescriptionPhotoTest {
    @Test
    fun reduzSemFicarAbaixoDoTamanhoMaximo() {
        assertEquals(1, sampleSize(1200, 900))    // já é pequena
        assertEquals(1, sampleSize(3000, 2000))   // 3000/2 = 1500 < 1600: mantém e reduz no scale
        assertEquals(2, sampleSize(4000, 3000))   // 4000/2 = 2000 >= 1600
        assertEquals(4, sampleSize(3000, 8000))   // lado maior na vertical
    }
}
