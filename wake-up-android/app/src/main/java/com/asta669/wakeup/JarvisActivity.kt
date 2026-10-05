package com.asta669.wakeup

import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

/** Shows and speaks the morning brief right after the alarm is dismissed. */
class JarvisActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var brief: String? = null

    private lateinit var briefView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        setContentView(R.layout.activity_jarvis)
        briefView = findViewById(R.id.briefText)
        briefView.text = "Jarvis prépare votre briefing…"

        findViewById<Button>(R.id.doneBtn).setOnClickListener {
            stopSpeaking()
            finish()
        }
        findViewById<Button>(R.id.repeatBtn).setOnClickListener { speak() }

        tts = TextToSpeech(this, this)

        val events = if (Prefs.useCalendar(this)) CalendarReader.todayEvents(this) else ""
        JarvisBrain.generate(
            name = Prefs.getName(this),
            routine = Prefs.getRoutine(this),
            events = events,
            apiKey = Prefs.getGeminiKey(this),
            model = Prefs.getGeminiModel(this)
        ) { text ->
            runOnUiThread {
                brief = text
                briefView.text = text
                speak()
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.FRENCH
            ttsReady = true
            speak()
        }
    }

    private fun speak() {
        val text = brief ?: return
        if (!ttsReady) return
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jarvis-brief")
    }

    private fun stopSpeaking() {
        tts?.stop()
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        super.onDestroy()
    }
}
