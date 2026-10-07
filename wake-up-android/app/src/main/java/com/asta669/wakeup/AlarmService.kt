package com.asta669.wakeup

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

/** A single sound owner, independent of the application's task in the recent-apps screen. */
class AlarmService : Service() {
    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var runningToken: String? = null
    private var originalVolume: Int? = null
    private val handler = Handler(Looper.getMainLooper())
    private var guardRunning = false

    companion object {
        const val EXTRA_SESSION_TOKEN = "alarm_session_token"
        private const val ACTION_START = "com.asta669.wakeup.START_RINGING"
        private const val ACTION_STOP_QR = "com.asta669.wakeup.STOP_QR"
        private const val ACTION_STOP_PREVIEW = "com.asta669.wakeup.STOP_PREVIEW"
        private const val ACTION_STOP_EMERGENCY = "com.asta669.wakeup.STOP_EMERGENCY"
        private const val NOTIFICATION_ID = 1
        private var activeService: AlarmService? = null
        const val PREVIEW_DURATION_MILLIS = 60_000L

        fun startPreview(context: Context): String = start(context, preview = true)
        internal fun startScheduled(context: Context): String = start(context, preview = false)

        private fun start(context: Context, preview: Boolean): String {
            val previousToken = AlarmSession.token(context)
            val token = AlarmSession.begin(context, preview)
            try {
                ContextCompat.startForegroundService(context, Intent(context, AlarmService::class.java)
                    .setAction(ACTION_START).putExtra(EXTRA_SESSION_TOKEN, token))
            } catch (error: RuntimeException) {
                if (previousToken == null) AlarmSession.clear(context, token)
                throw error
            }
            return token
        }

        fun stopPreview(context: Context, expectedToken: String) =
            requestStop(context, expectedToken, ACTION_STOP_PREVIEW)

        fun stopAfterQr(context: Context, expectedToken: String) =
            requestStop(context, expectedToken, ACTION_STOP_QR)

        fun stopForEmergency(context: Context, expectedToken: String) =
            requestStop(context, expectedToken, ACTION_STOP_EMERGENCY)

        private fun requestStop(context: Context, expectedToken: String, action: String) {
            if (AlarmSession.token(context) != expectedToken) return
            if (action == ACTION_STOP_PREVIEW && !AlarmSession.isPreview(context)) return
            val live = activeService
            if (live != null && Looper.myLooper() == Looper.getMainLooper()) {
                live.finishRinging(expectedToken)
                return
            }
            context.startService(Intent(context, AlarmService::class.java).setAction(action)
                .putExtra(EXTRA_SESSION_TOKEN, expectedToken))
        }

        /** Best effort: Android/system settings can still override an ordinary application. */
        fun enforceMaxVolume(context: Context) {
            val manager = context.getSystemService(AudioManager::class.java)
            try {
                val max = manager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                if (manager.getStreamVolume(AudioManager.STREAM_ALARM) != max)
                    manager.setStreamVolume(AudioManager.STREAM_ALARM, max, 0)
            } catch (_: SecurityException) {
                // DND policy or device policy may prevent a stream change.
            }
        }
    }

    private val volumeGuard = object : Runnable {
        override fun run() {
            if (!guardRunning) return
            val token = AlarmSession.token(this@AlarmService)
            if (token == null) {
                finishRinging()
                return
            }
            if (AlarmSession.isPreview(this@AlarmService) &&
                System.currentTimeMillis() - AlarmSession.startedAt(this@AlarmService) >= PREVIEW_DURATION_MILLIS) {
                finishRinging(token)
                return
            }
            enforceMaxVolume(this@AlarmService)
            // Refresh the bounded CPU lock while an alarm is actively ringing.
            if (wakeLock?.isHeld != true) acquireWakeLock()
            handler.postDelayed(this, 250L)
        }
    }

