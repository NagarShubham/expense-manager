package com.example.expancemanager.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.example.expancemanager.R
import com.example.expancemanager.data.Expense
import com.example.expancemanager.util.DateUtils
import com.example.expancemanager.util.ExpenseCategories
import com.example.expancemanager.viewmodel.ExpenseViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditExpenseScreen(
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
                amount = expense.amount.toString()
                category = expense.category
                description = expense.description
                selectedDate = expense.date
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isEditMode) stringResource(R.string.expense_edit_title) else stringResource(R.string.expense_add_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(dimensionResource(R.dimen.spacing_default))
                .verticalScroll(rememberScrollState())
        ) {
            // Title field
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(R.string.expense_field_title)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_default)))

            // Amount field
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text(stringResource(R.string.expense_field_amount)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                prefix = { Text(stringResource(R.string.currency_symbol)) },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_default)))

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
                    trailingIcon = {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    leadingIcon = {
                        Text(
                            text = ExpenseCategories.getCategoryEmoji(category, emojiMap),
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                )
                ExposedDropdownMenu(
                    expanded = showCategoryMenu,
                    onDismissRequest = { showCategoryMenu = false }
                ) {
                    categories.forEach { cat ->
                        key(cat) {
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = ExpenseCategories.getCategoryEmoji(cat, emojiMap),
                                            modifier = Modifier.padding(end = dimensionResource(R.dimen.spacing_small))
                                        )
                                        Text(cat)
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

            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_default)))

            // Date picker button
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(dimensionResource(R.dimen.spacing_default)),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.expense_field_date),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_tiny)))
                        Text(
                            text = DateUtils.formatDate(selectedDate),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    Text(
                        text = "📅",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }

            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_default)))

            // Description field
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(stringResource(R.string.expense_field_description)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dimensionResource(R.dimen.text_field_height_multiline)),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                maxLines = 4
            )

            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_xxlarge)))

            // Save button
            Button(
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
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dimensionResource(R.dimen.button_height_default)),
                enabled = isValid
            ) {
                Text(if (isEditMode) stringResource(R.string.expense_button_update) else stringResource(R.string.expense_button_add))
            }
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
