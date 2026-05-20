package io.getstream.chat.android.groupedchannels

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import io.getstream.chat.android.client.ChatClient
import io.getstream.chat.android.compose.ui.channels.list.ChannelList
import io.getstream.chat.android.compose.ui.theme.ChatTheme
import io.getstream.chat.android.compose.viewmodel.channels.ChannelListViewModel
import io.getstream.chat.android.compose.viewmodel.channels.ChannelViewModelFactory
import io.getstream.chat.android.groupedchannels.ui.theme.GroupedChannelsSampleAndroidTheme
import io.getstream.chat.android.state.extensions.globalStateFlow
import io.getstream.result.call.enqueue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {

    private val allFactory by lazy {
        ChannelViewModelFactory(groupKey = ChannelGroup.ALL.key)
    }
    private val newFactory by lazy {
        ChannelViewModelFactory(groupKey = ChannelGroup.NEW.key)
    }
    private val currentFactory by lazy {
        ChannelViewModelFactory(groupKey = ChannelGroup.CURRENT.key)
    }
    private val oldFactory by lazy {
        ChannelViewModelFactory(groupKey = ChannelGroup.OLD.key)
    }

    private val allViewModel: ChannelListViewModel by lazy {
        ViewModelProvider(this, allFactory)[ChannelGroup.ALL.key, ChannelListViewModel::class.java]
    }
    private val newViewModel: ChannelListViewModel by lazy {
        ViewModelProvider(this, newFactory)[ChannelGroup.NEW.key, ChannelListViewModel::class.java]
    }
    private val currentViewModel: ChannelListViewModel by lazy {
        ViewModelProvider(this, currentFactory)[ChannelGroup.CURRENT.key, ChannelListViewModel::class.java]
    }
    private val oldViewModel: ChannelListViewModel by lazy {
        ViewModelProvider(this, oldFactory)[ChannelGroup.OLD.key, ChannelListViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        initGroupedChannels()

        setContent {
            GroupedChannelsSampleAndroidTheme {
                var selected by rememberSaveable { mutableStateOf(ChannelGroup.ALL) }

                @OptIn(ExperimentalCoroutinesApi::class)
                val unreadByTab by remember {
                    ChatClient.instance()
                        .globalStateFlow
                        .flatMapLatest { it.groupedUnreadChannels }
                }.collectAsState(initial = emptyMap())

                Box(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                ) {
                    Column(Modifier.fillMaxSize()) {
                        TopBar(
                            title = "Grouped Channels",
                            onMarkAllRead = ::markAllRead,
                            onCreateChannel = ::createChannel,
                        )

                        val vm = when (selected) {
                            ChannelGroup.ALL -> allViewModel
                            ChannelGroup.NEW -> newViewModel
                            ChannelGroup.CURRENT -> currentViewModel
                            ChannelGroup.OLD -> oldViewModel
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            ChatTheme {
                                key(selected) {
                                    ChannelList(
                                        modifier = Modifier.fillMaxSize(),
                                        viewModel = vm,
                                        onChannelClick = { channel ->
                                            startActivity(
                                                ChannelActivity.createIntent(
                                                    this@MainActivity,
                                                    channel.cid,
                                                ),
                                            )
                                        },
                                    )
                                }
                            }
                        }

                        BottomTabBar(
                            tabs = ChannelGroup.entries,
                            selected = selected,
                            unreadByTab = unreadByTab,
                            onSelect = { selected = it },
                        )
                    }
                }
            }
        }
    }

    private fun initGroupedChannels() {
        ChatClient.instance()
            .queryGroupedChannels(watch = true)
            .enqueue(
                onSuccess = { grouped ->
                    // No action needed, state/db is prefilled automatically
                    Log.d("MainActivity", "Prefill grouped channels: ${grouped.groups.keys}")
                },
                onError = {
                    Log.e("MainActivity", "Failed to query grouped channels for prefill")
                },
            )
    }

    private fun markAllRead() {
        ChatClient.instance()
            .markAllRead()
            .enqueue(
                onSuccess = {
                    Log.d("MainActivity", "Marked all channels as read")
                },
                onError = {
                    Log.e("MainActivity", "Failed to mark all channels as read")
                },
            )
    }

    private fun createChannel() {
        val client = ChatClient.instance()
        val currentUserId = client.getCurrentUser()?.id ?: return
        val id = "new-channel-${System.currentTimeMillis()}"
        val name = "New Channel ${DateTimeFormatter.ISO_LOCAL_TIME.format(java.time.LocalTime.now())}"
        val channelClient = client.channel("messaging", id)
        channelClient.create(
            memberIds = listOf(currentUserId, "member_02"),
            extraData = mapOf(
                "name" to name,
                "group" to "new",
            ),
        ).enqueue(
            onSuccess = { channel ->
                Log.d("MainActivity", "Created channel ${channel.cid}")
            },
            onError = {
                Log.e("MainActivity", "Failed to create channel: $it")
            },
        )
    }
}

@Composable
private fun TopBar(
    title: String,
    onMarkAllRead: () -> Unit,
    onCreateChannel: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onMarkAllRead) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Mark all read",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                IconButton(onClick = onCreateChannel) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Create new channel",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomTabBar(
    tabs: List<ChannelGroup>,
    selected: ChannelGroup,
    unreadByTab: Map<String, Int>,
    onSelect: (ChannelGroup) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEach { tab ->
                BottomTabItem(
                    tab = tab,
                    selected = tab == selected,
                    unread = unreadByTab[tab.key] ?: 0,
                    onClick = { onSelect(tab) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun BottomTabItem(
    tab: ChannelGroup,
    selected: Boolean,
    unread: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            Color.Transparent
        },
        label = "tab-bg",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        },
        label = "tab-fg",
    )

    Box(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                shape = RoundedCornerShape(50),
                color = containerColor,
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    BadgedBox(
                        badge = {
                            if (unread > 0) {
                                Badge(
                                    containerColor = Color(0xFFFF3B30),
                                    contentColor = Color.White,
                                ) {
                                    Text(
                                        text = if (unread > 99) "99+" else unread.toString(),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        },
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
                            tint = contentColor,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = tab.label,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = contentColor,
            )
        }
    }
}

private enum class ChannelGroup(
    val key: String,
    val label: String,
    val icon: ImageVector,
) {
    ALL("all", "All", Icons.Filled.Inbox),
    NEW("new", "New", Icons.Filled.AutoAwesome),
    CURRENT("current", "Current", Icons.AutoMirrored.Filled.Chat),
    OLD("old", "Old", Icons.Filled.Archive),
}
