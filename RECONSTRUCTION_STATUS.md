# CC:HQ Sound Physics Compat — current project status

> **Start here in a new chat:** `docs/NEXT_CHAT_HANDOFF_2026-09-06.md`
>
> That document is the detailed continuation record. This file is the compact status view.

Repository:

`https://github.com/ztawfik523-lgtm/cchq-soundphysics-compat`

Current working branch:

`phase5-v7-1-lifecycle-state-finish`

Current project direction: the reconstruction is complete and the project is in finalization. The stable acoustic reference is **V7.1**; current work is about performance housekeeping, correctness, lifecycle stability, validation and release cleanup.

---

## Phase status

| Phase | Status |
| --- | --- |
| Phase 1 — freeze and inventory binary baseline | **COMPLETE / JAR-RECHECKED** |
| Phase 2 — reconstruct build project | **COMPLETE / JAR-RECHECKED** |
| Phase 3 — reconstruct Java source | **COMPLETE / RECHECKED** |
| Phase 4 — structural and behavioral equivalence audit | **COMPLETE / RECHECKED** |
| Phase 5 — runtime fixes, stable acoustic baseline, performance/lifecycle finalization | **IN FINALIZATION** |

Phase 5 already contains user-approved HF50 behavior, user-tested/frozen V7.1 diffraction, a CI-validated performance housekeeping pass, and a CI-validated lifecycle/state pass. The lifecycle candidate still needs the user's runtime lifecycle test.

---

## Original Hotfix3 baseline

Artifact:

`cchq_soundphysics_compat-0.1.0-beta11-hotfix3.jar`

SHA-256:

`83500f182fc9829aa1a5a51fbfa11ba6cdfb645699b25d1c445167666dabc1ef`

Pinned environment:

- Java 21
- Minecraft 1.21.1
- NeoForge 21.1.248
- ModDevGradle 2.0.144
- Gradle 9.2.1
- CC:Tweaked 1.120.2
- CC:HQ Speakers `ygA78R8l/u5PEI5Ax`, runtime `1.1.4-neoforge-1.21.1`
- Sound Physics Remastered 1.21.1-1.5.1 / `Dd2tmpsk`
- Cloth Config 15.0.140

Phase 4 final audited code/build:

`98e7dedb7ecf6fda22008b084b6bb41956edff78`

Final Phase 4 evidence:

- 60/60 class paths exact
- 60/60 structural ABI exact
- 69/69 constant values exact
- zero bootstrap/string-concat recipe mismatches
- 11/11 configured Mixin/accessor annotation sets reconciled
- 5/5 runtime resources byte-exact
- 550 methods audited
- no unresolved proven behavior discrepancy at closure

---

## Stable acoustic reference — V7.1

V7.1 is the frozen, stable sound baseline for the current project.

Commit:

`ffcf5f6e05d85b69f1f1dff8cfae1b082b71604d`

Refs:

- `phase5-diffraction-v7-1-runtime-approved`
- `archive-phase5-diffraction-v7-1-runtime-approved`

Accepted JAR SHA-256:

`30d457c2a52672f893b1076938e2fdea3f41759173dfd843ff652bd490692101`

The stable behavior includes the previously approved synchronized-speaker HF50 fix.

HF50 source:

`62d3a7a0a176c901402b913946d98f3cb455a8f4`

HF50 JAR SHA-256:

`fe894a42eebeea37e77f63e9acf65df22bdac72897fb6ac1eb9def198dcd032a`

Known characteristics of V7.1:

- bounded opening discovery radius of 8;
- opening contribution falls with explicit aperture spreading;
- the radius boundary can produce a noticeable transition beyond the search range;
- the accepted 3-deep-hole result is a little darker than one earlier prototype while portal contribution is still active;
- moving playback is substantially more expensive than stationary playback because progressive direct/SPR work refreshes heavily during movement.

These are documented properties of the stable reference, not evidence that the current source is unfinished.

---

## V7.1 performance benchmark result

The user benchmarked one and four sources; the planned 12-speaker section was skipped.

Representative four-source, diffraction-ON, moving windows:

- 14.4 acoustic / 6.8 SPR ms/s
- 13.7 acoustic / 6.9 SPR ms/s
- 14.6 acoustic / 7.4 SPR ms/s

Average:

- acoustic about 14.23 ms/s
- SPR about 7.03 ms/s

Important scaling observations:

- listener topology scans do not multiply by speaker count in the tested stationary case;
- listener-side lower portal leg is shared;
- source-side cross leg scales per source as expected;
- measured portal SPR-ray CPU is tiny;
- movement cost is mainly the existing progressive/direct/SPR machinery.

