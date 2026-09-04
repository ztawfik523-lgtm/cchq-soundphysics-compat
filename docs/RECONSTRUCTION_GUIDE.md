# Beta11 Hotfix3 Reconstruction Guide

This is the durable operating guide for reconstructing the tested **CC:HQ Sound Physics Compat Beta11 Hotfix3** source tree.

## 1. Authority and branch

Authoritative artifact:

`cchq_soundphysics_compat-0.1.0-beta11-hotfix3.jar`

SHA-256:

`83500f182fc9829aa1a5a51fbfa11ba6cdfb645699b25d1c445167666dabc1ef`

Repository:

`ztawfik523-lgtm/cchq-soundphysics-compat`

Reconstruction branch:

`beta11-source-reconstruction`

The exact JAR was re-supplied and independently verified on 2026-09-04. It is the source of truth through Phase 5.

The goal is not character-for-character recovery of lost Java. The goal is a complete, readable, rebuildable source project whose behavior matches Hotfix3 closely enough to become the safe development base for later work.

## 2. Canonical phases

Use only the five-phase plan in `docs/RECONSTRUCTION_PHASES.md`:

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

The older seven-pass/ad-hoc sequence previously written in this guide is obsolete.

## 3. Target environment

- Minecraft 1.21.1
- NeoForge 21.1.248
- Java 21
- ModDevGradle 2.0.144
- Gradle 9.2.1
- CC:Tweaked 1.120.2
- CC:HQ Speakers tested artifact resolving as `ygA78R8l-u5PEI5Ax.jar`
- Sound Physics Remastered 1.21.1-1.5.1 resolving as `qyVF9oeo-Dd2tmpsk.jar`
- optional Cloth Config UI
- compat is client-only

## 4. Reconstruction rules

Every working session must:

1. read `RECONSTRUCTION_STATUS.md` and `docs/BETA11_RECONSTRUCTION_HANDOFF.md` first;
2. inspect current branch/CI state;
3. treat Hotfix3 bytecode/decompile as authoritative;
4. stay inside the current canonical phase;
5. do not optimize, simplify, redesign, or silently "improve" behavior;
6. preserve descriptors, annotations, mixin targets, constants, OpenAL ordering, lifecycle ordering and scheduler/cache semantics;
7. record uncertainty rather than guessing;
8. commit coherent source/build/documentation changes;
9. update affected documentation before ending the run;
10. do not merge to `main` or start Beta11.1/B until Phase 5 closes.

## 5. Evidence precedence

Use evidence in this order:

1. exact Hotfix3 class bytecode/decompile;
2. exact Hotfix3 runtime Mixin metadata/descriptors;
3. already-audited reconstructed source and local call sites;
4. version-matched upstream dependency source/signatures;
5. historical handoffs only as supporting architectural context.

Never invent large runtime bodies from prose merely to remove compiler errors.

## 6. Phase 1 baseline

The exact-JAR recheck confirms:

- 75 ZIP entries;
- 10 directories;
- 65 non-directory files;
- 60 Java-21 classfiles;
- all 65 per-entry SHA-256s match `docs/baseline/HOTFIX3_SHA256SUMS.txt`;
- all five source-relevant resources match exact JAR bytes;
- manifest CRLF form is exact.

See:

- `docs/baseline/BETA11_HOTFIX3_INVENTORY.md`
- `docs/baseline/PHASE1_FINAL_VERIFICATION.md`
- `docs/baseline/PHASE1_JAR_RECHECK_2026-09-04.md`

## 7. Phase 2 build contract

The exact Hotfix3 source directly invokes SPR members widened by this mod's access transformer. Therefore runtime AT registration alone is insufficient for source compilation.

Current build contract:

- runtime keeps the untouched tested SPR JAR;
- `prepareSprCompileJar` transforms an isolated compile-only SPR copy with the exact Hotfix3 AT;
- javac uses `sound-physics-remastered-at.jar`;
- raw SPR is forbidden from compileClasspath;
- AT CLI 10.0.6 is used only as the Java-21-capable preprocessing tool.

