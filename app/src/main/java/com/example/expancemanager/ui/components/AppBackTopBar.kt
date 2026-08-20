package com.example.expancemanager.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.example.expancemanager.R
import com.example.expancemanager.ui.theme.ExpanseManagerTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AppBackTopBar(
    title: String,
    onNavigateBack: () -> Unit,
    backContentDescription: String = stringResource(R.string.action_back),
    containerColor: Color = MaterialTheme.colorScheme.surface,
    titleContentColor: Color = MaterialTheme.colorScheme.onSurface,
    titleFontWeight: FontWeight = FontWeight.Bold
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                fontWeight = titleFontWeight
            )
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = backContentDescription
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = containerColor,
            titleContentColor = titleContentColor
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, name = "Back top bar")
@Composable
private fun AppBackTopBarPreview() {
    ExpanseManagerTheme(dynamicColor = false) {
        AppBackTopBar(
            title = "Reports",
            onNavigateBack = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, name = "Back top bar primary")
@Composable
private fun AppBackTopBarPrimaryPreview() {
    ExpanseManagerTheme(dynamicColor = false) {
        AppBackTopBar(
            title = "All Categories",
            onNavigateBack = {},
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}
