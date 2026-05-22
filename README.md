## GroupedChannels Sample (Android)

### Setup

Snapshot version: **6.38.1-202605221530-SNAPSHOT**

To register the snapshot repository in your project, add the following line to the repositories block of your `settings.gradle.kts`:

```kotlin
maven { url = uri("https://central.sonatype.com/repository/maven-snapshots/") }
```

Then, to include the Stream Chat in you project, add the following lines to you app `build.gradle` dependencies block (or via the `libs.versions.toml` catalog):

```kotlin
implementation("io.getstream:stream-chat-android-compose:6.38.1-202605221530-SNAPSHOT")
implementation("io.getstream:stream-chat-android-offline:6.38.1-202605221530-SNAPSHOT")
```

### QueryGroupedChannels operation

The `queryGroupedChannels` operation is defined on the `ChatClient`:

```kotlin
@CheckResult
public fun queryGroupedChannels(
    groups: List<String>,
    limit: Int? = null,
    watch: Boolean = false,
    presence: Boolean = false,
): Call<GroupedChannels>
```

Calling it fetches the first page of each requested server-side group in a single round-trip. The `groups` parameter is required and must contain at least one group name; duplicates are silently de-duplicated. The group names are arbitrary strings chosen by the caller — this sample happens to use `all`, `new`, `current`, and `old`, but any names defined on the server side can be passed in. The result is persisted into the state/database, so any `ChannelListViewModel` bound to one of the requested group keys is populated without a separate `queryChannels` call.

```kotlin
ChatClient.instance()
    .queryGroupedChannels(
        groups = listOf("all", "new", "current", "old"),
        watch = true,
    )
    .enqueue(
        onSuccess = { grouped ->
            // No action needed — state/db is prefilled automatically.
            Log.d("ChatManager", "Prefill grouped channels: ${grouped.groups.keys}")
        },
        onError = {
            Log.e("ChatManager", "Failed to query grouped channels for prefill")
        },
    )
```

### ChannelViewModelFactory setup

Each tab (group) is backed by its own `ChannelListViewModel`. The factory takes a single `groupKey` argument identifying the server-side group; the SDK uses that key to resolve the filter, sort and event-matching logic for the corresponding group. The `groupKey` value must match one of the group names configured on the server side and requested in `queryGroupedChannels`.

**Note: Instantiating `ChannelListViewModel` will NOT automatically call `ChatClient.queryGroupedChannels` - you have to do that manually to prepopulate the data.**

The example below uses `all`, `new`, `current`, and `old` as illustration — substitute the group keys your backend exposes.

```kotlin
// One factory per group
private val allFactory     by lazy { ChannelViewModelFactory(groupKey = "all") }
private val newFactory     by lazy { ChannelViewModelFactory(groupKey = "new") }
private val currentFactory by lazy { ChannelViewModelFactory(groupKey = "current") }
private val oldFactory     by lazy { ChannelViewModelFactory(groupKey = "old") }

// Keyed ViewModels so multiple ChannelListViewModel instances coexist
private val allViewModel: ChannelListViewModel by lazy {
    ViewModelProvider(this, allFactory)["all", ChannelListViewModel::class.java]
}
// ... same for the remaining groups
```

Combined with `queryGroupedChannels`, a single network call populates every tab.

### Unread counts per group

To observe the live updates to the unread counts per group, you can observe the following flow:

```kotlin
ChatClient.instance()
    .globalStateFlow
    .flatMapLatest { it.groupedUnreadChannels }
```

The `groupedUnreadChannels` is a `Map` keyed by the group name (matching whatever groups were requested via `queryGroupedChannels`, e.g. `all`, `new`, `current`, `old` in this sample), with values equal to the current unread count of the group.

### Event matching

Event matching (deciding which group a channel belongs to after a new or updated message) is handled by the SDK based on the `groupKey` passed to `ChannelViewModelFactory`. The server-side group definition is the single source of truth, so no custom `ChatEventHandler` is required.
