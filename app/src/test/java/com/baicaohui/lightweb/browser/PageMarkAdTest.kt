package com.baicaohui.lightweb.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PageMarkAdTest {

    @Test
    fun `parses selection with selector and urls`() {
        val selection = PageMarkAd.parseSelection(
            """{"selector":"#ad-banner","urls":["https://ad.doubleclick.net/x","https://img.example.com/a.png"]}""",
        )
        assertEquals("#ad-banner", selection.selector)
        assertEquals(
            listOf("https://ad.doubleclick.net/x", "https://img.example.com/a.png"),
            selection.urls,
        )
    }

    @Test
    fun `parses json encoded raw from evaluateJavascript`() {
        val raw = "\"{\\\"selector\\\":\\\"h1\\\",\\\"urls\\\":[\\\"https://ad.example.net/x\\\"]}\""
        val selection = PageMarkAd.parseSelection(raw)
        assertEquals("h1", selection.selector)
        assertEquals(listOf("https://ad.example.net/x"), selection.urls)
    }

    @Test
    fun `parses full identified selection with html and bounds`() {
        val selection = PageMarkAd.parseSelection(
            """
            {"found":true,"selector":"#ad-banner","html":"<div id=\"ad-banner\"><a href=\"https://ad.example.net/x\">x</a></div>",
             "urls":["https://ad.example.net/x"],"left":10.5,"top":20.25,"width":300,"height":120}
            """.trimIndent(),
        )
        assertEquals(true, selection.found)
        assertEquals("#ad-banner", selection.selector)
        assertEquals(
            "<div id=\"ad-banner\"><a href=\"https://ad.example.net/x\">x</a></div>",
            selection.html,
        )
        assertEquals(listOf("https://ad.example.net/x"), selection.urls)
        assertEquals(10.5, selection.left, 0.001)
        assertEquals(20.25, selection.top, 0.001)
        assertEquals(300.0, selection.width, 0.001)
        assertEquals(120.0, selection.height, 0.001)
    }

    @Test
    fun `identify script finds ad element and returns bounds html urls`() {
        val script = PageMarkAd.identifyScript(cx = 120.0, cy = 300.0)
        assertTrue(script.contains("elementFromPoint"))
        assertTrue(script.contains("outerHTML"))
        assertTrue(script.contains("querySelectorAll"))
        assertTrue(script.contains("getBoundingClientRect"))
        assertTrue(script.contains("iframe"))
        assertTrue(script.contains("120"))
        assertTrue(script.contains("300"))
    }

    @Test
    fun `returns empty selection for blank or malformed input`() {
        assertEquals(MarkedAdSelection(), PageMarkAd.parseSelection(null))
        assertEquals(MarkedAdSelection(), PageMarkAd.parseSelection(""))
        assertEquals(MarkedAdSelection(), PageMarkAd.parseSelection("not json"))
    }

    @Test
    fun `selection script samples box and collects element urls`() {
        val script = PageMarkAd.selectionScript(cx = 100.0, cy = 200.0, w = 300.0, h = 120.0)
        assertTrue(script.contains("elementFromPoint"))
        assertTrue(script.contains("getBoundingClientRect"))
        assertTrue(script.contains("querySelectorAll"))
        assertTrue(script.contains("CSS.escape"))
        assertTrue(script.contains("100"))
        assertTrue(script.contains("200"))
        assertTrue(script.contains("300"))
        assertTrue(script.contains("120"))
    }

    @Test
    fun `hide script embeds selectors and display none`() {
        val script = PageMarkAd.hideSelectorScript(listOf("#ad-banner", ".sponsor > div"))
        assertTrue(script.contains("\"#ad-banner\""))
        assertTrue(script.contains("\".sponsor > div\""))
        assertTrue(script.contains("display:none!important"))
        assertTrue(script.contains("bch-marked-ad-hide"))
    }
}
