package com.baicaohui.lightweb.browser

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ResourceSniffControllerTest {

    @Test
    fun `add ignores when inactive`() = runTest {
        val controller = ResourceSniffController()
        controller.add("https://a.com/v.mp4", "https://a.com/")
        assertEquals(emptyList<SniffedResource>(), controller.resources.value)
    }

    @Test
    fun `start then add collects and dedupes`() = runTest {
        val controller = ResourceSniffController()
        controller.start()
        controller.add("https://a.com/v.mp4", "https://a.com/")
        controller.add("https://a.com/v.mp4", "https://a.com/")
        assertEquals(1, controller.resources.value.size)
        assertEquals(ResourceKind.VIDEO, controller.resources.value[0].kind)
        assertEquals("v.mp4", controller.resources.value[0].name)
    }

    @Test
    fun `add resolves relative urls`() = runTest {
        val controller = ResourceSniffController()
        controller.start()
        controller.add("img/photo.jpg", "https://a.com/page/index.html")
        assertEquals("https://a.com/page/img/photo.jpg", controller.resources.value[0].url)
    }

    @Test
    fun `add ignores unsupported`() = runTest {
        val controller = ResourceSniffController()
        controller.start()
        controller.add("https://a.com/page.html", "https://a.com/")
        assertEquals(emptyList<SniffedResource>(), controller.resources.value)
    }

    @Test
    fun `stop stops collecting`() = runTest {
        val controller = ResourceSniffController()
        controller.start()
        controller.stop()
        controller.add("https://a.com/a.mp3", "https://a.com/")
        assertEquals(emptyList<SniffedResource>(), controller.resources.value)
    }

    @Test
    fun `clear resets list`() = runTest {
        val controller = ResourceSniffController()
        controller.start()
        controller.add("https://a.com/a.png", "https://a.com/")
        controller.clear()
        assertEquals(emptyList<SniffedResource>(), controller.resources.value)
    }

    @Test
    fun `onPageStarted clears while active`() = runTest {
        val controller = ResourceSniffController()
        controller.start()
        controller.add("https://a.com/a.png", "https://a.com/")
        controller.onPageStarted()
        assertEquals(emptyList<SniffedResource>(), controller.resources.value)
        controller.add("https://a.com/b.jpg", "https://a.com/")
        assertEquals(1, controller.resources.value.size)
    }

    @Test
    fun `addDomResult parses evaluateJavascript payload`() = runTest {
        val controller = ResourceSniffController()
        controller.start()
        val raw = """"[{\"u\":\"https://a.com/v.mp4\",\"k\":\"video\"}]""""
        controller.addDomResult(raw, "https://a.com/")
        assertEquals(1, controller.resources.value.size)
        assertEquals("https://a.com/v.mp4", controller.resources.value[0].url)
    }

    @Test
    fun `addDomResult ignores null payload`() = runTest {
        val controller = ResourceSniffController()
        controller.start()
        controller.addDomResult("null", "https://a.com/")
        assertEquals(emptyList<SniffedResource>(), controller.resources.value)
    }

    @Test
    fun `updateSize replaces size for matching url`() = runTest {
        val controller = ResourceSniffController()
        controller.start()
        controller.add("https://a.com/a.png", "https://a.com/")
        controller.updateSize("https://a.com/a.png", 1234L)
        assertEquals(1234L, controller.resources.value[0].sizeBytes)
        controller.updateSize("https://a.com/missing.png", 99L)
        assertEquals(1234L, controller.resources.value[0].sizeBytes)
    }
}
