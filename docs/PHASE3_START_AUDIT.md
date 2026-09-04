# Phase 3 checkpoint — Beta11 Hotfix3 source reconstruction

## Scope

This document began as the Phase 3 start audit and is now the durable current Phase 3 checkpoint. It records the exact state after the authoritative Hotfix3 JAR became available and after Phases 1 and 2 were rechecked against it.

Authoritative baseline:

- JAR: `cchq_soundphysics_compat-0.1.0-beta11-hotfix3.jar`
- SHA-256: `83500f182fc9829aa1a5a51fbfa11ba6cdfb645699b25d1c445167666dabc1ef`
- branch: `beta11-source-reconstruction`

No Beta11.1/B optimization belongs in this phase.

## Earlier Phase 3 start batch

The first bounded Phase 3 batch reconstructed the remaining CC:HQ/sound-engine integration mixins:

- `HQSpeakerClientHandlerMixin.java` — commit `733309b1cac07f0eff5e2167d3b206382321571f`;
- `HQSpeakerStopPacketMixin.java` — commit `523ceb3303a12737ed993262557c2621409c6b79`;
- `SoundEngineLifecycleMixin.java` — commit `1a4129b06d9e30dfd27c827e9b02eadbb436c2a5`.

Hotfix3 runtime metadata proves the final lifecycle mixin has six callbacks:

- `pause`
- `resume`
- `stopAll`
- `destroy`
- `emergencyShutdown`
- `reload`

The HQ stop callback descriptor was also runtime-evidenced, and upstream version-matched HQ source confirms the static receive/stop entry shapes used by the mixins.

## Exact JAR availability changed the reconstruction boundary

The earlier version of this document said the authoritative Hotfix3 JAR was not available to the active reconstruction work. That is no longer true.

The supplied JAR was verified byte-for-byte against the frozen SHA-256 and is now the direct authority for classfile inspection/decompilation. Therefore:

1. Hotfix3 class bytecode/decompile is the first source of truth;
2. runtime Mixin metadata/descriptors are secondary exact evidence;
3. already-audited source/local call sites are supporting evidence;
4. version-matched upstream dependency source is used for external signatures;
5. historical handoffs are architectural context only.

The repository's `reference/beta11-hotfix3.jar.b64.part*` staging is still incomplete. That means the GitHub CFR workflow cannot yet independently reassemble the full authority in CI. This is only a repository-staging limitation; it is no longer a reason to guess current reconstruction code.

## Phase 1 and Phase 2 rechecks before continuing Phase 3

Phase 1 was revalidated from the exact JAR and remains complete. See `docs/baseline/PHASE1_JAR_RECHECK_2026-09-04.md`.

Phase 2 was also revalidated. The exact reconstructed source exposed a compile-time SPR access-transformer requirement. The build now preprocesses an isolated exact SPR copy with the Hotfix3 AT for javac while keeping the untouched tested SPR artifact at runtime.

Final Phase 2 recheck:

- classpath run `33864425672` — success;
- finish-gate run `33864425687` — success;
- commit `cef50d04fbb03b4f523961aeb95f2f0377856994`;
- compileClasspath: 90 files;
- transformed compile artifact: `sound-physics-remastered-at.jar`;
- raw SPR explicitly absent from compileClasspath;
- NeoForge artifacts/resources/mixins/AT wiring all pass;
- javac private-access errors are gone.

The current 17 compile errors all come from the still-missing `SoundPhysicsBridge` class. This is now a clean Phase 3 boundary.

## Reconstructed top-level source now present

### Audio/runtime

- `AcousticCapture`
- `AttenuationBridge`
- `AudioDecoder`
- `Beta9Optimizer`
- `Beta10Optimizer`
- `Beta11RoomRayCache`
- `CompatAudioManager`
- `DecodedAudio`
- `DistanceBridge`
- `EnvironmentSmoother`
- `HQPayloadView`
- `PerformanceStats`
- `PositionStabilizer`
- `ProgressiveOcclusionModel`
- `RoomSchedulerClient`
- `SyncStartCoordinator`

### Config

- `ClientConfig`
- `ClientConfigAccess`
- `ConfigScreenFactory`

### Root/integration/mixins

- `CCHQSoundPhysicsCompat`
- `HQSpeakerClientHandlerMixin`
- `HQSpeakerStopPacketMixin`
- `MinecraftMixin`
- `MinecraftRoomSchedulerMixin`
- `SoundEngineAccessor`
- `SoundEngineLifecycleMixin`
- `SoundManagerAccessor`
- `SoundPhysicsEnvironmentMixin`
- `SoundPhysicsOcclusionMemoMixin`
- `SoundPhysicsPositionMixin`
- `SoundPhysicsRoomRayMemoMixin`

`Beta10Optimizer` reconstruction reached commit `7cd38e414e99ffcf5d51fac46a2290f94ab967f4`.

## Current top-level authored gaps

Only two known top-level Java source gaps remain:

### 1. `SoundPhysicsBridge`

This is the principal runtime gap and currently blocks compilation.

Required nested topology from the Hotfix3 inventory:

