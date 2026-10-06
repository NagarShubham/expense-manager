package com.snagar.expensetracker.util

import android.content.ContentResolver
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.snagar.expensetracker.data.BudgetExcludedCategory
import com.snagar.expensetracker.data.BudgetRepository
import com.snagar.expensetracker.data.Category
import com.snagar.expensetracker.data.CategoryRepository
import com.snagar.expensetracker.data.Expense
import com.snagar.expensetracker.data.ExpenseRepository
import com.snagar.expensetracker.data.MonthlyBudget
import com.snagar.expensetracker.data.TransactionRunner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.security.DigestOutputStream
import java.security.MessageDigest
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Manages backup and restore operations for expense, budget, and exclusion data.
 * Exports data as JSON files and imports from JSON files.
 *
 * Version 1 backups contain expenses only; version 2 adds monthly budgets and
 * budget exclusions; version 3 adds user-managed [Category] rows. Missing fields are
 * treated as empty lists on import, so older backups still restore.
 */
internal class BackupManager
    @Inject
    constructor(
        private val contentResolver: ContentResolver,
        private val expenseRepository: ExpenseRepository,
        private val budgetRepository: BudgetRepository,
        private val categoryRepository: CategoryRepository,
        private val transactionRunner: TransactionRunner
    ) {
        private val gson: Gson = GsonBuilder()
            .setPrettyPrinting()
            .create()

        /**
         * Backup file payload. Optional lists are nullable so Gson can read v1 files
         * that omit budget fields without crashing.
         */
        data class BackupData(
            val version: Int = CURRENT_VERSION,
            val exportDate: Long = System.currentTimeMillis(),
            val totalExpenses: Int,
            val expenses: List<Expense>? = null,
            val monthlyBudgets: List<MonthlyBudget>? = null,
            val budgetExcludedCategories: List<BudgetExcludedCategory>? = null,
            val categories: List<Category>? = null
        )

        /**
         * All backed-up data: produced by [loadSnapshot] (one DB transaction, for export)
         * and by [importFromJson] (after normalizing a parsed file). One shape serves both
         * directions since a snapshot and an import result carry identical fields; kept as
         * a single type rather than two near-duplicate data classes.
         */
        data class BackupSnapshot(
            val expenses: List<Expense>,
            val monthlyBudgets: List<MonthlyBudget>,
            val budgetExcludedCategories: List<BudgetExcludedCategory>,
            val categories: List<Category>
        ) {
            val isEmpty: Boolean
                get() = expenses.isEmpty() &&
                    monthlyBudgets.isEmpty() &&
                    budgetExcludedCategories.isEmpty() &&
                    categories.isEmpty()
        }

        /**
         * Reads all four tables in one transaction, so a concurrent write can't produce a
         * backup mixing data from different moments. Used by manual export and auto-backup.
         */
        internal suspend fun loadSnapshot(): BackupSnapshot {
            lateinit var snapshot: BackupSnapshot
            transactionRunner {
                snapshot = BackupSnapshot(
                    expenses = expenseRepository.getAllExpensesForExport(),
                    monthlyBudgets = budgetRepository.getAllBudgetsForExport(),
                    budgetExcludedCategories = budgetRepository.getAllExcludedCategoriesForExport(),
                    categories = categoryRepository.getAllForExport()
                )
            }
            return snapshot
        }

        /**
         * Exports expenses, monthly budgets, and budget exclusions to a JSON file.
         */
        internal suspend fun exportToJson(
            uri: Uri,
            expenses: List<Expense>,
            monthlyBudgets: List<MonthlyBudget>,
            budgetExcludedCategories: List<BudgetExcludedCategory>,
            categories: List<Category>
        ): Result<String> =
            withContext(Dispatchers.IO) {
                try {
                    val backupData = buildBackupData(
                        expenses = expenses,
                        monthlyBudgets = monthlyBudgets,
                        budgetExcludedCategories = budgetExcludedCategories,
                        categories = categories
                    )

                    contentResolver.openOutputStream(uri)?.use { outputStream ->
                        OutputStreamWriter(outputStream, Charsets.UTF_8).use { writer ->
                            gson.toJson(backupData, BackupData::class.java, writer)
                            writer.flush()
                        }
                    } ?: return@withContext Result.failure(Exception("Unable to open output stream"))

                    Result.success(buildExportSuccessMessage(expenses.size, monthlyBudgets.size, budgetExcludedCategories.size))
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

        /**
         * Imports a backup file. Supports v1 (expenses only) and v2 (expenses + budgets + exclusions).
         */
        internal suspend fun importFromJson(uri: Uri): Result<BackupSnapshot> =
            withContext(Dispatchers.IO) {
                try {
                    when (val outcome = loadBackup(uri, ioFailureMessage = "Unable to open input stream")) {
                        is LoadOutcome.IoFailure -> Result.failure(Exception(outcome.message))
                        is LoadOutcome.ParseFailure -> Result.failure(Exception("Invalid backup file format"))
                        is LoadOutcome.Success -> {
                            val normalized = normalize(outcome.data)
                            if (normalized.isEmpty) {
                                return@withContext Result.failure(Exception("No data found in backup file"))
                            }
                            Result.success(normalized)
                        }
                    }
                } catch (e: Exception) {
                    Result.failure(Exception("Invalid backup file format: ${e.message}"))
                }
            }

        /**
         * Generates a default filename for backup.
         * Format: expense_backup_YYYYMMDD_HHMMSS.json
         */
        internal fun generateBackupFileName(): String =
            "expense_backup_${BACKUP_FILE_TIMESTAMP.format(LocalDateTime.now())}.json"

        /** Distinct prefix from manual backups so the retention sweep only deletes its own files. */
        internal fun generateAutoBackupFileName(): String =
            "$AUTO_BACKUP_FILE_PREFIX${BACKUP_FILE_TIMESTAMP.format(LocalDateTime.now())}.json"

        /**
         * SHA-256 of the payload [exportToJson] would write, with `exportDate` pinned so only
         * data changes affect it. Streamed into the digest to keep memory flat.
         */
        internal fun contentSignature(
            expenses: List<Expense>,
            monthlyBudgets: List<MonthlyBudget>,
            budgetExcludedCategories: List<BudgetExcludedCategory>,
            categories: List<Category>
        ): String {
            val digest = MessageDigest.getInstance(SIGNATURE_ALGORITHM)
            val backupData = buildBackupData(
                expenses = expenses,
                monthlyBudgets = monthlyBudgets,
                budgetExcludedCategories = budgetExcludedCategories,
                categories = categories,
                exportDate = SIGNATURE_EXPORT_DATE
            )
            OutputStreamWriter(DigestOutputStream(NullOutputStream, digest), Charsets.UTF_8).use { writer ->
                gson.toJson(backupData, BackupData::class.java, writer)
            }
            return digest.digest().joinToString("") { byte ->
                (byte.toInt() and 0xFF).toString(16).padStart(2, '0')
            }
        }

        private fun buildBackupData(
            expenses: List<Expense>,
            monthlyBudgets: List<MonthlyBudget>,
            budgetExcludedCategories: List<BudgetExcludedCategory>,
            categories: List<Category>,
            exportDate: Long = System.currentTimeMillis()
        ): BackupData =
            BackupData(
                version = CURRENT_VERSION,
                exportDate = exportDate,
                totalExpenses = expenses.size,
                expenses = expenses,
                monthlyBudgets = monthlyBudgets,
                budgetExcludedCategories = budgetExcludedCategories,
                categories = categories
            )

        /** Sink for [contentSignature]: the bytes only need to reach the digest, not storage. */
        private object NullOutputStream : OutputStream() {
            override fun write(b: Int) = Unit

            override fun write(
                b: ByteArray,
                off: Int,
                len: Int
            ) = Unit
        }

        private sealed interface LoadOutcome {
            data class Success(val data: BackupData) : LoadOutcome

            data class IoFailure(val message: String) : LoadOutcome

            data object ParseFailure : LoadOutcome
        }

        private fun loadBackup(uri: Uri, ioFailureMessage: String): LoadOutcome =
            try {
                val backupData =
                    contentResolver.openInputStream(uri)?.use { inputStream ->
                        InputStreamReader(inputStream, Charsets.UTF_8).use { reader ->
                            gson.fromJson(reader, BackupData::class.java)
                        }
                    } ?: return LoadOutcome.IoFailure(ioFailureMessage)

                LoadOutcome.Success(backupData)
            } catch (_: Exception) {
                LoadOutcome.ParseFailure
            }

        /** Fills in v1/v2 backups that omit later fields with empty lists, so old files still import. */
        private fun normalize(backupData: BackupData): BackupSnapshot =
            BackupSnapshot(
                expenses = backupData.expenses.orEmpty(),
                monthlyBudgets = backupData.monthlyBudgets.orEmpty(),
                budgetExcludedCategories = backupData.budgetExcludedCategories.orEmpty(),
                categories = backupData.categories.orEmpty()
            )

        private fun buildExportSuccessMessage(
            expenseCount: Int,
            budgetCount: Int,
            exclusionCount: Int
        ): String = buildList {
            if (expenseCount > 0) add("$expenseCount expenses")
            if (budgetCount > 0) add("$budgetCount budgets")
            if (exclusionCount > 0) add("$exclusionCount category exclusions")
        }.joinToString(", ", prefix = "Successfully exported ")

        companion object {
            const val CURRENT_VERSION = 3

            /** Filename prefix that marks a file as written by the auto-backup worker. */
            const val AUTO_BACKUP_FILE_PREFIX = "expense_autobackup_"

            private const val SIGNATURE_ALGORITHM = "SHA-256"

            /** Placeholder for the one field that must not influence [contentSignature]. */
            private const val SIGNATURE_EXPORT_DATE = 0L

            private val BACKUP_FILE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
        }
    }
