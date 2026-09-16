package com.example.expensemanager.util

import android.content.ContentResolver
import android.net.Uri
import com.example.expensemanager.data.BudgetExcludedCategory
import com.example.expensemanager.data.Category
import com.example.expensemanager.data.Expense
import com.example.expensemanager.data.MonthlyBudget
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.MessageDigest

class BackupManagerTest {
    private val uri = mockk<Uri>()
    private val contentResolver = mockk<ContentResolver>()
    private val outputBuffer = ByteArrayOutputStream()
    private val backupManager = BackupManager(contentResolver)

    @Test
    fun generateBackupFileName_usesExpectedPattern() {
        val fileName = backupManager.generateBackupFileName()

        assertThat(fileName).matches("expense_backup_\\d{8}_\\d{6}\\.json")
    }

    @Test
    fun exportAndImport_roundTripsAllData() = runTest {
        val expenses = listOf(
            Expense(
                id = 1L,
                title = "Coffee",
                amount = 4.5,
                category = "Food",
                date = 1_700_000_000_000L
            )
        )
        val budgets = listOf(MonthlyBudget(month = 0, year = 2024, expectedAmount = 500.0))
        val exclusions = listOf(BudgetExcludedCategory(month = 0, year = 2024, category = "Travel"))
        val categories = listOf(Category(name = "Food", emoji = "🍔", sortOrder = 0))

        stubExportStream()
        val exportResult = backupManager.exportToJson(uri, expenses, budgets, exclusions, categories)
        assertThat(exportResult.isSuccess).isTrue()

        stubImportStream(outputBuffer.toByteArray())
        val importResult = backupManager.importFromJson(uri)

        assertThat(importResult.isSuccess).isTrue()
        importResult.getOrThrow().let { imported ->
            assertThat(imported.expenses).isEqualTo(expenses)
            assertThat(imported.monthlyBudgets).isEqualTo(budgets)
            assertThat(imported.budgetExcludedCategories).isEqualTo(exclusions)
            assertThat(imported.categories).isEqualTo(categories)
        }
    }

    @Test
    fun importFromJson_supportsVersionOneBackupWithExpensesOnly() = runTest {
        val v1Json = """
            {
              "version": 1,
              "exportDate": 1700000000000,
              "totalExpenses": 1,
              "expenses": [
                {
                  "id": 1,
                  "title": "Legacy",
                  "amount": 10.0,
                  "category": "Food",
                  "description": "",
                  "date": 1700000000000,
                  "createdAt": 1700000000000
                }
              ]
            }
        """.trimIndent()

        stubImportStream(v1Json.toByteArray(Charsets.UTF_8))
        val importResult = backupManager.importFromJson(uri)

        assertThat(importResult.isSuccess).isTrue()
        importResult.getOrThrow().let { imported ->
            assertThat(imported.expenses).hasSize(1)
            assertThat(imported.expenses.first().title).isEqualTo("Legacy")
            assertThat(imported.monthlyBudgets).isEmpty()
            assertThat(imported.budgetExcludedCategories).isEmpty()
            assertThat(imported.categories).isEmpty()
        }
    }

    @Test
    fun importFromJson_v2BackupWithoutCategories_importsWithEmptyCategories() = runTest {
        // A v2 file predates user-managed categories; it must still import, and the
        // absent categories field must normalize to an empty list (so seeded defaults
        // are preserved rather than wiped).
        val v2Json = """
            {
              "version": 2,
              "exportDate": 1700000000000,
              "totalExpenses": 1,
              "expenses": [
                {
                  "id": 1,
                  "title": "Legacy",
                  "amount": 10.0,
                  "category": "Food",
                  "description": "",
                  "date": 1700000000000,
                  "createdAt": 1700000000000
                }
              ],
              "monthlyBudgets": [],
              "budgetExcludedCategories": []
            }
        """.trimIndent()

        stubImportStream(v2Json.toByteArray(Charsets.UTF_8))
        val importResult = backupManager.importFromJson(uri)

        assertThat(importResult.isSuccess).isTrue()
        assertThat(importResult.getOrThrow().categories).isEmpty()
    }

    @Test
    fun importFromJson_backupWithOnlyCategories_isImportable() = runTest {
        val json = """
            {
              "version": 3,
              "exportDate": 1700000000000,
              "totalExpenses": 0,
              "expenses": [],
              "monthlyBudgets": [],
              "budgetExcludedCategories": [],
              "categories": [
                { "name": "Pets", "emoji": "🐶", "sortOrder": 0 }
              ]
            }
        """.trimIndent()

        stubImportStream(json.toByteArray(Charsets.UTF_8))
        val importResult = backupManager.importFromJson(uri)

        assertThat(importResult.isSuccess).isTrue()
        importResult.getOrThrow().categories.let { categories ->
            assertThat(categories).hasSize(1)
            assertThat(categories.first()).isEqualTo(Category(name = "Pets", emoji = "🐶", sortOrder = 0))
        }
    }

