# Phase 3 start audit — Beta11 Hotfix3 source reconstruction

## Scope

This checkpoint records the recheck of Phase 2 and the first committed Phase 3 reconstruction batch on branch `beta11-source-reconstruction`.

Authoritative runtime baseline remains:

- JAR: `cchq_soundphysics_compat-0.1.0-beta11-hotfix3.jar`
- SHA-256: `83500f182fc9829aa1a5a51fbfa11ba6cdfb645699b25d1c445167666dabc1ef`

No Beta11.1/B optimization work belongs in this phase. Hotfix3 remains the behavioral authority until Phases 3–5 close.

## Phase 2 recheck — PASS

Phase 2 remains complete. The definitive original finish gate is GitHub Actions run `33856858450`.

The recheck confirmed that the reconstructed build project reaches the intended Phase 3 boundary:

- Java 21 / Gradle 9.2.1 wrapper;
- Minecraft 1.21.1 / NeoForge 21.1.248 development artifact pipeline;
- ModDevGradle 2.0.144;
- CC:Tweaked 1.120.2;
- tested Sound Physics Remastered 1.21.1-1.5.1 artifact;
- tested HQ Speakers dependency pinned by immutable Modrinth IDs;
- 90 compile-classpath files resolved;
- `createMinecraftArtifacts` succeeds;
- processed resource verification succeeds;
- all 11 configured client mixins are present in resource wiring;
- mixin config registration is present;
- access-transformer registration is present;
- `compileJava` reaches javac.

The prior finish-gate compile probe reported 44 `cannot find symbol` errors. Inspection confirms those are missing project-source reconstruction symbols rather than Gradle, dependency, NeoForge artifact, mixin-resource, or access-transformer failures.

After the first Phase 3 source additions below, finish-gate run `33858967668` also completed successfully. This verifies that the new reconstruction batch did not regress the Phase 2 build/wiring boundary.

## Phase 3 — STARTED

### Batch 1: CC:HQ integration and sound-engine lifecycle mixins

Committed sources:

1. `HQSpeakerClientHandlerMixin.java`
   - commit `733309b1cac07f0eff5e2167d3b206382321571f`
   - string target: `com.tom.hqspeaker.client.HQSpeakerClientHandler`
   - injects static `receive(...)` at `HEAD`, cancellable;
   - uses a coerced `Object` payload to preserve loose linkage;
   - cancels native CC:HQ whole-file handling only when `CompatAudioManager.tryHandleAudioPayload(...)` accepts the payload.

2. `HQSpeakerStopPacketMixin.java`
   - commit `523ceb3303a12737ed993262557c2621409c6b79`
   - string target: `com.tom.hqspeaker.network.HQSpeakerStopPacket`
   - mirrors the packet into `CompatAudioManager.tryHandleStopPayload(...)` at `HEAD`;
   - does not cancel CC:HQ's own stop handling.

3. `SoundEngineLifecycleMixin.java`
   - commit `1a4129b06d9e30dfd27c827e9b02eadbb436c2a5`
   - final Hotfix3 runtime metadata proves six lifecycle callbacks, not the older four-hook source shape:
     - `pause`
     - `resume`
     - `stopAll`
     - `destroy`
     - `emergencyShutdown`
     - `reload`
   - pause/resume/stopAll route to their matching `CompatAudioManager` lifecycle methods;
   - destroy/emergencyShutdown/reload route to sound-engine reset handling.

The six callback names/descriptors are runtime-evidenced. Their full source-level body equivalence is still subject to the Phase 4 structural/behavioral audit once the authoritative binary is fully restaged for direct class comparison.

## Evidence precedence for Phase 3

Use this order when reconstructing code:

1. authoritative Hotfix3 class bytecode/decompile;
2. exact runtime Mixin metadata/descriptors from Hotfix3 logs;
3. already-audited Hotfix3 source counterparts and local call sites;
4. version-matched upstream dependency source/signatures;
5. historical handoffs only as supporting architectural context.

Do **not** invent large method bodies from prose when exact Hotfix3 bytecode is unavailable.

## Current baseline-restaging limitation

The branch does not currently contain a complete copy of the authoritative Hotfix3 JAR. The retained `reference/` staging is incomplete, so large optimizer/scheduler/acoustic-core classes cannot be safely regenerated from the historical reports alone.

