package com.baicaohui.lightweb.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AdGuardHostsRulesTest {

    private fun readRules(): List<String> {
        val file = listOf(
            File("src/main/res/raw/adguard_hosts.txt"),
            File("app/src/main/res/raw/adguard_hosts.txt"),
        ).firstOrNull { it.exists() } ?: error("adguard_hosts.txt not found")
        return file.readLines()
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
    }

    @Test
    fun `adguard list is comprehensive`() {
        assertTrue("adguard hosts should have at least 100000 entries", readRules().size >= 100000)
    }

    @Test
    fun `adguard list contains major ad networks`() {
        val rules = readRules().toSet()
        listOf(
            "doubleclick.net",
            "adnxs.com",
            "pubmatic.com",
            "criteo.com",
            "taboola.com",
            "outbrain.com",
            "adsrvr.org",
        ).forEach { domain ->
            assertTrue("adguard hosts should contain $domain", domain in rules)
        }
    }

    @Test
    fun `adguard list has no duplicates and only hostnames`() {
        val rules = readRules()
        assertEquals("adguard hosts should not contain duplicates", rules.size, rules.toSet().size)
        rules.forEach { rule ->
            assertTrue("rule should be a plain hostname: $rule", rule.matches(Regex("[a-z0-9.-]+")))
        }
    }
}
