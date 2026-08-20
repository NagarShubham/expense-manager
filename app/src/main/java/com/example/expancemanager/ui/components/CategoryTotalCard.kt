package com.example.expancemanager.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.example.expancemanager.R
import com.example.expancemanager.data.CategoryTotal
import com.example.expancemanager.ui.theme.ExpanseManagerTheme
import com.example.expancemanager.util.DateUtils
import com.example.expancemanager.util.ExpenseCategories

@Composable
internal fun CategoryTotalCard(
    categoryTotal: CategoryTotal,
    totalAmount: Double,
    emojiMap: Map<String, String>,
    onClick: (() -> Unit)? = null
) {
    val percentage = remember(categoryTotal.total, totalAmount) {
        if (totalAmount > 0) (categoryTotal.total / totalAmount * 100).toInt() else 0
    }
    val formattedAmount = remember(categoryTotal.total) {
        DateUtils.formatAmount(categoryTotal.total, hideZeroDecimals = true)
    }
    val categoryEmoji = remember(categoryTotal.category, emojiMap) {
        ExpenseCategories.getCategoryEmoji(categoryTotal.category, emojiMap)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = dimensionResource(R.dimen.spacing_tiny))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        elevation = CardDefaults.cardElevation(defaultElevation = dimensionResource(R.dimen.card_elevation_default))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.spacing_default)),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = categoryEmoji,
                    fontSize = 32.sp,
                    modifier = Modifier.padding(end = dimensionResource(R.dimen.spacing_medium))
                )

                Column {
                    Text(
                        text = categoryTotal.category,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_tiny)))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        LinearProgressIndicator(
                            progress = { (percentage / 100f) },
                            modifier = Modifier
                                .weight(1f)
                                .height(dimensionResource(R.dimen.spacing_tiny)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                        Spacer(modifier = Modifier.width(dimensionResource(R.dimen.spacing_small)))
                        Text(
                            text = stringResource(R.string.reports_category_percent, percentage),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formattedAmount,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                onClick?.let {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = stringResource(R.string.cd_view_category_details),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = dimensionResource(R.dimen.spacing_tiny))
                    )
                }
            }
        }
    }
}

private val previewEmojiMap = ExpenseCategories.DEFAULT_CATEGORIES.toMap()

@Preview(showBackground = true, name = "Category total card")
@Composable
private fun CategoryTotalCardPreview() {
    ExpanseManagerTheme(dynamicColor = false) {
        Column(modifier = Modifier.padding(dimensionResource(R.dimen.spacing_default))) {
            CategoryTotalCard(
                categoryTotal = CategoryTotal(category = "Food & Dining", total = 3_200.1),
                totalAmount = 12_450.0,
                emojiMap = previewEmojiMap
            )
        }
    }
}

@Preview(showBackground = true, name = "Category total card clickable")
@Composable
private fun CategoryTotalCardClickablePreview() {
    ExpanseManagerTheme(dynamicColor = false) {
        Column(modifier = Modifier.padding(dimensionResource(R.dimen.spacing_default))) {
            CategoryTotalCard(
                categoryTotal = CategoryTotal(category = "Transportation", total = 1_800.0),
                totalAmount = 12_450.0,
                emojiMap = previewEmojiMap,
                onClick = {}
            )
        }
    }
}
