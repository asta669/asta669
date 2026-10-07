package com.asta669.wakeup

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.journeyapps.barcodescanner.DecoratedBarcodeView

/** Alarm screen is independent of the dashboard task and authenticates the kitchen QR locally. */
class AlarmActivity : AppCompatActivity() {
    private var camera: QrCamera? = null
    private var token: String? = null
    private var completed = false
    private lateinit var status: TextView
    private val handler = Handler(Looper.getMainLooper())
    private val permission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) camera?.resume() else status.text = "Caméra non autorisée. Autorisez-la dans les réglages ou utilisez l’arrêt de secours ci-dessous."
    }
    private val sessionWatch = object : Runnable {
        override fun run() {
            if (completed || isFinishing) return
            if (!AlarmSession.isRinging(this@AlarmActivity)) { finish(); return }
            if (token != AlarmSession.token(this@AlarmActivity)) render()
            handler.postDelayed(this, 500)
        }
    }
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        if (Build.VERSION.SDK_INT >= 27) { setShowWhenLocked(true); setTurnScreenOn(true) }
        @Suppress("DEPRECATION")
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        volumeControlStream = android.media.AudioManager.STREAM_ALARM
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { handleBack() }
        })
        render()
    }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); completed = false; render() }
    private fun render() {
        camera?.pause()
        token = AlarmSession.token(this)
        if (token == null) { finish(); return }
        val preview = AlarmSession.isPreview(this)
        val column = ScreenUi.screen(this, if (preview) "Essayez votre réveil." else "Debout, sir.",
            if (preview) "Test sonore · votre alarme programmée reste intacte."
            else "La première victoire du jour se trouve dans la cuisine.")
        val card = ScreenUi.card(column)
        ScreenUi.text(card, if (preview) "Volume maximal" else "Scannez. Respirez. Commencez.", 25, true)
        status = ScreenUi.text(card, if (Prefs.hasKitchenQr(this)) "Visez le QR code associé à votre cuisine pour arrêter la sonnerie."
            else "Aucun QR code n’est associé. Utilisez l’arrêt de secours, puis associez votre code depuis l’accueil.", secondary = true)
        if (Prefs.hasKitchenQr(this)) {
            val view = DecoratedBarcodeView(this)
            card.addView(view, LinearLayout.LayoutParams(-1, ScreenUi.dp(this, 260)))
            var lastWrongAt = 0L
            camera = QrCamera(view, { payload ->
                if (!completed && token == AlarmSession.token(this)) {
                    if (QrVerifier.matches(payload, Prefs.getQrHash(this))) dismissVerified()
                    else if (System.currentTimeMillis() - lastWrongAt > 1500) {
                        status.text = "Ce n’est pas le code associé. Rendez-vous au QR code de la cuisine."
                        lastWrongAt = System.currentTimeMillis()
                    }
                }
            }, { status.text = "Caméra indisponible. Réessayez ou maintenez le bouton d’arrêt de secours." })
            ScreenUi.button(card, "Activer / réessayer la caméra", true) { startCamera() }
            var torch = false
            ScreenUi.button(card, "Éclairer le code", true) { torch = !torch; camera?.setTorch(torch) }
        }
        if (preview) {
            ScreenUi.button(column, "Arrêter le test") {
                token?.let { AlarmService.stopPreview(this, it) }; completed = true; finish()
            }
            ScreenUi.text(column, "Le test s’arrête automatiquement après une minute.", 13, secondary = true)
        } else {
            val emergency = ScreenUi.button(column, "Secours · maintenir pour arrêter", true) {
                status.text = "En cas de problème, maintenez le bouton Secours puis confirmez l’arrêt."
            }
            emergency.setOnLongClickListener {
                MaterialAlertDialogBuilder(this).setTitle("Arrêt de secours")
                    .setMessage("Utilisez ce secours si le code ou la caméra est inutilisable. Cette alarme sera arrêtée ; le réveil quotidien restera programmé.")
                    .setNegativeButton("Continuer le réveil", null)
                    .setPositiveButton("Arrêter en secours") { _, _ ->
                        token?.let { AlarmService.stopForEmergency(this, it) }; completed = true; finish()
                    }.show()
                true
            }
        }
    }
    private fun startCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) camera?.resume()
        else permission.launch(Manifest.permission.CAMERA)
    }
    private fun dismissVerified() {
        val expected = token ?: return
        if (expected != AlarmSession.token(this)) return
        val preview = AlarmSession.isPreview(this)
        completed = true; camera?.pause()
        if (preview) AlarmService.stopPreview(this, expected) else AlarmService.stopAfterQr(this, expected)
        if (!preview && Prefs.isJarvisOn(this)) startActivity(Intent(this, JarvisActivity::class.java))
        finish()
    }
    override fun onResume() {
        super.onResume()
        if (AlarmSession.isRinging(this)) {
            if (token != AlarmSession.token(this)) render()
            if (Prefs.hasKitchenQr(this) && ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) camera?.resume()
            handler.removeCallbacks(sessionWatch); handler.post(sessionWatch)
        }
    }
    override fun onPause() { handler.removeCallbacks(sessionWatch); camera?.pause(); super.onPause() }
    override fun onDestroy() { handler.removeCallbacksAndMessages(null); camera?.pause(); super.onDestroy() }
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (AlarmSession.isRinging(this) && event.keyCode in listOf(KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_MUTE)) {
            AlarmService.enforceMaxVolume(this); return true
        }
        return super.dispatchKeyEvent(event)
    }
    private fun handleBack() {
        if (AlarmSession.isPreview(this)) { token?.let { AlarmService.stopPreview(this, it) }; finish() }
        else if (::status.isInitialized) status.text = "Scannez votre code de cuisine, ou utilisez le secours si le code est inutilisable."
    }
}
