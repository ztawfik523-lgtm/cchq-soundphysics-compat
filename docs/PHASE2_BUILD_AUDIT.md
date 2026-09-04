# Phase 2 build-project audit

Phase 2 started after Phase 1 was closed with byte-for-byte resource verification.

## Initial findings

The pre-existing reconstruction build was useful, but it was not yet trustworthy enough to call reproducible:

1. Java 21, Minecraft 1.21.1, NeoForge 21.1.248, ModDevGradle 2.0.144, SPR and CC:Tweaked were already represented.
2. The HQ dependency identity needed to be made explicit. The tested runtime reports CC:HQ Speakers internal mod version `1.1.4-neoforge-1.21.1`, while the actual user/runtime file is named `hqspeaker-1.0.1-Neoforge1.21.1(1).jar`. That filename corresponds to Modrinth project `ygA78R8l`, version `u5PEI5Ax` (the public Modrinth release labelled 1.0.1). The immutable project/version IDs are therefore the safest build identity.
3. The repository did not have a build-validation workflow for the reconstruction classpath.
4. The repository still lacks a complete Gradle wrapper; Phase 2 will add one before closure.
5. Java source reconstruction is incomplete by design and belongs to Phase 3, so build-system validation must not confuse missing source with dependency-resolution failure.

## Changes made at Phase 2 start

- centralised pinned dependency versions in `gradle.properties`;
- kept NeoForge at `21.1.248` and Java at 21;
- kept SPR on the exact Modrinth version ID already associated with the tested baseline;
- kept CC:Tweaked at `1.120.2`;
- pinned the tested HQ runtime artifact by immutable Modrinth coordinates `maven.modrinth:ygA78R8l:u5PEI5Ax` rather than attempting to construct a Maven version from the internal mod version string;
- added `verifyReconstructionClasspath`, which resolves the complete compile classpath without requiring incomplete Phase 3 sources to compile;
- added `.github/workflows/phase2-classpath.yml` using Java 21 and pinned Gradle 9.2.1.

## CI findings

Two early verification runs failed usefully:

- the first exposed a configuration-cache problem in the custom audit task;
- the next exposed that `1.1.4-neoforge,1.21.1` was not a valid Modrinth Maven version coordinate.

Both were corrected rather than bypassed.

The successful Phase 2 classpath run is GitHub Actions run `33856067169` on commit `7440e6e69eb1c277f5aca43ca6586201e9259531`.

It proved:

- Temurin Java 21 is active;
- Gradle 9.2.1 configures the project with ModDevGradle 2.0.144;
- NeoForge `21.1.248` resolves;
- CC:Tweaked `1.120.2` API resolves;
- Cloth Config resolves;
- SPR resolves as `qyVF9oeo-Dd2tmpsk.jar`;
- the exact HQ artifact resolves as `ygA78R8l-u5PEI5Ax.jar`;
- `compileClasspath` resolves successfully with 90 files;
- the Gradle project model configures and exposes the expected build/mod-development tasks.

## What must still be proven before Phase 2 is complete

- a complete Gradle wrapper (`gradlew`, `gradlew.bat`, wrapper properties and wrapper JAR) must be committed and validated;
- the project must reach Java compilation with dependency/classpath errors eliminated; source errors caused by still-missing Phase 3 classes are acceptable at this phase boundary only if they are clearly distinguished from build-system errors;
- resource/mixin/access-transformer wiring should be exercised by the Gradle tasks, not merely present as files.

Phase 2 is **in progress**, not complete.
