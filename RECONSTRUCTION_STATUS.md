# Beta11 Hotfix3 source reconstruction status

Authoritative runtime baseline: `cchq_soundphysics_compat-0.1.0-beta11-hotfix3.jar`

Authoritative Hotfix3 SHA-256:
`83500f182fc9829aa1a5a51fbfa11ba6cdfb645699b25d1c445167666dabc1ef`

Current investigation branch:
`phase5-issue-a-reflection-diagnostics`

Hotfix3 remains the behavioral authority for the frozen parity baseline. Phase 5 is active and has already received substantial real-game runtime validation. It is **not complete** because the final maintained behavior/source handover is still being resolved around synchronized multi-speaker coloration and a separate elevation/occlusion limitation.

## Canonical five-phase status

| Phase | Status |
| --- | --- |
| Phase 1 — Freeze and inventory binary baseline | **COMPLETE / JAR-RECHECKED** |
| Phase 2 — Reconstruct build project | **COMPLETE / JAR-RECHECKED** |
| Phase 3 — Reconstruct every Java class | **COMPLETE / RECHECKED** |
| Phase 4 — Structural and behavioral equivalence audit | **COMPLETE / RECHECKED / FROZEN** |
| Phase 5 — Runtime validation and source handover | **IN PROGRESS — CORE RUNTIME PASSED / ISSUE A DIAGNOSTICS IN PROGRESS** |

Canonical plan: `docs/RECONSTRUCTION_PHASES.md`.

---

## Phase 1 — COMPLETE / JAR-RECHECKED

The exact Hotfix3 JAR independently confirmed the frozen baseline:

- whole-JAR SHA-256 matches the authority;
- 75 ZIP entries, 65 files and 60 Java-21 classfiles;
- five source-relevant runtime resources frozen byte-for-byte.

See the `docs/baseline/` evidence and `docs/PHASE1_HOTFIX3_BYTECODE_AUDIT.md`.

## Phase 2 — COMPLETE / JAR-RECHECKED

Pinned build environment:

- Java 21
- Minecraft 1.21.1
- NeoForge 21.1.248
- ModDevGradle 2.0.144
- Gradle wrapper 9.2.1
- CC:Tweaked 1.120.2
- SPR 1.21.1-1.5.1 (pinned Modrinth version ID `Dd2tmpsk`)
- HQ Speakers pinned Modrinth project/version `ygA78R8l/u5PEI5Ax`
- Cloth Config 15.0.140

The source intentionally compiles against an isolated access-transformed SPR copy while runtime uses the untouched tested SPR artifact.

## Phase 3 — COMPLETE / RECHECKED

All authored Java source was reconstructed. The final Phase-3/Phase-4 closure reconciled exact 60/60 Hotfix3 class topology before Phase-5 extensions were introduced.

## Phase 4 — COMPLETE / RECHECKED / FROZEN

Permanent frozen parity branch:

`phase4-hotfix3-parity`

Permanent archive ref:

`archive-phase4-hotfix3-parity`

Both preserve the Phase-4 freeze rooted at:

`79eed29767343ee34022e8f6268b386f75e84c9f`

Final audited Phase-4 code/build head:

`98e7dedb7ecf6fda22008b084b6bb41956edff78`

Phase 4 established:

- 60/60 class paths exact;
- 60/60 structural ABI exact;
- 69/69 compiled `ConstantValue` entries exact;
- zero bootstrap/string-concat recipe mismatches;
- 11/11 Mixin/accessor semantic annotation sets reconciled;
- 5/5 non-class Hotfix3 resources byte-for-byte exact;
- 550 methods audited;
- no unresolved proven Hotfix3 behavioral discrepancy.

Phase-4 frozen behavior is not modified by any Phase-5 experiment.

---

# Phase 5 — IN PROGRESS

## Known-good runtime candidate

Frozen branch:

`phase5-test-candidate-1`

Candidate commit:

`44612192d875e43ecef66ca51798cab7adb17020`

Verified JAR SHA-256:

`6d782812f7915de1870a8b5ae0f619556e7ec1d24ef2eaffa7b5b225aa00bd93`

The candidate keeps original `ClientConfig.java` unchanged from the frozen Phase-4 source and adds advanced/debug controls whose defaults preserve Hotfix3 values.

## Real-game evidence already obtained

The user performed the real Minecraft/audio validation. Core findings already supported by runtime logs and listening:

