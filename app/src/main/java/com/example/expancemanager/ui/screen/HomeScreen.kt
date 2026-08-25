package com.example.expancemanager.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.expancemanager.R
import com.example.expancemanager.data.CategoryTotal
import com.example.expancemanager.ui.components.AppSpacing
import com.example.expancemanager.ui.components.CategorySnapshotCard
import com.example.expancemanager.ui.components.CircleIconButton
import com.example.expancemanager.ui.components.EmptyStateAction
import com.example.expancemanager.ui.components.EmptyStateMessage
import com.example.expancemanager.ui.components.ExpenseItemCard
import com.example.expancemanager.ui.components.HSpace
import com.example.expancemanager.ui.components.HeroAmountText
import com.example.expancemanager.ui.components.HeroGradientCard
import com.example.expancemanager.ui.components.OverlineText
import com.example.expancemanager.ui.components.SectionHeader
import com.example.expancemanager.ui.components.ShareBar
import com.example.expancemanager.ui.components.StatPill
import com.example.expancemanager.ui.components.VSpace
import com.example.expancemanager.ui.theme.appColors
import com.example.expancemanager.util.DateUtils
import com.example.expancemanager.viewmodel.ExpenseViewModel

/**
 * Constants for LazyColumn content types to optimize scrolling performance
 */
private object HomeContentType {
    const val MONTH_BAR = "month_bar"
    const val HERO = "hero"
    const val CATEGORY_BREAKDOWN = "category_breakdown"
    const val HEADER = "header"
    const val EXPENSE_ITEM = "expense_item"
    const val CATEGORY_CARD = "category_card"
}

/** Categories shown inline on Home before the list defers to the All Categories screen. */
private const val HOME_CATEGORY_LIMIT = 5
private const val HOME_CATEGORY_CARD_WIDTH_FRACTION = 0.42f

