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
| Phase 3 — Reconstruct every Java class | **COMPLETE** |
| Phase 4 — Structural and behavioral equivalence audit | **NEXT / NOT STARTED** |
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

## Phase 3 — COMPLETE

Phase 3 source reconstruction is closed against the exact Hotfix3 class inventory.

Final missing authored sources were reconstructed directly from the Hotfix3 classfiles:

- `SoundPhysicsBridge.java` — commit `91d70508a04001da788ac7520e09955d5f753b09`;
- `ClothConfigScreen.java` — commit `d336bdea9d39be801360b1f286d67f29d6333772`.

A strict Phase 3 closure workflow was added in commit:

`e918e3199b98332c0320eb4cd07e34740d1ec8ec`

Definitive closure run:

`33867207760`

Result: **SUCCESS**.

The hard gate ran a clean Java 21 build and reported:

```text
compileJava: PASS
jar: PASS
Hotfix3 60-class topology: PASS
source-relevant processed resources: PASS
```

Class topology reconciliation was exact by class path:

```text
Hotfix3 expected classes: 60
Reconstructed classes:    60
```

`diff -u` between the frozen Hotfix3 class-path list and compiled source output produced no differences. Therefore no baseline compat class path is missing and no extra nested/synthetic class path was introduced.

The five source-relevant processed resources are also present:

- `META-INF/MANIFEST.MF`
- `META-INF/neoforge.mods.toml`
- `META-INF/accesstransformer.cfg`
- `cchq_soundphysics_compat.mixins.json`
- `assets/cchq_soundphysics_compat/lang/en_us.json`

Important `SoundPhysicsBridge` source topology now intentionally produces:

- `SoundPhysicsBridge$Candidate`
- `SoundPhysicsBridge$RoomEnvironmentAccess`
- `SoundPhysicsBridge$RoomEnvironmentAccess$ConfigStamp`
- `SoundPhysicsBridge$RoomStamp`
- `SoundPhysicsBridge$SourceState`

The classfile also confirmed the stable HQ sound identity path:

`cchq_soundphysics_compat:hq_speaker/<speaker UUID without dashes>`

`ClothConfigScreen` was reconstructed with the tested binary's UI strings/defaults/ranges, including title `CC:HQ × Sound Physics` and the interval separator `•`.

Full Phase 3 evidence:

`docs/PHASE3_FINAL_VERIFICATION.md`

## Phase 3 completion boundary

Phase 3 completion means:

- every meaningful Hotfix3 class path has an intentional source/compiler-generated origin;
- the full project compiles;
- the reconstructed build emits exactly the 60 baseline compat class paths;
- all source-relevant resources are present in processed output.

It does **not** mean structural/behavioral equivalence has already been proven method-by-method, and it does not mean the reconstructed build has passed runtime validation. Those are Phases 4 and 5.

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

Begin **Phase 4 only**: structural and behavioral equivalence audit against the exact Hotfix3 JAR.

Do not start Phase 5 runtime claims or Beta11.1/B optimization until Phase 4 closes.
