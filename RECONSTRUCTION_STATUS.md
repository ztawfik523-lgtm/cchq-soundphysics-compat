# Beta11 Hotfix3 source reconstruction status

Authoritative runtime baseline: `cchq_soundphysics_compat-0.1.0-beta11-hotfix3.jar`

SHA-256: `83500f182fc9829aa1a5a51fbfa11ba6cdfb645699b25d1c445167666dabc1ef`

Branch: `beta11-source-reconstruction`

Hotfix3 remains the behavioral authority until Phases 3–5 close. Do not optimize while reconstructing and do not merge this branch to `main` merely because it compiles.

## Canonical five-phase status

| Phase | Status |
| --- | --- |
| Phase 1 — Freeze and inventory binary baseline | **COMPLETE / JAR-RECHECKED** |
| Phase 2 — Reconstruct build project | **COMPLETE / JAR-RECHECKED** |
| Phase 3 — Reconstruct every Java class | **IN PROGRESS** |
| Phase 4 — Structural and behavioral equivalence audit | Not started |
| Phase 5 — Runtime validation and source handover | Not started |

Canonical plan: `docs/RECONSTRUCTION_PHASES.md`.

## Phase 1 — COMPLETE / JAR-RECHECKED

The exact Hotfix3 JAR supplied on 2026-09-04 was independently rechecked against the frozen baseline.

Verified directly from the binary:

- whole-JAR SHA-256 exactly matches the frozen authority;
- 75 ZIP entries total;
- 10 directory entries;
- 65 non-directory files;
- 60 Java classfiles, all Java 21 / class major 65;
- package/class topology remains the Phase 1 inventory baseline;
- all 65 non-directory entry SHA-256 fingerprints match `docs/baseline/HOTFIX3_SHA256SUMS.txt`;
- the five source-relevant runtime resources match the exact JAR bytes and recorded Git blob identities;
- the manifest remains the exact CRLF Hotfix3 form.

Durable evidence:

- `docs/baseline/BETA11_HOTFIX3_INVENTORY.md`
- `docs/baseline/HOTFIX3_SHA256SUMS.txt`
- `docs/baseline/PHASE1_FINAL_VERIFICATION.md`
- `docs/baseline/PHASE1_JAR_RECHECK_2026-09-04.md`
- `docs/PHASE1_HOTFIX3_BYTECODE_AUDIT.md`

No Phase 1 baseline correction was required by the new JAR recheck.

## Phase 2 — COMPLETE / JAR-RECHECKED

Pinned project environment:

- Java 21
- Minecraft 1.21.1
- NeoForge 21.1.248
- ModDevGradle 2.0.144
- Gradle wrapper 9.2.1
- CC:Tweaked 1.120.2
- tested SPR artifact `qyVF9oeo-Dd2tmpsk.jar`
- tested HQ Speakers artifact `ygA78R8l-u5PEI5Ax.jar`, pinned by immutable Modrinth IDs
- Cloth Config for the optional config UI

The exact Hotfix3 bytecode exposed one build-project detail that the earlier Phase 2 probe could not prove: Hotfix3 source directly calls SPR members which are private in the published SPR JAR and are widened by this mod's access transformer. Runtime AT registration was already correct, but javac also needs to see an access-transformed SPR compile artifact.

The reconstruction now handles this explicitly:

- runtime still uses the untouched tested SPR JAR;
- `prepareSprCompileJar` creates an isolated compile-only SPR copy;
- the exact Hotfix3 access transformer is applied to that copy;
- javac uses `sound-physics-remastered-at.jar`;
- the raw SPR JAR is explicitly rejected from `compileClasspath`;
- nothing from the transformed compile copy is bundled as a replacement runtime SPR.

The first attempted standalone processor, AT CLI 10.0.1, failed because its bundled ASM could not parse Java-21 classfiles (`Unsupported class file major version 65`). The build-only processor was therefore moved to AT CLI 10.0.6. Runtime dependency versions were not changed.

Current recheck evidence:

- classpath workflow run `33864425672` — **SUCCESS**;
- finish-gate workflow run `33864425687` — **SUCCESS**;
- reconstruction commit `cef50d04fbb03b4f523961aeb95f2f0377856994`;
- `verifyReconstructionClasspath` resolves 90 files and verifies `sound-physics-remastered-at.jar` is present while raw SPR is absent;
- `createMinecraftArtifacts` succeeds;
- `verifyResourceWiring` succeeds with four critical processed resources, all 11 client mixins, and AT registration;
- `compileJava` now reaches javac with the SPR accessibility issue gone.

The current compile probe reports **17 source errors, all caused by the still-missing `SoundPhysicsBridge` symbol**. There are no remaining private-access errors for `SoundPhysics.runOcclusion`, and no dependency/plugin/NeoForge/AT preprocessing failure.

Therefore Phase 2 is closed again against the exact JAR-backed source requirements.

See `docs/PHASE2_BUILD_AUDIT.md`.

## Phase 3 — IN PROGRESS

The exact uploaded Hotfix3 JAR is now the direct evidence authority for reconstruction. The previous limitation that the binary was unavailable to the active reconstruction work is no longer valid for this reconstruction session. The repository's `reference/` base64 staging is still incomplete, so CI CFR automation still needs the full binary staged before it can decompile independently in GitHub Actions; this is a repository-staging limitation, not an evidence limitation for current reconstruction.

