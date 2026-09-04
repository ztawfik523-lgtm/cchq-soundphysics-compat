# Phase 2 build-project audit

Phase 2 started after Phase 1 was closed with byte-for-byte resource verification.

## Initial findings

The pre-existing reconstruction build was useful, but it was not yet trustworthy enough to call reproducible:

1. Java 21, Minecraft 1.21.1, NeoForge 21.1.248, ModDevGradle 2.0.144, SPR and CC:Tweaked were already represented.
2. The CC:HQ dependency was wrong for the tested baseline: the build referenced a local `hqspeaker-1.0.1-Neoforge1.21.1.jar`, while the runtime baseline uses CC:HQ Speakers 1.1.4 on NeoForge / Minecraft 1.21.1.
3. The repository did not have a build-validation workflow for the reconstruction classpath.
4. The repository still lacks a complete Gradle wrapper; Phase 2 will add one before closure.
5. Java source reconstruction is incomplete by design and belongs to Phase 3, so build-system validation must not confuse missing source with dependency-resolution failure.

## Changes made at Phase 2 start

- centralised pinned dependency versions in `gradle.properties`;
- kept NeoForge at `21.1.248` and Java at 21;
- kept SPR on the exact Modrinth version ID already associated with the tested baseline;
- kept CC:Tweaked at `1.120.2`;
- replaced the obsolete local HQ 1.0.1 dependency with a Modrinth Maven selector for CC:HQ Speakers 1.1.4, filtered to NeoForge / Minecraft 1.21.1;
- added `verifyReconstructionClasspath`, which resolves the complete compile classpath without requiring incomplete Phase 3 sources to compile;
- added `.github/workflows/phase2-classpath.yml` using Java 21 and pinned Gradle 9.2.1.

## What must still be proven before Phase 2 is complete

- the HQ 1.1.4 Modrinth selector must resolve to the intended NeoForge / 1.21.1 artifact;
- all pinned dependencies must resolve together under ModDevGradle 2.0.144;
- the Gradle project model must configure cleanly;
- a complete Gradle wrapper (`gradlew`, `gradlew.bat`, wrapper properties and wrapper JAR) must be committed;
- the project must reach Java compilation with dependency/classpath errors eliminated; source errors caused by still-missing Phase 3 classes are acceptable at this phase boundary only if they are clearly distinguished from build-system errors.

Phase 2 is **in progress**, not complete.
