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
import com.example.expancemanager.ui.components.EmptyStateMessage
import com.example.expancemanager.ui.components.ExpenseItemCard
import com.example.expancemanager.ui.components.PeriodSummaryHeader
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

    // Filter expenses by category
    val categoryExpenses = remember(uiState.expenses, category) {
        uiState.expenses.filter { it.category == category }
    }

    // Calculate total for this category
    val categoryTotal = remember(categoryExpenses) {
        categoryExpenses.sumOf { it.amount }
    }
    val formattedTotal = remember(categoryTotal) { DateUtils.formatAmount(categoryTotal) }
    val monthLabel = remember(month, year) { DateUtils.formatMonthYear(month, year) }
    val categoryEmoji = remember(category, uiState.categoryEmojiMap) {
        ExpenseCategories.getCategoryEmoji(category, uiState.categoryEmojiMap)
    }

    Scaffold(
        topBar = {
            AppBackTopBar(
                title = category,
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
                countLabel = if (categoryExpenses.size == 1) {
                    stringResource(R.string.category_transaction_count_singular, categoryExpenses.size)
                } else {
                    stringResource(R.string.category_transaction_count_plural, categoryExpenses.size)
                },
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
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
                        .padding(horizontal = dimensionResource(R.dimen.spacing_default)),
                    contentPadding = PaddingValues(vertical = dimensionResource(R.dimen.spacing_small))
                ) {
                    item {
                        Text(
                            text = stringResource(R.string.category_all_transactions),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = dimensionResource(R.dimen.spacing_small))
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
