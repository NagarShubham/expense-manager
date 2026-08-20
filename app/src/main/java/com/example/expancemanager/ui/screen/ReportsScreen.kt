package com.example.expancemanager.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.expancemanager.R
import com.example.expancemanager.data.CategoryTotal
import com.example.expancemanager.data.MonthlyTotal
import com.example.expancemanager.ui.components.AppBackTopBar
import com.example.expancemanager.ui.components.CategoryTotalCard
import com.example.expancemanager.ui.components.EmptyStateMessage
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
        topBar = {
            AppBackTopBar(
                title = stringResource(R.string.reports_title),
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
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(
                                horizontal = dimensionResource(R.dimen.spacing_default),
                                vertical = dimensionResource(R.dimen.spacing_small)
                            ),
                        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_default))
                    ) {
                        if (uiState.rangeLabel.isNotBlank()) {
                            Text(
                                text = uiState.rangeLabel,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        TotalSpendingCard(
                            total = report.totalSpending,
                            monthlyAverage = report.monthlyAverage
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))
                        ) {
                            ExtremeMonthCard(
                                title = stringResource(R.string.reports_highest_month),
                                month = report.highestMonth,
                                amountColor = MaterialTheme.colorScheme.error,
                                modifier = Modifier.weight(1f)
                            )
                            ExtremeMonthCard(
                                title = stringResource(R.string.reports_lowest_month),
                                month = report.lowestMonth,
                                amountColor = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        CategoryBreakdownCard(
                            totals = report.categoryTotals,
                            periodTotal = report.totalSpending,
                            emojiMap = uiState.categoryEmojiMap
                        )
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

@Composable
private fun PeriodSelectorRow(
    selected: ReportPeriodKind,
    onSelect: (ReportPeriodKind) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(
                horizontal = dimensionResource(R.dimen.spacing_default),
                vertical = dimensionResource(R.dimen.spacing_small)
            ),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))
    ) {
        REPORT_PERIOD_CHIPS.forEach { (kind, labelRes) ->
            FilterChip(
                selected = selected == kind,
                onClick = { onSelect(kind) },
                label = { Text(stringResource(labelRes)) }
            )
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
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimensionResource(R.dimen.spacing_default))
            .clickable(role = Role.Button, onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.spacing_default)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_medium))
        ) {
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = stringResource(R.string.reports_custom_pick),
                tint = MaterialTheme.colorScheme.primary
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.reports_custom_hint),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$startLabel – $endLabel",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
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
                    modifier = Modifier.padding(dimensionResource(R.dimen.spacing_default))
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.spacing_xlarge))
        ) {
            Text(
                text = stringResource(R.string.reports_total_spending),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_tiny)))
            Text(
                text = formattedTotal,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_medium)))
            Text(
                text = stringResource(R.string.reports_monthly_average),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
            )
            Text(
                text = formattedAverage,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun ExtremeMonthCard(
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
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(dimensionResource(R.dimen.spacing_default))) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_small)))
            if (month == null) {
                Text(
                    text = stringResource(R.string.reports_stat_unavailable),
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Text(
                    text = formattedMonth.orEmpty(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = formattedAmount.orEmpty(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )
            }
        }
    }
}

@Composable
private fun CategoryBreakdownCard(
    totals: List<CategoryTotal>,
    periodTotal: Double,
    emojiMap: Map<String, String>,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.reports_top_categories),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = dimensionResource(R.dimen.spacing_small))
        )
        totals.forEach { item ->
            CategoryTotalCard(
                categoryTotal = item,
                totalAmount = periodTotal,
                emojiMap = emojiMap,
            )
        }
    }
}
