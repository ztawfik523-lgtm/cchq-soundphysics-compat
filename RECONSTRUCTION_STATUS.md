# Beta11 Hotfix3 source reconstruction status

Authoritative runtime baseline: `cchq_soundphysics_compat-0.1.0-beta11-hotfix3.jar`

SHA-256: `83500f182fc9829aa1a5a51fbfa11ba6cdfb645699b25d1c445167666dabc1ef`

Branch: `beta11-source-reconstruction`

Hotfix3 remains the behavioral authority. Phase 4 is now closed; Phase 5 has not been started.

## Canonical five-phase status

| Phase | Status |
| --- | --- |
| Phase 1 — Freeze and inventory binary baseline | **COMPLETE / JAR-RECHECKED** |
| Phase 2 — Reconstruct build project | **COMPLETE / JAR-RECHECKED** |
| Phase 3 — Reconstruct every Java class | **COMPLETE / RECHECKED** |
| Phase 4 — Structural and behavioral equivalence audit | **COMPLETE / RECHECKED** |
| Phase 5 — Runtime validation and source handover | **NOT STARTED** |

Canonical plan: `docs/RECONSTRUCTION_PHASES.md`.

## Phase 1 — COMPLETE / JAR-RECHECKED

The exact Hotfix3 JAR independently confirms the frozen baseline:

- whole-JAR SHA-256 exactly matches the frozen authority;
- 75 ZIP entries, 65 files and 60 Java-21 classfiles;
- all frozen entry fingerprints were recorded;
- five source-relevant runtime resources were frozen byte-for-byte.

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

Hotfix3 source calls SPR members widened by this mod's access transformer. The reconstruction keeps untouched SPR at runtime while `prepareSprCompileJar` creates an isolated access-transformed compile copy for javac. AT CLI 10.0.6 is build-only.

Final Phase 4 recheck still passes the Phase 2 sanity gates from the final audited code/build head `98e7dedb7ecf6fda22008b084b6bb41956edff78`:

- reconstruction classpath run `33924056408`, job `101188553565` — **SUCCESS**;
- finish/build/resource run `33924056328`, job `101188553502` — **SUCCESS**.

See `docs/PHASE2_BUILD_AUDIT.md`.

## Phase 3 — COMPLETE / RECHECKED

All authored Java source is present. The original Phase 3 closure was followed by repeated closure gates after Phase 4 corrections.

Final Phase 3 recheck on the final Phase 4 code/build head:

- head: `98e7dedb7ecf6fda22008b084b6bb41956edff78`;
- run: `33924056330`;
- job: `101188553632`;
- result: **SUCCESS**.

That gate cleanly compiled the complete source and reconciled:

- exact **60/60** Hotfix3 class topology;
- source-relevant processed resources;
- complete source closure.

Evidence: `docs/PHASE3_FINAL_VERIFICATION.md`.

## Phase 4 — COMPLETE / RECHECKED

Final verification record:

`docs/PHASE4_FINAL_VERIFICATION.md`

Supporting records:

- `docs/PHASE4_START_AUDIT.md`
- `docs/PHASE4_PROGRESS_2026-09-04.md`
- `docs/PHASE4_MIXIN_ANNOTATION_AUDIT.md`

### Final audited code/build head

`98e7dedb7ecf6fda22008b084b6bb41956edff78`

This final Phase 4 code/build head includes the manifest packaging correction so the rebuilt JAR consumes the exact frozen Hotfix3 manifest rather than Gradle's generated minimal manifest.

### Final hard gates

| Gate | Run | Job | Result |
| --- | ---: | ---: | --- |
| Phase 2 classpath sanity | `33924056408` | `101188553565` | **SUCCESS** |
| Phase 2 finish/build/resource sanity | `33924056328` | `101188553502` | **SUCCESS** |
| Phase 3 source closure | `33924056330` | `101188553632` | **SUCCESS** |
| Phase 4 structural ABI | `33924056396` | `101188553422` | **SUCCESS** |
| Phase 4 structural export | `33924056370` | `101188553458` | **SUCCESS** |

