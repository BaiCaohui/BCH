package com.baicaohui.lightweb.ui.browser

import org.junit.Assert.assertEquals
import org.junit.Test

class MenuOrderTest {

    @Test
    fun `empty stored order resolves to default order`() {
        assertEquals(MenuOrder.DEFAULT_ORDER, MenuOrder.resolve(emptyList()))
    }

    @Test
    fun `default order includes reader after reload`() {
        assertEquals("reader", MenuOrder.DEFAULT_ORDER[1])
    }

    @Test
    fun `custom order is preserved`() {
        assertEquals(
            listOf("settings", "reload", "history"),
            MenuOrder.resolve(listOf("settings", "reload", "history")),
        )
    }

    @Test
    fun `unknown ids are removed`() {
        assertEquals(
            listOf("settings", "reload"),
            MenuOrder.resolve(listOf("unknown", "settings", "reload", "bad")),
        )
    }

    @Test
    fun `duplicate ids are deduped`() {
        assertEquals(
            listOf("reload", "settings"),
            MenuOrder.resolve(listOf("reload", "settings", "reload")),
        )
    }

    @Test
    fun `hiding every item falls back to default`() {
        assertEquals(MenuOrder.DEFAULT_ORDER, MenuOrder.resolve(listOf("not_a_real_item")))
    }

    @Test
    fun `move reorders list`() {
        assertEquals(
            listOf("b", "a", "c"),
            MenuOrder.move(listOf("a", "b", "c"), from = 0, to = 1),
        )
        assertEquals(
            listOf("a", "c", "b"),
            MenuOrder.move(listOf("a", "b", "c"), from = 2, to = 1),
        )
    }

    @Test
    fun `move with invalid index is no-op`() {
        val list = listOf("a", "b")
        assertEquals(list, MenuOrder.move(list, from = 0, to = 5))
        assertEquals(list, MenuOrder.move(list, from = -1, to = 1))
    }
}
