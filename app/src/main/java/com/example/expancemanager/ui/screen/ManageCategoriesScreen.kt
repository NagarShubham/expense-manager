package com.example.expancemanager.ui.screen

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.expancemanager.R
import com.example.expancemanager.data.Category
import com.example.expancemanager.data.CategoryRepository.CategoryResult
import com.example.expancemanager.ui.components.EmojiPickerBottomSheet
import com.example.expancemanager.ui.components.EmptyStateMessage
import com.example.expancemanager.util.ExpenseCategories
import com.example.expancemanager.util.showToast
import com.example.expancemanager.viewmodel.CategoryViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private object ManageCategoriesShapes {
    val headerCard = RoundedCornerShape(20.dp)
    val sectionCard = RoundedCornerShape(16.dp)
    val emojiIcon = RoundedCornerShape(12.dp)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ManageCategoriesScreen(
    viewModel: CategoryViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val categories by viewModel.categories.collectAsState()

    var editingCategory by remember { mutableStateOf<CategoryEditState?>(null) }
    var pendingDelete by remember { mutableStateOf<Category?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.manage_categories_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = stringResource(R.string.manage_categories_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { editingCategory = CategoryEditState.forNew() },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.manage_categories_add)) }
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        if (categories.isEmpty()) {
            Box(modifier = Modifier.padding(paddingValues)) {
                EmptyStateMessage(
                    emoji = "🏷️",
                    title = stringResource(R.string.manage_categories_empty_title),
                    subtitle = stringResource(R.string.manage_categories_empty_subtitle)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = dimensionResource(R.dimen.spacing_default)),
                contentPadding = PaddingValues(vertical = dimensionResource(R.dimen.spacing_small))
            ) {
                item(key = "header") {
                    CategoriesHeaderCard(categoryCount = categories.size)
                    Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_xlarge)))
                }

                item(key = "section_title") {
                    CategoriesSectionTitle(
                        text = stringResource(R.string.manage_categories_section_label)
                    )
                }

                item(key = "categories_group") {
                    DraggableCategoryList(
                        categories = categories,
                        onMove = { from, to -> viewModel.moveCategory(from, to) },
                        onEdit = { editingCategory = CategoryEditState.forEdit(it) },
                        onDelete = { pendingDelete = it }
                    )
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }

    editingCategory?.let { state ->
        CategoryEditDialog(
            state = state,
            onDismiss = { editingCategory = null },
            onSave = { name, emoji ->
                scope.launch {
                    val result =
                        if (state.originalName == null) {
                            viewModel.addCategory(name, emoji)
                        } else {
                            viewModel.updateCategory(state.originalName, name, emoji)
                        }
                    when (result) {
                        CategoryResult.Success -> editingCategory = null
                        CategoryResult.BlankName ->
                            context.showToast(resources.getString(R.string.category_error_blank))
                        CategoryResult.DuplicateName ->
                            context.showToast(resources.getString(R.string.category_error_duplicate))
                        CategoryResult.InUse -> Unit
                    }
                }
            }
        )
    }

    pendingDelete?.let { category ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.category_delete_dialog_title)) },
            text = {
                Text(stringResource(R.string.category_delete_dialog_message, category.name))
            },
            confirmButton = {
                TextButton(onClick = {
                    val toDelete = category
                    pendingDelete = null
                    scope.launch {
                        if (viewModel.deleteCategory(toDelete.name) == CategoryResult.InUse) {
                            context.showToast(
                                resources.getString(R.string.category_error_in_use)
                            )
                        }
                    }
                }) {
                    Text(
                        stringResource(R.string.delete_dialog_confirm),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.delete_dialog_cancel))
                }
            }
        )
    }
}

