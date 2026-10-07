package com.asta669.wakeup

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SchoolActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_school)
        findViewById<Button>(R.id.schoolBack).setOnClickListener { finish() }
        findViewById<Button>(R.id.schoolOn).setOnClickListener {
            showFeedback(SchoolMode.enable(this))
            refresh()
        }
        findViewById<Button>(R.id.schoolOff).setOnClickListener {
            showFeedback(SchoolMode.disable(this))
            refresh()
        }
        findViewById<Button>(R.id.schoolPermission).setOnClickListener {
            openSettings(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
        }
        findViewById<Button>(R.id.schoolAirplane).setOnClickListener {
            openSettings(Settings.ACTION_AIRPLANE_MODE_SETTINGS)
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val status = SchoolMode.status(this)
        findViewById<TextView>(R.id.schoolState).text = when {
            status.restoring -> "Restauration à terminer"
            status.active && status.fullyApplied -> "School ON"
            status.active -> "School ON · partiel"
            else -> "School OFF"
        }
        findViewById<TextView>(R.id.schoolDetails).text = if (status.active || status.restoring) {
            "Médias : ${if (status.mediaSilent) "silencieux" else "audibles"}\n" +
                "Appels : ${if (status.callsSilent) "silencieux" else "silence non confirmé"}\n" +
                "Notifications : ${if (status.notificationsSilent) "silencieuses" else "silence non confirmé"}\n" +
                "Filtre alarmes seules : ${if (status.alarmsOnly) "actif" else "non appliqué"}" +
                if (status.restoring) "\nVos réglages précédents sont conservés. Réessayez School OFF après avoir accordé l’accès nécessaire." else ""
        } else {
            "Prêt pour le cours, sir. Activez School pour mettre les sons ordinaires en pause."
        }
        findViewById<TextView>(R.id.schoolPermissionStatus).text = if (status.dndAccess) {
            "Accès accordé. School peut filtrer les interruptions en laissant passer les alarmes."
        } else {
            "Accès manquant. Autorisez Wake Up dans les réglages, revenez ici et appuyez sur School ON. Sans cet accès, le silence peut être partiel."
        }
        findViewById<TextView>(R.id.schoolAirplaneState).text = "Mode avion · ${if (status.airplaneMode) "activé" else "désactivé"}"
        findViewById<Button>(R.id.schoolOn).isEnabled = !status.restoring
        findViewById<Button>(R.id.schoolOff).isEnabled = status.active || status.restoring
    }

    private fun showFeedback(problems: List<String>) {
        findViewById<TextView>(R.id.schoolFeedback).apply {
            text = problems.joinToString("\n")
            visibility = if (problems.isEmpty()) View.GONE else View.VISIBLE
        }
    }

    private fun openSettings(action: String) {
        try {
            startActivity(Intent(action))
        } catch (_: ActivityNotFoundException) {
            showFeedback(listOf("Ce raccourci est indisponible. Ouvrez les paramètres du téléphone pour modifier ce réglage."))
        } catch (_: SecurityException) {
            showFeedback(listOf("Android refuse l’ouverture de ce réglage. Ouvrez les paramètres du téléphone."))
        }
    }
}
