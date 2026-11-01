package com.lumi

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
