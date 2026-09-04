# Beta11 Hotfix3 Reconstruction Handoff

This is the durable session handoff for `beta11-source-reconstruction`. Read this file, `RECONSTRUCTION_STATUS.md`, `docs/PHASE3_FINAL_VERIFICATION.md`, `docs/PHASE4_START_AUDIT.md`, and `docs/RECONSTRUCTION_PHASES.md` before changing code.

## Goal

Produce a complete, readable, rebuildable source-level project for the tested **Beta11 Hotfix3** compatibility mod and prove it structurally, behaviorally, and at runtime before future development moves entirely to normal source-level changes.

Do **not** begin Beta11.1/B optimization during reconstruction, structural audit, or runtime validation.

## Authoritative baseline

Artifact:

`cchq_soundphysics_compat-0.1.0-beta11-hotfix3.jar`

SHA-256:

`83500f182fc9829aa1a5a51fbfa11ba6cdfb645699b25d1c445167666dabc1ef`

The exact Hotfix3 JAR was supplied again on 2026-09-04 and independently verified against this frozen hash. It remains the behavioral authority through Phase 5.

If reconstructed/decompiled source, historical notes, or assumptions disagree with the classfile, trust the classfile and record the discrepancy instead of guessing.

## Target environment

- Minecraft 1.21.1
- NeoForge 21.1.248
- Java 21
- ModDevGradle 2.0.144
- Gradle 9.2.1
- CC:Tweaked 1.120.2
- CC:HQ Speakers tested artifact `ygA78R8l-u5PEI5Ax.jar`
- Sound Physics Remastered 1.21.1-1.5.1 artifact `qyVF9oeo-Dd2tmpsk.jar`
- client-only compat mod

## Branch discipline

Work only on:

`beta11-source-reconstruction`

Do not merge to `main` because source reconstruction/compilation passed. Do not create the Beta11.1 cleanup branch until Phase 5 closes.

## Canonical status

Use only the five phases in `docs/RECONSTRUCTION_PHASES.md`.

- Phase 1: **COMPLETE / JAR-RECHECKED**
- Phase 2: **COMPLETE / JAR-RECHECKED**
- Phase 3: **COMPLETE / RECHECKED**
- Phase 4: **IN PROGRESS**
- Phase 5: not started

## Phase 1 authority

Exact JAR recheck confirms:

- 75 ZIP entries;
- 10 directories;
- 65 non-directory files;
- 60 Java-21 classfiles;
- all 65 per-entry hashes match `docs/baseline/HOTFIX3_SHA256SUMS.txt`;
- the five exact runtime resources match the repository copies;
- manifest CRLF form is preserved.

See `docs/baseline/PHASE1_JAR_RECHECK_2026-09-04.md`.

## Phase 2 authority

The exact source requires direct calls to SPR members widened by the compat access transformer. The build therefore performs isolated compile-time AT preprocessing:

- runtime uses untouched tested SPR;
- `prepareSprCompileJar` transforms an isolated compile copy with the exact Hotfix3 AT;
- javac uses `sound-physics-remastered-at.jar`;
- raw SPR is forbidden on compileClasspath;
- AT CLI 10.0.6 is a build-only Java-21-capable processor.

JAR-backed Phase 2 evidence:

- classpath run `33864425672`: success;
- finish-gate run `33864425687`: success;
- 90 compile-classpath files;
- NeoForge artifact pipeline passes;
- resource/mixin/AT wiring passes.

See `docs/PHASE2_BUILD_AUDIT.md`.

## Phase 3 authority — COMPLETE / RECHECKED

All authored/source-bearing gaps are closed.

Final source commits:

- `SoundPhysicsBridge.java` — `91d70508a04001da788ac7520e09955d5f753b09`;
- `ClothConfigScreen.java` — `d336bdea9d39be801360b1f286d67f29d6333772`.

Original strict closure run:

`33867207760` — **SUCCESS**

Before Phase 4 started, the same hard closure gate reran against the fully documented branch head `70d37a3e6b072a6e215cecf3c4299b96e0276968`:

- run `33867785411`;
- job `101006475065`;
- result **SUCCESS**.

It again passed clean source build, exact 60/60 class-path topology, processed resources and closure summary. Phase 3 therefore remains closed under its intended source-completeness/build/topology definition.

Phase 4 is intentionally stricter and has already found field/annotation/source-shape drift that the Phase 3 topology gate could not detect.

See `docs/PHASE3_FINAL_VERIFICATION.md`.

## Phase 4 authority — IN PROGRESS

Durable start record:

`docs/PHASE4_START_AUDIT.md`

### Whole-project structural ABI layer

Added:

- `tools/class_abi.py` — commit `115375d76df09dcc9ab9f468f892a294a5810192`;
- exact 60-class baseline `docs/baseline/HOTFIX3_STRUCTURAL_ABI_SHA256.txt` — commit `7eda5a4ef95bc3cd547a5914227e304634ad0a7b`;
- `.github/workflows/phase4-structural-abi.yml` — commit `36fa51b90496bc4cac6de6fe947e4ea0bb45244b`.

