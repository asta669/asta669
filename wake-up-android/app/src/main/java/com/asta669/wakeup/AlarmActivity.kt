package com.asta669.wakeup

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.random.Random

/** Full-screen "WAKE UP!" screen shown over the lock screen. The alarm keeps
 *  ringing (via AlarmService) until the user solves the required number of
 *  math challenges. */
class AlarmActivity : AppCompatActivity() {

    private var remaining = 3
    private var total = 3
    private var answer = 0

    private lateinit var progress: TextView
    private lateinit var question: TextView
    private lateinit var input: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Show over the lock screen and turn the screen on.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )

        setContentView(R.layout.activity_alarm)

        total = Prefs.getDifficulty(this).coerceIn(1, 5)
        remaining = total

        progress = findViewById(R.id.progress)
        question = findViewById(R.id.question)
        input = findViewById(R.id.answerInput)

        findViewById<Button>(R.id.submitBtn).setOnClickListener { checkAnswer() }
        findViewById<Button>(R.id.snoozeBtn).setOnClickListener { snooze() }

        nextChallenge()
    }

    private fun nextChallenge() {
        if (remaining <= 0) {
            dismiss()
            return
        }
        progress.text = "Challenge ${total - remaining + 1} of $total"
        val a = Random.nextInt(2, 40)
        val b = Random.nextInt(2, 40)
        if (Random.nextBoolean()) {
            answer = a + b
            question.text = "$a + $b = ?"
        } else {
            answer = a - b
            question.text = "$a − $b = ?"
        }
        input.text.clear()
    }

    private fun checkAnswer() {
        val entered = input.text.toString().trim().toIntOrNull()
        if (entered != null && entered == answer) {
            remaining--
            nextChallenge()
        } else {
            input.text.clear()
            question.animate().alpha(0.3f).setDuration(100).withEndAction {
                question.animate().alpha(1f).duration = 150
            }
        }
    }

    private fun stopAlarmService() {
        val stop = Intent(this, AlarmService::class.java).setAction(AlarmService.ACTION_STOP)
        startService(stop)
    }

    private fun dismiss() {
        stopAlarmService()
        // Hand off to Jarvis for the spoken morning brief, if enabled.
        if (Prefs.isJarvisOn(this)) {
            startActivity(Intent(this, JarvisActivity::class.java))
        }
        finish()
    }

    private fun snooze() {
        stopAlarmService()
        AlarmScheduler.snooze(this, 5)
        finish()
    }

    // Block the back button — you must solve the challenge to leave.
    @Deprecated("Intentionally blocking back to force waking up")
    override fun onBackPressed() {
        // do nothing
    }
}
