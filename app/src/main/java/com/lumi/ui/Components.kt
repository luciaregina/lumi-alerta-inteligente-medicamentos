package com.lumi.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.lumi.LocalProfile

@Composable
fun Screen(title: String, label: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        label?.let { Text(it, style = MaterialTheme.typography.labelLarge) }
        Text(title, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
        content()
    }
}

/** Ícone + texto. O ícone é decorativo (o leitor de tela lê o texto) e cresce junto com a letra. */
@Composable
fun IconLabel(
    icon: ImageVector,
    text: String,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    modifier: Modifier = Modifier,
) {
    val size = with(LocalDensity.current) { style.fontSize.toDp() } * 1.2f
    val tint = if (color == Color.Unspecified) LocalContentColor.current else color
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size))
        Text(text, style = style, color = color)
    }
}

@Composable
fun SectionTitle(text: String) =
    Text(text, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })

/** Borda grossa quando o controle recebe foco (teclado, switch, leitor de tela). */
@Composable
fun Modifier.focusRing(): Modifier {
    var focused by remember { mutableStateOf(false) }
    val color = MaterialTheme.colorScheme.primary
    return onFocusChanged { focused = it.isFocused }
        .then(if (focused) Modifier.border(3.dp, color, MaterialTheme.shapes.small) else Modifier)
}

private val buttonHeight @Composable get() = if (LocalProfile.current.bigButtons) 72.dp else 56.dp

@Composable
fun BigButton(text: String, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = buttonHeight).focusRing()) {
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun BackButton(onClick: () -> Unit, text: String = "Voltar") {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = buttonHeight).focusRing()) {
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}
