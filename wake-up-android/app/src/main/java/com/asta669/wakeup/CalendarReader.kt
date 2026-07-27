package com.asta669.wakeup

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import java.util.Calendar

/** Reads today's events from the phone's calendar (which already syncs Google
 *  Calendar on Android) — no OAuth, no network, no API key. */
object CalendarReader {

    fun hasPermission(c: Context): Boolean =
        ContextCompat.checkSelfPermission(c, Manifest.permission.READ_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED

    /** Returns a human-readable list of today's events, e.g.
     *  "Réunion à 10:00, Déjeuner à 13:00", or an empty string if none/unavailable. */
    fun todayEvents(c: Context): String {
        if (!hasPermission(c)) return ""

        val dayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val dayEnd = dayStart + 24L * 60 * 60 * 1000

        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        builder.appendPath(dayStart.toString())
        builder.appendPath(dayEnd.toString())

        val projection = arrayOf(
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.ALL_DAY
        )

        val events = mutableListOf<String>()
        try {
            c.contentResolver.query(
                builder.build(), projection, null, null,
                CalendarContract.Instances.BEGIN + " ASC"
            )?.use { cursor ->
                while (cursor.moveToNext() && events.size < 8) {
                    val title = cursor.getString(0)?.takeIf { it.isNotBlank() } ?: continue
                    val begin = cursor.getLong(1)
                    val allDay = cursor.getInt(2) == 1
                    if (allDay) {
                        events.add(title)
                    } else {
                        val cal = Calendar.getInstance().apply { timeInMillis = begin }
                        val h = cal.get(Calendar.HOUR_OF_DAY)
                        val m = cal.get(Calendar.MINUTE)
                        events.add(String.format("%s à %02d:%02d", title, h, m))
                    }
                }
            }
        } catch (e: Exception) {
            return ""
        }
        return events.joinToString(", ")
    }
}
