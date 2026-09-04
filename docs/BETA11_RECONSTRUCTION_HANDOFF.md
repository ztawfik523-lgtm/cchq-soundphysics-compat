# Beta11 Hotfix3 Reconstruction Handoff

This is the durable session handoff for `beta11-source-reconstruction`. Read this file, `RECONSTRUCTION_STATUS.md`, and `docs/RECONSTRUCTION_PHASES.md` before changing code.

## Goal

Reconstruct a complete, readable, rebuildable source-level project for the tested **Beta11 Hotfix3** compatibility mod, preserving tested runtime behavior closely enough that future development can move entirely to normal source-level changes.

Do **not** begin Beta11.1/B optimization during reconstruction, structural audit, build validation, or runtime validation.

## Authoritative baseline

Artifact:

`cchq_soundphysics_compat-0.1.0-beta11-hotfix3.jar`

SHA-256:

`83500f182fc9829aa1a5a51fbfa11ba6cdfb645699b25d1c445167666dabc1ef`

The exact Hotfix3 JAR was supplied again on 2026-09-04 and independently verified against this frozen hash. It is the behavioral authority through Phase 5.

If reconstructed/decompiled source, historical notes, or assumptions disagree with the classfile, trust the classfile and record uncertainty instead of guessing.

## Target environment

- Minecraft 1.21.1
- NeoForge 21.1.248
- Java 21
- ModDevGradle 2.0.144
- Gradle 9.2.1
- CC:Tweaked 1.120.2
- CC:HQ Speakers tested artifact resolving as `ygA78R8l-u5PEI5Ax.jar`
- Sound Physics Remastered 1.21.1-1.5.1 resolving as `qyVF9oeo-Dd2tmpsk.jar`
- client-only compatibility mod

## Branch discipline

Work only on:

`beta11-source-reconstruction`

Do not merge to `main` because compilation succeeds. Do not create the Beta11.1 cleanup branch until Phase 5 closes.

## Canonical phases

The old ad-hoc reconstruction pass numbering is obsolete. Use only the five phases in `docs/RECONSTRUCTION_PHASES.md`:

1. freeze/inventory binary baseline;
2. reconstruct build project;
3. reconstruct every Java class;
4. structural/behavioral equivalence audit;
5. runtime validation/source handover.

Current state:

- Phase 1: **COMPLETE / JAR-RECHECKED**
- Phase 2: **COMPLETE / JAR-RECHECKED**
- Phase 3: **IN PROGRESS**
- Phase 4: not started
- Phase 5: not started

## Phase 1 authority

The exact JAR recheck confirms:

- 75 ZIP entries;
- 10 directories;
- 65 non-directory files;
- 60 Java-21 classfiles;
- all 65 per-entry hashes match `docs/baseline/HOTFIX3_SHA256SUMS.txt`;
- the five exact runtime resources match the repository copies;
- manifest CRLF form is preserved.

See `docs/baseline/PHASE1_JAR_RECHECK_2026-09-04.md`.

## Phase 2 authority

The exact reconstructed Hotfix3 source requires direct calls to SPR members widened by this mod's access transformer. The build therefore performs isolated compile-time AT preprocessing:

- runtime uses untouched tested SPR;
- `prepareSprCompileJar` transforms an isolated compile copy with the exact Hotfix3 AT;
- javac uses `sound-physics-remastered-at.jar`;
- raw SPR is forbidden on compileClasspath;
- AT CLI 10.0.6 is used only as the Java-21-capable build processor.

JAR-backed Phase 2 recheck:

- classpath run `33864425672`: success;
- finish-gate run `33864425687`: success;
- commit `cef50d04fbb03b4f523961aeb95f2f0377856994`;
- 90 compile-classpath files;
- NeoForge artifact pipeline passes;
- resource/mixin/AT wiring passes;
- current javac probe has 17 errors, all due only to missing `SoundPhysicsBridge`.

See `docs/PHASE2_BUILD_AUDIT.md`.

## Current Phase 3 state

The large earlier evidence limitation is gone: the exact JAR is available for direct classfile/decompiler work.

Most top-level source is already present, including:

- `CompatAudioManager`
- `SyncStartCoordinator`
- `AcousticCapture`
- `EnvironmentSmoother`
- `AttenuationBridge`
- `PositionStabilizer`
- `ProgressiveOcclusionModel`
- `PerformanceStats`
- `Beta9Optimizer`
- `Beta10Optimizer`
- `ClientConfig`
- `ClientConfigAccess`
- all 11 configured mixin/accessor source counterparts currently expected by the mixin config

