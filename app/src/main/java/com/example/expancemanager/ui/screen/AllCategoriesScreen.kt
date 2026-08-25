package com.example.expancemanager.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.expancemanager.R
import com.example.expancemanager.ui.components.AppBackTopBar
import com.example.expancemanager.ui.components.AppSpacing
import com.example.expancemanager.ui.components.CategoryTotalCard
import com.example.expancemanager.ui.components.EmptyStateMessage
import com.example.expancemanager.ui.components.PeriodSummaryHeader
import com.example.expancemanager.ui.components.SectionHeader
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

    val totalAmount = uiState.totalAmount
    val formattedTotal = remember(totalAmount) { DateUtils.formatAmount(totalAmount) }
    val monthLabel = remember(month, year) { DateUtils.formatMonthYear(month, year) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppBackTopBar(
                title = stringResource(R.string.categories_all_title),
                onNavigateBack = onNavigateBack
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
                }
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
                        .padding(horizontal = AppSpacing.screen),
                    contentPadding = PaddingValues(bottom = AppSpacing.xlarge)
                ) {
                    item(key = "section_header") {
                        SectionHeader(
                            title = stringResource(R.string.categories_all_title),
                            modifier = Modifier.fillMaxWidth()
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
