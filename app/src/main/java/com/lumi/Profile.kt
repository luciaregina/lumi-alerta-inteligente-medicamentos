package com.lumi

import android.content.SharedPreferences
import androidx.compose.runtime.staticCompositionLocalOf

enum class ThemeMode(val label: String) { AUTO("Automático"), LIGHT("Claro"), DARK("Escuro") }

/** Preferências de acessibilidade aplicadas no app todo. */
data class Profile(
    val fontScale: Float = 1f,
    val bigButtons: Boolean = false,
    val simpleMode: Boolean = false,
    val highContrast: Boolean = false,
    val spokenAlarm: Boolean = false,
    val flashAlarm: Boolean = false,
    val vibration: Boolean = false,
    val theme: ThemeMode = ThemeMode.AUTO,
)

val LocalProfile = staticCompositionLocalOf { Profile() }

fun SharedPreferences.loadProfile() = Profile(
    fontScale = getFloat("fontScale", 1f),
    bigButtons = getBoolean("bigButtons", false),
    simpleMode = getBoolean("simpleMode", false),
    highContrast = getBoolean("highContrast", false),
    spokenAlarm = getBoolean("spokenAlarm", false),
    flashAlarm = getBoolean("flashAlarm", false),
    vibration = getBoolean("vibration", false),
    theme = ThemeMode.valueOf(getString("theme", null) ?: ThemeMode.AUTO.name),
)

fun SharedPreferences.saveProfile(p: Profile) = edit()
    .putFloat("fontScale", p.fontScale)
    .putBoolean("bigButtons", p.bigButtons)
    .putBoolean("simpleMode", p.simpleMode)
    .putBoolean("highContrast", p.highContrast)
    .putBoolean("spokenAlarm", p.spokenAlarm)
    .putBoolean("flashAlarm", p.flashAlarm)
    .putBoolean("vibration", p.vibration)
    .putString("theme", p.theme.name)
    .apply()
