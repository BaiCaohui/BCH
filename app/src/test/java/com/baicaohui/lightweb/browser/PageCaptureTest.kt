package com.baicaohui.lightweb.browser

import org.junit.Assert.assertEquals
import org.junit.Test

class PageCaptureTest {

    @Test
    fun `decodes json encoded html from evaluateJavascript`() {
        val raw = "\"<!DOCTYPE html><html><body>你好</body></html>\""
        assertEquals(
            "<!DOCTYPE html><html><body>你好</body></html>",
            PageHtmlCapture.parseHtml(raw),
        )
    }

    @Test
    fun `returns raw text when not json encoded`() {
        assertEquals(
            "<html>plain</html>",
            PageHtmlCapture.parseHtml("<html>plain</html>"),
        )
    }

    @Test
    fun `returns blank for null or blank input`() {
        assertEquals("", PageHtmlCapture.parseHtml(null))
        assertEquals("", PageHtmlCapture.parseHtml(""))
        assertEquals("", PageHtmlCapture.parseHtml("\"\""))
    }

    @Test
    fun `script wraps outer html in try catch`() {
        val script = PageHtmlCapture.outerHtmlScript()
        assertEquals(true, script.contains("document.documentElement.outerHTML"))
        assertEquals(true, script.contains("try"))
        assertEquals(true, script.contains("catch"))
    }
}
