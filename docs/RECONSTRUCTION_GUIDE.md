# Beta11 Hotfix3 Reconstruction Guide

This is the durable operating guide for the tested **CC:HQ Sound Physics Compat Beta11 Hotfix3** source baseline.

## 1. Authority and branch

Authoritative artifact:

`cchq_soundphysics_compat-0.1.0-beta11-hotfix3.jar`

SHA-256:

`83500f182fc9829aa1a5a51fbfa11ba6cdfb645699b25d1c445167666dabc1ef`

Repository:

`ztawfik523-lgtm/cchq-soundphysics-compat`

Working branch:

`beta11-source-reconstruction`

The exact JAR was re-supplied and independently verified on 2026-09-04. It remains the source of truth through Phase 5.

The goal is not character-for-character recovery of lost Java. The goal is a readable, rebuildable source project whose structure and behavior are proven against Hotfix3 closely enough to become the safe development base for later work.

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
- Phase 3: **COMPLETE**
- Phase 4: **NEXT / NOT STARTED**
- Phase 5: not started

The older seven-pass/ad-hoc sequence is obsolete.

## 3. Target environment

- Minecraft 1.21.1
- NeoForge 21.1.248
- Java 21
- ModDevGradle 2.0.144
- Gradle 9.2.1
- CC:Tweaked 1.120.2
- CC:HQ Speakers tested artifact `ygA78R8l-u5PEI5Ax.jar`
- Sound Physics Remastered 1.21.1-1.5.1 artifact `qyVF9oeo-Dd2tmpsk.jar`
- optional Cloth Config UI
- client-only compat

## 4. Working rules

Every session must:

1. read `RECONSTRUCTION_STATUS.md`, `docs/BETA11_RECONSTRUCTION_HANDOFF.md`, and the final verification for the last completed phase;
2. inspect current branch and CI state;
3. treat Hotfix3 bytecode/classfile metadata as authoritative;
4. stay inside the current canonical phase;
5. do not optimize, simplify, redesign, or silently improve behavior;
6. preserve descriptors, annotations, mixin targets, constants, OpenAL/lifecycle ordering and scheduler/cache semantics unless the baseline proves otherwise;
7. record discrepancies and uncertainty explicitly;
8. commit coherent code/build/documentation changes;
9. update affected documentation before ending the run;
10. do not merge to `main` or start Beta11.1/B until Phase 5 closes.

## 5. Evidence precedence

For Phase 4 use:

1. exact Hotfix3 bytecode/classfile metadata;
2. reconstructed compiled output;
3. exact Hotfix3 runtime Mixin metadata/log evidence;
4. version-matched external dependency source/signatures;
5. historical handoffs only as supporting context.

Do not change source because a decompiler output merely looks cleaner. Explain and fix only behaviorally/structurally justified differences.

## 6. Phase 1 baseline — complete

Exact-JAR recheck confirms:

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

## 7. Phase 2 build contract — complete

Exact Hotfix3 source directly invokes SPR members widened by this mod's access transformer. Runtime AT registration alone is insufficient for javac.

Current build contract:

- runtime keeps untouched tested SPR;
- `prepareSprCompileJar` transforms an isolated compile-only SPR copy using the exact Hotfix3 AT;
- javac uses `sound-physics-remastered-at.jar`;
- raw SPR is forbidden from compileClasspath;
- AT CLI 10.0.6 is used only as the Java-21-capable preprocessing tool.

Phase 2 proof:

- classpath run `33864425672`: success;
- finish-gate run `33864425687`: success;
- compileClasspath: 90 files;
- NeoForge artifact pipeline passes;
- resource/mixin/AT wiring passes.

## 8. Phase 3 source reconstruction — complete

Final authored source closures:

- `SoundPhysicsBridge.java` — commit `91d70508a04001da788ac7520e09955d5f753b09`;
- `ClothConfigScreen.java` — commit `d336bdea9d39be801360b1f286d67f29d6333772`.

Strict source-closure workflow:

`.github/workflows/phase3-source-closure.yml`

Workflow commit:

`e918e3199b98332c0320eb4cd07e34740d1ec8ec`

Definitive run:

`33867207760` — **SUCCESS**.

The gate ran a clean build and proved:

```text
compileJava: PASS
jar: PASS
Hotfix3 60-class topology: PASS
source-relevant processed resources: PASS
```

Exact class-path counts:

```text
Hotfix3 expected classes: 60
Reconstructed classes:    60
```

The expected/actual class-path diff is empty.

Important final source detail confirmed from the classfile:

