# Phase 4 progress — 2026-09-04

Authoritative runtime baseline:

`cchq_soundphysics_compat-0.1.0-beta11-hotfix3.jar`

SHA-256:

`83500f182fc9829aa1a5a51fbfa11ba6cdfb645699b25d1c445167666dabc1ef`

Branch:

`beta11-source-reconstruction`

## Current status

- Phase 1 — **COMPLETE / JAR-RECHECKED**
- Phase 2 — **COMPLETE / JAR-RECHECKED**
- Phase 3 — **COMPLETE / RECHECKED**
- Phase 4 — **IN PROGRESS**
- Phase 5 — not started

Phase 4 is an equivalence audit only. No Beta11.1/B optimization belongs here.

## Fresh Phase 3 recheck on corrected Phase 4 source

After the first Phase 4 corrections, the complete Phase 3 hard gate was rerun against the corrected source head.

Evidence:

- source-correction head: `29dc17439944f4d2b029e33a8d75a5693827e8b6`
- workflow run: `33896745559`
- job: `101101056470`
- result: **SUCCESS**

The log explicitly reports:

```text
compileJava: PASS
jar: PASS
Hotfix3 60-class topology: PASS
source-relevant processed resources: PASS
Hotfix3 expected classes: 60
Reconstructed classes:    60
```

Therefore Phase 3 remains closed after Phase 4 source corrections.

## Whole-project structural ABI audit

The Phase 4 structural ABI gate fingerprints every Hotfix3/reconstructed compat class using:

- Java major version;
- class access flags, superclass and interfaces;
- field names, descriptors, access flags and constant values;
- method names, descriptors and access flags.

Baseline/tooling:

- `tools/class_abi.py`
- `docs/baseline/HOTFIX3_STRUCTURAL_ABI_SHA256.txt`
- `.github/workflows/phase4-structural-abi.yml`

### First real comparison

The first comparison that reached the ABI diff found **58/60** classes already exact and two real discrepancies:

1. `Beta9Optimizer`
   - reconstructed source contained extra method `resetControllerForHotfix3()` which does not exist in Hotfix3;
   - exact bytecode review also found small source drifts in `registerSource`, `updateAudibility`, and `updateDistance`.

2. `ProgressiveOcclusionModel$State`
   - reconstructed nested-class visibility made its implicit constructor private;
   - Hotfix3 constructor is package-private.

### Corrections

Exact corrections were committed in:

`ec282e4b7057f709b389884f261d38a582ebc15d`

Changes:

- removed the non-Hotfix3 `Beta9Optimizer.resetControllerForHotfix3()` helper;
- restored Hotfix3 `registerSource()` behavior: only `META.put(...)` and `DIRECT.remove(...)`;
- restored Hotfix3 unknown-source audibility transition semantics;
- restored Hotfix3 `updateDistance()` behavior: invalid input does not clear an already-known distance flag;
- restored `Beta10Optimizer.resetBeta9Controller()` to Hotfix3's reflection-based private-field reset path;
- changed `ProgressiveOcclusionModel.State` to package-private nested visibility so its constructor/access ABI matches Hotfix3.

### Fresh ABI result

Workflow:

- run `33896745650`
- job `101101056810`
- result: **SUCCESS**

Log:

```text
Expected classes:      60
Reconstructed classes: 60
Hotfix3 class/field/method structural ABI: PASS
```

**Structural ABI is now 60/60 exact under this fingerprint.**

This does not prove behavioral equivalence; annotations and method bodies are separate Phase 4 layers.

## Rebuilt-binary evidence export

Added:

`.github/workflows/phase4-structural-export.yml`

It clean-builds the reconstructed source and exports:

- reconstructed JAR;
- exact 60-class path list;
- `javap -p -s -constants` output for every class;
- rebuilt-JAR SHA-256.

Current-head export:

- workflow run `33897048940`
- job `101102019555`
- result: **SUCCESS**
- artifact id `9946220940`
- artifact digest `sha256:3ad4dcf8a3f12feca59451022402214f63544c82f159ff264aba672315a89aeb`
- workflow head `ca1c05918e6c32b91e948bbe57a77d51cee67bff`

