package com.example.expancemanager.util

import android.content.ContentResolver
import android.net.Uri
import com.example.expancemanager.data.BudgetExcludedCategory
import com.example.expancemanager.data.Expense
import com.example.expancemanager.data.MonthlyBudget
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Manages backup and restore operations for expense, budget, and exclusion data.
 * Exports data as JSON files and imports from JSON files.
 *
 * Version 1 backups contain expenses only; version 2 adds [monthlyBudgets] and
 * [budgetExcludedCategories]. Missing fields are treated as empty lists on import.
 */
class BackupManager @Inject constructor(private val contentResolver: ContentResolver) {
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
        val budgetExcludedCategories: List<BudgetExcludedCategory>? = null
    )

    data class BackupImportResult(
        val expenses: List<Expense>,
        val monthlyBudgets: List<MonthlyBudget>,
        val budgetExcludedCategories: List<BudgetExcludedCategory>
    )

    /**
     * Exports expenses, monthly budgets, and budget exclusions to a JSON file.
     */
    suspend fun exportToJson(
        uri: Uri,
        expenses: List<Expense>,
        monthlyBudgets: List<MonthlyBudget>,
        budgetExcludedCategories: List<BudgetExcludedCategory>
    ): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val backupData = BackupData(
                    version = CURRENT_VERSION,
                    totalExpenses = expenses.size,
                    expenses = expenses,
                    monthlyBudgets = monthlyBudgets,
                    budgetExcludedCategories = budgetExcludedCategories
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
    suspend fun importFromJson(uri: Uri): Result<BackupImportResult> =
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
    fun generateBackupFileName(): String =
        "expense_backup_${BACKUP_FILE_TIMESTAMP.format(LocalDateTime.now())}.json"

    private sealed interface LoadOutcome {
        data class Success(val data: BackupData) : LoadOutcome

        data class IoFailure(val message: String) : LoadOutcome

        data object ParseFailure : LoadOutcome
    }

    private data class NormalizedBackup(
        val expenses: List<Expense>,
        val monthlyBudgets: List<MonthlyBudget>,
        val budgetExcludedCategories: List<BudgetExcludedCategory>
    ) {
        fun hasImportableData(): Boolean =
            expenses.isNotEmpty() || monthlyBudgets.isNotEmpty() || budgetExcludedCategories.isNotEmpty()

        fun toImportResult(): BackupImportResult =
            BackupImportResult(
                expenses = expenses,
                monthlyBudgets = monthlyBudgets,
                budgetExcludedCategories = budgetExcludedCategories
            )
    }

    private fun loadBackup(uri: Uri, ioFailureMessage: String): LoadOutcome =
        try {
            val backupData =
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    InputStreamReader(inputStream, Charsets.UTF_8).use { reader ->
                        @Suppress("UNCHECKED_CAST")
                        gson.fromJson(reader, BACKUP_DATA_TYPE) as BackupData?
                    }
                } ?: return LoadOutcome.IoFailure(ioFailureMessage)

            if (backupData == null) {
                LoadOutcome.ParseFailure
            } else {
                LoadOutcome.Success(backupData)
            }
        } catch (_: Exception) {
            LoadOutcome.ParseFailure
        }

    private fun normalize(backupData: BackupData): NormalizedBackup =
        NormalizedBackup(
            expenses = backupData.expenses.orEmpty(),
            monthlyBudgets = backupData.monthlyBudgets.orEmpty(),
            budgetExcludedCategories = backupData.budgetExcludedCategories.orEmpty()
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
        const val CURRENT_VERSION = 2

        private val BACKUP_DATA_TYPE = object : TypeToken<BackupData>() {}.type

        private val BACKUP_FILE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
    }
}
