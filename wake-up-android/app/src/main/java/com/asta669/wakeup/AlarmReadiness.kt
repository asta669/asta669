package com.asta669.wakeup

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

/** Inspect actual Android grants rather than claiming that an alarm screen is guaranteed. */
object AlarmReadiness {
    const val CHANNEL_ID = "wakeup_alarm"

    data class Snapshot(
        val exactAlarms: Boolean,
        val notifications: Boolean,
        val fullScreen: Boolean,
        val channelEnabled: Boolean
    ) {
        val ready get() = exactAlarms && notifications && fullScreen && channelEnabled
    }

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(
            CHANNEL_ID, "Réveil", NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Sonnerie et accès au QR code du réveil"
            // Sound is owned by AlarmService, avoiding two simultaneous audio sources.
            setSound(null, null)
            enableVibration(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        })
    }

    fun snapshot(context: Context): Snapshot {
        ensureChannel(context)
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        val channel = notificationManager.getNotificationChannel(CHANNEL_ID)
        return Snapshot(
            exactAlarms = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms(),
            notifications = NotificationManagerCompat.from(context).areNotificationsEnabled(),
            fullScreen = Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE || notificationManager.canUseFullScreenIntent(),
            // Full-screen and heads-up presentation need a high-importance channel.
            channelEnabled = (channel?.importance ?: 0) >= NotificationManager.IMPORTANCE_HIGH
        )
    }

    fun exactAlarmSettingsIntent(context: Context): Intent =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))
        else appSettingsIntent(context)

    fun fullScreenSettingsIntent(context: Context): Intent =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:${context.packageName}"))
        else notificationSettingsIntent(context)

    fun notificationSettingsIntent(context: Context) = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)

    fun channelSettingsIntent(context: Context) = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        .putExtra(Settings.EXTRA_CHANNEL_ID, CHANNEL_ID)

    fun appSettingsIntent(context: Context) = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.parse("package:${context.packageName}"))
}
