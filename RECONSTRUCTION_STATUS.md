# Beta11 Hotfix3 source reconstruction status

Authoritative runtime baseline: `cchq_soundphysics_compat-0.1.0-beta11-hotfix3.jar`

SHA-256: `83500f182fc9829aa1a5a51fbfa11ba6cdfb645699b25d1c445167666dabc1ef`

This branch is intentionally separated from `main` until the reconstructed source tree has passed the full five-phase reconstruction plan in `docs/RECONSTRUCTION_PHASES.md`.

Do not optimize while reconstructing. Hotfix3 remains the behavioral authority until Phase 5 closes.

## Canonical phase status

| Phase | Status |
| --- | --- |
| Phase 1 — Freeze and inventory binary baseline | **COMPLETE** |
| Phase 2 — Reconstruct build project | **COMPLETE** |
| Phase 3 — Reconstruct every Java class | **NEXT / PARTIALLY PRE-RECOVERED** |
| Phase 4 — Structural and behavioral equivalence audit | Not started |
| Phase 5 — Runtime validation and source handover | Not started |

Durable phase evidence:

- `docs/RECONSTRUCTION_PHASES.md`
- `docs/baseline/PHASE1_FINAL_VERIFICATION.md`
- `docs/PHASE1_HOTFIX3_BYTECODE_AUDIT.md`
- `docs/PHASE2_BUILD_AUDIT.md`
- `docs/BETA11_RECONSTRUCTION_HANDOFF.md`
- `docs/RECONSTRUCTION_GUIDE.md`

## Phase 1 — COMPLETE

Phase 1 froze and inventoried the tested Hotfix3 artifact.

Established baseline facts include:

- authoritative JAR SHA-256 verified;
- all JAR entries/classes/resources inventoried;
- package and nested-class topology recorded;
- source-relevant runtime resources extracted exactly;
- `META-INF/MANIFEST.MF` corrected to the exact Hotfix3 bytes, including CRLF termination;
- per-file SHA-256 inventory retained under `docs/baseline/`.

Exact runtime resources retained under `src/main/resources` include:

- `META-INF/MANIFEST.MF`
- `META-INF/neoforge.mods.toml`
- `META-INF/accesstransformer.cfg`
- `cchq_soundphysics_compat.mixins.json`
- `assets/cchq_soundphysics_compat/lang/en_us.json`

See `docs/baseline/PHASE1_FINAL_VERIFICATION.md` for the closure evidence.

## Phase 2 — COMPLETE

Phase 2 reconstructed and verified the build project. The definitive finish gate is GitHub Actions run `33856858450`.

Verified build environment:

- Java 21
- Minecraft 1.21.1
- NeoForge 21.1.248
- ModDevGradle 2.0.144
- Gradle wrapper 9.2.1
- CC:Tweaked 1.120.2
- tested SPR artifact resolving as `qyVF9oeo-Dd2tmpsk.jar`
- tested HQ Speakers artifact pinned by immutable Modrinth IDs and resolving as `ygA78R8l-u5PEI5Ax.jar`
- Cloth Config for the optional config UI path

Phase 2 proof:

- complete Gradle wrapper committed in `58735fa6419ff439dc8532ea3ac98ad75e117501`;
- GitHub wrapper validation accepts `gradle/wrapper/gradle-wrapper.jar`;
- `./gradlew --version` resolves Gradle 9.2.1 on Java 21;
- `verifyReconstructionClasspath` resolves **90 compile-classpath files**;
- `createMinecraftArtifacts` completes the NeoForge development artifact pipeline;
- `processResources` + `verifyResourceWiring` verify the four critical processed resources, all 11 configured client mixins, mixin registration, and access-transformer registration;
- `compileJava` reaches javac with dependencies resolved;
- remaining compiler failures are project-source reconstruction gaps, not Gradle/dependency/access-transformer failures.

The Phase 2 compile probe currently reports 44 source-level `cannot find symbol` errors. Those are the expected Phase 3 boundary.

See `docs/PHASE2_BUILD_AUDIT.md` for full closure evidence.

## Phase 3 — NEXT / already partially recovered

Several Java sources were recovered before the canonical five-phase boundaries were finalized. Treat them as **partial Phase 3 progress**, not as evidence that Phase 3 is complete.