This is a Phase 3 evidence limitation, **not** a Phase 2 regression.

Until the exact binary is restaged, only classes whose behavior can be established from exact runtime metadata, already-audited code, or sufficiently exact preserved source should be committed.

## Remaining top-level authored source gaps after Batch 1

### Audio package

- `AttenuationBridge`
- `Beta9Optimizer`
- `Beta10Optimizer`
- `PerformanceStats`
- `PositionStabilizer`
- `ProgressiveOcclusionModel`
- `SoundPhysicsBridge`

Required nested structures from the Phase 1 binary inventory must also be reconstructed intentionally, including:

- `Beta9Optimizer$DirectEntry`
- `Beta9Optimizer$PendingDirect`
- `Beta9Optimizer$SourceMeta`
- `Beta10Optimizer$Context`
- `Beta10Optimizer$FilterState`
- `Beta10Optimizer$SourceAlState`
- `Beta10Optimizer$StampInfo`
- `PositionStabilizer$State`
- `ProgressiveOcclusionModel$State`
- `SoundPhysicsBridge$Candidate`
- `SoundPhysicsBridge$RoomEnvironmentAccess`
- `SoundPhysicsBridge$RoomEnvironmentAccess$ConfigStamp`
- `SoundPhysicsBridge$RoomStamp`
- `SoundPhysicsBridge$SourceState`

### Config package

- `ClientConfig`
- `ClientConfigAccess`
- `ClothConfigScreen`

### Mixin package

- `SoundPhysicsOcclusionMemoMixin`

That leaves **11 top-level authored Java classes** still absent after this first Phase 3 batch, plus their required nested class topology.

## Important recovered Hotfix3 behavior already present before Batch 1

Do not regress these while filling the remaining gaps:

- `SyncStartCoordinator` partial group grace uses `PARTIAL_FLUSH_NS = 100_000_000L` and stale group expiry `STALE_GROUP_NS = 5_000_000_000L`;
- full and expired-partial sync groups start with one `AL10.alSourcePlayv(int[])`;
- pending `AL_INITIAL` sources are protected during the partial-group grace period;
- `CompatAudioManager` preserves decode-generation/session identity and the four-entry completed decode-cache baseline;
- source setup uses the approved attenuation path and performs the initial `SoundPhysicsBridge.apply(...)` before synchronized play;
- `EnvironmentSmoother` never creates private EFX while a source is `AL_INITIAL`;
- bit-identical EFX parameter writes may be suppressed, but direct and auxiliary source attachments are repeated on every successful environment application;
- native SPR environment fallback remains available if isolated EFX setup/application fails;
- verifier-safe normal-Java semantics for `Beta10Optimizer.beta11RoomCacheActive()` are already established as:

```java
Context context = CONTEXT.get();
if (context != null && context.owner == OWNER_SPR) return context.cacheable;
return false;
```

## Next reconstruction order

1. Recover `SoundPhysicsOcclusionMemoMixin` only when its exact injection/redirect target can be established; do not infer a hot-path hook.
2. Recover small/frozen config and acoustic helper classes where exact preserved source or bytecode evidence is available.
3. Restage the authoritative Hotfix3 binary before implementing the large `Beta9Optimizer`, `Beta10Optimizer`, and `SoundPhysicsBridge` bodies.
4. Continue compile probes after each coherent batch.
5. Phase 3 closes only when every meaningful class/nested class in the Phase 1 inventory has an intentional source counterpart **and** the full project compiles.

## Frozen reconstruction invariants

- no Lua changes;
- preserve approved `SoundSource.BLOCKS` distance behavior;
- preserve the center + 8 inner + 8 outer progressive direct geometry;
- preserve private per-source EFX isolation;
- never optimize away required EFX reattachment;
- no private EFX before PLAYING/PAUSED eligibility;
- preserve `PositionStabilizer` behavior;
- do not inject/cancel/replace SPR `calculateOcclusion()`;
- no worker-thread SPR world/geometry raycasts;
- preserve strict source-lifetime generation identity;
- physics scheduling must not alter PCM sample position, OpenAL playback clock, buffer offset, or sync timing;
- preserve Hotfix3 partial sync-group grace/start behavior.
