package com.example.expancemanager.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Brand palette — a deep emerald "money" hue with a cool teal support, tuned so the
 * same identity reads in light and dark. Values are hand-picked tonal steps rather
 * than a generated Material palette so the hero surfaces keep their saturation.
 */

// ---- Light scheme ----
internal val LightPrimary = Color(0xFF00695A)
internal val LightOnPrimary = Color(0xFFFFFFFF)
internal val LightPrimaryContainer = Color(0xFFA9F2DC)
internal val LightOnPrimaryContainer = Color(0xFF00201A)
internal val LightSecondary = Color(0xFF4A635B)
internal val LightOnSecondary = Color(0xFFFFFFFF)
internal val LightSecondaryContainer = Color(0xFFCDE9DE)
internal val LightOnSecondaryContainer = Color(0xFF072019)
internal val LightTertiary = Color(0xFF3D6373)
internal val LightOnTertiary = Color(0xFFFFFFFF)
internal val LightTertiaryContainer = Color(0xFFC1E9FB)
internal val LightOnTertiaryContainer = Color(0xFF001F29)
internal val LightError = Color(0xFFB3261E)
internal val LightOnError = Color(0xFFFFFFFF)
internal val LightErrorContainer = Color(0xFFFFDAD6)
internal val LightOnErrorContainer = Color(0xFF410002)
internal val LightBackground = Color(0xFFF6F9F7)
internal val LightOnBackground = Color(0xFF121A18)
internal val LightSurface = Color(0xFFF6F9F7)
internal val LightOnSurface = Color(0xFF121A18)
internal val LightSurfaceVariant = Color(0xFFDBE5E0)
internal val LightOnSurfaceVariant = Color(0xFF3F4945)
internal val LightSurfaceContainerLowest = Color(0xFFFFFFFF)
internal val LightSurfaceContainerLow = Color(0xFFF0F4F1)
internal val LightSurfaceContainer = Color(0xFFEAEFEC)
internal val LightSurfaceContainerHigh = Color(0xFFE4EAE6)
internal val LightSurfaceContainerHighest = Color(0xFFDEE4E1)
internal val LightOutline = Color(0xFF6F7975)
internal val LightOutlineVariant = Color(0xFFBFC9C4)
internal val LightInverseSurface = Color(0xFF272E2C)
internal val LightInverseOnSurface = Color(0xFFEDF2EF)
internal val LightInversePrimary = Color(0xFF6FDBBF)

// ---- Dark scheme ----
internal val DarkPrimary = Color(0xFF6FDBBF)
internal val DarkOnPrimary = Color(0xFF00382E)
internal val DarkPrimaryContainer = Color(0xFF005143)
internal val DarkOnPrimaryContainer = Color(0xFF8BF8D8)
internal val DarkSecondary = Color(0xFFB1CCC2)
internal val DarkOnSecondary = Color(0xFF1C352E)
internal val DarkSecondaryContainer = Color(0xFF334B44)
internal val DarkOnSecondaryContainer = Color(0xFFCDE9DE)
internal val DarkTertiary = Color(0xFFA5CCDF)
internal val DarkOnTertiary = Color(0xFF073544)
internal val DarkTertiaryContainer = Color(0xFF244C5B)
internal val DarkOnTertiaryContainer = Color(0xFFC1E9FB)
internal val DarkError = Color(0xFFFFB4AB)
internal val DarkOnError = Color(0xFF690005)
internal val DarkErrorContainer = Color(0xFF93000A)
internal val DarkOnErrorContainer = Color(0xFFFFDAD6)
internal val DarkBackground = Color(0xFF0E1513)
internal val DarkOnBackground = Color(0xFFDEE4E1)
internal val DarkSurface = Color(0xFF0E1513)
internal val DarkOnSurface = Color(0xFFDEE4E1)
internal val DarkSurfaceVariant = Color(0xFF3F4945)
internal val DarkOnSurfaceVariant = Color(0xFFBFC9C4)
internal val DarkSurfaceContainerLowest = Color(0xFF090F0D)
internal val DarkSurfaceContainerLow = Color(0xFF161D1B)
internal val DarkSurfaceContainer = Color(0xFF1A2220)
internal val DarkSurfaceContainerHigh = Color(0xFF252D2A)
internal val DarkSurfaceContainerHighest = Color(0xFF303835)
internal val DarkOutline = Color(0xFF899390)
internal val DarkOutlineVariant = Color(0xFF3F4945)
internal val DarkInverseSurface = Color(0xFFDEE4E1)
internal val DarkInverseOnSurface = Color(0xFF272E2C)
internal val DarkInversePrimary = Color(0xFF00695A)

/**
 * Hero gradient stops. Painted top-start to bottom-end on the balance card so the
 * brand hue stays saturated instead of being flattened into a single container tone.
 */
internal val LightHeroGradient = listOf(
    Color(0xFF07584A),
    Color(0xFF0B7A63),
    Color(0xFF12A183)
)
internal val DarkHeroGradient = listOf(
    Color(0xFF04352C),
    Color(0xFF076151),
    Color(0xFF0B8E74)
)

// Semantic finance colors. Kept apart from the categorical slots below: state must
// never be confused with identity.
internal val LightPositive = Color(0xFF0F7A55)
internal val LightPositiveContainer = Color(0xFFD3F2E4)
internal val LightWarning = Color(0xFF9A5B00)
internal val LightWarningContainer = Color(0xFFFFE2BC)

internal val DarkPositive = Color(0xFF5FD3A6)
internal val DarkPositiveContainer = Color(0xFF10402F)
internal val DarkWarning = Color(0xFFF0B357)
internal val DarkWarningContainer = Color(0xFF4A2F00)

/**
 * Categorical accent slots used for category identity (avatar tint, share bars).
 *
 * These are the eight hues of the validated reference categorical theme, in the
 * order that clears every adjacent-pair gate: lightness band, chroma floor, CVD
 * separation and normal-vision separation, checked against this app's own light
 * (#F6F9F7) and dark (#0E1513) surfaces. Assigned in fixed order by category index
 * — never cycled through a generated hue — and always paired with a visible name +
 * amount label, which is also what satisfies the sub-3:1 contrast relief rule for
 * the aqua/yellow/magenta slots in light mode.
 *
 * Do not reorder or re-step without re-running the palette validator.
 */
internal val LightCategoryAccents = listOf(
    Color(0xFF2A78D6), // blue
    Color(0xFFEB6834), // orange
    Color(0xFF1BAF7A), // aqua
    Color(0xFFEDA100), // yellow
    Color(0xFFE87BA4), // magenta
    Color(0xFF008300), // green
    Color(0xFF4A3AA7), // violet
    Color(0xFFE34948)  // red
)

internal val DarkCategoryAccents = listOf(
    Color(0xFF3987E5), // blue
    Color(0xFFD95926), // orange
    Color(0xFF199E70), // aqua
    Color(0xFFC98500), // yellow
    Color(0xFFD55181), // magenta
    Color(0xFF008300), // green
    Color(0xFF9085E9), // violet
    Color(0xFFE66767)  // red
)