The rebuilt JAR is not expected to be byte-for-byte identical to the historical JAR because javac/compiler metadata/source layout may differ. Phase 4 compares semantic structure and behavior rather than requiring whole-file SHA identity.

## Method-body comparison started

A direct Hotfix3-vs-rebuilt `javap -p -c -s` comparison has begun for the highest-risk classes.

Method inventory already matches exactly for the current source in:

- `SoundPhysicsBridge` — 26/26 methods;
- `SyncStartCoordinator` — 11/11;
- `EnvironmentSmoother` — 18/18;
- `CompatAudioManager` — 38/38;
- `ProgressiveOcclusionModel` — 27/27;
- `Beta9Optimizer` — 42/42;
- `Beta10Optimizer` — 36/36.

This normalized bytecode comparison is a review aid, not a hard equivalence theorem: local-variable slots, constant-pool indices, branch offsets and javac source-shape decisions can differ without changing behavior.

### `SoundPhysicsBridge`

The first normalized pass found only very small compiler-layout differences in `apply(...)` and `runClearingSentinel(...)`; no proven semantic discrepancy has been found there yet.

The exact constants, field/method ABI, nested topology, scheduler candidate order, room stamp fields and sound-id path remain consistent with Hotfix3.

### `SyncStartCoordinator`

Exact bytecode review identified two harmless source-shape differences which can be aligned without behavior change:

- Hotfix3 stores `System.nanoTime()` to a local before `flushExpired(now)` inside `sourceState(...)`;
- Hotfix3's `playVector(...)` loop compares `i < ids.length`, while reconstructed source compared `i < sources.size()`.

Those two source forms were aligned in commit:

`cc019e5088df3ec3544b43b177208c6093f71943`

The already-correct Hotfix3 semantics remain:

- direct non-group start uses `alSourcePlay`;
- complete/expired partial groups use one `alSourcePlayv(int[])`;
- partial grace is 100 ms;
- stale group age is 5 s;
- pending `AL_INITIAL` sources are protected as paused;
- source removal iterates `GROUPS.values()` and removes empty groups.

## Earlier Phase 4 exact corrections retained

Before this progress checkpoint, Phase 4 had already corrected:

1. `HQSpeakerClientHandlerMixin`
   - exact receive descriptor;
   - `@At(..., remap=false)`;
   - `cancellable=true` / injection `remap=false`;
   - `@Coerce` payload;
   - restored `cchqphysics$reportedHook`.

2. `HQSpeakerStopPacketMixin`
   - exact handle descriptor;
   - nested `remap=false` metadata;
   - exact `@Coerce` parameters.

3. `SyncStartCoordinator.removeSource`
   - exact `GROUPS.values().iterator()` iteration shape.

See `docs/PHASE4_START_AUDIT.md` for the opening audit record.

## Still to audit before Phase 4 can close

Phase 4 remains **IN PROGRESS**. Required remaining work includes:

1. Finish exact annotation/Mixin metadata comparison for all 11 configured mixin/accessor classes.
2. Finish `SoundPhysicsBridge` scheduler/stamp/room-reuse/sentinel/fairness control-flow audit.
3. Finish `Beta9Optimizer` / `Beta10Optimizer` cache ownership, stamp gates, adaptive controller, ray-cache and OpenAL write-suppression audit.
4. Finish `EnvironmentSmoother`, `ProgressiveOcclusionModel`, `PositionStabilizer`, attenuation/distance and reflection formula/order audit.
5. Finish playback/decode/source lifetime/synchronized-start/lifecycle teardown audit.
6. Finish config defaults/ranges and Cloth Config UI constants/tooltips audit.
7. Re-run hard build/topology/ABI gates after any correction.
8. Produce a Phase 4 final verification record only after all proven discrepancies are resolved.

Do not start Phase 5 or Beta11.1/B until Phase 4 closes.
