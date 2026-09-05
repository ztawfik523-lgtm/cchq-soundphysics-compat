# Phase 5 Issue A — perceived reverb/treble/spatial coloration

Status: **IN PROGRESS — diagnostic A/B build prepared, runtime result pending**

Date: 2026-09-05

## Why this exists

During the Spectral Mix V2 listening run, the user reported that the music itself sometimes sounded different — described as possible extra reverb, extra treble, or an otherwise hard-to-name coloration. Crucially, the report first appeared **before Spectral Mix V2 was enabled**, so the coloration cannot be attributed solely to V2.

The strongest runtime clue from the supplied logs is that one synchronized source could be strongly occluded and simultaneously redirected toward SPR's reflected sound position, while other synchronized copies remained nearly clear. That combination can plausibly create a spatial/spectral smear that is perceived as reverb-like, phasey, brighter, or otherwise altered.

This is a hypothesis, not yet a proven root cause.

## Isolation rule

This experiment branches directly from the verified known-good Phase 5 candidate:

- baseline commit: `44612192d875e43ecef66ca51798cab7adb17020`
- branch: `phase5-issue-a-reflection-diagnostics`

It does **not** include Spectral Mix V1 or V2.

The default reflected-position behavior remains the known-good behavior on every launch.

## Diagnostic changes

### Runtime-only A/B switch

New client command:

- `/cchqphysics reflection_redirect on`
- `/cchqphysics reflection_redirect off`
- `/cchqphysics reflection_redirect status`

The switch is intentionally **not persisted**. Every game launch starts with reflection redirection **ON**.

`ON` means the existing PositionStabilizer behavior is used: when SPR supplies a reflected point and occlusion exceeds the existing configured threshold, the OpenAL source may be blended toward that reflected point using the existing blend/offset/smoothing values.

`OFF` means the same source is allowed to settle back toward its real speaker position using the existing clear-position smoothing path. No direct gain, cutoff, reverb-send, synchronization, PCM, source lifetime, or distance logic is modified by the switch.

### Expanded `/cchqphysics dump`

The Issue A snapshot records per source:

- real HQ speaker position
- currently applied OpenAL position
- SPR reflected point, when present
- applied positional offset in blocks
- current progressive occlusion
- whether reflection redirection was requested by the acoustic state
- whether reflection redirection was actually active
- captured SPR room direct cutoff/gain
- captured room send gains `r0..r3`
- captured room send high-frequency cutoffs `h0..h3`
- smoothed compat EFX direct cutoff/gain
- smoothed per-send gains `r0..r3`
- smoothed per-send HF values `h0..h3`

This is intended to distinguish three possibilities:

1. reflected-position redirection is responsible for the perceived coloration;
2. mixed clear/muffled synchronized copies cause the coloration even with reflection redirection disabled;
3. room/reverb send values themselves change in a way that correlates with the perceived effect.

## Required A/B test

Use the same synchronized multi-speaker scene where the coloration was previously noticeable.

1. Start the game with the Issue A test build. Do not run the reflection command yet; startup default must remain ON.
2. Start synchronized playback and move to a spot where the coloration is audible.
3. Run `/cchqphysics dump` while the effect is audible.
4. Without moving if possible, run `/cchqphysics reflection_redirect off`.
5. Give the stabilizer a moment to settle and listen to the same passage/scene.
6. Run `/cchqphysics dump` again.
7. Run `/cchqphysics reflection_redirect on` and confirm whether the original sensation returns.
8. Run one final `/cchqphysics dump`.
9. Return `latest.log` and describe whether OFF made the sound clearly better, worse, or merely different.

The most valuable observation is not “which sounds nicer”; it is whether the hard-to-name coloration **tracks the reflection-redirection switch reproducibly**.

## Interpretation

- If coloration appears with ON and disappears with OFF while room-send values remain similar, reflected positioning is the likely root cause.
- If coloration persists with OFF and clear/muffled copies remain spectrally different, synchronized spectral summation is the stronger explanation.
- If room/send values change sharply at the same time as the effect, investigate room/reverb scheduling separately.
- If the result is ambiguous, do not change acoustic defaults; add targeted telemetry rather than guessing.

## Separation from elevation issue

The elevation/hole over-occlusion report is tracked as a separate acoustic-model issue. It appears to arise from straight-line occlusion rays treating a diagonal path through terrain as many solid obstructions, without diffraction/path-around-edge behavior. No elevation fix is included in this Issue A branch.

## Closure rule

Do not mark Phase 5 complete based on this branch until the A/B result is reviewed. If reflected positioning is not the cause, this branch remains diagnostic evidence and the known-good candidate stays authoritative.
