package com.example.expancemanager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expancemanager.R
import com.example.expancemanager.data.Expense
import com.example.expancemanager.util.DateUtils
import com.example.expancemanager.util.ExpenseCategories

/**
 * Reusable confirmation dialog for deleting expenses
 */
@Composable
internal fun DeleteConfirmationDialog(
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
 * Expense row used on Home and category lists.
 * Swiping end-to-start (or tapping delete) opens a confirmation dialog;
 * the expense is removed only after confirm.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExpenseItemCard(
    expense: Expense,
    onExpenseClick: () -> Unit,
    onDeleteExpense: () -> Unit,
    emojiMap: Map<String, String>,
    showCategory: Boolean = true,
    showDescription: Boolean = false,
    modifier: Modifier = Modifier
) {
    var showDeleteDialog by remember(expense.id) { mutableStateOf(false) }
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                showDeleteDialog = true
            }
            false
        },
        positionalThreshold = { fullWidth -> fullWidth * 0.5f }
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier.fillMaxWidth(),
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = { SwipeDeleteBackground() }
    ) {
        Card(
            modifier = Modifier
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
                    CategoryIcon(category = expense.category, emojiMap = emojiMap)
                    Spacer(modifier = Modifier.width(dimensionResource(R.dimen.spacing_medium)))
                    ExpenseDetails(
                        title = expense.title,
                        category = expense.category,
                        date = expense.date,
                        description = expense.description,
                        showCategory = showCategory,
                        showDescription = showDescription
                    )
                }
                ExpenseActions(amount = expense.amount)
            }
        }
    }
    DeleteConfirmationDialog(
        showDialog = showDeleteDialog,
        onDismiss = { showDeleteDialog = false },
        onConfirm = onDeleteExpense
    )
}

@Composable
private fun SwipeDeleteBackground() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = dimensionResource(R.dimen.spacing_tiny))
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.errorContainer),
        contentAlignment = Alignment.CenterEnd
    ) {
        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = stringResource(R.string.cd_swipe_to_delete),
            tint = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(end = dimensionResource(R.dimen.spacing_default))
        )
    }
}

@Composable
private fun CategoryIcon(
    category: String,
    emojiMap: Map<String, String>
) {
    Box(
        modifier = Modifier
            .size(dimensionResource(R.dimen.icon_size_large))
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = ExpenseCategories.getCategoryEmoji(category, emojiMap),
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
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = remember(amount) { DateUtils.formatAmount(amount, hideZeroDecimals = true) },
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error
        )
    }
}

/**
 * Reusable empty state message
 */
@Composable
internal fun EmptyStateMessage(
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
