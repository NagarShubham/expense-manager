package com.example.expancemanager.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,
    error = DarkError,
    onError = DarkOnError,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceContainerLowest = DarkSurfaceContainerLowest,
    surfaceContainerLow = DarkSurfaceContainerLow,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    surfaceContainerHighest = DarkSurfaceContainerHighest,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    inverseSurface = DarkInverseSurface,
    inverseOnSurface = DarkInverseOnSurface,
    inversePrimary = DarkInversePrimary
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    tertiary = LightTertiary,
    onTertiary = LightOnTertiary,
    tertiaryContainer = LightTertiaryContainer,
    onTertiaryContainer = LightOnTertiaryContainer,
    error = LightError,
    onError = LightOnError,
    errorContainer = LightErrorContainer,
    onErrorContainer = LightOnErrorContainer,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceContainerLowest = LightSurfaceContainerLowest,
    surfaceContainerLow = LightSurfaceContainerLow,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    surfaceContainerHighest = LightSurfaceContainerHighest,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    inverseSurface = LightInverseSurface,
    inverseOnSurface = LightInverseOnSurface,
    inversePrimary = LightInversePrimary
)

/**
 * Brand colors Material's [androidx.compose.material3.ColorScheme] has no slot for:
 * the hero gradient, the finance semantics (under/over budget), and the categorical
 * accent slots.
 */
@Immutable
internal data class AppColors(
    val heroGradient: List<Color>,
    val onHero: Color,
    val onHeroMuted: Color,
    val positive: Color,
    val positiveContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val categoryAccents: List<Color>
) {
    /**
     * Accent slot for a category. Keyed off the category *name* so a category keeps its
     * color everywhere it appears — filtering or re-sorting a list never repaints the
     * survivors, which ranking by spend would.
     *
     * Color is a secondary cue here: every place an accent is used also shows the
     * category emoji, name and amount, so identity never rests on hue alone (which is
     * also what licenses the accents whose light-mode contrast sits under 3:1).
     */
    fun accentFor(category: String): Color {
        if (categoryAccents.isEmpty()) return positive
        var hash = 0
        for (char in category) {
            hash = hash * 31 + char.code
        }
        return categoryAccents[Math.floorMod(hash, categoryAccents.size)]
    }
}

private val LightAppColors = AppColors(
    heroGradient = LightHeroGradient,
    onHero = Color.White,
    onHeroMuted = Color(0xCCFFFFFF),
    positive = LightPositive,
    positiveContainer = LightPositiveContainer,
    warning = LightWarning,
    warningContainer = LightWarningContainer,
    categoryAccents = LightCategoryAccents
)

private val DarkAppColors = AppColors(
    heroGradient = DarkHeroGradient,
    onHero = Color.White,
    onHeroMuted = Color(0xCCFFFFFF),
    positive = DarkPositive,
    positiveContainer = DarkPositiveContainer,
    warning = DarkWarning,
    warningContainer = DarkWarningContainer,
    categoryAccents = DarkCategoryAccents
)

internal val LocalAppColors = staticCompositionLocalOf { LightAppColors }

/** Extended brand colors, alongside `MaterialTheme.colorScheme`. */
internal val MaterialTheme.appColors: AppColors
    @Composable
    @ReadOnlyComposable
    get() = LocalAppColors.current

@Composable
internal fun ExpanseManagerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Off by default: the emerald brand identity is the point of the redesign, so the
    // wallpaper-derived palette would undo it. Kept as a parameter for previews.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val appColors = if (darkTheme) DarkAppColors else LightAppColors

    // The in-app theme toggle is independent of the system setting, so the system bar
    // icons have to be told which way to go rather than following the OS.
    val view = LocalView.current
    if (!LocalInspectionMode.current) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = AppShapes,
            content = content
        )
    }
}
