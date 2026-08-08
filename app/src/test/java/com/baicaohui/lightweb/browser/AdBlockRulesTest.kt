package com.baicaohui.lightweb.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AdBlockRulesTest {

    private fun readRaw(name: String): List<String> {
        val file = listOf(
            File("src/main/res/raw/$name.txt"),
            File("app/src/main/res/raw/$name.txt"),
        ).firstOrNull { it.exists() } ?: error("raw rule file not found: $name")
        return file.readLines()
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
    }

    @Test
    fun `basic rules contain major ad networks`() {
        val rules = readRaw("adblock_basic").toSet()
        listOf(
            "doubleclick.net",
            "googlesyndication.com",
            "googleadservices.com",
            "amazon-adsystem.com",
            "adnxs.com",
            "appnexus.com",
            "pubmatic.com",
            "smartadserver.com",
            "adform.net",
            "criteo.com",
            "taboola.com",
            "outbrain.com",
            "moatads.com",
            "scorecardresearch.com",
            "openx.net",
            "casalemedia.com",
            "media.net",
            "adcolony.com",
            "inmobi.com",
            "mopub.com",
        ).forEach { domain ->
            assertTrue("basic rules should contain $domain", domain in rules)
        }
    }

    @Test
    fun `strict rules add exchanges and ad tech domains`() {
        val rules = readRaw("adblock_strict").toSet()
        listOf(
            "adsrvr.org",
            "adition.com",
            "bluekai.com",
            "demdex.net",
            "dataxu.com",
            "exelator.com",
            "freewheel.tv",
            "lotame.com",
            "rhythmone.com",
            "smartclip.net",
            "tubemogul.com",
            "weborama.fr",
            "yieldlove.com",
            "cpmstar.com",
            "millennialmedia.com",
            "mediavine.com",
        ).forEach { domain ->
            assertTrue("strict rules should contain $domain", domain in rules)
        }
    }

    @Test
    fun `basic rules are reasonably comprehensive`() {
        assertTrue("basic should have at least 80 rules", readRaw("adblock_basic").size >= 80)
    }

    @Test
    fun `strict rules are reasonably comprehensive`() {
        assertTrue("strict should have at least 50 rules", readRaw("adblock_strict").size >= 50)
    }

    @Test
    fun `rule files contain no duplicates`() {
        listOf("adblock_basic", "adblock_strict").forEach { name ->
            val rules = readRaw(name)
            assertEquals("$name should not contain duplicates", rules.size, rules.toSet().size)
        }
    }
}
