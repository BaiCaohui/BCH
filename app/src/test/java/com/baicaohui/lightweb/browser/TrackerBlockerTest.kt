package com.baicaohui.lightweb.browser

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackerBlockerTest {

    private val blocker = TrackerBlocker(
        hosts = setOf("google-analytics.com", "facebook.net", "mixpanel.com"),
    )

    @Test
    fun `blocks tracker host and subdomains`() {
        assertTrue(blocker.isTracker("https://www.google-analytics.com/ga.js"))
        assertTrue(blocker.isTracker("https://google-analytics.com"))
        assertTrue(blocker.isTracker("https://connect.facebook.net/en_US/fbevents.js"))
    }

    @Test
    fun `does not block normal host`() {
        assertFalse(blocker.isTracker("https://example.com"))
        assertFalse(blocker.isTracker("https://analytics.example.com"))
    }

    @Test
    fun `ignores non http urls`() {
        assertFalse(blocker.isTracker("about:blank"))
        assertFalse(blocker.isTracker(""))
    }
}
