package io.getstream.chat.android.groupedchannels

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import io.getstream.chat.android.client.ChatClient
import io.getstream.chat.android.compose.ui.channels.list.ChannelList
import io.getstream.chat.android.compose.ui.components.channels.UnreadCountIndicator
import io.getstream.chat.android.compose.ui.theme.ChatTheme
import io.getstream.chat.android.compose.viewmodel.channels.ChannelListViewModel
import io.getstream.chat.android.compose.viewmodel.channels.ChannelViewModelFactory
import io.getstream.chat.android.groupedchannels.ui.theme.GroupedChannelsSampleAndroidTheme
import io.getstream.chat.android.models.Channel
import io.getstream.chat.android.models.FilterObject
import io.getstream.chat.android.models.Filters
import io.getstream.chat.android.models.querysort.QuerySortByField
import io.getstream.chat.android.state.extensions.globalStateFlow
import io.getstream.result.call.enqueue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import java.util.Date
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

class MainActivity : ComponentActivity() {

    // region Channel list filters

    private val nowMillis = System.currentTimeMillis()
    private val minus24h = Date(nowMillis - 24.hours.inWholeMilliseconds)
    private val minus48h = Date(nowMillis - 48.hours.inWholeMilliseconds)
    private val minus14d = Date(nowMillis - 14.days.inWholeMilliseconds)

    // "all" — first-page blended inbox view
    private fun allFilter(): FilterObject =
        Filters.`in`("members", listOf(ChatManager.USER_ID))

    // "new" — recently opened / early-life channels
    // (message_count = 0 && created_at > now-24h) || (message_count = 1 && created_at > now-48h)
    private fun newFilter(): FilterObject = Filters.and(
        allFilter(),
        Filters.or(
            Filters.and(
                Filters.eq("message_count", 0),
                Filters.greaterThan("created_at", minus24h),
            ),
            Filters.and(
                Filters.eq("message_count", 1),
                Filters.greaterThan("created_at", minus48h),
            ),
        ),
    )

    // "current" — active channels
    // message_count >= 2 && last_message_at > now-14d
    private fun currentFilter(): FilterObject = Filters.and(
        allFilter(),
        Filters.greaterThanEquals("message_count", 2),
        Filters.greaterThan("last_message_at", minus14d),
    )

    // "expired" — stale / inactive channels
    // (message_count = 0 && created_at <= now-24h) ||
    // (message_count = 1 && created_at <= now-48h) ||
    // (message_count >= 2 && (last_message_at <= now-14d || last_message_at IS NULL))
    private fun expiredFilter(): FilterObject = Filters.and(
        allFilter(),
        Filters.or(
            Filters.and(
                Filters.eq("message_count", 0),
                Filters.lessThanEquals("created_at", minus24h),
            ),
            Filters.and(
                Filters.eq("message_count", 1),
                Filters.lessThanEquals("created_at", minus48h),
            ),
            Filters.and(
                Filters.greaterThanEquals("message_count", 2),
                Filters.or(
                    Filters.lessThanEquals("last_message_at", minus14d),
                    Filters.notExists("last_message_at"),
                ),
            ),
        ),
    )

    // region Per-tab ViewModel factories

    private val allFactory by lazy {
        ChannelViewModelFactory(
            chatClient = ChatClient.instance(),
            querySort = QuerySortByField.descByName("last_message_at"),
            filters = allFilter(),
            channelLimit = 20,
            chatEventHandlerFactory = GroupedChatEventHandlerFactory("all") { channel ->
                // accept all channels the user is member of
                true
            },
            skipInitialQuery = true,
        )
    }

    private val newFactory by lazy {
        ChannelViewModelFactory(
            chatClient = ChatClient.instance(),
            querySort = QuerySortByField.descByName("last_message_at"),
            filters = newFilter(),
            channelLimit = 20,
            chatEventHandlerFactory = GroupedChatEventHandlerFactory("new") { channel ->
                val mc = effectiveMessageCount(channel)
                val now = System.currentTimeMillis()
                val minus24h = Date(now - 24.hours.inWholeMilliseconds)
                val minus48h = Date(now - 48.hours.inWholeMilliseconds)
                (mc == 0 && channel.createdAt?.after(minus24h) == true) ||
                        (mc == 1 && channel.createdAt?.after(minus48h) == true)
            },
            skipInitialQuery = true,
        )
    }

    private val currentFactory by lazy {
        ChannelViewModelFactory(
            chatClient = ChatClient.instance(),
            querySort = QuerySortByField.descByName("last_message_at"),
            filters = currentFilter(),
            channelLimit = 20,
            chatEventHandlerFactory = GroupedChatEventHandlerFactory("current") { channel ->
                // check message count
                val mc = effectiveMessageCount(channel)
                if (mc < 2) return@GroupedChatEventHandlerFactory false
                // check lastMessageAt
                val lastMessageAt = channel.lastMessageAt ?: return@GroupedChatEventHandlerFactory false
                val now = System.currentTimeMillis()
                val minus14d = Date(now - 14.days.inWholeMilliseconds)
                lastMessageAt.after(minus14d)
            },
            skipInitialQuery = true,
        )
    }

