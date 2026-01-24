package com.example.expancemanager.util

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.example.expancemanager.R

object ExpenseCategories {
    private val categoryEmojiMap = mapOf(
        "Bills & Utilities" to "💡",
        "Transportation" to "🚗",
        "Food & Dining" to "🍔",
        "Personal Care" to "💆",
        "EMI" to "💳",
        "Baby" to "👶",
        "Groceries" to "🛒",
        "Investments" to "📈",
        "Travel" to "✈️",
        "Shopping" to "🛍️",
        "Entertainment" to "🎬",
        "Healthcare" to "🏥",
        "Education" to "📚",
        "Rent" to "🏠",
        "Insurance" to "🛡️",
        "Gifts" to "🎁",
        "Other" to "💰"
    )

    fun getCategories(context: Context): List<String> = context.resources.getStringArray(R.array.expense_categories).toList()

    /**
     * Get the emoji icon for a category
     * Emojis are universal and don't need localization
     */
    fun getCategoryEmoji(category: String): String {
        categoryEmojiMap[category]?.let { return it }
        return categoryEmojiMap.values.elementAtOrNull(
            categoryEmojiMap.keys.indexOf(category)
        ) ?: "💰"
    }
}

@Composable
fun rememberCategories(): List<String> {
    val context = LocalContext.current
    return ExpenseCategories.getCategories(context)
}
