package com.example.expancemanager.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expancemanager.ui.theme.AppRadius

@Composable
internal fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        OverlineText(
            text = title,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(
                start = AppSpacing.tiny,
                bottom = AppSpacing.small
            )
        )
        content()
        VSpace(AppSpacing.large)
    }
}

@Composable
internal fun SettingsGroupCard(content: @Composable ColumnScope.() -> Unit) {
    AppCard(contentPadding = 0.dp, content = content)
}

@Composable
internal fun SettingsIconContainer(
    iconEmoji: String? = null,
    iconResId: Int? = null,
    containerColor: Color,
    tint: Color = Color.Unspecified
) {
    Surface(
        modifier = Modifier.size(40.dp),
        shape = AppRadius.icon,
        color = containerColor
    ) {
        Box(contentAlignment = Alignment.Center) {
            when {
                iconEmoji != null -> Text(text = iconEmoji, fontSize = 18.sp)

                iconResId != null -> Icon(
                    painter = painterResource(iconResId),
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
internal fun SettingsInsetDivider(show: Boolean) {
    if (show) {
        HorizontalDivider(
            modifier = Modifier.padding(start = 72.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
    }
}

@Composable
internal fun SettingsSwitchRow(
    iconEmoji: String,
    iconContainerColor: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
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
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
internal fun SettingsNavigationRow(
    iconContainerColor: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    showDivider: Boolean,
    iconResId: Int? = null,
    iconTint: Color = Color.Unspecified,
    iconEmoji: String? = null
) {
    SettingsInsetDivider(showDivider)
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
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
                iconResId = iconResId,
                containerColor = iconContainerColor,
                tint = iconTint
            )
        },
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
internal fun SettingsActionRow(
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
                VSpace(AppSpacing.medium)
                if (tonal) {
                    FilledTonalButton(
                        onClick = onClick,
                        enabled = enabled,
                        shape = AppRadius.pill,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Text(buttonText, style = MaterialTheme.typography.labelLarge)
                    }
                } else {
                    OutlinedButton(
                        onClick = onClick,
                        enabled = enabled,
                        shape = AppRadius.pill,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Text(buttonText, style = MaterialTheme.typography.labelLarge)
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
