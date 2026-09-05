# Phase 5 Issue A — reflected-position / coloration diagnostics

Status: **IN PROGRESS — diagnostic A/B build pending clean verification and runtime test**

Date: 2026-09-05

## Purpose

This branch investigates the subjective report that synchronized HQ-speaker playback can sometimes sound as if extra reverb, treble, spatial smear, or other coloration has been added even though the private EFX path itself is healthy.

The report appeared **before** Spectral Mix V2 was enabled, so Issue A is deliberately based on the known-good Phase 5 candidate rather than on either synchronized-mix experiment.

Known-good base candidate:

`phase5-test-candidate-1`

Base commit:

`44612192d875e43ecef66ca51798cab7adb17020`

Issue-A branch:

`phase5-issue-a-reflection-diagnostics`

Build identity:

`0.1.0-beta11-phase5-issuea-test`

## Leading hypothesis — not yet a conclusion

One source in the synchronized group was observed with a substantial reflected-position offset while other correlated copies of the same audio remained nearly clear/direct. `PositionStabilizer` intentionally moves an occluded source toward SPR's reflected point when the reflection threshold is crossed.

With one standalone source this redirection may be desirable. With multiple synchronized/correlated copies, however, one displaced and muffled copy mixed with several bright/direct copies could plausibly be perceived as spatial smear, extra ambience, phase-like coloration, or a change in the music's timbre.

This remains a hypothesis. The diagnostic build is designed to answer it without changing the default acoustic behavior.

## Safety boundary

This Issue-A build does **not** intentionally change:

- PCM data or playback clocks;
- synchronized-start timing;
- source distance gain;
- progressive occlusion calculation;
- direct low-pass cutoff/gain;
- room/reverb calculation;
- private-EFX attachment behavior;
- Beta9/Beta10/Beta11 cache semantics;
- the default reflected-position behavior.

Every launch starts with reflection redirection globally **ON**, matching the known-good candidate. Diagnostic overrides are runtime-only and are not persisted.

## Diagnostic controls

### Global A/B

- `/cchqphysics reflection_redirect on`
- `/cchqphysics reflection_redirect off`
- `/cchqphysics reflection_redirect status`

The global default is ON.

### Per-source A/B

A code-review finding correctly noted that a global switch is too blunt to identify which correlated source is responsible. Issue A therefore also supports a per-source tri-state override:

- `/cchqphysics reflection_redirect source <sourceId> on`
- `/cchqphysics reflection_redirect source <sourceId> off`
- `/cchqphysics reflection_redirect source <sourceId> auto`
- `/cchqphysics reflection_redirect source <sourceId> status`

`auto` means follow the global setting.

Per-source overrides are cleared when that OpenAL source unregisters, so a later reused source ID cannot inherit a stale experiment.

## Diagnostic snapshots

`/cchqphysics dump` records the normal Phase 5 source/EFX state plus focused Issue-A position state.

Relevant records:

- `[phase5/source]` — source identity, playing/in-range state, progressive raw/center occlusion, direct cutoff/gain and real HQ position;
- `[phase5/issue-a/position]` — real position, currently applied OpenAL position, reflected point, applied offset magnitude, current occlusion, whether reflection was requested by SPR/occlusion, whether redirect is actually active, and the source-specific override/effective state;
- `[phase5/source-efx]` — private-EFX readiness and smoothed direct cutoff/gain.

The first draft of Issue-A diagnostics used Java reflection to inspect private `SoundPhysicsBridge` and `EnvironmentSmoother` state. That was removed after review. The current focused dump delegates to compile-checked package-local diagnostic paths instead, so source refactors cannot silently break an on-demand reflection inspector.

This first diagnostic pass intentionally does **not** add more invasive room-send accessors yet. If the A/B result fails to implicate reflected positioning, the next diagnostic revision should add explicit typed room/send snapshots (`r0..r3`, `h0..h3`) rather than restoring reflective field access.

## Test matrix

The purpose of the matrix is to distinguish an intrinsic reflected-position effect from a correlated multi-source interaction.

### Test A — one standalone speaker

1. Play one non-synchronized speaker in geometry where `/cchqphysics dump` shows `requestedRedirect=true` and a meaningful non-zero offset.
2. Listen with global reflection redirect ON.
3. Run `/cchqphysics dump`.
4. Toggle global redirect OFF without moving.
5. Allow the stabilizer to return toward the real position, then listen again.
6. Run `/cchqphysics dump` again.
7. Return global redirect ON before ending the test.

Interpretation:

- if coloration changes clearly with only one source, reflected positioning itself is audible in the reported way;
- if it does not, the problem is more likely related to correlated multi-source mixing.

### Test B — synchronized group, global A/B

1. Start the known 4-speaker synchronized setup.
2. Keep global redirect ON; reproduce the position where the coloration is easiest to hear.
3. Run `/cchqphysics dump`.
4. Toggle global redirect OFF and listen at the same position.
5. Run `/cchqphysics dump`.
6. Restore global redirect ON.

Interpretation:

- if only the synchronized group changes, correlated copies are an important part of the symptom.

### Test C — synchronized group, one-source isolation

1. With the synchronized group playing and global redirect ON, run `/cchqphysics dump`.
2. Identify a source showing `requestedRedirect=true`, `redirectActive=true`, and the largest meaningful applied offset.
3. Disable reflection only for that source:
   `/cchqphysics reflection_redirect source <id> off`
4. Leave every other source on `auto`.
5. Listen without moving and run another dump.
6. Return that source to `auto` before stopping playback.

Interpretation:

- if disabling one redirected member removes or substantially changes the coloration, the source-specific reflected-position interaction is strongly implicated;
- if nothing changes, reflected positioning is unlikely to be the main cause and the next investigation should instrument room/reverb send values directly.

## Elevation issue is separate

The newly discovered over-occlusion when the listener is several Y-levels below a nearby speaker is **not** part of Issue A. Current evidence points to straight-line occlusion rays treating a diagonal path through terrain as many solid obstructions even when an open route exists around a rim/edge.

That behavior will be investigated separately as an optional diffraction/escape-path experiment after Issue A is resolved. Keeping it separate prevents two acoustic changes from contaminating the same A/B test.

## Relationship to synchronized-mix experiments

- `phase5-test-candidate-1` remains the known-good reference.
- The first amplitude-suppression synchronized-mix experiment was rejected because it changed spatial weighting.
- `phase5-mix-v2-test-candidate` remains frozen as a separate spectral-only experiment.
- Issue A does not include either synchronized-mix experiment.

## Branch hygiene / historical tooling

This branch was created directly from the frozen `44612192...` candidate, which predates later documentation and cleanup commits on `phase5-test-extended`. That is why the first branch snapshot inherited an obsolete `RECONSTRUCTION_STATUS.md` saying Phase 5 was not started and also contained the historical `phase5_apply_batch1..4.py` scripts.

Those inherited one-shot mutation scripts are being removed from this branch. Their presence was historical scaffolding, not a requirement of the maintained Issue-A source.

The branch status/docs must describe its actual Phase-5 state rather than the older state of its frozen base commit.

## Closure rule

Do not merge an Issue-A behavioral change based only on theory. The branch may add diagnostics freely, but any permanent reflected-position behavior change requires runtime A/B evidence from the test matrix above.
