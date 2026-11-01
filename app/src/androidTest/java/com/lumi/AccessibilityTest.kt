package com.lumi

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.lumi.data.LumiDb
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Checagens automáticas de acessibilidade (SCRUM-36). O roteiro manual com TalkBack
 * está em docs/acessibilidade.md.
 */
@RunWith(AndroidJUnit4::class)
class AccessibilityTest {
    @get:Rule val compose = createComposeRule()

    @get:Rule val notifications: GrantPermissionRule =
        if (Build.VERSION.SDK_INT >= 33) GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)
        else GrantPermissionRule.grant()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val prefs = context.getSharedPreferences("lumi-test", Context.MODE_PRIVATE)

    @Before
    fun limpa() {
        prefs.edit().clear().commit()
        LumiDb.get(context).clearAllTables()
        compose.setContent { LumiApp(prefs) }
    }

    private fun cadastra(nome: String, idade: String) {
        compose.onNodeWithText("Seu nome").performTextInput(nome)
        compose.onNodeWithText("Sua idade").performTextInput(idade)
        compose.onNodeWithText("Continuar").performClick()
    }

    @Test
    fun cadastroMostraErrosClaros() {
        compose.onNodeWithText("Continuar").performClick()
        compose.onNodeWithText("Digite seu nome.").assertIsDisplayed()
        compose.onNodeWithText("Digite sua idade.").assertIsDisplayed()
    }

    @Test
    fun necessidadesSaoCheckboxesGrandes() {
        cadastra("Maria", "40")
        val checkbox = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox)
        compose.onAllNodes(checkbox).assertCountEquals(5)
        listOf("Visão", "Daltonismo / contraste", "Audição", "Dificuldade motora", "Memória / leitura difícil").forEach {
            compose.onNode(checkbox and androidx.compose.ui.test.hasText(it)).assertHeightIsAtLeast(48.dp)
        }
    }

    @Test
    fun perfilVisaoAplicaAjustesEMantemSelecaoAoVoltar() {
        cadastra("Maria", "70")
        compose.onNodeWithText("Visão").performClick()
        compose.onNodeWithText("Voltar").performClick()
        compose.onNodeWithText("Continuar").performClick() // nome e idade preservados
        compose.onNodeWithText("Ajustar o app para mim").performScrollTo().performClick()
        compose.onNodeWithText("Letras extra grandes (150%)").assertIsDisplayed()
        compose.onNodeWithText("Alto contraste (amarelo sobre preto) e avisos sempre com texto").assertIsDisplayed()
        compose.onNodeWithText("Alarme falado").assertIsDisplayed()
    }

    @Test
    fun abasTemRotuloEAlvoDeToqueMinimo() {
        cadastra("Maria", "40")
        compose.onNodeWithText("Ajustar o app para mim").performScrollTo().performClick()
        compose.onNodeWithText("Começar a usar").performScrollTo().performClick()
        listOf("Início", "Remédios", "Estoque", "Cuidador", "Ajustes").forEach { aba ->
            compose.onNode(androidx.compose.ui.test.hasText(aba) and hasClickAction(), useUnmergedTree = false)
                .assertHeightIsAtLeast(48.dp)
                .performClick()
                .assertIsSelected()
        }
        compose.onAllNodesWithText("Ajustes").assertCountEquals(2) // rótulo da aba + título da tela
    }
}