@Composable
internal fun HomeScreen(
    viewModel: ExpenseViewModel,
    onAddExpenseClick: () -> Unit = {},
    onExpenseClick: (Long) -> Unit = {},
    onCategoryClick: (String, Int, Int) -> Unit = { _, _, _ -> },
    onShowAllCategoriesClick: (Int, Int) -> Unit = { _, _ -> },
    onSearchClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddExpenseClick,
                expanded = false,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.home_empty_action)) }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(
                start = AppSpacing.screen,
                end = AppSpacing.screen,
                bottom = 96.dp
            )
        ) {
            item(key = "month_bar", contentType = HomeContentType.MONTH_BAR) {
                MonthSelector(
                    month = uiState.selectedMonth,
                    year = uiState.selectedYear,
                    onPreviousMonth = { viewModel.changeMonth(-1) },
                    onNextMonth = { viewModel.changeMonth(1) },
                    onMonthYearClick = { viewModel.goToCurrentMonth() },
                    onSearchClick = onSearchClick,
                    onSettingsClick = onSettingsClick
                )
            }

            // Budget comparison uses totalAmountForBudget (total minus excluded categories).
            item(key = "hero", contentType = HomeContentType.HERO) {
                BudgetHeroCard(
                    totalAmount = uiState.totalAmount,
                    totalAmountForBudget = uiState.totalAmountForBudget,
                    expectedAmount = uiState.expectedMonthlyAmount,
                    transactionCount = uiState.expenses.size,
                    excludedCategoryNames = uiState.excludedCategoryNames
                )
                VSpace(AppSpacing.small)
            }

            if (uiState.expenses.isEmpty()) {
                item(key = "empty") {
                    VSpace(AppSpacing.xxlarge)
                    EmptyStateMessage(
                        emoji = stringResource(R.string.home_empty_emoji),
                        title = stringResource(R.string.home_empty_title),
                        subtitle = stringResource(R.string.home_empty_subtitle),
                        modifier = Modifier.fillMaxWidth(),
                        action = {
                            EmptyStateAction(
                                text = stringResource(R.string.home_empty_action),
                                onClick = onAddExpenseClick
                            )
                        }
                    )
                }
            } else {
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
                    VSpace(AppSpacing.small)
                    SectionHeader(title = stringResource(R.string.home_recent_transactions, uiState.expenses.size))
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

@Composable
private fun MonthSelector(
    month: Int,
    year: Int,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onMonthYearClick: () -> Unit = {},
    onSearchClick: (() -> Unit)? = null,
    onSettingsClick: (() -> Unit)? = null
) {
    val formattedMonthYear = remember(month, year) { DateUtils.formatMonthYear(month, year) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = AppSpacing.medium, bottom = AppSpacing.default),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(
                    role = Role.Button,
                    onClickLabel = stringResource(R.string.cd_go_to_current_month),
                    onClick = onMonthYearClick
                )
        ) {
            // Wordmark above the month, so the header reads as an app bar without
            // needing an actual TopAppBar on the home screen.
            OverlineText(
                text = stringResource(R.string.app_name),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = formattedMonthYear,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.small)
        ) {
            CircleIconButton(
                onClick = onPreviousMonth,
                contentDescription = stringResource(R.string.cd_previous_month)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.cd_previous_month),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(22.dp)
                )
            }
            CircleIconButton(
                onClick = onNextMonth,
                contentDescription = stringResource(R.string.cd_next_month)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.cd_next_month),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(22.dp)
                )
            }
            if (onSearchClick != null) {
                CircleIconButton(
                    onClick = onSearchClick,
                    contentDescription = stringResource(R.string.cd_search)
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = stringResource(R.string.cd_search),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            if (onSettingsClick != null) {
                CircleIconButton(
                    onClick = onSettingsClick,
                    contentDescription = stringResource(R.string.cd_settings)
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = stringResource(R.string.cd_settings),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

private data class BudgetDerived(
    val formattedTotal: String,
    val formattedExpected: String?,
    val formattedUsedForBudget: String,
    val formattedRemaining: String?,
    val formattedOverspent: String?,
    val isOverspent: Boolean,
    val progress: Float,
    val percentUsed: Int,
    val percentOver: Int
)

/**
 * The screen's hero: total spent for the month on the brand gradient, with budget
 * progress folded in when a budget exists. Everything the card shows is also spelled
 * out in text, so the bar is reinforcement rather than the only signal.
 */
@Composable
private fun BudgetHeroCard(
    totalAmount: Double,
    totalAmountForBudget: Double,
    expectedAmount: Double?,
    transactionCount: Int,
    excludedCategoryNames: Set<String> = emptySet()
) {
    val derived = remember(totalAmount, totalAmountForBudget, expectedAmount) {
        val exp = expectedAmount
        val remaining = exp?.let { (it - totalAmountForBudget).coerceAtLeast(0.0) }
        val overspent = exp?.let { (totalAmountForBudget - it).coerceAtLeast(0.0) }
        val isOverspent = (overspent ?: 0.0) > 0
        val budgetRatio = if (exp != null && exp > 0) totalAmountForBudget / exp else 0.0
        val progress = budgetRatio.toFloat().coerceIn(0f, 1f)
        val percentUsed = (budgetRatio * 100).toInt().coerceIn(0, 999)
        val percentOver = if (budgetRatio > 1.0) {
            ((budgetRatio - 1.0) * 100).toInt().coerceIn(0, 999)
        } else {
            0
        }
        BudgetDerived(
            formattedTotal = DateUtils.formatAmount(totalAmount, hideZeroDecimals = true),
            formattedExpected = exp?.let { DateUtils.formatAmount(it, hideZeroDecimals = true) },
            formattedUsedForBudget = DateUtils.formatAmount(totalAmountForBudget, hideZeroDecimals = true),
            formattedRemaining = remaining?.let { DateUtils.formatAmount(it, hideZeroDecimals = true) },
            formattedOverspent = overspent?.let { DateUtils.formatAmount(it, hideZeroDecimals = true) },
            isOverspent = isOverspent,
            progress = progress,
            percentUsed = percentUsed,
            percentOver = percentOver
        )
    }
    val appColors = MaterialTheme.appColors
    val onHero = appColors.onHero
    val onHeroMuted = appColors.onHeroMuted

    HeroGradientCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                OverlineText(
                    text = stringResource(R.string.home_total_spent),
                    color = onHeroMuted
                )
                VSpace(AppSpacing.small)
                HeroAmountText(
                    text = derived.formattedTotal,
                    style = MaterialTheme.typography.displaySmall,
                    color = onHero
                )
            }
            if (expectedAmount != null) {
                HSpace(AppSpacing.small)
                StatPill(
                    text = if (derived.isOverspent) {
                        stringResource(R.string.budget_percent_over, derived.percentOver)
                    } else {
                        stringResource(R.string.budget_percent_used, derived.percentUsed)
                    },
                    // On the gradient, over-budget needs its own alarm color; on-budget
                    // stays a translucent wash so it reads as a quiet status chip.
                    containerColor = if (derived.isOverspent) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        onHero.copy(alpha = 0.2f)
                    },
                    contentColor = if (derived.isOverspent) {
                        MaterialTheme.colorScheme.onErrorContainer
                    } else {
                        onHero
                    }
                )
            }
        }

        if (expectedAmount == null) {
            VSpace(AppSpacing.medium)
            Text(
                text = if (transactionCount == 1) {
                    stringResource(R.string.home_transactions_count_singular)
                } else {
                    stringResource(R.string.home_transactions_count, transactionCount)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = onHeroMuted
            )
        } else {
            VSpace(AppSpacing.large)
            ShareBar(
                progress = derived.progress,
                color = onHero,
                trackColor = onHero.copy(alpha = 0.24f),
                height = 8.dp
            )
            VSpace(AppSpacing.medium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when {
                        derived.isOverspent && derived.formattedOverspent != null ->
                            stringResource(R.string.budget_summary_over, derived.formattedOverspent)

                        derived.formattedRemaining != null ->
                            stringResource(
                                R.string.budget_summary_left,
                                derived.formattedRemaining,
                                derived.formattedExpected.orEmpty()
                            )

                        else -> ""
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = onHero,
                    modifier = Modifier.weight(1f)
                )
                HSpace(AppSpacing.small)
                Text(
                    text = stringResource(
                        R.string.budget_summary_used_of,
                        derived.formattedUsedForBudget,
                        derived.formattedExpected.orEmpty()
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = onHeroMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (excludedCategoryNames.isNotEmpty()) {
                VSpace(AppSpacing.tiny)
                Text(
                    text = stringResource(
                        R.string.budget_categories_excluded_hint,
                        excludedCategoryNames.size
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = onHeroMuted
                )
            }
        }
    }
}

@Composable
private fun CategoryBreakdown(
    categoryTotals: List<CategoryTotal>,
    month: Int,
    year: Int,
    emojiMap: Map<String, String>,
    onCategoryClick: (String, Int, Int) -> Unit,
    onShowAllClick: (Int, Int) -> Unit = { _, _ -> }
) {
    val strip = remember(categoryTotals) {
        CategoryStrip(
            items = categoryTotals.take(HOME_CATEGORY_LIMIT),
            largestTotal = categoryTotals.maxOfOrNull { it.total } ?: 0.0,
            showViewAll = categoryTotals.size > HOME_CATEGORY_LIMIT
        )
    }

    Column {
        SectionHeader(
            title = stringResource(R.string.home_top_categories),
            actionLabel = if (strip.showViewAll) stringResource(R.string.home_view_all) else null,
            actionContentDescription = stringResource(R.string.cd_view_all_categories),
            onActionClick = if (strip.showViewAll) {
                { onShowAllClick(month, year) }
            } else {
                null
            }
        )
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.medium)
        ) {
            items(
                items = strip.items,
                key = { it.category },
                contentType = { HomeContentType.CATEGORY_CARD }
            ) { categoryTotal ->
                CategorySnapshotCard(
                    modifier = Modifier.fillParentMaxWidth(HOME_CATEGORY_CARD_WIDTH_FRACTION),
                    categoryTotal = categoryTotal,
                    largestTotal = strip.largestTotal,
                    emojiMap = emojiMap,
                    onClick = { onCategoryClick(categoryTotal.category, month, year) }
                )
            }
        }
    }
}

private data class CategoryStrip(
    val items: List<CategoryTotal>,
    val largestTotal: Double,
    val showViewAll: Boolean
)
