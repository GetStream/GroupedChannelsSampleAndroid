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

        ChatManager.initializeAndConnect(
            appContext = applicationContext,
            onComplete = {
                initGroupedChannels()
            },
            onError = {
                Log.e("MainActivity", "Failed to connect user")
            }
        )

        setContent {
            ChatTheme {
                setContent {
                    ChatTheme {
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
                                .systemBarsPadding(),
                        ) {
                            Column(Modifier.fillMaxSize()) {
                                ScrollableTabRow(
                                    selectedTabIndex = selected.ordinal,
                                    edgePadding = 0.dp,
                                ) {
                                    ChannelGroup.entries.forEach { tab ->
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
                                    ChannelGroup.ALL -> allViewModel
                                    ChannelGroup.NEW -> newViewModel
                                    ChannelGroup.CURRENT -> currentViewModel
                                    ChannelGroup.OLD -> oldViewModel
                                }
                                key(selected) {
                                    ChannelList(
                                        modifier = Modifier.fillMaxSize(),
                                        viewModel = vm,
                                        onChannelClick = { channel ->
                                            startActivity(
                                                ChannelActivity.createIntent(
                                                    this@MainActivity,
                                                    channel.cid
                                                )
                                            )
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
}

private enum class ChannelGroup(val key: String, val label: String) {
    ALL("all", "All"),
    NEW("new", "New"),
    CURRENT("current", "Current"),
    OLD("old", "Old"),
}