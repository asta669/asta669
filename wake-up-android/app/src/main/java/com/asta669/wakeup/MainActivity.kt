package com.asta669.wakeup

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private var hour = 7
    private var minute = 0
    private val notifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        render()
    }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        hour = state?.getInt("hour") ?: Prefs.getHour(this)
        minute = state?.getInt("minute") ?: Prefs.getMinute(this)
    }
    override fun onResume() { super.onResume(); render() }
    override fun onSaveInstanceState(out: Bundle) {
        out.putInt("hour", hour); out.putInt("minute", minute)
        super.onSaveInstanceState(out)
    }

    private fun render() {
        val main = ScreenUi.screen(this, "Bonjour, sir.", "Un réveil. Un mouvement. Votre journée commence.")
        if (AlarmSession.isRinging(this)) {
            val active = ScreenUi.card(main)
            ScreenUi.text(active, if (AlarmSession.isPreview(this)) "Test sonore en cours" else "Le réveil vous attend", 21, true)
            ScreenUi.button(active, "Ouvrir le réveil") { openAlarm() }
            if (AlarmSession.isPreview(this)) ScreenUi.button(active, "Arrêter le test", true) {
                AlarmSession.token(this)?.let { AlarmService.stopPreview(this, it) }
                main.postDelayed({ render() }, 350)
            }
        }
        val hero = ScreenUi.card(main)
        ScreenUi.text(hero, "VOTRE PROCHAIN RÉVEIL", 11, secondary = true).letterSpacing = .1f
        val clock = ScreenUi.text(hero, String.format(Locale.ROOT, "%02d:%02d", hour, minute), 64, true)
        clock.contentDescription = "Modifier l’heure du réveil"
        clock.isFocusable = true
        clock.setOnClickListener {
            TimePickerDialog(this, { _, h, m -> hour = h; minute = m; render() }, hour, minute, true).show()
        }
        val due = AlarmScheduler.nextScheduledAt(this) ?: AlarmScheduler.nextTriggerMillis(Prefs.getHour(this), Prefs.getMinute(this))
        ScreenUi.text(hero, if (Prefs.isEnabled(this))
            "Activé · ${SimpleDateFormat("EEE HH:mm", Locale.FRENCH).format(Date(due))} · chaque jour"
            else "Désactivé · touchez l’heure pour la choisir", 14, secondary = true)
        ScreenUi.button(hero, if (Prefs.isEnabled(this)) "Mettre à jour le réveil" else "Activer le réveil") { setAlarm() }
        if (Prefs.isEnabled(this)) ScreenUi.button(hero, "Désactiver", true) {
            if (realAlarm()) { openAlarm(); return@button }
            AlarmScheduler.cancel(this); Prefs.setEnabled(this, false); render()
        }
        val qr = ScreenUi.card(main)
        ScreenUi.text(qr, "Direction la cuisine", 22, true)
        ScreenUi.text(qr, if (Prefs.hasKitchenQr(this)) "Votre QR code est associé. Scannez-le pour arrêter la sonnerie."
            else "Associez un QR code et placez-le dans la cuisine. C’est votre clé pour arrêter le réveil.", secondary = true)
        ScreenUi.button(qr, if (Prefs.hasKitchenQr(this)) "Gérer mon QR code" else "Associer mon QR code", true) {
            if (realAlarm()) openAlarm() else startActivity(Intent(this, QrSetupActivity::class.java))
        }
        val sound = ScreenUi.card(main)
        ScreenUi.text(sound, "Une énergie nouvelle", 22, true)
        val labels = arrayOf("Pulsation", "Balise", "Ascension")
        val values = arrayOf("pulse", "beacon", "rise")
        val current = values.indexOf(Prefs.getAlarmSound(this)).coerceAtLeast(0)
        ScreenUi.text(sound, "${labels[current]} · volume maximal pendant l’alarme", secondary = true)
        ScreenUi.button(sound, "Choisir la sonnerie", true) {
            if (AlarmSession.isRinging(this)) { openAlarm(); return@button }
            MaterialAlertDialogBuilder(this).setTitle("Votre sonnerie")
                .setSingleChoiceItems(labels, current) { dialog, which ->
                    Prefs.setAlarmSound(this, values[which]); dialog.dismiss(); render()
                }.setNegativeButton("Fermer", null).show()
        }
        ScreenUi.button(sound, "Tester le réveil", true) {
            if (AlarmSession.isRinging(this)) { openAlarm(); return@button }
            MaterialAlertDialogBuilder(this).setTitle("Test au volume maximal")
                .setMessage("Le test joue la sonnerie immédiatement au maximum. Vous pourrez l’arrêter avec son bouton. Il s’arrête aussi après une minute.")
                .setNegativeButton("Annuler", null).setPositiveButton("Lancer le test") { _, _ ->
                    try { AlarmService.startPreview(this); openAlarm() }
                    catch (_: RuntimeException) { message("Android n’a pas autorisé le démarrage du test.") }
                }.show()
        }
        val school = ScreenUi.card(main)
        ScreenUi.text(school, "School", 24, true)
        ScreenUi.text(school, if (SchoolMode.isActive(this)) "ON · votre espace de concentration"
            else "Le calme pour apprendre. Vos alarmes restent prioritaires.", secondary = true)
        ScreenUi.button(school, "Ouvrir School", true) { startActivity(Intent(this, SchoolActivity::class.java)) }
        val jarvis = ScreenUi.card(main)
        ScreenUi.text(jarvis, "Votre matin, accompagné", 22, true)
        ScreenUi.text(jarvis, "Jarvis peut lire votre briefing après le réveil. Un moment pour préparer la suite.", secondary = true)
        ScreenUi.button(jarvis, "Personnaliser Jarvis", true) { startActivity(Intent(this, JarvisSettingsActivity::class.java)) }
        val ready = AlarmReadiness.snapshot(this)
        val permissions = ScreenUi.card(main)
        ScreenUi.text(permissions, "Prêt pour demain", 22, true)
        ScreenUi.text(permissions, listOf(
            "Alarmes exactes" to ready.exactAlarms,
            "Notifications" to (ready.notifications && ready.channelEnabled),
            "Affichage plein écran" to ready.fullScreen,
            "Caméra" to hasCameraPermission()
        ).joinToString("\n") { (label, ok) -> "$label : ${if (ok) "autorisé" else "à configurer"}" }, secondary = true)
        ScreenUi.button(permissions, "Vérifier les autorisations", true) { configurePermissions() }
        ScreenUi.text(permissions, "Redmi / HyperOS : vérifiez aussi l’affichage sur l’écran verrouillé, le démarrage en arrière-plan et les restrictions de batterie dans les réglages de l’application.", 13, secondary = true)
        ScreenUi.button(permissions, "Réglages de l’application", true) { launchSettings(AlarmReadiness.appSettingsIntent(this)) }
    }

    private fun realAlarm() = AlarmSession.isRinging(this) && !AlarmSession.isPreview(this)
    private fun openAlarm() = startActivity(Intent(this, AlarmActivity::class.java).putExtra(AlarmService.EXTRA_SESSION_TOKEN, AlarmSession.token(this)))
    private fun hasCameraPermission() = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    private fun setAlarm() {
        if (realAlarm()) { openAlarm(); return }
        if (!Prefs.hasKitchenQr(this) || !hasCameraPermission()) {
            message("Associez votre QR code et autorisez la caméra avant d’activer le réveil.")
            startActivity(Intent(this, QrSetupActivity::class.java)); return
        }
        if (!AlarmReadiness.snapshot(this).ready) { configurePermissions(); return }
        try {
            AlarmScheduler.schedule(this, hour, minute)
            Prefs.save(this, hour, minute, true)
            message("Réveil activé. Le QR code de la cuisine arrêtera la sonnerie.")
            render()
        } catch (_: SecurityException) { configurePermissions() }
    }
    private fun configurePermissions() {
        val state = AlarmReadiness.snapshot(this)
        when {
            !state.notifications && Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED -> notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
            !state.notifications -> launchSettings(AlarmReadiness.notificationSettingsIntent(this))
            !state.channelEnabled -> launchSettings(AlarmReadiness.channelSettingsIntent(this))
            !state.exactAlarms -> launchSettings(AlarmReadiness.exactAlarmSettingsIntent(this))
            !state.fullScreen -> launchSettings(AlarmReadiness.fullScreenSettingsIntent(this))
            !hasCameraPermission() -> startActivity(Intent(this, QrSetupActivity::class.java))
            else -> message("Les autorisations vérifiables sont prêtes. Essayez aussi le réveil avec l’écran verrouillé.")
        }
    }
    private fun launchSettings(intent: Intent) {
        try { startActivity(intent) } catch (_: RuntimeException) { message("Ouvrez les autorisations de Wake Up dans les paramètres Android.") }
    }
    private fun message(text: String) = Toast.makeText(this, text, Toast.LENGTH_LONG).show()
}
