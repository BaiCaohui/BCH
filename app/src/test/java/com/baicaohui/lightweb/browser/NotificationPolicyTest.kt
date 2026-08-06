package com.baicaohui.lightweb.browser

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationPolicyTest {

    @Test
    fun `suppression script overrides requestPermission and permission`() {
        val script = NotificationPolicy.suppressionScript()
        assertTrue(script.contains("Notification"))
        assertTrue(script.contains("requestPermission"))
        assertTrue(script.contains("denied"))
    }

    @Test
    fun `suppresses when not allowed`() {
        assertTrue(NotificationPolicy.shouldSuppress(PermissionDecision.BLOCK))
        assertTrue(NotificationPolicy.shouldSuppress(PermissionDecision.ASK))
        assertFalse(NotificationPolicy.shouldSuppress(PermissionDecision.ALLOW))
    }
}
