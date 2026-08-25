package com.example.expancemanager.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.expancemanager.R
import com.example.expancemanager.ui.components.AppBackTopBar
import com.example.expancemanager.ui.components.AppCard
import com.example.expancemanager.ui.components.AppSpacing
import com.example.expancemanager.ui.components.CategoryAvatar
import com.example.expancemanager.ui.components.HSpace
import com.example.expancemanager.ui.components.HeroGradientCard
import com.example.expancemanager.ui.components.OverlineText
import com.example.expancemanager.ui.components.SectionHeader
import com.example.expancemanager.ui.components.VSpace
import com.example.expancemanager.ui.theme.AppRadius
import com.example.expancemanager.ui.theme.TabularFigures
import com.example.expancemanager.ui.theme.appColors
import com.example.expancemanager.util.DateUtils
import com.example.expancemanager.util.ExpenseCategories
import com.example.expancemanager.util.showShortToast
import com.example.expancemanager.viewmodel.BudgetViewModel
import com.example.expancemanager.viewmodel.CategoryViewModel
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
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.Transparent
            ),
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
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
            .takeIf { it != null && it > 0 }
            ?.toString()
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
            BudgetAmountHero(
                amountText = amountText,
                onAmountChange = { new ->
                    val filtered = new.filter { c -> c.isDigit() || c == '.' }
                    if (filtered.count { it == '.' } <= 1) amountText = filtered
                }
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

/** The budget amount gets the hero treatment, same as the amount on Add Expense. */
@Composable
private fun BudgetAmountHero(
    amountText: String,
    onAmountChange: (String) -> Unit
) {
    val appColors = MaterialTheme.appColors
    HeroGradientCard {
        OverlineText(
            text = stringResource(R.string.budget_dialog_hint),
            color = appColors.onHeroMuted
        )
        VSpace(AppSpacing.small)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = stringResource(R.string.currency_symbol),
                style = MaterialTheme.typography.headlineMedium,
                color = appColors.onHeroMuted,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            HSpace(AppSpacing.small)
            Box(modifier = Modifier.weight(1f)) {
                if (amountText.isEmpty()) {
                    Text(
                        text = stringResource(R.string.expense_amount_placeholder),
                        style = MaterialTheme.typography.displaySmall.merge(TabularFigures),
                        color = appColors.onHero.copy(alpha = 0.4f)
                    )
                }
                BasicTextField(
                    value = amountText,
                    onValueChange = onAmountChange,
                    textStyle = MaterialTheme.typography.displaySmall
                        .merge(TabularFigures)
                        .copy(color = appColors.onHero),
                    singleLine = true,
                    cursorBrush = SolidColor(appColors.onHero),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