The two known top-level authored gaps are:

1. `SoundPhysicsBridge`
2. `ClothConfigScreen`

`SoundPhysicsBridge` is the current runtime/compile blocker and must not be replaced with a compile stub.

Required nested topology includes:

- `SoundPhysicsBridge$Candidate`
- `SoundPhysicsBridge$RoomEnvironmentAccess`
- `SoundPhysicsBridge$RoomEnvironmentAccess$ConfigStamp`
- `SoundPhysicsBridge$RoomStamp`
- `SoundPhysicsBridge$SourceState`

Reconstruct its scheduler, room/environment stamp system, clearing sentinel transitions, urgency/fairness selection, source lifecycle state, Beta9/Beta10 integration, room reuse semantics, and timing constants from exact Hotfix3 bytecode/decompile.

After it compiles, reconstruct `ClothConfigScreen`, then reconcile the full Phase 1 class inventory before closing Phase 3.

## Evidence precedence

Use this exact order:

1. Hotfix3 class bytecode/decompile;
2. exact Hotfix3 runtime Mixin metadata/descriptors;
3. already-audited local source/call sites;
4. version-matched upstream dependency source/signatures;
5. historical handoffs only as supporting context.

Never invent a large runtime method from prose because a compile error needs to disappear.

## Frozen runtime/acoustic invariants

Preserve all of these:

1. No Lua API changes.
2. Decode remains off-thread; positional playback remains mono PCM as in Hotfix3.
3. Shared OpenAL buffer/refcount behavior remains intact.
4. Full synchronized groups use `alSourcePlayv`.
5. Partial declared groups receive the Hotfix3 100 ms grace period.
6. Pending `AL_INITIAL` sources are protected during that grace period.
7. Approved `SoundSource.BLOCKS` distance behavior is preserved.
8. Direct occlusion remains center + 8 inner + 8 outer with Hotfix3 progressive refresh semantics.
9. Private per-source EFX isolation remains required.
10. Every real environment application must reattach direct/aux EFX; parameter suppression must never suppress attachment.
11. Do not create private EFX before PLAYING/PAUSED eligibility.
12. Preserve `PositionStabilizer` behavior.
13. Do not inject/cancel/replace SPR `calculateOcclusion()`.
14. No worker-thread SPR world/geometry raycasts.
15. Preserve strict source lifetime identity/generation semantics.
16. Physics scheduling must never alter PCM sample position, OpenAL playback clock, buffer offset, or sync timing.
17. Preserve all SoundEngine pause/resume/stop/destroy/emergencyShutdown/reload lifecycle behavior.
18. Preserve Beta10 exact direct reuse and bit-identical OpenAL write suppression.
19. Beta11 room cache remains scoped to the two source-centered environment/bounce raycasts in SPR `evaluateEnvironment`.
20. Cross-clone room reuse remains telemetry-only in Hotfix3.

## Important recovered details

### Sync Hotfix3

`SyncStartCoordinator` preserves:

- `PARTIAL_FLUSH_NS = 100_000_000L`
- `STALE_GROUP_NS = 5_000_000_000L`
- one `AL10.alSourcePlayv(int[])` for complete and expired-partial grouped starts
- pending-INITIAL lifecycle protection

### EFX

`EnvironmentSmoother` preserves the mandatory reattachment invariant and does not allocate private EFX while the source is still `AL_INITIAL`.

### Direct occlusion hook

`SoundPhysicsOcclusionMemoMixin` redirects SPR's internal `runOcclusion(...)` call inside `calculateOcclusion(...)`. It does not replace/cancel `calculateOcclusion()`.

### Verifier-safe Beta10 helper

```java
Context context = CONTEXT.get();
if (context != null && context.owner == OWNER_SPR) return context.cacheable;
return false;
```

### Build-time AT nuance

Do not remove the isolated transformed SPR compile JAR. Exact Hotfix3 source will otherwise fail javac on private SPR members even though runtime AT registration is correct.

## Reconstruction discipline

Each working session should:

1. read this handoff and the status file;
2. inspect current branch/CI state;
3. work on the current canonical phase only;
4. use exact classfile evidence before historical prose;
5. preserve frozen invariants;
6. commit coherent work;
7. update all affected reconstruction documentation;
8. stop rather than silently entering Beta11.1 optimization.

## Exact next prerequisite

Reconstruct `SoundPhysicsBridge` from the authoritative Hotfix3 classfile/decompile. Then run the compile gate, reconstruct `ClothConfigScreen`, and close the Phase 3 class inventory only if every meaningful binary class has a source/compiler-generated explanation and the full project compiles.