- correct Phase-5 build identity loaded;
- required compat Mixins applied;
- OpenAL initialized and SPR EFX became ready with four auxiliary sends;
- private per-source EFX was created successfully for active HQ sources;
- no observed private-EFX failure/native-fallback fault during ordinary playback;
- required EFX reattachment invariant held (`efxApplies == efxReattachPasses` in reported windows);
- 12-source stress playback ran without compat crash;
- listener/source movement and doorway clearing transitions operated;
- room/direct reuse and Beta9/Beta10/Beta11 telemetry operated;
- debug cache/room/EFX reset commands executed while playback survived;
- source cleanup/unregister and world shutdown completed normally;
- synchronized playback was heard starting together by the user;
- the coordinator consistently received one fewer compat audio source than HQ's declared expected count (for example 4/5) and used the existing partial-flush grace rather than losing audible speakers;
- the user reported the known-good candidate's general acoustics sounded correct.

The brief static heard only during `/cchqphysics reset_efx` is treated as a debug-command teardown/recreate artifact, not an ordinary playback failure.

## Synchronized-mix experiments

### V1 amplitude suppression — REJECTED

The first optional attempt reduced the whole-source gain of occluded synchronized copies. It made the mix feel more balanced in one dimension but distorted spatial weighting: a clear off-axis speaker could dominate left/right balance and heavily occluded speakers could nearly disappear.

That design is rejected and remains only as branch/history evidence.

### V2 spectral-only compensation — FROZEN EXPERIMENT / NOT YET FINAL

Frozen test branch:

`phase5-mix-v2-test-candidate`

V2 deliberately did not change source gain, source position, or reverb-send filters. It only bounded a direct low-pass cutoff lift for extremely dark synchronized copies when a clear peer existed.

The implementation/build gates were clean, but during the listening session the user also reported an unclear reverb/treble/spatial coloration. Importantly, the first report occurred before V2 was enabled, so V2 cannot be assumed to be the cause.

V2 therefore remains a separate experiment and is not the final maintained source.

## Issue A — reflected-position / coloration diagnostics

Current branch:

`phase5-issue-a-reflection-diagnostics`

Base:

known-good candidate commit `44612192d875e43ecef66ca51798cab7adb17020`

Issue-A does **not** include synchronized-mix V1 or V2.

Purpose:

Determine whether the hard-to-name perceived coloration is caused by:

1. reflected-position redirection itself;
2. the interaction of a redirected/muffled correlated copy with other bright/direct synchronized copies; or
3. another room/reverb behavior.

Diagnostic controls are runtime-only. Global reflection redirection starts ON every launch, matching known-good behavior. Per-source ON/OFF/AUTO overrides allow one member of a synchronized group to be isolated while the others remain unchanged.

Focused documentation and test matrix:

`docs/PHASE5_ISSUE_A_REFLECTION_DIAGNOSTICS.md`

The first diagnostic draft used reflection to inspect private Java state. That implementation was removed after review; the current snapshot uses compile-checked diagnostic paths instead.

## Separate elevation / diffraction issue

A distinct long-standing acoustic-model limitation was discovered when the listener was only a few blocks horizontally from a speaker but several Y-levels below it in an open-topped hole. Straight speaker-to-listener occlusion rays crossed many terrain blocks and produced extremely high occlusion, even though an acoustically plausible route existed around the rim.

This is tracked separately. No elevation/diffraction correction is included in Issue A. A future isolated experiment may probe alternate two-segment escape/diffraction paths while preserving legitimate sealed-floor/ceiling occlusion.

## Branch hygiene

This Issue-A branch was created directly from frozen candidate commit `44612192...`, which predates later Phase-5 documentation and cleanup commits. Therefore its initial snapshot inherited an obsolete status file saying Phase 5 was not started and inherited historical `phase5_apply_batch1..4.py` mutation scripts.

Those statements/files were historical artifacts of the frozen base, not the current project state. The Issue-A branch now removes the one-shot batch scripts and carries current Phase-5 status documentation.

## What remains before Phase 5 can close

1. Clean-build and verify the Issue-A diagnostic JAR.
2. Run the documented single-speaker / synchronized-group / one-source reflection A/B tests.
3. Decide from evidence whether reflected positioning needs a behavior change or is exonerated.
4. Investigate the elevation/diffraction limitation separately if desired before closure.
5. Decide whether V2 spectral compensation belongs in the maintained build or remains experimental.
6. Freeze the final extended/configurable source candidate.
7. Finalize runtime-validation report, reproducible build docs, supported stack/hash, source handover and README/status.
8. Mark Phase 5 **COMPLETE / RECHECKED** only after those decisions are frozen and verified.

Do not merge to `main` without explicit user approval.
