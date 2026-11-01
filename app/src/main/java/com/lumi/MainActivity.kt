package com.lumi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lumi.onboarding.Onboarding
import com.lumi.ui.theme.LumiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val prefs = getSharedPreferences("lumi", MODE_PRIVATE)
        setContent {
            var profile by remember { mutableStateOf(prefs.loadProfile()) }
            var userName by remember { mutableStateOf(prefs.getString("name", null)) }
            LumiTheme(profile) {
                Scaffold { padding ->
                    Box(Modifier.padding(padding)) {
                        val name = userName
                        if (name == null) {
                            Onboarding(
                                onProfile = { profile = it; prefs.saveProfile(it) },
                                onDone = { n, age ->
                                    prefs.edit().putString("name", n).putInt("age", age).apply()
                                    userName = n
                                },
                            )
                        } else {
                            // ponytail: tela provisória até a navegação (SCRUM-18) e o Início (SCRUM-19)
                            Text("Olá, $name!", Modifier.padding(24.dp), style = MaterialTheme.typography.headlineMedium)
                        }
                    }
                }
            }
        }
    }
}
