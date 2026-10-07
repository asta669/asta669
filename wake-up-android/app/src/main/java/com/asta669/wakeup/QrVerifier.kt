package com.asta669.wakeup

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/** QR data is an opaque local identifier: never a URL, command, or network request. */
object QrVerifier {
    const val MAX_PAYLOAD_BYTES = 1024

    /** Deliberately do not trim or normalise valid content: the enrolled code must match exactly. */
    fun hash(payload: String?): String? {
        if (payload == null || payload.isBlank() || payload.length > MAX_PAYLOAD_BYTES) return null
        val bytes = payload.toByteArray(StandardCharsets.UTF_8)
        if (bytes.size > MAX_PAYLOAD_BYTES) return null
        return MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }

    fun matches(payload: String?, enrolledHash: String): Boolean {
        if (!enrolledHash.matches(Regex("[0-9a-f]{64}"))) return false
        val candidate = hash(payload) ?: return false
        return MessageDigest.isEqual(
            candidate.toByteArray(StandardCharsets.US_ASCII),
            enrolledHash.toByteArray(StandardCharsets.US_ASCII)
        )
    }

    fun newKitchenToken(): String {
        val bytes = ByteArray(24).also { SecureRandom().nextBytes(it) }
        return "WAKEUP-KITCHEN:" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
}
