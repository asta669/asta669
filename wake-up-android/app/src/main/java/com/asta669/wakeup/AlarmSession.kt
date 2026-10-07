package com.asta669.wakeup

import android.content.Context
import java.util.UUID

/** Durable ringing state, kept separate from the user's daily alarm settings. */
object AlarmSession {
    private const val NAME = "alarm_session"
    private const val TOKEN = "token"
    private const val PREVIEW = "preview"
    private const val STARTED = "started"
    private const val VOLUME = "previous_volume"

    private fun prefs(context: Context) = context.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun token(context: Context): String? = prefs(context).getString(TOKEN, null)
    fun isRinging(context: Context) = token(context) != null
    fun isPreview(context: Context) = isRinging(context) && prefs(context).getBoolean(PREVIEW, false)
    fun startedAt(context: Context) = prefs(context).getLong(STARTED, 0L)
    fun previousVolume(context: Context): Int? = prefs(context).let {
        if (it.contains(VOLUME)) it.getInt(VOLUME, 0) else null
    }

    /** A real alarm takes precedence over a preview; repeated deliveries are idempotent. */
    @Synchronized
    internal fun begin(context: Context, preview: Boolean): String {
        val current = token(context)
        if (current != null && (preview || !isPreview(context))) return current
        val next = UUID.randomUUID().toString()
        check(prefs(context).edit().putString(TOKEN, next).putBoolean(PREVIEW, preview)
            .putLong(STARTED, System.currentTimeMillis()).commit()) { "Impossible de mémoriser le réveil actif." }
        return next
    }

    @Synchronized
    internal fun rememberVolume(context: Context, volume: Int) {
        if (previousVolume(context) == null) prefs(context).edit().putInt(VOLUME, volume).commit()
    }

    @Synchronized
    internal fun clear(context: Context, expectedToken: String): Boolean {
        if (token(context) != expectedToken) return false
        return prefs(context).edit().clear().commit()
    }
}
