package com.example.expancemanager.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expancemanager.data.BudgetRepository
import com.example.expancemanager.data.Category
import com.example.expancemanager.data.CategoryRepository
import com.example.expancemanager.data.CategoryTotal
import com.example.expancemanager.data.Expense
import com.example.expancemanager.data.ExpenseRepository
import com.example.expancemanager.nav.AppRoute
import com.example.expancemanager.nav.HomeScreenRoute
import com.example.expancemanager.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal data class ExpenseUiState(
    val expenses: List<Expense> = emptyList(),
    val totalAmount: Double = 0.0,
    val categoryTotals: List<CategoryTotal> = emptyList(),
    val selectedMonth: Int = DateUtils.currentMonthYear().first,
    val selectedYear: Int = DateUtils.currentMonthYear().second,
    /** Expected monthly expense (budget) for the selected month; null if not set */
    val expectedMonthlyAmount: Double? = null,
    /** Amount that counts toward budget (total minus excluded categories). Used for Used/Remaining/Progress. */
    val totalAmountForBudget: Double = 0.0,
    /** Category names excluded from monthly budget; expenses in these still show but don't count toward budget. */
    val excludedCategoryNames: Set<String> = emptySet(),
    /** User-managed categories, ordered. Used to source names and emojis across the UI. */
    val categories: List<Category> = emptyList()
) {
    /**
     * Convenience map of category name -> emoji for lookups via [ExpenseCategories.getCategoryEmoji].
     * Computed once per state instance (lazy) so it keeps a stable identity across reads; this lets
     * Compose skip recomposition and honor `remember(..., emojiMap)` keys in the UI instead of
     * rebuilding a fresh map on every access.
     */
    val categoryEmojiMap: Map<String, String> by lazy { categories.associate { it.name to it.emoji } }
}

@HiltViewModel
internal class ExpenseViewModel
    @Inject
    constructor(
        private val expenseRepository: ExpenseRepository,
        private val budgetRepository: BudgetRepository,
        private val categoryRepository: CategoryRepository
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ExpenseUiState())
        internal val uiState: StateFlow<ExpenseUiState> = _uiState.asStateFlow()

        /** Single source of truth for which month/year to load; one collector reacts to this. */
        private val selectedMonthYearFlow = MutableStateFlow(DateUtils.currentMonthYear())

        internal val backStack = mutableStateListOf<AppRoute>(HomeScreenRoute)

        /** Pushes a route onto the back stack. */
        internal fun navigateTo(route: AppRoute) {
            backStack.add(route)
        }

        /** Pops the current route; no-op if only the root (e.g. Home) remains. */
        internal fun navigateBack() {
            if (backStack.size > 1) {
                backStack.removeAt(backStack.lastIndex)
            }
        }

        init {
            viewModelScope.launch {
                val monthlyStateFlow =
                    selectedMonthYearFlow.flatMapLatest { (month, year) ->
                        val (startDate, endDate) = DateUtils.getMonthDateRange(month, year)
                        combine(
                            expenseRepository.getExpensesByDateRange(startDate, endDate),
                            expenseRepository.getTotalAmountByDateRange(startDate, endDate),
                            expenseRepository.getCategoryTotalsByDateRange(startDate, endDate),
                            budgetRepository.getBudgetByMonthYear(month, year),
                            budgetRepository.getExcludedCategoriesByMonthYear(month, year)
                        ) { expenses, total, categoryTotals, budget, excludedList ->
                            val excluded = excludedList.toSet()
                            val totalAmount = total ?: 0.0
                            // Sum over the already-aggregated per-category totals (from SQL GROUP BY)
                            // rather than re-scanning every expense row.
                            val excludedSum = categoryTotals.filter { it.category in excluded }.sumOf { it.total }
                            val totalAmountForBudget = (totalAmount - excludedSum).coerceAtLeast(0.0)
                            ExpenseUiState(
                                expenses = expenses,
                                totalAmount = totalAmount,
                                categoryTotals = categoryTotals,
                                selectedMonth = month,
                                selectedYear = year,
                                expectedMonthlyAmount = budget?.expectedAmount,
                                totalAmountForBudget = totalAmountForBudget,
                                excludedCategoryNames = excluded
                            )
                        }
                    }

                // Categories are month-independent; merge them in so every screen sees the
                // latest names/emojis regardless of the selected month.
                combine(monthlyStateFlow, categoryRepository.getCategories()) { state, categories ->
                    state.copy(categories = categories)
                }.collect { _uiState.value = it }
            }
        }

        internal fun loadExpensesForMonth(
            month: Int,
            year: Int
        ) {
            selectedMonthYearFlow.value = month to year
        }

        internal fun insertExpense(expense: Expense) {
            viewModelScope.launch(Dispatchers.IO) {
                expenseRepository.insertExpense(expense)
            }
        }

        internal fun updateExpense(expense: Expense) {
            viewModelScope.launch(Dispatchers.IO) {
                expenseRepository.updateExpense(expense)
            }
        }

        internal fun deleteExpense(expense: Expense) {
            viewModelScope.launch(Dispatchers.IO) {
                expenseRepository.deleteExpense(expense)
            }
        }

        internal suspend fun getExpenseById(id: Long): Expense? =
            withContext(Dispatchers.IO) {
                expenseRepository.getExpenseById(id)
            }

        internal fun changeMonth(increment: Int) {
            val (month, year) = selectedMonthYearFlow.value
            val (newMonth, newYear) = DateUtils.adjacentMonth(month, year, increment)
            loadExpensesForMonth(newMonth, newYear)
        }

        internal fun goToCurrentMonth() {
            val (month, year) = DateUtils.currentMonthYear()
            loadExpensesForMonth(month, year)
        }
    }
