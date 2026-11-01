package com.lumi.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.lumi.LocalProfile
import com.lumi.Profile

enum class Need(val label: String) {
    VISION("Visão"),
    COLOR("Daltonismo / contraste"),
    HEARING("Audição"),
    MOTOR("Dificuldade motora"),
    MEMORY("Memória / leitura difícil"),
}

/** Regras de autoconfiguração a partir de idade e necessidades. */
fun autoConfig(age: Int, needs: Set<Need>): Profile {
    var p = Profile()
    if (age >= 60) p = p.copy(fontScale = 1.25f, bigButtons = true, simpleMode = true)
    if (Need.VISION in needs) p = p.copy(fontScale = 1.5f, highContrast = true, spokenAlarm = true)
    if (Need.COLOR in needs) p = p.copy(highContrast = true)
    if (Need.HEARING in needs) p = p.copy(flashAlarm = true, vibration = true)
    if (Need.MOTOR in needs) p = p.copy(bigButtons = true)
    if (Need.MEMORY in needs) p = p.copy(simpleMode = true)
    return p
}

/** O que foi ajustado, em frases para o passo 3. */
fun Profile.summary(): List<String> = buildList {
    if (fontScale > 1f) add("Letras ${if (fontScale >= 1.5f) "extra grandes" else "maiores"} (${(fontScale * 100).toInt()}%)")
    if (bigButtons) add("Botões e áreas de toque maiores")
    if (highContrast) add("Alto contraste (amarelo sobre preto) e avisos sempre com texto")
    if (spokenAlarm) add("Alarme falado")
    if (flashAlarm) add("Alarme com tela piscando")
    if (vibration) add("Alarme com vibração")
    if (simpleMode) add("Modo simples: uma ação por vez")
    if (isEmpty()) add("Perfil padrão")
}

/** Erros do passo 1; null quando o campo está ok. */
data class BasicDataErrors(val name: String?, val age: String?) {
    val ok get() = name == null && age == null
}

fun validateBasicData(name: String, age: String): BasicDataErrors = BasicDataErrors(
    name = if (name.isBlank()) "Digite seu nome." else null,
    age = when (age.toIntOrNull()) {
        null -> "Digite sua idade."
        !in 1..120 -> "A idade deve ser entre 1 e 120 anos."
        else -> null
    },
)

@Composable
fun Onboarding(onProfile: (Profile) -> Unit, onDone: (name: String, age: Int) -> Unit) {
    // rememberSaveable: os dados sobrevivem ao voltar entre passos e à rotação da tela.
    var step by rememberSaveable { mutableIntStateOf(1) }
    var name by rememberSaveable { mutableStateOf("") }
    var age by rememberSaveable { mutableStateOf("") }
    var needs by rememberSaveable { mutableStateOf(emptySet<Need>()) }

    BackHandler(enabled = step > 1) { step-- }

    when (step) {
        1 -> BasicDataStep(name, { name = it }, age, { age = it }, onContinue = { step = 2 })
        2 -> NeedsStep(needs, { needs = it }, onBack = { step = 1 }, onContinue = {
            onProfile(autoConfig(age.toInt(), needs)) // aplica já, antes de mostrar o resumo
            step = 3
        })
        else -> SummaryStep(onBack = { step = 2 }, onDone = { onDone(name.trim(), age.toInt()) })
    }
}

@Composable
private fun SummaryStep(onBack: () -> Unit, onDone: () -> Unit) {
    StepScaffold("Passo 3 de 3", "Pronto! Ajustamos o app para você") {
        LocalProfile.current.summary().forEach { Text("✓ $it", style = MaterialTheme.typography.titleMedium) }
        Text("Você pode mudar isso quando quiser em Ajustes.")
        BigButton("Começar a usar", onDone)
        BackButton(onBack)
    }
}

@Composable
private fun BasicDataStep(
    name: String,
    onName: (String) -> Unit,
    age: String,
    onAge: (String) -> Unit,
    onContinue: () -> Unit,
) {
    var showErrors by rememberSaveable { mutableStateOf(false) }
    val errors = validateBasicData(name, age)

    StepScaffold("Passo 1 de 3", "Bem-vindo ao LUMI") {
        Text("Vamos começar com alguns dados para deixar o app do seu jeito.")
        OutlinedTextField(
            value = name,
            onValueChange = onName,
            label = { Text("Seu nome") },
            isError = showErrors && errors.name != null,
            supportingText = errors.name?.takeIf { showErrors }?.let { { Text(it) } },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = age,
            onValueChange = { onAge(it.filter(Char::isDigit).take(3)) },
            label = { Text("Sua idade") },
            isError = showErrors && errors.age != null,
            supportingText = errors.age?.takeIf { showErrors }?.let { { Text(it) } },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth(),
        )
        BigButton("Continuar") { if (errors.ok) onContinue() else showErrors = true }
    }
}

@Composable
private fun NeedsStep(
    needs: Set<Need>,
    onNeeds: (Set<Need>) -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit,
) {
    StepScaffold("Passo 2 de 3", "Precisa de alguma ajuda?") {
        Text("Marque o que combina com você. Pode marcar várias opções ou nenhuma.")
        Need.entries.forEach { need ->
            val checked = need in needs
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = if (LocalProfile.current.bigButtons) 80.dp else 64.dp)
                    .toggleable(checked, role = Role.Checkbox) { onNeeds(if (it) needs + need else needs - need) }
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = checked, onCheckedChange = null)
                Text(need.label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 16.dp))
            }
        }
        BigButton("Ajustar o app para mim", onContinue)
        BackButton(onBack)
    }
}

@Composable
internal fun StepScaffold(stepLabel: String, title: String, content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stepLabel, style = MaterialTheme.typography.labelLarge)
        Text(title, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
        content()
    }
}

private val buttonHeight @Composable get() = if (LocalProfile.current.bigButtons) 72.dp else 56.dp

@Composable
internal fun BigButton(text: String, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = buttonHeight)) {
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
internal fun BackButton(onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = buttonHeight)) {
        Text("Voltar", style = MaterialTheme.typography.titleMedium)
    }
}
