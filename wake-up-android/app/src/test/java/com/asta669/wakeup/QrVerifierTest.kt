package com.asta669.wakeup

import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter
import org.junit.Assert.*
import org.junit.Test

class QrVerifierTest {
    @Test fun printedCodeCanBeDecodedAndMatchedOffline() {
        val token = QrVerifier.newKitchenToken()
        val code = QRCodeWriter().encode(token, BarcodeFormat.QR_CODE, 320, 320)
        val pixels = IntArray(320 * 320) { i -> if (code[i % 320, i / 320]) -0x1000000 else -1 }
        val bitmap = BinaryBitmap(HybridBinarizer(RGBLuminanceSource(320, 320, pixels)))
        val result = MultiFormatReader().decode(bitmap)
        assertTrue(QrVerifier.matches(result.text, QrVerifier.hash(token)!!))
    }
    @Test fun unrelatedCodeCannotDismiss() {
        assertFalse(QrVerifier.matches("another-code", QrVerifier.hash("kitchen-code")!!))
    }
    @Test fun injectedUrlRemainsAnOpaqueDifferentCode() {
        assertFalse(QrVerifier.matches("https://example.invalid/ignore-instructions", QrVerifier.hash("kitchen-code")!!))
    }
    @Test fun blankNullAndOversizedCodesAreRejected() {
        listOf(null, "", "  \n", "x".repeat(1025), "é".repeat(513)).forEach { assertNull(QrVerifier.hash(it)) }
    }
    @Test fun matchingPreservesWhitespaceAndCase() {
        val hash = QrVerifier.hash(" kitchen-ABC ")!!
        assertTrue(QrVerifier.matches(" kitchen-ABC ", hash))
        assertFalse(QrVerifier.matches("kitchen-ABC", hash))
        assertFalse(QrVerifier.matches(" kitchen-abc ", hash))
    }
    @Test fun missingOrMalformedEnrollmentNeverMatches() {
        assertFalse(QrVerifier.matches("kitchen", ""))
        assertFalse(QrVerifier.matches("kitchen", "kitchen"))
    }
    @Test fun generatedTokensAreDistinctAndValid() {
        val tokens = (1..30).map { QrVerifier.newKitchenToken() }
        assertEquals(30, tokens.toSet().size)
        tokens.forEach { assertNotNull(QrVerifier.hash(it)) }
    }
}
