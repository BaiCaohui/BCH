package com.baicaohui.lightweb.ui.browser

import org.junit.Assert.assertEquals
import org.junit.Test

class MenuLayoutTest {

    @Test
    fun `page count for full page is one`() {
        assertEquals(1, menuPageCount(8, 8))
    }

    @Test
    fun `page count splits overflow`() {
        assertEquals(2, menuPageCount(9, 8))
        assertEquals(3, menuPageCount(17, 8))
    }

    @Test
    fun `page count zero items is zero`() {
        assertEquals(0, menuPageCount(0, 8))
    }

    @Test
    fun `columns adapt to width within bounds`() {
        assertEquals(3, menuColumnsForWidth(200f, 72f, 3, 5))
        assertEquals(4, menuColumnsForWidth(320f, 72f, 3, 5))
        assertEquals(5, menuColumnsForWidth(500f, 72f, 3, 5))
    }

    @Test
    fun `page items slice correctly`() {
        val items = (1..9).map { it.toString() }
        assertEquals(listOf("1", "2"), menuPageItems(items, 0, 2))
        assertEquals(listOf("3", "4"), menuPageItems(items, 1, 2))
        assertEquals(emptyList<String>(), menuPageItems(items, 5, 2))
    }
}
