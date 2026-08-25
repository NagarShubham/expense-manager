package com.example.expancemanager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expancemanager.ui.theme.AppRadius
import com.example.expancemanager.ui.theme.ExpanseManagerTheme
import com.example.expancemanager.ui.theme.appColors

/**
 * Hero summary for a drill-down screen: the period on the left, the headline amount
 * below it, and an optional category emoji badge on the right.
 */
@Composable
internal fun PeriodSummaryHeader(
    monthLabel: String,
    formattedAmount: String,
    countLabel: String,
    modifier: Modifier = Modifier,
    emoji: String? = null
) {
    val appColors = MaterialTheme.appColors
    HeroGradientCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.screen, vertical = AppSpacing.small)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                OverlineText(text = monthLabel, color = appColors.onHeroMuted)
                VSpace(AppSpacing.small)
                HeroAmountText(
                    text = formattedAmount,
                    style = MaterialTheme.typography.displaySmall,
                    color = appColors.onHero
                )
                VSpace(AppSpacing.tiny)
                Text(
                    text = countLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = appColors.onHeroMuted
                )
            }
            if (emoji != null) {
                HSpace(AppSpacing.default)
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(AppRadius.card)
                        .background(appColors.onHero.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = emoji, fontSize = 30.sp)
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Period summary")
@Composable
private fun PeriodSummaryHeaderPreview() {
    ExpanseManagerTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column {
                PeriodSummaryHeader(
                    monthLabel = "August 2026",
                    formattedAmount = "₹12,450",
                    countLabel = "8 categories"
                )
                PeriodSummaryHeader(
                    monthLabel = "August 2026",
                    formattedAmount = "₹3,200",
                    countLabel = "5 transactions",
                    emoji = "🍔"
                )
            }
        }
    }
}
