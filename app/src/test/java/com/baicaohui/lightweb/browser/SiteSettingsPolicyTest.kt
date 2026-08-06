package com.baicaohui.lightweb.browser

import com.baicaohui.lightweb.data.db.SiteSettingEntity
import com.baicaohui.lightweb.data.prefs.BrowserPrefs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SiteSettingsPolicyTest {

    private fun prefs() = BrowserPrefs(
        defaultJsEnabled = true,
        adLevel = "BASIC",
        safeBrowsing = true,
        thirdPartyCookies = false,
    )

    @Test
    fun `site null uses global defaults`() {
        val resolved = SiteSettingsPolicy.resolve(prefs(), null)
        assertTrue(resolved.javaScriptEnabled)
        assertTrue(resolved.safeBrowsing)
        assertFalse(resolved.thirdPartyCookies)
        assertEquals(AdLevel.BASIC, resolved.adLevel)
    }

    @Test
    fun `site overrides every field`() {
        val site = SiteSettingEntity(
            host = "x.com",
            jsEnabled = false,
            adLevel = "STRICT",
            safeBrowsing = false,
            thirdPartyCookies = true,
        )
        val resolved = SiteSettingsPolicy.resolve(prefs(), site)
        assertFalse(resolved.javaScriptEnabled)
        assertEquals(AdLevel.STRICT, resolved.adLevel)
        assertFalse(resolved.safeBrowsing)
        assertTrue(resolved.thirdPartyCookies)
    }

    @Test
    fun `invalid site ad level falls back to global`() {
        val site = SiteSettingEntity(host = "x.com", adLevel = "NOPE")
        assertEquals(AdLevel.BASIC, SiteSettingsPolicy.resolve(prefs(), site).adLevel)
    }

    @Test
    fun `null site ad level falls back to global`() {
        val site = SiteSettingEntity(host = "x.com", adLevel = null)
        assertEquals(AdLevel.BASIC, SiteSettingsPolicy.resolve(prefs(), site).adLevel)
    }

    @Test
    fun `invalid global ad level falls back to off`() {
        val resolved = SiteSettingsPolicy.resolve(prefs().copy(adLevel = "BAD"), null)
        assertEquals(AdLevel.OFF, resolved.adLevel)
    }

    @Test
    fun `anti tracking defaults to global and site can override`() {
        assertTrue(SiteSettingsPolicy.resolve(prefs(), null).antiTracking)
        assertFalse(
            SiteSettingsPolicy.resolve(
                prefs(),
                SiteSettingEntity(host = "x.com", antiTracking = false),
            ).antiTracking,
        )
    }

    @Test
    fun `https mode merges site override`() {
        assertEquals(
            HttpsMode.PREFER,
            SiteSettingsPolicy.resolve(prefs(), null).httpsMode,
        )
        assertEquals(
            HttpsMode.STRICT,
            SiteSettingsPolicy.resolve(
                prefs(),
                SiteSettingEntity(host = "x.com", httpsUpgrade = true),
            ).httpsMode,
        )
        assertEquals(
            HttpsMode.OFF,
            SiteSettingsPolicy.resolve(
                prefs(),
                SiteSettingEntity(host = "x.com", httpsUpgrade = false),
            ).httpsMode,
        )
    }

    @Test
    fun `autoplay merges site override`() {
        assertFalse(SiteSettingsPolicy.resolve(prefs(), null).autoplay)
        assertTrue(
            SiteSettingsPolicy.resolve(
                prefs(),
                SiteSettingEntity(host = "x.com", autoplay = true),
            ).autoplay,
        )
    }
}
