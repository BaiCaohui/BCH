package com.baicaohui.lightweb.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.unit.sp

fun bchTypography(scale: Float): Typography {
    val base = Typography()
    fun scaled(sp: Int) = (sp * scale).sp
    return Typography(
        headlineMedium = base.headlineMedium.copy(fontSize = scaled(28)),
        titleLarge = base.titleLarge.copy(fontSize = scaled(22)),
        titleMedium = base.titleMedium.copy(fontSize = scaled(16)),
        bodyLarge = base.bodyLarge.copy(fontSize = scaled(16)),
        bodyMedium = base.bodyMedium.copy(fontSize = scaled(14)),
        labelLarge = base.labelLarge.copy(fontSize = scaled(14)),
    )
}
