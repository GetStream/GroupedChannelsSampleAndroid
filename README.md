## GroupedChannels Sample (Android)

### Setup

Snapshot version: **6.37.3-202604281455-SNAPSHOT**

To register the snapshot repository in your project, add the following line to the repositories block of your `settings.gradle.kts`:

```kotlin
maven { url = uri("https://central.sonatype.com/repository/maven-snapshots/") }
```

Then, to include the Stream Chat in you project, add the following lines to you app `build.gradle` dependencies block (or via the `libs.versions.toml` catalog):

```kotlin
implementation("io.getstream:stream-chat-android-compose:6.37.3-202604281455-SNAPSHOT")
implementation("io.getstream:stream-chat-android-offline:6.37.3-202604281455-SNAPSHOT")
```

### QueryGroupedChannels operation

The QueryGroupedChannels operation is defined on the ChatClient :

```kotlin
@CheckResult
public fun queryGroupedChannels(
    limit: Int? = null,
    watch: Boolean = false,
    presence: Boolean = false,
): Call<GroupedChannels>
```

### ChannelViewModelFactory setup

After a successful connection to the Stream service, calling the `queryGroupedChannels` method will return a result holding the first page of each group (`all`, `new`, `current`, `expired`). You can then use these groups to prefill the `ChannelListViewModel` with the initial data, skipping the 4 separate `QueryChannels` invokations. To do that, you need the following setup:

```kotlin
// Create the ChannelViewModelFactory (one per group)
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
        // Prevent initial data fetch
        skipInitialQuery = true,
    )
}

// Fetch the initial data
ChatClient.instance().queryGroupedChannels(watch = true).enqueue(
  onSuccess = { result ->
    // retrieve data for "all" group
    val allData = result.groups["all"]
    // pre-fill the VM with "all" data
    allViewModel.prefill(allData)

    // pre-fill other groups ("current", "new", ""expired)
    // ...
  }
)
```

The important steps are:

- `ChannelViewModelFactory(skipInitialQuery = true)` - skips the initial load for the VM data
- `ChannelListViewModel.prefill(group)` - populates the VM with the data fetched from the GroupedChannels

### Unread counts per group

To observe the live updates to the unread counts per group, you can observe the following flow:

```kotlin
ChatClient.instance()
    .globalStateFlow
    .flatMapLatest { it.groupedUnreadChannels }
```

The `groupedUnreadChannels` is a `Map` keyed by the group name (`all`, `new`...), with values equal to the current unread count of the group.

### Event matching

To ensure channels switch to their appropriate group after receiving/sending a message, you would also need to implement custom `ChatEventHandler` -> which will cross-check each channel against the expected filters - telling the `ChannelList` whether the channel should be added/removed from its current state. The `ChatEventHandler` is then passed to the `ChannelViewModelFactory` constructor.

An example of such `ChatEventHandler` can be found [here](https://github.com/GetStream/GroupedChannelsSampleAndroid/blob/main/GroupedChannelsSampleAndroid/app/src/main/java/io/getstream/chat/android/groupedchannels/GroupedChatEventHandler.kt).
