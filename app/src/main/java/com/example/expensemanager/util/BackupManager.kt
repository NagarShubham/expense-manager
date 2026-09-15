package com.example.expensemanager.util

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import com.example.expensemanager.data.BudgetExcludedCategory
import com.example.expensemanager.data.Category
import com.example.expensemanager.data.Expense
import com.example.expensemanager.data.MonthlyBudget
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
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
                    val backupData = buildBackupData(expenses, monthlyBudgets, budgetExcludedCategories, categories)

                    contentResolver.openOutputStream(uri)?.use { outputStream ->
                        writeBackupData(outputStream, backupData)
                    } ?: return@withContext Result.failure(Exception("Unable to open output stream"))

                    Result.success(buildExportSuccessMessage(expenses.size, monthlyBudgets.size, budgetExcludedCategories.size))
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

        /**
         * Exports the backup JSON into the shared **Downloads/[subDir]** folder so the user can
         * find the file in their Downloads. Used by the headless scheduled auto-backup.
         *
         * On API 29+ this writes through [MediaStore] (scoped storage — no runtime permission
         * needed). On API 26–28 it writes directly to the public Downloads directory, which
         * requires the `WRITE_EXTERNAL_STORAGE` permission (declared with `maxSdkVersion="28"`).
         */
        internal suspend fun exportToDownloads(
            subDir: String,
            expenses: List<Expense>,
            monthlyBudgets: List<MonthlyBudget>,
            budgetExcludedCategories: List<BudgetExcludedCategory>,
            categories: List<Category>
        ): Result<String> =
            withContext(Dispatchers.IO) {
                try {
                    val backupData = buildBackupData(expenses, monthlyBudgets, budgetExcludedCategories, categories)
                    val fileName = generateBackupFileName()

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        writeViaMediaStore(subDir, fileName, backupData)
                    } else {
                        writeToPublicDownloads(subDir, fileName, backupData)
                    }

                    Result.success(
                        buildExportSuccessMessage(expenses.size, monthlyBudgets.size, budgetExcludedCategories.size)
                    )
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

        /**
         * Deletes older auto-backups so at most [keep] of the most recent files remain in
         * Downloads/[subDir]. Best-effort: pruning failures are swallowed so they can't fail a
         * successful backup.
         */
        internal fun pruneDownloads(subDir: String, keep: Int) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    pruneViaMediaStore(subDir, keep)
                } else {
                    prunePublicDownloads(subDir, keep)
                }
            } catch (_: Exception) {
                // Retention is non-critical; ignore.
            }
        }

        @RequiresApi(Build.VERSION_CODES.Q)
        private fun writeViaMediaStore(subDir: String, fileName: String, backupData: BackupData) {
            val relativePath = Environment.DIRECTORY_DOWNLOADS + File.separator + subDir
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "application/json")
                put(MediaStore.Downloads.RELATIVE_PATH, relativePath)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val uri = contentResolver.insert(collection, values)
                ?: throw IOException("Unable to create backup entry in Downloads")
            try {
                contentResolver.openOutputStream(uri)?.use { writeBackupData(it, backupData) }
                    ?: throw IOException("Unable to open output stream for Downloads backup")
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                contentResolver.update(uri, values, null, null)
            } catch (e: Exception) {
                // Roll back the half-written pending entry so no corrupt file lingers.
                contentResolver.delete(uri, null, null)
                throw e
            }
        }

        @Suppress("DEPRECATION")
        private fun writeToPublicDownloads(subDir: String, fileName: String, backupData: BackupData) {
            val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val dir = File(downloads, subDir).apply { mkdirs() }
            // Write to a temp file first, then atomically rename so a crash mid-write can't
            // leave a truncated/corrupt backup in place.
            val tempFile = File(dir, "$fileName.tmp")
            tempFile.outputStream().use { writeBackupData(it, backupData) }
            if (!tempFile.renameTo(File(dir, fileName))) {
                tempFile.delete()
                throw IOException("Unable to finalize backup file in Downloads")
            }
        }

        private fun buildBackupData(
            expenses: List<Expense>,
            monthlyBudgets: List<MonthlyBudget>,
            budgetExcludedCategories: List<BudgetExcludedCategory>,
            categories: List<Category>
        ): BackupData =
            BackupData(
                version = CURRENT_VERSION,
                totalExpenses = expenses.size,
                expenses = expenses,
                monthlyBudgets = monthlyBudgets,
                budgetExcludedCategories = budgetExcludedCategories,
                categories = categories
            )

        private fun writeBackupData(outputStream: OutputStream, backupData: BackupData) {
            OutputStreamWriter(outputStream, Charsets.UTF_8).use { writer ->
                gson.toJson(backupData, BackupData::class.java, writer)
                writer.flush()
            }
        }

        @RequiresApi(Build.VERSION_CODES.Q)
        private fun pruneViaMediaStore(subDir: String, keep: Int) {
            val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val projection = arrayOf(MediaStore.Downloads._ID)
            val selection =
                "${MediaStore.Downloads.RELATIVE_PATH} LIKE ? AND ${MediaStore.Downloads.DISPLAY_NAME} LIKE ?"
            val selectionArgs = arrayOf(
                "%${Environment.DIRECTORY_DOWNLOADS}/$subDir%",
                "expense_backup_%.json"
            )
            val sortOrder = "${MediaStore.Downloads.DATE_ADDED} DESC"

            val ids = mutableListOf<Long>()
            contentResolver.query(collection, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID)
                while (cursor.moveToNext()) {
                    ids.add(cursor.getLong(idColumn))
                }
            }
            ids.drop(keep).forEach { id ->
                contentResolver.delete(ContentUris.withAppendedId(collection, id), null, null)
            }
        }

        @Suppress("DEPRECATION")
        private fun prunePublicDownloads(subDir: String, keep: Int) {
            val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val backups = File(downloads, subDir).listFiles { f -> f.isFile && f.name.endsWith(".json") } ?: return
            if (backups.size <= keep) return
            backups.sortedByDescending { it.lastModified() }.drop(keep).forEach { it.delete() }
        }

        /**
         * Computes a stable content fingerprint (SHA-256) over the data that would be backed up.
         *
         * Only the row content is hashed — not [BackupData.exportDate] or counts derived from it —
         * so the fingerprint changes if and only if the underlying data changes. The scheduled
         * worker compares this against the previously stored fingerprint to skip redundant backups.
         */
        internal fun computeFingerprint(
            expenses: List<Expense>,
            monthlyBudgets: List<MonthlyBudget>,
            budgetExcludedCategories: List<BudgetExcludedCategory>,
            categories: List<Category>
        ): String {
            val content = mapOf(
                "version" to CURRENT_VERSION,
                "expenses" to expenses,
                "monthlyBudgets" to monthlyBudgets,
                "budgetExcludedCategories" to budgetExcludedCategories,
                "categories" to categories
            )
            val json = gson.toJson(content)
            val digest = MessageDigest.getInstance("SHA-256").digest(json.toByteArray(Charsets.UTF_8))
            return digest.joinToString("") { "%02x".format(it) }
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
                } catch (e: CancellationException) {
                    throw e
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

            private val BACKUP_FILE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
        }
    }
