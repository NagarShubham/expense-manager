package com.example.expancemanager.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import com.example.expancemanager.ui.theme.AppRadius
import com.example.expancemanager.ui.theme.ExpanseManagerTheme
import com.example.expancemanager.ui.theme.OverlineLabel
import com.example.expancemanager.ui.theme.TabularFigures
import com.example.expancemanager.ui.theme.appColors

internal object AppSpacing {
    val hairline = 2.dp
    val tiny = 4.dp
    val small = 8.dp
    val medium = 12.dp
    val default = 16.dp
    val large = 20.dp
    val xlarge = 24.dp
    val xxlarge = 32.dp
    val screen = 20.dp
}

/**
 * The standard content surface: a soft, borderless card one step above the screen
 * background. Elevation is carried by tone rather than shadow so stacked cards stay
 * calm.
 */
@Composable
internal fun AppCard(
    modifier: Modifier = Modifier,
    shape: Shape = AppRadius.card,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLowest,
    onClick: (() -> Unit)? = null,
    contentPadding: Dp = AppSpacing.large,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(role = Role.Button, onClick = onClick)
                } else {
                    Modifier
                }
            ),
        shape = shape,
        color = containerColor
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}

/**
 * The brand hero surface — a diagonal emerald gradient used for the one number that
 * owns a screen. Content is laid out by the caller and always drawn in [onHero] ink.
 */
@Composable
internal fun HeroGradientCard(
    modifier: Modifier = Modifier,
    shape: Shape = AppRadius.hero,
    content: @Composable ColumnScope.() -> Unit
) {
    val gradient = MaterialTheme.appColors.heroGradient
    val brush = remember(gradient) {
        Brush.linearGradient(
            colors = gradient,
            start = androidx.compose.ui.geometry.Offset.Zero,
            end = androidx.compose.ui.geometry.Offset.Infinite
        )
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(brush)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.xlarge),
            content = content
        )
    }
}

/** Small uppercase caption that sits directly above a number. */
@Composable
internal fun OverlineText(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Text(
        text = text.uppercase(),
        style = OverlineLabel,
        color = color,
        modifier = modifier
    )
}

/**
 * Currency amounts. Always rendered with tabular figures so columns of money align
 * and a changing value doesn't reflow the row.
 */
@Composable
internal fun AmountText(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight? = FontWeight.Bold,
    maxLines: Int = 1
) {
    Text(
        text = text,
        style = style.merge(TabularFigures),
        color = color,
        fontWeight = fontWeight,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

/**
 * The big amount on a hero card. Unlike [AmountText] it shrinks to fit instead of
 * ellipsing, because a lakh-scale total ("₹1,20,181.45") must stay readable in full —
 * a truncated headline number is worse than a slightly smaller one.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun HeroAmountText(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    minFontSize: TextUnit = 22.sp
) {
    val maxFontSize = if (style.fontSize.isSpecified) style.fontSize else 36.sp
    BasicText(
        text = text,
        modifier = modifier,
        // Line height is dropped so the box tracks the shrunken font instead of
        // reserving space for the full-size style.
        style = style.merge(TabularFigures).copy(
            color = color,
            fontWeight = FontWeight.Bold,
            lineHeight = TextUnit.Unspecified
        ),
        maxLines = 1,
        autoSize = TextAutoSize.StepBased(
            minFontSize = minFontSize,
            maxFontSize = maxFontSize,
            stepSize = 1.sp
        )
    )
}

/** Rounded tag used for percentages and status ("84% of budget", "12 transactions"). */
@Composable
internal fun StatPill(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = AppRadius.pill,
        color = containerColor
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.merge(TabularFigures),
            fontWeight = FontWeight.SemiBold,
            color = contentColor,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = AppSpacing.medium, vertical = 5.dp)
        )
    }
}

/**
 * Section title with an optional trailing action. Replaces the ad-hoc bold Text +
 * TextButton pairs the screens used to repeat.
 */
