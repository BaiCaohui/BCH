package com.baicaohui.lightweb.browser

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderPageTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun jsResult(payload: String): String =
        json.encodeToString(JsonPrimitive.serializer(), JsonPrimitive(payload))

    @Test
    fun `enter script embeds readability source and initial theme`() {
        val script = ReaderPage.enterScript("var marker = 42;", "dark")
        assertTrue(script.contains("var marker = 42;"))
        assertTrue(script.contains("\"dark\""))
        assertTrue(script.contains("new Readability("))
    }

    @Test
    fun `enter script keeps dollar template literals intact`() {
        val source = "var s = `\${x}`;"
        val script = ReaderPage.enterScript(source, "light")
        assertTrue(script.contains("var s = `\${x}`;"))
    }

    @Test
    fun `parse result extracts article`() {
        val payload = """{"ok":true,"title":"标题","byline":"作者","content":"<p>正文</p>"}"""
        val article = ReaderPage.parseResult(jsResult(payload))
        assertEquals("标题", article?.title)
        assertEquals("作者", article?.byline)
        assertEquals("<p>正文</p>", article?.contentHtml)
    }

    @Test
    fun `parse result null when not ok`() {
        val payload = """{"ok":false,"reason":"no-content"}"""
        assertNull(ReaderPage.parseResult(jsResult(payload)))
    }

    @Test
    fun `parse result null when content blank`() {
        val payload = """{"ok":true,"title":"t","byline":"","content":""}"""
        assertNull(ReaderPage.parseResult(jsResult(payload)))
    }

    @Test
    fun `parse result null on garbage`() {
        assertNull(ReaderPage.parseResult("not json"))
        assertNull(ReaderPage.parseResult(null))
    }

    @Test
    fun `parse exit true when ok`() {
        assertTrue(ReaderPage.parseExit(jsResult("""{"ok":true}""")))
        assertFalse(ReaderPage.parseExit(jsResult("""{"ok":false}""")))
        assertFalse(ReaderPage.parseExit(null))
    }

    @Test
    fun `exit script restores body and scroll`() {
        val script = ReaderPage.exitScript()
        assertTrue(script.contains("__bchReaderState"))
        assertTrue(script.contains("scrollTo"))
        assertTrue(script.contains("bodyChildren"))
    }

    @Test
    fun `offline html escapes text but keeps content html`() {
        val html = ReaderPage.offlineHtml(
            title = "<b>标题</b> & \"引号\"",
            byline = "作者 <script>",
            contentHtml = "<p>正文 <img src='x.png'></p>",
            theme = "sepia",
            offlineBadge = "离线缓存",
        )
        assertTrue(html.contains("&lt;b&gt;标题&lt;/b&gt; &amp; &quot;引号&quot;"))
        assertTrue(html.contains("作者 &lt;script&gt;"))
        assertTrue(html.contains("<p>正文 <img src='x.png'></p>"))
        assertTrue(html.contains("data-theme=\"sepia\""))
        assertTrue(html.contains("离线缓存"))
        assertTrue(html.contains("data-bch-font=\"-1\""))
    }

    @Test
    fun `offline html sanitizes unknown theme`() {
        val html = ReaderPage.offlineHtml("t", "", "<p>x</p>", "neon", "b")
        assertTrue(html.contains("data-theme=\"light\""))
    }

    @Test
    fun `html escape handles all special chars`() {
        assertEquals("&amp;&lt;&gt;&quot;&#39;", ReaderPage.htmlEscape("&<>\"'"))
    }
}
