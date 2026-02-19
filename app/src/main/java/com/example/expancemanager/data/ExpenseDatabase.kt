package com.example.expancemanager.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.expancemanager.util.SecureKeyGenerator
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

/**
 * Encrypted Room database using SQLCipher
 * All data is encrypted at rest using 256-bit AES encryption
 * Optimized with indexes for better query performance
 */
@Database(
    entities = [Expense::class, MonthlyBudget::class, BudgetExcludedCategory::class],
    version = 5,
    exportSchema = false
)
abstract class ExpenseDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao

    abstract fun monthlyBudgetDao(): MonthlyBudgetDao

    abstract fun budgetExcludedCategoryDao(): BudgetExcludedCategoryDao

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
         * Gets or creates an encrypted database instance.
         * Uses double-checked locking so only one instance is created under concurrent access.
         */
        fun getDatabase(context: Context): ExpenseDatabase =
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
                        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                        .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
                        .build()
                        .also { INSTANCE = it }
                }
            }

        /**
         * Closes and clears the database instance
         * Call this if you need to reset the database
         */
        fun closeDatabase() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }
}
