package com.asta669.wakeup

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ServiceController
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.time.Duration

/** Lifecycle/stream checks in Android's simulated runtime; hardware loudness needs a real phone. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
@LooperMode(LooperMode.Mode.PAUSED)
class AlarmServiceTest {
    private lateinit var context: Context
    private lateinit var audio: AudioManager
    private lateinit var controller: ServiceController<AlarmService>
    private lateinit var service: AlarmService
    private val initialVolume = 2

    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("alarm_session", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("wakeup_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        audio = context.getSystemService(AudioManager::class.java)
        audio.setStreamVolume(AudioManager.STREAM_ALARM, initialVolume, 0)
        controller = Robolectric.buildService(AlarmService::class.java).create()
        service = controller.get()
    }

    @After fun destroy() { controller.destroy() }

    private fun begin(preview: Boolean = false): String {
        val token = AlarmSession.begin(context, preview)
        service.onStartCommand(Intent(context, AlarmService::class.java)
            .setAction("com.asta669.wakeup.START_RINGING")
            .putExtra(AlarmService.EXTRA_SESSION_TOKEN, token), 0, 1)
        return token
    }

    @Test fun realDismissalRestoresVolumeAndKeepsNextAlarmEnabled() {
        Prefs.setEnabled(context, true)
        val token = begin()
        assertEquals(audio.getStreamMaxVolume(AudioManager.STREAM_ALARM), audio.getStreamVolume(AudioManager.STREAM_ALARM))
        AlarmService.stopAfterQr(context, token)
        assertEquals(initialVolume, audio.getStreamVolume(AudioManager.STREAM_ALARM))
        assertFalse(AlarmSession.isRinging(context))
        assertTrue(Prefs.isEnabled(context))
    }

    @Test fun maximumIsRestoredAfterExternalVolumeReduction() {
        begin()
        audio.setStreamVolume(AudioManager.STREAM_ALARM, 0, 0)
        shadowOf(android.os.Looper.getMainLooper()).idleFor(Duration.ofMillis(251))
        assertEquals(audio.getStreamMaxVolume(AudioManager.STREAM_ALARM), audio.getStreamVolume(AudioManager.STREAM_ALARM))
    }

    @Test fun stoppingPreviewCannotStopRealAlarm() {
        val preview = begin(preview = true)
        val real = begin(preview = false)
        AlarmService.stopPreview(context, preview)
        assertEquals(real, AlarmSession.token(context))
        AlarmService.stopPreview(context, real)
        assertEquals(real, AlarmSession.token(context))
        AlarmService.stopAfterQr(context, real)
        assertEquals(initialVolume, audio.getStreamVolume(AudioManager.STREAM_ALARM))
    }

    @Test fun repeatedStartsPreserveTheOriginalVolume() {
        val token = begin()
        assertEquals(token, begin())
        AlarmService.stopForEmergency(context, token)
        assertEquals(initialVolume, audio.getStreamVolume(AudioManager.STREAM_ALARM))
    }

    @Test fun staleQrCompletionDoesNotStopNewSession() {
        val first = begin()
        AlarmService.stopAfterQr(context, first)
        val next = begin()
        AlarmService.stopAfterQr(context, first)
        assertEquals(next, AlarmSession.token(context))
    }
}
