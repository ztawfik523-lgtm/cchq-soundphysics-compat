# ChatGPT handoff — Phase 5 acoustic-mix isolation

Read first after `docs/CHATGPT_HANDOFF_2026-09-05_SYNC_DIAG.md` if resuming after the 2026-09-05 sync diagnostic test.

## Current conclusion

The user reproduced the hard-to-name synchronized multi-speaker artifact and uploaded `latest.log` + `debug.log` from the verified sync diagnostic build.

Live OpenAL cursor evidence does not support persistent micro-desync: repeated sources sharing one 48 kHz buffer had identical raw playback frames. Later normalized spreads were only about 0.002 ms. The user still described the sound as the same song twice and slightly out of sync.

Reflection was inactive in the captured reproduction: all four position dumps had no reflected point, zero offset, requestedRedirect=false and redirectActive=false.

The strongest remaining evidence is correlated acoustic divergence: one source was heavily occluded with direct cutoff about 0.09, direct gain about 0.71 and h0..h3=0.05 while other synchronized copies were near clear. Later another source became moderately occluded as well.

## New isolated test build

Branch:

`phase5-acoustic-mix-isolation`

Exact runtime source:

`0a60cf63116bad56f4ddb66afbcd5b852e8df1c1`

Verified JAR SHA-256:

`42f9819a2089829650864821e0af2b4cdd443f9886be8c5b8deb5ada69db0554`

Build record:

`docs/PHASE5_ACOUSTIC_MIX_BUILD_RECORD.md`

Commands:

- `/cchqacoustic source <id> sends_off`
- `/cchqacoustic source <id> direct_hf_bypass`
- `/cchqacoustic source <id> auto`

This build retains the cursor telemetry and Issue-A dump paths.

## Interpretation

- if `sends_off` reliably removes/changes the artifact while direct-HF bypass does not: isolate reverb-send gain versus send-HF filtering next;
- if `direct_hf_bypass` reliably removes/changes it while sends-off does not: investigate direct low-pass phase/spectral interaction next;
- if both change it: use complement tests before any final algorithm;
- if neither changes it: do not tune by guesswork; the current acoustic-mix hypothesis is weakened.

Also judge whether the user's reported left-heavy/spatially wrong image changes under each mode.

Do not merge main, do not promote V1/V2, and do not start elevation/diffraction work during this probe.
