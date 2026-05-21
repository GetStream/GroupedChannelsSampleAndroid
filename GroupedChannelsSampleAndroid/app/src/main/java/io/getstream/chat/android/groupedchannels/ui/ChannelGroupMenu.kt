package io.getstream.chat.android.groupedchannels.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.getstream.chat.android.groupedchannels.ChannelGroup
import io.getstream.chat.android.models.Channel
import io.getstream.chat.android.models.ChannelCapabilities

/**
 * Trailing-content icon button on a channel item that opens a dropdown for moving the channel to
 * a different group. The button is disabled (and dimmed) when the current user lacks the
 * `update-channel` capability.
 */
@Composable
internal fun ChannelGroupMenu(
    channel: Channel,
    onMoveTo: (ChannelGroup) -> Unit,
) {
    var expanded by rememberSaveable(channel.cid) { mutableStateOf(false) }
    val canUpdate = ChannelCapabilities.UPDATE_CHANNEL in channel.ownCapabilities
    val currentGroup = channel.extraData["group"] as? String

    Box {
        IconButton(
            onClick = { expanded = true },
            enabled = canUpdate,
        ) {
            Icon(
                imageVector = Icons.Outlined.GridView,
                contentDescription = "Move to group",
                tint = if (canUpdate) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                },
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    text = "Move to group",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Pick which group this channel belongs to",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
            HorizontalDivider()
            ChannelGroup.entries
                .filter { it != ChannelGroup.ALL }
                .forEach { group ->
                    DropdownMenuItem(
                        text = { Text(group.label) },
                        leadingIcon = {
                            if (group.key == currentGroup) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                )
                            } else {
                                Spacer(Modifier.size(24.dp))
                            }
                        },
                        onClick = {
                            expanded = false
                            onMoveTo(group)
                        },
                    )
                }
        }
    }
}
