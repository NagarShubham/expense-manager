package com.example.expancemanager.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expancemanager.R
import com.example.expancemanager.data.Expense
import com.example.expancemanager.ui.components.AmountText
import com.example.expancemanager.ui.components.AppBackTopBar
import com.example.expancemanager.ui.components.AppCard
import com.example.expancemanager.ui.components.AppSpacing
import com.example.expancemanager.ui.components.CategoryAvatar
import com.example.expancemanager.ui.components.DeleteConfirmationDialog
import com.example.expancemanager.ui.components.HSpace
import com.example.expancemanager.ui.components.HeroAmountText
import com.example.expancemanager.ui.components.HeroGradientCard
import com.example.expancemanager.ui.components.OverlineText
import com.example.expancemanager.ui.components.SectionHeader
import com.example.expancemanager.ui.components.VSpace
import com.example.expancemanager.ui.theme.AppRadius
import com.example.expancemanager.ui.theme.appColors
import com.example.expancemanager.util.DateUtils
import com.example.expancemanager.util.ExpenseCategories
import com.example.expancemanager.viewmodel.ExpenseViewModel

private data class ExpenseDetailField(
    val iconEmoji: String,
    val labelRes: Int,
    val value: String
)

@Composable
internal fun ExpenseDetailScreen(
    expenseId: Long,
    viewModel: ExpenseViewModel,
    onNavigateBack: () -> Unit = {},
    onEditExpense: (Long) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val expenseFromMonth = remember(expenseId, uiState.expenses) {
        uiState.expenses.firstOrNull { it.id == expenseId }
    }
    var expense by remember { mutableStateOf<Expense?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(expenseId, expenseFromMonth) {
        expense = expenseFromMonth ?: viewModel.getExpenseById(expenseId)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppBackTopBar(
                title = stringResource(R.string.detail_title),
                onNavigateBack = onNavigateBack
            )
        },
        bottomBar = {
            expense?.let { exp ->
                ExpenseDetailBottomBar(
                    onEdit = { onEditExpense(exp.id) },
                    onDelete = { showDeleteDialog = true }
                )
            }
        }
    ) { paddingValues ->
        val exp = expense
        if (exp == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            ExpenseDetailContent(
                expense = exp,
                categoryEmoji = remember(exp.category, uiState.categoryEmojiMap) {
                    ExpenseCategories.getCategoryEmoji(exp.category, uiState.categoryEmojiMap)
                },
                formattedAmount = remember(exp.amount) { DateUtils.formatAmount(exp.amount) },
                formattedDate = remember(exp.date) { DateUtils.formatDate(exp.date) },
                contentPadding = paddingValues
            )
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
private fun ExpenseDetailContent(
    expense: Expense,
    categoryEmoji: String,
    formattedAmount: String,
    formattedDate: String,
    contentPadding: PaddingValues
) {
    val fields = remember(expense, categoryEmoji, formattedDate) {
        buildList {
            add(ExpenseDetailField("📝", R.string.detail_field_title, expense.title))
            add(ExpenseDetailField(categoryEmoji, R.string.detail_field_category, expense.category))
            add(ExpenseDetailField("📅", R.string.detail_field_date, formattedDate))
            if (expense.description.isNotBlank()) {
                add(ExpenseDetailField("💬", R.string.detail_field_description, expense.description))
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = AppSpacing.screen)
            .padding(bottom = AppSpacing.default)
    ) {
        ExpenseDetailHeroCard(
            categoryEmoji = categoryEmoji,
            title = expense.title,
            amount = formattedAmount,
            category = expense.category,
            date = formattedDate
        )

        VSpace(AppSpacing.large)

        SectionHeader(title = stringResource(R.string.detail_section_info))

        // One accent for the whole card — the expense's own category color — so the
        // rows read as a single record rather than four unrelated chips.
        val accent = MaterialTheme.appColors.accentFor(expense.category)
        AppCard(contentPadding = 0.dp) {
            fields.forEachIndexed { index, field ->
                ExpenseDetailInfoRow(
                    iconEmoji = field.iconEmoji,
                    label = stringResource(field.labelRes),
                    value = field.value,
                    accent = accent,
                    showDivider = index > 0
                )
            }
        }
    }
}

@Composable
private fun ExpenseDetailHeroCard(
    categoryEmoji: String,
    title: String,
    amount: String,
    category: String,
    date: String
) {
    val appColors = MaterialTheme.appColors
    HeroGradientCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(AppRadius.card)
                    .background(appColors.onHero.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = categoryEmoji, fontSize = 34.sp)
            }

            VSpace(AppSpacing.default)

            OverlineText(
                text = stringResource(R.string.detail_title),
                color = appColors.onHeroMuted
            )

            VSpace(AppSpacing.small)

            HeroAmountText(
                text = amount,
                style = MaterialTheme.typography.displaySmall,
                color = appColors.onHero
            )

            VSpace(AppSpacing.small)

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = appColors.onHero,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            VSpace(AppSpacing.default)

            Row(
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.small),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ExpenseDetailChip(text = category)
                ExpenseDetailChip(text = date)
            }
        }
    }
}

@Composable
private fun ExpenseDetailChip(text: String) {
    val appColors = MaterialTheme.appColors
    Surface(
        shape = AppRadius.pill,
        color = appColors.onHero.copy(alpha = 0.18f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = appColors.onHero,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = AppSpacing.medium, vertical = 6.dp)
        )
    }
}

@Composable
private fun ExpenseDetailInfoRow(
    iconEmoji: String,
    label: String,
    value: String,
    accent: Color,
    showDivider: Boolean
) {
    if (showDivider) {
        HorizontalDivider(
            modifier = Modifier.padding(start = 72.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(AppSpacing.default),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CategoryAvatar(emoji = iconEmoji, accent = accent, size = 40.dp, emojiSize = 18.dp)
        HSpace(AppSpacing.default)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            VSpace(2.dp)
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ExpenseDetailBottomBar(
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.screen, vertical = AppSpacing.medium),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.medium)
        ) {
            ExpenseDetailBottomBarButton(
                text = stringResource(R.string.action_delete),
                icon = Icons.Default.Delete,
                onClick = onDelete,
                modifier = Modifier.weight(1f),
                outlined = true,
                contentColor = MaterialTheme.colorScheme.error,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
            )
            ExpenseDetailBottomBarButton(
                text = stringResource(R.string.action_edit),
                icon = Icons.Default.Edit,
                onClick = onEdit,
                modifier = Modifier.weight(1f),
                outlined = false
            )
        }
    }
}

@Composable
private fun ExpenseDetailBottomBarButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    outlined: Boolean,
    contentColor: Color = MaterialTheme.colorScheme.primary,
    border: BorderStroke? = null
) {
    val content: @Composable RowScope.() -> Unit = {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        HSpace(AppSpacing.small)
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
    if (outlined) {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.height(52.dp),
            shape = AppRadius.pill,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = contentColor),
            border = border,
            content = content
        )
    } else {
        Button(
            onClick = onClick,
            modifier = modifier.height(52.dp),
            shape = AppRadius.pill,
            content = content
        )
    }
}
