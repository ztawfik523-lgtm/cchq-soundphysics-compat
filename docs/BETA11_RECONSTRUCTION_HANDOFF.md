# Beta11 Hotfix3 reconstruction and Phase 5 handoff

> **Authoritative continuation document:** `docs/NEXT_CHAT_HANDOFF_2026-09-06.md`
>
> Read that file first in a new session. This file is now a durable project summary and pointer; older Phase 1–4 documents are historical evidence, not the current task state.

## Goal

Maintain a complete, readable, rebuildable source-level compatibility mod for CC:HQ Speakers + Sound Physics Remastered on Minecraft 1.21.1 / NeoForge, while preserving the approved sound behavior and finishing performance, correctness, lifecycle and release work.

The reconstruction itself is complete. The project is **not** currently reconstructing Beta11 Hotfix3.

The user has frozen feature/acoustic development and explicitly wants only:

- performance work that does not change sound behavior;
- correctness/bug fixes;
- lifecycle/stability work;
- validation;
- release cleanup and finishing.

Do not start Beta12/Beta13 concepts, new acoustic models, new diffraction radius logic, or HF retuning.

## Original authoritative artifact

`cchq_soundphysics_compat-0.1.0-beta11-hotfix3.jar`

SHA-256:

`83500f182fc9829aa1a5a51fbfa11ba6cdfb645699b25d1c445167666dabc1ef`

The exact Hotfix3 binary was the authority used to reconstruct and audit the original source behavior.

## Target environment

- Minecraft 1.21.1
- NeoForge 21.1.248
- Java 21
- ModDevGradle 2.0.144
- Gradle 9.2.1
- CC:Tweaked 1.120.2
- CC:HQ Speakers Modrinth project/version `ygA78R8l/u5PEI5Ax`
- runtime HQ internal version `1.1.4-neoforge-1.21.1`
- Sound Physics Remastered 1.21.1-1.5.1 / version `Dd2tmpsk`
- client-only compat mod

Runtime SPR remains untouched. The build uses an isolated AT-transformed SPR compile copy because exact compat source calls members widened by the access transformer.

## Reconstruction status

- Phase 1: **COMPLETE / JAR-RECHECKED**
- Phase 2: **COMPLETE / JAR-RECHECKED**
- Phase 3: **COMPLETE / RECHECKED**
- Phase 4: **COMPLETE / RECHECKED**
- Phase 5: **IN FINALIZATION**

Phase 4 final audited code/build head:

`98e7dedb7ecf6fda22008b084b6bb41956edff78`

Phase 4 established:

- 60/60 class paths exact
- 60/60 structural ABI exact
- 69/69 constant values exact
- zero bootstrap/string-concat recipe mismatches
- 11/11 Mixin/accessor annotation sets reconciled
- 5/5 runtime resources byte-exact
- 550 methods audited
- no unresolved proven behavior discrepancy

## Branch discipline

Current working branch:

`phase5-v7-1-lifecycle-state-finish`

Do not merge to `main` unless the user explicitly asks.

Existing immutable/archive refs must not be casually moved.

Most important frozen acoustic ref:

`archive-phase5-diffraction-v7-1-runtime-approved` -> `ffcf5f6e05d85b69f1f1dff8cfae1b082b71604d`

## User decision style

Until the user explicitly changes the instruction, make reasonable implementation decisions without repeatedly asking them to choose.

Use the middle ground:

- bounded and maintainable;
- not overconservative;
- not aggressive/risky;
- protect frozen behavior.

## Frozen runtime/acoustic invariants

Preserve all of the following:

1. No Lua changes.
2. Approved `SoundSource.BLOCKS` distance behavior.
3. Direct occlusion remains center + 8 inner + 8 outer with approved progressive refresh semantics.
4. Private per-source EFX remains required.
5. Every actual environment application reattaches required direct/aux EFX.
6. No private EFX before PLAYING/PAUSED eligibility.
7. Preserve `PositionStabilizer` behavior unless fixing a proven lifecycle bug without changing normal acoustics.
8. Do not cancel or replace SPR `calculateOcclusion()`.
9. No worker-thread SPR world/geometry raycasts.
10. Preserve strict source lifetime identity/generation semantics.
11. Do not intentionally alter PCM sample position, OpenAL playback clock, buffer offset or synchronized-start timing.
12. Preserve pending `AL_INITIAL` sync protection and partial group grace.
13. Preserve Beta10 exact direct reuse and stable/bit-identical OpenAL write suppression.
14. Preserve Beta11 same-clone room-ray cache semantics; cross-clone reuse remains telemetry-only.
15. Preserve exact eligibility with `&`:

    `Beta9Optimizer.isAudibleAndRecord(state.sourceId) & beta9EligibleReal(state, now)`

16. Do not claim assistant-side listening/game testing. Runtime listening belongs to the user.

## Phase 5 synchronized-speaker fix — approved

Accepted HF50 source:

`62d3a7a0a176c901402b913946d98f3cb455a8f4`

Accepted JAR SHA:

`fe894a42eebeea37e77f63e9acf65df22bdac72897fb6ac1eb9def198dcd032a`

User verdict: `all good, lets move on`.