    override fun onCreate() {
        super.onCreate()
        activeService = this
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val token = AlarmSession.token(this)
        val expected = intent?.getStringExtra(EXTRA_SESSION_TOKEN)
        val stopAction = intent?.action in setOf(ACTION_STOP_QR, ACTION_STOP_PREVIEW, ACTION_STOP_EMERGENCY)
        if (stopAction) {
            if (token != null && expected == token &&
                (intent?.action != ACTION_STOP_PREVIEW || AlarmSession.isPreview(this))) {
                finishRinging(token)
                return START_NOT_STICKY
            }
            if (token == null) stopSelf(startId)
            return if (token == null) START_NOT_STICKY else START_STICKY
        }
        // A sticky service only resumes a persisted, unfinished session. Stale starts are ignored.
        if (token == null) {
            stopSelf(startId)
            return START_NOT_STICKY
        }
        if (intent != null && (intent.action != ACTION_START || expected != token)) return START_STICKY
        if (AlarmSession.isPreview(this) &&
            System.currentTimeMillis() - AlarmSession.startedAt(this) >= PREVIEW_DURATION_MILLIS) {
            finishRinging(token)
            return START_NOT_STICKY
        }

        startForegroundWithNotification(token)
        runningToken = token
        if (originalVolume == null) {
            val manager = getSystemService(AudioManager::class.java)
            AlarmSession.rememberVolume(this, manager.getStreamVolume(AudioManager.STREAM_ALARM))
            originalVolume = AlarmSession.previousVolume(this)
        }
        enforceMaxVolume(this)
        acquireWakeLock()
        if (player == null) startSound()
        if (vibrator == null) startVibration()
        if (!guardRunning) {
            guardRunning = true
            handler.post(volumeGuard)
        }
        // The full-screen PendingIntent is dispatched by Android. A direct background
        // startActivity call is intentionally unnecessary and restricted on modern Android.
        return START_STICKY
    }

    private fun startForegroundWithNotification(token: String) {
        AlarmReadiness.ensureChannel(this)
        val openAlarm = PendingIntent.getActivity(this, 4270,
            Intent(this, AlarmActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(EXTRA_SESSION_TOKEN, token),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val preview = AlarmSession.isPreview(this)
        val builder = NotificationCompat.Builder(this, AlarmReadiness.CHANNEL_ID)
            .setContentTitle(if (preview) "Essai du réveil" else "Votre réveil sonne, sir")
            .setContentText(if (preview) "Touchez pour terminer l’essai" else "Touchez pour scanner le QR code de la cuisine")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(runningToken == token)
            .setContentIntent(openAlarm)
            .addAction(0, "Ouvrir le réveil", openAlarm)
        val notificationManager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
            notificationManager.canUseFullScreenIntent()) {
            builder.setFullScreenIntent(openAlarm, true)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            startForeground(NOTIFICATION_ID, builder.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        else startForeground(NOTIFICATION_ID, builder.build())
    }

    private fun audioAttributes() = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()

    private fun startSound() {
        val resource = when (Prefs.getAlarmSound(this)) {
            "beacon" -> R.raw.alarm_beacon
            "rise" -> R.raw.alarm_rise
            else -> R.raw.alarm_pulse
        }
        val next = MediaPlayer()
        try {
            next.setAudioAttributes(audioAttributes())
            resources.openRawResourceFd(resource).use { source ->
                next.setDataSource(source.fileDescriptor, source.startOffset, source.length)
            }
            next.isLooping = true
            next.setVolume(1f, 1f)
            next.prepare()
            next.start()
            player = next
        } catch (error: Exception) {
            next.release()
            Log.e("WakeUpAlarm", "Selected sound unavailable; using the Android alarm tone.")
            val fallback = MediaPlayer()
            try {
                fallback.setAudioAttributes(audioAttributes())
                fallback.setDataSource(this, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
                fallback.isLooping = true
                fallback.prepare()
                fallback.start()
                player = fallback
            } catch (_: Exception) {
                fallback.release()
                Log.e("WakeUpAlarm", "Audio could not start; vibration and alarm notification remain active.")
            }
        }
    }

    private fun startVibration() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            getSystemService(VibratorManager::class.java).defaultVibrator
        else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 500, 400, 500, 400), 0),
            audioAttributes())
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val manager = getSystemService(PowerManager::class.java)
        if (wakeLock == null) wakeLock = manager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "wakeup:alarm")
        wakeLock?.acquire(10 * 60_000L)
    }

    private fun finishRinging(expectedToken: String? = null) {
        if (expectedToken != null && AlarmSession.token(this) != expectedToken) return
        // Restore before clearing persisted state and before any subsequent Jarvis speech.
        releaseResources()
        expectedToken?.let { AlarmSession.clear(this, it) }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun releaseResources() {
        guardRunning = false
        handler.removeCallbacks(volumeGuard)
        player?.let {
            try { it.stop() } catch (_: IllegalStateException) { }
            it.release()
        }
        player = null
        vibrator?.cancel()
        vibrator = null
        if (wakeLock?.isHeld == true) wakeLock?.release()
        wakeLock = null
        val restore = originalVolume ?: AlarmSession.previousVolume(this)
        if (restore != null) {
            val manager = getSystemService(AudioManager::class.java)
            try { manager.setStreamVolume(AudioManager.STREAM_ALARM,
                restore.coerceIn(0, manager.getStreamMaxVolume(AudioManager.STREAM_ALARM)), 0)
            } catch (_: SecurityException) { }
        }
        originalVolume = null
        runningToken = null
    }

    override fun onDestroy() {
        if (activeService === this) activeService = null
        releaseResources()
        super.onDestroy()
    }
}
