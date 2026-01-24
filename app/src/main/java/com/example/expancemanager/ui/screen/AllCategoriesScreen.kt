package com.example.expancemanager.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.expancemanager.R
import com.example.expancemanager.data.CategoryTotal
import com.example.expancemanager.ui.components.EmptyStateMessage
import com.example.expancemanager.util.DateUtils
import com.example.expancemanager.util.ExpenseCategories
import com.example.expancemanager.viewmodel.ExpenseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllCategoriesScreen(
    month: Int,
    year: Int,
    viewModel: ExpenseViewModel,
    onNavigateBack: () -> Unit = {},
    onCategoryClick: (String, Int, Int) -> Unit = { _, _, _ -> }
) {
    val uiState by viewModel.uiState.collectAsState()

    // Calculate total amount
    val totalAmount = remember(uiState.categoryTotals) {
        uiState.categoryTotals.sumOf { it.total }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.categories_all_title),
                        fontWeight = FontWeight.Bold
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
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = dimensionResource(R.dimen.spacing_default),
                        vertical = dimensionResource(R.dimen.spacing_small)
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = dimensionResource(R.dimen.card_elevation_raised))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(dimensionResource(R.dimen.spacing_xlarge)),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = DateUtils.formatMonthYear(month, year),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_small)))
                    Text(
                        text = DateUtils.formatAmount(totalAmount),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = if (uiState.categoryTotals.size == 1) {
                            stringResource(R.string.categories_count_singular, uiState.categoryTotals.size)
                        } else {
                            stringResource(R.string.categories_count_plural, uiState.categoryTotals.size)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }

            // Categories list
            if (uiState.categoryTotals.isEmpty()) {
                EmptyStateMessage(
                    emoji = "📊",
                    title = stringResource(R.string.categories_empty_title),
                    subtitle = stringResource(R.string.categories_empty_subtitle)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = dimensionResource(R.dimen.spacing_default)),
                    contentPadding = PaddingValues(vertical = dimensionResource(R.dimen.spacing_small))
                ) {
                    item {
                        Text(
                            text = stringResource(R.string.categories_all_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = dimensionResource(R.dimen.spacing_small))
                        )
                    }

                    items(uiState.categoryTotals, key = { it.category }) { categoryTotal ->
                        CategoryTotalCard(
                            categoryTotal = categoryTotal,
                            totalAmount = totalAmount,
                            onClick = { onCategoryClick(categoryTotal.category, month, year) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryTotalCard(
    categoryTotal: CategoryTotal,
    totalAmount: Double,
    onClick: () -> Unit
) {
    val percentage = remember(categoryTotal.total, totalAmount) {
        if (totalAmount > 0) (categoryTotal.total / totalAmount * 100).toInt() else 0
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = dimensionResource(R.dimen.spacing_tiny))
            .clickable { onClick() },
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
                Text(
                    text = ExpenseCategories.getCategoryEmoji(categoryTotal.category),
                    fontSize = 32.sp,
                    modifier = Modifier.padding(end = dimensionResource(R.dimen.spacing_medium))
                )

                Column {
                    Text(
                        text = categoryTotal.category,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_tiny)))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        LinearProgressIndicator(
                            progress = { (percentage / 100f) },
                            modifier = Modifier
                                .weight(1f)
                                .height(dimensionResource(R.dimen.spacing_tiny)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                        Spacer(modifier = Modifier.width(dimensionResource(R.dimen.spacing_small)))
                        Text(
                            text = "$percentage%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = DateUtils.formatAmount(categoryTotal.total),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.cd_view_category_details),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = dimensionResource(R.dimen.spacing_tiny))
                )
            }
        }
    }
}
