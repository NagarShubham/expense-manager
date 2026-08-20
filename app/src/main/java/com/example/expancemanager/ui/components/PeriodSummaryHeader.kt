package com.example.expancemanager.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.example.expancemanager.R
import com.example.expancemanager.ui.theme.ExpanseManagerTheme

@Composable
internal fun PeriodSummaryHeader(
    monthLabel: String,
    formattedAmount: String,
    countLabel: String,
    containerColor: Color,
    contentColor: Color,
    emoji: String? = null
) {
    val muted = contentColor.copy(alpha = 0.7f)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = dimensionResource(R.dimen.spacing_default),
                vertical = dimensionResource(R.dimen.spacing_small)
            ),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = dimensionResource(R.dimen.card_elevation_raised))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.spacing_xlarge)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (emoji != null) {
                Text(text = emoji, fontSize = 48.sp)
                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_small)))
            }
            Text(
                text = monthLabel,
                style = MaterialTheme.typography.titleMedium,
                color = muted
            )
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_small)))
            Text(
                text = formattedAmount,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
            Text(
                text = countLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = muted
            )
        }
    }
}

@Preview(showBackground = true, name = "Period summary")
@Composable
private fun PeriodSummaryHeaderPreview() {
    ExpanseManagerTheme(dynamicColor = false) {
        PeriodSummaryHeader(
            monthLabel = "August 2026",
            formattedAmount = "₹12,450",
            countLabel = "8 categories",
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Preview(showBackground = true, name = "Period summary with emoji")
@Composable
private fun PeriodSummaryHeaderWithEmojiPreview() {
    ExpanseManagerTheme(dynamicColor = false) {
        PeriodSummaryHeader(
            monthLabel = "August 2026",
            formattedAmount = "₹3,200",
            countLabel = "5 transactions",
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            emoji = "🍔"
        )
    }
}
