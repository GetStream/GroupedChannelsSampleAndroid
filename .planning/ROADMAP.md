# Roadmap: Android Channel State — Local Message Preservation

## Overview

Three phases deliver the sliding window reconciliation mechanism. Phase 1 builds the core
preservation function and wires it into the initial channel load path. Phase 2 extends coverage
to pagination and reconnect triggers, including both DB-present and no-DB branches. Phase 3
closes the edge cases (search context, cached messages) and validates everything with targeted
tests. No phase touches the legacy path or public API surface.

## Phases

**Phase Numbering:**
- Integer phases (1, 2, 3): Planned milestone work
- Decimal phases (2.1, 2.2): Urgent insertions (marked with INSERTED)

Decimal phases appear between their surrounding integers in numeric order.

- [x] **Phase 1: Core Reconciliation** - Define `isLocalOnly()`, implement `setMessagesPreservingLocalOnly()`, wire into initial load path
- [ ] **Phase 2: Full Trigger Coverage** - Apply preservation to pagination and reconnect triggers; handle DB-present and no-DB paths
- [ ] **Phase 3: Edge Cases and Tests** - Search context preservation, cached message handling, and full test suite

## Phase Details

### Phase 1: Core Reconciliation
**Goal**: The fundamental preservation mechanism exists and guards the initial channel load path against silently dropping local-only messages
**Depends on**: Nothing (first phase)
**Requirements**: PRES-01, PRES-04, PRES-05, ARCH-01
**Success Criteria** (what must be TRUE):
  1. `Message.isLocalOnly()` returns true for all four message types: pending send (`SYNC_NEEDED`/`IN_PROGRESS`), send failed (`FAILED_PERMANENTLY`), ephemeral (`type == "ephemeral"`/`"error"`), and pending edit/delete (`SYNC_NEEDED` on a message with an existing server ID)
  2. `ChannelStateImpl.setMessagesPreservingLocalOnly(incoming)` merges incoming server messages with current local-only messages atomically using `_messages.update { }`, excluding local-only messages whose ID appears in the incoming list with `syncStatus == COMPLETED` (delivered)
  3. `ChannelLogicImpl.onQueryChannelResult` calls `setMessagesPreservingLocalOnly` instead of `setMessages` at the relevant call sites — a local-only message present before the call is present after
  4. Window floor is derived from the oldest `createdAt` in the incoming server page; local-only messages below the floor are excluded from the merged result
  5. All changes are internal to `stream-chat-android-client`; `ChannelLogicLegacyImpl` and `ChannelStateLegacyImpl` are unmodified; no new public API is introduced
**Plans**: 4 plans

Plans:
- [x] 01-01-PLAN.md — Wave 0: Create MessageIsLocalOnlyTest.kt and ChannelStateImplPreservationTest.kt stubs
- [x] 01-02-PLAN.md — Wave 1: Implement isLocalOnly() predicate, selectLocalOnlyMessagesForChannel DB layer, ChannelEntity.oldestLoadedDate + ChatDatabase v102
- [x] 01-03-PLAN.md — Wave 2: Implement ChannelStateImpl.setMessagesPreservingLocalOnly() with tests
- [x] 01-04-PLAN.md — Wave 3: Wire call sites in ChannelLogicImpl, add updateOldestLoadedDateForChannel, add ChannelLogicImplTest call-site tests

### Phase 2: Full Trigger Coverage
**Goal**: Preservation applies to every server update trigger — pagination (load older/newer) and reconnect — with correct behavior whether OfflinePlugin is installed or not
**Depends on**: Phase 1
**Requirements**: PRES-02, PRES-03, PRES-07, PRES-08
**Success Criteria** (what must be TRUE):
  1. After `loadOlderMessages` or `loadNewerMessages` returns, any local-only message that falls within the current server window (>= window floor) remains in state and is visible
  2. After a reconnect triggers a channel refresh via `SyncManager`, local-only messages that were in state before disconnect are still present after the channel re-loads — no separate reconnect code path is needed
  3. When OfflinePlugin is absent, local-only messages held in `state.messages.value` survive a `setMessagesPreservingLocalOnly` call (state acts as the source of truth)
  4. When OfflinePlugin is present, `RepositoryFacade.selectLocalOnlyMessagesForChannel(cid)` is called at preservation time and any DB-persisted local-only messages (`SYNC_NEEDED`, `FAILED_PERMANENTLY`) outside the current state page are included in the merged result
**Plans**: TBD

Plans:
- [ ] 02-01: TBD

### Phase 3: Edge Cases and Tests
**Goal**: The preservation mechanism is correct under search/jump-to-message context and validated by a test suite covering all core scenarios with no regressions
**Depends on**: Phase 2
**Requirements**: PRES-06, TEST-01
**Success Criteria** (what must be TRUE):
  1. When `insideSearch == true`, a `setMessagesPreservingLocalOnly` call preserves local-only messages in both `_messages` (the mid-page list) and `_cachedLatestMessages` (the latest-messages cache) — no local-only message is lost when the user is in jump-to-message mode
  2. A unit test verifies: a failed message present before `onQueryChannelResult` fires is present after, with the same ID and `syncStatus == FAILED_PERMANENTLY`
  3. A unit test verifies: an ephemeral message present before channel re-entry (`queryChannel` call) is present after the server result arrives
  4. A unit test verifies: a pending edit (`SYNC_NEEDED` on existing server ID) present before pagination is present after `loadOlderMessages` returns
  5. A unit test verifies: when OfflinePlugin is absent, a pending send present in state survives a server message update with no crash or data loss
**Plans**: TBD

Plans:
- [ ] 03-01: TBD

## Progress

**Execution Order:**
Phases execute in numeric order: 1 → 2 → 3

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Core Reconciliation | 4/4 | Complete | 2026-03-13 |
| 2. Full Trigger Coverage | 0/? | Not started | - |
| 3. Edge Cases and Tests | 0/? | Not started | - |
