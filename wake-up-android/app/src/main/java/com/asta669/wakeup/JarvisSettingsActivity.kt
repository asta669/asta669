package com.asta669.wakeup

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

/** Lets the user configure Jarvis: enable it, set the free Gemini API key, their
 *  name, their routine, and whether to read the calendar. */
class JarvisSettingsActivity : AppCompatActivity() {

    private lateinit var enable: CheckBox
    private lateinit var useCalendar: CheckBox
    private lateinit var name: EditText
    private lateinit var key: EditText
    private lateinit var model: EditText
    private lateinit var routine: EditText

    private val calendarPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                Toast.makeText(this, "Sans l'accès à l'agenda, Jarvis ne lira pas vos rendez-vous.",
                    Toast.LENGTH_LONG).show()
                useCalendar.isChecked = false
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_jarvis_settings)

        enable = findViewById(R.id.enableJarvis)
        useCalendar = findViewById(R.id.useCalendar)
        name = findViewById(R.id.nameInput)
        key = findViewById(R.id.keyInput)
        model = findViewById(R.id.modelInput)
        routine = findViewById(R.id.routineInput)

        // Load current settings
        enable.isChecked = Prefs.isJarvisOn(this)
        useCalendar.isChecked = Prefs.useCalendar(this)
        name.setText(Prefs.getName(this))
        key.setText(Prefs.getGeminiKey(this))
        model.setText(Prefs.getGeminiModel(this))
        routine.setText(Prefs.getRoutine(this))

        useCalendar.setOnCheckedChangeListener { _, checked ->
            if (checked && !CalendarReader.hasPermission(this)) {
                calendarPermission.launch(Manifest.permission.READ_CALENDAR)
            }
        }

        findViewById<Button>(R.id.saveJarvisBtn).setOnClickListener { save(); finish() }
        findViewById<Button>(R.id.testJarvisBtn).setOnClickListener {
            save()
            startActivity(Intent(this, JarvisActivity::class.java))
        }
    }

    private fun save() {
        Prefs.saveJarvis(
            this,
            on = enable.isChecked,
            key = key.text.toString(),
            model = model.text.toString(),
            name = name.text.toString(),
            routine = routine.text.toString(),
            useCalendar = useCalendar.isChecked
        )
        Toast.makeText(this, "Jarvis enregistré", Toast.LENGTH_SHORT).show()
    }
}
