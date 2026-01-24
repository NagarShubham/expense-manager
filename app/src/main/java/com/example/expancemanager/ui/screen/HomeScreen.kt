package com.example.expancemanager.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.expancemanager.R
import com.example.expancemanager.data.CategoryTotal
import com.example.expancemanager.ui.components.EmptyStateMessage
import com.example.expancemanager.ui.components.ExpenseItemCard
import com.example.expancemanager.util.DateUtils
import com.example.expancemanager.util.ExpenseCategories
import com.example.expancemanager.viewmodel.ExpenseViewModel

@Composable
fun HomeScreen(
    viewModel: ExpenseViewModel,
    onAddExpenseClick: () -> Unit = {},
    onExpenseClick: (Long) -> Unit = {},
    onCategoryClick: (String, Int, Int) -> Unit = { _, _, _ -> },
    onShowAllCategoriesClick: (Int, Int) -> Unit = { _, _ -> },
    onSettingsClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddExpenseClick,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.cd_add_expense))
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Month selector with settings
            MonthSelector(
                month = uiState.selectedMonth,
                year = uiState.selectedYear,
                onPreviousMonth = { viewModel.changeMonth(-1) },
                onNextMonth = { viewModel.changeMonth(1) },
                onSettingsClick = onSettingsClick
            )

            // Summary card
            SummaryCard(totalAmount = uiState.totalAmount)

            // Expenses list
            if (uiState.expenses.isEmpty()) {
                EmptyStateMessage(
                    emoji = "📊",
                    title = stringResource(R.string.home_empty_title),
                    subtitle = stringResource(R.string.home_empty_subtitle)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = dimensionResource(R.dimen.spacing_default)),
                    contentPadding = PaddingValues(vertical = dimensionResource(R.dimen.spacing_small))
                ) {
                    if (uiState.categoryTotals.isNotEmpty()) {
                        item(key = "category_breakdown") {
                            CategoryBreakdown(
                                categoryTotals = uiState.categoryTotals,
                                month = uiState.selectedMonth,
                                year = uiState.selectedYear,
                                onCategoryClick = onCategoryClick,
                                onShowAllClick = onShowAllCategoriesClick
                            )
                        }
                    }

                    item(key = "recent_transactions_header") {
                        Text(
                            text = stringResource(R.string.home_recent_transactions),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = dimensionResource(R.dimen.spacing_small))
                        )
                    }

                    items(uiState.expenses, key = { it.id }) { expense ->
                        ExpenseItemCard(
                            expense = expense,
                            onExpenseClick = { onExpenseClick(expense.id) },
                            onDeleteExpense = { viewModel.deleteExpense(expense) },
                            showCategory = true,
                            showDescription = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MonthSelector(
    month: Int,
    year: Int,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSettingsClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = dimensionResource(R.dimen.spacing_default),
                vertical = dimensionResource(R.dimen.spacing_small)
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousMonth) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.cd_previous_month),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Text(
            text = DateUtils.formatMonthYear(month, year),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNextMonth) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = stringResource(R.string.cd_next_month),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            IconButton(onClick = onSettingsClick) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = stringResource(R.string.cd_settings),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun SummaryCard(totalAmount: Double) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = dimensionResource(R.dimen.spacing_default),
                vertical = dimensionResource(R.dimen.spacing_small)
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
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
                text = stringResource(R.string.home_total_expenses),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_small)))
            Text(
                text = DateUtils.formatAmount(totalAmount),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
fun CategoryBreakdown(
    categoryTotals: List<CategoryTotal>,
    month: Int,
    year: Int,
    onCategoryClick: (String, Int, Int) -> Unit,
    onShowAllClick: (Int, Int) -> Unit = { _, _ -> }
) {
    val displayedCategories = categoryTotals.take(5)
    val hasMoreCategories = categoryTotals.size > 5

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = dimensionResource(R.dimen.spacing_small))
    ) {
        Column(modifier = Modifier.padding(dimensionResource(R.dimen.spacing_default))) {
            Text(
                text = stringResource(R.string.home_category_breakdown),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = dimensionResource(R.dimen.spacing_medium))
            )

            displayedCategories.forEach { categoryTotal ->
                CategoryItem(
                    categoryTotal = categoryTotal,
                    onClick = { onCategoryClick(categoryTotal.category, month, year) }
                )
            }

            if (hasMoreCategories) {
                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_small)))
                ViewAllCategoriesButton(
                    totalCount = categoryTotals.size,
                    onClick = { onShowAllClick(month, year) }
                )
            }
        }
    }
}

@Composable
private fun CategoryItem(
    categoryTotal: CategoryTotal,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = dimensionResource(R.dimen.spacing_tiny)),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = ExpenseCategories.getCategoryEmoji(categoryTotal.category),
                fontSize = 20.sp,
                modifier = Modifier.padding(end = dimensionResource(R.dimen.spacing_small))
            )
            Text(
                text = categoryTotal.category,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = DateUtils.formatAmount(categoryTotal.total),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(R.string.cd_view_category_details),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = dimensionResource(R.dimen.spacing_tiny))
            )
        }
    }
}

@Composable
private fun ViewAllCategoriesButton(
    totalCount: Int,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(R.string.home_view_all_categories, totalCount),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(dimensionResource(R.dimen.spacing_tiny)))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = stringResource(R.string.cd_view_all_categories),
            tint = MaterialTheme.colorScheme.primary
        )
    }
}
