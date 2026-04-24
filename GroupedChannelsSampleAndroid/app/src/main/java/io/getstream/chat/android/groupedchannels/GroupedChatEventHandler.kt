package io.getstream.chat.android.groupedchannels

import io.getstream.chat.android.client.ChatClient
import io.getstream.chat.android.client.events.ChannelUpdatedEvent
import io.getstream.chat.android.client.events.ChannelVisibleEvent
import io.getstream.chat.android.client.events.ChatEvent
import io.getstream.chat.android.client.events.HasChannel
import io.getstream.chat.android.client.events.NewMessageEvent
import io.getstream.chat.android.client.events.NotificationAddedToChannelEvent
import io.getstream.chat.android.client.events.NotificationMessageNewEvent
import io.getstream.chat.android.client.setup.state.ClientState
import io.getstream.chat.android.models.Channel
import io.getstream.chat.android.models.FilterObject
import io.getstream.chat.android.state.event.handler.chat.ChatEventHandler
import io.getstream.chat.android.state.event.handler.chat.DefaultChatEventHandler
import io.getstream.chat.android.state.event.handler.chat.EventHandlingResult
import io.getstream.chat.android.state.event.handler.chat.factory.ChatEventHandlerFactory
import kotlinx.coroutines.flow.StateFlow

/**
 * A [ChatEventHandler] that uses a client-side [filter] lambda to decide whether a channel
 * belongs in the current query.
 *
 * @param filter Client-side predicate: returns `true` if the channel belongs in this list.
 */
class GroupedChatEventHandler(
    private val name: String,
    channels: StateFlow<Map<String, Channel>?>,
    clientState: ClientState,
    private val filter: (Channel) -> Boolean,
) : DefaultChatEventHandler(channels, clientState) {

    override fun handleChatEvent(
        event: ChatEvent,
        filter: FilterObject,
        cachedChannel: Channel?
    ): EventHandlingResult {
        // Prefer cachedChannel (from per-channel state, already updated with latest messages)
        val channel = cachedChannel ?: (event as? HasChannel)?.channel
        return when (event) {
            is NotificationAddedToChannelEvent,
            is NotificationMessageNewEvent,
                -> channel?.let(::watchOrRemove) ?: EventHandlingResult.Skip

            is ChannelUpdatedEvent,
            is ChannelVisibleEvent,
            is NewMessageEvent,
                -> channel?.let(::addOrRemove) ?: EventHandlingResult.Skip

            else -> super.handleChatEvent(event, filter, cachedChannel)
        }
    }

    private fun watchOrRemove(channel: Channel): EventHandlingResult {
        return if (this.filter(channel)) {
            EventHandlingResult.WatchAndAdd(channel.cid)
        } else {
            removeIfChannelExists(channel.cid)
        }
    }

    private fun addOrRemove(channel: Channel): EventHandlingResult {
        return if (this.filter(channel)) {
            addIfChannelIsAbsent(channel)
        } else {
            removeIfChannelExists(channel.cid)
        }
    }
}

/**
 * Factory that creates a [GroupedChatEventHandler] with the given client-side [filter].
 */
class GroupedChatEventHandlerFactory(
    private val name: String,
    private val filter: (Channel) -> Boolean,
) : ChatEventHandlerFactory() {
    override fun chatEventHandler(channels: StateFlow<Map<String, Channel>?>): ChatEventHandler {
        return GroupedChatEventHandler(name, channels, ChatClient.instance().clientState, filter)
    }
}