Current Phase 2 proof:

- classpath run `33864425672`: success;
- full finish-gate run `33864425687`: success;
- commit `cef50d04fbb03b4f523961aeb95f2f0377856994`;
- compileClasspath: 90 files;
- NeoForge artifact pipeline passes;
- resource/mixin/AT wiring passes;
- current javac private-access errors are gone.

The current compile probe has 17 errors, all caused by missing `SoundPhysicsBridge` references. That is a Phase 3 source boundary, not a build-system failure.

## 8. Frozen runtime/acoustic invariants

### Playback / decode

- No Lua changes.
- Decode stays off-thread.
- Stereo/multichannel audio remains downmixed to mono PCM for positional OpenAL playback.
- Shared OpenAL buffer/refcount semantics remain intact.
- Physics scheduling must never change PCM sample position, OpenAL playback clock, buffer offset or sync timing.
- Preserve strict source lifetime identity/generation semantics.

### Synchronization

- Full synchronized groups use one `AL10.alSourcePlayv(int[])`.
- `PARTIAL_FLUSH_NS = 100_000_000L`.
- `STALE_GROUP_NS = 5_000_000_000L`.
- Pending `AL_INITIAL` sources are protected during partial-group grace.
- After grace, all arrived sources in that group start together.

### Direct occlusion

- 17 conceptual paths: center + 8 inner + 8 outer.
- Full refresh uses all 17.
- Progressive refresh alternates center+inner and center+outer 9-path work while reusing the opposite ring.
- Preserve exact Hotfix3 weights, ring scales and invalidation thresholds from reconstructed source.
- Beta9/Beta10 exact direct reuse remains part of the baseline.

### SPR integration

- Do not cancel/replace SPR `calculateOcclusion()`.
- `SoundPhysicsOcclusionMemoMixin` redirects SPR's internal `runOcclusion(...)` invocation inside `calculateOcclusion(...)` rather than replacing the whole method.
- No worker-thread SPR world/geometry raycasts.
- Safe-clone/world access rules must remain as Hotfix3 implements them.

### Room/bounce cache

- Beta11 room cache applies only to the intended source-centered environment/bounce raycast callsites in SPR `evaluateEnvironment`.
- Exact same-clone reuse only.
- Preserve current/previous bank behavior and `BlockGetter` identity scope.
- Shared-airspace/listener-dependent work remains live.
- Cross-clone room reuse remains telemetry-only in Hotfix3.

### EFX

- Private per-source EFX isolation is required.
- **Every actual environment application must reattach direct/aux EFX.**
- Parameter-write suppression is allowed; attachment suppression is not.
- Do not create private EFX while the source is `AL_INITIAL`.
- Native SPR environment fallback remains available if isolated EFX fails.

### Position / distance

- Preserve `PositionStabilizer` semantics.
- Preserve approved `SoundSource.BLOCKS` distance behavior.
- Preserve Hotfix3 reflected/apparent-position restoration/stabilization behavior.

## 9. Important historical failures to avoid

### Original Beta11 verifier failure

A manually patched `Beta10Optimizer.beta11RoomCacheActive()` lacked a required stack-map frame and caused a `VerifyError`. Reconstructed source must express the working logic as normal Java so javac emits verifier-safe frames.

Working semantics:

```java
Context context = CONTEXT.get();
if (context != null && context.owner == OWNER_SPR) return context.cacheable;
return false;
```

### Incomplete sync-group no-sound bug

A declared group size could exceed the number of sources that actually arrived. Without Hotfix3 grace handling, sources remained `AL_INITIAL` and were later destroyed. Preserve the 100 ms partial flush and pending-initial protection.

### Alpha13 direct-occlusion replacement

