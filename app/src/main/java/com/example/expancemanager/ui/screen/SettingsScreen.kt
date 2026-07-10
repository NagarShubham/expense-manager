package com.example.expancemanager.ui.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.expancemanager.R
import com.example.expancemanager.util.showToast
import com.example.expancemanager.viewmodel.SettingViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToBudgetSettings: () -> Unit = {},
    onNavigateToManageCategories: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val expenseCount by viewModel.expenseCount.collectAsState()
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()

    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showImportConfirmDialog by remember { mutableStateOf(false) }
    var replaceExisting by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<android.net.Uri?>(null) }

    // Export launcher - creates a new file
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            scope.launch {
                isLoading = true
                val result = viewModel.exportData(it)
                isLoading = false

                if (result.isSuccess) {
                    context.showToast(result.getOrNull() ?: context.getString(R.string.settings_export_success_default))
                } else {
                    context.showToast(context.getString(R.string.settings_export_failed, result.exceptionOrNull()?.message.orEmpty()))
                }
            }
        }
    }

    // Import launcher - opens an existing file
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            pendingImportUri = it
            showImportConfirmDialog = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_navigate_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = dimensionResource(R.dimen.spacing_default))
                    .padding(bottom = dimensionResource(R.dimen.spacing_xlarge))
            ) {
                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_small)))

                SettingsOverviewHeader(expenseCount = expenseCount)

                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_default)))

                SettingsTipBanner()

                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_xlarge)))

                SettingsSection(title = stringResource(R.string.settings_appearance_section)) {
                    SettingsGroupCard {
                        SettingsSwitchRow(
                            iconEmoji = if (isDarkTheme) "🌙" else "☀️",
                            iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            title = stringResource(R.string.settings_dark_theme_title),
                            subtitle = stringResource(
                                if (isDarkTheme) R.string.settings_dark_theme_on
                                else R.string.settings_dark_theme_off
                            ),
                            checked = isDarkTheme,
                            onCheckedChange = viewModel::setDarkTheme,
                            showDivider = false
                        )
                    }
                }

                SettingsSection(title = stringResource(R.string.settings_budget_section)) {
                    SettingsGroupCard {
                        SettingsNavigationRow(
                            iconResId = R.drawable.ic_folder,
                            iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                            title = stringResource(R.string.settings_budget_title),
                            subtitle = stringResource(R.string.settings_budget_subtitle),
                            onClick = onNavigateToBudgetSettings,
                            showDivider = false
                        )
                    }
                }

                SettingsSection(title = stringResource(R.string.settings_categories_section)) {
                    SettingsGroupCard {
                        SettingsCategoriesRow(onClick = onNavigateToManageCategories)
                    }
                }

                SettingsSection(title = stringResource(R.string.settings_backup_section)) {
                    SettingsGroupCard {
                        SettingsActionRow(
                            iconResId = R.drawable.ic_upload,
                            iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                            title = stringResource(R.string.settings_export_title),
                            subtitle = stringResource(R.string.settings_export_subtitle),
                            buttonText = stringResource(R.string.settings_export_button),
                            enabled = !isLoading,
                            onClick = { showExportDialog = true },
                            tonal = false,
                            showDivider = true
                        )
                        SettingsActionRow(
                            iconResId = R.drawable.ic_folder,
                            iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            iconTint = MaterialTheme.colorScheme.onTertiaryContainer,
                            title = stringResource(R.string.settings_import_title),
                            subtitle = stringResource(R.string.settings_import_subtitle),
                            buttonText = stringResource(R.string.settings_import_button),
                            enabled = !isLoading,
                            onClick = { showImportDialog = true },
                            tonal = true,
                            showDivider = false
                        )
                    }
                }
            }
        }

        SettingsExportConfirmDialog(
            visible = showExportDialog,
            expenseCount = expenseCount,
            onDismiss = { showExportDialog = false },
            onConfirm = {
                showExportDialog = false
                exportLauncher.launch(viewModel.generateBackupFileName())
            }
        )
        SettingsImportOptionsDialog(
            visible = showImportDialog,
            replaceExisting = replaceExisting,
            onReplaceExistingChange = { replaceExisting = it },
            onDismiss = { showImportDialog = false },
            onSelectFile = {
                showImportDialog = false
                importLauncher.launch(arrayOf("application/json", "application/*", "*/*"))
            }
        )
        SettingsImportConfirmDialog(
            visible = showImportConfirmDialog && pendingImportUri != null,
            replaceExisting = replaceExisting,
            currentExpenseCount = expenseCount,
            onDismiss = {
                showImportConfirmDialog = false
                pendingImportUri = null
            },
            onConfirm = {
                showImportConfirmDialog = false
                val uri = pendingImportUri!!
                pendingImportUri = null
                scope.launch {
                    isLoading = true
                    val result = viewModel.importData(uri, replaceExisting)
                    isLoading = false
                    if (result.isSuccess) {
                        val imported = result.getOrNull()
                        context.showToast(
                            context.getString(
                                R.string.settings_import_success,
                                imported?.expenses?.size ?: 0,
                                imported?.monthlyBudgets?.size ?: 0,
                                imported?.budgetExcludedCategories?.size ?: 0
                            )
                        )
                    } else {
                        context.showToast(context.getString(R.string.settings_import_failed, result.exceptionOrNull()?.message.orEmpty()))
                    }
                }
            }
        )
    }
}

