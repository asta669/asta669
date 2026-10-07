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
class AlarmSessionTest {
    private lateinit var context: Context

    @Before fun reset() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("alarm_session", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("wakeup_prefs", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test fun completingPreviewKeepsDailyAlarmEnabled() {
        Prefs.setEnabled(context, true)
        val token = AlarmSession.begin(context, preview = true)
        assertTrue(AlarmSession.isPreview(context))
        assertTrue(AlarmSession.clear(context, token))
        assertFalse(AlarmSession.isRinging(context))
        assertTrue(Prefs.isEnabled(context))
    }

    @Test fun repeatedDeliveryReusesSession() {
        val token = AlarmSession.begin(context, preview = false)
        assertEquals(token, AlarmSession.begin(context, preview = false))
        assertEquals(token, AlarmSession.begin(context, preview = true))
        assertFalse(AlarmSession.isPreview(context))
    }

    @Test fun scheduledAlarmTakesOverPreviewAndRejectsStaleStop() {
        val preview = AlarmSession.begin(context, preview = true)
        AlarmSession.rememberVolume(context, 3)
        val alarm = AlarmSession.begin(context, preview = false)
        assertNotEquals(preview, alarm)
        assertFalse(AlarmSession.clear(context, preview))
        assertEquals(alarm, AlarmSession.token(context))
        assertFalse(AlarmSession.isPreview(context))
        assertEquals(3, AlarmSession.previousVolume(context))
    }

    @Test fun keepsVolumeFromBeforeRingingAcrossRepeatedStarts() {
        AlarmSession.begin(context, preview = false)
        AlarmSession.rememberVolume(context, 2)
        AlarmSession.rememberVolume(context, 15)
        assertEquals(2, AlarmSession.previousVolume(context))
    }

    @Test fun clearRemovesVolumeAndTokenAndAllowsFreshSession() {
        val first = AlarmSession.begin(context, preview = false)
        AlarmSession.rememberVolume(context, 4)
        assertTrue(AlarmSession.clear(context, first))
        assertNull(AlarmSession.previousVolume(context))
        assertNull(AlarmSession.token(context))
        assertNotEquals(first, AlarmSession.begin(context, preview = false))
    }

    @Test fun finishingOneSessionCannotClearItsSuccessor() {
        val first = AlarmSession.begin(context, preview = false)
        AlarmSession.clear(context, first)
        val next = AlarmSession.begin(context, preview = true)
        assertFalse(AlarmSession.clear(context, first))
        assertEquals(next, AlarmSession.token(context))
    }
}
