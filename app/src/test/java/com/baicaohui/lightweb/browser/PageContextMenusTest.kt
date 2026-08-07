package com.baicaohui.lightweb.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PageContextMenusTest {

    @Test
    fun `parse selection info from evaluateJavascript payload`() {
        val raw = """"{\"text\":\"hello\",\"left\":10,\"top\":20,\"width\":30,\"height\":40}""""
        val info = PageContextMenus.parseSelectionInfo(raw)
        assertNotNull(info)
        assertEquals("hello", info!!.text)
        assertEquals(10f, info.left)
        assertEquals(20f, info.top)
        assertEquals(30f, info.width)
        assertEquals(40f, info.height)
    }

    @Test
    fun `parse selection info returns null for null payload`() {
        assertNull(PageContextMenus.parseSelectionInfo("null"))
        assertNull(PageContextMenus.parseSelectionInfo(null))
    }

    @Test
    fun `parse text decodes quoted string`() {
        assertEquals("hi", PageContextMenus.parseText(""""hi""""))
    }

    @Test
    fun `parse text returns empty for null`() {
        assertEquals("", PageContextMenus.parseText("null"))
        assertEquals("", PageContextMenus.parseText(null))
    }
}
