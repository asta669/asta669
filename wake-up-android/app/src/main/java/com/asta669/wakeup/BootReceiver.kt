package com.asta669.wakeup

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Re-arms the alarm after the phone reboots (scheduled alarms are cleared on reboot). */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_LOCKED_BOOT_COMPLETED
        ) {
            if (Prefs.isEnabled(context)) {
                AlarmScheduler.schedule(context, Prefs.getHour(context), Prefs.getMinute(context))
            }
        }
    }
}
