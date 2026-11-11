// SPDX-License-Identifier: Apache-2.0
package com.awbuilds.reproparcel.core

import com.awbuilds.reproparcel.core.privacy.Redactor
import com.awbuilds.reproparcel.core.privacy.SafeAttribute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RedactorTest {
    @Test
    fun keepsAllowlistedSafeValues() {
        assertEquals("checkout_review", Redactor.sanitize(SafeAttribute.SCREEN_ID, "checkout_review"))
        assertEquals("GET", Redactor.sanitize(SafeAttribute.HTTP_METHOD, "get"))
        assertEquals("/orders/{id}", Redactor.sanitize(SafeAttribute.ROUTE_TEMPLATE, "/orders/{id}"))
        assertEquals("200", Redactor.sanitize(SafeAttribute.STATUS_CODE, "200"))
        assertEquals("portrait", Redactor.sanitize(SafeAttribute.ORIENTATION, "portrait"))
    }

    @Test
    fun dropsSecretsAndUnsafeShapes() {
        assertNull(Redactor.sanitize(SafeAttribute.SCREEN_ID, "user@example.com"))
        assertNull(Redactor.sanitize(SafeAttribute.SCREEN_ID, "Bearer abcdefghijklmnop"))
        assertNull(Redactor.sanitize(SafeAttribute.ROUTE_TEMPLATE, "/orders?id=123"))
        assertNull(Redactor.sanitize(SafeAttribute.ROUTE_TEMPLATE, "https://example.com/a"))
        assertNull(Redactor.sanitize(SafeAttribute.SCREEN_ID, "account 123456789"))
        assertNull(Redactor.sanitize(SafeAttribute.SCREEN_ID, "line\nbreak"))
        assertNull(Redactor.sanitize(SafeAttribute.SCREEN_ID, "a".repeat(300)))
    }

    @Test
    fun dropsUnknownKeys() {
        val sanitized = Redactor.sanitizeAttributes(
            mapOf(
                "screen_id" to "home",
                "password" to "hunter2",
                "raw_url" to "https://example.com/?token=abc",
            ),
        )
        assertEquals(setOf("screen_id"), sanitized.keys)
        assertEquals("home", sanitized["screen_id"])
    }

    @Test
    fun diagnosticsUseSafeClassNames() {
        assertEquals("IllegalStateException", Redactor.safeDiagnosticClass("IllegalStateException"))
        assertEquals("Bad_Name", Redactor.safeDiagnosticClass("Bad_Name"))
        assertTrue(Redactor.safeDiagnosticClass("\n\t").isNotEmpty())
    }
}
