package com.asta669.wakeup

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.TimePicker
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var timePicker: TimePicker
    private lateinit var difficulty: Spinner
    private lateinit var status: TextView
    private lateinit var setBtn: Button
    private lateinit var cancelBtn: Button

    // Difficulty spinner index -> number of challenges.
    private val diffValues = intArrayOf(1, 3, 5)

    private val notifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* result ignored */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        timePicker = findViewById(R.id.timePicker)
        difficulty = findViewById(R.id.difficultySpinner)
        status = findViewById(R.id.status)
        setBtn = findViewById(R.id.setBtn)
        cancelBtn = findViewById(R.id.cancelBtn)

        timePicker.setIs24HourView(true)

        difficulty.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item,
            listOf("Easy — 1 challenge", "Normal — 3 challenges", "Brutal — 5 challenges")
        )

        // Restore saved settings.
        timePicker.hour = Prefs.getHour(this)
        timePicker.minute = Prefs.getMinute(this)
        difficulty.setSelection(diffValues.indexOf(Prefs.getDifficulty(this)).coerceAtLeast(0))

        setBtn.setOnClickListener { onSetAlarm() }
        cancelBtn.setOnClickListener { onCancelAlarm() }
        findViewById<Button>(R.id.testBtn).setOnClickListener { previewAlarm() }
        findViewById<Button>(R.id.jarvisBtn).setOnClickListener {
            startActivity(Intent(this, JarvisSettingsActivity::class.java))
        }

        refreshStatus()
    }

    private fun onSetAlarm() {
        ensureNotificationPermission()
        if (!ensureExactAlarmPermission()) return

        val hour = timePicker.hour
        val minute = timePicker.minute
        val diff = diffValues[difficulty.selectedItemPosition]

        Prefs.save(this, hour, minute, diff, enabled = true)
        AlarmScheduler.schedule(this, hour, minute)
        refreshStatus()
        Toast.makeText(this, "Alarm set!", Toast.LENGTH_SHORT).show()
    }

    private fun onCancelAlarm() {
        AlarmScheduler.cancel(this)
        Prefs.setEnabled(this, false)
        refreshStatus()
        Toast.makeText(this, "Alarm cancelled", Toast.LENGTH_SHORT).show()
    }

    private fun previewAlarm() {
        ensureNotificationPermission()
        ContextCompat.startForegroundService(this, Intent(this, AlarmService::class.java))
    }

    private fun ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    /** Returns true if we can schedule exact alarms; otherwise sends the user to grant it. */
    private fun ensureExactAlarmPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!am.canScheduleExactAlarms()) {
                Toast.makeText(
                    this,
                    "Please allow \"Alarms & reminders\", then tap Set alarm again.",
                    Toast.LENGTH_LONG
                ).show()
                startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                return false
            }
        }
        return true
    }

    private fun refreshStatus() {
        if (Prefs.isEnabled(this)) {
            val trigger = AlarmScheduler.nextTriggerMillis(Prefs.getHour(this), Prefs.getMinute(this))
            val fmt = SimpleDateFormat("EEE HH:mm", Locale.getDefault())
            status.text = "⏰ Alarm ON — next ring ${fmt.format(Date(trigger))}"
            cancelBtn.isEnabled = true
        } else {
            status.text = "No alarm set"
            cancelBtn.isEnabled = false
        }
    }
}