The structural fingerprint includes Java major, class access/super/interfaces, field names/descriptors/access/constant values and method names/descriptors/access flags for every compat class.

The first structural CI attempts (`33869660406`, `33869841129`) failed before compilation because NeoForged's Maven endpoint returned HTTP 502 for `net.neoforged:minecraft-dependencies:1.21.1`. Do not classify those as structural mismatches; they never reached comparison.

### Exact discrepancies already corrected

1. `HQSpeakerClientHandlerMixin`
   - exact full `receive(HQSpeakerAudioPacket)V` injection descriptor restored;
   - nested `@At(..., remap=false)` restored;
   - missing Hotfix3 `private static boolean cchqphysics$reportedHook` restored with its first-entry write;
   - commit `3a3cb6c9fdb383ea72e5b2b5dce80c7a3c926987`.

2. `HQSpeakerStopPacketMixin`
   - exact full `handle(HQSpeakerStopPacket, IPayloadContext)V` descriptor restored;
   - nested `@At(..., remap=false)` restored;
   - commit `e9240528965c1fc0a31af22fb80a65b42720205e`.

3. `SyncStartCoordinator.removeSource`
   - source iteration aligned with exact Hotfix3 `GROUPS.values().iterator()` shape;
   - commit `7ac821eaa2dfe73dec7703ec1ba4d7fcf9761acc`.

### Opening checks that already match Hotfix3

No correction required so far for:

- room-ray redirect target/descriptor, `remap=false`, `require=2`;
- occlusion redirect target/descriptor, `remap=false`, `require=1`;
- exact environment/position injection descriptors;
- six sound-engine lifecycle HEAD hooks;
- 100 ms partial-sync grace / 5 s stale group and `alSourcePlayv` grouped starts;
- pending `AL_INITIAL` lifecycle protection;
- EnvironmentSmoother PLAYING/PAUSED private-EFX creation gate;
- mandatory direct/aux EFX source reattachment on every successful environment apply;
- opening `SoundPhysicsBridge` scheduler/sentinel constants and `ConcurrentHashMap`/`LinkedHashMap` static storage choices.

## Evidence precedence for Phase 4

Use this order:

1. exact Hotfix3 bytecode/classfile metadata;
2. reconstructed compiled output;
3. exact Hotfix3 runtime Mixin metadata/log evidence;
4. version-matched dependency source/signatures;
5. historical handoffs only as supporting context.

Phase 4 should compare and explain differences rather than silently rewriting source to “look nicer.”

## Frozen runtime/acoustic invariants

Preserve and explicitly audit all of these:

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

## Important recovered details to keep auditing

### Sync Hotfix3

- `PARTIAL_FLUSH_NS = 100_000_000L`
- `STALE_GROUP_NS = 5_000_000_000L`
- one `AL10.alSourcePlayv(int[])` for complete and expired-partial group starts
- pending-INITIAL lifecycle protection

### EFX

`EnvironmentSmoother` must retain mandatory direct/aux reattachment on every successful environment application and must not create private EFX during `AL_INITIAL`.

### Direct occlusion hook

`SoundPhysicsOcclusionMemoMixin` redirects SPR's internal `runOcclusion(...)` invocation inside `calculateOcclusion(...)`. It does not cancel/replace `calculateOcclusion()`.

### Verifier-safe Beta10 helper

```java
Context context = CONTEXT.get();
if (context != null && context.owner == OWNER_SPR) return context.cacheable;
return false;
```

### Build-time AT nuance

Do not remove the isolated transformed SPR compile JAR. Exact source otherwise fails javac on private SPR members even though runtime AT registration is correct.

## Current Phase 4 audit order

1. Get the 60-class structural ABI comparison to execute once the NeoForge dependency endpoint is available and reconcile every reported difference.
2. Finish exact annotations/Mixin metadata for all 11 configured integration classes.
3. Audit `SoundPhysicsBridge` scheduling/stamps/reuse/sentinel/fairness control flow.
4. Audit Beta9/Beta10 cache/controller/stamp/OpenAL suppression behavior.
5. Audit EnvironmentSmoother/PositionStabilizer/progressive direct/distance formulas and ordering.
6. Audit CompatAudioManager/decode/source lifetime/sync/lifecycle cleanup.
7. Audit config defaults/ranges and Cloth Config UI constants.
8. Close Phase 4 only when structural and behavioral discrepancies are resolved.

## Reconstruction discipline

Each Phase 4 session should:

1. read this handoff/status/Phase 3 final/Phase 4 start record;
2. inspect current branch and exact baseline;
3. audit bounded behavior against bytecode;
4. record discrepancies explicitly;
5. commit only justified equivalence fixes;
6. update documentation;
7. stop before Phase 5 runtime claims or Beta11.1 optimization.

## Exact next prerequisite

Continue **Phase 4 — Structural and behavioral equivalence audit**.

Do not begin Phase 5 or Beta11.1/B until Phase 4 closes.
