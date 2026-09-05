# ChatGPT handoff addendum — Phase 5 sync timing + acoustic diagnostics

Date: 2026-09-05

This file supersedes the **immediate-next-action** portion of `docs/CHATGPT_HANDOFF_2026-09-05.md`. Keep the original handoff for the full reconstruction history and invariants.

## Current state

Phase 5 is still **IN PROGRESS**.

Frozen refs remain:

- Phase 4: `79eed29767343ee34022e8f6268b386f75e84c9f`
- known-good Phase 5: `44612192d875e43ecef66ca51798cab7adb17020`
- reviewed Issue A: `973f1df7dad886fb0f5fffd4264015fecac2e786`
- V2 experiment: `ab1e1e70a13ebb6f3dadd30581b069f06a15142a`

Never merge to `main` without explicit user approval.

## Reflection result now obtained

The user performed the standalone Issue-A reflection A/B with a genuinely redirected source:

- source `2`
- `requestedRedirect=true`
- `redirectActive=true`
- `offset=2.50`
- occlusion about `3.56`

User listening result:

- reflection ON vs OFF did **not** remove/create the hard-to-name coloration;
- reflection ON gave more accurate spatial direction.

A synchronized reproduction snapshot also had zero reflected offsets on all active copies.

Therefore do not chase or remove reflection next. Treat reflected-position coloration as strongly weakened/exonerated for the reproduced problem and preserve reflection behavior unless new contrary evidence appears.

## User clarified the sync theory

"Wrong syncing" means a possible **tiny live playback delay/advance between synchronized copies**, not merely different occlusion values.

Keep two hypotheses distinct:

1. **micro-desync** — one OpenAL source cursor is slightly ahead/behind;
2. **correlated acoustic mix** — cursors are aligned but direct/reverb processing differs substantially between copies.

Measure both in the same playback session because that preserves the exact reproduction state, but analyze the hypotheses independently. If both appear, timing comes first.

## Current diagnostic build

Branch:

`phase5-sync-acoustic-diagnostics`

Exact runtime source:

`95bd4b06b78786d4f7b1ad33b665f4685e45a54b`

Build identity:

`0.1.0-beta11-phase5-syncdiag-test`

Verification:

- run `33939999239`
- job `101235407189`
- result **SUCCESS**
- artifact `cchq-phase5-sync-acoustic-diagnostics`
- artifact id `9961502178`
- artifact digest `sha256:d721d21f164ed9ce7652f9973e9f824b932d82a7e2d719be44a50c88a3ee1373`
- JAR SHA-256 `1910778a12219f84e5ad5a71449e353e99f89ef572fb599a3bc79bc568fcdb9e`
- 70 classfiles

The build contains neither V1 nor V2.

## What changed

Read-only diagnostics only:

- `/cchqphysics dump` now exposes the applied `r0..r3` / `h0..h3` values already stored by `EnvironmentSmoother`;
- it also schedules a sound-thread cursor measurement reading `AL_SAMPLE_OFFSET` and `AL_SEC_OFFSET`, source state, attached buffer id and sample rate;
- cursor reads run ascending then descending and are midpoint-normalized to reduce query-order skew;
- sources that share the same OpenAL buffer get a frame/ms spread summary.

The diagnostic does not write playback offsets or change PCM, sync release, gain, position, reflection, filters, or room logic.

## User test — exact next action

Do **not** ask for a source ID and do **not** ask the user to repeat reflection A/B.

1. Install the syncdiag JAR.
2. Reproduce the 4-speaker synchronized setup where the weird brightness/reverb/spatial coloration is clearly audible.
3. Keep player and speakers stationary.
4. While the coloration is audible, run `/cchqphysics dump`.
5. Run it two more times about 1–2 seconds apart without moving or restarting playback.
6. Ask for `latest.log` plus whether the coloration stayed audible during those three snapshots.

Analyze:

- `[phase5/syncdiag/timing]`
- `[phase5/syncdiag/cursor]`
- `[phase5/source-efx]`

Do not infer micro-desync from one tiny frame difference. Look for repeatable spread beyond query/mixer noise, considering `querySpanUs`, raw spread, midpoint spread and repeated snapshots.

If timing is effectively aligned but `r/h` + direct vectors differ strongly while coloration is audible, pursue the acoustic-mix hypothesis next.

If timing skew is repeatable and clearly non-trivial, isolate synchronization before acoustic tuning.

If both appear, isolate timing first.

## Separate issue

The vertical/open-hole over-occlusion problem remains a separate diffraction/path-around-edge limitation. Do not mix it into this diagnostic.
