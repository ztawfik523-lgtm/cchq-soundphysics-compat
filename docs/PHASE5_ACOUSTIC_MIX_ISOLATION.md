# Phase 5 acoustic-mix isolation probe

Status: source prepared; runtime conclusions require user listening.

## Evidence motivating this probe

The synchronized timing diagnostic session on 2026-09-05 showed four active sources sharing one OpenAL buffer with identical raw playback frames in repeated snapshots. Two clean later snapshots had midpoint-normalized spreads of only about 0.002 ms. The user's doubled/slightly-out-of-sync perception therefore persisted without measurable source-cursor skew.

At the same time, the applied acoustic state differed strongly. One occluded source repeatedly had direct cutoff near 0.09, direct gain near 0.71, and reverb-send HF factors h0..h3 at 0.05 while clear synchronized copies were near direct cutoff/HF 1.0. This makes correlated differently filtered copies the next hypothesis to isolate.

This is not a final acoustic change.

## Runtime-only modes

Commands use a separate client command root:

- `/cchqacoustic source <sourceId> sends_off`
- `/cchqacoustic source <sourceId> direct_hf_bypass`
- `/cchqacoustic source <sourceId> auto`
- `/cchqacoustic source <sourceId> status`
- `/cchqacoustic status`

`auto` is the default and restores the maintained calculation.

### sends_off

For only the selected source, set the four private auxiliary-send filter gains to 0 while leaving:

- direct gain unchanged;
- direct HF cutoff unchanged;
- source gain unchanged;
- source position unchanged;
- playback cursor/timing unchanged;
- room/acoustic calculations running normally.

This asks whether the reverb path from one differently processed correlated copy is causing the perceived doubling/smear.

### direct_hf_bypass

For only the selected source, set the private direct filter's `AL_LOWPASS_GAINHF` to 1.0 while keeping the already-calculated direct `AL_LOWPASS_GAIN` exactly unchanged. Auxiliary sends are unchanged.

This asks whether the strongly different direct low-pass path is causing the phasey/doubled perception while avoiding V1's rejected whole-source amplitude manipulation.

## Recommended first test

Reproduce the same four-speaker scene. Use `/cchqphysics dump` to identify the strongly occluded/dark source (source 3 in the 2026-09-05 captured run, but do not assume IDs persist across launches).

Without moving or restarting playback:

1. listen in `auto`;
2. run `sends_off` for that source, allow roughly one normal acoustic refresh, listen;
3. restore `auto`, listen;
4. run `direct_hf_bypass` for that source, listen;
5. restore `auto`;
6. run `/cchqphysics dump` after each state if practical and return `latest.log` plus the subjective A/B result.

If only `sends_off` changes/removes the artifact, split reverb send gain versus send-HF filtering next. If only `direct_hf_bypass` changes/removes it, investigate direct low-pass phase/spectral interaction. If neither changes it, the hypothesis is weakened and the next probe should avoid guessing.

Per-source probe state is runtime-only and is cleared when the OpenAL source unregisters.
