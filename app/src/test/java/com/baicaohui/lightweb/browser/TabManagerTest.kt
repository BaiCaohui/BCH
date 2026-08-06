package com.baicaohui.lightweb.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

    @Test
    fun `enterIncognito starts with a new empty tab and switches mode`() {
        val manager = TabManager()
        val tab = manager.enterIncognito()
        assertEquals(true, manager.incognito.value)
        assertEquals(listOf(tab), manager.tabs.value)
        assertEquals("", tab.url)
    }

    @Test
    fun `enterIncognito preserves normal tabs and exit restores them`() {
        val manager = TabManager()
        val normal = manager.newTab("https://a.com")
        manager.enterIncognito()
        manager.newTab("https://b.com")
        manager.exitIncognito()
        assertEquals(false, manager.incognito.value)
        assertEquals(listOf(normal), manager.tabs.value)
        assertEquals(normal.id, manager.currentId.value)
    }

    @Test
    fun `incognito tabs are isolated from normal tabs`() {
        val manager = TabManager()
        val normal = manager.newTab("https://a.com")
        val inc = manager.enterIncognito()
        val inc2 = manager.newTab("https://b.com")
        assertEquals(listOf(inc, inc2), manager.tabs.value)
        manager.exitIncognito()
        assertEquals(listOf(normal), manager.tabs.value)
        val inc3 = manager.enterIncognito()
        assertEquals(listOf(inc3), manager.tabs.value)
        assertNotEquals(inc.id, inc3.id)
    }

    @Test
    fun `exitIncognito discards incognito tabs`() {
        val manager = TabManager()
        val normal = manager.newTab()
        manager.enterIncognito()
        manager.newTab("https://x.com")
        manager.exitIncognito()
        assertEquals(setOf(normal.id), manager.allTabIds())
    }

    @Test
    fun `enterIncognito twice does not duplicate stacks`() {
        val manager = TabManager()
        val normal = manager.newTab("https://a.com")
        manager.enterIncognito()
        manager.enterIncognito()
        assertEquals(1, manager.tabs.value.size)
        manager.exitIncognito()
        assertEquals(listOf(normal), manager.tabs.value)
    }

    @Test
    fun `new tab ids are unique across incognito stacks`() {
        val manager = TabManager()
        val normal = manager.newTab()
        val inc = manager.enterIncognito()
        manager.exitIncognito()
        val normal2 = manager.newTab()
        assertNotEquals(normal.id, inc.id)
        assertNotEquals(normal.id, normal2.id)
    }

    @Test
    fun `allTabIds includes both stacks`() {
        val manager = TabManager()
        val normal = manager.newTab()
        val inc = manager.enterIncognito()
        assertEquals(setOf(normal.id, inc.id), manager.allTabIds())
    }

    @Test
    fun `restore always lands in normal mode`() {
        val manager = TabManager()
        manager.enterIncognito()
        val inc = manager.newTab("https://x.com")
        val restored = TabManager()
        restored.restore(listOf(TabSnapshot(id = inc.id, url = inc.url)))
        assertEquals(false, restored.incognito.value)
        assertEquals(listOf(inc.id), restored.tabs.value.map { it.id })
        assertTrue(!restored.incognito.value)
    }

    @Test
    fun `initialIncognito starts with incognito flag and empty tabs`() {
        val manager = TabManager(initialIncognito = true)
        assertEquals(true, manager.incognito.value)
        assertEquals(0, manager.tabs.value.size)
        assertNull(manager.currentId.value)
        val tab = manager.newTab("https://x.com")
        assertEquals(true, manager.incognito.value)
        assertEquals(listOf(tab), manager.tabs.value)
    }

    @Test
    fun `restore on initialIncognito manager lands in normal mode`() {
        val manager = TabManager(initialIncognito = true)
        manager.newTab("https://x.com")
        manager.restore(listOf(TabSnapshot(id = 1, url = "https://y.com")))
        assertEquals(false, manager.incognito.value)
        assertEquals(listOf("https://y.com"), manager.tabs.value.map { it.url })
    }

    @Test
    fun `restored snapshot resets reader flags`() {
        val manager = TabManager()
        val tab = manager.newTab("https://a.com")
        manager.update(tab.id) { it.copy(readerMode = true, readerOffline = true) }
        val restored = TabManager()
        restored.restore(manager.snapshots())
        val restoredTab = restored.tabs.value.single()
        assertEquals(false, restoredTab.readerMode)
        assertEquals(false, restoredTab.readerOffline)
    }

}
