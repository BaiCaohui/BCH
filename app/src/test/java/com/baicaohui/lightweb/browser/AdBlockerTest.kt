package com.baicaohui.lightweb.browser

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdBlockerTest {

    private val blocker = AdBlocker(
        basicHosts = setOf("doubleclick.net", "googlesyndication.com"),
        strictHosts = setOf("criteo.com", "taboola.com"),
    )

    @Test
    fun `off never blocks`() {
        assertFalse(blocker.isBlocked("https://ad.doubleclick.net/x", AdLevel.OFF))
    }

    @Test
    fun `basic blocks basic host and subdomain`() {
        assertTrue(blocker.isBlocked("https://ad.doubleclick.net/x", AdLevel.BASIC))
        assertTrue(blocker.isBlocked("https://doubleclick.net", AdLevel.BASIC))
    }

    @Test
    fun `basic does not block strict host`() {
        assertFalse(blocker.isBlocked("https://www.criteo.com", AdLevel.BASIC))
    }

    @Test
    fun `strict blocks strict host`() {
        assertTrue(blocker.isBlocked("https://www.criteo.com", AdLevel.STRICT))
    }

    @Test
    fun `unrelated host not blocked`() {
        assertFalse(blocker.isBlocked("https://example.com", AdLevel.STRICT))
    }
}
