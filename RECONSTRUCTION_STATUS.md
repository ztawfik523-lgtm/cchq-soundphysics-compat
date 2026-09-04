# Beta11 Hotfix3 source reconstruction status

Authoritative runtime baseline: `cchq_soundphysics_compat-0.1.0-beta11-hotfix3.jar`

SHA-256: `83500f182fc9829aa1a5a51fbfa11ba6cdfb645699b25d1c445167666dabc1ef`

This branch is intentionally separated from `main` until the reconstructed source tree can reproduce the tested baseline closely enough to continue development safely.

Durable context:

- [`docs/BETA11_RECONSTRUCTION_HANDOFF.md`](docs/BETA11_RECONSTRUCTION_HANDOFF.md)
- [`docs/RECONSTRUCTION_GUIDE.md`](docs/RECONSTRUCTION_GUIDE.md)
- [`docs/PHASE1_HOTFIX3_BYTECODE_AUDIT.md`](docs/PHASE1_HOTFIX3_BYTECODE_AUDIT.md)

Every bounded reconstruction run must inspect this file and the current branch before making changes. Do not optimize while reconstructing.

## Recovered exactly from the tested artifact

Resources copied from Hotfix3 itself:

- `META-INF/neoforge.mods.toml`
- `META-INF/accesstransformer.cfg`
- `cchq_soundphysics_compat.mixins.json`
- `assets/cchq_soundphysics_compat/lang/en_us.json`

## Reconstructed from original Beta11 build inputs

These files came from source used while constructing Beta11 rather than speculative decompilation:

- `AudioDecoder.java`
- `Beta11RoomRayCache.java`
- `SoundPhysicsRoomRayMemoMixin.java`

## Reconstructed against Hotfix3 bytecode before the latest Phase 1 audit

- `CCHQSoundPhysicsCompat.java`
- `DecodedAudio.java`
- `HQPayloadView.java`
- `DistanceBridge.java`
- `RoomSchedulerClient.java`
- `ConfigScreenFactory.java`
- `MinecraftMixin.java`
- `MinecraftRoomSchedulerMixin.java`
- `SoundEngineAccessor.java`
- `SoundManagerAccessor.java`
- `SoundPhysicsEnvironmentMixin.java`
- `SoundPhysicsPositionMixin.java`

These are source-level runtime reconstructions, not claims of byte-for-byte compiler reproduction.

## Phase 1 — core playback/lifecycle: bytecode-audited and repaired

The complete authoritative Hotfix3 JAR became available to the active reconstruction run and was verified to the expected SHA-256 before inspection with `javap`.

This exposed real differences between the earlier behavioral reconstruction and the tested Hotfix3 bytecode. Because Phase 1 was therefore still incomplete, the scheduled Phase 2 run correctly stopped and repaired Phase 1 instead of proceeding.

### `SyncStartCoordinator.java`

Now reconstructed to the actual Hotfix3 source shape/behavior:

- exact `Group` nested authored state (`expected`, `sources`, `createdNs`);
- `PARTIAL_FLUSH_NS = 100_000_000L`;
- `STALE_GROUP_NS = 5_000_000_000L`;
- synchronized `play(int, HQPayloadView.Audio)` entry point;
- synchronized `sourceState(int, int)` helper;
- pending `AL_INITIAL` maps to `AL_PAUSED` only for `AL_SOURCE_STATE`;
- complete and expired partial groups start through one `AL10.alSourcePlayv(int[])`;
- source removal scans pending groups and drops empty groups;
- group clear behavior matches the bytecode-visible baseline.

The older reconstruction's `GroupState` plus reverse source-to-group map was removed because it was not the Hotfix3 layout/API.

### `CompatAudioManager.java`

Repaired against exact Hotfix3 bytecode evidence:

