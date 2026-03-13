# Requirements: Android Channel State — Local Message Preservation

**Defined:** 2026-03-12
**Core Value:** Local-only messages are never silently discarded when server data updates the channel state.

## v1 Requirements

### Message Preservation

- [x] **PRES-01**: When `onQueryChannelResult` fires (initial channel load), local-only messages present in state are merged back rather than discarded
- [ ] **PRES-02**: When a pagination response arrives (loadOlderMessages / loadNewerMessages), local-only messages in the affected window are preserved
- [ ] **PRES-03**: When reconnect sync triggers a channel refresh, local-only messages are preserved
- [x] **PRES-04**: Window boundaries are derived from the server response fields (`oldestMessageAt` / `newestMessageAt`) where available, matching the iOS approach
- [x] **PRES-05**: All four local-only message types are preserved: pending send (SYNC_NEEDED/IN_PROGRESS), send failed (FAILED_PERMANENTLY), ephemeral (`type == "ephemeral"`), and pending edit/delete
- [ ] **PRES-06**: When `insideSearch == true`, reconciliation does not reset the active window — local-only messages remain visible in search context without corrupting jump-to-message state
- [ ] **PRES-07**: When OfflinePlugin is absent (no DB), in-memory local-only messages are preserved across server updates (best-effort, state as pseudo-SSOT)
- [ ] **PRES-08**: When OfflinePlugin is present, DB-persisted local-only messages (SYNC_NEEDED, FAILED_PERMANENTLY) are included in the merged result after a server update

### Architecture

- [x] **ARCH-01**: The reconciliation mechanism lives in the state layer (`stream-chat-android-client`), has no public API surface, and does not touch the legacy channel logic path (`ChannelLogicLegacyImpl` / `ChannelStateLegacyImpl`)

### Testing

- [ ] **TEST-01**: Test coverage for all core scenarios: failed message survives server reload, ephemeral message survives channel re-entry, pending edit survives pagination, no regression when OfflinePlugin is absent

## v2 Requirements

### Thread State

- **THRD-01**: Thread message reconciliation — local-only messages in thread state preserved across server updates (separate scoped fix)

## Out of Scope

| Feature | Reason |
|---------|--------|
| DB-as-SSOT full refactor | Too large, explicitly deferred |
| Public SDK API surface changes | Internal implementation only |
| Structural alignment with iOS SDK | Behavioral alignment only; Android plugin arch differs too much |
| Legacy channel logic path changes | `ChannelLogicLegacyImpl` / `ChannelStateLegacyImpl` must not be affected |
| Thread state reconciliation | Out of scope for this milestone |

## Traceability

Which phases cover which requirements. Updated during roadmap creation.

| Requirement | Phase | Status |
|-------------|-------|--------|
| PRES-01 | Phase 1 | Complete |
| PRES-04 | Phase 1 | Complete |
| PRES-05 | Phase 1 | Complete |
| ARCH-01 | Phase 1 | Complete |
| PRES-02 | Phase 2 | Pending |
| PRES-03 | Phase 2 | Pending |
| PRES-07 | Phase 2 | Pending |
| PRES-08 | Phase 2 | Pending |
| PRES-06 | Phase 3 | Pending |
| TEST-01 | Phase 3 | Pending |

**Coverage:**
- v1 requirements: 10 total
- Mapped to phases: 10
- Unmapped: 0 ✓

---
*Requirements defined: 2026-03-12*
*Last updated: 2026-03-12 — traceability filled after roadmap creation*
