package com.baicaohui.lightweb.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlSecurityTest {

    @Test
    fun `normalize adds https to bare domain`() {
        assertEquals("https://example.com", UrlSecurity.normalize("example.com"))
    }

    @Test
    fun `normalize keeps full url`() {
        assertEquals(
            "https://example.com/a?b=1",
            UrlSecurity.normalize("https://example.com/a?b=1"),
        )
    }

    @Test
    fun `normalize keeps search phrase`() {
        assertEquals("hello world", UrlSecurity.normalize("hello world"))
    }

    @Test
    fun `normalize blank returns empty`() {
        assertEquals("", UrlSecurity.normalize("  "))
    }

    @Test
    fun `safe schemes allowed`() {
        assertTrue(UrlSecurity.isSafeUrl("https://a.com"))
        assertTrue(UrlSecurity.isSafeUrl("http://a.com"))
        assertTrue(UrlSecurity.isSafeUrl("about:blank"))
    }

    @Test
    fun `dangerous schemes blocked`() {
        assertFalse(UrlSecurity.isSafeUrl("intent://scan/#Intent;scheme=zxing;end"))
        assertFalse(UrlSecurity.isSafeUrl("javascript:alert(1)"))
        assertFalse(UrlSecurity.isSafeUrl("file:///etc/passwd"))
        assertFalse(UrlSecurity.isSafeUrl("content://settings"))
    }

    @Test
    fun `search url uses template and encodes query`() {
        val result = UrlSecurity.toSearchUrl("你好 world", "https://www.bing.com/search?q=%s")
        assertTrue(result.startsWith("https://www.bing.com/search?q="))
        assertTrue(result.contains("%E4%BD%A0%E5%A5%BD"))
    }

    @Test
    fun `extractHost returns host or null`() {
        assertEquals("m.example.com", UrlSecurity.extractHost("https://m.example.com/p?q=1"))
        assertNull(UrlSecurity.extractHost("not a url"))
    }
}
