# Beta11 Hotfix3 reconstruction and Phase 5 handoff

> **Current continuation document:** `docs/NEXT_CHAT_HANDOFF_2026-09-06.md`
>
> Read that file first in a new session. This document is the durable project summary; the phase-specific audit files remain the detailed historical evidence.

## Project goal

Maintain a readable, rebuildable source-level compatibility layer between CC:HQ Speakers and Sound Physics Remastered on Minecraft 1.21.1 / NeoForge, while preserving the sound behavior that has already been accepted and finishing the remaining performance, lifecycle, validation and release work.

The Beta11 Hotfix3 reconstruction itself is complete. Current work is Phase 5 finalization rather than reconstruction.

The user currently prefers the assistant to make ordinary implementation decisions directly, using a practical middle ground between overconservative and aggressive choices.

## Original authoritative artifact

`cchq_soundphysics_compat-0.1.0-beta11-hotfix3.jar`

SHA-256:

`83500f182fc9829aa1a5a51fbfa11ba6cdfb645699b25d1c445167666dabc1ef`

Target environment:

- Minecraft 1.21.1
- NeoForge 21.1.248
- Java 21
- ModDevGradle 2.0.144
- Gradle 9.2.1
- CC:Tweaked 1.120.2
- CC:HQ Speakers `ygA78R8l/u5PEI5Ax`, runtime `1.1.4-neoforge-1.21.1`
- Sound Physics Remastered 1.21.1-1.5.1 / `Dd2tmpsk`
- client-side compatibility mod

Runtime SPR remains the untouched tested artifact. The build uses an isolated access-transformed SPR compile copy because the exact reconstructed source accesses members widened by the compat access transformer.

## Reconstruction status

- Phase 1: **COMPLETE / JAR-RECHECKED**
- Phase 2: **COMPLETE / JAR-RECHECKED**
- Phase 3: **COMPLETE / RECHECKED**
- Phase 4: **COMPLETE / RECHECKED**
- Phase 5: **IN FINALIZATION**

Phase 4 final audited code/build:

`98e7dedb7ecf6fda22008b084b6bb41956edff78`

Phase 4 established:

- 60/60 class paths exact
- 60/60 structural ABI exact
- 69/69 constant values exact
- zero bootstrap/string-concat recipe mismatches
- 11/11 configured Mixin/accessor annotation sets reconciled
- 5/5 runtime resources byte-exact
- 550 methods audited
- no unresolved proven Hotfix3 behavior discrepancy at closure

## Stable acoustic reference

The current stable sound reference is **V7.1**.

Commit:

`ffcf5f6e05d85b69f1f1dff8cfae1b082b71604d`

Frozen refs:

- `phase5-diffraction-v7-1-runtime-approved`
- `archive-phase5-diffraction-v7-1-runtime-approved`

Accepted V7.1 JAR SHA-256:

`30d457c2a52672f893b1076938e2fdea3f41759173dfd843ff652bd490692101`

V7.1 is the comparison point for later finishing changes. It keeps the V6 leg/spectral model and adds explicit aperture spreading, which produced the accepted runtime result.

The approved synchronized-speaker HF50 behavior is part of that stable result.

HF50 source:

`62d3a7a0a176c901402b913946d98f3cb455a8f4`

HF50 JAR SHA-256:

`fe894a42eebeea37e77f63e9acf65df22bdac72897fb6ac1eb9def198dcd032a`

User verdict on HF50: `all good, lets move on`.

Known V7.1 characteristics include the radius-8 opening search boundary and a slightly darker accepted 3-deep-hole result. These are documented properties of the stable reference and can be used as comparison points if a later correctness fix is evaluated.

## Core architecture carried forward

The stable/finishing branches retain:

- `SoundSource.BLOCKS` distance behavior;
- progressive direct geometry using center + 8 inner + 8 outer paths;
- approved full/partial direct refresh behavior;
- private per-source EFX;
- direct/aux EFX reattachment on actual environment application;
- private EFX setup after PLAYING/PAUSED eligibility;
- SPR `calculateOcclusion()` as part of the normal acoustic pipeline;
- sound-thread ownership of SPR world/geometry raycasts;
- strict source generation/lifetime semantics;
- Hotfix3 partial synchronized-start grace and pending-INITIAL protection;
- Beta10 exact direct reuse and stable OpenAL write suppression;
- Beta11 exact same-clone room-ray memoization, with cross-clone reuse kept as telemetry;
- the deliberate eligibility expression:

  `Beta9Optimizer.isAudibleAndRecord(state.sourceId) & beta9EligibleReal(state, now)`

The assistant does not perform subjective listening. Runtime sound judgments come from the user; CI, source and logs provide technical evidence.

## Diffraction development summary

The elevation/opening issue came from straight source-to-listener rays becoming overly blocked even when a plausible route existed around an opening or edge.

Relevant checkpoints:

