package io.getstream.chat.android.groupedchannels

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import io.getstream.chat.android.compose.ui.messages.MessagesScreen
import io.getstream.chat.android.compose.ui.theme.ChatTheme
import io.getstream.chat.android.compose.viewmodel.messages.MessagesViewModelFactory

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
                MessagesScreen(
                    viewModelFactory = factory,
                    onBackPressed = { finish() },
                )
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
