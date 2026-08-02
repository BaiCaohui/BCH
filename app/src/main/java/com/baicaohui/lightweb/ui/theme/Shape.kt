package com.baicaohui.lightweb.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

fun bchShapes(style: ShapeStyle): Shapes {
    val medium = if (style == ShapeStyle.ROUNDED) 20.dp else 12.dp
    val large = if (style == ShapeStyle.ROUNDED) 28.dp else 16.dp
    val extraLarge = if (style == ShapeStyle.ROUNDED) 32.dp else 24.dp
    return Shapes(
        extraSmall = RoundedCornerShape(4.dp),
        small = RoundedCornerShape(8.dp),
        medium = RoundedCornerShape(medium),
        large = RoundedCornerShape(large),
        extraLarge = RoundedCornerShape(extraLarge),
    )
}
