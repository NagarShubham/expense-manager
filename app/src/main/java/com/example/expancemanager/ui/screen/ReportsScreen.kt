package com.example.expancemanager.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.expancemanager.R
import com.example.expancemanager.data.MonthlyTotal
import com.example.expancemanager.ui.components.AmountText
import com.example.expancemanager.ui.components.AppBackTopBar
import com.example.expancemanager.ui.components.AppCard
import com.example.expancemanager.ui.components.AppSpacing
import com.example.expancemanager.ui.components.CategoryTotalCard
import com.example.expancemanager.ui.components.EmptyStateMessage
import com.example.expancemanager.ui.components.HSpace
import com.example.expancemanager.ui.components.HeroAmountText
import com.example.expancemanager.ui.components.HeroGradientCard
import com.example.expancemanager.ui.components.MetricTile
import com.example.expancemanager.ui.components.OverlineText
import com.example.expancemanager.ui.components.SectionHeader
import com.example.expancemanager.ui.components.VSpace
import com.example.expancemanager.ui.theme.AppRadius
import com.example.expancemanager.ui.theme.appColors
import com.example.expancemanager.util.DateUtils
import com.example.expancemanager.util.ReportPeriodKind
import com.example.expancemanager.viewmodel.ReportsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReportsScreen(
    onNavigateBack: () -> Unit,
    viewModel: ReportsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val selection = uiState.selection
    val report = uiState.report
    var showDateRangePicker by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppBackTopBar(
                title = stringResource(R.string.reports_title),
                subtitle = uiState.rangeLabel.takeIf { it.isNotBlank() },
                onNavigateBack = onNavigateBack
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            PeriodSelectorRow(
                selected = selection.kind,
                onSelect = { kind ->
                    viewModel.selectPeriod(kind)
                    if (kind == ReportPeriodKind.CUSTOM) {
                        showDateRangePicker = true
                    }
                }
            )

            if (selection.kind == ReportPeriodKind.CUSTOM) {
                CustomRangeCalendarButton(
                    startMillis = selection.customStartMillis,
                    endMillis = selection.customEndMillis,
                    onClick = { showDateRangePicker = true }
                )
            }

            if (showDateRangePicker) {
                CustomDateRangePickerDialog(
                    initialStartMillis = selection.customStartMillis,
                    initialEndMillis = selection.customEndMillis,
                    onDismiss = { showDateRangePicker = false },
                    onConfirm = { start, end ->
                        viewModel.setCustomDateRange(start, end)
                        showDateRangePicker = false
                    }
                )
            }

            when {
                uiState.isRangeInvalid -> {
                    EmptyStateMessage(
                        emoji = stringResource(R.string.reports_invalid_emoji),
                        title = stringResource(R.string.reports_invalid_title),
                        subtitle = stringResource(R.string.reports_invalid_subtitle)
                    )
                }

                !report.hasExpenses -> {
                    EmptyStateMessage(
                        emoji = stringResource(R.string.reports_empty_emoji),
                        title = stringResource(R.string.reports_empty_title),
                        subtitle = stringResource(R.string.reports_empty_subtitle)
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = AppSpacing.screen),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            bottom = AppSpacing.xlarge
                        ),
                        verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)
                    ) {
                        item(key = "total_card") {
                            TotalSpendingCard(
                                total = report.totalSpending,
                                monthlyAverage = report.monthlyAverage
                            )
                        }

                        item(key = "extreme_months") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(AppSpacing.medium)
                            ) {
                                ExtremeMonthTile(
                                    title = stringResource(R.string.reports_highest_month),
                                    month = report.highestMonth,
                                    amountColor = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.weight(1f)
                                )
                                ExtremeMonthTile(
                                    title = stringResource(R.string.reports_lowest_month),
                                    month = report.lowestMonth,
                                    amountColor = MaterialTheme.appColors.positive,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        item(key = "category_header") {
                            SectionHeader(title = stringResource(R.string.reports_top_categories))
                        }

                        items(
                            items = report.categoryTotals,
                            key = { it.category }
                        ) { item ->
                            CategoryTotalCard(
                                categoryTotal = item,
                                totalAmount = report.totalSpending,
                                emojiMap = uiState.categoryEmojiMap
                            )
                        }
                    }
                }
            }
        }
    }
}

private val REPORT_PERIOD_CHIPS = listOf(
    ReportPeriodKind.LAST_SIX_MONTHS to R.string.reports_period_last_six,
    ReportPeriodKind.THIS_YEAR to R.string.reports_period_this_year,
    ReportPeriodKind.CUSTOM to R.string.reports_period_custom
)

