package io.getstream.chat.android.groupedchannels

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.getstream.chat.android.client.ChatClient
import io.getstream.chat.android.compose.ui.messages.MessagesScreen
import io.getstream.chat.android.compose.ui.theme.ChatTheme
import io.getstream.chat.android.compose.viewmodel.messages.MessagesViewModelFactory
import io.getstream.result.call.enqueue

/**
 * Minimum-feature Channel screen.
 */
class ChannelActivity : ComponentActivity() {

    private val cid: String by lazy {
        requireNotNull(intent.getStringExtra(KEY_CHANNEL_ID)) { "Channel ID must be provided" }
    }

    private val factory by lazy {
        MessagesViewModelFactory(
            context = this,
            channelId = cid,
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ChatTheme {
                val context = LocalContext.current
                var updating by remember { mutableStateOf(false) }

                Box(modifier = Modifier.fillMaxSize()) {
                    MessagesScreen(
                        viewModelFactory = factory,
                        onBackPressed = { finish() },
                    )
                    FloatingActionButton(
                        onClick = {
                            if (updating) return@FloatingActionButton
                            val (channelType, channelId) = cid.split(":", limit = 2)
                                .let { it[0] to it[1] }
                            updating = true
                            ChatClient.instance()
                                .updateChannelPartial(
                                    channelType = channelType,
                                    channelId = channelId,
                                    set = mapOf("group" to "current"),
                                    unset = emptyList(),
                                )
                                .enqueue(
                                    onSuccess = {
                                        updating = false
                                        Toast.makeText(
                                            context,
                                            "Moved to Current",
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    },
                                    onError = {
                                        updating = false
                                        Toast.makeText(
                                            context,
                                            "Failed to move channel",
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    },
                                )
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .navigationBarsPadding()
                            .imePadding()
                            .padding(end = 16.dp, bottom = 80.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            contentDescription = "Move to Current",
                        )
                    }
                }
            }
        }
    }

    companion object {
        private const val KEY_CHANNEL_ID = "channelId"

        fun createIntent(context: Context, channelId: String): Intent {
            return Intent(context, ChannelActivity::class.java).apply {
                putExtra(KEY_CHANNEL_ID, channelId)
            }
        }
    }
}
