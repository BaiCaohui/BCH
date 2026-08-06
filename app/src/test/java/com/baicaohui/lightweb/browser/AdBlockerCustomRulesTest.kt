package com.baicaohui.lightweb.browser

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdBlockerCustomRulesTest {

    private val blocker = AdBlocker(basicHosts = emptySet(), strictHosts = emptySet())

    @Test
    fun `custom host rule blocks host and subdomains`() {
        val rules = listOf("ads.example.com")
        assertTrue(blocker.isBlocked("https://ads.example.com/banner", AdLevel.OFF, rules))
        assertTrue(blocker.isBlocked("https://cdn.ads.example.com/x", AdLevel.OFF, rules))
        assertFalse(blocker.isBlocked("https://example.com", AdLevel.OFF, rules))
    }

    @Test
    fun `pipes host rule matches subdomains`() {
        val rules = listOf("||track.example.net")
        assertTrue(blocker.isBlocked("https://a.b.track.example.net/x", AdLevel.OFF, rules))
        assertFalse(blocker.isBlocked("https://example.net", AdLevel.OFF, rules))
    }

    @Test
    fun `url substring rule matches anywhere in url`() {
        val rules = listOf("example.com/ads/")
        assertTrue(blocker.isBlocked("https://www.example.com/ads/banner.png", AdLevel.OFF, rules))
        assertFalse(blocker.isBlocked("https://www.example.com/ads2/banner.png", AdLevel.OFF, rules))
    }

    @Test
    fun `glob rule converts star to wildcard`() {
        val rules = listOf("*/banner/*")
        assertTrue(blocker.isBlocked("https://x.com/a/banner/wide.gif", AdLevel.OFF, rules))
        assertFalse(blocker.isBlocked("https://x.com/a/banner2/x", AdLevel.OFF, rules))
    }

    @Test
    fun `custom rules work together with built-in level`() {
        val withBuiltin = AdBlocker(
            basicHosts = setOf("doubleclick.net"),
            strictHosts = emptySet(),
        )
        assertTrue(
            withBuiltin.isBlocked(
                "https://ad.doubleclick.net/x",
                AdLevel.BASIC,
                listOf("custom.example"),
            ),
        )
        assertTrue(
            withBuiltin.isBlocked(
                "https://custom.example/x",
                AdLevel.BASIC,
                listOf("custom.example"),
            ),
        )
    }

    @Test
    fun `empty and comment rules are ignored`() {
        val rules = listOf("", "   ", "# comment", "||")
        assertFalse(blocker.isBlocked("https://example.com", AdLevel.OFF, rules))
    }
}
