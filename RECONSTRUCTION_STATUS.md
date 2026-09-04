# Beta11 Hotfix3 source reconstruction status

Authoritative runtime baseline: `cchq_soundphysics_compat-0.1.0-beta11-hotfix3.jar`

SHA-256: `83500f182fc9829aa1a5a51fbfa11ba6cdfb645699b25d1c445167666dabc1ef`

Branch: `beta11-source-reconstruction`

Hotfix3 remains the behavioral authority until Phase 5 closes. Do not optimize during baseline reconstruction/audit and do not merge this branch to `main` merely because it compiles.

## Canonical five-phase status

| Phase | Status |
| --- | --- |
| Phase 1 — Freeze and inventory binary baseline | **COMPLETE / JAR-RECHECKED** |
| Phase 2 — Reconstruct build project | **COMPLETE / JAR-RECHECKED** |
| Phase 3 — Reconstruct every Java class | **COMPLETE / RECHECKED** |
| Phase 4 — Structural and behavioral equivalence audit | **IN PROGRESS** |
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
- all 65 entry fingerprints match `docs/baseline/HOTFIX3_SHA256SUMS.txt`;
- the five source-relevant resources match the exact JAR bytes;
- manifest CRLF form is preserved.

Evidence:

- `docs/baseline/BETA11_HOTFIX3_INVENTORY.md`
- `docs/baseline/HOTFIX3_SHA256SUMS.txt`
- `docs/baseline/PHASE1_FINAL_VERIFICATION.md`
- `docs/baseline/PHASE1_JAR_RECHECK_2026-09-04.md`
- `docs/PHASE1_HOTFIX3_BYTECODE_AUDIT.md`

## Phase 2 — COMPLETE / JAR-RECHECKED

Pinned project environment:

- Java 21
- Minecraft 1.21.1
- NeoForge 21.1.248
- ModDevGradle 2.0.144
- Gradle wrapper 9.2.1
- CC:Tweaked 1.120.2
- tested SPR artifact `qyVF9oeo-Dd2tmpsk.jar`
- tested HQ Speakers artifact `ygA78R8l-u5PEI5Ax.jar`
- Cloth Config for the optional config UI

Exact Hotfix3 source requires direct calls to SPR members widened by this mod's access transformer. The reconstructed build therefore keeps untouched SPR at runtime while `prepareSprCompileJar` creates an isolated access-transformed compile copy for javac. The raw SPR JAR is rejected from compileClasspath. AT CLI 10.0.6 is used only as the Java-21-capable build-time processor.

JAR-backed Phase 2 evidence:

- classpath run `33864425672` — success;
- finish-gate run `33864425687` — success;
- 90 compile-classpath files;
- NeoForge artifact pipeline passes;
- resource/mixin/AT wiring passes.

See `docs/PHASE2_BUILD_AUDIT.md`.

## Phase 3 — COMPLETE / RECHECKED

Phase 3 source reconstruction remains closed against the exact Hotfix3 class inventory.

Final authored closures were:

- `SoundPhysicsBridge.java` — commit `91d70508a04001da788ac7520e09955d5f753b09`;
- `ClothConfigScreen.java` — commit `d336bdea9d39be801360b1f286d67f29d6333772`.

The original strict closure run `33867207760` passed clean build, JAR build, exact 60/60 class-path topology and processed resources.

Before opening Phase 4, the same hard Phase 3 closure workflow was rechecked on the fully documented branch head:

- head `70d37a3e6b072a6e215cecf3c4299b96e0276968`;
- run `33867785411`;
- job `101006475065`;
- result **SUCCESS**.

All hard closure steps passed again. This confirms Phase 3 remains complete under its intended source-completeness/build/topology definition.

Phase 4 is stricter and may legitimately find field, descriptor, annotation or behavioral drift that Phase 3's class-path topology gate could not detect. Such Phase 4 corrections do not retroactively change the Phase 3 definition.

Full Phase 3 evidence:

`docs/PHASE3_FINAL_VERIFICATION.md`

## Phase 4 — IN PROGRESS

Phase 4 started with a whole-project structural ABI layer plus exact bytecode/annotation spot checks.

Durable start record:

`docs/PHASE4_START_AUDIT.md`

### Structural ABI tooling

Added:

