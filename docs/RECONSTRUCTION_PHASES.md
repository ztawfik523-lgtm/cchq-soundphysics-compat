# Beta11 Hotfix3 source reconstruction — five phases

The authoritative runtime baseline is `cchq_soundphysics_compat-0.1.0-beta11-hotfix3.jar` with SHA-256 `83500f182fc9829aa1a5a51fbfa11ba6cdfb645699b25d1c445167666dabc1ef`.

The goal is a complete, rebuildable source project whose behavior matches the tested Hotfix3 baseline closely enough for future development to move entirely to source-level changes. Byte-for-byte class identity is not required.

## Phase 1 — Freeze and inventory the binary baseline

- verify the authoritative JAR hash;
- inventory every class and resource in the JAR;
- extract exact non-class resources;
- record package/class topology and important nested classes;
- record the mod metadata, mixin list, access transformer, and language resource exactly;
- establish a checklist so nothing can disappear during reconstruction.

**Exit criterion:** the repository has a complete manifest of what exists in Hotfix3 and exact copies of all source-relevant resources.

## Phase 2 — Reconstruct the build project

- recreate Gradle/NeoForge project structure for Java 21 / Minecraft 1.21.1;
- add the required compile/runtime dependencies for NeoForge, CC:Tweaked, CC:HQ Speakers, SPR, and optional Cloth Config;
- place the exact resources from Phase 1 under `src/main/resources`;
- reproduce mod id/version/mixin/access-transformer wiring;
- create a deterministic local build command and CI workflow.

**Exit criterion:** Gradle resolves the project and reaches Java compilation with the expected dependency classpath.

## Phase 3 — Reconstruct every Java class

- decompile every Hotfix3 class as a starting point;
- clean decompiler artifacts into maintainable Java;
- restore annotations, mixin descriptors, nested classes, constants, and method signatures;
- manually reconstruct hand-patched bytecode as normal Java source;
- preserve all frozen acoustic, OpenAL, lifecycle, sync, and scheduler invariants.

**Exit criterion:** every class in the binary inventory has an intentional source counterpart and the project compiles.

## Phase 4 — Structural and behavioral equivalence audit

- compare class/method inventory and descriptors between reconstructed build and Hotfix3;
- audit constants, mixin targets, OpenAL calls, EFX reattachment semantics, scheduler thresholds, sync grace logic, caches, and distance/occlusion formulas;
- run standalone decode/downmix/cache/sync tests;
- fix discrepancies until the reconstructed build passes the structural audit.

**Exit criterion:** no unexplained behaviorally meaningful difference remains in the source-level comparison.

## Phase 5 — Runtime validation and source handover

- launch the reconstructed build in the lightweight Minecraft test instance;
- run startup, single-speaker, multi-speaker, 11-speaker sync, stop/restart, movement, doorway, camera-only, and lifecycle tests;
- compare logs and acoustic behavior against the known Hotfix3 baseline;
- mark/tag the reconstructed baseline only after it passes;
- create the `beta11.1-b-cleanup` development branch from that validated source baseline.

**Exit criterion:** GitHub source becomes the authoritative development base; Hotfix3 remains only the historical binary reference.
