package com.example.expancemanager.util

import android.content.ContentResolver
import android.net.Uri
import com.example.expancemanager.data.BudgetExcludedCategory
import com.example.expancemanager.data.Category
import com.example.expancemanager.data.Expense
import com.example.expancemanager.data.MonthlyBudget
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

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
}
