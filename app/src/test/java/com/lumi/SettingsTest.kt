package com.lumi

import com.lumi.settings.stepFont
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsTest {
    @Test
    fun tamanhoDaLetraFicaEntre90e200() {
        assertEquals(1.1f, stepFont(1f, 1))
        assertEquals(1.4f, stepFont(1.25f, 1)) // 125% do cadastro arredonda para 130% e sobe
        assertEquals(2f, stepFont(2f, 1))
        assertEquals(0.9f, stepFont(0.9f, -1))
    }
}
