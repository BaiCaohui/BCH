package com.baicaohui.lightweb.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class TonalPaletteGeneratorTest {

    private val seed = Color(0xFF2B7FFF)

    @Test
    fun `light scheme primary equals seed`() {
        assertEquals(seed, TonalPaletteGenerator.lightScheme(seed).primary)
    }

    @Test
    fun `light and dark background differ`() {
        assertNotEquals(
            TonalPaletteGenerator.lightScheme(seed).background,
            TonalPaletteGenerator.darkScheme(seed).background,
        )
    }

    @Test
    fun `primary and container are distinct in light scheme`() {
        assertNotEquals(
            TonalPaletteGenerator.lightScheme(seed).primary,
            TonalPaletteGenerator.lightScheme(seed).primaryContainer,
        )
    }
}
