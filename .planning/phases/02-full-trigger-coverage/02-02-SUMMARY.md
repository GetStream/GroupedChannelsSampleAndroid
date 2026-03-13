---
phase: 02-full-trigger-coverage
plan: 02
subsystem: channel-state
tags: [kotlin, channel-state, pagination, reconciliation, local-only-messages, preservation]

# Dependency graph
requires:
  - phase: 01-core-reconciliation
    provides: setMessagesPreservingLocalOnly primitive in ChannelStateImpl, updateOldestLoadedDateForChannel write path
  - phase: 02-full-trigger-coverage
    plan: 01
    provides: selectOldestLoadedDateForChannel read path in ChannelRepository/RepositoryFacade
provides:
  - filteringOlderMessages branch in updateMessages uses setMessagesPreservingLocalOnly
  - isFilteringNewerMessages (endReached=false) branch uses setMessagesPreservingLocalOnly with windowFloor
  - isFilteringNewerMessages (endReached=true) branch uses setMessagesPreservingLocalOnly with null floor
  - updateDataForChannel reconnect path (isChannelsStateUpdate=false) uses setMessagesPreservingLocalOnly with persistedFloor
  - updateDataForChannel else-upsert branch uses setMessagesPreservingLocalOnly with persistedFloor
  - DB-seed path (isChannelsStateUpdate=true) still uses setMessages full-replace (intentional invariant)
  - TODO(Phase 2) comment removed — reconnect floor read is now implemented
affects: [02-03, ChannelLogicImplTest, full-trigger-coverage-verification]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - isChannelsStateUpdate discriminator: distinguishes DB-seed (full-replace) from SyncManager reconnect (preservation) within the same shouldRefreshMessages=true branch
    - Sequential suspend calls for DB prefetch at top of messageLimit > 0 block — no coroutineScope.launch needed because updateDataForChannel is already suspend
    - null windowFloor = no-ceiling signal: passing null as floor to setMessagesPreservingLocalOnly means "preserve all local-only messages" (used for endReached=true and empty-state paths)

key-files:
  created: []
  modified:
    - stream-chat-android/stream-chat-android-client/src/main/java/io/getstream/chat/android/client/internal/state/plugin/logic/channel/internal/ChannelLogicImpl.kt
    - stream-chat-android/stream-chat-android-client/src/test/java/io/getstream/chat/android/client/internal/state/plugin/logic/channel/internal/ChannelLogicImplTest.kt

key-decisions:
  - "isChannelsStateUpdate=true guards the DB-seed full-replace path — OfflinePlugin already includes local-only in DB data; setMessages is intentional"
  - "isChannelsStateUpdate=false (default, SyncManager reconnect) triggers setMessagesPreservingLocalOnly with persistedFloor read from repository"
  - "endReached=true in isFilteringNewerMessages passes null as windowFloor — semantically equivalent to the non-filtering (latest) path, which also passes null"
  - "updateDataForChannel prefetches localOnlyFromDb + persistedFloor once per call at top of messageLimit > 0 block — avoids redundant reads per branch"

patterns-established:
  - "isChannelsStateUpdate discriminator: use existing boolean parameter as branch discriminator to avoid adding new parameters"
  - "Suspend prefetch before when-block: read both DB values sequentially before branching — simpler than conditional per-branch reads"
  - "TDD RED-GREEN for behaviour change: update existing upsertMessages-asserting tests + add new setMessagesPreservingLocalOnly-asserting tests in RED, then fix production code"

requirements-completed: [PRES-02, PRES-03, PRES-07, PRES-08]

# Metrics
duration: 6min
completed: 2026-03-13
---

# Phase 2 Plan 02: Wire Full Trigger Coverage Summary

**setMessagesPreservingLocalOnly wired into all 4 remaining call sites: filteringOlderMessages, isFilteringNewerMessages (both end-reached branches), and updateDataForChannel reconnect + contiguous-upsert paths — every server message update trigger now preserves local-only messages**

