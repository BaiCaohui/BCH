package com.baicaohui.lightweb.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CookieDataManagerTest {

    @Test
    fun `parses cookie names from header`() {
        assertEquals(
            listOf("sid", "theme", "pref"),
            CookieDataManager.cookieNames("sid=abc; theme=dark; pref=\"x;y\""),
        )
    }

    @Test
    fun `empty header yields no names`() {
        assertTrue(CookieDataManager.cookieNames(null).isEmpty())
        assertTrue(CookieDataManager.cookieNames("").isEmpty())
        assertTrue(CookieDataManager.cookieNames("   ").isEmpty())
    }

    @Test
    fun `duplicate names from merged http and https headers are deduped`() {
        assertEquals(
            listOf("sid", "theme"),
            CookieDataManager.cookieNames("sid=1; theme=dark; sid=2"),
        )
    }

    @Test
    fun `expired entries cover http and https with path`() {
        val entries = CookieDataManager.expiredSetCookieEntries(
            "example.com",
            "sid=abc; theme=dark",
        )
        assertEquals(4, entries.size)
        assertTrue(entries.contains("https://example.com/" to expiredFor("sid")))
        assertTrue(entries.contains("http://example.com/" to expiredFor("sid")))
        assertTrue(entries.contains("https://example.com/" to expiredFor("theme")))
    }

    @Test
    fun `normalizes host input`() {
        assertEquals("example.com", CookieDataManager.normalizeHost(" Example.COM "))
        assertEquals("example.com", CookieDataManager.normalizeHost("https://Example.com/"))
    }

    private fun expiredFor(name: String): String =
        "$name=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/"
}
