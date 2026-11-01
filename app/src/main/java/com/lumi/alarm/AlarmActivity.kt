package com.lumi.alarm

import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.lumi.data.LumiDb
import com.lumi.data.Medication
import com.lumi.loadProfile
import com.lumi.ui.focusRing
import com.lumi.ui.theme.LumiTheme
import kotlinx.coroutines.launch
import java.util.Locale

fun alarmSpeech(m: Medication, time: String) = "Hora do remédio. ${m.name}. ${m.dose}. $time"

/** Alarme em tela cheia, aparece até com o celular bloqueado. */
class AlarmActivity : ComponentActivity() {
    private var tts: TextToSpeech? = null
    private val vibrator by lazy { getSystemService(Vibrator::class.java) }

    override fun onDestroy() {
        tts?.shutdown()
        vibrator.cancel()
        super.onDestroy()
    }

    private fun speak(text: String) {
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.forLanguageTag("pt-BR")
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "alarm")
            }
        }
    }

    /** Usuário pediu "remover animações" no sistema. */
    private fun reducedMotion() =
        Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val medId = intent.getLongExtra(EXTRA_MED, 0)
        val time = intent.getStringExtra(EXTRA_TIME) ?: return finish()
        val dao = LumiDb.get(this).dao()
        val profile = getSharedPreferences("lumi", MODE_PRIVATE).loadProfile()
        Alarms.dismiss(this, medId, time)

        setContent {
            var med by remember { mutableStateOf<Medication?>(null) }
            LaunchedEffect(medId) {
                val m = dao.med(medId) ?: return@LaunchedEffect finish()
                med = m
                if (profile.spokenAlarm) speak(alarmSpeech(m, time))
                if (profile.vibration) vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 800, 400), 0))
            }
            LumiTheme(profile) {
                med?.let { m ->
                    AlarmScreen(
                        m, time, flash = profile.flashAlarm && !reducedMotion(),
                        onTaken = { lifecycleScope.launch { dao.markTaken(m.id, time); finish() } },
                        onSnooze = { Alarms.snooze(this, m.id, time); finish() },
                    )
                }
            }
        }
    }
}

@Composable
private fun AlarmScreen(m: Medication, time: String, flash: Boolean, onTaken: () -> Unit, onSnooze: () -> Unit) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    // Pisca 1 vez por segundo: abaixo do limite de 3 por segundo (WCAG 2.3.1) para não causar crises.
    val phase by rememberInfiniteTransition(label = "flash").animateFloat(
        0f, 1f, infiniteRepeatable(tween(1000, easing = LinearEasing)), label = "flash",
    )
    val on = flash && phase < 0.5f
    val colors = MaterialTheme.colorScheme
    Surface(
        Modifier.fillMaxSize().semantics { paneTitle = "Alarme de remédio" },
        color = if (on) colors.primary else colors.surface,
        contentColor = if (on) colors.onPrimary else colors.onSurface,
    ) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "Hora do remédio",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
            )
            Text(m.name, style = MaterialTheme.typography.displaySmall, textAlign = TextAlign.Center)
            Text(m.dose, style = MaterialTheme.typography.headlineMedium)
            Text(time, style = MaterialTheme.typography.displayMedium)
            Button(
                onClick = onTaken,
                modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp).focusRequester(focus).focusRing(),
            ) { Text("Tomei", style = MaterialTheme.typography.headlineSmall) }
            OutlinedButton(
                onClick = onSnooze,
                modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp).focusRing(),
            ) { Text("Lembrar em 10 min", style = MaterialTheme.typography.titleLarge) }
        }
    }
}
