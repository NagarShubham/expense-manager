package com.example.expancemanager.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expancemanager.data.BudgetRepository
import com.example.expancemanager.data.MonthlyBudget
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
internal class BudgetViewModel
    @Inject
    constructor(
        private val budgetRepository: BudgetRepository
    ) : ViewModel() {
        internal suspend fun getExpectedBudgetForMonth(
            month: Int,
            year: Int
        ): Double? =
            withContext(Dispatchers.IO) {
                budgetRepository.getBudgetByMonthYearOnce(month, year)?.expectedAmount
            }

        internal suspend fun setMonthlyBudgetAndWait(
            month: Int,
            year: Int,
            expectedAmount: Double
        ) = insertBudget(month, year, expectedAmount)

        internal fun clearMonthlyBudget(
            month: Int,
            year: Int
        ) {
            viewModelScope.launch(Dispatchers.IO) {
                budgetRepository.deleteBudgetByMonthYear(month, year)
            }
        }

        internal fun setCategoryExcludedFromBudget(
            month: Int,
            year: Int,
            category: String,
            excluded: Boolean
        ) {
            viewModelScope.launch(Dispatchers.IO) {
                budgetRepository.setCategoryExcluded(month, year, category, excluded)
            }
        }

        internal fun getExcludedByMonthYear(
            month: Int,
            year: Int
        ): Flow<List<String>> = budgetRepository.getExcludedCategoriesByMonthYear(month, year)

        private suspend fun insertBudget(
            month: Int,
            year: Int,
            expectedAmount: Double
        ) = withContext(Dispatchers.IO) {
            budgetRepository.insertOrUpdateBudget(
                MonthlyBudget(month = month, year = year, expectedAmount = expectedAmount)
            )
        }
    }
