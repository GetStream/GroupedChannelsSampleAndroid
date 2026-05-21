package io.getstream.chat.android.groupedchannels

import android.content.Context
import android.util.Log
import io.getstream.chat.android.client.ChatClient
import io.getstream.chat.android.client.logger.ChatLogLevel
import io.getstream.chat.android.models.Channel
import io.getstream.chat.android.models.User
import io.getstream.chat.android.offline.plugin.factory.StreamOfflinePluginFactory
import io.getstream.chat.android.state.plugin.config.StatePluginConfig
import io.getstream.chat.android.state.plugin.factory.StreamStatePluginFactory
import io.getstream.result.call.enqueue
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Handles ChatClient initialization, connection, and the channel operations used by the sample.
 */
object ChatManager {

    private const val TAG = "ChatManager"
    private const val API_KEY = "vrvdwv6pk4yz"

    /**
     * Initializes the ChatClient with offline and state plugins, then connects the user.
     */
    fun initializeAndConnect(
        appContext: Context,
        loginUser: LoginUser,
        onComplete: () -> Unit,
        onError: () -> Unit,
    ) {
        val state = StreamStatePluginFactory(
            config = StatePluginConfig(),
            appContext = appContext,
        )
        val offline = StreamOfflinePluginFactory(
            appContext = appContext,
        )
        val chatClient = ChatClient.Builder(API_KEY, appContext)
            .withPlugins(state, offline)
            .logLevel(ChatLogLevel.ALL)
            .build()
        chatClient.connectUser(
            user = User(id = loginUser.id, name = loginUser.name),
            token = loginUser.token,
        ).enqueue(
            onSuccess = { onComplete() },
            onError = { onError() },
        )
    }

    /**
     * Prefills the local state/db with grouped channels for the current user.
     */
    fun prefillGroupedChannels() {
        ChatClient.instance()
            .queryGroupedChannels(watch = true)
            .enqueue(
                onSuccess = { grouped ->
                    // No action needed, state/db is prefilled automatically
                    Log.d(TAG, "Prefill grouped channels: ${grouped.groups.keys}")
                },
                onError = {
                    Log.e(TAG, "Failed to query grouped channels for prefill")
                },
            )
    }

    /**
     * Marks all channels as read for the current user.
     */
    fun markAllRead() {
        ChatClient.instance()
            .markAllRead()
            .enqueue(
                onSuccess = {
                    Log.d(TAG, "Marked all channels as read")
                },
                onError = {
                    Log.e(TAG, "Failed to mark all channels as read")
                },
            )
    }

    /**
     * Creates a new 1:1 channel between the current user and [otherUser], starting in the "new" group.
     */
    fun createChannelWith(otherUser: LoginUser) {
        val client = ChatClient.instance()
        val currentUserId = client.getCurrentUser()?.id ?: return
        val id = "new-channel-${System.currentTimeMillis()}"
        val name = "New Channel ${DateTimeFormatter.ISO_LOCAL_TIME.format(LocalTime.now())}"
        client.channel("messaging", id)
            .create(
                memberIds = listOf(currentUserId, otherUser.id),
                extraData = mapOf(
                    "name" to name,
                    "group" to "new",
                ),
            ).enqueue(
                onSuccess = { channel ->
                    Log.d(TAG, "Created channel ${channel.cid} with ${otherUser.id}")
                },
                onError = {
                    Log.e(TAG, "Failed to create channel: $it")
                },
            )
    }

    /**
     * Moves [channel] to the given [groupKey] via a partial channel update.
     */
    fun moveChannelToGroup(channel: Channel, groupKey: String) {
        ChatClient.instance()
            .updateChannelPartial(
                channelType = channel.type,
                channelId = channel.id,
                set = mapOf("group" to groupKey),
                unset = emptyList(),
            )
            .enqueue(
                onSuccess = {
                    Log.d(TAG, "Channel ${channel.cid} moved to '$groupKey'")
                },
                onError = {
                    Log.e(TAG, "Failed to move channel ${channel.cid}: $it")
                },
            )
    }
}
