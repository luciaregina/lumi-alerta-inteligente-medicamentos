package com.lumi.settings

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.lumi.ui.BigButton
import com.lumi.ui.Screen

@Composable
fun SettingsScreen(onRedoOnboarding: () -> Unit) {
    Screen("Ajustes") {
        Text("Quer responder o cadastro de novo e reajustar o app?")
        BigButton("Refazer cadastro", onRedoOnboarding)
    }
}