    @Test
    fun importFromJson_failsWhenBackupHasNoData() = runTest {
        val emptyJson = """
            {
              "version": 2,
              "exportDate": 1700000000000,
              "totalExpenses": 0,
              "expenses": [],
              "monthlyBudgets": [],
              "budgetExcludedCategories": []
            }
        """.trimIndent()

        stubImportStream(emptyJson.toByteArray(Charsets.UTF_8))
        val importResult = backupManager.importFromJson(uri)

        assertThat(importResult.isFailure).isTrue()
        assertThat(importResult.exceptionOrNull()?.message).contains("No data found")
    }

    @Test
    fun importFromJson_failsForInvalidJson() = runTest {
        stubImportStream("{ not valid json".toByteArray(Charsets.UTF_8))
        val importResult = backupManager.importFromJson(uri)

        assertThat(importResult.isFailure).isTrue()
    }

    @Test
    fun generateAutoBackupFileName_usesDistinctPrefixFromManualExport() {
        // The retention sweep deletes by prefix, so an auto-backup must never be
        // mistakable for a manual export sitting in the same folder.
        val autoName = backupManager.generateAutoBackupFileName()

        assertThat(autoName).matches("expense_autobackup_\\d{8}_\\d{6}\\.json")
        assertThat(autoName).startsWith(BackupManager.AUTO_BACKUP_FILE_PREFIX)
        assertThat(backupManager.generateBackupFileName()).doesNotMatch("expense_autobackup_.*")
    }

    @Test
    fun contentSignature_isStableForUnchangedData() {
        // The export timestamp moves every run; if it leaked into the signature the
        // worker would rewrite the backup file every 15 minutes forever.
        val first = backupManager.contentSignature(EXPENSES, BUDGETS, EXCLUSIONS, CATEGORIES)
        val second = backupManager.contentSignature(EXPENSES, BUDGETS, EXCLUSIONS, CATEGORIES)

        assertThat(first).isEqualTo(second)
        assertThat(first).matches("[0-9a-f]{64}")
    }

    @Test
    fun contentSignature_changesForEveryBackedUpField() {
        val baseline = backupManager.contentSignature(EXPENSES, BUDGETS, EXCLUSIONS, CATEGORIES)

        val variants = mapOf(
            "edited expense title" to backupManager.contentSignature(
                EXPENSES.map { it.copy(title = "Tea") },
                BUDGETS,
                EXCLUSIONS,
                CATEGORIES
            ),
            "edited expense amount" to backupManager.contentSignature(
                EXPENSES.map { it.copy(amount = 5.0) },
                BUDGETS,
                EXCLUSIONS,
                CATEGORIES
            ),
            "added expense" to backupManager.contentSignature(
                EXPENSES + EXPENSES.first().copy(id = 2L),
                BUDGETS,
                EXCLUSIONS,
                CATEGORIES
            ),
            "edited budget" to backupManager.contentSignature(
                EXPENSES,
                BUDGETS.map { it.copy(expectedAmount = 600.0) },
                EXCLUSIONS,
                CATEGORIES
            ),
            "removed exclusion" to backupManager.contentSignature(EXPENSES, BUDGETS, emptyList(), CATEGORIES),
            "renamed category emoji" to backupManager.contentSignature(
                EXPENSES,
                BUDGETS,
                EXCLUSIONS,
                CATEGORIES.map { it.copy(emoji = "🥐") }
            )
        )

        variants.forEach { (change, signature) ->
            assertWithMessage(change).that(signature).isNotEqualTo(baseline)
        }
    }

    @Test
    fun contentSignature_matchesTheFileThatWouldBeWritten() = runTest {
        // The signature has to describe the exported payload exactly, or the worker will
        // skip a backup it should have written. Compare against a real export with the
        // one volatile field (exportDate) normalized away.
        stubExportStream()
        assertThat(backupManager.exportToJson(uri, EXPENSES, BUDGETS, EXCLUSIONS, CATEGORIES).isSuccess).isTrue()

        val exported = outputBuffer.toString(Charsets.UTF_8.name())
        val normalized = exported.replace(Regex("\"exportDate\": \\d+"), "\"exportDate\": 0")
        val expected = MessageDigest
            .getInstance("SHA-256")
            .digest(normalized.toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> (byte.toInt() and 0xFF).toString(16).padStart(2, '0') }

        assertThat(backupManager.contentSignature(EXPENSES, BUDGETS, EXCLUSIONS, CATEGORIES)).isEqualTo(expected)
    }

    private fun stubExportStream() {
        outputBuffer.reset()
        every { contentResolver.openOutputStream(uri) } answers {
            outputBuffer.reset()
            outputBuffer
        }
    }

    private fun stubImportStream(bytes: ByteArray) {
        every { contentResolver.openInputStream(uri) } returns ByteArrayInputStream(bytes)
    }

    private companion object {
        val EXPENSES = listOf(
            Expense(
                id = 1L,
                title = "Coffee",
                amount = 4.5,
                category = "Food",
                date = 1_700_000_000_000L,
                createdAt = 1_700_000_000_000L
            )
        )
        val BUDGETS = listOf(MonthlyBudget(month = 0, year = 2024, expectedAmount = 500.0))
        val EXCLUSIONS = listOf(BudgetExcludedCategory(month = 0, year = 2024, category = "Travel"))
        val CATEGORIES = listOf(Category(name = "Food", emoji = "🍔", sortOrder = 0))
    }
}
