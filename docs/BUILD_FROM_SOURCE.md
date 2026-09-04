# Reproducible source build

This document describes the canonical source build for CC:HQ Sound Physics Compat after the Beta11 Hotfix3 source reconstruction.

## Supported build/runtime baseline

- Java 21
- Minecraft 1.21.1
- NeoForge 21.1.248
- Gradle wrapper 9.2.1
- ModDevGradle 2.0.144
- CC:Tweaked 1.120.2
- Sound Physics Remastered 1.21.1-1.5.1, pinned by Modrinth version ID `Dd2tmpsk`
- CC:HQ Speakers tested Modrinth artifact pinned by project/version IDs `ygA78R8l` / `u5PEI5Ax`
- Cloth Config 15.0.140 is used by the optional config GUI

The runtime HQ Speakers JAR reports internal mod version `1.1.4-neoforge-1.21.1` even though the pinned Modrinth file is the 1.0.1 NeoForge/1.21.1 artifact. The immutable Modrinth IDs are the build authority.

## Prerequisites

1. Check out the intended maintained/final candidate branch.
2. Install a Java 21 JDK, or allow Gradle toolchains to resolve Java 21.
3. Allow Gradle access to Maven Central, NeoForged Maven, SquidDev Maven, Modrinth Maven and CurseMaven for the pinned dependencies.

No historical Hotfix3 JAR is required to compile the maintained source.

## Canonical clean build

Linux/macOS:

```text
./gradlew --no-configuration-cache clean jar
```

Windows:

```text
gradlew.bat --no-configuration-cache clean jar
```

The built mod JAR is written under:

```text
build/libs/
```

For the final Phase-5 feature-test candidate the expected filename contains:

```text
phase5-final-test
```

## Why SPR is preprocessed for javac

The authoritative Hotfix3 source behavior directly calls Sound Physics Remastered members that are private in the published SPR JAR but widened at runtime by this compat mod's access transformer.

The build therefore deliberately uses two SPR views:

- **runtime:** the untouched pinned SPR JAR;
- **compile time:** an isolated copy transformed with this mod's exact `META-INF/accesstransformer.cfg`.

The `prepareSprCompileJar` task creates:

```text
build/reconstruction/compile/sound-physics-remastered-at.jar
```

Java compilation depends on that task automatically.

The build-only transformer is:

```text
net.neoforged.accesstransformers:at-cli:10.0.6:fatjar
```

AT CLI 10.0.6 is intentionally used because the older loader-era 10.0.1 tool cannot process Java-21 / class-major-65 bytecode. The transformer is never packaged into the mod runtime.

## Useful verification commands

Resolve and verify the reconstruction compile contract:

```text
./gradlew --no-configuration-cache verifyReconstructionClasspath
```

Verify resource, Mixin and access-transformer wiring:

```text
./gradlew --no-configuration-cache verifyResourceWiring
```

Clean-compile without packaging:

```text
./gradlew --no-configuration-cache clean compileJava
```

Full clean JAR build:

```text
./gradlew --no-configuration-cache clean jar
```

## Build invariants

A valid maintained build must retain all of the following unless a future version explicitly changes the design:

- Java 21 source/target environment;
- untouched pinned SPR artifact at runtime;
- access-transformed SPR copy only for javac;
- raw upstream SPR must not replace the transformed copy on `compileClasspath`;
- exact compat access transformer packaged and registered;
- compat Mixin config packaged and registered;
- exact source-owned manifest wiring through the Gradle `jar` task;
- client-only mod registration;
- no requirement for the historical decompiled/reconstruction workspace to build the maintained source.

## Configuration files generated at runtime

The maintained extended source can create three client configs:

```text
cchq_soundphysics_compat-client.toml
cchq_soundphysics_compat-advanced.toml
cchq_soundphysics_compat-mixing.toml
```

The first file contains the normal acoustic/user controls. The advanced file contains scheduler/cache/debug controls whose defaults preserve Hotfix3 behavior. The mixing file contains the optional synchronized multi-speaker occlusion-suppression feature and defaults to OFF.

## Phase-4 parity reference

The frozen Hotfix3-equivalent reconstruction remains available independently at:

- `phase4-hotfix3-parity`
- `archive-phase4-hotfix3-parity`

Both anchor commit:

```text
79eed29767343ee34022e8f6268b386f75e84c9f
```

The final audited Phase-4 code/build commit inside that history is:

```text
98e7dedb7ecf6fda22008b084b6bb41956edff78
```

Do not modify those branches when developing later versions.
