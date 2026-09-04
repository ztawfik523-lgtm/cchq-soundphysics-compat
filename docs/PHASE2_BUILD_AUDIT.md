# Phase 2 build-project audit — COMPLETE

Authoritative runtime baseline: `cchq_soundphysics_compat-0.1.0-beta11-hotfix3.jar`

SHA-256: `83500f182fc9829aa1a5a51fbfa11ba6cdfb645699b25d1c445167666dabc1ef`

Phase 2 follows the canonical five-phase plan in `docs/RECONSTRUCTION_PHASES.md`: reconstruct the Gradle/NeoForge build project and prove that it resolves the expected dependency classpath and reaches Java compilation. Java source completeness belongs to Phase 3.

## Pinned build environment

- Java 21
- Minecraft 1.21.1
- NeoForge 21.1.248
- ModDevGradle 2.0.144
- Gradle wrapper 9.2.1
- CC:Tweaked 1.120.2
- Sound Physics Remastered pinned by immutable Modrinth version ID and resolving as `qyVF9oeo-Dd2tmpsk.jar`
- tested CC:HQ Speakers artifact pinned by immutable Modrinth coordinates `maven.modrinth:ygA78R8l:u5PEI5Ax`, resolving as `ygA78R8l-u5PEI5Ax.jar`
- Cloth Config present for the optional config UI path

The HQ dependency identity is intentionally pinned by immutable Modrinth IDs. The tested runtime reports internal mod version `1.1.4-neoforge-1.21.1`, while the actual tested file corresponds to the public Modrinth 1.0.1 artifact. Deriving Maven coordinates from the internal version string was therefore rejected.

## Build-project reconstruction completed

Phase 2 added or verified:

- Java 21 / NeoForge 1.21.1 ModDevGradle project structure;
- exact Phase 1 runtime resources under `src/main/resources`;
- mod id/version wiring;
- mixin registration;
- access-transformer registration;
- deterministic dependency coordinates;
- `verifyReconstructionClasspath` Gradle audit task;
- `verifyResourceWiring` Gradle audit task;
- CI workflows for classpath/build-project verification;
- a complete committed Gradle wrapper:
  - `gradlew`
  - `gradlew.bat`
  - `gradle/wrapper/gradle-wrapper.properties`
  - `gradle/wrapper/gradle-wrapper.jar`

The wrapper was generated and committed by CI in commit `58735fa6419ff439dc8532ea3ac98ad75e117501`.

The committed wrapper is validated by GitHub's wrapper validation and uses Gradle `9.2.1`. The wrapper JAR SHA-256 reported by the validator is:

`423cb469ccc0ecc31f0e4e1c309976198ccb734cdcbb7029d4bda0f18f57e8d9`

## Definitive Phase 2 finish gate

The definitive successful finish gate is GitHub Actions run:

`33856858450`

on reconstruction commit:

`ebeb11e3d8e9b665a5709282ffe4d1a24954c30e`

Every Phase 2 gate step completed successfully.

### 1. Wrapper/toolchain validation — PASS

`./gradlew --version` reported Gradle 9.2.1 under Java 21, and `gradle-wrapper.properties` points to:

`https://services.gradle.org/distributions/gradle-9.2.1-bin.zip`

### 2. Dependency/classpath resolution — PASS

`./gradlew verifyReconstructionClasspath` resolved the compile classpath successfully with **90 files**.

Important resolved artifacts include:

- `neoforge-21.1.248.jar`
- `cc-tweaked-1.21.1-common-api-1.120.2.jar`
- `cc-tweaked-1.21.1-core-api-1.120.2.jar`
- `qyVF9oeo-Dd2tmpsk.jar` — tested SPR dependency
- `ygA78R8l-u5PEI5Ax.jar` — tested HQ Speakers dependency
- Cloth Config
- LWJGL/OpenAL and the expected Minecraft/NeoForge dependency graph

No unresolved dependency remained.

### 3. NeoForge artifact pipeline / access-transformer setup — PASS

`./gradlew createMinecraftArtifacts` completed successfully through the NeoForge development pipeline, including mapping merge, client/server merge, rename, binary patch, dev transforms, and final NeoForge binary generation.

This proves the pinned Minecraft/NeoForge toolchain configures and executes rather than merely parsing `build.gradle`.

### 4. Resource/mixin/access-transformer wiring — PASS

`./gradlew verifyResourceWiring` first ran `processResources`, then verified these processed resources exist:

- `META-INF/MANIFEST.MF`
- `META-INF/neoforge.mods.toml`
- `META-INF/accesstransformer.cfg`
- `cchq_soundphysics_compat.mixins.json`

It also verified that `neoforge.mods.toml` registers both the mixin configuration and the access transformer.

Final output:

`Resource wiring verified: 4 processed resources, 11 client mixins, access transformer registered.`

Four configured mixin Java sources are still absent and were explicitly classified as Phase 3 reconstruction work:

- `HQSpeakerClientHandlerMixin`
- `HQSpeakerStopPacketMixin`
- `SoundPhysicsOcclusionMemoMixin`
- `SoundEngineLifecycleMixin`

Their absence is not a Phase 2 wiring failure because the exact mixin resource is present and registered; source reconstruction is the next canonical phase.

### 5. Java compilation boundary — PASS for Phase 2

`./gradlew compileJava` reaches the real Java compiler with the complete resolved dependency classpath.

It currently exits with source-level compilation errors because Phase 3 is incomplete. The finish run recorded **44 `cannot find symbol` errors** for not-yet-reconstructed project classes/symbols such as:

- `ClientConfig`
- `PerformanceStats`
- `SoundPhysicsBridge`
- `ProgressiveOcclusionModel`
- `PositionStabilizer`
- `Beta10Optimizer`
- `AttenuationBridge`
- `Beta9Optimizer`

Critically, the compile probe found no unresolved Maven dependency, missing external JAR, plugin-resolution, Gradle-model, or access-transformer configuration failure. The build therefore reaches exactly the Phase 2 exit boundary defined in `docs/RECONSTRUCTION_PHASES.md`.

## Useful failures caught during Phase 2

Phase 2 did not simply make CI green by bypassing errors. Earlier runs exposed and corrected:

- a configuration-cache incompatibility in the custom classpath audit task;
- an invalid attempt to derive a Modrinth Maven coordinate from HQ's internal `1.1.4-neoforge-1.21.1` mod version;
- an initially over-strict resource audit that incorrectly treated missing Phase 3 mixin Java sources as Phase 2 resource-wiring failures.

The final gate separates build-project correctness from Java-source completeness intentionally.

## Exit-criterion decision

Canonical Phase 2 exit criterion:

> Gradle resolves the project and reaches Java compilation with the expected dependency classpath.

That criterion is satisfied.

**Phase 2 is COMPLETE.**

The next canonical phase is **Phase 3 — reconstruct every Java class**. Do not begin Beta11.1/B optimization work until Phases 3–5 establish a validated source baseline.
