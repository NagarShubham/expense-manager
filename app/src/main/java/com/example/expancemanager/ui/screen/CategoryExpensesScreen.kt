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
import com.example.expancemanager.ui.components.EmptyStateMessage
import com.example.expancemanager.ui.components.ExpenseItemCard
import com.example.expancemanager.ui.components.PeriodSummaryHeader
import com.example.expancemanager.ui.components.SectionHeader
import com.example.expancemanager.util.DateUtils
import com.example.expancemanager.util.ExpenseCategories
import com.example.expancemanager.viewmodel.ExpenseViewModel

@Composable
internal fun CategoryExpensesScreen(
    category: String,
    month: Int,
    year: Int,
    viewModel: ExpenseViewModel,
    onNavigateBack: () -> Unit = {},
    onExpenseClick: (Long) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    val categoryExpenses = remember(uiState.expenses, category) {
        uiState.expenses.filter { it.category == category }
    }
    val categoryTotal = remember(uiState.categoryTotals, category) {
        uiState.categoryTotals.firstOrNull { it.category == category }?.total ?: 0.0
    }
    val formattedTotal = remember(categoryTotal) { DateUtils.formatAmount(categoryTotal) }
    val monthLabel = remember(month, year) { DateUtils.formatMonthYear(month, year) }
    val categoryEmoji = remember(category, uiState.categoryEmojiMap) {
        ExpenseCategories.getCategoryEmoji(category, uiState.categoryEmojiMap)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppBackTopBar(
                title = category,
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
                countLabel = if (categoryExpenses.size == 1) {
                    stringResource(R.string.category_transaction_count_singular, categoryExpenses.size)
                } else {
                    stringResource(R.string.category_transaction_count_plural, categoryExpenses.size)
                },
                emoji = categoryEmoji
            )

            if (categoryExpenses.isEmpty()) {
                EmptyStateMessage(
                    emoji = categoryEmoji,
                    title = stringResource(R.string.category_empty_title),
                    subtitle = stringResource(R.string.category_empty_subtitle, category)
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
                            title = stringResource(R.string.category_all_transactions),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    items(categoryExpenses, key = { it.id }) { expense ->
                        ExpenseItemCard(
                            expense = expense,
                            onExpenseClick = { onExpenseClick(expense.id) },
                            onDeleteExpense = { viewModel.deleteExpense(expense) },
                            emojiMap = uiState.categoryEmojiMap,
                            showCategory = false,
                            showDescription = true
                        )
                    }
                }
            }
        }
    }
}
