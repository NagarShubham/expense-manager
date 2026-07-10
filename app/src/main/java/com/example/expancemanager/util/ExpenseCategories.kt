package com.example.expancemanager.util

object ExpenseCategories {
    const val FALLBACK_EMOJI = "💰"

    /**
     * Canonical default categories: English name + emoji. This is the single
     * source of truth used to seed the `categories` table (see the DB migration
     * and first-run seeding) and as an emoji fallback when a category has no row yet.
     *
     * The names MUST match the original English values so that expenses created
     * before categories became editable still resolve.
     */
    val DEFAULT_CATEGORIES: List<Pair<String, String>> = listOf(
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

    /** Emoji lookup by name from the default set, for the resource/index fallback path. */
    private val defaultEmojiByName: Map<String, String> = DEFAULT_CATEGORIES.toMap()

    /**
     * Returns the emoji for [category], preferring a user-managed [emojiMap] (name -> emoji
     * from the categories table) and falling back to the built-in default set, then
     * [FALLBACK_EMOJI]. Safe to call before the DB has loaded (empty map).
     */
    fun getCategoryEmoji(
        category: String,
        emojiMap: Map<String, String>
    ): String = emojiMap[category] ?: defaultEmojiByName[category] ?: FALLBACK_EMOJI
}
