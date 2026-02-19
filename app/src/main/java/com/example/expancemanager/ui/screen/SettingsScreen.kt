package com.example.expancemanager.ui.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
    onNavigateToBudgetSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val expenseCount by viewModel.expenseCount.collectAsState()

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
                title = { Text(stringResource(R.string.settings_title)) },
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
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(dimensionResource(R.dimen.spacing_default))
            ) {
                SettingsBackupHero(expenseCount = expenseCount)
                SettingsInfoCard()
                SettingsSectionLabel(stringResource(R.string.settings_budget_section))
                SettingsClickableCard(
                    iconResId = R.drawable.ic_folder,
                    iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                    surfaceColor = MaterialTheme.colorScheme.primaryContainer,
                    title = stringResource(R.string.settings_budget_title),
                    subtitle = stringResource(R.string.settings_budget_subtitle),
                    subtitleMaxLines = 2,
                    onClick = onNavigateToBudgetSettings
                )
                SettingsSectionLabel(stringResource(R.string.settings_actions_label))
                SettingsCardWithButton(
                    iconResId = R.drawable.ic_upload,
                    iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                    surfaceColor = MaterialTheme.colorScheme.primaryContainer,
                    title = stringResource(R.string.settings_export_title),
                    subtitle = stringResource(R.string.settings_export_subtitle),
                    buttonText = stringResource(R.string.settings_export_button),
                    enabled = !isLoading,
                    onClick = { showExportDialog = true },
                    tonal = false
                )
                SettingsCardWithButton(
                    iconResId = R.drawable.ic_folder,
                    iconTint = MaterialTheme.colorScheme.onTertiaryContainer,
                    surfaceColor = MaterialTheme.colorScheme.tertiaryContainer,
                    title = stringResource(R.string.settings_import_title),
                    subtitle = stringResource(R.string.settings_import_subtitle),
                    buttonText = stringResource(R.string.settings_import_button),
                    enabled = !isLoading,
                    onClick = { showImportDialog = true },
                    tonal = true
                )
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
                        context.showToast(context.getString(R.string.settings_import_success, result.getOrNull() ?: 0))
                    } else {
                        context.showToast(context.getString(R.string.settings_import_failed, result.exceptionOrNull()?.message.orEmpty()))
                    }
                }
            }
        )
    }
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
                Text(stringResource(R.string.cancel))
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
                Text(stringResource(R.string.cancel))
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
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun SettingsBackupHero(expenseCount: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = dimensionResource(R.dimen.spacing_xlarge)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = dimensionResource(R.dimen.card_elevation_default))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.spacing_xlarge)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "💾", style = MaterialTheme.typography.displayMedium)
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_small)))
            Text(
                text = stringResource(R.string.settings_backup_section),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_tiny)))
            Text(
                text = stringResource(R.string.settings_backup_expense_count, expenseCount),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun SettingsInfoCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = dimensionResource(R.dimen.spacing_default)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.spacing_default)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(end = dimensionResource(R.dimen.spacing_small))
            )
            Text(
                text = stringResource(R.string.settings_backup_info),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun SettingsSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(
            start = dimensionResource(R.dimen.spacing_small),
            bottom = dimensionResource(R.dimen.spacing_small)
        )
    )
}

@Composable
private fun SettingsClickableCard(
    iconResId: Int,
    iconTint: Color,
    surfaceColor: Color,
    title: String,
    subtitle: String,
    subtitleMaxLines: Int = 1,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = dimensionResource(R.dimen.spacing_default))
            .clickable(onClick = onClick),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = dimensionResource(R.dimen.card_elevation_default))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.spacing_default)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = surfaceColor,
                modifier = Modifier.padding(end = dimensionResource(R.dimen.spacing_default))
            ) {
                Icon(
                    painter = painterResource(id = iconResId),
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.padding(dimensionResource(R.dimen.spacing_small))
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = subtitleMaxLines
                )
            }
        }
    }
}

@Composable
private fun SettingsCardWithButton(
    iconResId: Int,
    iconTint: Color,
    surfaceColor: Color,
    title: String,
    subtitle: String,
    buttonText: String,
    enabled: Boolean,
    onClick: () -> Unit,
    tonal: Boolean
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().padding(bottom = dimensionResource(R.dimen.spacing_default)),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = dimensionResource(R.dimen.card_elevation_default))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(dimensionResource(R.dimen.spacing_default))) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = surfaceColor,
                    modifier = Modifier.padding(end = dimensionResource(R.dimen.spacing_default))
                ) {
                    Icon(
                        painter = painterResource(id = iconResId),
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.padding(dimensionResource(R.dimen.spacing_small))
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_default)))
            if (tonal) {
                FilledTonalButton(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
                    Text(buttonText)
                }
            } else {
                Button(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
                    Text(buttonText)
                }
            }
        }
    }
}
