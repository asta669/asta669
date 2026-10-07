package com.asta669.wakeup

import org.junit.Assert.*
import org.junit.Test

class BriefSafetyTest {
    private val key = "example_key_for_tests_only_12345"

    @Test fun acceptsPlainModelIdentifiers() {
        assertTrue(BriefSafety.validCredentials(key, "gemini-2.0-flash"))
    }

    @Test fun rejectsUrlAndQueryInjectionInModel() {
        listOf("../other", "model?key=other", "model/operation", "https://other.invalid", "").forEach {
            assertFalse(it, BriefSafety.validCredentials(key, it))
        }
    }

    @Test fun rejectsHeaderInjectionOrAbsentKey() {
        assertFalse(BriefSafety.validCredentials("$key\r\nX-Other: value", "gemini-2.0-flash"))
        assertFalse(BriefSafety.validCredentials("", "gemini-2.0-flash"))
    }
}
