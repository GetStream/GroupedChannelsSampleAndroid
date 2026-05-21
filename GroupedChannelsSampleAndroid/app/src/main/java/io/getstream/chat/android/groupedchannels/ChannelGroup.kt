package io.getstream.chat.android.groupedchannels

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * The buckets a channel can be sorted into in the sample app.
 *
 * The [key] is the value stored in the channel's `extraData["group"]` and used by the backend's
 * `queryGroupedChannels` endpoint.
 */
internal enum class ChannelGroup(
    val key: String,
    val label: String,
    val icon: ImageVector,
) {
    ALL("all", "All", Icons.Filled.Inbox),
    NEW("new", "New", Icons.Filled.AutoAwesome),
    CURRENT("current", "Current", Icons.AutoMirrored.Filled.Chat),
    OLD("old", "Old", Icons.Filled.Archive),
}
