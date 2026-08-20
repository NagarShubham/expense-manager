package com.example.expancemanager.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expancemanager.R
import com.example.expancemanager.data.Expense
import com.example.expancemanager.ui.components.DeleteConfirmationDialog
import com.example.expancemanager.util.DateUtils
import com.example.expancemanager.util.ExpenseCategories
import com.example.expancemanager.viewmodel.ExpenseViewModel

private object ExpenseDetailDimens {
    val heroIconSize = 72.dp
    val rowIconSize = 40.dp
    val dividerInset = 72.dp
}

private object ExpenseDetailShapes {
    val heroCard = RoundedCornerShape(20.dp)
    val sectionCard = RoundedCornerShape(16.dp)
    val icon = RoundedCornerShape(12.dp)
    val chip = RoundedCornerShape(12.dp)
}

private data class ExpenseDetailField(
    val iconEmoji: String,
    val labelRes: Int,
    val value: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExpenseDetailScreen(
    expenseId: Long,
    viewModel: ExpenseViewModel,
    onNavigateBack: () -> Unit = {},
    onEditExpense: (Long) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var expense by remember { mutableStateOf<Expense?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(expenseId) { expense = viewModel.getExpenseById(expenseId) }

    Scaffold(
        topBar = {
            ExpenseDetailTopBar(onNavigateBack = onNavigateBack)
        },
        bottomBar = {
            expense?.let { exp ->
                ExpenseDetailBottomBar(
                    onEdit = { onEditExpense(exp.id) },
                    onDelete = { showDeleteDialog = true }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpenseDetailTopBar(onNavigateBack: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(R.string.detail_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.action_back)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
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
    val defaultPadding = dimensionResource(R.dimen.spacing_default)
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
            .padding(horizontal = defaultPadding)
            .padding(bottom = defaultPadding)
    ) {
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_small)))

        ExpenseDetailHeroCard(
            categoryEmoji = categoryEmoji,
            title = expense.title,
            amount = formattedAmount,
            category = expense.category,
            date = formattedDate
        )

        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_xlarge)))

        ExpenseDetailSectionTitle(text = stringResource(R.string.detail_section_info))

        ExpenseDetailSectionCard {
            fields.forEachIndexed { index, field ->
                ExpenseDetailInfoRow(
                    iconEmoji = field.iconEmoji,
                    label = stringResource(field.labelRes),
                    value = field.value,
                    showDivider = index > 0
                )
            }
        }
    }
}

@Composable
private fun ExpenseDetailSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(
            start = dimensionResource(R.dimen.spacing_tiny),
            bottom = dimensionResource(R.dimen.spacing_small)
        )
    )
}

@Composable
private fun ExpenseDetailSectionCard(content: @Composable ColumnScope.() -> Unit) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = ExpenseDetailShapes.sectionCard,
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    ) {
        Column(content = content)
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
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = ExpenseDetailShapes.heroCard,
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.spacing_xlarge)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ExpenseDetailEmojiIcon(
                emoji = categoryEmoji,
                size = ExpenseDetailDimens.heroIconSize,
                fontSize = 36.sp,
                shape = ExpenseDetailShapes.heroCard
            )

            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_default)))

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_small)))

            Text(
                text = amount,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )

            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_default)))

            Row(
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ExpenseDetailChip(text = category)
                ExpenseDetailChip(text = date)
            }
        }
    }
}

@Composable
private fun ExpenseDetailEmojiIcon(
    emoji: String,
    size: Dp,
    fontSize: TextUnit,
    shape: Shape = ExpenseDetailShapes.icon,
    containerColor: Color = MaterialTheme.colorScheme.surface
) {
    Surface(
        modifier = Modifier.size(size),
        shape = shape,
        color = containerColor
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = emoji, fontSize = fontSize)
        }
    }
}

@Composable
private fun ExpenseDetailChip(text: String) {
    Surface(
        shape = ExpenseDetailShapes.chip,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(
                horizontal = dimensionResource(R.dimen.spacing_medium),
                vertical = dimensionResource(R.dimen.spacing_tiny)
            )
        )
    }
}

@Composable
private fun ExpenseDetailInfoRow(
    iconEmoji: String,
    label: String,
    value: String,
    showDivider: Boolean
) {
    if (showDivider) {
        HorizontalDivider(
            modifier = Modifier.padding(start = ExpenseDetailDimens.dividerInset),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    }
    ListItem(
        headlineContent = {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingContent = {
            ExpenseDetailEmojiIcon(
                emoji = iconEmoji,
                size = ExpenseDetailDimens.rowIconSize,
                fontSize = 18.sp,
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
private fun ExpenseDetailBottomBar(
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        tonalElevation = 3.dp,
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.spacing_default)),
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_medium))
        ) {
            ExpenseDetailBottomBarButton(
                text = stringResource(R.string.action_delete),
                icon = Icons.Default.Delete,
                onClick = onDelete,
                modifier = Modifier.weight(1f),
                outlined = true,
                contentColor = MaterialTheme.colorScheme.error,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
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
    val iconSpacing = dimensionResource(R.dimen.spacing_small)
    val content: @Composable RowScope.() -> Unit = {
        Icon(icon, contentDescription = null, modifier = Modifier.padding(end = iconSpacing))
        Text(text)
    }
    if (outlined) {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = contentColor),
            border = border
        ) {
            content()
        }
    } else {
        Button(onClick = onClick, modifier = modifier) {
            content()
        }
    }
}