`cchq_soundphysics_compat:hq_speaker/<speaker UUID without dashes>`

See `docs/PHASE3_FINAL_VERIFICATION.md`.

## 9. Frozen runtime/acoustic invariants

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
- Preserve exact Hotfix3 weights, ring scales and invalidation thresholds.
- Beta9/Beta10 exact direct reuse remains part of the baseline.

### SPR integration

- Do not cancel/replace SPR `calculateOcclusion()`.
- `SoundPhysicsOcclusionMemoMixin` redirects SPR's internal `runOcclusion(...)` invocation inside `calculateOcclusion(...)`.
- No worker-thread SPR world/geometry raycasts.
- Preserve safe-clone/world access rules.

### Room/bounce cache

- Beta11 room cache applies only to the intended source-centered environment/bounce raycast callsites in SPR `evaluateEnvironment`.
- Exact same-clone reuse only.
- Preserve current/previous bank behavior and `BlockGetter` identity scope.
- Shared-airspace/listener-dependent work stays live.
- Cross-clone room reuse remains telemetry-only.

### EFX

- Private per-source EFX isolation is required.
- **Every actual environment application must reattach direct/aux EFX.**
- Parameter-write suppression is allowed; attachment suppression is not.
- Do not create private EFX while a source is `AL_INITIAL`.
- Native SPR fallback remains available if isolated EFX fails.

### Position / distance

- Preserve `PositionStabilizer` semantics.
- Preserve approved `SoundSource.BLOCKS` distance behavior.
- Preserve reflected/apparent-position restoration/stabilization behavior.

## 10. Important historical failures to audit against

### Original Beta11 verifier failure

A manually patched `Beta10Optimizer.beta11RoomCacheActive()` lacked a stack-map frame and caused a `VerifyError`. Reconstructed normal Java must retain the working semantics:

```java
Context context = CONTEXT.get();
if (context != null && context.owner == OWNER_SPR) return context.cacheable;
return false;
```

### Incomplete sync-group no-sound bug

Declared group size can exceed actually arrived sources. Preserve Hotfix3's 100 ms partial flush and pending-initial protection.

### Alpha13 direct-occlusion replacement

Do not replace/cancel `calculateOcclusion()`; that strategy caused severe sound-thread stalls after geometry changes.

### Beta2 EFX attach-once

Do not suppress EFX reattachment because parameters are unchanged; that broke muffling.

## 11. Phase 4 scope — current work

No feature work. Audit reconstructed output against the exact Hotfix3 binary for:

- all 60 class paths, meaningful method descriptors/access flags and nested/enclosing metadata;
- annotations;
- constants and thresholds;
- mixin targets, injection methods/descriptors, ordinals, `require`, cancellation and remap flags;
- config defaults/ranges;
- OpenAL call inventory, ordering and sound-thread ownership;
- source creation/start/stop/refcount/lifecycle ordering;
- sync group grace/start/cleanup behavior;
- direct distance and progressive occlusion formulas;
- Beta9/Beta10 cache keys/reuse/write suppression;
- room scheduler stamps, age/fairness rules, sentinel transitions and exact reuse gates;
- Beta11 room-ray cache size/probes/banks/keys/scope;
- private-EFX allocation/application/mandatory reattachment;
- position stabilization;
- stop/reload/session teardown;
- source-relevant resources.

Fix only concrete baseline discrepancies. Record any intentional compiler/decompiler differences that are not behaviorally meaningful.

## 12. Phase 4 completion rule

Phase 4 closes only when no unexplained behaviorally meaningful source-level difference remains between reconstructed output and Hotfix3 for the audited surfaces.

A matching 60-class path set from Phase 3 is necessary but is **not** sufficient for Phase 4.

## 13. Phase 5 expectations

After Phase 4 closes, run the reconstructed mod in the lightweight test environment and validate at least:

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

Only after Phase 5 passes should source become authoritative and a Beta11.1/B branch be created.

## 14. Future roadmap — not current work

After validated source handover:

- Beta11.1/B: exact decode/cache/OpenAL/allocation/diagnostic cleanup;
- Beta12/C1: persistent progressive room state;
- Beta12.x/C2: acoustic work scheduler;
- Beta13/D: sparse adaptive room-position memory;
- adaptive ray/bounce quality reduction remains shelved;
- HQ enhanced/music spatial mode remains optional backlog.

## 15. Exact next prerequisite

Begin **Phase 4 — Structural and behavioral equivalence audit** against the exact Hotfix3 JAR.

Do not reopen Phase 3 unless Phase 4 identifies a concrete source reconstruction discrepancy.
