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

enum class Need(val label: String) {
    VISION("Visão"),
    COLOR("Daltonismo / contraste"),
    HEARING("Audição"),
    MOTOR("Dificuldade motora"),
    MEMORY("Memória / leitura difícil"),
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
fun Onboarding() {
    // rememberSaveable: os dados sobrevivem ao voltar entre passos e à rotação da tela.
    var step by rememberSaveable { mutableIntStateOf(1) }
    var name by rememberSaveable { mutableStateOf("") }
    var age by rememberSaveable { mutableStateOf("") }
    var needs by rememberSaveable { mutableStateOf(emptySet<Need>()) }

    BackHandler(enabled = step > 1) { step-- }

    when (step) {
        1 -> BasicDataStep(name, { name = it }, age, { age = it }, onContinue = { step = 2 })
        2 -> NeedsStep(needs, { needs = it }, onBack = { step = 1 }, onContinue = { step = 3 })
        else -> StepScaffold("Passo 3 de 3", "Em breve") {}
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
                    .heightIn(min = 64.dp)
                    .toggleable(checked, role = Role.Checkbox) { onNeeds(if (it) needs + need else needs - need) }
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = checked, onCheckedChange = null)
                Text(need.label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 16.dp))
            }
        }
        BigButton("Ajustar o app para mim", onContinue)
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
            Text("Voltar", style = MaterialTheme.typography.titleMedium)
        }
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

@Composable
internal fun BigButton(text: String, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}
