# Beta11 Hotfix3 source reconstruction status

Authoritative runtime baseline: `cchq_soundphysics_compat-0.1.0-beta11-hotfix3.jar`

SHA-256: `83500f182fc9829aa1a5a51fbfa11ba6cdfb645699b25d1c445167666dabc1ef`

Branch: `beta11-source-reconstruction`

Hotfix3 remains the behavioral authority until Phase 5 closes. Do not optimize during the baseline audit and do not merge this branch to `main` merely because it compiles.

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

The exact Hotfix3 JAR supplied on 2026-09-04 independently confirms the frozen baseline:

- whole-JAR SHA-256 exactly matches;
- 75 ZIP entries, 65 files and 60 Java-21 classfiles;
- all 65 entry fingerprints match `docs/baseline/HOTFIX3_SHA256SUMS.txt`;
- all five source-relevant runtime resources match the exact JAR bytes;
- manifest CRLF form is preserved.

Evidence:

- `docs/baseline/BETA11_HOTFIX3_INVENTORY.md`
- `docs/baseline/HOTFIX3_SHA256SUMS.txt`
- `docs/baseline/PHASE1_FINAL_VERIFICATION.md`
- `docs/baseline/PHASE1_JAR_RECHECK_2026-09-04.md`
- `docs/PHASE1_HOTFIX3_BYTECODE_AUDIT.md`

## Phase 2 — COMPLETE / JAR-RECHECKED

Pinned build environment:

- Java 21
- Minecraft 1.21.1
- NeoForge 21.1.248
- ModDevGradle 2.0.144
- Gradle wrapper 9.2.1
- CC:Tweaked 1.120.2
- tested SPR artifact `qyVF9oeo-Dd2tmpsk.jar`
- tested HQ Speakers artifact `ygA78R8l-u5PEI5Ax.jar`
- Cloth Config optional UI dependency

Hotfix3 source calls SPR members widened by this mod's access transformer. The reconstruction therefore keeps untouched SPR at runtime while `prepareSprCompileJar` creates an isolated access-transformed compile copy for javac; raw SPR is rejected from compileClasspath. AT CLI 10.0.6 is build-only.

Verified Phase 2 evidence includes successful classpath and finish gates, 90 compile-classpath files, NeoForge artifact creation and resource/mixin/AT wiring.

See `docs/PHASE2_BUILD_AUDIT.md`.

## Phase 3 — COMPLETE / RECHECKED

All authored Java source is present, including the final two classes:

- `SoundPhysicsBridge.java` — original Phase 3 closure commit `91d70508a04001da788ac7520e09955d5f753b09`;
- `ClothConfigScreen.java` — original Phase 3 closure commit `d336bdea9d39be801360b1f286d67f29d6333772`.

Original strict closure run `33867207760` passed clean compile, JAR build, exact 60/60 class topology and processed resources.

### Fresh recheck after Phase 4 corrections

Phase 3 was re-run again after the first exact Phase 4 source corrections:

- source head: `29dc17439944f4d2b029e33a8d75a5693827e8b6`;
- run: `33896745559`;
- job: `101101056470`;
- result: **SUCCESS**.

The log reports:

```text
compileJava: PASS
jar: PASS
Hotfix3 60-class topology: PASS
source-relevant processed resources: PASS
Hotfix3 expected classes: 60
Reconstructed classes:    60
```

Therefore Phase 3 remains closed under its source-completeness/build/topology definition while Phase 4 performs stricter equivalence work.

Evidence: `docs/PHASE3_FINAL_VERIFICATION.md` and `docs/PHASE4_PROGRESS_2026-09-04.md`.

## Phase 4 — IN PROGRESS

Phase 4 is comparing the reconstructed output against the exact Hotfix3 classfiles. Compilation alone is not accepted as equivalence evidence.

Durable records:

- `docs/PHASE4_START_AUDIT.md`
- `docs/PHASE4_PROGRESS_2026-09-04.md`

### Whole-project structural ABI

Tooling:

- `tools/class_abi.py`
- `docs/baseline/HOTFIX3_STRUCTURAL_ABI_SHA256.txt`
- `.github/workflows/phase4-structural-abi.yml`

The ABI fingerprint covers all 60 compat classes and includes Java major version, class access/super/interfaces, field names/descriptors/access/constants, and method names/descriptors/access.

The first real comparison reached **58/60** and exposed two remaining structural discrepancies. Exact classfile review corrected them in source commit:

`ec282e4b7057f709b389884f261d38a582ebc15d`

Corrections included:

- remove non-Hotfix3 `Beta9Optimizer.resetControllerForHotfix3()`;
- restore exact Beta9 `registerSource`, unknown-audibility transition and invalid-distance semantics;
- restore Beta10's original reflection-based reset of private Beta9 controller fields;
- restore package-private `ProgressiveOcclusionModel.State` nested visibility/constructor ABI.

Fresh ABI gate:

- run `33896745650`;
- job `101101056810`;
- result: **SUCCESS**;
- expected classes: 60;
- reconstructed classes: 60;
- `Hotfix3 class/field/method structural ABI: PASS`.

So the current source is **60/60 structurally exact under the ABI fingerprint**.

### Structural evidence export

`.github/workflows/phase4-structural-export.yml` clean-builds and exports the reconstructed JAR, class list and `javap -p -s -constants` output for all 60 classes.

Current-head export:

- run `33897048940`;
- job `101102019555`;
- result: **SUCCESS**;
- artifact id `9946220940`;
- artifact digest `sha256:3ad4dcf8a3f12feca59451022402214f63544c82f159ff264aba672315a89aeb`.

Whole-JAR byte identity is not required because recompiled classfile metadata/layout may legitimately differ; Phase 4 compares structural and behavioral semantics.

### Exact discrepancies already corrected

Earlier Phase 4 corrections:

1. `HQSpeakerClientHandlerMixin`
   - exact receive descriptor;
   - exact nested `@At(..., remap=false)` metadata;
   - exact cancellable/remap behavior and `@Coerce`;
   - restored `cchqphysics$reportedHook`.

2. `HQSpeakerStopPacketMixin`
   - exact handle descriptor;
   - nested `remap=false` metadata;
   - exact `@Coerce` parameters.

3. `SyncStartCoordinator.removeSource`
   - exact `GROUPS.values().iterator()` source shape.

Current Phase 4 corrections:

4. Beta9/Beta10 controller/cache registration ABI and semantics listed above.
5. `ProgressiveOcclusionModel.State` constructor/access ABI.
6. `SyncStartCoordinator.sourceState` and `playVector` were further aligned to exact Hotfix3 bytecode source shape in commit `cc019e5088df3ec3544b43b177208c6093f71943` (stored `now` local and `ids.length` loop bound).

### Method-body audit underway

Direct Hotfix3-vs-rebuilt `javap -p -c -s` comparison has begun for the highest-risk classes. Method inventory currently matches exactly in:

- `SoundPhysicsBridge` — 26/26;
- `SyncStartCoordinator` — 11/11;
- `EnvironmentSmoother` — 18/18;
- `CompatAudioManager` — 38/38;
- `ProgressiveOcclusionModel` — 27/27;
- `Beta9Optimizer` — 42/42;
- `Beta10Optimizer` — 36/36.

The bytecode comparison is used as a review aid because compiler-local layout, branch offsets and constant-pool choices may differ without semantic drift.

Opening `SoundPhysicsBridge` comparison has found only very small compiler-layout differences in `apply(...)` and `runClearingSentinel(...)`; no proven semantic mismatch has been established there yet.

### Already-confirmed invariant checks

No correction was required for:

- `SoundPhysicsRoomRayMemoMixin` exact redirect/remap/`require=2`;
- `SoundPhysicsOcclusionMemoMixin` exact internal `runOcclusion` redirect/remap/`require=1`;
- exact environment/position injection descriptors;
- all six sound-engine lifecycle HEAD hooks;
- Hotfix3 synchronized-start constants and `alSourcePlayv` behavior;
- pending-INITIAL protection;
- EnvironmentSmoother PLAYING/PAUSED private-EFX creation gate;
- mandatory auxiliary/direct EFX reattachment on each successful environment application;
- opening `SoundPhysicsBridge` scheduler/sentinel constants and collection types.

### Remaining Phase 4 work

Phase 4 is **not complete**. Still required:

1. Finish annotation/Mixin metadata comparison across all 11 configured mixin/accessor classes.
2. Finish `SoundPhysicsBridge` scheduler, source/room stamp, same-clone reuse, sentinel transition and fairness control-flow audit.
3. Finish Beta9/Beta10 direct cache, ray ownership, stamp validation, adaptive controller and OpenAL write-suppression audit.
4. Finish progressive occlusion, position stabilization, attenuation/distance/reflection and EFX formulas/order.
5. Finish playback/decode/source-lifetime/sync/lifecycle cleanup audit.
6. Finish config defaults/ranges and Cloth Config UI constants/tooltips.
7. Re-run hard build/topology/ABI gates after every proven correction.
8. Create `PHASE4_FINAL_VERIFICATION.md` only after all structural and behavioral audit evidence is clean.

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

Continue **Phase 4 only**, beginning with high-risk method-body and annotation equivalence. Do not begin Phase 5 or Beta11.1/B optimization until Phase 4 closes.