Do not replace/cancel `calculateOcclusion()`. That strategy caused severe sound-thread stalls after geometry changes.

### Beta2 EFX attach-once

Do not suppress EFX reattachment because parameters are unchanged. That broke muffling.

## 10. Current Phase 3 source state

Most top-level Java source has been reconstructed, including:

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
- all currently configured mixin/accessor source counterparts

The two known top-level authored gaps are:

1. `SoundPhysicsBridge`
2. `ClothConfigScreen`

`SoundPhysicsBridge` is the current compile blocker.

Required nested topology:

- `SoundPhysicsBridge$Candidate`
- `SoundPhysicsBridge$RoomEnvironmentAccess`
- `SoundPhysicsBridge$RoomEnvironmentAccess$ConfigStamp`
- `SoundPhysicsBridge$RoomStamp`
- `SoundPhysicsBridge$SourceState`

Do not add a compile-only stub. Recover from exact Hotfix3 classfile/decompile:

- registration/unregistration/state ownership;
- `available()`;
- `apply(...)`;
- `schedulerTick()`;
- Beta9 capture-stamp/logging seams;
- room/config stamps;
- stationary exact reuse;
- clearing/sentinel transition path;
- candidate urgency/fairness scheduling;
- room environment capture/application;
- exact scheduler timing and age thresholds;
- integration with Beta9/Beta10/ProgressiveOcclusionModel/EnvironmentSmoother/Beta11RoomRayCache.

Then reconstruct `ClothConfigScreen` and reconcile every Phase 1 class/nested class against authored or compiler-generated source before closing Phase 3.

## 11. Definition of Phase 3 complete

Phase 3 closes only when:

- every meaningful Hotfix3 class has an intentional source counterpart or documented compiler-generated origin;
- nested classes/constants/descriptors/annotations/mixin targets are intentional;
- hand-patched bytecode semantics are represented as verifier-safe Java;
- `SoundPhysicsBridge` and `ClothConfigScreen` are reconstructed;
- full Java compilation succeeds.

A green compile does not make the reconstruction authoritative; Phase 4 and Phase 5 still remain.

## 12. Phase 4 expectations

No feature work. Compare reconstructed output against Hotfix3 for:

- class/method inventory and descriptors;
- nested-class topology;
- constants and thresholds;
- mixin targets/descriptors/ordinals/require values;
- config defaults;
- OpenAL call ordering/ownership;
- sync grace/lifecycle behavior;
- EFX reattachment semantics;
- distance/direct/room formulas;
- cache sizes/probe counts/ages;
- scheduler fairness/age ceilings/stamps/sentinel transitions;
- stop/reload teardown.

Fix only baseline discrepancies.

## 13. Phase 5 expectations

Run the reconstructed mod in the lightweight test environment and validate at least:

- startup;
- one speaker;
- multi-speaker playback;
- full synchronized group;
- partial/incomplete synchronized group;
- stop/restart;
- movement and doorway transitions;
- camera-only movement;
- pause/resume/stopAll/reload/destroy lifecycle;
- no VerifyError/mixin target/OpenAL/EFX failure;
- logs/acoustics consistent with known Hotfix3 behavior.

Only after Phase 5 passes should source become authoritative and a Beta11.1/B development branch be created.

## 14. Future roadmap — not for reconstruction

After validated source handover:

- Beta11.1/B: exact decode/cache/OpenAL/allocation/diagnostic cleanup;
- Beta12/C1: persistent progressive room state;
- Beta12.x/C2: acoustic work scheduler;
- Beta13/D: sparse adaptive room-position memory;
- adaptive ray/bounce quality reduction remains shelved;
- HQ enhanced/music spatial mode remains optional backlog.

## 15. Exact next prerequisite

Reconstruct `SoundPhysicsBridge` from the exact Hotfix3 classfile/decompile, rerun the compile gate, then reconstruct `ClothConfigScreen` and close the Phase 3 inventory deliberately.
