package com.example.expensemanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.example.expensemanager.data.PreferenceRepository
import com.example.expensemanager.nav.AddExpenseRoute
import com.example.expensemanager.nav.AllCategoriesRoute
import com.example.expensemanager.nav.BudgetSettingsRoute
import com.example.expensemanager.nav.CategoryExpensesRoute
import com.example.expensemanager.nav.EditExpenseRoute
import com.example.expensemanager.nav.ExpenseDetailRoute
import com.example.expensemanager.nav.HomeScreenRoute
import com.example.expensemanager.nav.ManageCategoriesRoute
import com.example.expensemanager.nav.ReportsRoute
import com.example.expensemanager.nav.SearchRoute
import com.example.expensemanager.nav.SettingsRoute
import com.example.expensemanager.ui.screen.AddEditExpenseScreen
import com.example.expensemanager.ui.screen.AllCategoriesScreen
import com.example.expensemanager.ui.screen.BiometricAppGate
import com.example.expensemanager.ui.screen.BudgetSettingsScreen
import com.example.expensemanager.ui.screen.CategoryExpensesScreen
import com.example.expensemanager.ui.screen.ExpenseDetailScreen
import com.example.expensemanager.ui.screen.HomeScreen
import com.example.expensemanager.ui.screen.ManageCategoriesScreen
import com.example.expensemanager.ui.screen.ReportsScreen
import com.example.expensemanager.ui.screen.SearchScreen
import com.example.expensemanager.ui.screen.SettingsScreen
import com.example.expensemanager.ui.theme.ExpenseManagerTheme
import com.example.expensemanager.util.BiometricAuthenticator
import com.example.expensemanager.viewmodel.ExpenseViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var preferenceRepository: PreferenceRepository

    @Inject
    lateinit var biometricAuthenticator: BiometricAuthenticator

    private val viewModel: ExpenseViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        biometricAuthenticator.bindActivity(this)
        enableEdgeToEdge()
        setContent {
            val isDarkTheme by preferenceRepository.isDarkTheme.collectAsState()
            val isBiometricLockEnabled by preferenceRepository.isBiometricLockEnabled.collectAsState()

            ExpenseManagerTheme(darkTheme = isDarkTheme) {
                BiometricAppGate(
                    isBiometricLockEnabled = isBiometricLockEnabled,
                    activity = this@MainActivity,
                    biometricAuthenticator = biometricAuthenticator
                ) {
                    MainContent()
                }
            }
        }
    }

    override fun onDestroy() {
        biometricAuthenticator.unbindActivity(this)
        super.onDestroy()
    }

    @Composable
    private fun MainContent() {
        val entryProvider = remember(viewModel) { appNavGraph(viewModel) }
        NavDisplay(
            backStack = viewModel.backStack,
            modifier = Modifier.fillMaxSize(),
            entryProvider = entryProvider
        )
    }

    private fun appNavGraph(viewModel: ExpenseViewModel) =
        entryProvider {
            entry<HomeScreenRoute> {
                HomeScreen(
                    viewModel = viewModel,
                    onAddExpenseClick = { viewModel.navigateTo(AddExpenseRoute) },
                    onExpenseClick = { viewModel.navigateTo(ExpenseDetailRoute(it)) },
                    onCategoryClick = { c, m, y -> viewModel.navigateTo(CategoryExpensesRoute(c, m, y)) },
                    onShowAllCategoriesClick = { m, y -> viewModel.navigateTo(AllCategoriesRoute(m, y)) },
                    onSearchClick = { viewModel.navigateTo(SearchRoute) },
                    onSettingsClick = { viewModel.navigateTo(SettingsRoute) }
                )
            }

            entry<AllCategoriesRoute> { route ->
                AllCategoriesScreen(
                    month = route.month,
                    year = route.year,
                    viewModel = viewModel,
                    onNavigateBack = viewModel::navigateBack,
                    onCategoryClick = { c, m, y -> viewModel.navigateTo(CategoryExpensesRoute(c, m, y)) }
                )
            }

            entry<CategoryExpensesRoute> { route ->
                CategoryExpensesScreen(
                    category = route.category,
                    month = route.month,
                    year = route.year,
                    viewModel = viewModel,
                    onNavigateBack = viewModel::navigateBack,
                    onExpenseClick = { viewModel.navigateTo(ExpenseDetailRoute(it)) }
                )
            }

            entry<AddExpenseRoute> {
                AddEditExpenseScreen(
                    viewModel = viewModel,
                    onNavigateBack = viewModel::navigateBack
                )
            }

            entry<EditExpenseRoute> { route ->
                AddEditExpenseScreen(
                    expenseId = route.expenseId,
                    viewModel = viewModel,
                    onNavigateBack = viewModel::navigateBack
                )
            }

            entry<ExpenseDetailRoute> { route ->
                ExpenseDetailScreen(
                    expenseId = route.expenseId,
                    viewModel = viewModel,
                    onNavigateBack = viewModel::navigateBack,
                    onEditExpense = {
                        viewModel.navigateBack()
                        viewModel.navigateTo(EditExpenseRoute(it))
                    }
                )
            }

            entry<SearchRoute> {
                SearchScreen(
                    onNavigateBack = viewModel::navigateBack,
                    onExpenseClick = { viewModel.navigateTo(ExpenseDetailRoute(it)) }
                )
            }

            entry<ReportsRoute> { ReportsScreen(onNavigateBack = viewModel::navigateBack) }
            entry<SettingsRoute> {
                SettingsScreen(
                    onNavigateBack = viewModel::navigateBack,
                    onNavigateToBudgetSettings = { viewModel.navigateTo(BudgetSettingsRoute) },
                    onNavigateToManageCategories = { viewModel.navigateTo(ManageCategoriesRoute) },
                    onNavigateToReports = { viewModel.navigateTo(ReportsRoute) }
                )
            }

            entry<BudgetSettingsRoute> {
                BudgetSettingsScreen(
                    onNavigateBack = viewModel::navigateBack
                )
            }

            entry<ManageCategoriesRoute> {
                ManageCategoriesScreen(
                    onNavigateBack = viewModel::navigateBack
                )
            }
        }
}
