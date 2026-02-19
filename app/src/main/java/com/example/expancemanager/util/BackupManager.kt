package com.example.expancemanager.util

import android.app.Application
import android.net.Uri
import com.example.expancemanager.data.Expense
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * Manages backup and restore operations for expense data
 * Exports data as JSON files and imports from JSON files
 */
class BackupManager @Inject constructor(private val application: Application) {
    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .create()

    /**
     * Data class for backup metadata
     */
    data class BackupData(
        val version: Int = 1,
        val exportDate: Long = System.currentTimeMillis(),
        val totalExpenses: Int,
        val expenses: List<Expense>
    )

    /**
     * Exports expenses to a JSON file
     * @param uri URI where to save the backup file
     * @param expenses List of expenses to export
     * @return Result indicating success or failure
     */
    suspend fun exportToJson(uri: Uri, expenses: List<Expense>): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val backupData = BackupData(
                    totalExpenses = expenses.size,
                    expenses = expenses
                )

                val jsonString = gson.toJson(backupData)

                application.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(jsonString.toByteArray())
                    outputStream.flush()
                } ?: return@withContext Result.failure(Exception("Unable to open output stream"))

                Result.success("Successfully exported ${expenses.size} expenses")
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Imports expenses from a JSON file
     * @param uri URI of the backup file to import
     * @return Result containing list of expenses or error
     */
    suspend fun importFromJson(uri: Uri): Result<List<Expense>> =
        withContext(Dispatchers.IO) {
            try {
                val jsonString = application.contentResolver.openInputStream(uri)?.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).use { reader ->
                        reader.readText()
                    }
                } ?: return@withContext Result.failure(Exception("Unable to open input stream"))

                val type = object : TypeToken<BackupData>() {}.type
                val backupData: BackupData = gson.fromJson(jsonString, type)

                if (backupData.expenses.isEmpty()) {
                    return@withContext Result.failure(Exception("No expenses found in backup file"))
                }

                Result.success(backupData.expenses)
            } catch (e: Exception) {
                Result.failure(Exception("Invalid backup file format: ${e.message}"))
            }
        }

    /**
     * Generates a default filename for backup
     * Format: expense_backup_YYYYMMDD_HHMMSS.json
     */
    fun generateBackupFileName(): String {
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        return "expense_backup_${dateFormat.format(Date())}.json"
    }

    /**
     * Validates if a backup file is valid
     * @param uri URI of the file to validate
     * @return Result indicating if the file is valid
     */
    suspend fun validateBackupFile(uri: Uri): Result<Int> =
        withContext(Dispatchers.IO) {
            try {
                val jsonString = application.contentResolver.openInputStream(uri)?.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).use { reader ->
                        reader.readText()
                    }
                } ?: return@withContext Result.failure(Exception("Unable to read file"))

                val type = object : TypeToken<BackupData>() {}.type
                val backupData: BackupData = gson.fromJson(jsonString, type)

                Result.success(backupData.totalExpenses)
            } catch (e: Exception) {
                Result.failure(Exception("Invalid backup file"))
            }
        }
}