- `ClientConfig.enabled()` interception gate restored;
- `_STREAM` and `PCM_S16LE` bridge exclusions restored;
- decode identity restored to `format + ':' + shortHash(data)`;
- `MAX_DECODE_CACHE_ENTRIES = 4` and completed-entry cap restored;
- listener X/Y/Z tracking initialized/reset to `Double.NaN`;
- `EnvironmentSmoother.register(sourceId)` restored immediately after source creation;
- reference/max distances now use `AttenuationBridge` rather than fixed values;
- initial `SoundPhysicsBridge.apply(...)` remains before start;
- start now uses exact `SyncStartCoordinator.play(sourceId, audio)` call shape;
- active-source installation occurs after the start/check path as in Hotfix3;
- maintenance and cleanup query lifecycle state through `SyncStartCoordinator.sourceState(...)`;
- listener distance update calls `Beta9Optimizer.updateDistance(...)`;
- beyond-max-distance audibility gating is restored;
- gain writes use `Beta10Optimizer.alSourcefStable(...)` and `updateAudibility(...)`;
- normal destruction begins with `EnvironmentSmoother.unregister(sourceId)` before OpenAL stop/detach/delete;
- `stopAllSources()` clears active/native buffers and `Beta11RoomRayCache`;
- session invalidation calls `SyncStartCoordinator.clear()` and `SoundPhysicsBridge.clearSourceIds()` and resets listener coordinates;
- pause/resume use the Hotfix3 source-state helper so pending INITIAL sources retain the grace-period protection;
- package-private `beta10OnSoundThread(Runnable)` seam is retained.

### Important retained baseline oddity

The Hotfix3 bytecode's failed-start cleanup after `EnvironmentSmoother.register(sourceId)` does not visibly call `EnvironmentSmoother.unregister` in the failure `finally` path. Reconstruction does not silently redesign this. Any improvement belongs to later explicitly justified development, not baseline recovery.

### Durable exact evidence

See `docs/PHASE1_HOTFIX3_BYTECODE_AUDIT.md` for the descriptors and behavior found during this audit.

## Phase 1 status

**Phase 1 source shape/behavior is now materially closer to the tested Hotfix3 baseline and the previously known descriptor/layout uncertainty for `SyncStartCoordinator` is resolved.**

Compilation is not yet expected because Phase 1 now correctly references Phase 2/3 classes that are still missing from the source tree (`EnvironmentSmoother`, `AttenuationBridge`, `Beta9Optimizer`, `Beta10Optimizer`, `SoundPhysicsBridge`, etc.). Those references are baseline evidence, not speculative dependencies.

The GitHub `reference/` base64 staging is still incomplete; the branch's manual CFR workflow should not be treated as a verified full-JAR reconstruction path until the complete reference is staged. The bytecode audit above used the separately available complete tested artifact and its verified SHA.

## Next bounded phase — SPR/acoustic core

Phase 2 remains next and was intentionally **not** started/committed in the Phase 1 repair run.

Reconstruct/verify:

- `SoundPhysicsBridge`
- `Beta10Optimizer`, including verifier-safe normal-Java `beta11RoomCacheActive()` semantics
- `AcousticCapture`
- `EnvironmentSmoother`
- private per-source EFX application/reattachment behavior
- direct-cache / OpenAL write-suppression integration
- exact acoustic register/unregister teardown chain now referenced by `CompatAudioManager`

Critical invariant for Phase 2: **every actual environment application must reattach direct/aux EFX even when parameter writes are suppressed.** No worker-thread SPR world/geometry raycasts or OpenAL ownership changes.

## Later reconstruction work

After Phase 2:

- `ProgressiveOcclusionModel`
- `PositionStabilizer`
- `Beta9Optimizer`
- `PerformanceStats`
- `AttenuationBridge`
- remaining scheduler/config/helper classes
- `ClientConfig`
- `ClientConfigAccess`
- `ClothConfigScreen`
- `HQSpeakerClientHandlerMixin`
- `HQSpeakerStopPacketMixin`
- `SoundEngineLifecycleMixin`
- `SoundPhysicsOcclusionMemoMixin`
- inventory closure for all meaningful baseline classes/resources

Then:

1. baseline audit of descriptors/constants/mixin targets/OpenAL/EFX/cache/scheduler/lifecycle behavior;
2. complete build-system/dependency setup;
3. compile and inspect the produced JAR;
4. run focused decode/cache/sync harnesses and lightweight Minecraft correctness tests;
5. only after those pass should this source become the development base for Beta11.1/B.

## Project/build skeleton

A provisional Java 21 / NeoForge 1.21.1 ModDevGradle skeleton exists for NeoForge 21.1.248, CC:Tweaked 1.120.2, SPR 1.5.1, and the tested CC:HQ Speakers version. It is not authoritative until the source tree is complete and a real build succeeds.

## Rule

Do not merge this branch into `main` merely because it compiles. The Hotfix3 JAR remains the behavioral authority until reconstruction, audit, build, and correctness verification are complete. Do not start Beta11.1/B optimizations during reconstruction.