### Reconstructed from original Beta11 build inputs

- `AudioDecoder.java`
- `Beta11RoomRayCache.java`
- `SoundPhysicsRoomRayMemoMixin.java`

### Reconstructed/audited against Hotfix3 bytecode

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
- `AcousticCapture.java`
- `EnvironmentSmoother.java`
- `SyncStartCoordinator.java`
- `CompatAudioManager.java`

These are source-level runtime reconstructions, not claims of byte-for-byte compiler reproduction.

### Important Hotfix3 behavior already recovered

`SyncStartCoordinator.java` has the verified Hotfix3 group lifecycle shape, including:

- `PARTIAL_FLUSH_NS = 100_000_000L`;
- `STALE_GROUP_NS = 5_000_000_000L`;
- full and expired-partial group start through one `AL10.alSourcePlayv(int[])`;
- pending `AL_INITIAL` source-state protection during the partial-group grace period.

`CompatAudioManager.java` has important bytecode-audited baseline behavior restored, including:

- `ClientConfig.enabled()` interception gate;
- decode identity shape and four-entry completed decode cache baseline;
- `AttenuationBridge` distance usage;
- `EnvironmentSmoother` registration/lifecycle ordering;
- initial `SoundPhysicsBridge.apply(...)` before synchronized start;
- Hotfix3 source-state helper usage during lifecycle maintenance;
- Beta9/Beta10 distance/audibility/OpenAL write paths;
- session and stop-all cleanup seams.

`EnvironmentSmoother.java` preserves the critical private-EFX invariant:

- private EFX is not created while a source is `AL_INITIAL`;
- parameters may use bit-identical write suppression;
- **direct and auxiliary EFX attachments are repeated on every successful environment application**;
- native SPR fallback remains available if isolated EFX setup/application fails.

The verifier-safe normal-Java semantics of `Beta10Optimizer.beta11RoomCacheActive()` were also established from Hotfix3 bytecode:

```java
Context context = CONTEXT.get();
if (context != null && context.owner == OWNER_SPR) return context.cacheable;
return false;
```

### Known Phase 3 gaps

The current compile boundary identifies unreconstructed project symbols/classes including:

- `ClientConfig`
- `PerformanceStats`
- `SoundPhysicsBridge`
- `ProgressiveOcclusionModel`
- `PositionStabilizer`
- `Beta10Optimizer`
- `AttenuationBridge`
- `Beta9Optimizer`

The resource-wiring audit also explicitly identifies four configured mixin source files still absent:

- `HQSpeakerClientHandlerMixin`
- `HQSpeakerStopPacketMixin`
- `SoundPhysicsOcclusionMemoMixin`
- `SoundEngineLifecycleMixin`

Additional helpers/config classes and nested structures must be reconciled against the Phase 1 binary inventory before Phase 3 can close.

### Phase 3 exit rule

Do not mark Phase 3 complete merely when the first compile succeeds.

Before closure:

1. every meaningful Hotfix3 class in the Phase 1 inventory must have an intentional source counterpart;
2. nested classes, constants, descriptors, annotations, and mixin targets must be reconstructed intentionally;
3. hand-patched bytecode behavior must be represented as normal verifier-safe Java;
4. the full project must compile successfully.

## Frozen runtime/acoustic invariants during reconstruction

Preserve these while rebuilding source:

- no Lua changes;
- approved `SoundSource.BLOCKS` distance behavior;
- approved progressive direct geometry: center + 8 inner + 8 outer paths;
- private per-source EFX isolation;
- never optimize away EFX reattachment from an actual environment application;
- no private EFX before PLAYING/PAUSED eligibility;
- preserve `PositionStabilizer` behavior;
- do not inject/cancel/replace SPR `calculateOcclusion()`;
- no worker-thread SPR world/geometry raycasts;
- strict source lifetime identity;
- physics scheduling must not alter PCM sample position, OpenAL playback clock, buffer offset, or sync timing;
- preserve Hotfix3 partial sync-group grace/start behavior.

## Rule

Do not merge `beta11-source-reconstruction` into `main` merely because it compiles. Do not start Beta11.1/B optimization during reconstruction. The source tree becomes authoritative only after Phases 3, 4, and 5 close against the known-good Hotfix3 baseline.
