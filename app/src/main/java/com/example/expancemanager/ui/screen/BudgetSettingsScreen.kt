package com.example.expancemanager.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.expancemanager.R
import com.example.expancemanager.ui.components.AppBackTopBar
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
        onExpandedChange = onExpandedChange
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
            modifier = modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
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
        topBar = {
            AppBackTopBar(
                title = stringResource(R.string.budget_screen_title),
                onNavigateBack = onNavigateBack,
                backContentDescription = stringResource(R.string.cd_navigate_back),
                titleFontWeight = FontWeight.Normal
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(dimensionResource(R.dimen.spacing_default))
        ) {
            Text(
                text = stringResource(R.string.settings_budget_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = dimensionResource(R.dimen.spacing_medium))
            )

            BudgetMonthYearDropdown(
                label = stringResource(R.string.budget_month),
                value = DateUtils.formatMonthYear(selectedMonth, selectedYear),
                expanded = showMonthMenu,
                onExpandedChange = { showMonthMenu = it },
                modifier = Modifier.fillMaxWidth()
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

            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_default)))

            BudgetMonthYearDropdown(
                label = stringResource(R.string.budget_year),
                value = selectedYear.toString(),
                expanded = showYearMenu,
                onExpandedChange = { showYearMenu = it },
                modifier = Modifier.fillMaxWidth()
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

            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_default)))

            OutlinedTextField(
                value = amountText,
                onValueChange = { new ->
                    val f = new.filter { c -> c.isDigit() || c == '.' }
                    if (f.count { it == '.' } <= 1) amountText = f
                },
                label = { Text(stringResource(R.string.budget_dialog_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_xlarge)))

            val amount = amountText.toDoubleOrNull() ?: 0.0
            val hasExistingBudget = amountText.isNotBlank() && amount > 0

            Button(
                onClick = {
                    if (amount <= 0) {
                        context.showShortToast(context.getString(R.string.budget_invalid_amount))
                        return@Button
                    }
                    scope.launch {
                        viewModel.setMonthlyBudgetAndWait(selectedMonth, selectedYear, amount)
                        context.showShortToast(context.getString(R.string.budget_saved))
                        onNavigateBack()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (hasExistingBudget) {
                        stringResource(R.string.budget_dialog_save)
                    } else {
                        stringResource(R.string.budget_set_budget)
                    }
                )
            }

            if (hasExistingBudget) {
                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_default)))
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            viewModel.clearMonthlyBudget(selectedMonth, selectedYear)
                            amountText = ""
                            context.showShortToast(context.getString(R.string.budget_cleared))
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.budget_clear))
                }
            }

            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_xlarge)))

            // Categories excluded from budget
            Text(
                text = stringResource(R.string.budget_excluded_categories_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = dimensionResource(R.dimen.spacing_small))
            )
            Text(
                text = stringResource(R.string.budget_excluded_categories_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = dimensionResource(R.dimen.spacing_medium))
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(dimensionResource(R.dimen.spacing_default))) {
                    allCategories.forEach { cat ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = cat in excludedCategoryNames,
                                onCheckedChange = { viewModel.setCategoryExcludedFromBudget(selectedMonth, selectedYear, cat, it) }
                            )
                            Text(
                                text = "${ExpenseCategories.getCategoryEmoji(cat, emojiMap)} $cat",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = dimensionResource(R.dimen.spacing_small))
                            )
                        }
                    }
                }
            }
        }
    }
}