    private val expiredFactory by lazy {
        ChannelViewModelFactory(
            chatClient = ChatClient.instance(),
            querySort = QuerySortByField.descByName("last_message_at"),
            filters = expiredFilter(),
            channelLimit = 20,
            chatEventHandlerFactory = GroupedChatEventHandlerFactory("expired") { channel ->
                val mc = effectiveMessageCount(channel)
                val now = System.currentTimeMillis()
                val minus24h = Date(now - 24.hours.inWholeMilliseconds)
                val minus48h = Date(now - 48.hours.inWholeMilliseconds)
                val minus14d = Date(now - 14.days.inWholeMilliseconds)
                // mc == 0: expired if created <= now-24h
                if (mc == 0) return@GroupedChatEventHandlerFactory channel.createdAt?.after(minus24h) != true
                // mc == 1: expired if created <= now-48h
                if (mc == 1) return@GroupedChatEventHandlerFactory channel.createdAt?.after(minus48h) != true
                // mc >= 2: expired if lastMessageAt is old or absent
                channel.lastMessageAt?.after(minus14d) != true
            },
            skipInitialQuery = true,
        )
    }

    // endregion

    // region Per-tab ViewModels (keyed so that multiple instances of ChannelListViewModel coexist)

    private val allViewModel: ChannelListViewModel by lazy {
        ViewModelProvider(this, allFactory)[GroupTab.ALL.key, ChannelListViewModel::class.java]
    }
    private val newViewModel: ChannelListViewModel by lazy {
        ViewModelProvider(this, newFactory)[GroupTab.NEW.key, ChannelListViewModel::class.java]
    }
    private val currentViewModel: ChannelListViewModel by lazy {
        ViewModelProvider(this, currentFactory)[GroupTab.CURRENT.key, ChannelListViewModel::class.java]
    }
    private val expiredViewModel: ChannelListViewModel by lazy {
        ViewModelProvider(this, expiredFactory)[GroupTab.EXPIRED.key, ChannelListViewModel::class.java]
    }

    // endregion


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        ChatManager.initializeAndConnect(
            appContext = applicationContext,
            onComplete = {
                prefillWithGroupedChannels()
            },
            onError = {
                Log.e("MainActivity", "Failed to connect user")
            }
        )

        setContent {
            ChatTheme {
                setContent {
                    ChatTheme {
                        var selected by rememberSaveable { mutableStateOf(GroupTab.ALL) }

                        @OptIn(ExperimentalCoroutinesApi::class)
                        val unreadByTab by remember {
                            ChatClient.instance()
                                .globalStateFlow
                                .flatMapLatest { it.groupedUnreadChannels }
                        }.collectAsState(initial = emptyMap())
                        Box(
                            Modifier
                                .fillMaxSize()
                                .systemBarsPadding(),
                        ) {
                            Column(Modifier.fillMaxSize()) {
                                ScrollableTabRow(
                                    selectedTabIndex = selected.ordinal,
                                    edgePadding = 0.dp,
                                ) {
                                    GroupTab.entries.forEach { tab ->
                                        Tab(
                                            selected = selected == tab,
                                            onClick = { selected = tab },
                                            text = {
                                                val unread = unreadByTab[tab.key] ?: 0
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(tab.label)
                                                    if (unread > 0) {
                                                        Spacer(Modifier.width(6.dp))
                                                        UnreadCountIndicator(unreadCount = unread)
                                                    }
                                                }
                                            },
                                        )
                                    }
                                }
                                val vm = when (selected) {
                                    GroupTab.ALL -> allViewModel
                                    GroupTab.NEW -> newViewModel
                                    GroupTab.CURRENT -> currentViewModel
                                    GroupTab.EXPIRED -> expiredViewModel
                                }
                                key(selected) {
                                    ChannelList(
                                        modifier = Modifier.fillMaxSize(),
                                        viewModel = vm,
                                        onChannelClick = { channel ->
                                            startActivity(ChannelActivity.createIntent(this@MainActivity, channel.cid))
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun prefillWithGroupedChannels() {
        ChatClient.instance()
            .queryGroupedChannels(watch = true)
            .enqueue(
                onSuccess = { grouped ->
                    grouped.groups[GroupTab.ALL.key]?.let { allViewModel.prefill(it) }
                    grouped.groups[GroupTab.NEW.key]?.let { newViewModel.prefill(it) }
                    grouped.groups[GroupTab.CURRENT.key]?.let { currentViewModel.prefill(it) }
                    grouped.groups[GroupTab.EXPIRED.key]?.let { expiredViewModel.prefill(it) }
                },
                onError = {
                    Log.e("MainActivity", "Failed to query grouped channels for prefill")
                },
            )
    }

    private fun effectiveMessageCount(channel: Channel): Int {
        return maxOf(
            channel.messageCount ?: 0,
            channel.messages.size,
            if (channel.lastMessageAt == null) 0 else 1,
        )
    }
}

private enum class GroupTab(val key: String, val label: String) {
    ALL("all", "All"),
    NEW("new", "New"),
    CURRENT("current", "Current"),
    EXPIRED("expired", "Expired"),
}