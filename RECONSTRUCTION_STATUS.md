# CC:HQ Sound Physics Compat reconstruction / Phase 5 status

> **Read first:** `docs/NEXT_CHAT_HANDOFF_2026-09-06.md`
>
> That file is the authoritative continuation document for the next session. It contains exact commits, hashes, accepted/rejected experiments, benchmark numbers, lifecycle fixes, and the pending runtime procedure.

Historical authoritative baseline:

`cchq_soundphysics_compat-0.1.0-beta11-hotfix3.jar`

SHA-256:

`83500f182fc9829aa1a5a51fbfa11ba6cdfb645699b25d1c445167666dabc1ef`

Current working branch:

`phase5-v7-1-lifecycle-state-finish`

Current project rule: **no more acoustic features/tuning. Work is now performance, correctness/stability, validation, cleanup and release finishing only.**

## Canonical five-phase status

| Phase | Status |
| --- | --- |
| Phase 1 — Freeze and inventory binary baseline | **COMPLETE / JAR-RECHECKED** |
| Phase 2 — Reconstruct build project | **COMPLETE / JAR-RECHECKED** |
| Phase 3 — Reconstruct every Java class | **COMPLETE / RECHECKED** |
| Phase 4 — Structural and behavioral equivalence audit | **COMPLETE / RECHECKED** |
| Phase 5 — Runtime validation, approved fixes, performance, lifecycle and handover | **IN FINALIZATION** |

Phase 5 is no longer “not started.” It contains runtime-approved HF50 behavior, runtime-tested/frozen V7.1 diffraction behavior, a validated performance housekeeping pass, and a validated lifecycle/state source pass that is awaiting user runtime validation.

---

## Phase 1 — COMPLETE / JAR-RECHECKED

The exact Hotfix3 JAR confirms:

- 75 ZIP entries
- 65 files
- 60 Java-21 classfiles
- frozen entry fingerprints
- five source-relevant runtime resources byte-for-byte

Evidence remains under `docs/baseline/` and `docs/PHASE1_HOTFIX3_BYTECODE_AUDIT.md`.

---

## Phase 2 — COMPLETE / JAR-RECHECKED

Pinned build environment:

- Java 21
- Minecraft 1.21.1
- NeoForge 21.1.248
- ModDevGradle 2.0.144
- Gradle 9.2.1
- CC:Tweaked 1.120.2
- SPR Modrinth version `Dd2tmpsk`
- HQ Speakers Modrinth project/version `ygA78R8l/u5PEI5Ax`
- Cloth Config file `5729127`

Runtime uses untouched SPR. Compilation uses an isolated access-transformed SPR copy because the reconstructed exact source directly calls members widened by the compat access transformer.

---

## Phase 3 — COMPLETE / RECHECKED

All authored Java source is present and rebuildable.

Final Phase 3 closure remained valid after Phase 4 corrections.

---

## Phase 4 — COMPLETE / RECHECKED

Final audited code/build head:

`98e7dedb7ecf6fda22008b084b6bb41956edff78`

Final equivalence results:

- 60/60 class paths exact
- 60/60 structural ABI exact
- 69/69 `ConstantValue` entries exact
- zero bootstrap/string-concat recipe mismatches
- 11/11 configured Mixin/accessor semantic annotation sets reconciled
- 5/5 non-class resources byte-exact
- 550 methods audited
- no unresolved proven Hotfix3 behavior discrepancy

Frozen Phase 4 refs:

- `phase4-hotfix3-parity` -> `79eed29767343ee34022e8f6268b386f75e84c9f`
- `archive-phase4-hotfix3-parity` -> same

Do not move archive refs.

---

## Phase 5 — IN FINALIZATION

### A. Synchronized-speaker HF50 issue — RESOLVED / APPROVED

Accepted source commit:

`62d3a7a0a176c901402b913946d98f3cb455a8f4`

Accepted JAR SHA-256:

`fe894a42eebeea37e77f63e9acf65df22bdac72897fb6ac1eb9def198dcd032a`

User verdict: `all good, lets move on`.

HF50 is frozen. Do not retune.

### B. Elevation/opening diffraction — V7.1 RESOLVED / FROZEN

Rejected major prototype:

- V7 source `ae3c4ef55173a5be527f114e18af8de8bc43d315` bundled spreading with an unwanted leg attenuation rewrite and was rejected by the user.

Accepted V7.1 frozen source:

`ffcf5f6e05d85b69f1f1dff8cfae1b082b71604d`

Frozen refs:

- `phase5-diffraction-v7-1-runtime-approved`
- `archive-phase5-diffraction-v7-1-runtime-approved`

Both point exactly at `ffcf5f6e...`.

Accepted V7.1 JAR SHA-256:

`30d457c2a52672f893b1076938e2fdea3f41759173dfd843ff652bd490692101`

Known/accepted limitation: hard opening discovery radius 8 can produce a noticeable transition beyond the radius. The user accepted this. Do not reopen acoustic design.

### C. Performance benchmark — COMPLETED FOR FROZEN V7.1

User benchmarked 1-source and 4-source stationary/moving cases and skipped 12 speakers.

Important result:

