package com.example.expancemanager.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expancemanager.data.Category
import com.example.expancemanager.data.CategoryRepository
import com.example.expancemanager.data.ExpenseRepository
import com.example.expancemanager.util.DateUtils
import com.example.expancemanager.util.PeriodSpendingReport
import com.example.expancemanager.util.ReportInsights
import com.example.expancemanager.util.ReportPeriodKind
import com.example.expancemanager.util.ReportPeriodResolver
import com.example.expancemanager.util.ResolvedReportPeriod
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

internal data class ReportPeriodSelection(
    val kind: ReportPeriodKind = ReportPeriodKind.LAST_SIX_MONTHS,
    val customStartMillis: Long = defaultCustomStartMillis(),
    val customEndMillis: Long = defaultCustomEndMillis()
)

private fun defaultCustomStartMillis(): Long {
    val start = ReportPeriodResolver.shiftMonths(
        ReportPeriodResolver.currentYearMonth(),
        -(ReportPeriodResolver.LAST_SIX_MONTH_COUNT - 1)
    )
    return DateUtils.localMillisToUtcPickerDate(DateUtils.getMonthDateRange(start.month, start.year).first)
}

private fun defaultCustomEndMillis(): Long = DateUtils.localMillisToUtcPickerDate(System.currentTimeMillis())

internal data class ReportsUiState(
    val selection: ReportPeriodSelection = ReportPeriodSelection(),
    val rangeLabel: String = "",
    val periodEndMonth: Int = DateUtils.currentMonthYear().first,
    val periodEndYear: Int = DateUtils.currentMonthYear().second,
    val isRangeInvalid: Boolean = false,
    val report: PeriodSpendingReport = PeriodSpendingReport.Empty,
    val categories: List<Category> = emptyList()
) {
    val categoryEmojiMap: Map<String, String> by lazy { categories.associate { it.name to it.emoji } }
}

@HiltViewModel
internal class ReportsViewModel
    @Inject
    constructor(
        private val expenseRepository: ExpenseRepository,
        private val categoryRepository: CategoryRepository
    ) : ViewModel() {
        private val selectionFlow = MutableStateFlow(ReportPeriodSelection())

        private val _uiState = MutableStateFlow(ReportsUiState())
        internal val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()

        init {
            viewModelScope.launch {
                val reportFlow = selectionFlow.flatMapLatest { selection ->
                    val resolved = ReportPeriodResolver.resolve(
                        kind = selection.kind,
                        customStartMillis = selection.customStartMillis,
                        customEndMillis = selection.customEndMillis
                    )
                    if (resolved == null) {
                        flowOf(
                            ReportsUiState(
                                selection = selection,
                                rangeLabel = "",
                                isRangeInvalid = true
                            )
                        )
                    } else {
                        combine(
                            expenseRepository.getTotalAmountByDateRange(resolved.startMillis, resolved.endMillis),
                            expenseRepository.getCategoryTotalsByDateRange(resolved.startMillis, resolved.endMillis),
                            expenseRepository.getMonthlyTotalsByDateRange(resolved.startMillis, resolved.endMillis)
                        ) { total, categories, monthly ->
                            ReportsUiState(
                                selection = selection,
                                rangeLabel = rangeLabel(selection.kind, resolved),
                                periodEndMonth = resolved.end.month,
                                periodEndYear = resolved.end.year,
                                isRangeInvalid = false,
                                report = ReportInsights.buildPeriodReport(
                                    totalSpending = total ?: 0.0,
                                    monthlyTotals = monthly,
                                    categoryTotals = categories,
                                    monthsInRange = resolved.months
                                )
                            )
                        }
                    }
                }

                combine(reportFlow, categoryRepository.getCategories()) { state, categories ->
                    state.copy(categories = categories)
                }.collect { _uiState.value = it }
            }
        }

        internal fun selectPeriod(kind: ReportPeriodKind) {
            selectionFlow.update { it.copy(kind = kind) }
        }

        internal fun setCustomDateRange(
            startMillis: Long,
            endMillis: Long
        ) {
            selectionFlow.update {
                it.copy(
                    kind = ReportPeriodKind.CUSTOM,
                    customStartMillis = startMillis,
                    customEndMillis = endMillis
                )
            }
        }

        private fun rangeLabel(
            kind: ReportPeriodKind,
            resolved: ResolvedReportPeriod
        ): String {
            if (kind == ReportPeriodKind.CUSTOM) {
                return "${DateUtils.formatDate(resolved.startMillis)} – ${DateUtils.formatDate(resolved.endMillis)}"
            }
            val startLabel = DateUtils.formatMonthYear(resolved.start.month, resolved.start.year)
            val endLabel = DateUtils.formatMonthYear(resolved.end.month, resolved.end.year)
            return if (resolved.start == resolved.end) startLabel else "$startLabel – $endLabel"
        }
    }
