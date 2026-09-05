# Phase 5 acoustic-mix isolation — build record

Date: 2026-09-05

Status: **STATIC / COMPILE / BUILD VERIFIED — AWAITING USER A/B**

## Evidence from the preceding synchronized diagnostic run

User runtime logs from the `0.1.0-beta11-phase5-syncdiag-test` build showed four active synchronized compat sources sharing OpenAL buffer 2 at 48 kHz.

Timing snapshots:

- first dump: raw spread 0 frames; midpoint spread 0.2086 ms while the query window itself was 525.2 us;
- second dump: raw spread 0 frames; midpoint spread 0.00235 ms;
- third dump: raw spread 0 frames; midpoint spread 0.00210 ms;
- later dump near the user's spatial complaint: raw spread 0 frames; midpoint spread 0.00240 ms.

The user explicitly reported that playback still felt like the same song twice and slightly out of sync while the live playback cursors remained locked. Therefore a persistent OpenAL source-cursor micro-desync is not supported by this run.

The same snapshots showed strong acoustic divergence between correlated copies. In the stable scene, source 3 repeatedly carried approximately:

- direct cutoff 0.088–0.091;
- direct gain 0.707–0.710;
- h0..h3 = 0.050;
- r0 around 0.28 and r1 around 0.11.

Clear peers were near direct cutoff/HF 1.0. Later, source 4 also became substantially occluded with direct cutoff around 0.374 while source 3 remained around 0.091.

All captured Issue-A position dumps in this reproduction reported reflected position `none`, offset 0.00, requestedRedirect=false and redirectActive=false. Reflection redirection therefore was not active during the reproduced doubled/spatial artifact.

The coordinator still received four compat sources for an HQ expected count of eight and used the existing partial flush. The four sources were nevertheless sample-aligned after release, so this metadata/count mismatch is not evidence of the audible doubling in this run.

## Exact runtime source

Branch:

`phase5-acoustic-mix-isolation`

Runtime source commit:

`0a60cf63116bad56f4ddb66afbcd5b852e8df1c1`

Source tree:

`cc9b9d32e0185f1ba43601b7f1e7edbd29682208`

Build identity:

`0.1.0-beta11-phase5-acousticprobe-test`

Parent diagnostic runtime source:

`95bd4b06b78786d4f7b1ad33b665f4685e45a54b`

Frozen authorities remain:

- Phase 4: `79eed29767343ee34022e8f6268b386f75e84c9f`
- known-good Phase 5: `44612192d875e43ecef66ca51798cab7adb17020`
- reviewed Issue A: `973f1df7dad886fb0f5fffd4264015fecac2e786`

## Probe modes

Runtime-only client commands:

- `/cchqacoustic source <sourceId> sends_off`
- `/cchqacoustic source <sourceId> direct_hf_bypass`
- `/cchqacoustic source <sourceId> auto`
- `/cchqacoustic source <sourceId> status`
- `/cchqacoustic status`

`sends_off` changes only the selected source's private auxiliary-send filter gains to zero.

`direct_hf_bypass` changes only the selected source's private direct-filter `AL_LOWPASS_GAINHF` to 1.0. Its calculated direct filter gain is preserved exactly.

Source gain, source position, PCM, playback cursor/timing, sync release, room scheduling, reflection behavior, Beta9/10/11 algorithms and the rest of the acoustic core are unchanged.

Overrides clear when a source unregisters.

## CI verification

Workflow:

`Phase 5 acoustic mix isolation verification`

Run:

`33940602003`

Job:

`101237146215`

Result:

**SUCCESS**

All gates passed, including exact frozen refs, Phase-5 default audit, Hotfix3 ClientConfig preservation, unchanged playback/geometry/sync core from the prior diagnostic runtime source, probe-scope assertions, clean compile, JAR build, artifact inspection and upload.

## Artifact

Artifact id:

`9961702729`

Artifact digest:

`sha256:1e84fae4e16ae7b2427daeb4ab5b2053ab75675c57ecbcf70082a5335243c01a`

JAR:

`cchq_soundphysics_compat-0.1.0-beta11-phase5-acousticprobe-test.jar`

Independently rechecked JAR SHA-256:

`42f9819a2089829650864821e0af2b4cdd443f9886be8c5b8deb5ada69db0554`

Class count:

`73`

Embedded metadata confirms:

```text
source_commit=0a60cf63116bad56f4ddb66afbcd5b852e8df1c1
syncdiag_runtime_source=95bd4b06b78786d4f7b1ad33b665f4685e45a54b
known_good_candidate=44612192d875e43ecef66ca51798cab7adb17020
issue_a_reviewed=973f1df7dad886fb0f5fffd4264015fecac2e786
phase4_frozen=79eed29767343ee34022e8f6268b386f75e84c9f
per_source_sends_off_probe=true
per_source_direct_hf_bypass_probe=true
source_gain_mutation=false
source_position_mutation=false
playback_timing_mutation=false
spectral_mix_v1_included=false
spectral_mix_v2_included=false
game_launch_performed=false
```

## Required user A/B

Reproduce the same scene, run `/cchqphysics dump`, and identify the darkest/most occluded source from its cutoff/HF values rather than assuming source ID 3 survives a relaunch.

Then without moving or restarting:

1. AUTO baseline;
2. selected source `sends_off`;
3. restore `auto`;
4. selected source `direct_hf_bypass`;
5. restore `auto`.

For each state, judge both:

- doubled/slightly-out-of-sync/phasey coloration;
- spatial direction/image.

A dump after each state is preferred.

No final fix should be promoted from this build; it is an isolation probe only.