The detailed benchmark table and strict 10-second-window interpretation are in `docs/NEXT_CHAT_HANDOFF_2026-09-06.md`.

---

## Performance housekeeping pass

Branch:

`phase5-v7-1-performance-pass`

Clean head:

`962eab8b052466ca984496a7dec0767dc65803f4`

Changes are intentionally small and behavior-neutral:

- cache pruning normally runs at most once per second;
- soft-limit overflow still prunes immediately;
- stale lookup TTL/recheck semantics remain unchanged;
- avoid candidate-list allocations on immediate-empty paths;
- pre-size candidate lists.

A more aggressive multi-cell topology cache was considered and left out because it could reuse older topology state after A -> B -> A movement and subtly change the acoustic timeline.

Successful validation run:

`33957036689`

JAR SHA-256:

`b7945374fc95935a3c951e660efb660bbcc777744395e81849b5abf680592b41`

Class count: 81.

---

## Lifecycle/state finalization pass

Current branch:

`phase5-v7-1-lifecycle-state-finish`

Performance base:

`962eab8b052466ca984496a7dec0767dc65803f4`

Clean lifecycle source checkpoint before documentation-only commits:

`be03d30efe98ca03bdf27764bcea567df5ef3875`

Compared with the performance base, the clean lifecycle source changes only:

- `src/main/java/dev/cchqphysics/compat/audio/CompatAudioManager.java`
- `src/main/java/dev/cchqphysics/compat/mixin/SoundEngineLifecycleMixin.java`

Three concrete state issues were fixed:

1. **ClientLevel identity:** actual level identity is tracked so disconnect/rejoin and dimension/world replacement invalidate the previous session state.
2. **Teardown ordering:** normal stop-all and emergency shutdown use blocking sound-thread cleanup, removing the race between queued compat cleanup and vanilla sound executor/OpenAL destruction. Redundant destroy/reload hooks were removed in favor of the authoritative stop-all path.
3. **Failed-start cleanup:** if source creation fails after compat registration but before installation into `ACTIVE`, the failure path unregisters the source before deleting the raw OpenAL source.

Lifecycle source commits:

- `24e41b49f8ccd5b32e484b90def8f50c3767c8d9`
- `cfcc122da3be03171377f14717ae30b4c6bbb696`

Successful validation run:

`33962380234`

Job:

`101296383519`

Artifact:

`9968357487`

Lifecycle candidate JAR SHA-256:

`6d0fa98ee6c76d23a3e0764501d16dc5c993149e0de77181cdab6fc0a9abdc18`

Class count: 81.

CI metadata recorded `acoustic_model_changes=false`, `lua_changes=false`, and `game_launch_performed=false`.

The built candidate still reports internal version `0.1.0-beta11-phase5-v7-1-performance-test`; release naming cleanup is still pending.

---

## Current invariants worth checking during final audit

These are the established architectural facts most useful when reviewing finishing changes:

- `SoundSource.BLOCKS` distance behavior;
- center + 8 inner + 8 outer direct geometry;
- approved progressive full/partial refresh behavior;
- private per-source EFX and direct/aux reattachment on actual environment application;
- private EFX setup only once the source is PLAYING/PAUSED eligible;
- SPR `calculateOcclusion()` remains part of the normal pipeline;
- no worker decode thread owns SPR world/geometry raycasts;
- strict source generation/lifetime semantics;
- Hotfix3 partial sync grace and pending-INITIAL protection;
- Beta10 exact direct reuse / stable OpenAL write suppression;
- Beta11 exact same-clone room-ray memoization with cross-clone reuse as telemetry;
- exact eligibility expression:

  `Beta9Optimizer.isAudibleAndRecord(state.sourceId) & beta9EligibleReal(state, now)`

These are comparison points for a final audit, not a replacement for evidence if a real bug is found later.

---

## Immediate next checkpoint

The lifecycle candidate is source/CI validated but has not yet received the user's runtime lifecycle test.

Candidate SHA:

`6d0fa98ee6c76d23a3e0764501d16dc5c993149e0de77181cdab6fc0a9abdc18`

Runtime sequence:

1. play -> stop -> play
2. pause -> resume
3. stopAll -> restart
4. sound/resource reload -> restart
5. disconnect -> rejoin -> restart
6. dimension change -> restart
7. four speakers playing for several minutes while moving
8. upload `latest.log` and `debug.log`

If that run is clean, the natural next step is release cleanup/final audit: final version naming, presentation cleanup, final source comparison, final JAR/resource/hash inspection, and a final stable/archive checkpoint.

`main` remains untouched at this handoff; any eventual merge can stay an explicit release decision.
