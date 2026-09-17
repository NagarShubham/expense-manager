package com.example.expensemanager.util

import android.content.ContentResolver
import android.net.Uri
import com.example.expensemanager.data.BudgetExcludedCategory
import com.example.expensemanager.data.Category
import com.example.expensemanager.data.Expense
import com.example.expensemanager.data.MonthlyBudget
import com.google.gson.Gson
import com.google.gson.GsonBuilder
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
    constructor(private val contentResolver: ContentResolver) {
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

        data class BackupImportResult(
            val expenses: List<Expense>,
            val monthlyBudgets: List<MonthlyBudget>,
            val budgetExcludedCategories: List<BudgetExcludedCategory>,
            val categories: List<Category>
        )

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
        internal suspend fun importFromJson(uri: Uri): Result<BackupImportResult> =
            withContext(Dispatchers.IO) {
                try {
                    when (val outcome = loadBackup(uri, ioFailureMessage = "Unable to open input stream")) {
                        is LoadOutcome.IoFailure -> Result.failure(Exception(outcome.message))
                        is LoadOutcome.ParseFailure -> Result.failure(Exception("Invalid backup file format"))
                        is LoadOutcome.Success -> {
                            val normalized = normalize(outcome.data)
                            if (!normalized.hasImportableData()) {
                                return@withContext Result.failure(Exception("No data found in backup file"))
                            }
                            Result.success(normalized.toImportResult())
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

        /**
         * Filename for an automatic backup. Deliberately a *different* prefix from
         * [generateBackupFileName] even though the file contents are identical: both kinds
         * of backup can land in the same folder, and the auto-backup retention sweep must
         * only ever be able to delete files it wrote itself.
         */
        internal fun generateAutoBackupFileName(): String =
            "$AUTO_BACKUP_FILE_PREFIX${BACKUP_FILE_TIMESTAMP.format(LocalDateTime.now())}.json"

        /**
         * Fingerprint of the exact payload [exportToJson] would write for this data.
         *
         * [BackupData.exportDate] is pinned to [SIGNATURE_EXPORT_DATE] because it changes on
         * every run; leaving it in would make every comparison a mismatch and defeat the
         * whole point of change detection. Everything else goes through the same Gson
         * instance and the same [BackupData] shape as a real export, so the signature tracks
         * the file's content field-for-field — including any field added to the format later.
         *
         * The JSON is streamed straight into the digest rather than built as a String, so
         * peak memory stays flat no matter how long the expense history is.
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

        private data class NormalizedBackup(
            val expenses: List<Expense>,
            val monthlyBudgets: List<MonthlyBudget>,
            val budgetExcludedCategories: List<BudgetExcludedCategory>,
            val categories: List<Category>
        ) {
            fun hasImportableData(): Boolean =
                expenses.isNotEmpty() ||
                    monthlyBudgets.isNotEmpty() ||
                    budgetExcludedCategories.isNotEmpty() ||
                    categories.isNotEmpty()

            fun toImportResult(): BackupImportResult =
                BackupImportResult(
                    expenses = expenses,
                    monthlyBudgets = monthlyBudgets,
                    budgetExcludedCategories = budgetExcludedCategories,
                    categories = categories
                )
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

        private fun normalize(backupData: BackupData): NormalizedBackup =
            NormalizedBackup(
                expenses = backupData.expenses.orEmpty(),
                monthlyBudgets = backupData.monthlyBudgets.orEmpty(),
                budgetExcludedCategories = backupData.budgetExcludedCategories.orEmpty(),
                categories = backupData.categories.orEmpty()
            )

        private fun buildExportSuccessMessage(
            expenseCount: Int,
            budgetCount: Int,
            exclusionCount: Int
        ): String {
            val parts = buildList {
                if (expenseCount > 0) add("$expenseCount expenses")
                if (budgetCount > 0) add("$budgetCount budgets")
                if (exclusionCount > 0) add("$exclusionCount category exclusions")
            }
            return "Successfully exported ${parts.joinToString(", ")}"
        }

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