- `tools/class_abi.py` — commit `115375d76df09dcc9ab9f468f892a294a5810192`;
- `docs/baseline/HOTFIX3_STRUCTURAL_ABI_SHA256.txt` — commit `7eda5a4ef95bc3cd547a5914227e304634ad0a7b`;
- `.github/workflows/phase4-structural-abi.yml` — commit `36fa51b90496bc4cac6de6fe947e4ea0bb45244b`.

The structural fingerprint covers all 60 compat classes and includes class/field/method access flags, superclass/interfaces, field descriptors and constants, method descriptors and Java major version.

The first two CI attempts (`33869660406`, `33869841129`) did not reach ABI comparison because NeoForged's Maven endpoint returned HTTP 502 while resolving `net.neoforged:minecraft-dependencies:1.21.1`. This is an external dependency-service failure, not an ABI result.

### First exact discrepancies found and corrected

Phase 4 has already found source details that compilation and class-path topology could not see:

1. `HQSpeakerClientHandlerMixin`
   - restored exact `receive(Lcom/tom/hqspeaker/network/HQSpeakerAudioPacket;)V` injection descriptor;
   - restored nested `@At(..., remap=false)` metadata;
   - restored Hotfix3 field `private static boolean cchqphysics$reportedHook` and its first-entry write;
   - commit `3a3cb6c9fdb383ea72e5b2b5dce80c7a3c926987`.

2. `HQSpeakerStopPacketMixin`
   - pinned exact `handle(Lcom/tom/hqspeaker/network/HQSpeakerStopPacket;Lnet/neoforged/neoforge/network/handling/IPayloadContext;)V` descriptor;
   - restored nested `@At(..., remap=false)` metadata;
   - commit `e9240528965c1fc0a31af22fb80a65b42720205e`.

3. `SyncStartCoordinator.removeSource`
   - aligned its group iteration shape from `entrySet()` to the Hotfix3 `GROUPS.values().iterator()` form;
   - commit `7ac821eaa2dfe73dec7703ec1ba4d7fcf9761acc`.

### Opening Phase 4 checks already confirmed

No correction was required for:

- `SoundPhysicsRoomRayMemoMixin`: exact redirect target, `remap=false`, `require=2`;
- `SoundPhysicsOcclusionMemoMixin`: exact internal `runOcclusion` redirect, `remap=false`, `require=1`;
- exact environment/position injection descriptors;
- six sound-engine lifecycle HEAD hooks;
- Hotfix3 sync constants and grouped `alSourcePlayv` behavior;
- pending-INITIAL lifecycle protection;
- EnvironmentSmoother PLAYING/PAUSED EFX creation gate;
- mandatory auxiliary/direct EFX reattachment on every successful environment apply;
- opening `SoundPhysicsBridge` scheduler/sentinel constants and static collection implementations.

### Phase 4 next audit order

1. Complete the 60-class structural ABI comparison once the external NeoForge endpoint permits compilation.
2. Finish exact annotation/Mixin metadata comparison for all 11 configured mixin/accessor classes.
3. Audit `SoundPhysicsBridge` scheduler/stamps/room reuse/sentinel/fairness control flow.
4. Audit Beta9/Beta10 direct cache, controller state, stamp gates and OpenAL write suppression.
5. Audit progressive occlusion, position stabilization, distance/reflection and EFX formulas/order.
6. Audit playback/decode/source lifetime/sync/lifecycle cleanup.
7. Audit config defaults/ranges and Cloth Config UI constants.
8. Close Phase 4 only after every proven discrepancy is resolved and the structural/behavioral evidence is green.

## Frozen invariants for Phases 4–5

Preserve and audit:

- no Lua changes;
- approved `SoundSource.BLOCKS` distance behavior;
- center + 8 inner + 8 outer progressive direct geometry;
- private per-source EFX isolation;
- direct/aux EFX reattachment on every actual environment application;
- no private EFX before PLAYING/PAUSED eligibility;
- `PositionStabilizer` behavior;
- do not inject/cancel/replace SPR `calculateOcclusion()`;
- no worker-thread SPR world/geometry raycasts;
- strict source lifetime identity/generation semantics;
- scheduling must not alter PCM sample position, OpenAL playback clock, buffer offset or sync timing;
- Hotfix3 100 ms partial sync grace and pending-INITIAL protection;
- Beta10 exact direct reuse and bit-identical OpenAL write suppression;
- Beta11 same-clone room-ray cache scope and telemetry-only cross-clone reuse.

## Exact next prerequisite

Continue **Phase 4 only**. Do not begin Phase 5 runtime validation or Beta11.1/B optimization until Phase 4 closes.