### Reconstructed source currently present

Original/preserved Beta11 build inputs:

- `AudioDecoder.java`
- `Beta11RoomRayCache.java`
- `SoundPhysicsRoomRayMemoMixin.java`

Reconstructed/audited runtime and helper source includes:

- `CCHQSoundPhysicsCompat.java`
- `DecodedAudio.java`
- `HQPayloadView.java`
- `DistanceBridge.java`
- `AttenuationBridge.java`
- `AcousticCapture.java`
- `EnvironmentSmoother.java`
- `SyncStartCoordinator.java`
- `CompatAudioManager.java`
- `RoomSchedulerClient.java`
- `PositionStabilizer.java`
- `ProgressiveOcclusionModel.java`
- `PerformanceStats.java`
- `Beta9Optimizer.java`
- `Beta10Optimizer.java`
- `ClientConfig.java`
- `ClientConfigAccess.java`
- `ConfigScreenFactory.java`
- `MinecraftMixin.java`
- `MinecraftRoomSchedulerMixin.java`
- `SoundEngineAccessor.java`
- `SoundManagerAccessor.java`
- `SoundPhysicsEnvironmentMixin.java`
- `SoundPhysicsPositionMixin.java`
- `SoundPhysicsOcclusionMemoMixin.java`
- `HQSpeakerClientHandlerMixin.java`
- `HQSpeakerStopPacketMixin.java`
- `SoundEngineLifecycleMixin.java`

Important Phase 3 commits include:

- `733309b1cac07f0eff5e2167d3b206382321571f` — HQ receive mixin;
- `523ceb3303a12737ed993262557c2621409c6b79` — HQ stop mixin;
- `1a4129b06d9e30dfd27c827e9b02eadbb436c2a5` — six-hook sound-engine lifecycle mixin;
- `7cd38e414e99ffcf5d51fac46a2290f94ab967f4` — Hotfix3 `Beta10Optimizer` reconstruction.

### Current top-level authored gaps

Only two known top-level authored Java sources remain absent:

1. `audio/SoundPhysicsBridge.java` — **principal runtime/acoustic-core gap**;
2. `config/ClothConfigScreen.java` — optional config UI source.

`SoundPhysicsBridge` is responsible for the remaining compile boundary. The current 17 javac errors are references to this missing class from `Beta9Optimizer`, `Beta10Optimizer`, `RoomSchedulerClient`, `EnvironmentSmoother`, `Beta11RoomRayCache`, and `CompatAudioManager`.

Required `SoundPhysicsBridge` nested topology from the binary inventory must be reconstructed intentionally:

- `SoundPhysicsBridge$Candidate`
- `SoundPhysicsBridge$RoomEnvironmentAccess`
- `SoundPhysicsBridge$RoomEnvironmentAccess$ConfigStamp`
- `SoundPhysicsBridge$RoomStamp`
- `SoundPhysicsBridge$SourceState`

Do not implement a compile-only stub. Reconstruct the Hotfix3 scheduler, stamps, room-result reuse/clearing behavior, urgency/fairness logic, source registration/lifetime state, and acoustic application paths from the actual classfile/decompile.

### Phase 3 exit rule

Do not mark Phase 3 complete merely when javac turns green.

Before closure:

1. every meaningful Hotfix3 class in the Phase 1 inventory must have an intentional source counterpart or a documented compiler-generated origin;
2. nested classes, constants, descriptors, annotations and mixin targets must be intentional;
3. hand-patched bytecode behavior must be represented as verifier-safe normal Java;
4. `SoundPhysicsBridge` and `ClothConfigScreen` must be reconstructed;
5. the full project must compile successfully.

See `docs/PHASE3_START_AUDIT.md` for the detailed current checkpoint.

## Frozen reconstruction invariants

Preserve throughout Phases 3–5:

- no Lua changes;
- approved `SoundSource.BLOCKS` distance behavior;
- center + 8 inner + 8 outer progressive direct geometry;
- private per-source EFX isolation;
- never optimize away direct/aux EFX reattachment from an actual environment application;
- no private EFX before PLAYING/PAUSED eligibility;
- preserve `PositionStabilizer` behavior;
- do not inject/cancel/replace SPR `calculateOcclusion()`;
- no worker-thread SPR world/geometry raycasts;
- strict source lifetime identity;
- physics scheduling must not alter PCM sample position, OpenAL playback clock, buffer offset, or sync timing;
- preserve Hotfix3 100 ms partial-sync-group grace and pending-INITIAL protection;
- preserve Beta10 exact direct reuse and bit-identical OpenAL write suppression;
- preserve Beta11 same-clone room-ray cache scope and keep cross-clone reuse telemetry-only.

## Next exact prerequisite

Continue Phase 3 by reconstructing `SoundPhysicsBridge` from the exact Hotfix3 classfile/decompile, then run the full compile gate. Reconstruct `ClothConfigScreen` before Phase 3 closure. Only after Phase 3 closes should Phase 4 structural/behavioral equivalence auditing begin.
