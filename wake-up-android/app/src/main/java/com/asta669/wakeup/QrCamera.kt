package com.asta669.wakeup

import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.CameraPreview
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.journeyapps.barcodescanner.DefaultDecoderFactory

/** A QR-only camera reader. Decoding is entirely on the device. */
class QrCamera(
    private val view: DecoratedBarcodeView,
    private val onRead: (String?) -> Unit,
    private val onError: () -> Unit
) {
    private var accepting = false

    init {
        view.barcodeView.decoderFactory = DefaultDecoderFactory(listOf(BarcodeFormat.QR_CODE))
        view.setStatusText("")
        view.barcodeView.addStateListener(object : CameraPreview.StateListener {
            override fun previewSized() = Unit
            override fun previewStarted() = Unit
            override fun previewStopped() = Unit
            override fun cameraClosed() = Unit
            override fun cameraError(error: Exception) {
                accepting = false
                view.pause()
                onError()
            }
        })
        view.decodeContinuous(object : BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult) {
                if (accepting && result.barcodeFormat == BarcodeFormat.QR_CODE) onRead(result.text)
            }
        })
    }

    fun resume() {
        accepting = true
        view.resume()
    }

    fun pause() {
        accepting = false
        view.pause()
    }

    fun setTorch(enabled: Boolean) {
        if (enabled) view.setTorchOn() else view.setTorchOff()
    }
}
