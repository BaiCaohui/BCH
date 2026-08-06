package com.baicaohui.lightweb.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HttpsPolicyTest {

    @Test
    fun `off never upgrades`() {
        assertFalse(HttpsPolicy.shouldUpgrade("http://example.com", HttpsMode.OFF))
    }

    @Test
    fun `prefer upgrades http`() {
        assertTrue(HttpsPolicy.shouldUpgrade("http://example.com/a", HttpsMode.PREFER))
        assertEquals(
            "https://example.com/a",
            HttpsPolicy.upgrade("http://example.com/a"),
        )
    }

    @Test
    fun `strict upgrades http too`() {
        assertTrue(HttpsPolicy.shouldUpgrade("http://example.com", HttpsMode.STRICT))
    }

    @Test
    fun `https is never upgraded`() {
        assertFalse(HttpsPolicy.shouldUpgrade("https://example.com", HttpsMode.PREFER))
    }

    @Test
    fun `prefer falls back from https failure`() {
        assertTrue(HttpsPolicy.shouldFallback("https://example.com", HttpsMode.PREFER))
        assertFalse(HttpsPolicy.shouldFallback("https://example.com", HttpsMode.STRICT))
        assertFalse(HttpsPolicy.shouldFallback("http://example.com", HttpsMode.PREFER))
        assertEquals(
            "http://example.com/a",
            HttpsPolicy.toHttp("https://example.com/a"),
        )
    }

    @Test
    fun `insecure detection`() {
        assertTrue(HttpsPolicy.isInsecure("http://example.com"))
        assertFalse(HttpsPolicy.isInsecure("https://example.com"))
        assertFalse(HttpsPolicy.isInsecure("about:blank"))
    }
}
