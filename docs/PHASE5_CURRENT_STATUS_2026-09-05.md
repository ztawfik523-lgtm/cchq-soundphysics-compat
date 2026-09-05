# Phase 5 current status — 2026-09-05

Phase 5 remains **IN PROGRESS**. The reconstructed source and known-good runtime candidate are preserved; two optional synchronized-mix experiments and one current reflection/coloration diagnostic branch are isolated from that baseline.

## Preserved baselines

- Phase 4 Hotfix3 parity branch: `phase4-hotfix3-parity`
  - frozen head: `79eed29767343ee34022e8f6268b386f75e84c9f`
- archival Phase 4 ref: `archive-phase4-hotfix3-parity`
  - same frozen head
- known-good Phase 5 runtime candidate: `phase5-test-candidate-1`
  - frozen head: `44612192d875e43ecef66ca51798cab7adb17020`
  - user runtime tests established clean startup, source lifecycle, private EFX isolation, wall/doorway behavior, movement, stress behavior, synchronized starts, safe cache/room reset behavior, and normal shutdown

No experiment below changes those frozen refs.

## Synchronized-mix experiments

### V1 — rejected

The first multi-speaker compensation experiment reduced whole-source gain for strongly occluded synchronized copies. Runtime listening showed that although the combined mix could sound more balanced spectrally, the algorithm distorted spatial weighting and could pull the apparent image toward a clearer side speaker. This design is rejected; it is retained only as historical evidence.

### V2 — preserved, not accepted yet

Branch/test candidate: `phase5-mix-v2-test-candidate`

V2 avoids source gain and position changes. It only provides a conservative bounded lift to extremely low direct low-pass cutoff values when a genuinely clear peer exists in the same synchronized group. Independent verification proved its build reproducible and byte-identical across two builds.

Runtime listening was technically healthy, but the user still noticed a hard-to-name reverb/treble/spatial coloration that had already been present before V2 was enabled. Therefore V2 is preserved but not yet promoted to the maintained final candidate.

## Issue A — reflected-position/coloration investigation

Branch: `phase5-issue-a-reflection-diagnostics`

Base: known-good candidate `44612192d875e43ecef66ca51798cab7adb17020`

Purpose: determine whether the perceived coloration is caused by one or more occluded synchronized sources being spatially redirected toward SPR reflected positions while other copies remain clear, versus spectral summation or room/reverb-send behavior.

This branch intentionally excludes both synchronized-mix experiments.

Diagnostic additions:

- runtime-only `/cchqphysics reflection_redirect on|off|status`
- startup default remains **ON**, matching known-good behavior
- expanded `/cchqphysics dump` records real/applied/reflected source positions, redirect offset/state, captured room sends, and smoothed private-EFX sends

Detailed procedure and interpretation rules:

- `docs/PHASE5_ISSUE_A_REFLECTION_DIAGNOSTICS.md`

## Separate elevation issue

The user also identified excessive muffling when the listener is vertically separated from a nearby speaker by open terrain geometry such as standing down in an open-topped hole. Logs showed very high straight-line occlusion values in that scenario. Source inspection shows the current progressive model samples center + 16 surrounding rays in three dimensions, but all paths remain straight source-to-listener rays and therefore cannot model diffraction/path-around-edge propagation.

This is a separate future experiment. No elevation/diffraction correction is included in Issue A.

## Closure rule

Do not mark Phase 5 **COMPLETE / RECHECKED** until the active diagnostic question is resolved or explicitly documented as a known limitation. The known-good candidate remains the rollback authority throughout experimental work.
