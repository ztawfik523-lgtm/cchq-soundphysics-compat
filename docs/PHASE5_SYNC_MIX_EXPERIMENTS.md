# Phase 5 synchronized multi-speaker mix experiments

Status: **V2 REAL-GAME TEST PENDING**

This document records the two post-parity attempts to improve the perceptual mix of several synchronized CC:HQ speakers playing the same content when some sources are clear and others are heavily occluded.

## Stable fallback

The known-good pre-experiment Phase 5 candidate is permanently frozen at:

- branch: `phase5-test-candidate-1`
- commit: `44612192d875e43ecef66ca51798cab7adb17020`

The user already runtime-tested that candidate successfully. It remains the immediate rollback target if any synchronized-mix experiment is rejected.

The Phase 4 Hotfix3 parity state is separately preserved and is not modified by these experiments.

## Problem observed on the known-good candidate

With multiple synchronized copies of the same track, several heavily low-passed/occluded sources can still contribute substantial low-frequency and mid-frequency energy. The sum can therefore sound globally darker/more muffled even when another synchronized speaker has a clear path.

Runtime logs ruled out shared-filter contamination: different OpenAL sources retained independent direct cutoff/gain and private EFX state.

## V1 — amplitude suppression — REJECTED

V1 attempted to reduce the direct source gain of strongly occluded synchronized copies while leaving a clear source untouched.

Real-game testing rejected this design. Although the combined sound became somewhat less muddy, it distorted positional balance: a clear source could dominate the stereo image while blocked/rear sources became unnaturally quiet. In the reported setup this produced excessive left/right skew and made a blocked speaker behind the listener feel almost absent.

Reason for rejection: the original problem was primarily spectral summation, but V1 changed per-source amplitude. OpenAL's spatializer correctly derives left/right balance from source position, listener orientation and source gain; adding large unequal gain multipliers changed the spatial energy distribution.

The V1 branch is retained only as experimental history and must not become the maintained source.

## V2 — spectral-only bounded compensation — PENDING TEST

V2 was branched directly from the known-good candidate rather than from V1.

Source test candidate:

- branch: `phase5-mix-v2-test-candidate`
- commit: `ab1e1e70a13ebb6f3dadd30581b069f06a15142a`

Clean working experiment branch:

- branch: `phase5-mix-v2`
- read-only verifier head used for the reproducible rebuild: `eb821ef34081d507429e1ffbc41a8f581a9aaa1f`

Verified JAR:

- version: `0.1.0-beta11-phase5-mixv2-test`
- SHA-256: `bba8d93e696403ae857dd155db2969c7591886aa1e8734b0b949f1a749c8319c`
- classfiles: 67

Two independent CI builds produced byte-for-byte identical JARs with the same SHA-256.

### V2 design constraints

V2 does **not** modify:

- OpenAL source gain / `AL_GAIN`
- source position / spatial panning path
- `CompatAudioManager` distance-gain behavior
- `DistanceBridge`
- `PositionStabilizer`
- the four private reverb-send filter cutoffs/gains
- synchronization start timing

The CI gate explicitly compares the gain/position path against `phase5-test-candidate-1` and rejects any difference.

V2 changes only the target of the compat-owned **direct low-pass cutoff** after the intrinsic progressive-occlusion result has been calculated.

### Activation conditions

The feature defaults **OFF**.

When enabled, compensation for one source is allowed only when:

1. the source belongs to a live synchronized group;
2. another live source belongs to that same sync group;
3. at least one peer has a valid intrinsic progressive direct cutoff at or above `peer_clear_cutoff`;
4. the source's own intrinsic cutoff is below the calculated bounded floor.

Default values:

- enabled: `false`
- `peer_clear_cutoff = 0.65`
- `clarity_floor_ratio = 0.18`
- `max_cutoff_lift = 0.12`

The adjusted cutoff is:

`max(baseCutoff, min(clearestPeerCutoff * clarityFloorRatio, baseCutoff + maxCutoffLift))`

and is clamped to `[0, 1]`.

Example with a fully clear peer (`1.0`):

- intrinsic cutoff `0.047` -> at most `0.167`
- intrinsic cutoff `0.109` -> at most `0.180`
- intrinsic cutoff `0.199` -> unchanged

This is intentionally conservative. A deeply blocked speaker remains strongly muffled; V2 only tries to stop several extreme low-pass copies from dominating the summed spectral character.

If every synchronized source is blocked and no peer reaches the clear threshold, V2 performs no compensation.

### Debug evidence

`/cchqphysics dump` reports, for each live synchronized source:

- intrinsic cutoff
- synchronized peer count
- clearest peer cutoff
- adjusted cutoff
- applied delta
- feature enabled state

This makes the actual correction observable without changing the source gain or position.

## Required real-game V2 test

Use the same physical four-speaker arrangement that exposed the V1 problem.

Enable `Synchronized Mix (Experimental) -> Compensate synchronized spectral mud` and leave the three default values unchanged.

Judge specifically:

1. Does left/right/front/rear positioning feel like the known-good candidate rather than V1?
2. Is the blocked/rear speaker still audibly present rather than being effectively suppressed?
3. Does the combined synchronized track sound less globally muffled when one speaker is genuinely clear?
4. Do sync, reverb, doorway transitions and ordinary movement still sound normal?

While the group is playing, run `/cchqphysics dump` and preserve `latest.log`.

If V2 is rejected, abandon this branch and return immediately to `phase5-test-candidate-1`. No parity or known-good source state needs to be reverted or reconstructed.
