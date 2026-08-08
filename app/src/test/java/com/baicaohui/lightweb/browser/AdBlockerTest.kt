package com.baicaohui.lightweb.browser

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdBlockerTest {

    private val blocker = AdBlocker(
        basicHosts = setOf("doubleclick.net", "googlesyndication.com"),
        strictHosts = setOf("criteo.com", "taboola.com"),
        adguardHosts = setOf("adservice.google.com", "adsrvr.org"),
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

    @Test
    fun `marked host is blocked even when level is off`() {
        assertTrue(
            blocker.isBlocked(
                "https://ad.example.net/x",
                AdLevel.OFF,
                markedHosts = setOf("example.net"),
            ),
        )
    }

    @Test
    fun `marked host matches subdomains`() {
        assertTrue(
            blocker.isBlocked(
                "https://cdn.ads.example.net/banner.js",
                AdLevel.OFF,
                markedHosts = setOf("ads.example.net"),
            ),
        )
    }

    @Test
    fun `unmarked host not blocked at off`() {
        assertFalse(
            blocker.isBlocked(
                "https://example.com",
                AdLevel.OFF,
                markedHosts = setOf("ads.example.net"),
            ),
        )
    }

    @Test
    fun `adguard host blocked at basic`() {
        assertTrue(blocker.isBlocked("https://adservice.google.com/x", AdLevel.BASIC))
        assertTrue(blocker.isBlocked("https://ads.adsrvr.org/x", AdLevel.BASIC))
    }

    @Test
    fun `adguard host not blocked at off`() {
        assertFalse(blocker.isBlocked("https://adservice.google.com/x", AdLevel.OFF))
    }

    @Test
    fun `adguard host matches subdomains`() {
        assertTrue(blocker.isBlocked("https://cdn.adsrvr.org/x", AdLevel.BASIC))
    }
}
