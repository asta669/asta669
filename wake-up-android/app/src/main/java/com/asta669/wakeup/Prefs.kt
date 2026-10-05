package com.asta669.wakeup

import android.content.Context

/** Tiny wrapper over SharedPreferences to remember the alarm settings. */
object Prefs {
    private const val NAME = "wakeup_prefs"
    private const val K_HOUR = "hour"
    private const val K_MINUTE = "minute"
    private const val K_ENABLED = "enabled"
    private const val K_DIFFICULTY = "difficulty"

    private fun sp(c: Context) = c.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun getHour(c: Context) = sp(c).getInt(K_HOUR, 7)
    fun getMinute(c: Context) = sp(c).getInt(K_MINUTE, 0)
    fun isEnabled(c: Context) = sp(c).getBoolean(K_ENABLED, false)
    /** Number of challenges required to dismiss the alarm. */
    fun getDifficulty(c: Context) = sp(c).getInt(K_DIFFICULTY, 3)

    fun save(c: Context, hour: Int, minute: Int, difficulty: Int, enabled: Boolean) {
        sp(c).edit()
            .putInt(K_HOUR, hour)
            .putInt(K_MINUTE, minute)
            .putInt(K_DIFFICULTY, difficulty)
            .putBoolean(K_ENABLED, enabled)
            .apply()
    }

    fun setEnabled(c: Context, enabled: Boolean) {
        sp(c).edit().putBoolean(K_ENABLED, enabled).apply()
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
