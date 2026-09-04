# Beta11 Hotfix3 Reconstruction Handoff

This document is the durable context for every scheduled reconstruction run. Read this file and `RECONSTRUCTION_STATUS.md` before changing code.

## Goal

Reconstruct a complete, rebuildable, source-level project for the tested **Beta11 Hotfix3** compatibility mod. The reconstructed source should preserve the tested runtime behavior closely enough that future development can move away from direct JAR/bytecode patching.

Do **not** begin Beta11.1 performance cleanup (roadmap B) during reconstruction, audit, build-system, or compile passes.

## Authoritative baseline

Artifact: `cchq_soundphysics_compat-0.1.0-beta11-hotfix3.jar`

SHA-256:

`83500f182fc9829aa1a5a51fbfa11ba6cdfb645699b25d1c445167666dabc1ef`

The Hotfix3 JAR is the behavioral authority until reconstruction, audit, build, and runtime checks are complete. If decompiled source and assumptions disagree with the JAR, trust the JAR. Mark uncertainty rather than guessing.

## Target environment

- Minecraft 1.21.1
- NeoForge 21.1.248
- Java 21
- CC:Tweaked 1.120.2
- CC:HQ Speakers 1.1.4-neoforge-1.21.1
- Sound Physics Remastered 1.21.1-1.5.1
- Client-only compat mod

## Current branch

Work only on:

`beta11-source-reconstruction`

Do not merge to `main` just because the project compiles.

## Source already known/recovered

Original Beta11 build inputs already present in the branch include:

- `AudioDecoder.java`
- `Beta11RoomRayCache.java`
- `SoundPhysicsRoomRayMemoMixin.java`

Additional source/resources have already been reconstructed or recovered on the branch. Always inspect the actual branch state first; do not rely on this document as a complete file inventory.

## Frozen runtime/acoustic invariants

Preserve all of these during reconstruction:

1. Intercept CC:HQ high-level whole-file playback; no Lua API changes.
2. Decode remains off-thread; stereo/multichannel audio is downmixed to mono PCM for positional OpenAL playback.
3. Shared OpenAL buffer/refcount behavior is preserved.
4. Synchronized groups use `alSourcePlayv`.
5. Hotfix3 incomplete-group behavior is preserved: a 100 ms grace period allows a partial declared sync group to start together instead of leaving arrived sources stuck in `AL_INITIAL`.
6. During that grace period, pending `AL_INITIAL` sources must be protected from lifecycle cleanup by the Hotfix3 source-state helper behavior.
7. Approved distance behavior using `SoundSource.BLOCKS` is preserved.
8. Progressive direct occlusion remains the approved 17 conceptual paths: center + 8 inner + 8 outer, with alternating 9-path partial refreshes and exact weighting semantics.
9. Private per-source EFX is required to avoid SPR global mutable filter cross-source contamination.
10. **Critical invariant:** every actual environment application must reattach direct/aux EFX. Parameter-write suppression may remain, but EFX attachment itself must never be optimized away from an environment apply.
11. No private EFX before the source is PLAYING/PAUSED eligible.
12. `PositionStabilizer` semantics are preserved.
13. Do not inject/cancel/replace SPR `calculateOcclusion()`.
14. No worker-thread SPR world/geometry raycasts.
15. Strict source lifetime identity remains sourceId + monotonic generation + speaker UUID.
16. Physics scheduling must never alter PCM sample position, OpenAL playback clock, buffer offset, or sync-group timing.
17. SoundEngine pause/resume/stop/destroy/reload lifecycle handling from Beta11 remains intact.
18. Beta10 exact direct-ray cache and OpenAL bit-identical parameter-write suppression behavior must remain intact.
19. Beta11 room cache only memoizes the two source-centered environment/bounce `RaycastUtils.rayCast` callsites inside SPR `evaluateEnvironment`; listener/shared-airspace geometry remains live.
20. Cross-clone room reuse is telemetry-only in Beta11 Hotfix3; actual cross-clone reuse must not be introduced during reconstruction.

## Hotfix3 sync bug history

A prior Beta11-fixed build produced no sound because HQ metadata declared a sync group of 12 while only 11 OpenAL sources arrived. The group never reached 12/12, sources remained `AL_INITIAL`, and lifecycle cleanup destroyed them.

Hotfix3 corrected this by:

- keeping full groups on immediate `alSourcePlayv(int[])`;
- defining `PARTIAL_FLUSH_NS = 100_000_000L`;
- starting all arrived sources together after that grace period;
- treating pending initial sources as effectively paused for maintenance/lifecycle state checks until start;
- preserving sound-thread ownership of OpenAL calls.

