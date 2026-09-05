# Phase 5 current status — 2026-09-05

Phase 5 remains **IN PROGRESS**. Core runtime validation is already strong and all frozen rollback/parity refs remain preserved. The active investigation has moved past reflected-position Issue A into a read-only synchronized timing + acoustic-send diagnostic.

## Preserved baselines

- Phase 4 Hotfix3 parity: `phase4-hotfix3-parity` → `79eed29767343ee34022e8f6268b386f75e84c9f`
- archival Phase 4 ref: `archive-phase4-hotfix3-parity` → same frozen head
- known-good Phase 5 runtime candidate: `phase5-test-candidate-1` → `44612192d875e43ecef66ca51798cab7adb17020`
- reviewed Issue-A runtime source: `phase5-issue-a-test-candidate-2` → `973f1df7dad886fb0f5fffd4264015fecac2e786`
- V2 frozen experiment: `phase5-mix-v2-test-candidate` → `ab1e1e70a13ebb6f3dadd30581b069f06a15142a`

No current diagnostic changes any frozen ref.

## Issue A — reflection result

Verified Issue-A JAR:

- identity: `0.1.0-beta11-phase5-issuea-test`
- SHA-256: `d649f14cdce89db21a79c396dbdecca681daf3d0389dc794a7ad52929f8c8451`
- source: `973f1df7dad886fb0f5fffd4264015fecac2e786`
- workflow run `33935819269`: **SUCCESS**

User runtime evidence on 2026-09-05:

- standalone source `2` reached a real reflected-position redirect with `requestedRedirect=true`, `redirectActive=true`, `offset=2.50`, and occlusion about `3.56`;
- toggling reflection ON/OFF did **not** produce/remove the hard-to-name brightness/reverb/coloration;
- the user reported reflection ON gave **more accurate spatial direction**;
- during a synchronized reproduction snapshot, all four active sources had zero reflected-position offset / no active requested redirect.

Interpretation: reflected-position redirection is **strongly weakened/exonerated for the reproduced coloration** and currently appears useful for localization. Do not remove or retune reflection on this evidence.

## Clarified remaining sync theory

The user's original "wrong syncing" theory means **micro-desync**: one synchronized copy may be slightly ahead of or behind the others, creating comb-filter/phase-like brightness, smear, or fake-reverb perception.

This is distinct from the second remaining hypothesis: synchronized copies may be time-aligned but receive materially different direct filters and reverb-send states.

The two hypotheses should be measured in the **same playback session** but interpreted independently:

1. **micro-desync hypothesis** — actual OpenAL playback-cursor skew;
2. **correlated acoustic-mix hypothesis** — aligned copies with materially different direct + reverb-send processing.

If both appear, timing is isolated first because timing error changes the perceptual result of any acoustic A/B.

## Current diagnostic branch

Branch:

`phase5-sync-acoustic-diagnostics`

Exact runtime-source commit:

`95bd4b06b78786d4f7b1ad33b665f4685e45a54b`

Identity:

`0.1.0-beta11-phase5-syncdiag-test`

The branch is based on the verified Issue-A/docs lineage and preserves all known-good acoustic/sync behavior. The diagnostic additions are read-only:

- existing `EnvironmentSmoother` typed state now prints applied `r0..r3` and `h0..h3` during `/cchqphysics dump`;
- a sound-thread cursor snapshot reads OpenAL source state, buffer id, buffer sample rate, `AL_SAMPLE_OFFSET`, and `AL_SEC_OFFSET`;
- source cursor reads are performed in ascending and descending order and normalized to the query-window midpoint to reduce sequential-read skew;
- sources sharing one OpenAL buffer receive a frame/ms spread summary.

No playback offset, PCM, source state, gain, position, reflection setting, direct filter, reverb-send filter, room scheduling, or sync release is written by the new diagnostic.

## Verification

GitHub Actions:

- workflow: `Phase 5 synchronized timing and acoustic diagnostics verification`
- run: `33939999239`
- job: `101235407189`
- exact head SHA: `95bd4b06b78786d4f7b1ad33b665f4685e45a54b`
- result: **SUCCESS**
- clean Java compile: **SUCCESS**
- JAR build: **SUCCESS**
- artifact inspection/upload: **SUCCESS**

Artifact:

- name: `cchq-phase5-sync-acoustic-diagnostics`
- artifact id: `9961502178`
- artifact digest: `sha256:d721d21f164ed9ce7652f9973e9f824b932d82a7e2d719be44a50c88a3ee1373`
- JAR: `cchq_soundphysics_compat-0.1.0-beta11-phase5-syncdiag-test.jar`
- independently rechecked JAR SHA-256: `1910778a12219f84e5ad5a71449e353e99f89ef572fb599a3bc79bc568fcdb9e`
- class count: `70`

Embedded build metadata records:

- `audio_behavior_mutation=false`
- `typed_reverb_send_telemetry=true`
- `openal_cursor_telemetry=true`
- `reflection_diagnostics_retained=true`
- `spectral_mix_v1_included=false`
- `spectral_mix_v2_included=false`
- `game_launch_performed=false`

See:

- `docs/PHASE5_SYNC_ACOUSTIC_DIAGNOSTICS.md`
- `docs/PHASE5_SYNC_ACOUSTIC_BUILD_RECORD.md`
- `docs/CHATGPT_HANDOFF_2026-09-05_SYNC_DIAG.md`

## Immediate user runtime test

Use the synchronized setup where the unwanted brightness/reverb/spatial coloration is clearly audible.

1. Keep player and speakers stationary.
2. Start synchronized playback normally.
3. While the coloration is audible, run `/cchqphysics dump`.
4. Repeat `/cchqphysics dump` two more times, about 1–2 seconds apart, without moving or restarting playback.
5. Send `latest.log` and state whether the coloration remained audible during the three snapshots.

No reflection toggles and no source-ID selection are required for this test.

Interpretation uses:

- `[phase5/syncdiag/timing]` for actual cursor spread;
- `[phase5/syncdiag/cursor]` for per-source cursor estimates;
- `[phase5/source-efx]` for `r0..r3`, `h0..h3`, direct cutoff and gain.

No runtime conclusion should be recorded until the user performs this test.

## Separate elevation / diffraction issue

The open-top-hole / vertical-separation over-occlusion problem remains separate. It is a straight-ray/diffraction limitation and is not included in the synchronized timing/acoustic diagnostic.

## Closure rule

Do not mark Phase 5 **COMPLETE / RECHECKED** until the synchronized-coloration cause is resolved or accepted as a limitation, the elevation decision is made, the V2 decision is made, and the final maintained candidate is frozen and verified.

Do not merge to `main` without explicit user approval.
