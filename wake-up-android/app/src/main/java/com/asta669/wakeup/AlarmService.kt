package com.asta669.wakeup

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat

/** Foreground service that keeps the alarm ringing (sound + vibration) and shows the
 *  full-screen alarm screen over the lock screen until the challenge is solved. */
class AlarmService : Service() {

    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null

    companion object {
        const val ACTION_STOP = "com.asta669.wakeup.STOP"
        private const val CHANNEL_ID = "wakeup_alarm"
        private const val NOTIF_ID = 1
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopEverything()
            return START_NOT_STICKY
        }
        startForegroundWithNotification()
        acquireWakeLock()
        startSound()
        startVibration()
        // Try to bring the full-screen alarm to the foreground directly as well.
        val alarmIntent = Intent(this, AlarmActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try { startActivity(alarmIntent) } catch (_: Exception) {}
        return START_STICKY
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID, "Alarm", NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Wake Up! alarm"
                setSound(null, null) // the service plays the sound itself
                enableVibration(false)
                setBypassDnd(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            nm.createNotificationChannel(channel)
        }
    }

    private fun startForegroundWithNotification() {
        createChannel()
        val fullScreen = PendingIntent.getActivity(
            this, 0, Intent(this, AlarmActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("⏰ WAKE UP!")
            .setContentText("Tap to solve the challenge and stop the alarm")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreen, true)
            .setContentIntent(fullScreen)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIF_ID, notification)
        }
    }

    private fun startSound() {
        // Force the alarm stream to full volume so it can't be quietly dialed down.
        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        try {
            am.setStreamVolume(
                AudioManager.STREAM_ALARM,
                am.getStreamMaxVolume(AudioManager.STREAM_ALARM),
                0
            )
        } catch (_: Exception) {}

        player = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM) // plays even in silent/vibrate mode
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            val afd = resources.openRawResourceFd(R.raw.alarm)
            setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            isLooping = true
            setVolume(1.0f, 1.0f)
            prepare()
            afd.close()
            start()
        }
    }

    private fun startVibration() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        val pattern = longArrayOf(0, 500, 400, 500, 400)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, 0)
        }
    }

    private fun acquireWakeLock() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK, "wakeup:alarm"
        ).also { it.acquire(10 * 60 * 1000L) } // safety timeout: 10 min
    }

    private fun stopEverything() {
        try { player?.stop() } catch (_: Exception) {}
        player?.release(); player = null
        vibrator?.cancel()
        if (wakeLock?.isHeld == true) wakeLock?.release()
        wakeLock = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopEverything()
        super.onDestroy()
    }
}
