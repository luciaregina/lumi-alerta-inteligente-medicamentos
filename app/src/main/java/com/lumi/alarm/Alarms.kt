package com.lumi.alarm

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.lumi.R
import com.lumi.data.LumiDb
import com.lumi.data.Medication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

private const val CHANNEL = "doses"
const val EXTRA_MED = "medId"
const val EXTRA_TIME = "time"
private const val EXTRA_SNOOZE = "snooze"

/** Próximo disparo de um horário "HH:MM": hoje, se ainda não passou; senão amanhã. */
fun nextTrigger(time: String, now: LocalDateTime): LocalDateTime {
    val today = now.toLocalDate().atTime(LocalTime.parse(time))
    return if (today.isAfter(now)) today else today.plusDays(1)
}

object Alarms {
    /** Agenda um disparo exato (acorda o aparelho) para a dose. */
    fun at(context: Context, medId: Long, time: String, atMillis: Long, requestCode: Int, snooze: Boolean = false) {
        val am = context.getSystemService(AlarmManager::class.java)
        val pi = PendingIntent.getBroadcast(
            context, requestCode,
            Intent(context, AlarmReceiver::class.java)
                .putExtra(EXTRA_MED, medId).putExtra(EXTRA_TIME, time).putExtra(EXTRA_SNOOZE, snooze),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pi)
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pi)
        }
    }

    /** "Lembrar em 10 min": código próprio para não substituir o alarme diário. */
    fun snooze(context: Context, medId: Long, time: String) =
        at(context, medId, time, System.currentTimeMillis() + 10 * 60_000, -code(medId, time), snooze = true)

    /** Agenda o próximo disparo diário de um horário. */
    fun daily(context: Context, medId: Long, time: String, now: LocalDateTime = LocalDateTime.now()) {
        val at = nextTrigger(time, now).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        at(context, medId, time, at, code(medId, time))
    }

    /** (Re)agenda todos os horários. Remédios removidos são ignorados quando o alarme tocar. */
    fun scheduleAll(context: Context, meds: List<Medication>) =
        meds.forEach { m -> m.times.forEach { daily(context, m.id, it) } }

    fun code(medId: Long, time: String) = (medId * 10_000 + time.replace(":", "").toInt()).toInt()

    fun screenIntent(context: Context, medId: Long, time: String) =
        Intent(context, AlarmActivity::class.java)
            .putExtra(EXTRA_MED, medId).putExtra(EXTRA_TIME, time)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

    /** Notificação com tela cheia: é o jeito permitido de abrir o alarme com o app fechado. */
    fun show(context: Context, medId: Long, time: String) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "Horário dos remédios", NotificationManager.IMPORTANCE_HIGH),
        )
        val open = PendingIntent.getActivity(
            context, code(medId, time), screenIntent(context, medId, time),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Hora do remédio")
            .setContentText("Dose das $time")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setFullScreenIntent(open, true)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        if (NotificationManagerCompat.from(context).areNotificationsEnabled()) nm.notify(code(medId, time), n)
    }

    fun dismiss(context: Context, medId: Long, time: String) =
        context.getSystemService(NotificationManager::class.java).cancel(code(medId, time))
}

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val medId = intent.getLongExtra(EXTRA_MED, 0)
        val time = intent.getStringExtra(EXTRA_TIME) ?: return
        val snooze = intent.getBooleanExtra(EXTRA_SNOOZE, false)
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = LumiDb.get(context).dao()
                val med = dao.med(medId)
                if (med != null && time in med.times) {
                    // Alarme diário é de disparo único: já deixa o de amanhã agendado.
                    if (!snooze) Alarms.daily(context, medId, time, LocalDateTime.now().plusMinutes(1))
                    if (!dao.isTaken(LocalDate.now().toString(), medId, time)) Alarms.show(context, medId, time)
                }
            } finally {
                pending.finish()
            }
        }
    }
}

/** Alarmes do AlarmManager somem ao reiniciar o celular: agenda tudo de novo. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Alarms.scheduleAll(context, LumiDb.get(context).dao().allMeds())
            } finally {
                pending.finish()
            }
        }
    }
}