/**
 * Period filter as a row of pill toggles rather than Material FilterChips — the
 * selected pill is a solid brand fill, which reads faster than a checkmark.
 */
@Composable
private fun PeriodSelectorRow(
    selected: ReportPeriodKind,
    onSelect: (ReportPeriodKind) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = AppSpacing.screen, vertical = AppSpacing.small),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.small)
    ) {
        REPORT_PERIOD_CHIPS.forEach { (kind, labelRes) ->
            val isSelected = selected == kind
            val label = stringResource(labelRes)
            Surface(
                shape = AppRadius.pill,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
                modifier = Modifier.clickable(
                    role = Role.RadioButton,
                    onClickLabel = label,
                    onClick = { onSelect(kind) }
                )
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.padding(
                        horizontal = AppSpacing.default,
                        vertical = AppSpacing.small + 2.dp
                    )
                )
            }
        }
    }
}

@Composable
private fun CustomRangeCalendarButton(
    startMillis: Long,
    endMillis: Long,
    onClick: () -> Unit
) {
    val startLabel = remember(startMillis) {
        DateUtils.formatDate(DateUtils.utcPickerDateToLocalStart(startMillis))
    }
    val endLabel = remember(endMillis) {
        DateUtils.formatDate(DateUtils.utcPickerDateToLocalEnd(endMillis))
    }
    AppCard(
        modifier = Modifier
            .padding(horizontal = AppSpacing.screen)
            .padding(bottom = AppSpacing.small),
        onClick = onClick,
        contentPadding = AppSpacing.default
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = AppRadius.icon,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = stringResource(R.string.reports_custom_pick),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(AppSpacing.small)
                )
            }
            HSpace(AppSpacing.medium)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.reports_custom_hint),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$startLabel – $endLabel",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomDateRangePickerDialog(
    initialStartMillis: Long,
    initialEndMillis: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long, Long) -> Unit
) {
    val pickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = initialStartMillis,
        initialSelectedEndDateMillis = initialEndMillis
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            val start = pickerState.selectedStartDateMillis
            val end = pickerState.selectedEndDateMillis
            TextButton(
                enabled = start != null && end != null && start <= end,
                onClick = {
                    if (start != null && end != null) {
                        onConfirm(start, end)
                    }
                }
            ) {
                Text(stringResource(R.string.action_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    ) {
        DateRangePicker(
            state = pickerState,
            modifier = Modifier.height(520.dp),
            title = {
                Text(
                    text = stringResource(R.string.reports_date_range_title),
                    modifier = Modifier.padding(AppSpacing.default)
                )
            },
            showModeToggle = false
        )
    }
}

@Composable
private fun TotalSpendingCard(
    total: Double,
    monthlyAverage: Double
) {
    val formattedTotal = remember(total) { DateUtils.formatAmount(total) }
    val formattedAverage = remember(monthlyAverage) { DateUtils.formatAmount(monthlyAverage) }
    val appColors = MaterialTheme.appColors

    HeroGradientCard {
        OverlineText(
            text = stringResource(R.string.reports_total_spending),
            color = appColors.onHeroMuted
        )
        VSpace(AppSpacing.small)
        HeroAmountText(
            text = formattedTotal,
            style = MaterialTheme.typography.displaySmall,
            color = appColors.onHero
        )
        VSpace(AppSpacing.large)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(AppRadius.chip)
                .background(appColors.onHero.copy(alpha = 0.14f))
                .padding(horizontal = AppSpacing.medium, vertical = AppSpacing.small),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.reports_monthly_average),
                style = MaterialTheme.typography.labelMedium,
                color = appColors.onHeroMuted
            )
            AmountText(
                text = formattedAverage,
                style = MaterialTheme.typography.titleMedium,
                color = appColors.onHero
            )
        }
    }
}

@Composable
private fun ExtremeMonthTile(
    title: String,
    month: MonthlyTotal?,
    amountColor: Color,
    modifier: Modifier = Modifier
) {
    val formattedMonth = remember(month?.month, month?.year) {
        month?.let { DateUtils.formatMonthYear(it.month, it.year) }
    }
    val formattedAmount = remember(month?.total) {
        month?.let { DateUtils.formatAmount(it.total) }
    }
    MetricTile(
        label = title,
        value = formattedAmount ?: stringResource(R.string.reports_stat_unavailable),
        valueColor = if (month == null) MaterialTheme.colorScheme.onSurfaceVariant else amountColor,
        footnote = formattedMonth,
        modifier = modifier
    )
}
