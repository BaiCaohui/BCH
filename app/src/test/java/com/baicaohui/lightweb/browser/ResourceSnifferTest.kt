package com.baicaohui.lightweb.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ResourceSnifferTest {

    @Test
    fun `kind detects video extensions with query and case`() {
        assertEquals(ResourceKind.VIDEO, ResourceSniffer.kindOf("https://a.com/v.MP4?token=1"))
        assertEquals(ResourceKind.VIDEO, ResourceSniffer.kindOf("https://a.com/live.m3u8"))
        assertEquals(ResourceKind.VIDEO, ResourceSniffer.kindOf("https://a.com/a.flv"))
        assertEquals(ResourceKind.VIDEO, ResourceSniffer.kindOf("https://a.com/a.webm"))
    }

    @Test
    fun `kind detects audio extensions`() {
        assertEquals(ResourceKind.AUDIO, ResourceSniffer.kindOf("https://a.com/a.mp3"))
        assertEquals(ResourceKind.AUDIO, ResourceSniffer.kindOf("https://a.com/a.m4a"))
        assertEquals(ResourceKind.AUDIO, ResourceSniffer.kindOf("https://a.com/a.ogg"))
        assertEquals(ResourceKind.AUDIO, ResourceSniffer.kindOf("https://a.com/a.wav"))
    }

    @Test
    fun `kind detects image extensions`() {
        assertEquals(ResourceKind.IMAGE, ResourceSniffer.kindOf("https://a.com/a.jpg"))
        assertEquals(ResourceKind.IMAGE, ResourceSniffer.kindOf("https://a.com/a.jpeg"))
        assertEquals(ResourceKind.IMAGE, ResourceSniffer.kindOf("https://a.com/a.png"))
        assertEquals(ResourceKind.IMAGE, ResourceSniffer.kindOf("https://a.com/a.gif"))
        assertEquals(ResourceKind.IMAGE, ResourceSniffer.kindOf("https://a.com/a.svg"))
        assertEquals(ResourceKind.IMAGE, ResourceSniffer.kindOf("https://a.com/a.webp"))
    }

    @Test
    fun `kind ignores unsupported extensions`() {
        assertNull(ResourceSniffer.kindOf("https://a.com/page.html"))
        assertNull(ResourceSniffer.kindOf("https://a.com/api"))
        assertNull(ResourceSniffer.kindOf("https://a.com/file.pdf"))
    }

    @Test
    fun `name keeps extension and decodes`() {
        assertEquals("photo one.jpg", ResourceSniffer.nameFor("https://a.com/photo%20one.jpg"))
        assertEquals("video.mp4", ResourceSniffer.nameFor("https://a.com/video.mp4?x=1"))
        assertEquals("download", ResourceSniffer.nameFor("https://a.com/"))
    }

    @Test
    fun `resolve handles relative and protocol-relative urls`() {
        assertEquals(
            "https://a.com/v.mp4",
            ResourceSniffer.resolveUrl("https://a.com/page", "v.mp4"),
        )
        assertEquals(
            "https://cdn.com/v.mp4",
            ResourceSniffer.resolveUrl("https://a.com/page", "//cdn.com/v.mp4"),
        )
        assertEquals(
            "https://a.com/v.mp4",
            ResourceSniffer.resolveUrl("https://a.com/dir/page.html", "../v.mp4"),
        )
        assertEquals(
            "https://a.com/v.mp4",
            ResourceSniffer.resolveUrl("https://a.com/page", "https://a.com/v.mp4"),
        )
    }

    @Test
    fun `resolve rejects blank and unsupported schemes`() {
        assertNull(ResourceSniffer.resolveUrl("https://a.com", ""))
        assertNull(ResourceSniffer.resolveUrl("https://a.com", "javascript:alert(1)"))
        assertNull(ResourceSniffer.resolveUrl("https://a.com", "data:image/png;base64,xx"))
    }

    @Test
    fun `mime maps per kind and extension`() {
        assertEquals("video/mp4", ResourceSniffer.mimeFor(ResourceKind.VIDEO, "https://a.com/v.mp4"))
        assertEquals("video/x-flv", ResourceSniffer.mimeFor(ResourceKind.VIDEO, "https://a.com/v.flv"))
        assertEquals(
            "application/vnd.apple.mpegurl",
            ResourceSniffer.mimeFor(ResourceKind.VIDEO, "https://a.com/live.m3u8"),
        )
        assertEquals("audio/mpeg", ResourceSniffer.mimeFor(ResourceKind.AUDIO, "https://a.com/a.mp3"))
        assertEquals("image/svg+xml", ResourceSniffer.mimeFor(ResourceKind.IMAGE, "https://a.com/a.svg"))
    }
}
