package com.example.expancemanager.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expancemanager.data.BudgetRepository
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
import java.util.Calendar
import javax.inject.Inject

data class ExpenseUiState(
    val expenses: List<Expense> = emptyList(),
    val totalAmount: Double = 0.0,
    val categoryTotals: List<CategoryTotal> = emptyList(),
    val selectedMonth: Int = Calendar.getInstance().get(Calendar.MONTH),
    val selectedYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    /** Expected monthly expense (budget) for the selected month; null if not set */
    val expectedMonthlyAmount: Double? = null,
    /** Amount that counts toward budget (total minus excluded categories). Used for Used/Remaining/Progress. */
    val totalAmountForBudget: Double = 0.0,
    /** Category names excluded from monthly budget; expenses in these still show but don't count toward budget. */
    val excludedCategoryNames: Set<String> = emptySet()
)

@HiltViewModel
class ExpenseViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val budgetRepository: BudgetRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ExpenseUiState())
    val uiState: StateFlow<ExpenseUiState> = _uiState.asStateFlow()

    /** Single source of truth for which month/year to load; one collector reacts to this. */
    private val selectedMonthYearFlow = MutableStateFlow(
        Calendar.getInstance().run { get(Calendar.MONTH) to get(Calendar.YEAR) }
    )

    val backStack = mutableStateListOf<AppRoute>(HomeScreenRoute)

    /** Pushes a route onto the back stack. */
    fun navigateTo(route: AppRoute) {
        backStack.add(route)
    }

    /** Pops the current route; no-op if only the root (e.g. Home) remains. */
    fun navigateBack() {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
        }
    }

    init {
        viewModelScope.launch {
            selectedMonthYearFlow
                .flatMapLatest { (month, year) ->
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
                        val excludedSum = expenses.filter { it.category in excluded }.sumOf { it.amount }
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
                }.collect { _uiState.value = it }
        }
    }

    fun loadExpensesForMonth(
        month: Int,
        year: Int
    ) {
        selectedMonthYearFlow.value = month to year
    }

    fun insertExpense(expense: Expense) {
        viewModelScope.launch(Dispatchers.IO) {
            expenseRepository.insertExpense(expense)
        }
    }

    fun updateExpense(expense: Expense) {
        viewModelScope.launch(Dispatchers.IO) {
            expenseRepository.updateExpense(expense)
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch(Dispatchers.IO) {
            expenseRepository.deleteExpense(expense)
        }
    }

    suspend fun getExpenseById(id: Long): Expense? =
        withContext(Dispatchers.IO) {
            expenseRepository.getExpenseById(id)
        }

    fun changeMonth(increment: Int) {
        val (month, year) = selectedMonthYearFlow.value
        val (newMonth, newYear) = DateUtils.adjacentMonth(month, year, increment)
        loadExpensesForMonth(newMonth, newYear)
    }

    fun goToCurrentMonth() {
        val calendar = Calendar.getInstance()
        loadExpensesForMonth(
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.YEAR)
        )
    }
}
