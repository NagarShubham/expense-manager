package com.example.expancemanager.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.expancemanager.R
import com.example.expancemanager.data.Expense
import com.example.expancemanager.ui.components.DeleteConfirmationDialog
import com.example.expancemanager.util.DateUtils
import com.example.expancemanager.util.ExpenseCategories
import com.example.expancemanager.viewmodel.ExpenseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseDetailScreen(
    expenseId: Long,
    viewModel: ExpenseViewModel,
    onNavigateBack: () -> Unit = {},
    onEditExpense: (Long) -> Unit = {}
) {
    val context = LocalContext.current
    var expense by remember { mutableStateOf<Expense?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(expenseId) { expense = viewModel.getExpenseById(expenseId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.detail_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    IconButton(onClick = { expense?.let { onEditExpense(it.id) } }) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.action_edit))
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = stringResource(R.string.action_delete),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        expense?.let { exp ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(dimensionResource(R.dimen.spacing_default))
            ) {
                // Category icon
                Box(
                    modifier = Modifier
                        .size(dimensionResource(R.dimen.icon_size_large))
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .align(Alignment.CenterHorizontally),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = ExpenseCategories.getCategoryEmoji(context, exp.category),
                        fontSize = 48.sp
                    )
                }

                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_xlarge)))

                Text(
                    text = DateUtils.formatAmount(exp.amount),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_xxlarge)))

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(dimensionResource(R.dimen.spacing_default))) {
                        DetailRow(label = stringResource(R.string.detail_field_title), value = exp.title)
                        HorizontalDivider(modifier = Modifier.padding(vertical = dimensionResource(R.dimen.spacing_small)))

                        DetailRow(label = stringResource(R.string.detail_field_category), value = exp.category)
                        HorizontalDivider(modifier = Modifier.padding(vertical = dimensionResource(R.dimen.spacing_small)))

                        DetailRow(label = stringResource(R.string.detail_field_date), value = DateUtils.formatDate(exp.date))

                        if (exp.description.isNotBlank()) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = dimensionResource(R.dimen.spacing_small)))
                            DetailRow(label = stringResource(R.string.detail_field_description), value = exp.description)
                        }
                    }
                }
            }
        } ?: Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }

    DeleteConfirmationDialog(
        showDialog = showDeleteDialog,
        onDismiss = { showDeleteDialog = false },
        onConfirm = {
            expense?.let {
                viewModel.deleteExpense(it)
                onNavigateBack()
            }
        },
        message = stringResource(R.string.detail_delete_confirmation)
    )
}

@Composable
fun DetailRow(
    label: String,
    value: String
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_tiny)))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
    }
}
