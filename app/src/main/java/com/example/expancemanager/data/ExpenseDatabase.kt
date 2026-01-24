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
@Database(entities = [Expense::class], version = 2, exportSchema = false)
abstract class ExpenseDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao

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
         * Gets or creates an encrypted database instance
         * Uses SQLCipher for transparent encryption/decryption
         * Optimized with proper configurations
         */
        fun getDatabase(context: Context): ExpenseDatabase =
            INSTANCE ?: synchronized(this) {
                // Generate or retrieve secure passphrase
                val passphrase = SecureKeyGenerator.getOrGenerateKey(context)

                // Create SQLCipher factory with passphrase
                val factory = SupportOpenHelperFactory(
                    passphrase.toByteArray(Charsets.UTF_8),
                    null,
                    false
                )

                // Build encrypted database with optimizations
                val instance = Room
                    .databaseBuilder(
                        context.applicationContext,
                        ExpenseDatabase::class.java,
                        "expense_database"
                    ).openHelperFactory(factory)
                    .addMigrations(MIGRATION_1_2)
                    .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING) // WAL for better concurrency
                    .build()

                INSTANCE = instance
                instance
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
