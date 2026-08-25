package com.example.expancemanager.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expancemanager.ui.theme.AppRadius
import com.example.expancemanager.util.EmojiData
import kotlinx.coroutines.launch

/**
 * WhatsApp-style emoji picker shown as a modal bottom sheet: a scrollable row of
 * category tabs above a grid of emojis. Tapping an emoji returns it via [onEmojiSelected]
 * and dismisses the sheet. Emojis render with the platform emoji font (same as elsewhere).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EmojiPickerBottomSheet(
    onEmojiSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    // Flatten groups into a single grid, tracking where each group starts so tabs can
    // scroll to it and the selected tab can follow scrolling.
    val groups = EmojiData.groups
    val flatEmojis = remember { groups.flatMap { it.emojis } }
    val groupStartIndex = remember {
        var running = 0
        groups.map { group ->
            val start = running
            running += group.emojis.size
            start
        }
    }

    val gridState = rememberLazyGridState()
    var selectedTab by remember { mutableIntStateOf(0) }

    // Keep the selected tab in sync with the first visible item as the user scrolls.
    LaunchedEffect(gridState) {
        snapshotFlow { gridState.firstVisibleItemIndex }.collect { firstVisible ->
            selectedTab = groupStartIndex.indexOfLast { it <= firstVisible }.coerceAtLeast(0)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = AppRadius.hero,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = AppSpacing.medium,
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                groups.forEachIndexed { index, group ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = {
                            selectedTab = index
                            scope.launch { gridState.animateScrollToItem(groupStartIndex[index]) }
                        },
                        text = {
                            Text(
                                text = group.title,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(8),
                state = gridState,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .padding(horizontal = 8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                itemsIndexed(
                    items = flatEmojis,
                    key = { index, emoji -> "$index-$emoji" }
                ) { _, emoji ->
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(CircleShape)
                            .clickable {
                                onEmojiSelected(emoji)
                                scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = emoji, fontSize = 26.sp)
                    }
                }
            }
        }
    }
}