Do not regress this logic.

## Beta11 verifier history

Original Beta11 had a `VerifyError` in `Beta10Optimizer.beta11RoomCacheActive()` because manually injected branch bytecode lacked the required StackMapTable frame. The working Hotfix3 contains the verifier-safe fix. Reconstructed normal Java should express the final working semantics; source does not need to reproduce patched StackMapTable bytes literally.

## Performance/reference observations

These are not targets to optimize during reconstruction, but useful sanity checks:

- Hotfix3 was acoustically healthy and kept 11-speaker and 7-speaker groups active instead of collapsing to zero.
- EFX invariant held in logs: `efxApplies == efxReattachPasses` throughout active windows.
- Camera-only rotation did not spuriously wake movement geometry scheduling.
- Beta10 direct geometry is already extremely cheap; do not redesign it during reconstruction.
- Beta11 same-clone room cache worked during movement; stationary misses were dominated by SPR safe-clone replacement roughly once per second.

## Reconstruction discipline

Every scheduled run must:

1. Read this file and `RECONSTRUCTION_STATUS.md`.
2. Inspect the current branch before doing work.
3. Work on one bounded phase only.
4. Compare reconstructed code against available Hotfix3 evidence rather than guessing.
5. Preserve invariants above.
6. Commit completed work to `beta11-source-reconstruction`.
7. Update `RECONSTRUCTION_STATUS.md` with exactly what is done, what remains, and any uncertainty.
8. Stop after the bounded phase; do not opportunistically start roadmap B.

If a prerequisite from an earlier phase is still incomplete, continue/fix that prerequisite first and do not skip ahead.

## Planned bounded phases

### Phase 1 — Core playback/lifecycle

Primary focus:

- `CompatAudioManager`
- `SyncStartCoordinator` and nested/group logic
- Hotfix3 100 ms partial-group grace semantics
- source-state protection for pending initial sources
- source creation/start/stop/refcount/lifecycle paths
- room-cache teardown hooks where owned by the manager

### Phase 2 — SPR/acoustic core

Primary focus:

- `SoundPhysicsBridge`
- `Beta10Optimizer`, including verifier-safe `beta11RoomCacheActive()` semantics
- `AcousticCapture`
- `EnvironmentSmoother`
- private EFX application/reattachment behavior
- direct-cache/write-suppression integration

### Phase 3 — Remaining helpers/mixins/config/resources

Primary focus:

- `ProgressiveOcclusionModel`
- `PositionStabilizer`
- `Beta9Optimizer`
- `PerformanceStats`
- `AttenuationBridge`
- remaining scheduler/config/helper classes
- remaining mixins/accessors
- resources and metadata not yet recovered exactly
- verify every non-synthetic Hotfix3 class has reconstructed source or a documented reason it is compiler-generated

### Phase 4 — Baseline audit + build-system readiness

No feature work. Audit:

- class and method inventory
- method descriptors and important annotations
- constants and thresholds
- mixin targets/descriptors/ordinals/require values
- OpenAL calls and ownership
- sync-group behavior
- EFX attachment invariant
- distance/direct/room semantics
- cache sizes/probe counts/ages
- resources (`neoforge.mods.toml`, mixin JSON, access transformer, language/resources)

Then finish Gradle/dependency/mapping/toolchain configuration needed for a real Java 21 / NeoForge 1.21.1 build. Fix only reconstruction/build discrepancies justified by the baseline.

### Phase 5 — Compile and inspect

Only after prerequisites are sufficiently complete:

- run/trigger the Gradle build;
- inspect compiler errors/warnings;
- fix reconstruction/build mistakes only;
- inspect produced JAR class/resource inventory;
- compare key behavior-bearing methods/constants/invariants against Hotfix3;
- run any available lightweight decode/cache/sync harnesses;
- report clearly whether the source can now be considered a usable reconstructed Beta11 baseline.

Do not start Beta11.1 B optimizations in this phase.

## Later roadmap (not part of these tasks)

After reconstructed Beta11 is verified:

- **B / Beta11.1:** exact decode/cache/OpenAL/allocation/diagnostic cleanup.
- **C / Beta12:** persistent progressive temporal room geometry.
- **C2:** acoustic work scheduler.
- **D / Beta13:** sparse adaptive room-position memory.
- Adaptive ray/bounce quality reduction remains shelved.
- HQ-specific enhanced/music spatial mode is a later optional feature.
