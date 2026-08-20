package com.example.expancemanager.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.expancemanager.R
import com.example.expancemanager.data.CategoryTotal
import com.example.expancemanager.ui.theme.AppRadius
import com.example.expancemanager.ui.theme.ExpanseManagerTheme
import com.example.expancemanager.ui.theme.appColors
import com.example.expancemanager.util.DateUtils
import com.example.expancemanager.util.ExpenseCategories

/**
 * Category row with its share of the period: accent avatar, name, share bar, amount.
 * The bar is the category's own accent so the same category reads the same on every
 * screen; the percentage is always spelled out beside it.
 */
@Composable
internal fun CategoryTotalCard(
    categoryTotal: CategoryTotal,
    totalAmount: Double,
    emojiMap: Map<String, String>,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val share = remember(categoryTotal.total, totalAmount) {
        if (totalAmount > 0) (categoryTotal.total / totalAmount).toFloat() else 0f
    }
    val percentage = remember(share) { (share * 100).toInt() }
    val formattedAmount = remember(categoryTotal.total) {
        DateUtils.formatAmount(categoryTotal.total, hideZeroDecimals = true)
    }
    val categoryEmoji = remember(categoryTotal.category, emojiMap) {
        ExpenseCategories.getCategoryEmoji(categoryTotal.category, emojiMap)
    }
    val appColors = MaterialTheme.appColors
    val accent = remember(categoryTotal.category, appColors) { appColors.accentFor(categoryTotal.category) }

    AppCard(
        modifier = modifier.padding(vertical = AppSpacing.tiny),
        onClick = onClick,
        contentPadding = AppSpacing.default
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryAvatar(emoji = categoryEmoji, accent = accent)
            HSpace(AppSpacing.medium)

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = categoryTotal.category,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    HSpace(AppSpacing.small)
                    AmountText(
                        text = formattedAmount,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                VSpace(AppSpacing.small)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    ShareBar(
                        progress = share,
                        color = accent,
                        modifier = Modifier.weight(1f)
                    )
                    HSpace(AppSpacing.small)
                    Text(
                        text = stringResource(R.string.reports_category_percent, percentage),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (onClick != null) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.cd_view_category_details),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(start = AppSpacing.tiny)
                        .size(20.dp)
                )
            }
        }
    }
}

/**
 * Compact category tile for a horizontal strip: icon, name, amount, share bar.
 * Accent is limited to the avatar and bar so the card stays a surface, not a wash.
 */
@Composable
internal fun CategorySnapshotCard(
    categoryTotal: CategoryTotal,
    largestTotal: Double,
    emojiMap: Map<String, String>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.width(148.dp)
) {
    val appColors = MaterialTheme.appColors
    val visuals = remember(categoryTotal, largestTotal, emojiMap, appColors) {
        CategorySnapshotVisuals(
            emoji = ExpenseCategories.getCategoryEmoji(categoryTotal.category, emojiMap),
            formattedAmount = DateUtils.formatAmount(categoryTotal.total, hideZeroDecimals = true),
            share = if (largestTotal > 0) (categoryTotal.total / largestTotal).toFloat() else 0f,
            accent = appColors.accentFor(categoryTotal.category)
        )
    }

    Surface(
        modifier = modifier,
        shape = AppRadius.card,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(AppSpacing.medium)) {
            CategoryAvatar(
                emoji = visuals.emoji,
                accent = visuals.accent,
                size = 36.dp,
                emojiSize = 18.dp
            )
            VSpace(AppSpacing.medium)
            Text(
                text = categoryTotal.category,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            VSpace(AppSpacing.tiny)
            AmountText(
                text = visuals.formattedAmount,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            VSpace(AppSpacing.medium)
            ShareBar(
                progress = visuals.share,
                color = visuals.accent,
                height = 5.dp,
                animate = false
            )
        }
    }
}

private data class CategorySnapshotVisuals(
    val emoji: String,
    val formattedAmount: String,
    val share: Float,
    val accent: Color
)

private val previewEmojiMap = ExpenseCategories.DEFAULT_CATEGORIES.toMap()

@Preview(showBackground = true, name = "Category total card")
@Composable
private fun CategoryTotalCardPreview() {
    ExpanseManagerTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.padding(AppSpacing.screen)) {
                CategoryTotalCard(
                    categoryTotal = CategoryTotal(category = "Food & Dining", total = 3_200.1),
                    totalAmount = 12_450.0,
                    emojiMap = previewEmojiMap
                )
                CategoryTotalCard(
                    categoryTotal = CategoryTotal(category = "Transportation", total = 1_800.0),
                    totalAmount = 12_450.0,
                    emojiMap = previewEmojiMap,
                    onClick = {}
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Category total card — dark")
@Composable
private fun CategoryTotalCardDarkPreview() {
    ExpanseManagerTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.padding(AppSpacing.screen)) {
                CategoryTotalCard(
                    categoryTotal = CategoryTotal(category = "Shopping", total = 5_400.0),
                    totalAmount = 12_450.0,
                    emojiMap = previewEmojiMap,
                    onClick = {}
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Category snapshot card")
@Preview(
    showBackground = true,
    name = "Category snapshot card — dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun CategorySnapshotCardPreview() {
    ExpanseManagerTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Row(
                modifier = Modifier.padding(AppSpacing.screen),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.medium)
            ) {
                CategorySnapshotCard(
                    categoryTotal = CategoryTotal(category = "Food & Dining", total = 3_200.1),
                    largestTotal = 3_200.1,
                    emojiMap = previewEmojiMap,
                    onClick = {}
                )
                CategorySnapshotCard(
                    categoryTotal = CategoryTotal(category = "Shopping", total = 1_800.0),
                    largestTotal = 3_200.1,
                    emojiMap = previewEmojiMap,
                    onClick = {}
                )
            }
        }
    }
}