@Composable
internal fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    actionContentDescription: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = AppSpacing.small),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (actionLabel != null && onActionClick != null) {
            Row(
                modifier = Modifier
                    .clip(AppRadius.pill)
                    .clickable(
                        role = Role.Button,
                        onClickLabel = actionContentDescription,
                        onClick = onActionClick
                    )
                    .padding(horizontal = AppSpacing.small, vertical = AppSpacing.tiny),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Rounded emoji avatar tinted with the category's accent slot. The tint is a wash of
 * the accent rather than the accent itself so the emoji stays readable.
 */
@Composable
internal fun CategoryAvatar(
    emoji: String,
    accent: Color,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    emojiSize: Dp = 22.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(AppRadius.icon)
            .background(accent.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center
    ) {
        Text(text = emoji, fontSize = emojiSize.value.sp)
    }
}

/**
 * Thin share bar with rounded ends. Used for "this category's slice of the period"
 * and for budget progress; always accompanied by the value as text.
 */
@Composable
internal fun ShareBar(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    height: Dp = 6.dp,
    animate: Boolean = true
) {
    val target = progress.coerceIn(0f, 1f)
    val animated by animateFloatAsState(
        targetValue = target,
        label = "share_bar_progress"
    )
    val fraction = if (animate) animated else target
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(AppRadius.pill)
            .background(trackColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction)
                .clip(AppRadius.pill)
                .background(color)
        )
    }
}

/**
 * Two-tone stat tile: a caption, a number, and an optional footnote. Used for the
 * paired highest/lowest month cards and similar small metrics.
 */
@Composable
internal fun MetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    footnote: String? = null,
    leading: @Composable (BoxScope.() -> Unit)? = null
) {
    AppCard(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        contentPadding = AppSpacing.default
    ) {
        if (leading != null) {
            Box(content = leading)
            Spacer(modifier = Modifier.height(AppSpacing.medium))
        }
        OverlineText(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(AppSpacing.tiny))
        AmountText(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = valueColor
        )
        if (footnote != null) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = footnote,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Circular tonal icon button — month stepper, settings, and other bare-background taps. */
@Composable
internal fun CircleIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(percent = 50))
            .background(containerColor)
            .clickable(role = Role.Button, onClickLabel = contentDescription, onClick = onClick),
        contentAlignment = Alignment.Center,
        content = content
    )
}

/** Horizontal spacer sized from the app scale. */
@Composable
internal fun HSpace(width: Dp) {
    Spacer(modifier = Modifier.width(width))
}

/** Vertical spacer sized from the app scale. */
@Composable
internal fun VSpace(height: Dp) {
    Spacer(modifier = Modifier.height(height))
}

@Preview(showBackground = true, name = "App components — light")
@Composable
private fun AppComponentsPreview() {
    ExpanseManagerTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.padding(AppSpacing.screen)) {
                HeroGradientCard {
                    OverlineText("Total spent", MaterialTheme.appColors.onHeroMuted)
                    VSpace(AppSpacing.small)
                    AmountText(
                        text = "₹42,180",
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.appColors.onHero
                    )
                }
                VSpace(AppSpacing.large)
                SectionHeader(title = "Top categories", actionLabel = "View all", onActionClick = {})
                AppCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CategoryAvatar(emoji = "🍔", accent = MaterialTheme.appColors.accentFor("Food"))
                        HSpace(AppSpacing.medium)
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Food & Dining", style = MaterialTheme.typography.bodyLarge)
                            VSpace(AppSpacing.small)
                            ShareBar(
                                progress = 0.62f,
                                color = MaterialTheme.appColors.accentFor("Food"),
                                animate = false
                            )
                        }
                        HSpace(AppSpacing.medium)
                        AmountText(
                            text = "₹12,400",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                VSpace(AppSpacing.default)
                StatPill(
                    text = "84% of budget",
                    containerColor = MaterialTheme.appColors.positiveContainer,
                    contentColor = MaterialTheme.appColors.positive
                )
            }
        }
    }
}
