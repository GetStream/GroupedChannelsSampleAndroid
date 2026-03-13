---
phase: 01-core-reconciliation
plan: 04
subsystem: state
tags: [kotlin, coroutines, channel-logic, message-preservation, room, tdd]

# Dependency graph
requires:
  - phase: 01-core-reconciliation
    plan: 02
    provides: "selectLocalOnlyMessagesForChannel DB query chain; ChannelEntity.oldestLoadedDate field"
  - phase: 01-core-reconciliation
    plan: 03
    provides: "ChannelStateImpl.setMessagesPreservingLocalOnly(incoming, localOnlyFromDb, windowFloor)"

provides:
  - "ChannelLogicImpl.updateMessages calls setMessagesPreservingLocalOnly at all three server-result call sites"
  - "ChannelDao.updateOldestLoadedDate @Query UPDATE targeted to oldestLoadedDate column"
  - "ChannelRepository.updateOldestLoadedDateForChannel interface + NoOp + Database implementations"
  - "windowFloor derived from min(channel.messages.createdAt) and persisted to DB when non-null"
  - "4 new ChannelLogicImplTest.PreservationCallSites cases passing"
  - "Phase 1 complete: PRES-01, PRES-04, ARCH-01 fully satisfied end-to-end"

affects:
  - "02: Phase 2 DB-seed path reads ChannelEntity.oldestLoadedDate as window floor (TODO comment in code)"

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "coroutineScope.launch { } in non-suspend onQueryChannelResult to enable suspend prefetch before updateMessages"
    - "windowFloor = min(channel.messages.mapNotNull { getCreatedAtOrNull() }.minOrNull()) — null on empty page"
    - "Targeted DAO UPDATE: updateOldestLoadedDate only writes one field, avoids full-row replace"

key-files:
  created: []
  modified:
    - "stream-chat-android-client/src/main/java/io/getstream/chat/android/client/internal/state/plugin/logic/channel/internal/ChannelLogicImpl.kt"
    - "stream-chat-android-client/src/main/java/io/getstream/chat/android/client/internal/offline/repository/domain/channel/internal/ChannelDao.kt"
    - "stream-chat-android-client/src/main/java/io/getstream/chat/android/client/persistance/repository/ChannelRepository.kt"
    - "stream-chat-android-client/src/main/java/io/getstream/chat/android/client/persistance/repository/noop/NoOpChannelRepository.kt"
    - "stream-chat-android-client/src/main/java/io/getstream/chat/android/client/internal/offline/repository/domain/channel/internal/DatabaseChannelRepository.kt"
    - "stream-chat-android-client/src/test/java/io/getstream/chat/android/client/internal/state/plugin/logic/channel/internal/ChannelLogicImplTest.kt"

key-decisions:
  - "coroutineScope.launch used in onQueryChannelResult (non-suspend) to perform DB prefetch before updateMessages; UnconfinedTestDispatcher ensures launch executes eagerly in tests"
  - "updateMessages made suspend to allow floor persistence call (repository.updateOldestLoadedDateForChannel) inside the function"
  - "DB-seed path (updateDataForChannel) unchanged — state.setMessages at line 323 retained; OfflinePlugin already includes local-only in DB data"
  - "RepositoryFacade picks up updateOldestLoadedDateForChannel via ChannelRepository delegation — no RepositoryFacade.kt modification needed"
  - "Test verify uses anyOrNull() for localOnlyFromDb param — Mockito mock may return null for unstubbed suspend List<Message> return"

patterns-established:
  - "Pattern: non-suspend lifecycle hook + coroutineScope.launch for async prefetch before state update"
  - "Pattern: targeted DAO UPDATE (single field) vs full entity replace — preserves other fields without cache invalidation"

requirements-completed: [PRES-01, PRES-04, ARCH-01]

# Metrics
duration: 17min
completed: 2026-03-13
---

# Phase 01 Plan 04: ChannelLogicImpl Wiring Summary

**setMessagesPreservingLocalOnly wired into ChannelLogicImpl.updateMessages at all three server-result call sites, with DB floor persistence via targeted ChannelDao UPDATE and 4 new ChannelLogicImplTest.PreservationCallSites cases passing**

