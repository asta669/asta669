package com.asta669.wakeup

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PrefsIntegrationTest {
    private lateinit var context: Context

    @Before fun clear() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("wakeup_prefs", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test fun scheduledAlarmSurvivesFreshContextAndDropsLegacyDifficulty() {
        context.getSharedPreferences("wakeup_prefs", Context.MODE_PRIVATE).edit()
            .putInt("difficulty", 5).commit()
        Prefs.save(context, 7, 30, true)
        val fresh = context.createConfigurationContext(context.resources.configuration)
        assertTrue(Prefs.isEnabled(fresh))
        assertEquals(7, Prefs.getHour(fresh))
        assertEquals(30, Prefs.getMinute(fresh))
        assertFalse(fresh.getSharedPreferences("wakeup_prefs", Context.MODE_PRIVATE).contains("difficulty"))
    }

    @Test fun cancelPreservesChosenTimeAndKitchenCode() {
        val hash = "a".repeat(64)
        Prefs.setQrHash(context, hash)
        Prefs.save(context, 6, 45, true)
        Prefs.setEnabled(context, false)
        assertFalse(Prefs.isEnabled(context))
        assertEquals(6, Prefs.getHour(context))
        assertEquals(45, Prefs.getMinute(context))
        assertEquals(hash, Prefs.getQrHash(context))
    }

    @Test fun invalidEnrollmentCannotOverwritePreviousCode() {
        Prefs.setQrHash(context, "b".repeat(64))
        try {
            Prefs.setQrHash(context, "https://example.invalid/not-a-hash")
            fail("Invalid hash must be rejected")
        } catch (_: IllegalArgumentException) { }
        assertEquals("b".repeat(64), Prefs.getQrHash(context))
    }

    @Test fun corruptSoundSettingFallsBackToBundledSound() {
        context.getSharedPreferences("wakeup_prefs", Context.MODE_PRIVATE).edit()
            .putString("alarm_sound", "../../outside.wav").commit()
        assertEquals("pulse", Prefs.getAlarmSound(context))
    }

    @Test fun invalidTimeCannotEraseExistingAlarm() {
        Prefs.save(context, 8, 15, true)
        try {
            Prefs.save(context, 25, 80, false)
            fail("Invalid time must be rejected")
        } catch (_: IllegalArgumentException) { }
        assertEquals(8, Prefs.getHour(context))
        assertTrue(Prefs.isEnabled(context))
    }
}