### Final exported evidence

- artifact id: `9956169844`
- artifact digest: `sha256:eabb3f3cfd54bcd113c5b3af5a018ee740dcbec0fd5167d817511e32ef5c9215`
- rebuilt JAR SHA-256: `efd8c44fec7e0446d97e8e60a99e811a4e57be1f83782a55c2fa74dc8bc09bf6`

Whole-JAR byte identity with the historical artifact is not required because normal recompilation changes classfile compiler/debug/layout metadata. Phase 4 instead established the equivalence layers below.

### Final equivalence results

- **65/65 file topology** in the rebuilt JAR;
- **60/60 class paths exact**;
- **60/60 structural ABI exact** under the class/field/method fingerprint;
- **69/69 compiled `ConstantValue` entries exact**;
- **0 bootstrap method argument / string-concat recipe mismatches**;
- **11/11 configured Mixin/accessor semantic annotation sets reconciled**;
- **5/5 non-class JAR resources byte-for-byte exact**, including the exact 55-byte CRLF manifest;
- method/control-flow audit covered **550 methods**;
- normalized comparison yielded 478 instruction-equivalent methods and 72 residual compiler-shape differences in 13 reviewed classes;
- all residual differences were reviewed and no unresolved proven Hotfix3 behavior discrepancy remains.

The final manifest-only build correction did not perturb Java output: all 60 compiled classfiles in the final `98e7dedb...` export are byte-for-byte identical to the previously audited `ed7db4e8...` export.

### Important Phase 4 corrections retained

Phase 4 corrected proven discrepancies before closure, including:

- exact HQ receive and stop Mixin descriptors/metadata/coercion;
- restoration of the HQ receive reported-hook field;
- exact SyncStartCoordinator removal/source-state/play-vector behavior/source shape;
- removal of the non-Hotfix3 Beta9 reset helper;
- exact Beta9 registration, unknown-audibility and invalid-distance behavior;
- restoration of Beta10's reflection-based private Beta9 controller reset;
- exact `ProgressiveOcclusionModel.State` access/constructor ABI;
- exact EnvironmentSmoother OpenAL failure-code formatting;
- exact ClientConfigAccess reflection failure diagnostic text;
- exact Hotfix3 manifest packaging.

### Frozen invariants at Phase 4 closure

Preserved by the reconstructed source/audit:

- no Lua changes;
- approved `SoundSource.BLOCKS` distance behavior;
- center + 8 inner + 8 outer progressive direct geometry;
- private per-source EFX isolation;
- direct/aux EFX reattachment on every actual environment application;
- no private EFX before PLAYING/PAUSED eligibility;
- `PositionStabilizer` behavior;
- do not cancel or replace SPR `calculateOcclusion()`;
- no worker-thread SPR world/geometry raycasts;
- strict source lifetime identity/generation semantics;
- scheduling must not intentionally alter PCM sample position, OpenAL playback clock, buffer offset or synchronized-start timing;
- Hotfix3 100 ms partial sync grace and pending-INITIAL protection;
- Beta10 exact direct reuse and bit-identical OpenAL write suppression;
- Beta11 same-clone room-ray cache scope.

## Phase 5 — NOT STARTED

Phase 4 closure does **not** imply runtime validation.

Not performed as part of Phase 4:

- Minecraft launch/runtime test;
- runtime Mixin application validation;
- real CC:HQ + SPR playback test;
- OpenAL/EFX runtime validation;
- synchronized-start measurement;
- runtime room-cache/telemetry validation;
- source/release handover.

Those remain Phase 5 work and must not begin unless explicitly requested.

## Exact next prerequisite

**Stop here. Phase 4 is closed.**

The next canonical phase would be Phase 5 runtime validation/source handover, but it is **NOT STARTED** and is outside the current request.