@Composable
private fun DraggableCategoryList(
    categories: List<Category>,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit,
    onEdit: (Category) -> Unit,
    onDelete: (Category) -> Unit
) {
    val density = LocalDensity.current
    var draggedIndex by remember { mutableIntStateOf(-1) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var itemHeight by remember { mutableFloatStateOf(0f) }

    Column(modifier = Modifier.fillMaxWidth()) {
        categories.forEachIndexed { index, category ->
            val isDragged = draggedIndex == index

            val elevation by animateDpAsState(
                targetValue = if (isDragged) 8.dp else 0.dp,
                label = "drag_elevation"
            )

            Box(
                modifier = Modifier
                    .then(
                        if (isDragged) {
                            Modifier
                                .zIndex(1f)
                                .offset { IntOffset(0, dragOffset.roundToInt()) }
                        } else {
                            Modifier.zIndex(0f)
                        }
                    )
                    .onGloballyPositioned { coordinates ->
                        if (itemHeight == 0f) {
                            itemHeight = coordinates.size.height.toFloat()
                        }
                    }
            ) {
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .shadow(
                            elevation = elevation,
                            shape = ManageCategoriesShapes.sectionCard,
                            clip = false
                        ),
                    shape = ManageCategoriesShapes.sectionCard,
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isDragged) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        } else {
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                        }
                    )
                ) {
                    CategoryRow(
                        category = category,
                        onEdit = { onEdit(category) },
                        onDelete = { onDelete(category) },
                        dragModifier = Modifier.pointerInput(Unit) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    draggedIndex = index
                                    dragOffset = 0f
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragOffset += dragAmount.y

                                    if (itemHeight > 0f) {
                                        val steps = (dragOffset / itemHeight).roundToInt()
                                        val targetIndex = (draggedIndex + steps)
                                            .coerceIn(categories.indices)

                                        if (targetIndex != draggedIndex) {
                                            val prevIndex = draggedIndex
                                            onMove(prevIndex, targetIndex)
                                            draggedIndex = targetIndex
                                            dragOffset -= (targetIndex - prevIndex) * itemHeight
                                        }
                                    }
                                },
                                onDragEnd = {
                                    draggedIndex = -1
                                    dragOffset = 0f
                                },
                                onDragCancel = {
                                    draggedIndex = -1
                                    dragOffset = 0f
                                }
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoriesHeaderCard(categoryCount: Int) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = ManageCategoriesShapes.headerCard,
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
                shape = ManageCategoriesShapes.headerCard,
                color = MaterialTheme.colorScheme.primary
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = "🏷️", fontSize = 24.sp)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.manage_categories_header_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_tiny)))
                Text(
                    text = stringResource(R.string.manage_categories_header_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                shape = ManageCategoriesShapes.emojiIcon,
                color = MaterialTheme.colorScheme.surface
            ) {
                Text(
                    text = categoryCount.toString(),
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
private fun CategoriesSectionTitle(text: String) {
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
private fun CategoryRow(
    category: Category,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    dragModifier: Modifier = Modifier
) {
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
        headlineContent = {
            Text(
                text = category.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = stringResource(R.string.manage_categories_reorder),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = dragModifier.size(20.dp)
                )
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = ManageCategoriesShapes.emojiIcon,
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = category.emoji, fontSize = 18.sp)
                    }
                }
            }
        },
        trailingContent = {
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.manage_categories_delete),
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                )
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

/** Editable state for the add/edit dialog. [originalName] is null when adding. */
private data class CategoryEditState(
    val originalName: String?,
    val name: String,
    val emoji: String
) {
    companion object {
        fun forNew() = CategoryEditState(
            originalName = null,
            name = "",
            emoji = ExpenseCategories.FALLBACK_EMOJI
        )

        fun forEdit(category: Category) =
            CategoryEditState(
                originalName = category.name,
                name = category.name,
                emoji = category.emoji
            )
    }
}

@Composable
private fun CategoryEditDialog(
    state: CategoryEditState,
    onDismiss: () -> Unit,
    onSave: (name: String, emoji: String) -> Unit
) {
    var name by remember(state) { mutableStateOf(state.name) }
    var emoji by remember(state) { mutableStateOf(state.emoji) }
    var showEmojiPicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (state.originalName == null) {
                        R.string.category_dialog_add_title
                    } else {
                        R.string.category_dialog_edit_title
                    }
                )
            )
        },
        text = {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(
                        dimensionResource(R.dimen.spacing_medium)
                    )
                ) {
                    Surface(
                        modifier = Modifier
                            .size(56.dp)
                            .clickable { showEmojiPicker = true },
                        shape = ManageCategoriesShapes.emojiIcon,
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = emoji, fontSize = 28.sp)
                        }
                    }
                    Text(
                        text = stringResource(R.string.category_dialog_pick_emoji),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_default)))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.category_dialog_name_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name, emoji) },
                enabled = name.isNotBlank()
            ) {
                Text(stringResource(R.string.category_dialog_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.delete_dialog_cancel))
            }
        }
    )

    if (showEmojiPicker) {
        EmojiPickerBottomSheet(
            onEmojiSelected = { emoji = it },
            onDismiss = { showEmojiPicker = false }
        )
    }
}
