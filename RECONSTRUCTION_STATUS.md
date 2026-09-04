# Beta11 Hotfix3 source reconstruction status

Authoritative runtime baseline: `cchq_soundphysics_compat-0.1.0-beta11-hotfix3.jar`

SHA-256: `83500f182fc9829aa1a5a51fbfa11ba6cdfb645699b25d1c445167666dabc1ef`

This branch is intentionally separated from `main` until the reconstructed source tree can reproduce the tested baseline closely enough to continue development safely.

**Durable handoff/context for all scheduled reconstruction runs:** [`docs/RECONSTRUCTION_GUIDE.md`](docs/RECONSTRUCTION_GUIDE.md). Scheduled runs should read that guide and this status file before making changes.

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

## Pass 1 — core playback/lifecycle reconstructed from recovered lineage + Hotfix3 invariants

Added:

- `CompatAudioManager.java`
- `SyncStartCoordinator.java` (including authored nested `GroupState`)

Evidence used for this pass was deliberately layered rather than guessed:

- recovered pre-Hotfix source lineage for `CompatAudioManager`;
- accepted beta handoff documentation showing the sound-thread ownership model, every-second-client-tick maintenance cadence, stable UUID-aware `SoundPhysicsBridge.apply(...)` call shape, and synchronized-group behavior;
- tested Hotfix3 runtime/log evidence;
- the Hotfix3 reconstruction guide's explicit sync fix.

Hotfix3 behavior now represented in source:

- synchronized complete groups use one `AL10.alSourcePlayv(int[])` start;
- `PARTIAL_FLUSH_NS = 100_000_000L`;
- incomplete declared groups flush after the 100 ms grace and start all sources that actually arrived together;
- an `AL_INITIAL` source still pending in `SyncStartCoordinator` is treated as live by maintenance instead of being destroyed;
- stop/replacement removes pending sources from sync state;
- reset/stop-all clears sync state;
- world/sound-session teardown clears `Beta11RoomRayCache` and resets `RoomSchedulerClient`;
- source maintenance remains sound-thread-owned and does not move physics/audio clocks to worker threads.

### Pass 1 verification caveat — do not erase this

The complete Hotfix3 JAR is **not yet fully staged in this branch**. `reference/beta11-hotfix3.jar.b64.part00` is only the first fragment, and the manual CFR workflow currently fails the SHA check before decompilation because the reference is incomplete.

Therefore `CompatAudioManager` and `SyncStartCoordinator` are currently **behavioral/source-level reconstructions, not yet bytecode-descriptor-verified Hotfix3 reproductions**. In particular, the exact Hotfix3 private method names/descriptors and exact nested `GroupState` field layout remain audit items. Do not silently rewrite them based on taste; compare them to the complete tested artifact when it becomes available.

Pass 2 must also verify the exact acoustic/capture teardown calls surrounding source creation/destruction once `SoundPhysicsBridge` and `AcousticCapture` are reconstructed. The current manager intentionally does not invent undocumented capture cleanup calls.

### Scheduled Phase 1 verification

Re-read the durable handoff, current branch history, `CompatAudioManager`, and `SyncStartCoordinator` before touching code. No source rewrite was justified in this bounded verification pass: the existing implementation already matches every Phase 1 behavior currently evidenced by the Hotfix3 handoff and runtime history.

Verified without speculative changes:

- complete sync groups are launched through one `AL10.alSourcePlayv(int[])` call;
- partial groups use the documented `100_000_000L` first-arrival grace and also launch the arrived set through one `playv` call;
- pending membership is indexed by source id, allowing `AL_INITIAL` sources to survive maintenance while waiting for the grace/full group;
- source replacement/explicit stop removes pending sync membership before deleting the source;
- `stopAllSources()` clears sync state before walking active sources;
- buffer refs are incremented before source installation and released on failed installation or source destruction;
- OpenAL source create/start/stop/delete work is scheduled/owned on the Minecraft sound thread;
- world/session invalidation clears decode/ready/generation state and clears `Beta11RoomRayCache` / resets `RoomSchedulerClient`;
- no Beta11.1/B optimization was introduced.

Remaining uncertainty is unchanged and explicit: because the complete Hotfix3 reference JAR is not staged, exact private descriptors, exact nested `GroupState` layout, and exact acoustic/capture register/destroy hooks cannot yet be bytecode-verified. Those items must remain audit/Phase 2 work rather than being guessed here.

## Project/build skeleton

Added a Java 21 / NeoForge 1.21.1 ModDevGradle project skeleton targeting NeoForge 21.1.248, CC:Tweaked 1.120.2, SPR 1.5.1, and the tested local CC:HQ Speakers jar.

The build definition is **provisional until the full source tree compiles**. In particular, the local CC:HQ jar is intentionally not committed and must be supplied under `libs/` for a real compile/run.

A manual-only CFR reference workflow is also staged. It must not be run until the complete baseline reference has been staged and its reconstructed JAR verifies to the Hotfix3 SHA above.

## Still to reconstruct/verify before source-level Beta11.1 work

Next prerequisite — SPR/acoustic core:

- `Beta10Optimizer`, including the verifier-safe `beta11RoomCacheActive()` implementation
- `SoundPhysicsBridge`
- `AcousticCapture`
- verify the exact source-register/source-destroy acoustic lifecycle hooks used by Hotfix3

Then:

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
