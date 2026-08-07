package com.baicaohui.lightweb.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultBrowserTest {

    @Test
    fun `action requests role on Android 11 and above`() {
        assertEquals(DefaultBrowserAction.REQUEST_ROLE, DefaultBrowser.actionFor(30))
        assertEquals(DefaultBrowserAction.REQUEST_ROLE, DefaultBrowser.actionFor(37))
    }

    @Test
    fun `action opens default apps settings below Android 11`() {
        assertEquals(DefaultBrowserAction.OPEN_SETTINGS, DefaultBrowser.actionFor(26))
        assertEquals(DefaultBrowserAction.OPEN_SETTINGS, DefaultBrowser.actionFor(29))
    }

    @Test
    fun `role holder decides status on Android 11 and above`() {
        assertTrue(
            DefaultBrowser.isDefault(
                apiLevel = 30,
                ownPackage = "com.baicaohui.lightweb",
                isRoleHeld = { true },
                resolvedWebPackage = { null },
            ),
        )
        assertFalse(
            DefaultBrowser.isDefault(
                apiLevel = 37,
                ownPackage = "com.baicaohui.lightweb",
                isRoleHeld = { false },
                resolvedWebPackage = { "com.baicaohui.lightweb" },
            ),
        )
    }

    @Test
    fun `resolved package decides status below Android 11`() {
        assertTrue(
            DefaultBrowser.isDefault(
                apiLevel = 29,
                ownPackage = "com.baicaohui.lightweb",
                isRoleHeld = { false },
                resolvedWebPackage = { "com.baicaohui.lightweb" },
            ),
        )
        assertFalse(
            DefaultBrowser.isDefault(
                apiLevel = 29,
                ownPackage = "com.baicaohui.lightweb",
                isRoleHeld = { false },
                resolvedWebPackage = { "com.android.chrome" },
            ),
        )
        assertFalse(
            DefaultBrowser.isDefault(
                apiLevel = 26,
                ownPackage = "com.baicaohui.lightweb",
                isRoleHeld = { false },
                resolvedWebPackage = { null },
            ),
        )
    }
}
