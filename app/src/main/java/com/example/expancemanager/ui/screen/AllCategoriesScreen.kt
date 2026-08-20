package com.example.expancemanager.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.example.expancemanager.R
import com.example.expancemanager.ui.components.AppBackTopBar
import com.example.expancemanager.ui.components.CategoryTotalCard
import com.example.expancemanager.ui.components.EmptyStateMessage
import com.example.expancemanager.ui.components.PeriodSummaryHeader
import com.example.expancemanager.util.DateUtils
import com.example.expancemanager.viewmodel.ExpenseViewModel

@Composable
internal fun AllCategoriesScreen(
    month: Int,
    year: Int,
    viewModel: ExpenseViewModel,
    onNavigateBack: () -> Unit = {},
    onCategoryClick: (String, Int, Int) -> Unit = { _, _, _ -> }
) {
    val uiState by viewModel.uiState.collectAsState()

    // Calculate total amount
    val totalAmount = remember(uiState.categoryTotals) {
        uiState.categoryTotals.sumOf { it.total }
    }
    val formattedTotal = remember(totalAmount) { DateUtils.formatAmount(totalAmount) }
    val monthLabel = remember(month, year) { DateUtils.formatMonthYear(month, year) }

    Scaffold(
        topBar = {
            AppBackTopBar(
                title = stringResource(R.string.categories_all_title),
                onNavigateBack = onNavigateBack,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            PeriodSummaryHeader(
                monthLabel = monthLabel,
                formattedAmount = formattedTotal,
                countLabel = if (uiState.categoryTotals.size == 1) {
                    stringResource(R.string.categories_count_singular, uiState.categoryTotals.size)
                } else {
                    stringResource(R.string.categories_count_plural, uiState.categoryTotals.size)
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )

            if (uiState.categoryTotals.isEmpty()) {
                EmptyStateMessage(
                    emoji = stringResource(R.string.home_empty_emoji),
                    title = stringResource(R.string.categories_empty_title),
                    subtitle = stringResource(R.string.categories_empty_subtitle)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = dimensionResource(R.dimen.spacing_default)),
                    contentPadding = PaddingValues(vertical = dimensionResource(R.dimen.spacing_small))
                ) {
                    item {
                        Text(
                            text = stringResource(R.string.categories_all_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = dimensionResource(R.dimen.spacing_small))
                        )
                    }

                    items(uiState.categoryTotals, key = { it.category }) { categoryTotal ->
                        CategoryTotalCard(
                            categoryTotal = categoryTotal,
                            totalAmount = totalAmount,
                            emojiMap = uiState.categoryEmojiMap,
                            onClick = { onCategoryClick(categoryTotal.category, month, year) }
                        )
                    }
                }
            }
        }
    }
}
