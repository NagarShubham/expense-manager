package com.example.expancemanager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.example.expancemanager.R
import com.example.expancemanager.data.Expense
import com.example.expancemanager.util.DateUtils
import com.example.expancemanager.util.ExpenseCategories

/**
 * Reusable confirmation dialog for deleting expenses
 */
@Composable
fun DeleteConfirmationDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    title: String = stringResource(R.string.delete_dialog_title),
    message: String = stringResource(R.string.delete_dialog_message)
) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(title) },
            text = { Text(message) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onConfirm()
                        onDismiss()
                    }
                ) {
                    Text(stringResource(R.string.delete_dialog_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.delete_dialog_cancel))
                }
            }
        )
    }
}

/**
 * Reusable expense item card used across different screens
 * Optimized for smooth scrolling in LazyColumn
 */
@Composable
fun ExpenseItemCard(
    expense: Expense,
    onExpenseClick: () -> Unit,
    onDeleteExpense: () -> Unit,
    showCategory: Boolean = true,
    showDescription: Boolean = false,
    modifier: Modifier = Modifier
) {
    var showDeleteDialog by remember(expense.id) { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = dimensionResource(R.dimen.spacing_tiny))
            .clickable { onExpenseClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = dimensionResource(R.dimen.card_elevation_default))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.spacing_default)),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Category emoji icon
                CategoryIcon(category = expense.category)

                Spacer(modifier = Modifier.width(dimensionResource(R.dimen.spacing_medium)))

                // Expense details
                ExpenseDetails(
                    title = expense.title,
                    category = expense.category,
                    date = expense.date,
                    description = expense.description,
                    showCategory = showCategory,
                    showDescription = showDescription
                )
            }

            // Amount and delete button
            ExpenseActions(
                amount = expense.amount,
                onDeleteClick = { showDeleteDialog = true }
            )
        }
    }
    DeleteConfirmationDialog(
        showDialog = showDeleteDialog,
        onDismiss = { showDeleteDialog = false },
        onConfirm = onDeleteExpense
    )
}

@Composable
private fun CategoryIcon(category: String) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .size(dimensionResource(R.dimen.icon_size_large))
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = ExpenseCategories.getCategoryEmoji(context, category),
            fontSize = 24.sp
        )
    }
}

@Composable
private fun ExpenseDetails(
    title: String,
    category: String,
    date: Long,
    description: String,
    showCategory: Boolean,
    showDescription: Boolean
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )

        val subtitle = remember(category, date, showCategory) {
            if (showCategory) "$category • ${DateUtils.formatDayMonth(date)}" else DateUtils.formatDate(date)
        }

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )

        if (showDescription && description.isNotBlank()) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ExpenseActions(
    amount: Double,
    onDeleteClick: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = remember(amount) { DateUtils.formatAmount(amount) },
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error
        )

        IconButton(onClick = onDeleteClick) {
            Icon(
                Icons.Default.Delete,
                contentDescription = stringResource(R.string.action_delete),
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

/**
 * Reusable empty state message
 */
@Composable
fun EmptyStateMessage(
    emoji: String,
    title: String,
    subtitle: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(dimensionResource(R.dimen.spacing_xxlarge)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = emoji,
                fontSize = 64.sp
            )
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_default)))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_small)))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}
