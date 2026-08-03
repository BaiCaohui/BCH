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

    @Test
    fun `snapshots and restore roundtrip`() {
        val manager = TabManager()
        val a = manager.newTab("https://a.com")
        manager.update(a.id) { it.copy(title = "A", status = TabStatus.READY, progress = 100) }
        val b = manager.newTab("https://b.com")

        val restored = TabManager()
        restored.restore(manager.snapshots())

        assertEquals(manager.tabs.value.map { it.url }, restored.tabs.value.map { it.url })
        assertEquals(manager.tabs.value.map { it.title }, restored.tabs.value.map { it.title })
        assertEquals(b.id, restored.currentId.value)

        val c = restored.newTab("https://c.com")
        assertEquals(listOf(a.id, b.id, c.id), restored.tabs.value.map { it.id })
    }

    @Test
    fun `restore empty list is no-op`() {
        val manager = TabManager()
        manager.restore(emptyList())
        assertEquals(0, manager.tabs.value.size)
        assertNull(manager.currentId.value)
    }

}
