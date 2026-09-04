# Phase 5 runtime validation

Status: **RUNTIME CORE PASSED / FINAL OPTIONAL-FEATURE RETEST PENDING**

Phase 5 must not be marked COMPLETE until the optional synchronized occluded-source suppression feature receives its final user listening test. All core/runtime parity behavior described below has already been exercised successfully.

## Candidate history

### Runtime-tested extended candidate

- branch: `phase5-test-candidate-1`
- commit: `44612192d875e43ecef66ca51798cab7adb17020`
- version: `0.1.0-beta11-phase5-test`
- CI verification run: `33927205360` — SUCCESS
- JAR SHA-256: `6d782812f7915de1870a8b5ae0f619556e7ec1d24ef2eaffa7b5b225aa00bd93`

### Finalization candidate

- branch: `phase5-finalization`
- version target: `0.1.0-beta11-phase5-final-test`
- adds optional synchronized occluded-source suppression
- suppression feature default: OFF
- default-OFF behavior intentionally preserves the already-runtime-tested candidate behavior

## Tested runtime stack

Observed in user runtime logs:

- Windows 10 amd64
- Microsoft OpenJDK 21.0.7+6-LTS
- Minecraft 1.21.1
- NeoForge 21.1.248
- CC:Tweaked 1.120.2
- CC:HQ Speakers internal runtime version `1.1.4-neoforge-1.21.1`
- Sound Physics Remastered 1.21.1-1.5.1
- Cloth Config 15.0.140
- OpenAL Soft
- EFX available with 4 auxiliary sends

## Startup and Mixin/runtime wiring

Passed:

- compat mod discovered with the intended Phase-5 test identity;
- Java-21 Mixin compatibility initialized;
- compat startup completed without verifier/linkage/classloading failure;
- HQ receive/stop hooks applied;
- SPR environment, occlusion memo, room-ray memo and position hooks applied;
- advanced config registered;
- Cloth Config path available;
- Sound Physics initialized normally;
- EFX extension recognized;
- four auxiliary sends and four SPR aux slots initialized;
- sound engine started normally.

No compat-caused startup crash was observed.

## Playback and source lifecycle

Passed across real runs:

- ordinary HQ whole-file playback;
- 12 concurrent compat sources;
- 2-source scenario;
- 4-source scenario;
- source registration/generation tracking;
- source cleanup after playback/disconnect;
- fresh source creation after prior source groups finished;
- integrated-server save/disconnect/shutdown without compat crash.

## Direct occlusion / movement / transitions

User exercised and subjectively confirmed correct behavior for:

- open-air playback;
- thick-wall obstruction;
- movement while obstructed/open;
- doorway approaches and crossings;
- return from blocked to open;
- direction/spatial behavior;
- reverb/reflection behavior.

Telemetry showed expected dynamic behavior:

- progressive full and partial probe refreshes;
- clearing-sentinel candidates and confirmations;
- immediate direct/room refreshes on confirmed transitions;
- transition latency tracking;
- higher probe/ray load during movement and geometry changes;
- lower acoustic cost and greater reuse while stationary.

## Private EFX isolation

Passed.

Observed private EFX chains were created independently for each compat source. In a later four-speaker test the per-source acoustic states were simultaneously different, including one fully clear source and several heavily occluded sources. This demonstrates that a clear source was not inheriting another source's low-pass state.

Critical invariant verified repeatedly:

```text
efxApplies == efxReattachPasses
```

across the runtime windows, including the 12-source stress scenario. Therefore required direct/aux EFX reattachment was not optimized away.

No private-EFX creation failure or failure-triggered native-SPR fallback was observed in the supplied runtime logs.

## EFX reset command

`/cchqphysics reset_efx` was exercised successfully during playback.

The user heard a very short, quiet static artifact only when explicitly issuing this debug command. Logs show the command destroys the live private EFX chains and then recreates them shortly afterward. Playback recovered normally and the artifact did not occur during ordinary playback.

Disposition: **expected debug-command side effect; not a normal-runtime failure.**

## Cache/reset diagnostics

Exercised successfully:

- `/cchqphysics dump`
- `/cchqphysics reset_caches`
- `/cchqphysics refresh_rooms`
- `/cchqphysics reset_efx`

Snapshots reported active/eligible sources, acoustic state, EFX state and optimizer state without stopping normal playback.

## Beta9 / Beta10 / Beta11 behavior

Passed under real load:

- Beta9 direct reuse;
- stable/relevance room backoff;
- adaptive load-factor response;
- movement resets;
- Beta10 exact direct-ray sharing;
- bit-identical OpenAL write suppression telemetry;
- Beta11 same-clone room-ray memoization;
- cross-clone reuse-potential telemetry only.

The room-ray hit rate varied appropriately with scene stability. Cross-clone would-reuse counts were often high, which supports future optimization research but does not authorize unsafe cross-clone reuse in the maintained default.

## Performance observations

The 12-source startup/load window included transient spikes, including an SPR maximum in the high-teens of milliseconds and a scheduler queue spike near 95 ms. The system then settled substantially.

Under sustained 12-source movement the mod continued processing successfully without crash or invariant failure. In the later 2-source scene, acoustic/SPR cost and queue delays were substantially lower.

No performance observation in the supplied logs demonstrated a correctness failure requiring a Phase-5 reconstruction fix.

## Synchronized start behavior

User subjectively reported synchronized speakers starting together with no strange timing artifact.

Observed compat sync telemetry showed incomplete declared groups such as:

- 2 actual compat sources for an expected group size of 3;
- 4 actual compat sources for an expected group size of 5.

In both cases the Hotfix3 partial-group grace behavior started all actual arrived compat sources together after the grace interval. No physical speaker was reported missing or silent.

Disposition: the declared HQ group size appears not to map one-to-one to compat audio-source count in these observed payloads. The partial-flush path is therefore required compatibility behavior and must be preserved.

## Multi-speaker perceptual mixing observation

With several synchronized copies of the same track, the user reported that one speaker could feel dominant and that the combined mix could still sound muffled even while one unobstructed speaker was present.

Per-source logs ruled out shared-filter contamination: one source was fully clear while other sources independently had stronger cutoff/gain attenuation.

Interpretation: several synchronized blocked copies can still contribute substantial low-frequency energy while the clear copy contributes the full spectrum. Their summed result can perceptually skew muffled even though isolation is correct.

This motivated an **optional post-parity feature** on `phase5-finalization`:

- synchronized occluded-source suppression;
- applies only to active sources sharing both sync-group identity and exact payload key;
- uses each source's own progressive raw occlusion;
- leaves clear sources untouched;
- retains a configurable minimum gain factor;
- default OFF to preserve the already-validated behavior.

## Remaining test before formal closure

One short user test remains:

1. install the `0.1.0-beta11-phase5-final-test` candidate;
2. enable `Reduce occluded synchronized copies`;
3. reproduce the same multi-speaker / mixed blocked-and-clear scene;
4. confirm clear speakers now dominate the combined mix more naturally;
5. confirm no new timing, crackle, dropout, EFX or positional issue;
6. run `/cchqphysics dump` once while the feature is active and return the log.

After this succeeds, this document should be changed to **COMPLETE / RECHECKED**, the final candidate should be frozen, and Phase 5 can be formally closed.
