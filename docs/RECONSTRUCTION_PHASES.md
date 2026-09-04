# Beta11 Hotfix3 source reconstruction — five phases

The authoritative runtime baseline is `cchq_soundphysics_compat-0.1.0-beta11-hotfix3.jar` with SHA-256 `83500f182fc9829aa1a5a51fbfa11ba6cdfb645699b25d1c445167666dabc1ef`.

The goal is a complete, rebuildable source project whose behavior matches the tested Hotfix3 baseline closely enough for future development to move entirely to source-level changes. Byte-for-byte compiler identity is not required, but behaviorally meaningful differences must be understood and justified.

## Current status — 2026-09-04

- Phase 1 — **COMPLETE / JAR-RECHECKED**
- Phase 2 — **COMPLETE / JAR-RECHECKED**
- Phase 3 — **IN PROGRESS**
- Phase 4 — not started
- Phase 5 — not started

The exact Hotfix3 JAR has been re-supplied and verified. The only known remaining top-level authored Phase 3 source gaps are `SoundPhysicsBridge` and `ClothConfigScreen`.

## Phase 1 — Freeze and inventory the binary baseline

- verify the authoritative JAR hash;
- inventory every class and resource in the JAR;
- extract exact non-class resources;
- record package/class topology and important nested classes;
- record mod metadata, mixin list, access transformer and language resource exactly;
- retain per-entry fingerprints so nothing can disappear during reconstruction.

**Exit criterion:** the repository has a complete manifest of what exists in Hotfix3 and exact copies of all source-relevant resources.

**Current result:** satisfied and independently rechecked from the exact JAR. See `docs/baseline/PHASE1_JAR_RECHECK_2026-09-04.md`.

## Phase 2 — Reconstruct the build project

- recreate Gradle/NeoForge project structure for Java 21 / Minecraft 1.21.1;
- add required compile/runtime dependencies for NeoForge, CC:Tweaked, CC:HQ Speakers, SPR and optional Cloth Config;
- place exact resources from Phase 1 under `src/main/resources`;
- reproduce mod id/version/mixin/access-transformer wiring;
- create deterministic local build/CI commands;
- ensure javac sees any dependency access widening required by exact Hotfix3 source.

Important Hotfix3-specific compile contract:

- runtime uses the untouched tested SPR JAR;
- javac uses an isolated SPR copy preprocessed with this mod's exact access transformer;
- the raw private-member SPR artifact must not leak onto compileClasspath.

**Exit criterion:** Gradle resolves the project and reaches Java compilation with the expected dependency classpath and accessibility contract.

**Current result:** satisfied. JAR-backed recheck runs `33864425672` and `33864425687` are successful; current javac errors are only Phase 3 references to missing `SoundPhysicsBridge`.

## Phase 3 — Reconstruct every Java class

- decompile/inspect every Hotfix3 class as needed;
- clean decompiler artifacts into maintainable Java;
- restore annotations, mixin descriptors, nested classes, constants and method signatures;
- manually reconstruct hand-patched bytecode as normal verifier-safe Java;
- preserve all frozen acoustic, OpenAL, lifecycle, sync and scheduler invariants;
- account for compiler-generated nested/synthetic output intentionally.

**Exit criterion:** every meaningful class in the binary inventory has an intentional source/compiler-generated origin and the full Java project compiles.

**Current result:** in progress. `SoundPhysicsBridge` is the principal runtime/compile blocker; `ClothConfigScreen` also remains to be reconstructed before closure.

## Phase 4 — Structural and behavioral equivalence audit

- compare class/method inventory and descriptors between reconstructed build and Hotfix3;
- audit constants, mixin targets, OpenAL calls, EFX reattachment semantics, scheduler thresholds, sync grace logic, caches and distance/occlusion formulas;
- compare nested-class topology and important annotations;
- run standalone decode/downmix/cache/sync tests where useful;
- fix discrepancies until no unexplained behaviorally meaningful difference remains.

**Exit criterion:** no unexplained behaviorally meaningful difference remains in the source-level/compiled-output comparison.

## Phase 5 — Runtime validation and source handover

- launch the reconstructed build in the lightweight Minecraft test instance;
- run startup, single-speaker, multi-speaker, 11-speaker sync, partial/incomplete sync, stop/restart, movement, doorway, camera-only and lifecycle tests;
- compare logs and acoustic behavior against known Hotfix3 baseline behavior;
- mark/tag the reconstructed baseline only after it passes;
- create the Beta11.1/B development branch from that validated source baseline.

**Exit criterion:** GitHub source becomes the authoritative development base; Hotfix3 remains only the historical binary reference.

## Rule

Do not start Beta11.1/B optimization during any of these phases. Do not merge `beta11-source-reconstruction` into `main` solely because compilation succeeds.
