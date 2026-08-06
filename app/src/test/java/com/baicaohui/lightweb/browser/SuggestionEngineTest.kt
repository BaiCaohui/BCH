package com.baicaohui.lightweb.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SuggestionEngineTest {

    @Test
    fun `endpointFor maps bing template`() {
        assertEquals(
            "https://api.bing.com/osjson.aspx?query=%s",
            SuggestionEngine.endpointFor("https://www.bing.com/search?q=%s"),
        )
    }

    @Test
    fun `endpointFor maps baidu template`() {
        assertEquals(
            "https://suggestion.baidu.com/su?wd=%s&json=1",
            SuggestionEngine.endpointFor("https://www.baidu.com/s?wd=%s"),
        )
    }

    @Test
    fun `endpointFor maps google template`() {
        assertEquals(
            "https://suggestqueries.google.com/complete/search?client=firefox&q=%s",
            SuggestionEngine.endpointFor("https://www.google.com/search?q=%s"),
        )
    }

    @Test
    fun `endpointFor returns null for custom engine`() {
        assertNull(SuggestionEngine.endpointFor("https://example.com/search?q=%s"))
    }

    @Test
    fun `parseBing extracts suggestions`() {
        assertEquals(
            listOf("hello world", "hello kitty"),
            SuggestionEngine.parseBing("""["hello",["hello world","hello kitty"],[],[]]"""),
        )
    }

    @Test
    fun `parseGoogle extracts suggestions`() {
        assertEquals(
            listOf("a", "b"),
            SuggestionEngine.parseGoogle("""["q",["a","b"]]"""),
        )
    }

    @Test
    fun `parseBaidu extracts suggestions from jsonp`() {
        assertEquals(
            listOf("测试1", "测试2"),
            SuggestionEngine.parseBaidu("""window.baidu.sug({"q":"测试","s":["测试1","测试2"]})"""),
        )
    }

    @Test
    fun `parseBaidu extracts suggestions from plain json`() {
        assertEquals(
            listOf("a", "b"),
            SuggestionEngine.parseBaidu("""{"q":"x","s":["a","b"]}"""),
        )
    }

    @Test
    fun `malformed body yields empty`() {
        assertEquals(emptyList<String>(), SuggestionEngine.parseBing("not json"))
        assertEquals(emptyList<String>(), SuggestionEngine.parseBaidu("broken"))
    }
}
