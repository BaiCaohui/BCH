package com.baicaohui.lightweb.browser

import com.baicaohui.lightweb.data.db.SiteSettingEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SitePermissionPolicyTest {

    @Test
    fun `sensitive kinds ask by default`() {
        assertEquals(
            PermissionDecision.ASK,
            SitePermissionPolicy.resolve(PermissionKind.LOCATION, null, true, false),
        )
        assertEquals(
            PermissionDecision.ASK,
            SitePermissionPolicy.resolve(PermissionKind.CAMERA, null, true, false),
        )
        assertEquals(
            PermissionDecision.ASK,
            SitePermissionPolicy.resolve(PermissionKind.MICROPHONE, null, true, false),
        )
        assertEquals(
            PermissionDecision.ASK,
            SitePermissionPolicy.resolve(PermissionKind.POPUPS, null, true, false),
        )
    }

    @Test
    fun `notifications and autoplay blocked by default`() {
        assertEquals(
            PermissionDecision.BLOCK,
            SitePermissionPolicy.resolve(PermissionKind.NOTIFICATIONS, null, true, false),
        )
        assertEquals(
            PermissionDecision.BLOCK,
            SitePermissionPolicy.resolve(PermissionKind.AUTOPLAY, null, true, false),
        )
    }

    @Test
    fun `autoplay allowed by global switch`() {
        assertEquals(
            PermissionDecision.ALLOW,
            SitePermissionPolicy.resolve(PermissionKind.AUTOPLAY, null, true, true),
        )
    }

    @Test
    fun `site overrides global decision`() {
        val site = SiteSettingEntity(
            host = "x.com",
            camera = false,
            notifications = true,
            popups = true,
        )
        assertEquals(
            PermissionDecision.BLOCK,
            SitePermissionPolicy.resolve(PermissionKind.CAMERA, site, true, false),
        )
        assertEquals(
            PermissionDecision.ALLOW,
            SitePermissionPolicy.resolve(PermissionKind.NOTIFICATIONS, site, true, false),
        )
        assertEquals(
            PermissionDecision.ALLOW,
            SitePermissionPolicy.resolve(PermissionKind.POPUPS, site, true, false),
        )
    }

    @Test
    fun `master switch forces block`() {
        val site = SiteSettingEntity(host = "x.com", camera = true)
        assertEquals(
            PermissionDecision.BLOCK,
            SitePermissionPolicy.resolve(PermissionKind.CAMERA, site, false, false),
        )
    }

    @Test
    fun `maps webview resources to kinds`() {
        val kinds = SitePermissionPolicy.kindsForResources(
            arrayOf(
                android.webkit.PermissionRequest.RESOURCE_VIDEO_CAPTURE,
                android.webkit.PermissionRequest.RESOURCE_AUDIO_CAPTURE,
            ),
        )
        assertTrue(PermissionKind.CAMERA in kinds)
        assertTrue(PermissionKind.MICROPHONE in kinds)
        assertFalse(PermissionKind.LOCATION in kinds)
    }
}