- V3: `b1b97fc7050163e70fbb039f7b76e51eef3c50d0` — useful proof of concept but too special-case.
- V4: `053af2cca0dd84e98956fec6a860cb46e860630e` — listener-centered topology scan + max two verified portal candidates; good performance, imperfect side verification.
- V5: `595b4736d3f861e5d67c8b6a64f2cf9297a7f4fe` — split lower/upper waypoints; clearer near opening but wrong replacement-model shape.
- V6: `f72c106cd94108a418cb6d64cdeb4eb09a68de58` — secondary two-band aperture energy on top of the normal direct result; exposed missing spreading.
- V7: `ae3c4ef55173a5be527f114e18af8de8bc43d315` — spreading plus a leg attenuation rewrite; user preferred the earlier behavior.
- V7.1: `ffcf5f6e05d85b69f1f1dff8cfae1b082b71604d` — V6 leg/spectral behavior plus spreading only; accepted and frozen.

V7.1 spreading:

```java
spread = apertureSpreadScale / (apertureSpreadScale + apertureDistance);
coupling = portalCoupling * activation * horizonFade * spread;
lowAmp = coupling * pathGain * lowDiff;
highAmp = coupling * pathGain * pathCutoff * highDiff;
```

Default `aperture_spread_scale = 3`.

## Performance result and housekeeping pass

Frozen V7.1 was benchmarked with one and four sources. The planned 12-speaker section was skipped.

Representative four-source moving result:

- acoustic 13.7–14.6 ms/s
- SPR 6.8–7.4 ms/s

The benchmark showed:

- listener topology work is shared rather than multiplied by source count in the tested stationary case;
- listener lower-leg work is shared;
- per-source cross-leg work scales as designed;
- portal SPR-ray CPU is negligible;
- movement cost comes mainly from the existing progressive/direct/SPR refresh work.

Performance branch:

`phase5-v7-1-performance-pass`

Clean head:

`962eab8b052466ca984496a7dec0767dc65803f4`

Kept performance changes:

- cache pruning normally at most once per second;
- immediate pruning when existing soft limits are exceeded;
- unchanged cache validity/TTL/recheck semantics;
- fewer unnecessary candidate-list allocations;
- pre-sized candidate lists.

A multi-cell topology cache was considered and left out because its A -> B -> A reuse behavior could subtly change acoustic state timing.

Successful CI run:

`33957036689`

Performance JAR SHA:

`b7945374fc95935a3c951e660efb660bbcc777744395e81849b5abf680592b41`

## Lifecycle/state finishing pass

Current branch:

`phase5-v7-1-lifecycle-state-finish`

Clean lifecycle source checkpoint before documentation-only commits:

`be03d30efe98ca03bdf27764bcea567df5ef3875`

Compared with the performance base, the clean lifecycle source changes only:

- `CompatAudioManager.java`
- `SoundEngineLifecycleMixin.java`

Three concrete issues were fixed:

### ClientLevel identity

The manager now tracks the actual `ClientLevel` instance. Disconnect/rejoin and dimension/world replacement therefore invalidate the old compat session instead of treating a non-null level as continuous identity.

### Teardown ordering

Normal stop-all cleanup and emergency shutdown now use blocking sound-thread execution so compat-owned source/EFX teardown completes before vanilla destroys the sound executor/OpenAL state.

The lifecycle mixin now uses four hooks:

- pause HEAD
- resume HEAD
- stopAll HEAD
- emergencyShutdown HEAD

The redundant destroy/reload hooks were removed so normal teardown has one authoritative path.

### Failed-start registration cleanup

If source setup fails after `EnvironmentSmoother.register(sourceId)` but before insertion into `ACTIVE`, the failure path now unregisters the source before deleting the raw OpenAL source. This clears the associated progressive, position, bridge, sync and EFX state for that partial lifetime.

Lifecycle source commits:

- `24e41b49f8ccd5b32e484b90def8f50c3767c8d9`
- `cfcc122da3be03171377f14717ae30b4c6bbb696`

Successful lifecycle validation:

- run `33962380234`
- job `101296383519`
- artifact `9968357487`
- artifact digest `sha256:3668d1fe38b64591ba6854e8937290ee4544c22bca12565682a0c7abd1394541`
- JAR SHA `6d0fa98ee6c76d23a3e0764501d16dc5c993149e0de77181cdab6fc0a9abdc18`
- 81 classes
- Java 21 compile/build/inspection passed
- frozen acoustic file comparison passed
- Lua diff check passed
- `game_launch_performed=false`

The candidate still uses internal version:

`0.1.0-beta11-phase5-v7-1-performance-test`

That is release naming debt rather than a source-state mismatch.

## Current next checkpoint

Implementation stopped here for the chat handoff.

The lifecycle candidate still needs the user's runtime lifecycle/stability run.

Candidate SHA:

`6d0fa98ee6c76d23a3e0764501d16dc5c993149e0de77181cdab6fc0a9abdc18`

Suggested runtime sequence:

1. play -> stop -> play
2. pause -> resume
3. stopAll -> restart
4. sound/resource reload -> restart
5. disconnect -> rejoin -> restart
6. dimension change -> restart
7. four-speaker sustained moving playback
8. upload `latest.log` and `debug.log`

A clean runtime result leads naturally into release naming/cleanup, final source audit, final JAR/resource/hash inspection and a final stable/archive checkpoint.

`main` remains separate from the Phase 5 working branches; merging can remain an explicit release decision.

For exact benchmark numbers, branch/commit history, lifecycle details and next-chat procedure, use `docs/NEXT_CHAT_HANDOFF_2026-09-06.md` as the authoritative continuation file.
