package com.asta669.wakeup

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/** A system alarm delivery can launch the ringing foreground service after the UI is closed. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != null && intent.action != AlarmScheduler.ACTION_DAILY_ALARM) return
        if (!Prefs.isEnabled(context)) return
        // Re-arm independently, even if Android refuses this occurrence's service start.
        try {
            AlarmScheduler.schedule(context, Prefs.getHour(context), Prefs.getMinute(context))
        } catch (error: SecurityException) {
            Log.w("WakeUpAlarm", "Exact alarm permission must be restored.")
        }
        try {
            AlarmService.startScheduled(context)
        } catch (error: RuntimeException) {
            Log.e("WakeUpAlarm", "Android refused the ringing service: ${error.javaClass.simpleName}")
        }
    }
}
