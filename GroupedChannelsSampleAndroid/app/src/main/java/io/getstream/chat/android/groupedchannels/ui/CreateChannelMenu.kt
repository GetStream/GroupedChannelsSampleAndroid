package io.getstream.chat.android.groupedchannels.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.getstream.chat.android.groupedchannels.LoginUser

/**
 * Dropdown anchored to the "+" action in the top bar that lets the user pick a peer to start
 * a 1:1 channel with. The current user is expected to be filtered out of [users] by the caller.
 */
@Composable
internal fun CreateChannelMenu(
    expanded: Boolean,
    users: List<LoginUser>,
    onDismiss: () -> Unit,
    onUserSelected: (LoginUser) -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(
                text = "New channel",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "Pick a user to start a 1:1 channel with",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
        HorizontalDivider()
        users.forEach { user ->
            DropdownMenuItem(
                text = { Text(user.name) },
                leadingIcon = {
                    Avatar(
                        seed = user.id,
                        initials = user.name.initials(),
                        modifier = Modifier.size(28.dp),
                        fontSize = 11.sp,
                    )
                },
                onClick = { onUserSelected(user) },
            )
        }
    }
}
