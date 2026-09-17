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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.expensemanager.R
import com.example.expensemanager.data.Expense
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
import com.example.expensemanager.viewmodel.ExpenseViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddEditExpenseScreen(
    expenseId: Long? = null,
    viewModel: ExpenseViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val categories = remember(uiState.categories) { uiState.categories.map { it.name } }
    val emojiMap = uiState.categoryEmojiMap
    // rememberSaveable so typed input survives rotation / process death.
    var title by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("") }
    // Category state is independent of the (async-loading) list so a late emission
    // never clobbers an edit-loaded or user-picked value.
    var category by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var selectedDate by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }
    // One-shot guard so defaulting the category never re-runs on a later categories emission.
    var categoryInitialized by rememberSaveable { mutableStateOf(false) }
    var showCategoryMenu by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val isEditMode = expenseId != null
    val isValid = title.isNotBlank() && (amount.toDoubleOrNull() ?: 0.0) > 0

    // Default the picker to the first category once the list loads (add mode only, once).
    LaunchedEffect(categories) {
        if (!isEditMode && !categoryInitialized && categories.isNotEmpty()) {
            category = categories.first()
            categoryInitialized = true
        }
    }

    // Load expense if editing
    LaunchedEffect(expenseId) {
        expenseId?.let { id ->
            viewModel.getExpenseById(id)?.let { expense ->
                title = expense.title
                amount = DateUtils.formatAmountForInput(expense.amount)
                category = expense.category
                description = expense.description
                selectedDate = expense.date
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppBackTopBar(
                title = if (isEditMode) {
                    stringResource(R.string.expense_edit_title)
                } else {
                    stringResource(R.string.expense_add_title)
                },
                onNavigateBack = onNavigateBack
            )
        },
        bottomBar = {
            SaveExpenseBar(
                label = if (isEditMode) {
                    stringResource(R.string.expense_button_update)
                } else {
                    stringResource(R.string.expense_button_add)
                },
                enabled = isValid,
                onClick = {
                    scope.launch {
                        amount.toDoubleOrNull()?.let { amountValue ->
                            val expense = Expense(
                                id = expenseId ?: 0,
                                title = title.trim(),
                                amount = amountValue,
                                category = category,
                                description = description.trim(),
                                date = selectedDate
                            )

                            if (isEditMode) {
                                viewModel.updateExpense(expense)
                            } else {
                                viewModel.insertExpense(expense)
                            }
                            onNavigateBack()
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AppSpacing.screen)
        ) {
            HeroAmountInput(
                label = stringResource(R.string.expense_field_amount),
                amountText = amount,
                onAmountChange = { new -> sanitizeAmountInput(new)?.let { amount = it } }
            )

            VSpace(AppSpacing.large)

            SectionHeader(title = stringResource(R.string.expense_section_details))

            AppCard(contentPadding = AppSpacing.default) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.expense_field_title)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppRadius.chip,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    singleLine = true,
                    colors = appTextFieldColors()
                )

                VSpace(AppSpacing.medium)

                // Category dropdown
                ExposedDropdownMenuBox(
                    expanded = showCategoryMenu,
                    onExpandedChange = { showCategoryMenu = it }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.expense_field_category)) },
                        shape = AppRadius.chip,
                        colors = appTextFieldColors(),
                        trailingIcon = {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                        leadingIcon = {
                            CategoryAvatar(
                                emoji = ExpenseCategories.getCategoryEmoji(category, emojiMap),
                                accent = MaterialTheme.appColors.accentFor(category),
                                size = 32.dp,
                                emojiSize = 16.dp
                            )
                        }
                    )
                    ExposedDropdownMenu(
                        expanded = showCategoryMenu,
                        onDismissRequest = { showCategoryMenu = false },
                        shape = AppRadius.card
                    ) {
                        categories.forEach { cat ->
                            key(cat) {
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            CategoryAvatar(
                                                emoji = ExpenseCategories.getCategoryEmoji(cat, emojiMap),
                                                accent = MaterialTheme.appColors.accentFor(cat),
                                                size = 32.dp,
                                                emojiSize = 16.dp
                                            )
                                            HSpace(AppSpacing.medium)
                                            Text(cat, style = MaterialTheme.typography.bodyLarge)
                                        }
                                    },
                                    onClick = {
                                        category = cat
                                        showCategoryMenu = false
                                    }
                                )
                            }
                        }
                    }
                }

                VSpace(AppSpacing.medium)

                DateRow(
                    date = selectedDate,
                    onClick = { showDatePicker = true }
                )

                VSpace(AppSpacing.medium)

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.expense_field_description)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    shape = AppRadius.chip,
                    colors = appTextFieldColors(),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    maxLines = 4
                )
            }

            VSpace(AppSpacing.large)
        }
    }

    // Date picker dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { selectedDate = it }
                    showDatePicker = false
                }) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        ) { DatePicker(state = datePickerState) }
    }
}

@Composable
private fun DateRow(
    date: Long,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = AppRadius.chip,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.default, vertical = AppSpacing.medium),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.expense_field_date),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                VSpace(2.dp)
                Text(
                    text = remember(date) { DateUtils.formatDate(date) },
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            CategoryAvatar(
                emoji = "📅",
                accent = MaterialTheme.colorScheme.primary,
                size = 36.dp,
                emojiSize = 18.dp
            )
        }
    }
}

/** Sticky save action so the primary button never scrolls out of reach. */
@Composable
private fun SaveExpenseBar(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        tonalElevation = 0.dp
    ) {
        Button(
            onClick = onClick,
            enabled = enabled,
            shape = AppRadius.pill,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.screen, vertical = AppSpacing.medium)
                .height(56.dp)
        ) {
            Text(text = label, style = MaterialTheme.typography.labelLarge)
        }
    }
}