- `SoundPhysicsBridge$Candidate`
- `SoundPhysicsBridge$RoomEnvironmentAccess`
- `SoundPhysicsBridge$RoomEnvironmentAccess$ConfigStamp`
- `SoundPhysicsBridge$RoomStamp`
- `SoundPhysicsBridge$SourceState`

This class must be reconstructed from exact Hotfix3 bytecode/decompile. Do not create a stub just to satisfy javac.

Behavior to recover intentionally includes:

- source registration/unregistration and source-state ownership;
- `available()` integration gate;
- `apply(...)` direct/room scheduling integration;
- `schedulerTick()`;
- Beta9 capture-stamp generation/comparison/logging seams;
- room stamps/config fingerprints;
- stationary exact reuse semantics;
- clearing/sentinel transition behavior;
- candidate urgency/fairness selection;
- room environment capture/application flow;
- scheduler timing/age thresholds and all Hotfix3 constants;
- interaction with Beta9/Beta10/ProgressiveOcclusionModel/EnvironmentSmoother/Beta11RoomRayCache without altering playback timing.

### 2. `ClothConfigScreen`

Optional config UI source remains absent. It does not currently block the runtime compile boundary because nothing requires the class at javac resolution time, but it must be reconstructed before Phase 3 can close because it is present in the Hotfix3 class inventory.

## Important exact behavior recovered during Phase 3

### `SoundPhysicsOcclusionMemoMixin`

The exact Hotfix3 hook redirects SPR's internal `runOcclusion(...)` invocation inside `calculateOcclusion(...)`. It does **not** cancel or replace `calculateOcclusion()` itself. This preserves the frozen prohibition against the earlier alpha13 replacement strategy.

### `ProgressiveOcclusionModel`

Recovered Hotfix3 shape includes the approved progressive 17-path geometry and alternating partial refresh behavior. Preserve exact weights/ring scales/refresh invalidation thresholds from the reconstructed source; do not tune during reconstruction.

### `PositionStabilizer`

Recovered source preserves Hotfix3 reflection redirection/smoothing behavior. Do not simplify or replace it during reconstruction.

### `Beta9Optimizer`

Recovered exact direct-reuse logic preserves source-position/input identity checks and source/environment stamp validation around real evaluations.

### `Beta10Optimizer`

Recovered source preserves the Hotfix3 direct-ray/cache/controller and bit-identical OpenAL write suppression behavior. The verifier-safe semantics of `beta11RoomCacheActive()` remain:

```java
Context context = CONTEXT.get();
if (context != null && context.owner == OWNER_SPR) return context.cacheable;
return false;
```

### Config

`ClientConfig` and `ClientConfigAccess` have been reconstructed, including the Hotfix3 configuration defaults/ranges used by the direct/room scheduler and position/reflection behavior.

## Current compile boundary

Latest JAR-backed Phase 2 finish-gate compile probe reports 17 errors. Every error is a reference to `SoundPhysicsBridge`; examples originate in:

- `Beta9Optimizer`
- `Beta10Optimizer`
- `RoomSchedulerClient`
- `EnvironmentSmoother`
- `Beta11RoomRayCache`
- `CompatAudioManager`

The earlier `SoundPhysics.runOcclusion(...) has private access` errors are gone after the Phase 2 AT compile preprocessing fix.

## Phase 3 exit criterion

Phase 3 is complete only when all of the following hold:

1. `SoundPhysicsBridge` is reconstructed from exact Hotfix3 evidence;
2. `ClothConfigScreen` is reconstructed;
3. every meaningful class/nested class in the Phase 1 inventory has an intentional source origin or documented compiler-generated origin;
4. descriptors/annotations/mixin targets/constants are intentional;
5. manually patched Hotfix3 semantics are represented as verifier-safe Java;
6. the full Java project compiles.

A first green compile is necessary but not sufficient; Phase 4 must still perform structural and behavioral equivalence auditing.

## Frozen reconstruction invariants

- no Lua changes;
- approved `SoundSource.BLOCKS` distance behavior;
- center + 8 inner + 8 outer progressive direct geometry;
- private per-source EFX isolation;
- reattach direct/aux EFX on every actual environment application;
- no private EFX before PLAYING/PAUSED eligibility;
- preserve `PositionStabilizer`;
- do not inject/cancel/replace SPR `calculateOcclusion()`;
- no worker-thread SPR geometry/world raycasts;
- strict source lifetime identity;
- physics scheduling must not change PCM sample position, OpenAL playback clock, buffer offset or sync timing;
- preserve Hotfix3 100 ms partial sync grace and pending-INITIAL protection;
- preserve Beta10 direct cache/write-suppression behavior;
- preserve Beta11 room cache scope and keep cross-clone reuse telemetry-only.

## Next reconstruction order

1. Reconstruct `SoundPhysicsBridge` directly from Hotfix3 classfile/decompile.
2. Run compile and inspect any newly exposed source-form issues.
3. Reconstruct `ClothConfigScreen`.
4. Reconcile every Phase 1 class/nested-class inventory entry against source output.
5. Close Phase 3 only after full compile and inventory closure.
6. Then begin Phase 4; do not start Beta11.1/B work.
