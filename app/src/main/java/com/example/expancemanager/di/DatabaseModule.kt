package com.example.expancemanager.di

import android.content.Context
import androidx.room.withTransaction
import com.example.expancemanager.data.BudgetExcludedCategoryDao
import com.example.expancemanager.data.BudgetRepository
import com.example.expancemanager.data.CategoryDao
import com.example.expancemanager.data.CategoryRepository
import com.example.expancemanager.data.ExpenseDao
import com.example.expancemanager.data.TransactionRunner
import com.example.expancemanager.data.ExpenseDatabase
import com.example.expancemanager.data.ExpenseRepository
import com.example.expancemanager.data.MonthlyBudgetDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseModule {
    @Provides
    @Singleton
    internal fun provideExpenseDatabase(
        @ApplicationContext context: Context
    ): ExpenseDatabase {
        // getDatabase() opens SQLCipher + reads from the Android Keystore.
        // Both are slow operations (~100–300 ms on a cold start). Room itself defers
        // the actual file open until the first query (which runs on Dispatchers.IO via
        // the DAO Flow collectors), so returning the builder result here is safe —
        // the heavy work happens off the main thread on first DB access.
        return ExpenseDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    internal fun provideExpenseDao(database: ExpenseDatabase): ExpenseDao = database.expenseDao()

    @Provides
    @Singleton
    internal fun provideExpenseRepository(expenseDao: ExpenseDao): ExpenseRepository = ExpenseRepository(expenseDao)

    @Provides
    @Singleton
    internal fun provideMonthlyBudgetDao(database: ExpenseDatabase): MonthlyBudgetDao = database.monthlyBudgetDao()

    @Provides
    @Singleton
    internal fun provideBudgetExcludedCategoryDao(database: ExpenseDatabase): BudgetExcludedCategoryDao = database.budgetExcludedCategoryDao()

    @Provides
    @Singleton
    internal fun provideBudgetRepository(
        monthlyBudgetDao: MonthlyBudgetDao,
        budgetExcludedCategoryDao: BudgetExcludedCategoryDao
    ): BudgetRepository = BudgetRepository(monthlyBudgetDao, budgetExcludedCategoryDao)

    @Provides
    @Singleton
    internal fun provideCategoryDao(database: ExpenseDatabase): CategoryDao = database.categoryDao()

    @Provides
    @Singleton
    internal fun provideTransactionRunner(database: ExpenseDatabase): TransactionRunner =
        TransactionRunner { block -> database.withTransaction { block() } }

    @Provides
    @Singleton
    internal fun provideCategoryRepository(
        categoryDao: CategoryDao,
        expenseDao: ExpenseDao,
        budgetExcludedCategoryDao: BudgetExcludedCategoryDao,
        transactionRunner: TransactionRunner
    ): CategoryRepository =
        CategoryRepository(
            categoryDao = categoryDao,
            expenseDao = expenseDao,
            budgetExcludedCategoryDao = budgetExcludedCategoryDao,
            transactionRunner = transactionRunner
        )
}