- topology scanning is listener-shared, not multiplied by speaker count;
- lower portal leg is listener-shared;
- cross portal leg scales per source as intended;
- portal SPR ray CPU is negligible;
- moving cost is dominated by the existing progressive/direct/SPR machinery.

Representative 4-source, diffraction ON, moving windows:

- 14.4 acoustic / 6.8 SPR ms/s
- 13.7 acoustic / 6.9 SPR ms/s
- 14.6 acoustic / 7.4 SPR ms/s

See `docs/NEXT_CHAT_HANDOFF_2026-09-06.md` for the complete test table and the strict contained-window interpretation rule.

### D. Performance housekeeping pass — SOURCE/CI COMPLETE

Branch:

`phase5-v7-1-performance-pass`

Final clean head:

`962eab8b052466ca984496a7dec0767dc65803f4`

The pass intentionally avoided a multi-cell topology cache because that could change acoustic state/timing on A->B->A movement.

Only behavior-neutral changes were kept:

- prune cache housekeeping normally at most once per second;
- still prune immediately above existing soft limits;
- preserve all lookup TTL/recheck validity rules;
- avoid unnecessary candidate-list allocations;
- pre-size lists.

Successful validation run:

`33957036689`

JAR SHA-256:

`b7945374fc95935a3c951e660efb660bbcc777744395e81849b5abf680592b41`

A dedicated user runtime comparison of that isolated performance JAR was not completed before lifecycle work; the lifecycle candidate includes the same performance pass.

### E. Lifecycle/state source audit — SOURCE/CI COMPLETE; RUNTIME PENDING

Branch:

`phase5-v7-1-lifecycle-state-finish`

Clean lifecycle source checkpoint before documentation-only commits:

`be03d30efe98ca03bdf27764bcea567df5ef3875`

Compared with performance base `962eab8...`, the clean lifecycle source changes only:

- `CompatAudioManager.java`
- `SoundEngineLifecycleMixin.java`

Three proven lifecycle/state issues were fixed:

1. **ClientLevel/world identity transition:** actual `ClientLevel` identity is tracked so disconnect/rejoin/dimension/world replacement invalidates the prior session instead of carrying stale state.
2. **Teardown ordering:** stop-all and emergency sound-engine cleanup use blocking sound-thread execution so compat-owned OpenAL/EFX state is torn down before vanilla destroys the executor/device. Redundant destroy/reload hooks were removed.
3. **Failed-start stale registrations:** if source setup fails after `EnvironmentSmoother.register(sourceId)` but before installation into `ACTIVE`, the failure path now calls `EnvironmentSmoother.unregister(sourceId)` before deleting the raw AL source.

Successful lifecycle validation run:

`33962380234`

Job:

`101296383519`

Artifact ID:

`9968357487`

Artifact digest:

`sha256:3668d1fe38b64591ba6854e8937290ee4544c22bca12565682a0c7abd1394541`

Lifecycle candidate JAR SHA-256:

`6d0fa98ee6c76d23a3e0764501d16dc5c993149e0de77181cdab6fc0a9abdc18`

Class count:

81

The artifact still has internal test version:

`0.1.0-beta11-phase5-v7-1-performance-test`

This is only naming debt; the lifecycle fixes are present in the built branch source. Release cleanup has not happened yet.

`game_launch_performed=false` for CI.

**User runtime lifecycle validation has not happened yet. Do not call lifecycle complete at runtime until logs/user report confirm it.**

---

## Frozen invariants at current handoff

Preserve:

- no Lua changes
- `SoundSource.BLOCKS` distance behavior
- center + 8 inner + 8 outer direct geometry
- approved progressive full/partial refresh behavior
- private per-source EFX
- mandatory direct/aux EFX reattachment
- no private EFX before PLAYING/PAUSED
- do not cancel/replace SPR `calculateOcclusion()`
- no worker-thread SPR world/geometry raycasts
- strict source lifetime/generation semantics
- no intentional PCM/OpenAL clock/buffer-offset/sync timing changes
- Hotfix3 partial sync grace / pending INITIAL protection
- HF50 approved behavior
- V7.1 approved diffraction behavior
- Beta10 exact direct reuse / stable OpenAL write suppression
- Beta11 same-clone room-ray cache; cross-clone telemetry only
- exact eligibility expression using `&`:

  `Beta9Optimizer.isAudibleAndRecord(state.sourceId) & beta9EligibleReal(state, now)`

---

## Exact next action

Implementation is intentionally paused for chat handoff.

When the user starts the next session, first read:

`docs/NEXT_CHAT_HANDOFF_2026-09-06.md`

Then, when the user is ready, runtime-test lifecycle candidate SHA:

`6d0fa98ee6c76d23a3e0764501d16dc5c993149e0de77181cdab6fc0a9abdc18`

Test sequence:

1. play -> stop -> play
2. pause -> resume
3. stopAll -> restart
4. sound/resource reload -> restart
5. disconnect -> rejoin -> restart
6. dimension change -> restart
7. four speakers playing for several minutes while moving
8. upload `latest.log` + `debug.log`

If clean, proceed directly to release cleanup/final audit. Do not add acoustic features or retune V7.1/HF50.

Do not merge to `main` unless explicitly requested.