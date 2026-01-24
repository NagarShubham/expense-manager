package com.example.expancemanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.example.expancemanager.nav.AddExpenseRoute
import com.example.expancemanager.nav.AllCategoriesRoute
import com.example.expancemanager.nav.CategoryExpensesRoute
import com.example.expancemanager.nav.EditExpenseRoute
import com.example.expancemanager.nav.ExpenseDetailRoute
import com.example.expancemanager.nav.HomeScreenRoute
import com.example.expancemanager.nav.SettingsRoute
import com.example.expancemanager.ui.screen.AddEditExpenseScreen
import com.example.expancemanager.ui.screen.AllCategoriesScreen
import com.example.expancemanager.ui.screen.CategoryExpensesScreen
import com.example.expancemanager.ui.screen.ExpenseDetailScreen
import com.example.expancemanager.ui.screen.HomeScreen
import com.example.expancemanager.ui.screen.SettingsScreen
import com.example.expancemanager.ui.theme.ExpanceManagerTheme
import com.example.expancemanager.viewmodel.ExpenseViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ExpanceManagerTheme { MainContent() }
        }
    }

    @Composable
    private fun MainContent() {
        val backStack = remember { mutableStateListOf<Any>(HomeScreenRoute) }
        // Create ViewModel once and reuse - prevents recreation on config changes
        val viewModel: ExpenseViewModel = viewModel(
            factory = ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )

        NavDisplay(
            backStack = backStack,
            modifier = Modifier.fillMaxSize(),
            entryProvider = entryProvider {
                entry<HomeScreenRoute> {
                    HomeScreen(
                        viewModel = viewModel,
                        onAddExpenseClick = { backStack.add(AddExpenseRoute) },
                        onExpenseClick = { backStack.add(ExpenseDetailRoute(it)) },
                        onCategoryClick = { category, month, year ->
                            backStack.add(CategoryExpensesRoute(category, month, year))
                        },
                        onShowAllCategoriesClick = { month, year ->
                            backStack.add(AllCategoriesRoute(month, year))
                        },
                        onSettingsClick = { backStack.add(SettingsRoute) }
                    )
                }

                entry<AllCategoriesRoute> { route ->
                    AllCategoriesScreen(
                        month = route.month,
                        year = route.year,
                        viewModel = viewModel,
                        onNavigateBack = { backStack.removeAt(backStack.lastIndex) },
                        onCategoryClick = { category, month, year ->
                            backStack.add(CategoryExpensesRoute(category, month, year))
                        }
                    )
                }

                entry<CategoryExpensesRoute> { route ->
                    CategoryExpensesScreen(
                        category = route.category,
                        month = route.month,
                        year = route.year,
                        viewModel = viewModel,
                        onNavigateBack = { backStack.removeAt(backStack.lastIndex) },
                        onExpenseClick = { backStack.add(ExpenseDetailRoute(it)) }
                    )
                }

                entry<AddExpenseRoute> {
                    AddEditExpenseScreen(
                        viewModel = viewModel,
                        onNavigateBack = { backStack.removeAt(backStack.lastIndex) }
                    )
                }

                entry<EditExpenseRoute> { route ->
                    AddEditExpenseScreen(
                        expenseId = route.expenseId,
                        viewModel = viewModel,
                        onNavigateBack = { backStack.removeAt(backStack.lastIndex) }
                    )
                }

                entry<ExpenseDetailRoute> { route ->
                    ExpenseDetailScreen(
                        expenseId = route.expenseId,
                        viewModel = viewModel,
                        onNavigateBack = { backStack.removeAt(backStack.lastIndex) },
                        onEditExpense = {
                            backStack.removeAt(backStack.lastIndex)
                            backStack.add(EditExpenseRoute(it))
                        }
                    )
                }

                entry<SettingsRoute> {
                    SettingsScreen(
                        viewModel = viewModel,
                        onNavigateBack = { backStack.removeAt(backStack.lastIndex) }
                    )
                }
            }
        )
    }
}
