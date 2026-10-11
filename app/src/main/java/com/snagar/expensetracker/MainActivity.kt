package com.snagar.expensetracker

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
import com.snagar.expensetracker.backup.AutoBackupStore
import com.snagar.expensetracker.data.PreferenceRepository
import com.snagar.expensetracker.nav.AddExpenseRoute
import com.snagar.expensetracker.nav.AllCategoriesRoute
import com.snagar.expensetracker.nav.BudgetSettingsRoute
import com.snagar.expensetracker.nav.CategoryExpensesRoute
import com.snagar.expensetracker.nav.EditExpenseRoute
import com.snagar.expensetracker.nav.ExpenseDetailRoute
import com.snagar.expensetracker.nav.HomeScreenRoute
import com.snagar.expensetracker.nav.ManageCategoriesRoute
import com.snagar.expensetracker.nav.ReportsRoute
import com.snagar.expensetracker.nav.SearchRoute
import com.snagar.expensetracker.nav.SettingsRoute
import com.snagar.expensetracker.ui.screen.AddEditExpenseScreen
import com.snagar.expensetracker.ui.screen.AllCategoriesScreen
import com.snagar.expensetracker.ui.screen.BiometricAppGate
import com.snagar.expensetracker.ui.screen.BudgetSettingsScreen
import com.snagar.expensetracker.ui.screen.CategoryExpensesScreen
import com.snagar.expensetracker.ui.screen.ExpenseDetailScreen
import com.snagar.expensetracker.ui.screen.HomeScreen
import com.snagar.expensetracker.ui.screen.ManageCategoriesScreen
import com.snagar.expensetracker.ui.screen.ReportsScreen
import com.snagar.expensetracker.ui.screen.SearchScreen
import com.snagar.expensetracker.ui.screen.SettingsScreen
import com.snagar.expensetracker.ui.theme.ExpenseTrackerTheme
import com.snagar.expensetracker.util.BiometricAuthenticator
import com.snagar.expensetracker.viewmodel.ExpenseViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var preferenceRepository: PreferenceRepository

    @Inject
    lateinit var biometricAuthenticator: BiometricAuthenticator

    @Inject
    internal lateinit var autoBackupStore: AutoBackupStore

    private val viewModel: ExpenseViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        biometricAuthenticator.bindActivity(this)
        startAutoBackup()
        enableEdgeToEdge()
        setContent {
            val isDarkTheme by preferenceRepository.isDarkTheme.collectAsState()
            val isBiometricLockEnabled by preferenceRepository.isBiometricLockEnabled.collectAsState()

            ExpenseTrackerTheme(darkTheme = isDarkTheme) {
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

    /** Restores a lost auto-backup schedule on launch. Here, not in `Application`, so worker-only wakes skip it. */
    private fun startAutoBackup() {
        autoBackupStore.ensureScheduled()
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
