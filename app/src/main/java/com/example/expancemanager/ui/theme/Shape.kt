package com.example.expancemanager.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Softer, larger radii than the Material defaults — the rounded-card look that reads
 * as "financial app" rather than "stock Android form".
 */
internal val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

/** Named radii for surfaces that aren't driven by the Material shape scale. */
internal object AppRadius {
    val pill = RoundedCornerShape(999.dp)
    val chip = RoundedCornerShape(12.dp)
    val icon = RoundedCornerShape(14.dp)
    val card = RoundedCornerShape(20.dp)
    val hero = RoundedCornerShape(28.dp)
}
