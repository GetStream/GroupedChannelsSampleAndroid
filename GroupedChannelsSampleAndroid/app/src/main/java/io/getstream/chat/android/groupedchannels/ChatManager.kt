package io.getstream.chat.android.groupedchannels

import android.content.Context
import io.getstream.chat.android.client.ChatClient
import io.getstream.chat.android.client.logger.ChatLogLevel
import io.getstream.chat.android.models.User
import io.getstream.chat.android.offline.plugin.factory.StreamOfflinePluginFactory
import io.getstream.chat.android.state.plugin.config.StatePluginConfig
import io.getstream.chat.android.state.plugin.factory.StreamStatePluginFactory
import io.getstream.result.call.enqueue

/**
 * Handles ChatClient initialization and connection for the sample app.
 */
object ChatManager {

    private const val API_KEY = "vrvdwv6pk4yz"

    /**
     * Initializes the ChatClient with offline and state plugins, then connects the user.
     *
     * @param appContext The application context for initializing the ChatClient and plugins.
     * @param loginUser The user to connect with.
     * @param onComplete Callback invoked when the user is successfully connected.
     * @param onError Callback invoked if there is an error during connection.
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
}