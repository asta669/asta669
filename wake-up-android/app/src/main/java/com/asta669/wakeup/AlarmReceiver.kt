package com.asta669.wakeup

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

/** Fired by AlarmManager at the scheduled time. Starts the ringing service and
 *  re-arms the alarm for the next day so it repeats daily. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Start the foreground service that plays the sound and shows the full-screen alarm.
        val serviceIntent = Intent(context, AlarmService::class.java)
        ContextCompat.startForegroundService(context, serviceIntent)

        // Re-arm for the next day (daily repeat) if the alarm is still enabled.
        if (Prefs.isEnabled(context)) {
            AlarmScheduler.schedule(context, Prefs.getHour(context), Prefs.getMinute(context))
        }
    }
}
