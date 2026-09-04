# Beta11 Hotfix3 source reconstruction status

Authoritative runtime baseline: `cchq_soundphysics_compat-0.1.0-beta11-hotfix3.jar`

SHA-256: `83500f182fc9829aa1a5a51fbfa11ba6cdfb645699b25d1c445167666dabc1ef`

This branch is intentionally separated from `main` until the reconstructed source tree can reproduce the tested baseline closely enough to continue development safely.

## Recovered exactly from the tested artifact

Resources copied from Hotfix3 itself:

- `META-INF/neoforge.mods.toml`
- `META-INF/accesstransformer.cfg`
- `cchq_soundphysics_compat.mixins.json`
- `assets/cchq_soundphysics_compat/lang/en_us.json`

## Reconstructed from original Beta11 build inputs

These files came from source used while constructing Beta11 rather than from speculative decompilation:

- `AudioDecoder.java`
- `Beta11RoomRayCache.java`
- `SoundPhysicsRoomRayMemoMixin.java`

## Reconstructed against Hotfix3 bytecode

The following source has now been written from the actual tested class/method descriptors and bytecode:

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

These are source-level reconstructions of the runtime behavior. They are not claimed byte-for-byte compiler reproductions.

## Project/build skeleton

Added a Java 21 / NeoForge 1.21.1 ModDevGradle project skeleton targeting NeoForge 21.1.248, CC:Tweaked 1.120.2, SPR 1.5.1, and the tested local CC:HQ Speakers jar.

The build definition is **provisional until the full source tree compiles**. In particular, the local CC:HQ jar is intentionally not committed and must be supplied under `libs/` for a real compile/run.

A manual-only CFR reference workflow is also staged. It must not be run until the complete baseline reference has been staged and its reconstructed JAR verifies to the Hotfix3 SHA above.

## Still to reconstruct/verify before source-level Beta11.1 work

Highest-priority runtime classes:

- `CompatAudioManager`, including Hotfix3 sync-pending source protection and room-cache teardown
- final Hotfix3 `SyncStartCoordinator` grace-period implementation
- `Beta10Optimizer`, including the verifier-safe `beta11RoomCacheActive()` implementation
- `SoundPhysicsBridge`
- `AcousticCapture`
- `EnvironmentSmoother`
- `ProgressiveOcclusionModel`
- `PositionStabilizer`
- `Beta9Optimizer`
- `PerformanceStats`
- `AttenuationBridge`

Remaining integration/config classes:

- `ClientConfig`
- `ClientConfigAccess`
- `ClothConfigScreen`
- `HQSpeakerClientHandlerMixin`
- `HQSpeakerStopPacketMixin`
- `SoundEngineLifecycleMixin`
- `SoundPhysicsOcclusionMemoMixin`

Then:

1. compile the complete source tree;
2. audit class/method descriptors, constants, mixin targets, config defaults, OpenAL calls, EFX lifecycle, scheduler thresholds, and sync behavior against Hotfix3;
3. run the existing decode/cache/sync harnesses;
4. run the lightweight Minecraft correctness test;
5. only then make the source branch the development base for Beta11.1/B.

## Rule

Do not merge this branch into `main` merely because it compiles. The Hotfix3 JAR remains the behavioral reference until reconstruction is verified.
