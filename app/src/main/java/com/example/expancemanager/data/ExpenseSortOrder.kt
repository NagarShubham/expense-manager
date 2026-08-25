package com.example.expancemanager.data

import androidx.compose.runtime.Immutable

/** Sort options for search results. */
internal enum class ExpenseSortOrder {
    DATE_DESC,
    DATE_ASC,
    AMOUNT_DESC,
    AMOUNT_ASC
}

/**
 * A search/filter specification over the whole expense history.
 *
 * Unlike the rest of the app, which is strictly month-scoped, this deliberately spans
 * every month: the questions it answers ("what did that trip cost", "every expense over
 * ₹5,000") don't respect month boundaries. All fields are optional — [isActive] reports
 * whether anything beyond the default sort has been set, which the UI uses to decide
 * whether to show a "clear filters" affordance.
 */
@Immutable
internal data class ExpenseFilter(
    /** Matched case-insensitively against title and description; blank means "no text filter". */
    val query: String = "",
    /** Empty means all categories. */
    val categories: Set<String> = emptySet(),
    val minAmount: Double? = null,
    val maxAmount: Double? = null,
    /** Inclusive local-time bounds; null means unbounded on that side. */
    val startDate: Long? = null,
    val endDate: Long? = null,
    val sortOrder: ExpenseSortOrder = ExpenseSortOrder.DATE_DESC
) {
    /** True when any constraint beyond the default sort is set. */
    val isActive: Boolean
        get() = query.isNotBlank() ||
            categories.isNotEmpty() ||
            minAmount != null ||
            maxAmount != null ||
            startDate != null ||
            endDate != null

    /** True when any constraint other than the free-text query is set. */
    val hasNonQueryFilters: Boolean
        get() = categories.isNotEmpty() ||
            minAmount != null ||
            maxAmount != null ||
            startDate != null ||
            endDate != null

    /**
     * Number of distinct filter facets in use, shown as a badge on the filter button.
     * The category set counts once no matter how many are selected, and a min/max pair
     * counts as a single "amount" facet, so the badge tracks facets rather than values.
     */
    val activeFilterCount: Int
        get() = listOf(
            categories.isNotEmpty(),
            minAmount != null || maxAmount != null,
            startDate != null || endDate != null
        ).count { it }
}
