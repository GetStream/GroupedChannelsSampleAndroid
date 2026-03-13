---
phase: 02-full-trigger-coverage
plan: 01
subsystem: database
tags: [room, dao, repository, channel, kotlin]

# Dependency graph
requires:
  - phase: 01-core-reconciliation
    provides: updateOldestLoadedDateForChannel (write path) and ChannelEntity.oldestLoadedDate column
provides:
  - selectOldestLoadedDate(cid) @Query on ChannelDao — single-column SELECT for oldestLoadedDate
  - selectOldestLoadedDateForChannel(cid): Date? on ChannelRepository interface
  - NoOpChannelRepository.selectOldestLoadedDateForChannel returns null (no-floor signal)
  - DatabaseChannelRepository.selectOldestLoadedDateForChannel delegates to channelDao.selectOldestLoadedDate
affects: [02-02, 02-03, ChannelLogicImpl updateDataForChannel DB-seed path]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - Single-column DAO SELECT to avoid full entity fetch when only one field needed
    - No-op returns null = "no floor" — preservation logic treats null as preserve-all

key-files:
  created: []
  modified:
    - stream-chat-android/stream-chat-android-client/src/main/java/io/getstream/chat/android/client/internal/offline/repository/domain/channel/internal/ChannelDao.kt
    - stream-chat-android/stream-chat-android-client/src/main/java/io/getstream/chat/android/client/persistance/repository/ChannelRepository.kt
    - stream-chat-android/stream-chat-android-client/src/main/java/io/getstream/chat/android/client/persistance/repository/noop/NoOpChannelRepository.kt
    - stream-chat-android/stream-chat-android-client/src/main/java/io/getstream/chat/android/client/internal/offline/repository/domain/channel/internal/DatabaseChannelRepository.kt

key-decisions:
  - "Single-column DAO SELECT avoids full entity fetch + Channel model conversion (Channel model does not carry oldestLoadedDate)"
  - "RepositoryFacade requires zero changes — ChannelRepository by channelsRepository delegation auto-exposes new method"
  - "NoOpChannelRepository returns null to signal no floor (OfflinePlugin absent = preserve all local-only messages)"

patterns-established:
  - "Single-column SELECT pattern: use @Query targeting specific column when Channel model conversion would lose the field"
  - "Repository delegation transparency: new ChannelRepository interface methods are automatically exposed via RepositoryFacade without editing it"

requirements-completed: [PRES-08]

# Metrics
duration: 3min
completed: 2026-03-13
---

# Phase 2 Plan 01: selectOldestLoadedDateForChannel Repository Method Summary

**Single-column Room DAO SELECT + matching ChannelRepository interface/no-op/DB implementations enabling DB-seed reconnect path to read persisted window floor without full entity fetch**

## Performance

- **Duration:** 3 min
- **Started:** 2026-03-13T08:20:31Z
- **Completed:** 2026-03-13T08:23:02Z
- **Tasks:** 2
- **Files modified:** 4

## Accomplishments
- Added `selectOldestLoadedDate(cid)` @Query to ChannelDao — a single-column SELECT targeting the `oldestLoadedDate` column, avoiding full entity deserialization
- Added `selectOldestLoadedDateForChannel(cid): Date?` to ChannelRepository interface with KDoc noting null semantics
- Added no-op override returning null in NoOpChannelRepository (signals "no floor" to preservation logic)
- Added DB implementation in DatabaseChannelRepository delegating directly to channelDao.selectOldestLoadedDate

## Task Commits

Each task was committed atomically:

1. **Task 1: Add selectOldestLoadedDate to ChannelDao** - `4f74e2148e` (feat)
2. **Task 2: Add selectOldestLoadedDateForChannel to repository layer** - `229fd38cd0` (feat)

## Files Created/Modified
- `stream-chat-android/stream-chat-android-client/src/main/java/io/getstream/chat/android/client/internal/offline/repository/domain/channel/internal/ChannelDao.kt` - Added selectOldestLoadedDate @Query method
- `stream-chat-android/stream-chat-android-client/src/main/java/io/getstream/chat/android/client/persistance/repository/ChannelRepository.kt` - Added selectOldestLoadedDateForChannel interface method
- `stream-chat-android/stream-chat-android-client/src/main/java/io/getstream/chat/android/client/persistance/repository/noop/NoOpChannelRepository.kt` - Added no-op override returning null
- `stream-chat-android/stream-chat-android-client/src/main/java/io/getstream/chat/android/client/internal/offline/repository/domain/channel/internal/DatabaseChannelRepository.kt` - Added DB implementation delegating to DAO

## Decisions Made
- Used a dedicated single-column DAO query rather than reusing `selectChannel` because the `Channel` model does not carry `oldestLoadedDate` — the field exists only on `ChannelEntity` and is not mapped through the entity-to-model conversion
- RepositoryFacade required zero changes because the `ChannelRepository by channelsRepository` delegation pattern automatically exposes any new interface method

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
None - the commit had to be made inside the `stream-chat-android/` subdirectory repo (it is its own git repo), not the workspace root. This is consistent with Phase 1 execution.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- `selectOldestLoadedDateForChannel` is callable from any code holding a `RepositoryFacade` reference
- Phase 2 plan 02 (ChannelLogicImpl wiring) can now read the persisted floor from DB at reconnect/seed time
- All success criteria satisfied: no-op returns null, DB implementation returns persisted Date, RepositoryFacade unchanged

---
*Phase: 02-full-trigger-coverage*
*Completed: 2026-03-13*
