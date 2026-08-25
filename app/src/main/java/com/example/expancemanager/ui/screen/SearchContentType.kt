package com.example.expancemanager.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.expancemanager.R
import com.example.expancemanager.data.Category
import com.example.expancemanager.data.Expense
import com.example.expancemanager.data.ExpenseFilter
import com.example.expancemanager.data.ExpenseSortOrder
import com.example.expancemanager.ui.components.AppBackTopBar
import com.example.expancemanager.ui.components.AppSpacing
import com.example.expancemanager.ui.components.CategoryAvatar
import com.example.expancemanager.ui.components.EmptyStateMessage
import com.example.expancemanager.ui.components.ExpenseItemCard
import com.example.expancemanager.ui.components.HSpace
import com.example.expancemanager.ui.components.SectionHeader
import com.example.expancemanager.ui.components.StatPill
import com.example.expancemanager.ui.components.VSpace
import com.example.expancemanager.ui.theme.AppRadius
import com.example.expancemanager.ui.theme.TabularFigures
import com.example.expancemanager.ui.theme.appColors
import com.example.expancemanager.util.DateUtils
import com.example.expancemanager.util.ExpenseCategories
import com.example.expancemanager.viewmodel.SearchUiState
import com.example.expancemanager.viewmodel.SearchViewModel
import kotlinx.coroutines.launch

private object SearchContentType {
    const val SUMMARY = "summary"
    const val EXPENSE_ITEM = "expense_item"
    const val NOTICE = "notice"
}

/**
 * History-wide search. Every other list in the app is scoped to one month; this screen
 * exists for the questions that aren't — "what did the trip cost", "where did that
 * ₹4,000 go", "everything over ₹5,000 this year".
 */
@Composable
internal fun SearchScreen(
    onNavigateBack: () -> Unit,
    onExpenseClick: (Long) -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val queryText by viewModel.queryText.collectAsState()
    var showFilterSheet by remember { mutableStateOf(false) }
    val filter = uiState.filter

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppBackTopBar(
                title = stringResource(R.string.search_title),
                onNavigateBack = onNavigateBack
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            SearchBar(
                query = queryText,
                activeFilterCount = filter.activeFilterCount,
                onQueryChange = viewModel::setQuery,
                onClearQuery = { viewModel.setQuery("") },
                onFilterClick = { showFilterSheet = true }
            )

            when {
                // Nothing typed and no filters: invite a search rather than claiming
                // "no matches" for a query the user never made.
                !filter.isActive && uiState.results.isEmpty() -> {
                    EmptyStateMessage(
                        emoji = stringResource(R.string.search_start_emoji),
                        title = stringResource(R.string.search_start_title),
                        subtitle = stringResource(R.string.search_start_subtitle)
                    )
                }

                uiState.isEmptyResult -> {
                    EmptyStateMessage(
                        emoji = stringResource(R.string.search_empty_emoji),
                        title = stringResource(R.string.search_empty_title),
                        subtitle = stringResource(R.string.search_empty_subtitle)
                    )
                }

                else -> {
                    SearchResults(
                        uiState = uiState,
                        onExpenseClick = onExpenseClick,
                        onDeleteExpense = viewModel::deleteExpense
                    )
                }
            }
        }
    }

    if (showFilterSheet) {
        FilterSheet(
            filter = filter,
            categories = uiState.categories,
            emojiMap = uiState.categoryEmojiMap,
            onDismiss = { showFilterSheet = false },
            onToggleCategory = viewModel::toggleCategory,
            onAmountRangeChange = viewModel::setAmountRange,
            onDateRangeChange = viewModel::setDateRange,
            onSortOrderChange = viewModel::setSortOrder,
            onReset = viewModel::clearFilters
        )
    }
}

@Composable
private fun SearchBar(
    query: String,
    activeFilterCount: Int,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    onFilterClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.screen)
            .padding(bottom = AppSpacing.medium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text(stringResource(R.string.search_field_hint)) },
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = stringResource(R.string.cd_search)
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = onClearQuery) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = stringResource(R.string.cd_clear_search)
                        )
                    }
                }
            },
            singleLine = true,
            maxLines = 1,
            shape = AppRadius.chip,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.Transparent
            )
        )
        HSpace(AppSpacing.small)
        FilterButton(
            activeFilterCount = activeFilterCount,
            onClick = onFilterClick
        )
    }
}

