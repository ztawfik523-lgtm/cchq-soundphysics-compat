# Phase 2 build-project audit — COMPLETE / JAR-RECHECKED

Authoritative runtime baseline: `cchq_soundphysics_compat-0.1.0-beta11-hotfix3.jar`

SHA-256: `83500f182fc9829aa1a5a51fbfa11ba6cdfb645699b25d1c445167666dabc1ef`

Phase 2 follows the canonical five-phase plan in `docs/RECONSTRUCTION_PHASES.md`: reconstruct the Java 21 / NeoForge build project and prove that it resolves the expected dependency graph, processes the exact runtime metadata, applies the required compile-time access contract, and reaches javac. Java source completeness belongs to Phase 3.

## Pinned environment

- Java 21
- Minecraft 1.21.1
- NeoForge 21.1.248
- ModDevGradle 2.0.144
- Gradle wrapper 9.2.1
- CC:Tweaked 1.120.2
- Sound Physics Remastered: immutable Modrinth version resolving as `qyVF9oeo-Dd2tmpsk.jar`
- tested CC:HQ Speakers artifact: immutable Modrinth coordinates resolving as `ygA78R8l-u5PEI5Ax.jar`
- Cloth Config for the optional config UI

The HQ artifact is intentionally pinned by immutable Modrinth IDs because its public artifact identity and internal reported mod version do not map cleanly enough to derive coordinates safely.

## Original Phase 2 closure

The original definitive finish gate was GitHub Actions run `33856858450`. It established that the wrapper, dependency graph, NeoForge development artifact pipeline, processed resources, mixin registration and access-transformer registration were all functional and that compilation reached javac.

The complete Gradle wrapper was committed in `58735fa6419ff439dc8532ea3ac98ad75e117501`. GitHub wrapper validation accepts the wrapper JAR; Gradle is pinned to 9.2.1.

## Why the exact Hotfix3 JAR required a Phase 2 recheck

Once the exact Hotfix3 JAR was available for direct source reconstruction, reconstructed methods began calling SPR members exactly as Hotfix3 does. This exposed a distinction that the earlier incomplete-source compile probe could not test:

- Hotfix3 directly calls SPR methods such as `SoundPhysics.runOcclusion(Vec3, Vec3)`;
- those members are private in the published SPR JAR;
- Hotfix3's `META-INF/accesstransformer.cfg` widens them at runtime;
- javac must also compile against a dependency view in which those members have already been access-transformed.

The previous build correctly packaged and registered the AT for runtime, but compileClasspath still exposed the untouched SPR JAR. With exact reconstructed source this produced `has private access in SoundPhysics` errors. That was a genuine build-project reconstruction gap, not a Phase 3 semantic issue.

## Compile-time SPR access-transformer preprocessing

`build.gradle` now separates the SPR runtime artifact from the compile artifact.

Runtime contract:

- the tested upstream `qyVF9oeo-Dd2tmpsk.jar` remains untouched;
- runtime behavior continues to rely on the mod's exact Hotfix3 access transformer;
- no modified SPR JAR is bundled as a runtime replacement.

Compile contract:

- `sprCompileInput` resolves the exact upstream SPR artifact in isolation;
- `prepareSprCompileJar` applies `src/main/resources/META-INF/accesstransformer.cfg` to an isolated copy;
- output is `build/reconstruction/compile/sound-physics-remastered-at.jar`;
- javac receives that transformed copy through `compileOnly`;
- `verifyReconstructionClasspath` explicitly fails if raw `qyVF9oeo-Dd2tmpsk.jar` leaks onto compileClasspath.

The first attempted standalone processor, AT CLI 10.0.1, failed with:

`Unsupported class file major version 65`

because its bundled ASM generation could not parse Java-21 classfiles. This was corrected by using AT CLI 10.0.6 as a build-only preprocessing tool. Runtime dependency versions were not changed.

Relevant build commits:

- `b004051e2786d59e56649c192f8537f1396b7d95` — add isolated SPR AT compile preprocessing;
- `cef50d04fbb03b4f523961aeb95f2f0377856994` — use Java-21-capable AT CLI 10.0.6.

## Final JAR-backed Phase 2 recheck

### Classpath gate — PASS

GitHub Actions run `33864425672` completed successfully on commit `cef50d04fbb03b4f523961aeb95f2f0377856994`.

It proved:

- `prepareSprCompileJar` successfully transforms the exact SPR artifact with the exact Hotfix3 AT;
- compileClasspath resolves **90 files**;
- `sound-physics-remastered-at.jar` is present;
- raw `qyVF9oeo-Dd2tmpsk.jar` is absent from compileClasspath;
- the Gradle project model remains valid.

### Full finish gate — PASS

GitHub Actions run `33864425687` completed successfully on the same commit.

All finish-gate stages passed:

1. wrapper generation/validation;
2. Java 21 / Gradle 9.2.1 validation;
3. reconstruction classpath resolution through the committed wrapper;
4. isolated SPR AT preprocessing;
5. NeoForge `createMinecraftArtifacts` pipeline;
6. `processResources` + `verifyResourceWiring`;
7. javac boundary classification.

Resource output remains:

`Resource wiring verified: 4 processed resources, 11 client mixins, access transformer registered.`

### Current javac boundary

After the AT compile fix, the previous private-access errors disappear completely.

The current compile probe reports **17 errors**, all `cannot find symbol` references to the still-unreconstructed `SoundPhysicsBridge` class from already-present sources including:

- `Beta9Optimizer`
- `Beta10Optimizer`
- `RoomSchedulerClient`
- `EnvironmentSmoother`
- `Beta11RoomRayCache`
- `CompatAudioManager`

No current compile error is caused by unresolved external dependencies, missing plugins, NeoForge artifact generation, AT preprocessing, raw-SPR accessibility, or resource/mixin wiring.

## Phase 2 exit decision

Canonical exit criterion:

> Gradle resolves the project and reaches Java compilation with the expected dependency classpath.

The JAR-backed recheck strengthens that criterion: javac now sees the same SPR accessibility contract required by exact Hotfix3 source.

**Phase 2 is COMPLETE / JAR-RECHECKED.**

The remaining 17 compile errors are wholly inside the Phase 3 source boundary. Continue with `SoundPhysicsBridge`, then `ClothConfigScreen`; do not begin Beta11.1 optimization work.
