package com.asta669.wakeup

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar
import java.util.TimeZone

/** Daily exact alarm; previews never replace its PendingIntent. */
object AlarmScheduler {
    private const val REQUEST_CODE = 4269
    internal const val ACTION_DAILY_ALARM = "com.asta669.wakeup.DAILY_ALARM"
    private const val STATE = "alarm_schedule"
    private const val NEXT = "next_trigger"

    fun nextTriggerMillis(hour: Int, minute: Int): Long =
        nextTriggerMillis(hour, minute, System.currentTimeMillis(), TimeZone.getDefault())

    internal fun nextTriggerMillis(hour: Int, minute: Int, now: Long, zone: TimeZone): Long {
        require(hour in 0..23 && minute in 0..59) { "Heure invalide." }
        val next = Calendar.getInstance(zone).apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (next.timeInMillis <= now) next.add(Calendar.DAY_OF_YEAR, 1)
        return next.timeInMillis
    }

    private fun operation(context: Context, legacy: Boolean = false): PendingIntent = PendingIntent.getBroadcast(
        context, REQUEST_CODE,
        Intent(context, AlarmReceiver::class.java).apply { if (!legacy) action = ACTION_DAILY_ALARM },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    fun schedule(context: Context, hour: Int, minute: Int) {
        val manager = context.getSystemService(AlarmManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !manager.canScheduleExactAlarms()) {
            throw SecurityException("Autorisez les alarmes exactes dans les réglages Android.")
        }
        val trigger = nextTriggerMillis(hour, minute)
        val show = PendingIntent.getActivity(context, 4268, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        manager.setAlarmClock(AlarmManager.AlarmClockInfo(trigger, show), operation(context))
        // Replace alarms scheduled by older app versions, which used an actionless intent.
        manager.cancel(operation(context, legacy = true))
        context.getSharedPreferences(STATE, Context.MODE_PRIVATE).edit().putLong(NEXT, trigger).apply()
    }

    fun nextScheduledAt(context: Context): Long? =
        context.getSharedPreferences(STATE, Context.MODE_PRIVATE).getLong(NEXT, 0L).takeIf { it > 0 }

    fun cancel(context: Context) {
        val manager = context.getSystemService(AlarmManager::class.java)
        manager.cancel(operation(context))
        manager.cancel(operation(context, legacy = true))
        context.getSharedPreferences(STATE, Context.MODE_PRIVATE).edit().remove(NEXT).apply()
    }
}