## Performance

- **Duration:** 17 min
- **Started:** 2026-03-13T01:41:48Z
- **Completed:** 2026-03-13T01:57:55Z
- **Tasks:** 2
- **Files modified:** 6

## Accomplishments

- `updateOldestLoadedDateForChannel(cid, date)` added to the full repository layer: `ChannelDao` (@Query UPDATE), `ChannelRepository` interface, `NoOpChannelRepository` (Unit), `DatabaseChannelRepository` (delegates to DAO). `RepositoryFacade` picks it up automatically via `ChannelRepository by channelsRepository` delegation.
- `ChannelLogicImpl.updateMessages` made `suspend`, signature extended with `localOnlyFromDb: List<Message>` and `windowFloor: Date?`. Three `state.setMessages` call sites replaced with `state.setMessagesPreservingLocalOnly(channel.messages, localOnlyFromDb, windowFloor)`.
- `ChannelLogicImpl.onQueryChannelResult` wraps the message-update block in `coroutineScope.launch { }` to prefetch `localOnlyFromDb` from DB and derive `windowFloor` before calling the now-suspend `updateMessages`.
- Floor persistence: `repository.updateOldestLoadedDateForChannel(cid, windowFloor)` called at the end of `updateMessages` when `windowFloor != null`.
- DB-seed path (`updateDataForChannel`, `shouldRefreshMessages=true`) unchanged — `state.setMessages(sortedMessages)` at line 323 retained.
- 4 new `ChannelLogicImplTest.PreservationCallSites` cases passing (no-filtering, aroundId not-in-search, aroundId in-search, updateDataForChannel DB-seed).
- All 12 `ChannelStateImplPreservationTest` and all 8 `MessageIsLocalOnlyTest` cases still pass.
- Full `:stream-chat-android-client:testDebugUnitTest` suite green.

## Task Commits

1. **Task 1: Add updateOldestLoadedDateForChannel to channel repository layer** — `40b910e697` (feat)
2. **Task 2: Wire setMessagesPreservingLocalOnly into ChannelLogicImpl** — `165d4e68e1` (feat)

## Files Created/Modified

- `stream-chat-android-client/src/main/java/.../ChannelLogicImpl.kt` (modified) — `updateMessages` made suspend + 2 new params; 3 `setMessages` call sites replaced; `coroutineScope.launch` block added in `onQueryChannelResult`; floor persistence added after `when` block
- `stream-chat-android-client/src/main/java/.../ChannelDao.kt` (modified) — Added `updateOldestLoadedDate(cid, date)` @Query UPDATE
- `stream-chat-android-client/src/main/java/.../ChannelRepository.kt` (modified) — Added `updateOldestLoadedDateForChannel(cid, date)` interface method
- `stream-chat-android-client/src/main/java/.../NoOpChannelRepository.kt` (modified) — Added no-op override returning `Unit`
- `stream-chat-android-client/src/main/java/.../DatabaseChannelRepository.kt` (modified) — Added implementation delegating to `channelDao.updateOldestLoadedDate`
- `stream-chat-android-client/src/test/java/.../ChannelLogicImplTest.kt` (modified) — Added `PreservationCallSites` nested class with 4 test cases; updated 3 existing `setMessages` verifications to `setMessagesPreservingLocalOnly`; added `anyOrNull` import

## Decisions Made

- `coroutineScope.launch { }` used in `onQueryChannelResult` (non-suspend override) to enable the DB prefetch. With `UnconfinedTestDispatcher` (the test coroutine extension), launched coroutines execute eagerly in tests — so existing tests that don't use `runTest` continue to work.
- `updateMessages` made `suspend` (was private fun) — this is the only way to cleanly call `repository.updateOldestLoadedDateForChannel` (suspend) and `repository.selectLocalOnlyMessagesForChannel` (suspend) from within the function body. The call site is already inside the `coroutineScope.launch` block.
- Test verify for `localOnlyFromDb` uses `anyOrNull()` not `any()`. Mockito's default for an unstubbed suspend function returning `List<Message>` may return `null` through the coroutine continuation mechanism; `anyOrNull()` handles both null and non-null cases.
- No changes to `RepositoryFacade.kt` — both `selectLocalOnlyMessagesForChannel` (MessageRepository delegation) and `updateOldestLoadedDateForChannel` (ChannelRepository delegation) are available automatically.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Used anyOrNull() for localOnlyFromDb test verify parameter**

