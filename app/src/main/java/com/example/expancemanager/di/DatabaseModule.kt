package com.example.expancemanager.di

import android.content.Context
import com.example.expancemanager.data.BudgetExcludedCategoryDao
import com.example.expancemanager.data.BudgetRepository
import com.example.expancemanager.data.ExpenseDao
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
object DatabaseModule {
    @Provides
    @Singleton
    fun provideExpenseDatabase(
        @ApplicationContext context: Context
    ): ExpenseDatabase = ExpenseDatabase.getDatabase(context)

    @Provides
    @Singleton
    fun provideExpenseDao(database: ExpenseDatabase): ExpenseDao = database.expenseDao()

    @Provides
    @Singleton
    fun provideExpenseRepository(expenseDao: ExpenseDao): ExpenseRepository = ExpenseRepository(expenseDao)

    @Provides
    @Singleton
    fun provideMonthlyBudgetDao(database: ExpenseDatabase): MonthlyBudgetDao = database.monthlyBudgetDao()

    @Provides
    @Singleton
    fun provideBudgetExcludedCategoryDao(database: ExpenseDatabase): BudgetExcludedCategoryDao = database.budgetExcludedCategoryDao()

    @Provides
    @Singleton
    fun provideBudgetRepository(
        monthlyBudgetDao: MonthlyBudgetDao,
        budgetExcludedCategoryDao: BudgetExcludedCategoryDao
    ): BudgetRepository = BudgetRepository(monthlyBudgetDao, budgetExcludedCategoryDao)
}
