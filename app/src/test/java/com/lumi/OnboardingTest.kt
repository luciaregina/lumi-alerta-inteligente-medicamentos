package com.lumi

import com.lumi.onboarding.Need
import com.lumi.onboarding.autoConfig
import com.lumi.onboarding.summary
import com.lumi.onboarding.validateBasicData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingTest {
    @Test
    fun validaNomeEIdade() {
        assertTrue(validateBasicData("Maria", "70").ok)
        assertEquals("Digite seu nome.", validateBasicData("  ", "70").name)
        assertEquals("Digite sua idade.", validateBasicData("Maria", "").age)
        assertEquals("A idade deve ser entre 1 e 120 anos.", validateBasicData("Maria", "0").age)
        assertEquals("A idade deve ser entre 1 e 120 anos.", validateBasicData("Maria", "121").age)
        assertTrue(validateBasicData("Maria", "120").ok)
    }
}

class AutoConfigTest {
    @Test
    fun aplicaRegrasDoMock() {
        assertEquals(Profile(), autoConfig(30, emptySet()))
        assertEquals(listOf("Perfil padrão"), Profile().summary())

        val idoso = autoConfig(65, emptySet())
        assertEquals(Profile(fontScale = 1.25f, bigButtons = true, simpleMode = true), idoso)

        // Visão vence a escala de idade (1,5x > 1,25x)
        val visao = autoConfig(70, setOf(Need.VISION))
        assertEquals(1.5f, visao.fontScale)
        assertTrue(visao.highContrast && visao.spokenAlarm && visao.bigButtons)

        assertTrue(autoConfig(30, setOf(Need.COLOR)).highContrast)
        assertEquals(Profile(flashAlarm = true, vibration = true), autoConfig(30, setOf(Need.HEARING)))
        assertEquals(Profile(bigButtons = true), autoConfig(30, setOf(Need.MOTOR)))
        assertEquals(Profile(simpleMode = true), autoConfig(30, setOf(Need.MEMORY)))
    }
}
