package com.example.expancemanager.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.expancemanager.R
import com.example.expancemanager.data.CategoryTotal
import com.example.expancemanager.ui.components.EmptyStateMessage
import com.example.expancemanager.ui.components.ExpenseItemCard
import com.example.expancemanager.util.DateUtils
import com.example.expancemanager.util.ExpenseCategories
import com.example.expancemanager.viewmodel.ExpenseViewModel

/**
 * Constants for LazyColumn content types to optimize scrolling performance
 */
private object HomeContentType {
    const val CATEGORY_BREAKDOWN = "category_breakdown"
    const val HEADER = "header"
    const val EXPENSE_ITEM = "expense_item"
}

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
                onMonthYearClick = { viewModel.goToCurrentMonth() },
                onSettingsClick = onSettingsClick
            )

            // Summary card with optional expected budget (budget comparison uses totalAmountForBudget = total minus excluded categories)
            if (uiState.expenses.isNotEmpty()) {
                BudgetSummaryCard(
                    totalAmount = uiState.totalAmount,
                    totalAmountForBudget = uiState.totalAmountForBudget,
                    expectedAmount = uiState.expectedMonthlyAmount,
                    excludedCategoryNames = uiState.excludedCategoryNames,
                )
            }

            // Expenses list
            if (uiState.expenses.isEmpty()) {
                EmptyStateMessage(
                    emoji = stringResource(R.string.home_empty_emoji),
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
                        item(
                            key = "category_breakdown",
                            contentType = HomeContentType.CATEGORY_BREAKDOWN
                        ) {
                            CategoryBreakdown(
                                categoryTotals = uiState.categoryTotals,
                                month = uiState.selectedMonth,
                                year = uiState.selectedYear,
                                emojiMap = uiState.categoryEmojiMap,
                                onCategoryClick = onCategoryClick,
                                onShowAllClick = onShowAllCategoriesClick
                            )
                        }
                    }

                    item(
                        key = "recent_transactions_header",
                        contentType = HomeContentType.HEADER
                    ) {
                        Text(
                            text = stringResource(R.string.home_recent_transactions),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = dimensionResource(R.dimen.spacing_small))
                        )
                    }

                    items(
                        items = uiState.expenses,
                        key = { it.id },
                        contentType = { HomeContentType.EXPENSE_ITEM }
                    ) { expense ->
                        ExpenseItemCard(
                            expense = expense,
                            onExpenseClick = { onExpenseClick(expense.id) },
                            onDeleteExpense = { viewModel.deleteExpense(expense) },
                            emojiMap = uiState.categoryEmojiMap,
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
    onMonthYearClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    val formattedMonthYear = remember(month, year) { DateUtils.formatMonthYear(month, year) }
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
            text = formattedMonthYear,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clickable(
                role = Role.Button,
                onClickLabel = stringResource(R.string.cd_go_to_current_month),
                onClick = onMonthYearClick
            )
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

private data class BudgetDerived(
    val formattedTotal: String,
    val formattedExpected: String?,
    val formattedUsedForBudget: String,
    val remaining: Double?,
    val overspent: Double?,
    val isOverspent: Boolean,
    val progress: Float,
    val percentUsed: Int
)

@Composable
fun BudgetSummaryCard(
    totalAmount: Double,
    totalAmountForBudget: Double = totalAmount,
    expectedAmount: Double?,
    excludedCategoryNames: Set<String> = emptySet(),
) {
    val derived = remember(totalAmount, totalAmountForBudget, expectedAmount) {
        val exp = expectedAmount
        val remaining = exp?.let { (it - totalAmountForBudget).coerceAtLeast(0.0) }
        val overspent = exp?.let { (totalAmountForBudget - it).coerceAtLeast(0.0) }
        val isOverspent = (overspent ?: 0.0) > 0
        val progress = if (exp != null && exp > 0) (totalAmountForBudget / exp).toFloat().coerceIn(0f, 1f) else 0f
        val percentUsed = if (exp != null && exp > 0) ((totalAmountForBudget / exp) * 100).toInt().coerceIn(0, 999) else 0
        BudgetDerived(
            formattedTotal = DateUtils.formatAmount(totalAmount),
            formattedExpected = exp?.let { DateUtils.formatAmount(it) },
            formattedUsedForBudget = DateUtils.formatAmount(totalAmountForBudget),
            remaining = remaining,
            overspent = overspent,
            isOverspent = isOverspent,
            progress = progress,
            percentUsed = percentUsed
        )
    }

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
                .padding(dimensionResource(R.dimen.spacing_default)),
            horizontalAlignment = if (expectedAmount == null) Alignment.CenterHorizontally else Alignment.Start
        ) {
            if (expectedAmount == null) {
                Text(
                    text = stringResource(R.string.home_total_expenses),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                Text(
                    text = derived.formattedTotal,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.home_total_expenses),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Text(
                            text = derived.formattedTotal,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(dimensionResource(R.dimen.spacing_small)),
                        color = if (derived.isOverspent) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = if (derived.isOverspent) stringResource(R.string.budget_status_over) else stringResource(R.string.budget_percent_used, derived.percentUsed),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (derived.isOverspent) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(
                                horizontal = dimensionResource(R.dimen.spacing_small),
                                vertical = dimensionResource(R.dimen.spacing_tiny)
                            )
                        )
                    }
                }
            }

            if (expectedAmount != null) {
                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_small)))
                Text(
                    text = when {
                        derived.isOverspent && derived.overspent != null -> stringResource(R.string.budget_summary_over, DateUtils.formatAmount(derived.overspent))
                        derived.remaining != null -> stringResource(R.string.budget_summary_left, DateUtils.formatAmount(derived.remaining), derived.formattedExpected ?: "")
                        else -> ""
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (derived.isOverspent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_small)))
                LinearProgressIndicator(
                    progress = { derived.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(dimensionResource(R.dimen.spacing_small)),
                    color = if (derived.isOverspent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                )
                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_tiny)))
                Text(
                    text = stringResource(R.string.budget_summary_used_of, derived.formattedUsedForBudget, derived.formattedExpected ?: ""),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                if (excludedCategoryNames.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.budget_categories_excluded_hint, excludedCategoryNames.size),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryBreakdown(
    categoryTotals: List<CategoryTotal>,
    month: Int,
    year: Int,
    emojiMap: Map<String, String>,
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
                    emojiMap = emojiMap,
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
    emojiMap: Map<String, String>,
    onClick: () -> Unit
) {
    val categoryEmoji = remember(categoryTotal.category, emojiMap) {
        ExpenseCategories.getCategoryEmoji(categoryTotal.category, emojiMap)
    }
    val formattedAmount = remember(categoryTotal.total) {
        DateUtils.formatAmount(categoryTotal.total)
    }

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
                text = categoryEmoji,
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
                text = formattedAmount,
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
