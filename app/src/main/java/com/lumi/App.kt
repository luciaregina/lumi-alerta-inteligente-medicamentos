package com.lumi

import android.Manifest
import android.content.SharedPreferences
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.lumi.alarm.Alarms
import com.lumi.caregiver.CaregiverScreen
import com.lumi.data.LumiDb
import com.lumi.meds.HomeScreen
import com.lumi.meds.MedsScreen
import com.lumi.onboarding.Onboarding
import com.lumi.settings.SettingsScreen
import com.lumi.stock.StockScreen
import com.lumi.ui.theme.LumiTheme

enum class Tab(val label: String, val icon: String) {
    HOME("Início", "🏠"),
    MEDS("Remédios", "💊"),
    STOCK("Estoque", "📦"),
    CAREGIVER("Cuidador", "👤"),
    SETTINGS("Ajustes", "⚙️"),
}

@Composable
fun LumiApp(prefs: SharedPreferences) {
    var profile by remember { mutableStateOf(prefs.loadProfile()) }
    var userName by remember { mutableStateOf(prefs.getString("name", null)) }
    val context = LocalContext.current
    val dao = remember { LumiDb.get(context).dao() }
    var tab by rememberSaveable { mutableStateOf(Tab.HOME) }
    val setProfile = { p: Profile -> profile = p; prefs.saveProfile(p) }

    LumiTheme(profile) {
        val name = userName
        if (name == null) {
            Scaffold { padding ->
                Box(Modifier.padding(padding)) {
                    Onboarding(onProfile = setProfile, onDone = { n, age ->
                        prefs.edit().putString("name", n).putInt("age", age).apply()
                        userName = n
                        tab = Tab.HOME
                    })
                }
            }
            return@LumiTheme
        }
        // Reagenda os alarmes sempre que a lista de remédios muda.
        val meds by dao.meds().collectAsState(initial = emptyList())
        LaunchedEffect(meds) { Alarms.scheduleAll(context, meds) }
        if (Build.VERSION.SDK_INT >= 33) {
            val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
            LaunchedEffect(Unit) { ask.launch(Manifest.permission.POST_NOTIFICATIONS) }
        }
        Scaffold(bottomBar = {
            NavigationBar {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = t == tab,
                        onClick = { tab = t },
                        // O ícone é decorativo; o leitor de tela lê só o rótulo.
                        icon = { Text(t.icon, Modifier.clearAndSetSemantics {}) },
                        label = { Text(t.label) },
                    )
                }
            }
        }) { padding ->
            Box(Modifier.padding(padding)) {
                when (tab) {
                    Tab.HOME -> HomeScreen(name, dao, onOpenStock = { tab = Tab.STOCK })
                    Tab.MEDS -> MedsScreen(dao)
                    Tab.STOCK -> StockScreen(dao)
                    Tab.CAREGIVER -> CaregiverScreen(dao)
                    Tab.SETTINGS -> SettingsScreen(profile, setProfile, onRedoOnboarding = {
                        prefs.edit().remove("name").remove("age").apply()
                        userName = null
                    })
                }
            }
        }
    }
}
