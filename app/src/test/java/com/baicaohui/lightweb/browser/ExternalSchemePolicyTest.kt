package com.baicaohui.lightweb.browser

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExternalSchemePolicyTest {

    @Test
    fun `safe http url never prompts`() {
        assertFalse(
            ExternalSchemePolicy.shouldPrompt(
                url = "https://www.bilibili.com/",
                hasGesture = true,
                canOpenExternally = true,
            ),
        )
        assertFalse(
            ExternalSchemePolicy.shouldPrompt(
                url = "http://example.com/a",
                hasGesture = false,
                canOpenExternally = false,
            ),
        )
    }

    @Test
    fun `about blank never prompts`() {
        assertFalse(
            ExternalSchemePolicy.shouldPrompt(
                url = "about:blank",
                hasGesture = true,
                canOpenExternally = true,
            ),
        )
    }

    @Test
    fun `auto deep link without gesture never prompts`() {
        assertFalse(
            ExternalSchemePolicy.shouldPrompt(
                url = "bilibili://video/123",
                hasGesture = false,
                canOpenExternally = true,
            ),
        )
    }

    @Test
    fun `deep link with no handler never prompts`() {
        assertFalse(
            ExternalSchemePolicy.shouldPrompt(
                url = "bilibili://video/123",
                hasGesture = true,
                canOpenExternally = false,
            ),
        )
    }

    @Test
    fun `explicit deep link with handler prompts`() {
        assertTrue(
            ExternalSchemePolicy.shouldPrompt(
                url = "bilibili://video/123",
                hasGesture = true,
                canOpenExternally = true,
            ),
        )
    }

    @Test
    fun `mailto with gesture and handler prompts`() {
        assertTrue(
            ExternalSchemePolicy.shouldPrompt(
                url = "mailto:test@example.com",
                hasGesture = true,
                canOpenExternally = true,
            ),
        )
    }
}