## Performance

- **Duration:** 6 min
- **Started:** 2026-03-13T08:25:54Z
- **Completed:** 2026-03-13T08:31:54Z
- **Tasks:** 2 (each with TDD RED + GREEN phases)
- **Files modified:** 2

## Accomplishments
- Wired `setMessagesPreservingLocalOnly` into `filteringOlderMessages` branch — `loadBefore` (load older page) now preserves local-only messages at or above the window floor
- Wired `setMessagesPreservingLocalOnly` into `isFilteringNewerMessages` both sub-branches — `loadAfter` (mid-page) uses windowFloor; end-reached uses null floor (no-ceiling)
- Wired `updateDataForChannel` reconnect path: `isChannelsStateUpdate=false` (SyncManager reconnect) now uses preservation with persistedFloor; `isChannelsStateUpdate=true` (DB-seed) preserves full-replace invariant
- Wired `updateDataForChannel` else-upsert branch (contiguous incoming) with preservation
- Removed `TODO(Phase 2)` comment — reconnect floor read is fully implemented
- Added 8 new TDD tests for the wired branches; updated 5 existing tests for new behaviour

## Task Commits

Each task was committed atomically:

1. **Task 1 RED: Add failing tests for pagination branches** - `1551ef236f` (test)
2. **Task 1 GREEN: Wire setMessagesPreservingLocalOnly into pagination branches** - `065797e7c8` (feat)
3. **Task 2 GREEN: Wire updateDataForChannel reconnect path with preservation** - `e897300a41` (feat)

_Note: Tasks 1 and 2 share the same RED commit — all failing tests for both tasks were added together before any production code changes._

## Files Created/Modified
- `stream-chat-android/stream-chat-android-client/src/main/java/io/getstream/chat/android/client/internal/state/plugin/logic/channel/internal/ChannelLogicImpl.kt` - Wired setMessagesPreservingLocalOnly into 4 call sites; added DB prefetch in updateDataForChannel; added isChannelsStateUpdate discriminator; removed TODO(Phase 2)
- `stream-chat-android/stream-chat-android-client/src/test/java/io/getstream/chat/android/client/internal/state/plugin/logic/channel/internal/ChannelLogicImplTest.kt` - Added 8 new preservation tests; updated 5 existing tests to match new behaviour; added isNull import

## Decisions Made
- `isChannelsStateUpdate` boolean used as discriminator within the `shouldRefreshMessages || currentMessages.isEmpty()` branch rather than adding a new parameter — cleaner and the boolean already encodes the semantic difference
- DB prefetch (`selectLocalOnlyMessagesForChannel` + `selectOldestLoadedDateForChannel`) placed once before the `when` block — simpler than conditional reads per branch, and both values are needed by the reconnect and else branches regardless
- `endReached=true` passes `null` as windowFloor — semantically the same as the non-filtering (latest messages) path which already passed null; null = "no ceiling, include all local-only"
- DB-seed path invariant maintained: `isChannelsStateUpdate=true` still calls `setMessages` (full-replace) because OfflinePlugin already includes local-only messages in the DB data — preservation would incorrectly double-inject them

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None — the TDD approach surfaced that 5 existing tests needed updating to pass `isChannelsStateUpdate=true` explicitly (or to expect `setMessagesPreservingLocalOnly` instead of `upsertMessages`). All updates were straightforward.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- Full trigger coverage is complete: every server message update path (initial load, pagination, reconnect) now calls `setMessagesPreservingLocalOnly`
- The DB-seed invariant is preserved: `updateStateFromDatabase` → `updateDataForChannel(isChannelsStateUpdate=true)` still does full-replace
- Phase 2 plan 03 (integration/E2E verification) can proceed — all PRES-02, PRES-03, PRES-07, PRES-08 requirements satisfied
- 92 tests pass, BUILD SUCCESSFUL

---
*Phase: 02-full-trigger-coverage*
*Completed: 2026-03-13*
