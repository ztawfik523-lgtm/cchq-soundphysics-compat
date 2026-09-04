# Beta11 Hotfix3 source reconstruction status

Authoritative runtime baseline: `cchq_soundphysics_compat-0.1.0-beta11-hotfix3.jar`

SHA-256: `83500f182fc9829aa1a5a51fbfa11ba6cdfb645699b25d1c445167666dabc1ef`

This branch is intentionally separated from `main` until the reconstructed source tree can reproduce the tested baseline closely enough to continue development safely.

## Reconstructed from original Beta11 build inputs

- `AudioDecoder.java`
- `Beta11RoomRayCache.java`
- `SoundPhysicsRoomRayMemoMixin.java`

These files came from the source used while constructing Beta11 and are not speculative decompilations.

## Still to reconstruct/verify before source-level Beta11.1 work

- `CompatAudioManager` including Hotfix3 sync-pending source protection and room-cache teardown
- final Hotfix3 `SyncStartCoordinator` grace-period implementation
- `Beta10Optimizer` including the fixed `beta11RoomCacheActive()` verifier-safe implementation
- `SoundPhysicsBridge`
- existing Beta7/Beta8/Beta9/Beta10 helper/config/mixin classes
- NeoForge Gradle build files and dependency declarations
- resources: mixin config, `neoforge.mods.toml`, access transformer
- bytecode/source equivalence audit against the Hotfix3 JAR

## Rule

Do not merge this branch into `main` merely because it compiles. The Hotfix3 JAR remains the behavioral reference until reconstruction is verified.
