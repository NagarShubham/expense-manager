package com.example.expensemanager.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.expensemanager.R
import com.example.expensemanager.ui.components.AppBackTopBar
import com.example.expensemanager.ui.components.AppCard
import com.example.expensemanager.ui.components.AppSpacing
import com.example.expensemanager.ui.components.CategoryAvatar
import com.example.expensemanager.ui.components.HSpace
import com.example.expensemanager.ui.components.HeroAmountInput
import com.example.expensemanager.ui.components.SectionHeader
import com.example.expensemanager.ui.components.VSpace
import com.example.expensemanager.ui.components.appTextFieldColors
import com.example.expensemanager.ui.components.sanitizeAmountInput
import com.example.expensemanager.ui.theme.AppRadius
import com.example.expensemanager.ui.theme.appColors
import com.example.expensemanager.util.DateUtils
import com.example.expensemanager.util.ExpenseCategories
import com.example.expensemanager.util.showShortToast
import com.example.expensemanager.viewmodel.BudgetViewModel
import com.example.expensemanager.viewmodel.CategoryViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BudgetMonthYearDropdown(
    label: String,
    value: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = modifier
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            shape = AppRadius.chip,
            singleLine = true,
            colors = appTextFieldColors(),
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) }
        ) {
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BudgetSettingsScreen(
    viewModel: BudgetViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val categoryViewModel: CategoryViewModel = hiltViewModel()
    val categories by categoryViewModel.categories.collectAsState()
    var excludedCategoryNames by remember { mutableStateOf<Set<String>>(emptySet()) }
    val allCategories = remember(categories) { categories.map { it.name } }
    val emojiMap = remember(categories) { categories.associate { it.name to it.emoji } }
    val (currentMonth, currentYear) = remember { DateUtils.currentMonthYear() }
    var selectedMonth by rememberSaveable { mutableStateOf(currentMonth) }
    var selectedYear by rememberSaveable { mutableStateOf(currentYear) }
    var amountText by rememberSaveable { mutableStateOf("") }
    var showMonthMenu by remember { mutableStateOf(false) }
    var showYearMenu by remember { mutableStateOf(false) }

    val monthOptions = remember { 0..11 }
    val yearRange = remember(currentYear) { (currentYear - 2)..(currentYear + 1) }

    // One-shot load of the expected amount when the month/year changes.
    LaunchedEffect(selectedMonth, selectedYear) {
        amountText = viewModel
            .getExpectedBudgetForMonth(selectedMonth, selectedYear)
            ?.takeIf { it > 0 }
            ?.let { DateUtils.formatAmountForInput(it) }
            .orEmpty()
    }

    // Separate, continuous collection of the excluded-category set for the month/year.
    LaunchedEffect(selectedMonth, selectedYear) {
        viewModel.getExcludedByMonthYear(selectedMonth, selectedYear).collectLatest {
            excludedCategoryNames = it.toSet()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppBackTopBar(
                title = stringResource(R.string.budget_screen_title),
                subtitle = DateUtils.formatMonthYear(selectedMonth, selectedYear),
                onNavigateBack = onNavigateBack,
                backContentDescription = stringResource(R.string.cd_navigate_back)
            )
        }
    ) { paddingValues ->
        val amount = amountText.toDoubleOrNull() ?: 0.0
        val hasExistingBudget = amountText.isNotBlank() && amount > 0

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AppSpacing.screen)
                .padding(bottom = AppSpacing.xlarge)
        ) {
            HeroAmountInput(
                label = stringResource(R.string.budget_dialog_hint),
                amountText = amountText,
                onAmountChange = { new -> sanitizeAmountInput(new)?.let { amountText = it } }
            )

            VSpace(AppSpacing.default)

            Text(
                text = stringResource(R.string.settings_budget_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            VSpace(AppSpacing.large)

            SectionHeader(title = stringResource(R.string.budget_period_section))

            AppCard(contentPadding = AppSpacing.default) {
                Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.medium)) {
                    BudgetMonthYearDropdown(
                        label = stringResource(R.string.budget_month),
                        value = DateUtils.formatMonthYear(selectedMonth, selectedYear),
                        expanded = showMonthMenu,
                        onExpandedChange = { showMonthMenu = it },
                        modifier = Modifier.weight(1.4f)
                    ) {
                        monthOptions.forEach { month ->
                            key(month) {
                                DropdownMenuItem(
                                    text = { Text(DateUtils.formatMonthYear(month, selectedYear)) },
                                    onClick = {
                                        selectedMonth = month
                                        showMonthMenu = false
                                    }
                                )
                            }
                        }
                    }

                    BudgetMonthYearDropdown(
                        label = stringResource(R.string.budget_year),
                        value = selectedYear.toString(),
                        expanded = showYearMenu,
                        onExpandedChange = { showYearMenu = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        yearRange.forEach { year ->
                            key(year) {
                                DropdownMenuItem(
                                    text = { Text(year.toString()) },
                                    onClick = {
                                        selectedYear = year
                                        showYearMenu = false
                                    }
                                )
                            }
                        }
                    }
                }

                VSpace(AppSpacing.default)

                Button(
                    onClick = {
                        if (amount <= 0) {
                            context.showShortToast(resources.getString(R.string.budget_invalid_amount))
                            return@Button
                        }
                        scope.launch {
                            viewModel.setMonthlyBudgetAndWait(selectedMonth, selectedYear, amount)
                            context.showShortToast(resources.getString(R.string.budget_saved))
                            onNavigateBack()
                        }
                    },
                    shape = AppRadius.pill,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text(
                        text = if (hasExistingBudget) {
                            stringResource(R.string.budget_dialog_save)
                        } else {
                            stringResource(R.string.budget_set_budget)
                        },
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                if (hasExistingBudget) {
                    VSpace(AppSpacing.small)
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                viewModel.clearMonthlyBudget(selectedMonth, selectedYear)
                                amountText = ""
                                context.showShortToast(resources.getString(R.string.budget_cleared))
                            }
                        },
                        shape = AppRadius.pill,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.budget_clear),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            VSpace(AppSpacing.large)

            // Categories excluded from budget
            SectionHeader(title = stringResource(R.string.budget_excluded_categories_title))
            Text(
                text = stringResource(R.string.budget_excluded_categories_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = AppSpacing.medium)
            )

            AppCard(contentPadding = AppSpacing.small) {
                allCategories.forEach { cat ->
                    key(cat) {
                        val isExcluded = cat in excludedCategoryNames
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(AppRadius.chip)
                                .clickable {
                                    viewModel.setCategoryExcludedFromBudget(
                                        selectedMonth,
                                        selectedYear,
                                        cat,
                                        !isExcluded
                                    )
                                }
                                .padding(horizontal = AppSpacing.small, vertical = AppSpacing.small),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CategoryAvatar(
                                emoji = ExpenseCategories.getCategoryEmoji(cat, emojiMap),
                                accent = MaterialTheme.appColors.accentFor(cat),
                                size = 36.dp,
                                emojiSize = 18.dp
                            )
                            HSpace(AppSpacing.medium)
                            Text(
                                text = cat,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f)
                            )
                            Checkbox(
                                checked = isExcluded,
                                onCheckedChange = {
                                    viewModel.setCategoryExcludedFromBudget(
                                        selectedMonth,
                                        selectedYear,
                                        cat,
                                        it
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
