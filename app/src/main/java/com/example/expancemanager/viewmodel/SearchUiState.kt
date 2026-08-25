package com.example.expancemanager.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expancemanager.data.Category
import com.example.expancemanager.data.CategoryRepository
import com.example.expancemanager.data.Expense
import com.example.expancemanager.data.ExpenseFilter
import com.example.expancemanager.data.ExpenseRepository
import com.example.expancemanager.data.ExpenseSortOrder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
internal data class SearchUiState(
    val filter: ExpenseFilter = ExpenseFilter(),
    val results: List<Expense> = emptyList(),
    /** Sum of [results]; the answer to "what did this slice cost me". */
    val resultTotal: Double = 0.0,
    /** True once the result list has hit the repository cap and is not the full answer. */
    val isTruncated: Boolean = false,
    val categories: List<Category> = emptyList(),
    /** True before the first query settles, so the UI can hold off on the empty state. */
    val isLoading: Boolean = true
) {
    val categoryEmojiMap: Map<String, String> by lazy { categories.associate { it.name to it.emoji } }

    val resultCount: Int get() = results.size

    /** Distinct from "no expenses at all" — this is a filter that matched nothing. */
    val isEmptyResult: Boolean get() = !isLoading && results.isEmpty()
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
internal class SearchViewModel
    @Inject
    constructor(
        private val expenseRepository: ExpenseRepository,
        categoryRepository: CategoryRepository
    ) : ViewModel() {
        /**
         * The live filter. Text edits and control changes share one flow so a single
         * downstream query serves both; only the text needs debouncing.
         */
        private val filterFlow = MutableStateFlow(ExpenseFilter())

        /**
         * What the text field displays. Kept separate from [filterFlow] so typing echoes
         * instantly while the query itself waits out the debounce.
         */
        private val _queryText = MutableStateFlow("")
        internal val queryText: StateFlow<String> = _queryText.asStateFlow()

        internal val uiState: StateFlow<SearchUiState> =
            combine(
                filterFlow
                    // Only the free-text field is debounced; toggling a category or sort
                    // should feel immediate, and those arrive on this same flow.
                    .debounce { filter -> if (filter.query.isBlank()) 0L else QUERY_DEBOUNCE_MS }
                    .distinctUntilChanged()
                    .flatMapLatest { filter ->
                        expenseRepository.searchExpenses(filter).map { results -> filter to results }
                    },
                categoryRepository.getCategories()
            ) { (filter, results), categories ->
                SearchUiState(
                    filter = filter,
                    results = results,
                    resultTotal = results.sumOf { it.amount },
                    isTruncated = results.size >= ExpenseRepository.DEFAULT_SEARCH_LIMIT,
                    categories = categories,
                    isLoading = false
                )
            }
                .distinctUntilChanged()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState())

        internal fun setQuery(query: String) {
            _queryText.value = query
            filterFlow.update { it.copy(query = query) }
        }

        /** Adds or removes [category] from the category filter. */
        internal fun toggleCategory(category: String) {
            filterFlow.update { filter ->
                val next = if (category in filter.categories) {
                    filter.categories - category
                } else {
                    filter.categories + category
                }
                filter.copy(categories = next)
            }
        }

        internal fun setAmountRange(
            min: Double?,
            max: Double?
        ) {
            filterFlow.update { it.copy(minAmount = min, maxAmount = max) }
        }

        internal fun setDateRange(
            start: Long?,
            end: Long?
        ) {
            filterFlow.update { it.copy(startDate = start, endDate = end) }
        }

        internal fun setSortOrder(sortOrder: ExpenseSortOrder) {
            filterFlow.update { it.copy(sortOrder = sortOrder) }
        }

        /** Clears every filter facet but keeps the typed query and the chosen sort. */
        internal fun clearFilters() {
            filterFlow.update { filter ->
                ExpenseFilter(query = filter.query, sortOrder = filter.sortOrder)
            }
        }

        /** Resets the screen to its opening state, text field included. */
        internal fun clearAll() {
            _queryText.value = ""
            filterFlow.value = ExpenseFilter()
        }

        internal fun deleteExpense(expense: Expense) {
            viewModelScope.launch(Dispatchers.IO) {
                expenseRepository.deleteExpense(expense)
            }
        }

        private companion object {
            /** Long enough to skip intermediate keystrokes, short enough to feel live. */
            const val QUERY_DEBOUNCE_MS = 250L
        }
    }
