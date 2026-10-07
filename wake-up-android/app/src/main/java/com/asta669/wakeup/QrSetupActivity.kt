package com.asta669.wakeup

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.print.PrintHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.journeyapps.barcodescanner.DecoratedBarcodeView

class QrSetupActivity : AppCompatActivity() {
    private var camera: QrCamera? = null
    private var generated: String? = null
    private var dialogOpen = false
    private var scanRequested = false
    private lateinit var status: TextView
    private val permission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) beginScan() else status.text = "Caméra refusée. Autorisez-la dans les réglages Android pour utiliser le QR code au réveil."
    }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        generated = state?.getString("generated")
        render()
    }
    private fun blocked(): Boolean = AlarmSession.isRinging(this) && !AlarmSession.isPreview(this)
    private fun render() {
        camera?.pause()
        val column = ScreenUi.screen(this, "La cuisine vous attend.", "Un seul code. Un vrai mouvement pour commencer la journée.")
        if (blocked()) {
            ScreenUi.text(ScreenUi.card(column), "Le code associé ne peut pas être modifié pendant une alarme.")
            ScreenUi.button(column, "Revenir au réveil") { finish() }
            return
        }
        val card = ScreenUi.card(column)
        ScreenUi.text(card, "Votre point de réveil", 23, true)
        status = ScreenUi.text(card, if (Prefs.hasKitchenQr(this)) "Un code est déjà associé. Un nouveau code ne le remplacera qu’après votre confirmation."
            else "Scannez un QR code existant ou créez-en un à imprimer. Placez-le ensuite dans la cuisine.", secondary = true)
        ScreenUi.button(card, "Scanner un code existant") { requestScan() }
        val view = DecoratedBarcodeView(this)
        card.addView(view, LinearLayout.LayoutParams(-1, ScreenUi.dp(this, 240)))
        view.visibility = android.view.View.GONE
        camera = QrCamera(view, { payload ->
            val hash = QrVerifier.hash(payload)
            if (hash == null) status.text = "Ce code est vide ou trop volumineux. Choisissez un autre QR code."
            else if (!dialogOpen && !blocked()) {
                dialogOpen = true; camera?.pause()
                MaterialAlertDialogBuilder(this).setTitle("Associer ce QR code ?")
                    .setMessage("Seul ce code pourra arrêter le réveil. Placez-le dans votre cuisine. Son contenu ne sera pas ouvert ni envoyé sur Internet.")
                    .setNegativeButton("Annuler") { _, _ -> }
                    .setPositiveButton("Associer") { _, _ ->
                        if (!blocked()) { Prefs.setQrHash(this, hash); status.text = "Code associé. Votre cuisine devient votre point de réveil."; scanRequested = false }
                    }.setOnDismissListener { dialogOpen = false; if (scanRequested && !blocked()) camera?.resume() }.show()
            }
        }, { status.text = "La caméra est indisponible. Fermez les autres applications qui l’utilisent, puis réessayez."; scanRequested = false })
        view.tag = "qr-camera"
        val printCard = ScreenUi.card(column)
        ScreenUi.text(printCard, "Créer mon code", 23, true)
        ScreenUi.text(printCard, "Imprimez ou enregistrez le document en PDF, puis affichez-le dans la cuisine. Ne validez l’association qu’après avoir conservé le code.", secondary = true)
        ScreenUi.button(printCard, if (generated == null) "Générer un QR code" else "Générer un autre code", true) {
            if (!blocked()) { generated = QrVerifier.newKitchenToken(); render() }
        }
        generated?.let { token ->
            val bitmap = qrBitmap(token)
            printCard.addView(ImageView(this).apply {
                setImageBitmap(bitmap); contentDescription = "QR code personnel à imprimer"
                setBackgroundColor(Color.WHITE); setPadding(12, 12, 12, 12)
            }, LinearLayout.LayoutParams(-1, ScreenUi.dp(this, 260)))
            ScreenUi.button(printCard, "Imprimer / enregistrer en PDF", true) {
                PrintHelper(this).apply { scaleMode = PrintHelper.SCALE_MODE_FIT }.printBitmap("Wake Up — code cuisine", bitmap)
            }
            ScreenUi.button(printCard, "J’ai conservé ce code : l’associer") {
                if (blocked()) return@button
                MaterialAlertDialogBuilder(this).setTitle("Le code est prêt ?")
                    .setMessage("Conservez le document imprimé ou le PDF. Une fois associé, ce code sera nécessaire pour arrêter vos alarmes.")
                    .setNegativeButton("Pas encore", null)
                    .setPositiveButton("Associer") { _, _ ->
                        if (!blocked()) {
                            Prefs.setQrHash(this, QrVerifier.hash(token)!!)
                            status.text = "Code associé. Autorisez la caméra pour être prêt au réveil."
                            if (!hasPermission()) permission.launch(Manifest.permission.CAMERA)
                        }
                    }.show()
            }
        }
        ScreenUi.button(column, "Terminer", true) { finish() }
    }
    private fun hasPermission() = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    private fun requestScan() {
        if (blocked()) return
        scanRequested = true
        if (hasPermission()) beginScan() else permission.launch(Manifest.permission.CAMERA)
    }
    private fun beginScan() {
        if (blocked()) return
        if (!scanRequested) return
        findViewById<android.view.View>(android.R.id.content).findViewWithTag<DecoratedBarcodeView>("qr-camera")?.visibility = android.view.View.VISIBLE
        status.text = "Placez votre QR code dans le cadre. La lecture reste sur votre téléphone."
        camera?.resume()
    }
    override fun onResume() { super.onResume(); if (blocked()) { finish(); return }; if (scanRequested && hasPermission() && !dialogOpen) beginScan() }
    override fun onPause() { camera?.pause(); super.onPause() }
    override fun onSaveInstanceState(out: Bundle) { out.putString("generated", generated); super.onSaveInstanceState(out) }
    private fun qrBitmap(payload: String): Bitmap {
        val matrix = QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, 800, 800, mapOf(EncodeHintType.MARGIN to 4))
        return Bitmap.createBitmap(800, 800, Bitmap.Config.ARGB_8888).apply {
            val pixels = IntArray(800 * 800) { i -> if (matrix[i % 800, i / 800]) Color.BLACK else Color.WHITE }
            setPixels(pixels, 0, 800, 0, 0, 800, 800)
        }
    }
}
