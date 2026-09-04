# Phase 3 checkpoint — Beta11 Hotfix3 source reconstruction

> **CLOSED / SUPERSEDED:** Phase 3 completed successfully on 2026-09-04. The authoritative closure record is `docs/PHASE3_FINAL_VERIFICATION.md`. This file is retained as the historical start/mid-phase checkpoint and should not be used for current gap counts or next-step instructions.

Authoritative baseline:

- JAR: `cchq_soundphysics_compat-0.1.0-beta11-hotfix3.jar`
- SHA-256: `83500f182fc9829aa1a5a51fbfa11ba6cdfb645699b25d1c445167666dabc1ef`
- branch: `beta11-source-reconstruction`

## Final closure summary

Phase 3 is now **COMPLETE**.

Final authored source closures:

- `SoundPhysicsBridge.java` — commit `91d70508a04001da788ac7520e09955d5f753b09`;
- `ClothConfigScreen.java` — commit `d336bdea9d39be801360b1f286d67f29d6333772`.

Strict closure workflow commit:

`e918e3199b98332c0320eb4cd07e34740d1ec8ec`

Definitive run:

`33867207760` — **SUCCESS**.

Closure output:

```text
compileJava: PASS
jar: PASS
Hotfix3 60-class topology: PASS
source-relevant processed resources: PASS
```

Topology counts:

```text
Hotfix3 expected classes: 60
Reconstructed classes:    60
```

The expected/actual class-path diff is empty.

The canonical next phase is **Phase 4 — Structural and behavioral equivalence audit**. No Beta11.1/B optimization work belongs there.

## Historical Phase 3 start context

The first bounded Phase 3 batch reconstructed the remaining CC:HQ/sound-engine integration mixins:

- `HQSpeakerClientHandlerMixin.java` — commit `733309b1cac07f0eff5e2167d3b206382321571f`;
- `HQSpeakerStopPacketMixin.java` — commit `523ceb3303a12737ed993262557c2621409c6b79`;
- `SoundEngineLifecycleMixin.java` — commit `1a4129b06d9e30dfd27c827e9b02eadbb436c2a5`.

Hotfix3 runtime metadata proved the final lifecycle mixin has six callbacks:

- `pause`
- `resume`
- `stopAll`
- `destroy`
- `emergencyShutdown`
- `reload`

The HQ stop callback descriptor was runtime-evidenced, and version-matched upstream HQ source confirmed the static receive/stop entry shapes used by the mixins.

## Exact JAR availability change

An earlier checkpoint said the authoritative JAR was unavailable to active reconstruction. That limitation ended when the exact JAR was supplied and verified byte-for-byte against the frozen SHA-256.

Evidence precedence used for the remainder of Phase 3 was:

1. Hotfix3 class bytecode/classfile metadata;
2. exact runtime Mixin metadata/descriptors;
3. already-audited source/local call sites;
4. version-matched dependency source/signatures;
5. historical handoffs as supporting context only.

## Phase 1 and Phase 2 rechecks performed before source closure

Phase 1 was revalidated from the exact JAR and remained complete. See `docs/baseline/PHASE1_JAR_RECHECK_2026-09-04.md`.

Phase 2 was revalidated and refined so javac sees SPR members widened by the exact Hotfix3 access transformer through an isolated transformed compile copy.

Final Phase 2 recheck:

- classpath run `33864425672` — success;
- finish-gate run `33864425687` — success;
- compileClasspath: 90 files;
- transformed compile artifact: `sound-physics-remastered-at.jar`;
- raw SPR absent from compileClasspath;
- NeoForge artifacts/resources/mixins/AT wiring pass.

Before `SoundPhysicsBridge` reconstruction, the clean source boundary was 17 javac errors, all references to that missing class. Commit `91d70508a04001da788ac7520e09955d5f753b09` removed that blocker and compiled successfully.

## Source reconstructed through Phase 3

Phase 3 accounts for the full Hotfix3 compat topology, including:

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
- `SoundPhysicsBridge`
- `SyncStartCoordinator`

### Config

- `ClientConfig`
- `ClientConfigAccess`
- `ClothConfigScreen`
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

Compiler-generated/nested outputs reconcile exactly to the Phase 1 60-class path set.

## Important exact behavior recovered during Phase 3

### `SoundPhysicsOcclusionMemoMixin`

The exact Hotfix3 hook redirects SPR's internal `runOcclusion(...)` invocation inside `calculateOcclusion(...)`. It does **not** cancel or replace `calculateOcclusion()`.

### `ProgressiveOcclusionModel`

Recovered source preserves the approved 17-path geometry and alternating partial refresh behavior.

### `PositionStabilizer`

Recovered source preserves Hotfix3 reflection redirection/smoothing behavior.

### `Beta9Optimizer` / `Beta10Optimizer`

Recovered source preserves exact direct-reuse/stamp semantics, Beta10 direct caching/controller behavior, bit-identical OpenAL write suppression and verifier-safe `beta11RoomCacheActive()` normal-Java behavior.

### `SoundPhysicsBridge`

Recovered source represents source lifetime state, balanced room scheduling, stationary room stamps/reuse, clearing sentinel transition logic, urgency/fairness selection, acoustic capture/application and position integration. The classfile confirmed stable sound identity:

`cchq_soundphysics_compat:hq_speaker/<speaker UUID without dashes>`

### `ClothConfigScreen`

Recovered source retains the tested UI strings/defaults/ranges, including title `CC:HQ × Sound Physics` and interval separator `•`.

## Frozen invariants carried into Phase 4

- no Lua changes;
- approved `SoundSource.BLOCKS` distance behavior;
- center + 8 inner + 8 outer progressive direct geometry;
- private per-source EFX isolation;
- direct/aux EFX reattachment on every actual environment application;
- no private EFX before PLAYING/PAUSED eligibility;
- preserve `PositionStabilizer`;
- do not inject/cancel/replace SPR `calculateOcclusion()`;
- no worker-thread SPR geometry/world raycasts;
- strict source lifetime identity;
- physics scheduling must not change PCM sample position, OpenAL playback clock, buffer offset or sync timing;
- preserve Hotfix3 100 ms partial sync grace and pending-INITIAL protection;
- preserve Beta10 direct cache/write-suppression behavior;
- preserve Beta11 room cache scope and keep cross-clone reuse telemetry-only.

For current status and next work, use `docs/PHASE3_FINAL_VERIFICATION.md`, `RECONSTRUCTION_STATUS.md`, and `docs/BETA11_RECONSTRUCTION_HANDOFF.md`.
