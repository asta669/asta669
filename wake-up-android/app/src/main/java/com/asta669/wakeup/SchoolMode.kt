package com.asta669.wakeup

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.provider.Settings

/** School only changes ordinary sounds; it never reads or writes STREAM_ALARM. */
object SchoolMode {
    data class Status(
        val active: Boolean,
        val restoring: Boolean,
        val mediaSilent: Boolean,
        val callsSilent: Boolean,
        val notificationsSilent: Boolean,
        val dndAccess: Boolean,
        val alarmsOnly: Boolean,
        val airplaneMode: Boolean
    ) {
        val fullyApplied: Boolean get() = mediaSilent && callsSilent && notificationsSilent && alarmsOnly
    }

    fun isActive(context: Context): Boolean = Store(context).read()?.let { !it.restoring } == true

    fun status(context: Context): Status {
        val snapshot = Store(context).read()
        val system = AndroidSystem(context)
        return Status(
            active = snapshot != null && !snapshot.restoring,
            restoring = snapshot?.restoring == true,
            mediaSilent = system.read(SchoolSetting.MEDIA) == 0,
            callsSilent = system.read(SchoolSetting.RING) == 0 && system.read(SchoolSetting.RINGER) == AudioManager.RINGER_MODE_SILENT,
            notificationsSilent = system.read(SchoolSetting.NOTIFICATIONS) == 0,
            dndAccess = system.canChangeDnd(),
            alarmsOnly = system.read(SchoolSetting.DND) == NotificationManager.INTERRUPTION_FILTER_ALARMS,
            airplaneMode = Settings.Global.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) == 1
        )
    }

    fun enable(context: Context): List<String> = SchoolController(Store(context), AndroidSystem(context)).enable().problems
    fun disable(context: Context): List<String> = SchoolController(Store(context), AndroidSystem(context)).disable().problems

    private class AndroidSystem(context: Context) : SchoolSystem {
        private val audio = context.getSystemService(AudioManager::class.java)
        private val notifications = context.getSystemService(NotificationManager::class.java)

        override fun read(setting: SchoolSetting): Int = when (setting) {
            SchoolSetting.MEDIA -> audio.getStreamVolume(AudioManager.STREAM_MUSIC)
            SchoolSetting.RING -> audio.getStreamVolume(AudioManager.STREAM_RING)
            SchoolSetting.NOTIFICATIONS -> audio.getStreamVolume(AudioManager.STREAM_NOTIFICATION)
            SchoolSetting.RINGER -> audio.ringerMode
            SchoolSetting.DND -> notifications.currentInterruptionFilter
        }

        override fun set(setting: SchoolSetting, value: Int) {
            when (setting) {
                SchoolSetting.MEDIA -> audio.setStreamVolume(AudioManager.STREAM_MUSIC, value, 0)
                SchoolSetting.RING -> audio.setStreamVolume(AudioManager.STREAM_RING, value, 0)
                SchoolSetting.NOTIFICATIONS -> audio.setStreamVolume(AudioManager.STREAM_NOTIFICATION, value, 0)
                SchoolSetting.RINGER -> audio.ringerMode = value
                SchoolSetting.DND -> notifications.setInterruptionFilter(value)
            }
        }

        override fun canChangeDnd(): Boolean = notifications.isNotificationPolicyAccessGranted
    }

    private class Store(context: Context) : SchoolStateStore {
        private val prefs = context.getSharedPreferences("school_mode_v1", Context.MODE_PRIVATE)

        override fun read(): SchoolSnapshot? {
            if (!prefs.getBoolean("has_snapshot", false)) return null
            val values = SchoolSetting.entries.associateWith { prefs.getInt("before_${it.name}", 0) }
            val pending = prefs.getStringSet("pending", emptySet()).orEmpty()
                .mapNotNull { name -> SchoolSetting.entries.firstOrNull { it.name == name } }.toSet()
            return SchoolSnapshot(values, pending, prefs.getBoolean("restoring", false))
        }

        override fun write(snapshot: SchoolSnapshot): Boolean {
            val editor = prefs.edit().putBoolean("has_snapshot", true)
                .putBoolean("restoring", snapshot.restoring)
                .putStringSet("pending", snapshot.pending.map { it.name }.toSet())
            snapshot.values.forEach { (setting, value) -> editor.putInt("before_${setting.name}", value) }
            return editor.commit()
        }

        override fun clear(): Boolean = prefs.edit().clear().commit()
    }
}
