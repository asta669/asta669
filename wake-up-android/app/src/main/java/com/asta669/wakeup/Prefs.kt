package com.asta669.wakeup

import android.content.Context

/** Tiny wrapper over SharedPreferences to remember the alarm settings. */
object Prefs {
    private const val NAME = "wakeup_prefs"
    private const val K_HOUR = "hour"
    private const val K_MINUTE = "minute"
    private const val K_ENABLED = "enabled"
    private const val K_QR_HASH = "kitchen_qr_hash"
    private const val K_ALARM_SOUND = "alarm_sound"

    private fun sp(c: Context) = c.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun getHour(c: Context) = sp(c).getInt(K_HOUR, 7)
    fun getMinute(c: Context) = sp(c).getInt(K_MINUTE, 0)
    fun isEnabled(c: Context) = sp(c).getBoolean(K_ENABLED, false)
    fun save(c: Context, hour: Int, minute: Int, enabled: Boolean) {
        require(hour in 0..23 && minute in 0..59)
        sp(c).edit()
            .putInt(K_HOUR, hour)
            .putInt(K_MINUTE, minute)
            .remove("difficulty")
            .putBoolean(K_ENABLED, enabled)
            .apply()
    }

    fun setEnabled(c: Context, enabled: Boolean) {
        sp(c).edit().putBoolean(K_ENABLED, enabled).apply()
    }

    fun getQrHash(c: Context): String = sp(c).getString(K_QR_HASH, "") ?: ""
    fun hasKitchenQr(c: Context): Boolean = getQrHash(c).matches(Regex("[a-f0-9]{64}"))
    fun setQrHash(c: Context, hash: String) {
        require(hash.matches(Regex("[a-f0-9]{64}")))
        sp(c).edit().putString(K_QR_HASH, hash).apply()
    }

    fun getAlarmSound(c: Context): String =
        sp(c).getString(K_ALARM_SOUND, "pulse")?.takeIf { it in setOf("pulse", "beacon", "rise") } ?: "pulse"

    fun setAlarmSound(c: Context, sound: String) {
        require(sound in setOf("pulse", "beacon", "rise"))
        sp(c).edit().putString(K_ALARM_SOUND, sound).apply()
    }

    // ---- Jarvis (spoken morning brief) ----
    private const val K_JARVIS_ON = "jarvis_on"
    private const val K_GEMINI_KEY = "gemini_key"
    private const val K_GEMINI_MODEL = "gemini_model"
    private const val K_NAME = "user_name"
    private const val K_ROUTINE = "routine"
    private const val K_USE_CALENDAR = "use_calendar"

    fun isJarvisOn(c: Context) = sp(c).getBoolean(K_JARVIS_ON, false)
    fun getGeminiKey(c: Context) = sp(c).getString(K_GEMINI_KEY, "") ?: ""
    fun getGeminiModel(c: Context) = sp(c).getString(K_GEMINI_MODEL, "gemini-2.0-flash") ?: "gemini-2.0-flash"
    fun getName(c: Context) = sp(c).getString(K_NAME, "Monsieur") ?: "Monsieur"
    fun getRoutine(c: Context) =
        sp(c).getString(K_ROUTINE, "20 min de footing, lire 10 pages, écrire un post sur Substack")
            ?: ""
    fun useCalendar(c: Context) = sp(c).getBoolean(K_USE_CALENDAR, true)

    fun saveJarvis(
        c: Context, on: Boolean, key: String, model: String,
        name: String, routine: String, useCalendar: Boolean
    ) {
        sp(c).edit()
            .putBoolean(K_JARVIS_ON, on)
            .putString(K_GEMINI_KEY, key.trim())
            .putString(K_GEMINI_MODEL, model.trim().ifEmpty { "gemini-2.0-flash" })
            .putString(K_NAME, name.trim().ifEmpty { "Monsieur" })
            .putString(K_ROUTINE, routine.trim())
            .putBoolean(K_USE_CALENDAR, useCalendar)
            .apply()
    }
}
