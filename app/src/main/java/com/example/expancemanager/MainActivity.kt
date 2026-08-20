package com.example.expancemanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.example.expancemanager.data.PreferenceRepository
import com.example.expancemanager.nav.AddExpenseRoute
import com.example.expancemanager.nav.AllCategoriesRoute
import com.example.expancemanager.nav.BudgetSettingsRoute
import com.example.expancemanager.nav.CategoryExpensesRoute
import com.example.expancemanager.nav.EditExpenseRoute
import com.example.expancemanager.nav.ExpenseDetailRoute
import com.example.expancemanager.nav.HomeScreenRoute
import com.example.expancemanager.nav.ManageCategoriesRoute
import com.example.expancemanager.nav.ReportsRoute
import com.example.expancemanager.nav.SettingsRoute
import com.example.expancemanager.ui.screen.AddEditExpenseScreen
import com.example.expancemanager.ui.screen.AllCategoriesScreen
import com.example.expancemanager.ui.screen.BiometricAppGate
import com.example.expancemanager.ui.screen.BudgetSettingsScreen
import com.example.expancemanager.ui.screen.CategoryExpensesScreen
import com.example.expancemanager.ui.screen.ExpenseDetailScreen
import com.example.expancemanager.ui.screen.HomeScreen
import com.example.expancemanager.ui.screen.ManageCategoriesScreen
import com.example.expancemanager.ui.screen.ReportsScreen
import com.example.expancemanager.ui.screen.SettingsScreen
import com.example.expancemanager.ui.theme.ExpanseManagerTheme
import com.example.expancemanager.util.BiometricAuthenticator
import com.example.expancemanager.viewmodel.ExpenseViewModel
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

            ExpanseManagerTheme(darkTheme = isDarkTheme) {
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
        NavDisplay(
            backStack = viewModel.backStack,
            modifier = Modifier.fillMaxSize(),
            entryProvider = appNavGraph(viewModel)
        )
    }

    @Stable
    @Composable
    private fun appNavGraph(viewModel: ExpenseViewModel) =
        entryProvider {
            entry<HomeScreenRoute> {
                HomeScreen(
                    viewModel = viewModel,
                    onAddExpenseClick = { viewModel.navigateTo(AddExpenseRoute) },
                    onExpenseClick = { viewModel.navigateTo(ExpenseDetailRoute(it)) },
                    onCategoryClick = { c, m, y -> viewModel.navigateTo(CategoryExpensesRoute(c, m, y)) },
                    onShowAllCategoriesClick = { m, y -> viewModel.navigateTo(AllCategoriesRoute(m, y)) },
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
