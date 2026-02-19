package com.example.expancemanager.util

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.example.expancemanager.R

object ExpenseCategories {
    /**
     * Emojis in the same order as [R.array.expense_categories], so lookup by index
     * works correctly for any locale (categories are stored as localized strings).
     */
    private val categoryEmojisByIndex = listOf(
        "💡", // Bills & Utilities
        "🚗", // Transportation
        "🍔", // Food & Dining
        "💆", // Personal Care
        "💳", // EMI
        "👶", // Baby
        "🛒", // Groceries
        "📈", // Investments
        "✈️", // Travel
        "🛍️", // Shopping
        "🎬", // Entertainment
        "🏥", // Healthcare
        "📚", // Education
        "🏠", // Rent
        "🛡️", // Insurance
        "🎁", // Gifts
        "💰" // Other
    )

    private const val FALLBACK_EMOJI = "💰"

    fun getCategories(context: Context): List<String> = context.resources.getStringArray(R.array.expense_categories).toList()

    /**
     * Returns the emoji for the given category name.
     * Uses the same order as [getCategories] so it works for any locale.
     */
    fun getCategoryEmoji(
        context: Context,
        category: String
    ): String {
        val categories = getCategories(context)
        val index = categories.indexOf(category)
        return if (index in categoryEmojisByIndex.indices) categoryEmojisByIndex[index] else FALLBACK_EMOJI
    }
}

@Composable
fun rememberCategories(): List<String> {
    val context = LocalContext.current
    return ExpenseCategories.getCategories(context)
}