@Composable
private fun SettingsOverviewHeader(expenseCount: Int) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.spacing_xlarge)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_default))
        ) {
            Surface(
                modifier = Modifier.size(dimensionResource(R.dimen.icon_size_large)),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_upload),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(dimensionResource(R.dimen.spacing_medium))
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.settings_backup_section),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_tiny)))
                Text(
                    text = stringResource(R.string.settings_backup_expense_count, expenseCount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Text(
                    text = expenseCount.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(
                        horizontal = dimensionResource(R.dimen.spacing_medium),
                        vertical = dimensionResource(R.dimen.spacing_small)
                    )
                )
            }
        }
    }
}

@Composable
private fun SettingsTipBanner() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier.padding(dimensionResource(R.dimen.spacing_default)),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(20.dp)
                    .padding(top = 2.dp)
            )
            Text(
                text = stringResource(R.string.settings_backup_info),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = dimensionResource(R.dimen.spacing_small))
            )
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(
                start = dimensionResource(R.dimen.spacing_tiny),
                bottom = dimensionResource(R.dimen.spacing_small)
            )
        )
        content()
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_default)))
    }
}

@Composable
private fun SettingsGroupCard(content: @Composable ColumnScope.() -> Unit) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(content = content)
    }
}

@Composable
private fun SettingsIconContainer(
    iconEmoji: String? = null,
    iconResId: Int? = null,
    containerColor: Color,
    tint: Color = Color.Unspecified
) {
    Surface(
        modifier = Modifier.size(40.dp),
        shape = RoundedCornerShape(12.dp),
        color = containerColor
    ) {
        when {
            iconEmoji != null -> Text(
                text = iconEmoji,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(8.dp)
            )

            iconResId != null -> Icon(
                painter = painterResource(iconResId),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

@Composable
private fun SettingsInsetDivider(show: Boolean) {
    if (show) {
        HorizontalDivider(
            modifier = Modifier.padding(start = 72.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun SettingsSwitchRow(
    iconEmoji: String,
    iconContainerColor: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    showDivider: Boolean
) {
    SettingsInsetDivider(showDivider)
    ListItem(
        headlineContent = {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        },
        supportingContent = {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingContent = {
            SettingsIconContainer(
                iconEmoji = iconEmoji,
                containerColor = iconContainerColor
            )
        },
        trailingContent = {
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
private fun SettingsNavigationRow(
    iconResId: Int,
    iconContainerColor: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    showDivider: Boolean
) {
    SettingsInsetDivider(showDivider)
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        headlineContent = {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        },
        supportingContent = {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingContent = {
            SettingsIconContainer(
                iconResId = iconResId,
                containerColor = iconContainerColor,
                tint = iconTint
            )
        },
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
private fun SettingsCategoriesRow(onClick: () -> Unit) {
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        headlineContent = {
            Text(
                text = stringResource(R.string.settings_categories_title),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        },
        supportingContent = {
            Text(
                text = stringResource(R.string.settings_categories_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingContent = {
            SettingsIconContainer(
                iconEmoji = "🏷️",
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        },
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
private fun SettingsActionRow(
    iconResId: Int,
    iconContainerColor: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    buttonText: String,
    enabled: Boolean,
    onClick: () -> Unit,
    tonal: Boolean,
    showDivider: Boolean
) {
    SettingsInsetDivider(showDivider)
    ListItem(
        headlineContent = {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        },
        supportingContent = {
            Column {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_small)))
                if (tonal) {
                    FilledTonalButton(
                        onClick = onClick,
                        enabled = enabled,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(buttonText)
                    }
                } else {
                    OutlinedButton(
                        onClick = onClick,
                        enabled = enabled,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(buttonText)
                    }
                }
            }
        },
        leadingContent = {
            SettingsIconContainer(
                iconResId = iconResId,
                containerColor = iconContainerColor,
                tint = iconTint
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
private fun SettingsExportConfirmDialog(
    visible: Boolean,
    expenseCount: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    if (!visible) return
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_upload),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = stringResource(R.string.settings_export_confirm_title),
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column {
                Text(stringResource(R.string.settings_export_confirm_message))
                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_small)))
                Text(
                    text = stringResource(R.string.settings_export_count_message, expenseCount),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(stringResource(R.string.settings_export_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
private fun SettingsImportOptionsDialog(
    visible: Boolean,
    replaceExisting: Boolean,
    onReplaceExistingChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onSelectFile: () -> Unit
) {
    if (!visible) return
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_folder),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary
            )
        },
        title = {
            Text(
                text = stringResource(R.string.settings_import_title),
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.settings_import_options_message),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_default)))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (replaceExisting) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(dimensionResource(R.dimen.spacing_small)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = replaceExisting,
                            onCheckedChange = onReplaceExistingChange
                        )
                        Column(modifier = Modifier.padding(start = dimensionResource(R.dimen.spacing_small))) {
                            Text(
                                text = stringResource(R.string.settings_import_replace_option),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (replaceExisting) {
                                Text(
                                    text = stringResource(R.string.settings_import_replace_warning),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onSelectFile) {
                Text(stringResource(R.string.settings_import_select_file))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
private fun SettingsImportConfirmDialog(
    visible: Boolean,
    replaceExisting: Boolean,
    currentExpenseCount: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    if (!visible) return
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = if (replaceExisting) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                }
            )
        },
        title = {
            Text(
                text = stringResource(R.string.settings_import_confirm_title),
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column {
                Text(
                    if (replaceExisting) {
                        stringResource(R.string.settings_import_confirm_replace)
                    } else {
                        stringResource(R.string.settings_import_confirm_merge)
                    }
                )
                if (replaceExisting) {
                    Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_small)))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.settings_import_deleted_warning, currentExpenseCount),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(dimensionResource(R.dimen.spacing_small))
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = if (replaceExisting) {
                    ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                } else {
                    ButtonDefaults.buttonColors()
                }
            ) {
                Text(stringResource(R.string.settings_import_confirm_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
