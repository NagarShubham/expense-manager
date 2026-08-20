package com.example.expancemanager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expancemanager.R
import com.example.expancemanager.data.Expense
import com.example.expancemanager.ui.theme.AppRadius
import com.example.expancemanager.ui.theme.ExpanseManagerTheme
import com.example.expancemanager.ui.theme.appColors
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
            title = {
                Text(text = title, style = MaterialTheme.typography.titleLarge)
            },
            text = {
                Text(text = message, style = MaterialTheme.typography.bodyMedium)
            },
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
    val categoryEmoji = remember(expense.category, emojiMap) {
        ExpenseCategories.getCategoryEmoji(expense.category, emojiMap)
    }
    val formattedAmount = remember(expense.amount) {
        DateUtils.formatAmount(expense.amount, hideZeroDecimals = true)
    }
    val appColors = MaterialTheme.appColors
    val accent = remember(expense.category, appColors) { appColors.accentFor(expense.category) }
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
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = AppSpacing.tiny),
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = { SwipeDeleteBackground() }
    ) {
        AppCard(
            onClick = onExpenseClick,
            contentPadding = AppSpacing.medium
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CategoryAvatar(
                    emoji = categoryEmoji,
                    accent = accent
                )
                HSpace(AppSpacing.medium)
                ExpenseDetails(
                    title = expense.title,
                    category = expense.category,
                    date = expense.date,
                    description = expense.description,
                    showCategory = showCategory,
                    showDescription = showDescription,
                    modifier = Modifier.weight(1f)
                )
                HSpace(AppSpacing.small)
                AmountText(
                    text = stringResource(
                        R.string.amount_negative,
                        formattedAmount
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    //color = MaterialTheme.colorScheme.onSurface
                    color = MaterialTheme.colorScheme.error
                )
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
            .clip(AppRadius.card)
            .background(MaterialTheme.colorScheme.errorContainer),
        contentAlignment = Alignment.CenterEnd
    ) {
        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = stringResource(R.string.cd_swipe_to_delete),
            tint = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(end = AppSpacing.large)
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
    showDescription: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        val subtitle = remember(category, date, showCategory) {
            if (showCategory) "$category • ${DateUtils.formatDayMonth(date)}" else DateUtils.formatDate(date)
        }

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        if (showDescription && description.isNotBlank()) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Reusable empty state. The emoji sits in a soft tonal disc so an empty screen still
 * looks composed rather than unfinished.
 */
@Composable
internal fun EmptyStateMessage(
    emoji: String,
    title: String,
    subtitle: String,
    // Defaults to filling the screen; callers inside a LazyColumn item pass
    // fillMaxWidth() instead, since a lazy item's height constraint is unbounded.
    modifier: Modifier = Modifier.fillMaxSize(),
    action: @Composable (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(AppSpacing.xxlarge),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier.size(96.dp),
                shape = AppRadius.hero,
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = emoji, fontSize = 44.sp)
                }
            }
            VSpace(AppSpacing.large)
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
            VSpace(AppSpacing.small)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (action != null) {
                VSpace(AppSpacing.large)
                action()
            }
        }
    }
}

/** Primary call-to-action used by empty states. */
@Composable
internal fun EmptyStateAction(
    text: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        shape = AppRadius.pill,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = AppSpacing.xlarge,
            vertical = AppSpacing.medium
        )
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

@Preview(showBackground = true, name = "Expense row")
@Composable
private fun ExpenseItemCardPreview() {
    ExpanseManagerTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.padding(AppSpacing.screen)) {
                ExpenseItemCard(
                    expense = Expense(
                        id = 1,
                        title = "Lunch at Ovenstory",
                        amount = 420.0,
                        category = "Food & Dining",
                        description = "Team lunch",
                        date = 0L
                    ),
                    onExpenseClick = {},
                    onDeleteExpense = {},
                    emojiMap = ExpenseCategories.DEFAULT_CATEGORIES.toMap()
                )
            }
        }
    }
}
