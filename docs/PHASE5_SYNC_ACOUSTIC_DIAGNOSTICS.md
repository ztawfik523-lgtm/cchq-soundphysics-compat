# Phase 5 synchronized timing + acoustic-send diagnostics

Status: diagnostic source prepared for clean compile/build verification; no runtime conclusion until the user tests the produced JAR.

## Why this build exists

Issue-A reflection redirection is now strongly weakened as the cause of the reported synchronized-speaker coloration. In the user's standalone A/B, disabling reflected-position redirection did not remove the coloration; reflection ON only gave more accurate spatial direction. In a reproduced synchronized snapshot, the active sources also had zero reflected-position offset.

Two hypotheses remain intentionally separate even though they can be measured during the same playback:

1. **micro-desync hypothesis** — one synchronized OpenAL source may be slightly ahead of or behind its peers, producing comb-filter/phase-like brightness, smear, or fake-reverb perception;
2. **correlated acoustic-mix hypothesis** — sources may be sample-aligned but receive materially different direct filtering and reverb-send state, and summing those correlated differently processed copies may create the coloration.

The build measures both at once so the user does not need two separate reproduction sessions. The interpretation remains separate.

## Safety / non-mutation rule

This branch must not change playback timing, PCM data, source gain, source position, reflection behavior, direct filters, reverb-send filters, room scheduling, or sync-group release behavior.

The only runtime additions are read-only diagnostics:

- `EnvironmentSmoother.debugDumpEfx()` now prints its already-applied typed `r0..r3` and `h0..h3` state alongside cutoff/gain;
- `/cchqphysics dump` schedules a sound-thread OpenAL cursor snapshot;
- the cursor snapshot reads `AL_SAMPLE_OFFSET`, `AL_SEC_OFFSET`, source state, attached buffer id, and buffer sample rate;
- reads are performed once in ascending and once in descending source-id order and normalized to the midpoint of the query window to reduce sequential-query skew;
- sources sharing the same OpenAL buffer are summarized with raw and midpoint-normalized frame spread plus millisecond spread.

No cursor value is ever written.

## Test procedure

Use the synchronized setup where the weird brightness/reverb/spatial coloration is clearly audible.

1. Keep the player and speakers stationary.
2. Start the synchronized playback normally.
3. While the unwanted coloration is audible, run `/cchqphysics dump`.
4. Repeat the dump two more times a second or two apart without restarting playback or moving.
5. Send `latest.log` and describe whether the coloration stayed audible during all three dumps.

The same three dumps are enough for both hypotheses.

## Timing interpretation

Look for:

`[phase5/syncdiag/timing]`

The strongest timing evidence is a **repeatable** non-trivial `midpointSpreadMs` between sources sharing one OpenAL buffer. A tiny or unstable few-frame difference can be query/mixer quantization and is not enough by itself to claim audible desync. The logged `querySpanUs`, raw spread, midpoint-normalized spread, and repeated snapshots must be considered together.

This is especially useful because the maintained sync path starts each collected source vector through `alSourcePlayv(...)`; this diagnostic checks the actual live playback cursor instead of assuming that the logical group release proves sample alignment.

## Acoustic interpretation

Look for the per-source lines:

`[phase5/source-efx]`

They now include:

- `r0 r1 r2 r3` — applied smoothed reverb-send gains;
- `h0 h1 h2 h3` — applied smoothed send low-pass/high-frequency factors;
- `cutoff` and `gain` — applied direct-filter state.

If cursor spread stays effectively zero while the EFX vectors differ strongly between synchronized copies at the exact time the coloration is audible, the acoustic-mix hypothesis gains weight.

If cursor spread is repeatably several milliseconds or otherwise clearly beyond the query window/noise while the coloration is audible, investigate synchronization first before changing acoustic algorithms.

If both are present, do not tune either system yet: isolate timing first because timing error can itself alter the perceived result of any acoustic A/B.

## Frozen references preserved

- Phase 4 frozen branch: `79eed29767343ee34022e8f6268b386f75e84c9f`
- Phase 5 known-good candidate: `44612192d875e43ecef66ca51798cab7adb17020`
- Reviewed Issue-A runtime source: `973f1df7dad886fb0f5fffd4264015fecac2e786`

This diagnostic branch is intentionally separate from all frozen refs and contains neither rejected V1 nor frozen V2 spectral compensation.
