package com.asta669.wakeup

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.util.Log

/** Restore future scheduling only; Android 15 forbids starting media playback from boot. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val accepted = setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_MY_PACKAGE_REPLACED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED)
        if (intent.action !in accepted) return
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // A reboot may preserve the forced volume, but not the ringing service.
            AlarmSession.previousVolume(context)?.let { previous ->
                val manager = context.getSystemService(AudioManager::class.java)
                try { manager.setStreamVolume(AudioManager.STREAM_ALARM,
                    previous.coerceIn(0, manager.getStreamMaxVolume(AudioManager.STREAM_ALARM)), 0)
                } catch (_: SecurityException) { }
            }
            // Do not resurrect a previous boot's ringing or preview session.
            AlarmSession.token(context)?.let { AlarmSession.clear(context, it) }
        }
        if (!Prefs.isEnabled(context)) return
        try {
            AlarmScheduler.schedule(context, Prefs.getHour(context), Prefs.getMinute(context))
        } catch (error: SecurityException) {
            Log.w("WakeUpAlarm", "Alarm remains saved; exact alarm permission is required to re-arm it.")
        }
    }
}
