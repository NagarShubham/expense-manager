package com.example.expancemanager.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.expancemanager.R
import com.example.expancemanager.ui.components.EmptyStateMessage
import com.example.expancemanager.ui.components.ExpenseItemCard
import com.example.expancemanager.util.DateUtils
import com.example.expancemanager.util.ExpenseCategories
import com.example.expancemanager.viewmodel.ExpenseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryExpensesScreen(
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = category,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Category header with emoji and total
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = dimensionResource(R.dimen.spacing_default),
                        vertical = dimensionResource(R.dimen.spacing_small)
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = dimensionResource(R.dimen.card_elevation_raised))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(dimensionResource(R.dimen.spacing_xlarge)),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = ExpenseCategories.getCategoryEmoji(category, uiState.categoryEmojiMap),
                        fontSize = 48.sp
                    )
                    Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_small)))
                    Text(
                        text = DateUtils.formatMonthYear(month, year),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_small)))
                    Text(
                        text = DateUtils.formatAmount(categoryTotal),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = if (categoryExpenses.size == 1) {
                            stringResource(R.string.category_transaction_count_singular, categoryExpenses.size)
                        } else {
                            stringResource(R.string.category_transaction_count_plural, categoryExpenses.size)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    )
                }
            }

            // Expenses list
            if (categoryExpenses.isEmpty()) {
                EmptyStateMessage(
                    emoji = ExpenseCategories.getCategoryEmoji(category, uiState.categoryEmojiMap),
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