/** Filter entry point; badges the count so active filters are never invisible. */
@Composable
private fun FilterButton(
    activeFilterCount: Int,
    onClick: () -> Unit
) {
    val isActive = activeFilterCount > 0
    Surface(
        shape = AppRadius.chip,
        color = if (isActive) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        modifier = Modifier
            .size(56.dp)
            .clickable(
                role = Role.Button,
                onClickLabel = stringResource(R.string.cd_open_filters),
                onClick = onClick
            )
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (isActive) {
                Text(
                    text = activeFilterCount.toString(),
                    style = MaterialTheme.typography.titleMedium.merge(TabularFigures),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = stringResource(R.string.cd_open_filters),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun SearchResults(
    uiState: SearchUiState,
    onExpenseClick: (Long) -> Unit,
    onDeleteExpense: (Expense) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = AppSpacing.screen,
            end = AppSpacing.screen,
            bottom = AppSpacing.xxlarge
        )
    ) {
        item(key = "summary", contentType = SearchContentType.SUMMARY) {
            ResultSummary(
                count = uiState.resultCount,
                total = uiState.resultTotal
            )
        }

        if (uiState.isTruncated) {
            item(key = "truncated", contentType = SearchContentType.NOTICE) {
                TruncationNotice(limit = uiState.resultCount)
            }
        }

        items(
            items = uiState.results,
            key = { it.id },
            contentType = { SearchContentType.EXPENSE_ITEM }
        ) { expense ->
            ExpenseItemCard(
                expense = expense,
                onExpenseClick = { onExpenseClick(expense.id) },
                onDeleteExpense = { onDeleteExpense(expense) },
                emojiMap = uiState.categoryEmojiMap,
                showCategory = true,
                showDescription = true
            )
        }
    }
}

/**
 * Result count plus the summed amount. The total is the point: "12 results" is
 * trivia, "12 results, ₹18,400" answers what the trip or the category actually cost.
 */
@Composable
private fun ResultSummary(
    count: Int,
    total: Double
) {
    val formattedTotal = remember(total) { DateUtils.formatAmount(total, hideZeroDecimals = true) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AppSpacing.small),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (count == 1) {
                stringResource(R.string.search_result_count_singular)
            } else {
                stringResource(R.string.search_result_count, count)
            },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        StatPill(
            text = stringResource(R.string.search_result_total, formattedTotal),
            containerColor = MaterialTheme.appColors.positiveContainer,
            contentColor = MaterialTheme.appColors.positive
        )
    }
}

/** Says so out loud when the result cap hides matches, rather than implying completeness. */
@Composable
private fun TruncationNotice(limit: Int) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AppSpacing.small),
        shape = AppRadius.chip,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Text(
            text = stringResource(R.string.search_truncated, limit),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(AppSpacing.medium)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterSheet(
    filter: ExpenseFilter,
    categories: List<Category>,
    emojiMap: Map<String, String>,
    onDismiss: () -> Unit,
    onToggleCategory: (String) -> Unit,
    onAmountRangeChange: (Double?, Double?) -> Unit,
    onDateRangeChange: (Long?, Long?) -> Unit,
    onSortOrderChange: (ExpenseSortOrder) -> Unit,
    onReset: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var showDateRangePicker by remember { mutableStateOf(false) }

    // Amount inputs are local until they parse: committing on every keystroke would
    // requery mid-number ("5" then "50" then "500").
    var minText by remember(filter.minAmount) {
        mutableStateOf(filter.minAmount?.let { formatAmountForInput(it) } ?: "")
    }
    var maxText by remember(filter.maxAmount) {
        mutableStateOf(filter.maxAmount?.let { formatAmountForInput(it) } ?: "")
    }
    val minValue = minText.toDoubleOrNull()
    val maxValue = maxText.toDoubleOrNull()
    val isAmountRangeInvalid = minValue != null && maxValue != null && minValue > maxValue

    fun dismiss() {
        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // The facet list is taller than the sheet on most phones, so it scrolls in
            // its own weighted slot. Without this the Column would squeeze the apply
            // button — the last child — down to a few pixels.
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = AppSpacing.screen)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.filter_sheet_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                    TextButton(onClick = onReset) {
                        Text(stringResource(R.string.filter_reset))
                    }
                }

                SectionHeader(title = stringResource(R.string.filter_sort_section))
                SortOptions(
                    selected = filter.sortOrder,
                    onSelect = onSortOrderChange
                )

                SectionHeader(title = stringResource(R.string.filter_category_section))
                CategoryFilterChips(
                    categories = categories,
                    selected = filter.categories,
                    emojiMap = emojiMap,
                    onToggle = onToggleCategory
                )

                SectionHeader(title = stringResource(R.string.filter_amount_section))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.medium)
                ) {
                    AmountBoundField(
                        value = minText,
                        label = stringResource(R.string.filter_amount_min),
                        isError = isAmountRangeInvalid,
                        modifier = Modifier.weight(1f),
                        onValueChange = { text ->
                            minText = text
                            onAmountRangeChange(text.toDoubleOrNull(), maxText.toDoubleOrNull())
                        }
                    )
                    AmountBoundField(
                        value = maxText,
                        label = stringResource(R.string.filter_amount_max),
                        isError = isAmountRangeInvalid,
                        modifier = Modifier.weight(1f),
                        onValueChange = { text ->
                            maxText = text
                            onAmountRangeChange(minText.toDoubleOrNull(), text.toDoubleOrNull())
                        }
                    )
                }
                if (isAmountRangeInvalid) {
                    VSpace(AppSpacing.tiny)
                    Text(
                        text = stringResource(R.string.filter_amount_invalid),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                SectionHeader(title = stringResource(R.string.filter_date_section))
                DateRangeRow(
                    startDate = filter.startDate,
                    endDate = filter.endDate,
                    onClick = { showDateRangePicker = true },
                    onClear = { onDateRangeChange(null, null) }
                )

                VSpace(AppSpacing.large)
            }

            // Pinned outside the scroll area so "Show results" is always reachable.
            Button(
                onClick = { dismiss() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.screen)
                    .padding(bottom = AppSpacing.xxlarge)
                    .heightIn(min = 52.dp),
                shape = AppRadius.pill
            ) {
                Text(
                    text = stringResource(R.string.filter_apply),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }

    if (showDateRangePicker) {
        val pickerState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = filter.startDate
                ?.let { DateUtils.localMillisToUtcPickerDate(it) },
            initialSelectedEndDateMillis = filter.endDate
                ?.let { DateUtils.localMillisToUtcPickerDate(it) }
        )
        DatePickerDialog(
            onDismissRequest = { showDateRangePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    // The picker hands back UTC midnight; convert to local start/end of
                    // day so a range includes every expense logged on its edge dates.
                    onDateRangeChange(
                        pickerState.selectedStartDateMillis
                            ?.let { DateUtils.utcPickerDateToLocalStart(it) },
                        pickerState.selectedEndDateMillis
                            ?.let { DateUtils.utcPickerDateToLocalEnd(it) }
                    )
                    showDateRangePicker = false
                }) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDateRangePicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        ) {
            DateRangePicker(
                state = pickerState,
                title = {
                    Text(
                        text = stringResource(R.string.filter_date_range_title),
                        modifier = Modifier.padding(AppSpacing.default)
                    )
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortOptions(
    selected: ExpenseSortOrder,
    onSelect: (ExpenseSortOrder) -> Unit
) {
    val options = listOf(
        ExpenseSortOrder.DATE_DESC to R.string.filter_sort_date_desc,
        ExpenseSortOrder.DATE_ASC to R.string.filter_sort_date_asc,
        ExpenseSortOrder.AMOUNT_DESC to R.string.filter_sort_amount_desc,
        ExpenseSortOrder.AMOUNT_ASC to R.string.filter_sort_amount_asc
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.small)) {
        options.forEach { (order, labelRes) ->
            FilterChip(
                selected = selected == order,
                onClick = { onSelect(order) },
                label = { Text(stringResource(labelRes)) },
                shape = AppRadius.pill
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryFilterChips(
    categories: List<Category>,
    selected: Set<String>,
    emojiMap: Map<String, String>,
    onToggle: (String) -> Unit
) {
    if (selected.isEmpty()) {
        Text(
            text = stringResource(R.string.filter_category_all),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        VSpace(AppSpacing.small)
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.small)) {
        categories.forEach { category ->
            val isSelected = category.name in selected
            FilterChip(
                selected = isSelected,
                onClick = { onToggle(category.name) },
                label = { Text(category.name) },
                shape = AppRadius.pill,
                leadingIcon = {
                    CategoryAvatar(
                        emoji = ExpenseCategories.getCategoryEmoji(category.name, emojiMap),
                        accent = MaterialTheme.appColors.accentFor(category.name),
                        size = 24.dp,
                        emojiSize = 12.dp
                    )
                },
                colors = FilterChipDefaults.filterChipColors()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AmountBoundField(
    value: String,
    label: String,
    isError: Boolean,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier,
        singleLine = true,
        isError = isError,
        prefix = { Text(stringResource(R.string.currency_symbol)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        shape = AppRadius.chip,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = Color.Transparent
        )
    )
}

@Composable
private fun DateRangeRow(
    startDate: Long?,
    endDate: Long?,
    onClick: () -> Unit,
    onClear: () -> Unit
) {
    val label = remember(startDate, endDate) {
        when {
            startDate != null && endDate != null ->
                "${DateUtils.formatDate(startDate)} – ${DateUtils.formatDate(endDate)}"

            startDate != null -> DateUtils.formatDate(startDate)
            endDate != null -> DateUtils.formatDate(endDate)
            else -> null
        }
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = AppRadius.chip,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.default, vertical = AppSpacing.medium),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label ?: stringResource(R.string.filter_date_any),
                style = MaterialTheme.typography.bodyLarge,
                color = if (label == null) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            if (label == null) {
                Icon(
                    Icons.Default.DateRange,
                    contentDescription = stringResource(R.string.filter_date_pick),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                IconButton(onClick = onClear, modifier = Modifier.size(24.dp)) {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = stringResource(R.string.filter_date_any),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
/**
 * Renders an amount into the text field's expected format: plain digits, and no
 * trailing ".0" for whole rupees (the common case) so the field reads like something
 * the user typed rather than a machine value.
 */
internal fun formatAmountForInput(amount: Double): String =
    if (amount == amount.toLong().toDouble()) {
        amount.toLong().toString()
    } else {
        amount.toString()
    }
