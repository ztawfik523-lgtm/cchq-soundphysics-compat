# Phase 5 current status — 2026-09-05

Phase 5 remains **IN PROGRESS**. The reconstructed source and known-good runtime candidate are preserved; two optional synchronized-mix experiments and one current reflection/coloration diagnostic branch are isolated from that baseline.

## Preserved baselines

- Phase 4 Hotfix3 parity branch: `phase4-hotfix3-parity`
  - frozen head: `79eed29767343ee34022e8f6268b386f75e84c9f`
- archival Phase 4 ref: `archive-phase4-hotfix3-parity`
  - same frozen head
- known-good Phase 5 runtime candidate: `phase5-test-candidate-1`
  - frozen head: `44612192d875e43ecef66ca51798cab7adb17020`
  - verified JAR SHA-256: `6d782812f7915de1870a8b5ae0f619556e7ec1d24ef2eaffa7b5b225aa00bd93`
  - user runtime tests established clean startup, source lifecycle, private EFX isolation, wall/doorway behavior, movement, stress behavior, synchronized starts, safe cache/room reset behavior, and normal shutdown

No experiment below changes those frozen refs.

## Synchronized-mix experiments

### V1 — rejected

The first multi-speaker compensation experiment reduced whole-source gain for strongly occluded synchronized copies. Runtime listening showed that although the combined mix could sound more balanced spectrally, the algorithm distorted spatial weighting and could pull the apparent image toward a clearer side speaker. This design is rejected; it is retained only as historical evidence.

### V2 — preserved, not accepted yet

Branch/test candidate: `phase5-mix-v2-test-candidate`

Exact frozen source commit: `ab1e1e70a13ebb6f3dadd30581b069f06a15142a`

Verified V2 JAR SHA-256: `bba8d93e696403ae857dd155db2969c7591886aa1e8734b0b949f1a749c8319c`

V2 avoids source gain and position changes. It only provides a conservative bounded lift to extremely low direct low-pass cutoff values when a genuinely clear peer exists in the same synchronized group. Independent verification proved its build reproducible and byte-identical across two builds.

Runtime listening was technically healthy, but the user still noticed a hard-to-name reverb/treble/spatial coloration that had already been present before V2 was enabled. Therefore V2 is preserved but not yet promoted to the maintained final candidate.

## Issue A — reflected-position/coloration investigation

Working branch: `phase5-issue-a-reflection-diagnostics`

Known-good base: `44612192d875e43ecef66ca51798cab7adb17020`

Reviewed frozen test branch: `phase5-issue-a-test-candidate-2`

Exact reviewed/test source commit: `973f1df7dad886fb0f5fffd4264015fecac2e786`

Build identity: `0.1.0-beta11-phase5-issuea-test`

Verified Issue-A JAR SHA-256: `d649f14cdce89db21a79c396dbdecca681daf3d0389dc794a7ad52929f8c8451`

Verification run `33935819269` / job `101223434623` completed **SUCCESS**. Static/build verification is therefore finished; Issue A is now **awaiting user A/B listening evidence**.

Purpose: determine whether the perceived coloration is caused by one or more occluded synchronized sources being spatially redirected toward SPR reflected positions while other copies remain clear, versus spectral summation or room/reverb-send behavior.

This branch intentionally excludes both synchronized-mix experiments.

Current diagnostic additions:

- runtime-only `/cchqphysics reflection_redirect on|off|status`
- per-source `/cchqphysics reflection_redirect source <sourceId> on|off|auto|status`
- startup default remains **ON**, matching known-good behavior
- per-source overrides are cleared when that OpenAL source unregisters
- `/cchqphysics dump` records real/applied/reflected positions, redirect offset/state, normal source state, and private-EFX direct state
- the first reflective private-state inspector was removed; current diagnostics use compile-checked package-local paths

External review findings were incorporated before the final verified build: global-only A/B was expanded to per-source isolation, fragile reflective state access was removed, stale inherited Phase-5 docs were corrected, and obsolete one-shot Phase-5 patch scripts were removed.

Detailed procedure and interpretation rules:

- `docs/PHASE5_ISSUE_A_REFLECTION_DIAGNOSTICS.md`
- `docs/PHASE5_ISSUE_A_BUILD_RECORD.md`

## Separate elevation issue

The user also identified excessive muffling when the listener is vertically separated from a nearby speaker by open terrain geometry such as standing down in an open-topped hole. Logs showed very high straight-line occlusion values in that scenario. Source inspection shows the current progressive model samples center + 16 surrounding rays in three dimensions, but all paths remain straight source-to-listener rays and therefore cannot model diffraction/path-around-edge propagation.

This is a separate future experiment. No elevation/diffraction correction is included in Issue A.

## Immediate next action

Use the verified Issue-A JAR and run the documented A/B matrix:

1. one standalone speaker with a real non-zero reflected-position offset — reflection ON vs OFF;
2. synchronized 4-speaker scene — global ON vs OFF only as a coarse sanity check;
3. synchronized scene — identify the largest redirected source with `/cchqphysics dump`, disable reflection only for that source, and compare without moving.

The third test is the primary deciding evidence. If disabling one redirected synchronized source reliably removes or changes the reported coloration, reflected-position interaction is implicated. If it does not, the next diagnostic revision should add typed room/reverb-send telemetry (`r0..r3`, `h0..h3`) rather than altering acoustics by guesswork.

## Closure rule

Do not mark Phase 5 **COMPLETE / RECHECKED** until Issue A is resolved or explicitly accepted as a known limitation, the elevation decision is made, and the final maintained candidate is frozen and verified. The known-good candidate remains the rollback authority throughout experimental work.

Do not merge to `main` without explicit user approval.
