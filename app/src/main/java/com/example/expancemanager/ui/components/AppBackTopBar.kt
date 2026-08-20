package com.example.expancemanager.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.expancemanager.R
import com.example.expancemanager.ui.theme.ExpanseManagerTheme

/**
 * Shared back-navigation app bar. Sits flat on the screen background — separation
 * comes from the cards below it rather than from a tinted bar — with a circular
 * tonal back button and an optional second line of context.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AppBackTopBar(
    title: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    backContentDescription: String = stringResource(R.string.action_back),
    containerColor: Color = MaterialTheme.colorScheme.background,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        modifier = modifier,
        title = {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        navigationIcon = {
            CircleIconButton(
                onClick = onNavigateBack,
                contentDescription = backContentDescription,
                modifier = Modifier.padding(start = AppSpacing.small)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = backContentDescription,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = containerColor,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, name = "Back top bar")
@Composable
private fun AppBackTopBarPreview() {
    ExpanseManagerTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            AppBackTopBar(title = "Spending insights", onNavigateBack = {})
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, name = "Back top bar with subtitle")
@Composable
private fun AppBackTopBarSubtitlePreview() {
    ExpanseManagerTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            AppBackTopBar(
                title = "Manage categories",
                subtitle = "Organize your expense groups",
                onNavigateBack = {}
            )
        }
    }
}
