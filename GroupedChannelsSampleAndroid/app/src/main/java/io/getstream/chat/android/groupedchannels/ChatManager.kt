package io.getstream.chat.android.groupedchannels

import android.content.Context
import android.util.Log
import io.getstream.chat.android.client.ChatClient
import io.getstream.chat.android.client.logger.ChatLogLevel
import io.getstream.chat.android.models.User
import io.getstream.chat.android.offline.plugin.factory.StreamOfflinePluginFactory
import io.getstream.chat.android.state.plugin.config.StatePluginConfig
import io.getstream.chat.android.state.plugin.factory.StreamStatePluginFactory
import io.getstream.result.call.enqueue

object ChatManager {

    public val USER_ID = "bench-bq-0"

    fun initializeAndConnect(
        appContext: Context,
        onComplete: () -> Unit,
        onError: () -> Unit,
    ) {
        val apiKey = "vrvdwv6pk4yz"
        val token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX2lkIjoiYmVuY2gtYnEtMCJ9._GNHNHTR4WyCLTHfoSisYNdXC3sDorPVwRPcb6bwdBQ"

        val state = StreamStatePluginFactory(
            config = StatePluginConfig(),
            appContext = appContext,
        )
        val offline = StreamOfflinePluginFactory(
            appContext = appContext,
        )
        val chatClient = ChatClient.Builder(apiKey, appContext)
            .baseUrl("https://chat-edge-dublin-ce2.stream-io-api.com/")
            .withPlugins(state, offline)
            .logLevel(ChatLogLevel.ALL)
            .build()
        chatClient.connectUser(
            user = User(id = USER_ID, name = "bench-bq-0"),
            token = token,
        ).enqueue(
            onSuccess = { onComplete() },
            onError = { onError() },
        )
    }
}