- **Found during:** Task 2 test execution (TDD GREEN phase)
- **Issue:** Mockito's default behavior for an unstubbed suspend `List<Message>` return type may yield null through the coroutine continuation mechanism. Using `any()` (which doesn't match null in Kotlin) caused 6 test failures.
- **Fix:** Changed `any()` to `anyOrNull()` for the `localOnlyFromDb` parameter in all `setMessagesPreservingLocalOnly` verify calls.
- **Files modified:** `ChannelLogicImplTest.kt`
- **Verification:** All 6 previously-failing tests pass with `anyOrNull()`
- **Committed in:** `165d4e68e1` (Task 2 commit)

---

**Total deviations:** 1 auto-fixed (Rule 1 — test verify argument matcher correction)
**Impact on plan:** Minor test fix. Behavior is correctly verified — `anyOrNull()` is the appropriate matcher for a parameter that may be null when the mock hasn't been explicitly stubbed.

## Issues Encountered

- `./gradlew :stream-chat-android-client:compileKotlin` fails with "ambiguous task" on Android modules (same as Plans 01-03). Used `compileDebugKotlin` for compilation verification. This is a known issue documented in STATE.md decisions.
- Internal Kotlin functions (`setMessagesPreservingLocalOnly`) compile to mangled bytecode names (appended with `$module_name`). Mockito tracks and verifies the mangled name correctly when using `verify(mock).method()` from within the same source set.

## User Setup Required

None — no external service configuration required.

## Next Phase Readiness

- **Phase 1 complete.** All 4 plans delivered:
  - Plan 01: Wave 0 @Disabled test stubs
  - Plan 02: isLocalOnly() predicate + selectLocalOnlyMessagesForChannel DB chain + ChannelEntity.oldestLoadedDate field
  - Plan 03: ChannelStateImpl.setMessagesPreservingLocalOnly implementation (12 tests)
  - Plan 04: ChannelLogicImpl wiring (this plan)
- **Phase 2 prerequisite:** `ChannelEntity.oldestLoadedDate` is persisted after each `onQueryChannelResult`. Phase 2 DB-seed path in `updateDataForChannel` can read this value to reconstruct the window floor without a network response. The TODO comment in `updateMessages` marks this integration point.
- **No blockers for Phase 2.**

---
*Phase: 01-core-reconciliation*
*Completed: 2026-03-13*

## Self-Check: PASSED

- FOUND: ChannelLogicImpl.kt (modified — updateMessages suspend, 3 call sites changed)
- FOUND: ChannelDao.kt (modified — updateOldestLoadedDate @Query)
- FOUND: ChannelRepository.kt (modified — updateOldestLoadedDateForChannel interface method)
- FOUND: NoOpChannelRepository.kt (modified — no-op override)
- FOUND: DatabaseChannelRepository.kt (modified — implementation)
- FOUND: ChannelLogicImplTest.kt (modified — PreservationCallSites + 3 updated verifications)
- FOUND commit: 40b910e697 (Task 1: repository layer)
- FOUND commit: 165d4e68e1 (Task 2: ChannelLogicImpl wiring)
- FOUND: state.setMessages at updateDataForChannel line 323 (DB-seed path unchanged)
- FOUND: state.setMessagesPreservingLocalOnly at updateMessages lines 408, 422, 426
- FOUND: repository.updateOldestLoadedDateForChannel in updateMessages when windowFloor != null
- VERIFIED: ChannelLogicLegacyImpl has no setMessagesPreservingLocalOnly or isLocalOnly references
- VERIFIED: Full testDebugUnitTest suite passes (BUILD SUCCESSFUL)
