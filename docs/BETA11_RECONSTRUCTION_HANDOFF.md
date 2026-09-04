# Beta11 Hotfix3 Reconstruction Handoff

This is the durable session handoff for `beta11-source-reconstruction`. Read this file, `RECONSTRUCTION_STATUS.md`, `docs/PHASE3_FINAL_VERIFICATION.md`, and `docs/RECONSTRUCTION_PHASES.md` before changing code.

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
- Phase 3: **COMPLETE**
- Phase 4: **NEXT / NOT STARTED**
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

## Phase 3 authority — COMPLETE

All authored/source-bearing gaps are closed.

Final source commits:

- `SoundPhysicsBridge.java` — `91d70508a04001da788ac7520e09955d5f753b09`;
- `ClothConfigScreen.java` — `d336bdea9d39be801360b1f286d67f29d6333772`.

Strict closure workflow added in:

`e918e3199b98332c0320eb4cd07e34740d1ec8ec`

Definitive Phase 3 run:

`33867207760`

Result: **SUCCESS**.

The hard gate proves:

```text
compileJava: PASS
jar: PASS
Hotfix3 60-class topology: PASS
source-relevant processed resources: PASS
```

Exact topology counts:

```text
Hotfix3 expected classes: 60
Reconstructed classes:    60
```

The expected/actual class-path diff is empty. No baseline class path is missing and no extra nested/synthetic class path was introduced.

`SoundPhysicsBridge` now intentionally reconstructs the Hotfix3 source/room scheduler stack, including its five nested outputs:

- `Candidate`
- `RoomEnvironmentAccess`
- `RoomEnvironmentAccess.ConfigStamp`
- `RoomStamp`
- `SourceState`

The stable per-speaker sound identity confirmed from the classfile is:

`cchq_soundphysics_compat:hq_speaker/<speaker UUID without dashes>`

`ClothConfigScreen` now reconstructs the tested optional UI, including exact title `CC:HQ × Sound Physics`, config defaults/ranges and binary UI wording.

See `docs/PHASE3_FINAL_VERIFICATION.md`.

## Evidence precedence for Phase 4

Use this order:

1. exact Hotfix3 bytecode/classfile metadata;
2. reconstructed compiled output;
3. exact Hotfix3 runtime mixin metadata/log evidence;
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

## Important recovered details to audit in Phase 4

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

## Phase 4 scope — next work only

Perform the structural and behavioral equivalence audit against the exact Hotfix3 JAR. At minimum audit:

- all 60 classes and meaningful method descriptors/access flags;
- nested/enclosing topology;
- important constants and thresholds;
- mixin targets, injection descriptors, ordinals, `require`, cancellation behavior and remap flags;
- OpenAL calls/order/thread ownership;
- synchronization/group lifecycle;
- source generation/lifetime identity;
- direct distance/occlusion formulas and probe topology;
- Beta9/Beta10 exact cache semantics;
- room scheduler/stamp/sentinel/fairness behavior;
- Beta11 room-ray cache scope/keys/banks;
- private-EFX creation/application/mandatory reattachment;
- position stabilization;
- config defaults/ranges;
- source-relevant resources.

Fix only discrepancies justified by the Hotfix3 baseline.

## Reconstruction discipline

Each Phase 4 session should:

1. read this handoff/status/final Phase 3 record;
2. inspect current branch and exact baseline;
3. audit bounded behavior against bytecode;
4. record discrepancies explicitly;
5. commit only justified equivalence fixes;
6. update documentation;
7. stop before Phase 5 runtime claims or Beta11.1 optimization.

## Exact next prerequisite

Begin **Phase 4 — Structural and behavioral equivalence audit**.

Phase 3 source work is closed. Do not reopen it unless Phase 4 finds a concrete baseline discrepancy.
