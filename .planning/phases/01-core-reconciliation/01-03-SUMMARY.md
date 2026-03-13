---
phase: 01-core-reconciliation
plan: 03
subsystem: state
tags: [kotlin, stateflow, channel-state, message-preservation, tdd, atomic-merge]

# Dependency graph
requires:
  - phase: 01-core-reconciliation
    plan: 01
    provides: "Wave 0 @Disabled stubs for ChannelStateImplPreservationTest (12 tests)"
  - phase: 01-core-reconciliation
    plan: 02
    provides: "Message.isLocalOnly() predicate in MessageLocalOnlyExt.kt"
provides:
  - "ChannelStateImpl.setMessagesPreservingLocalOnly(incoming, localOnlyFromDb, windowFloor) internal method"
  - "12 passing ChannelStateImplPreservationTest cases covering all preservation scenarios"
  - "setMessages full-replace semantics intact (unchanged)"
affects:
  - "01-04: ChannelLogicImpl call sites switch from setMessages() to setMessagesPreservingLocalOnly()"

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "_messages.update { current -> } CAS atomic merge — never direct value assignment for preservation path"
    - "6-step merge algorithm: fromState union + DB union + dedup + ID collision (server wins) + floor filter + sort"
    - "shouldIgnoreUpsertion() reused as guard for incoming messages (same as setMessages)"
    - "Post-update registration loop mirrors setMessages lines 287-291 (addQuotedMessage / registerPollForMessage)"

key-files:
  created: []
  modified:
    - "stream-chat-android-client/src/main/java/io/getstream/chat/android/client/internal/state/plugin/state/channel/internal/ChannelStateImpl.kt"
    - "stream-chat-android-client/src/test/java/io/getstream/chat/android/client/internal/state/plugin/state/channel/internal/ChannelStateImplPreservationTest.kt"

key-decisions:
  - "setMessagesPreservingLocalOnly uses _messages.update { } (CAS) not _messages.value = ... to avoid two-emission flicker"
  - "setMessages intentionally kept as _messages.value = messagesToSet — DB-seed path requires full-replace (OfflinePlugin already includes local-only in DB data)"
  - "No-DB fallback is implicit: fromState = current.filter { isLocalOnly() } collects in-memory local-only before DB union"
  - "windowFloor null = no floor restriction (empty incoming page or first open) — all local-only included"
  - "createdAt null treated as at-or-above floor (include) — prevents exclusion of messages with missing timestamps"

patterns-established:
  - "Pattern: atomic merge in _messages.update lambda — read current state, compute new list, write in single CAS operation"
  - "Pattern: server wins on ID collision via incomingIds set lookup before merging"
  - "Pattern: !d.before(windowFloor) for inclusive floor comparison (>= not >)"

requirements-completed: [PRES-01, PRES-04, PRES-05]

# Metrics
duration: 8min
completed: 2026-03-13
---

# Phase 01 Plan 03: setMessagesPreservingLocalOnly Implementation Summary

**Atomic CAS merge function on ChannelStateImpl that preserves local-only messages (FAILED_PERMANENTLY, ephemeral, AWAITING_ATTACHMENTS, SYNC_NEEDED) when server data replaces the channel message window**

## Performance

- **Duration:** 8 min
- **Started:** 2026-03-13T01:19:16Z
- **Completed:** 2026-03-13T01:27:30Z
- **Tasks:** 1 (TDD: RED + GREEN)
- **Files modified:** 2

## Accomplishments

- `ChannelStateImpl.setMessagesPreservingLocalOnly(incoming, localOnlyFromDb, windowFloor)` implemented as an internal method immediately after `setMessages`
- 6-step merge algorithm: (1) gather in-memory local-only via `isLocalOnly()`, (2) union with DB-sourced local-only, (3) dedup by ID, (4) drop where server has same ID (server wins), (5) apply windowFloor filter, (6) sort with MESSAGE_COMPARATOR
- All 12 `ChannelStateImplPreservationTest` cases pass; all 8 `MessageIsLocalOnlyTest` cases still pass
- `setMessages` is untouched — full-replace semantics preserved for DB-seed path

## Task Commits

TDD task committed as two phases:

1. **TDD RED — failing tests** — `c103d87538` (test)
2. **TDD GREEN — implementation** — `9f58487a43` (feat)

## Files Created/Modified

- `stream-chat-android-client/src/main/java/.../ChannelStateImpl.kt` (modified) — Added `setMessagesPreservingLocalOnly` internal method (~64 lines) after `setMessages`; `setMessages` unchanged
- `stream-chat-android-client/src/test/java/.../ChannelStateImplPreservationTest.kt` (modified) — Replaced all 12 `@Disabled` stubs with real test implementations; imports added (`java.util.Date`, `java.util.concurrent.TimeUnit`, JUnit assertions)

## Decisions Made

- Used `_messages.update { current -> }` CAS pattern throughout the lambda — reads current state, computes merged list, writes atomically in a single emission. The existing `setMessages` intentionally uses `_messages.value = messagesToSet` (full replace) which is correct for the DB-seed path where OfflinePlugin already includes local-only messages.
- `windowFloor == null` means no floor restriction — this covers the case of an empty incoming page or first-ever channel open. All local-only messages are included.
- `createdAt == null` treated as at-or-above floor (include). The expression `msg.getCreatedAtOrNull()?.let { d -> !d.before(windowFloor) } ?: true` returns `true` when `createdAt` is null, preventing accidental exclusion.
- No-DB fallback is implicit: `fromState = current.filter { it.isLocalOnly() }` collects all in-memory local-only messages before the DB union step. When `localOnlyFromDb` is `emptyList()`, in-memory messages are still preserved.

## Deviations from Plan

None - plan executed exactly as written. The implementation matches the specification in PLAN.md exactly, including the 7-step comment structure in the code.

## Issues Encountered

- `./gradlew :stream-chat-android-client:compileKotlin` fails with "ambiguous task" on Android module (same as Plans 01 and 02). Used `compileDebugKotlin` for production code verification. This is a known issue documented in STATE.md decisions.

## User Setup Required

None — no external service configuration required.

## Next Phase Readiness

- Plan 04 (`ChannelLogicImpl` wiring) can now call `channelState.setMessagesPreservingLocalOnly(incoming, localOnlyFromDb, windowFloor)` at the 3 call sites in `updateMessages` (~lines 396, 411, 414)
- The DB-seed path at ~line 316 (`updateDataForChannel`) must continue using `setMessages` — not preservation path
- No blockers for Plan 04

---
*Phase: 01-core-reconciliation*
*Completed: 2026-03-13*

## Self-Check: PASSED

- FOUND: ChannelStateImpl.kt (modified)
- FOUND: ChannelStateImplPreservationTest.kt (modified)
- FOUND: 01-03-SUMMARY.md
- FOUND commit: c103d87538 (TDD RED — failing tests)
- FOUND commit: 9f58487a43 (TDD GREEN — implementation)
- FOUND: setMessagesPreservingLocalOnly in ChannelStateImpl.kt
- FOUND: setMessages uses _messages.value = (full-replace semantics intact)
- FOUND: _messages.update { current -> } CAS pattern in setMessagesPreservingLocalOnly
