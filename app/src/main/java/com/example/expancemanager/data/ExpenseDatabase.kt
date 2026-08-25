package com.example.expancemanager.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.expancemanager.util.ExpenseCategories
import com.example.expancemanager.util.SecureKeyGenerator
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

/**
 * Encrypted Room database using SQLCipher
 * All data is encrypted at rest using 256-bit AES encryption
 * Optimized with indexes for better query performance
 */
@Database(
    entities = [Expense::class, MonthlyBudget::class, BudgetExcludedCategory::class, Category::class],
    version = 7,
    exportSchema = false
)
internal abstract class ExpenseDatabase : RoomDatabase() {
    internal abstract fun expenseDao(): ExpenseDao

    internal abstract fun monthlyBudgetDao(): MonthlyBudgetDao

    internal abstract fun budgetExcludedCategoryDao(): BudgetExcludedCategoryDao

    internal abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile
        private var INSTANCE: ExpenseDatabase? = null

        // Load SQLCipher native library
        init {
            System.loadLibrary("sqlcipher")
        }

        /**
         * Migration from version 1 to 2: Add indexes for performance
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add indexes to improve query performance
                db.execSQL("CREATE INDEX IF NOT EXISTS index_expenses_date ON expenses(date)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_expenses_category ON expenses(category)")
            }
        }

        /**
         * Migration from version 2 to 3: Add monthly_budgets table for expected monthly expense
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS monthly_budgets (
                        month INTEGER NOT NULL,
                        year INTEGER NOT NULL,
                        expectedAmount REAL NOT NULL,
                        PRIMARY KEY(month, year)
                    )
                    """.trimIndent()
                )
            }
        }

        /**
         * Migration from version 3 to 4: Add budget_excluded_categories for categories excluded from budget
         */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS budget_excluded_categories (
                        category TEXT NOT NULL PRIMARY KEY
                    )
                    """.trimIndent()
                )
            }
        }

        /**
         * Migration from version 4 to 5: Excluded categories per month/year (month, year, category)
         */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS budget_excluded_categories_new (
                        month INTEGER NOT NULL,
                        year INTEGER NOT NULL,
                        category TEXT NOT NULL,
                        PRIMARY KEY(month, year, category)
                    )
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE IF EXISTS budget_excluded_categories")
                db.execSQL("ALTER TABLE budget_excluded_categories_new RENAME TO budget_excluded_categories")
            }
        }

        /**
         * Migration from version 5 to 6: Add the user-manageable `categories` table and
         * seed it with the built-in defaults. Only creates + seeds a new table; the
         * `expenses` and `budget_excluded_categories` tables are left untouched, so no
         * existing data is altered. Seeded names match the English defaults so existing
         * expenses keep resolving their category + emoji.
         */
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS categories (
                        name TEXT NOT NULL PRIMARY KEY,
                        emoji TEXT NOT NULL,
                        sortOrder INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                seedDefaultCategories(db)
            }
        }

        /**
         * Migration from version 6 to 7: Add an index on budget_excluded_categories.category
         * to speed up the category-rename cascade UPDATE (the composite PK's leftmost column
         * is `month`, so a category-only predicate couldn't use it).
         */
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_budget_excluded_categories_category " +
                        "ON budget_excluded_categories(category)"
                )
            }
        }

        /**
         * Inserts the built-in default categories. Uses INSERT OR IGNORE so it is
         * idempotent and never clobbers existing rows. Shared by the v5→v6 migration
         * (existing installs) and the [onCreate] callback (fresh installs).
         */
        private fun seedDefaultCategories(db: SupportSQLiteDatabase) {
            ExpenseCategories.DEFAULT_CATEGORIES.forEachIndexed { index, (name, emoji) ->
                db.execSQL(
                    "INSERT OR IGNORE INTO categories (name, emoji, sortOrder) VALUES (?, ?, ?)",
                    arrayOf(name, emoji, index)
                )
            }
        }

        /** Seeds default categories on a brand-new database (no migration runs on first create). */
        private val seedCallback = object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                seedDefaultCategories(db)
            }
        }

        /**
         * Gets or creates an encrypted database instance.
         * Uses double-checked locking so only one instance is created under concurrent access.
         */
        internal fun getDatabase(context: Context): ExpenseDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: run {
                    val passphrase = SecureKeyGenerator.getOrGenerateKey(context)
                    val factory = SupportOpenHelperFactory(
                        passphrase.toByteArray(Charsets.UTF_8),
                        null,
                        false
                    )
                    Room
                        .databaseBuilder(
                            context.applicationContext,
                            ExpenseDatabase::class.java,
                            "expense_database"
                        ).openHelperFactory(factory)
                        .addMigrations(
                            MIGRATION_1_2,
                            MIGRATION_2_3,
                            MIGRATION_3_4,
                            MIGRATION_4_5,
                            MIGRATION_5_6,
                            MIGRATION_6_7
                        )
                        .addCallback(seedCallback)
                        .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
                        .build()
                        .also { INSTANCE = it }
                }
            }
    }
}
