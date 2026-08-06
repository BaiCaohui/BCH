package com.baicaohui.lightweb.ui.home

import com.baicaohui.lightweb.data.db.HistoryEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HistorySuggestionsTest {

    private fun entry(title: String, url: String) =
        HistoryEntity(id = 0, title = title, url = url)

    @Test
    fun `matches by title case insensitive`() {
        assertTrue(HistorySuggestions.matches("bch", "BCH 首页", "https://x.com"))
    }

    @Test
    fun `matches by url`() {
        assertTrue(HistorySuggestions.matches("example", "标题", "https://example.com/a"))
    }

    @Test
    fun `blank query matches everything`() {
        assertTrue(HistorySuggestions.matches("  ", "t", "https://x.com"))
    }

    @Test
    fun `non matching query rejects`() {
        assertFalse(HistorySuggestions.matches("zzz", "标题", "https://x.com"))
    }

    @Test
    fun `suggest filters and limits`() {
        val list = listOf(
            entry("A", "https://a.com"),
            entry("B", "https://b.com"),
            entry("AB", "https://ab.com"),
        )
        assertEquals(
            listOf("A", "AB"),
            HistorySuggestions.suggest(list, "a", limit = 8).map { it.title },
        )
        assertEquals(1, HistorySuggestions.suggest(list, "a", limit = 1).size)
    }
}
