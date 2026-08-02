package com.baicaohui.lightweb.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TabManagerTest {

    @Test
    fun `new tab is added and selected`() {
        val manager = TabManager()
        val tab = manager.newTab("https://a.com")
        assertEquals(listOf(tab), manager.tabs.value)
        assertEquals(tab.id, manager.currentId.value)
        assertEquals(tab, manager.current)
    }

    @Test
    fun `close current selects most recently accessed remaining`() {
        val manager = TabManager()
        val a = manager.newTab("https://a.com")
        manager.newTab("https://b.com")
        val c = manager.newTab("https://c.com")
        manager.select(a.id)
        manager.closeTab(a.id)
        assertEquals(c.id, manager.currentId.value)
        assertEquals(2, manager.tabs.value.size)
    }

    @Test
    fun `select updates current`() {
        val manager = TabManager()
        val a = manager.newTab()
        val b = manager.newTab()
        manager.select(a.id)
        assertEquals(a.id, manager.currentId.value)
    }

    @Test
    fun `over limit evicts oldest`() {
        val manager = TabManager(maxTabs = 2)
        manager.newTab("https://a.com")
        manager.newTab("https://b.com")
        val c = manager.newTab("https://c.com")
        assertEquals(listOf("https://b.com", "https://c.com"), manager.tabs.value.map { it.url })
        assertEquals(c.id, manager.currentId.value)
    }

    @Test
    fun `update modifies tab fields`() {
        val manager = TabManager()
        val tab = manager.newTab("https://a.com")
        manager.update(tab.id) { it.copy(title = "A", status = TabStatus.READY, progress = 100) }
        val updated = manager.tabs.value.first()
        assertEquals("A", updated.title)
        assertEquals(TabStatus.READY, updated.status)
        assertEquals(100, updated.progress)
    }

    @Test
    fun `close all leaves null current`() {
        val manager = TabManager()
        val a = manager.newTab()
        manager.closeTab(a.id)
        assertNull(manager.currentId.value)
        assertNull(manager.current)
    }
}