HF50 is frozen.

## Phase 5 diffraction — V7.1 approved/frozen

Rejected V7 source:

`ae3c4ef55173a5be527f114e18af8de8bc43d315`

Reason: it bundled aperture spreading with an unwanted leg attenuation rewrite and sounded worse than the previous iteration.

Accepted/frozen V7.1 source:

`ffcf5f6e05d85b69f1f1dff8cfae1b082b71604d`

Accepted V7.1 JAR SHA:

`30d457c2a52672f893b1076938e2fdea3f41759173dfd843ff652bd490692101`

V7.1 keeps the V6 leg/spectral formula and adds only explicit aperture spreading.

Known limitation accepted by the user: opening discovery has a hard radius 8 ceiling and the 8->10-ish transition can feel a little sudden. Do not “fix” this during release finishing.

## Performance conclusion

The V7.1 benchmark with 1 and 4 sources showed:

- topology scan work is listener-shared;
- lower portal leg is listener-shared;
- per-source cross leg scales as intended;
- portal ray timing is negligible;
- movement cost mainly comes from the existing progressive/direct/SPR machinery.

Representative 4-source moving V7.1 baseline:

- acoustic: 13.7–14.6 ms/s
- SPR: 6.8–7.4 ms/s

The user skipped the 12-speaker test. It is not required to prove portal scaling is reasonable.

## Performance housekeeping pass

Branch:

`phase5-v7-1-performance-pass`

Final clean head:

`962eab8b052466ca984496a7dec0767dc65803f4`

Kept changes are intentionally behavior-neutral:

- cache pruning normally at most once per second;
- immediate pruning remains above existing soft limits;
- lookup TTL/recheck semantics unchanged;
- unnecessary candidate-list allocation removed/pre-sized.

A multi-cell topology cache was explicitly rejected because it could alter A->B->A acoustic state/timing.

Successful CI run:

`33957036689`

JAR SHA:

`b7945374fc95935a3c951e660efb660bbcc777744395e81849b5abf680592b41`

## Lifecycle/state finish pass

Current branch:

`phase5-v7-1-lifecycle-state-finish`

Clean lifecycle source checkpoint before documentation-only handoff commits:

`be03d30efe98ca03bdf27764bcea567df5ef3875`

The lifecycle pass fixed three concrete issues without changing acoustic math:

### 1. World/dimension identity

`CompatAudioManager` now tracks the actual `ClientLevel` object. A transition to null or a different level invalidates the old session and tears down old sources instead of carrying stale session state into disconnect/rejoin or dimension/world replacement.

### 2. Blocking sound-engine teardown

Normal stop-all and emergency shutdown now invalidate the compat session and complete compat source cleanup using `SoundEngineExecutor.executeBlocking(...)` before vanilla destroys the sound executor/OpenAL state.

The lifecycle Mixin now has only four hooks:

- pause HEAD
- resume HEAD
- stopAll HEAD
- emergencyShutdown HEAD

Redundant destroy/reload hooks were removed so normal lifecycle teardown has one authoritative path.

### 3. Failed-start registration cleanup

If a new AL source is registered with `EnvironmentSmoother` and then setup fails before insertion into `ACTIVE`, the failure path now calls `EnvironmentSmoother.unregister(sourceId)` before deleting the AL source. This prevents stale per-source compat registration after a partial start failure.

Successful lifecycle validation:

- run `33962380234`
- job `101296383519`
- artifact `9968357487`
- artifact digest `sha256:3668d1fe38b64591ba6854e8937290ee4544c22bca12565682a0c7abd1394541`
- JAR SHA `6d0fa98ee6c76d23a3e0764501d16dc5c993149e0de77181cdab6fc0a9abdc18`
- 81 classes
- Java 21 compile/build/inspection passed
- frozen acoustic diff assertions passed
- no Lua changes
- `game_launch_performed=false`

The internal mod version is still:

`0.1.0-beta11-phase5-v7-1-performance-test`

That is naming debt only. Release cleanup has not yet renamed it.

## Current exact next action

**Implementation is paused for handoff.**

The lifecycle candidate has not yet received user runtime validation after these lifecycle fixes.

When the next chat resumes runtime work, use the candidate whose SHA is:

`6d0fa98ee6c76d23a3e0764501d16dc5c993149e0de77181cdab6fc0a9abdc18`

Test:

1. play -> stop -> play
2. pause -> resume
3. stopAll -> restart
4. sound/resource reload -> restart
5. disconnect -> rejoin -> restart
6. dimension change -> restart
7. four-speaker sustained moving playback
8. upload both `latest.log` and `debug.log`

Analyze the full sequence and verify exact startup identity before concluding anything.

If runtime passes, go directly to:

1. release naming/config/log cleanup;
2. final frozen-invariant audit;
3. final Java 21 build/JAR hash/class/resource inspection;
4. new final immutable checkpoint;
5. merge only if the user explicitly requests it.

Do not reopen HF50 or V7.1 acoustics.

For complete benchmark numbers, rejected prototypes, exact refs and the precise next-chat procedure, read `docs/NEXT_CHAT_HANDOFF_2026-09-06.md`.