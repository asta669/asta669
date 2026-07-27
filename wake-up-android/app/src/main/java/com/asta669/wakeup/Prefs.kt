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
}
