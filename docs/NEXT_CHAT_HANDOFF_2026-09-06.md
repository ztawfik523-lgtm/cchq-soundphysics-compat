# CC:HQ Sound Physics Compat — authoritative next-chat handoff

**Handoff date:** 2026-09-06 (Africa/Cairo)

This document is the first file the next ChatGPT session should read. It intentionally uses concrete names, commits, hashes, test values, accepted/rejected behavior, and the exact next action. Do not infer the project state from older Phase 1–4 documents before reading this file.

Repository:

`https://github.com/ztawfik523-lgtm/cchq-soundphysics-compat`

Current working branch:

`phase5-v7-1-lifecycle-state-finish`

The user explicitly asked to stop implementation here because the previous chat was reaching its context limit. Do not continue coding until the user asks in the new chat.

---

## 1. User decision-making instruction

Until the user explicitly reverses it, the assistant should make implementation/tradeoff decisions itself instead of repeatedly asking the user to choose.

The requested decision style is the **middle ground**:

- do not automatically choose the safest/most conservative option if it causes unnecessary complexity or leaves obvious issues unfixed;
- do not choose the riskiest/aggressive option just for performance or elegance;
- prefer a bounded, maintainable solution with good correctness and reasonable performance;
- when a minor implementation detail is not user-facing, decide it directly.

This instruction does **not** override the hard project constraints below.

---

## 2. Hard project constraints

These are non-negotiable unless the user explicitly changes them.

1. **No more acoustic features or sound tuning.** The user said: `forget it, no more features or adjustments, just performance and fixing and finishing touches`.
2. **V7.1 acoustic behavior is frozen.** Known imperfections are accepted unless they become a correctness/stability bug.
3. **HF50 synchronized-speaker behavior is frozen and approved.** Do not retune it.
4. **No Lua changes.**
5. **Do not merge to `main` unless the user explicitly asks.** `main` is intentionally untouched.
6. Preserve the exact eligibility expression with bitwise `&`, not short-circuit `&&`:

   `Beta9Optimizer.isAudibleAndRecord(state.sourceId) & beta9EligibleReal(state, now)`

7. Do not intentionally alter PCM sample position, OpenAL playback clock, buffer offsets, or sync-group timing.
8. Keep `SoundSource.BLOCKS` distance behavior.
9. Keep the progressive direct geometry: center + 8 inner + 8 outer paths; approved full/partial refresh behavior.
10. Keep private per-source EFX and mandatory direct/aux EFX reattachment on every actual environment application.
11. Do not create private EFX before the source is PLAYING/PAUSED eligible.
12. Do not cancel or replace SPR `calculateOcclusion()`.
13. No worker-thread SPR world/geometry raycasts.
14. Preserve strict source lifetime/generation semantics.
15. Preserve Beta10 exact direct reuse and bit-identical OpenAL write suppression.
16. Preserve Beta11 same-clone room-ray memo. Cross-clone room reuse remains telemetry-only.
17. Never claim the assistant heard/tested game audio. The user performs runtime listening/testing. Build metadata should continue to say `game_launch_performed=false` unless a human runtime test is explicitly reported by the user.
18. User dislikes manual source-ID diagnostics. Prefer global/automatic/no-ID diagnostics.
19. When logs are uploaded, reconstruct the **entire current test sequence** before drawing conclusions. Do not inspect one convenient snippet and guess.

---

## 3. Environment and authoritative original baseline

Pinned stack:

- Java 21
- Minecraft 1.21.1
- NeoForge 21.1.248
- ModDevGradle 2.0.144
- Gradle 9.2.1
- CC:Tweaked 1.120.2
- CC:HQ Speakers runtime artifact: Modrinth project `ygA78R8l`, version `u5PEI5Ax`; runtime reports `1.1.4-neoforge-1.21.1`
- Sound Physics Remastered 1.21.1-1.5.1, Modrinth version `Dd2tmpsk`
- Cloth Config 15.0.140 / Curse file `5729127`
- runtime uses untouched SPR
- compilation uses an isolated access-transformed SPR copy produced by `prepareSprCompileJar`

Original authoritative historical artifact:

`cchq_soundphysics_compat-0.1.0-beta11-hotfix3.jar`

SHA-256:

`83500f182fc9829aa1a5a51fbfa11ba6cdfb645699b25d1c445167666dabc1ef`

Phase 4 reconstruction parity was completed before Phase 5 feature/runtime work:

- 60/60 class paths exact
- 60/60 structural ABI exact
- 69/69 `ConstantValue` entries exact
- zero bootstrap/string-concat recipe mismatches
- 11/11 Mixin/accessor annotation sets reconciled
- 5/5 runtime resources byte-exact
- 550 methods audited
- no unresolved proven behavior drift at Phase 4 closure

Final audited Phase 4 code/build head:

`98e7dedb7ecf6fda22008b084b6bb41956edff78`

Phase 4 frozen refs:

- `phase4-hotfix3-parity` -> `79eed29767343ee34022e8f6268b386f75e84c9f`
- `archive-phase4-hotfix3-parity` -> same

Do not move existing archive refs.

---

## 4. Phase 5 accepted synchronized-speaker HF fix

The synchronized-speaker spectral-skew issue was isolated and fixed before diffraction work.

The accepted configurable HF50 source commit is:

`62d3a7a0a176c901402b913946d98f3cb455a8f4`

Frozen/archive refs include:

- `archive-phase5-hf50-config-runtime-approved` -> `62d3a7a0a176c901402b913946d98f3cb455a8f4`
- `archive-phase5-sync-hf50-configurable-validated` -> documentation head `56786cd447f5c65e08a9bc9556b3f08b709e2118`

Approved config defaults:

- enabled = true
- dark_source_cutoff = 0.35
- peer_clear_cutoff = 0.75
- min_peer_gap = 0.40
- clarity_floor_ratio = 0.50
- max_cutoff_lift = 0.55

Accepted HF50 test JAR SHA-256:

`fe894a42eebeea37e77f63e9acf65df22bdac72897fb6ac1eb9def198dcd032a`

The user explicitly approved it with: `all good, lets move on`.

**Do not alter HF50.**

---

## 5. Phase 5 diffraction history: what was rejected vs accepted

The elevation/opening problem was that straight direct rays could report absurd muffling when a plausible path existed around an opening/edge. Several prototypes were tested.

### V3

Archive/runtime-approved historical checkpoint:

`b1b97fc7050163e70fbb039f7b76e51eef3c50d0`

It improved the target cases but was too special-case.

### V4

Branch: `phase5-opening-diffraction-test-v4`

Head: `053af2cca0dd84e98956fec6a860cb46e860630e`

It used a cheap opening topology scan and max two SPR-verified portal candidates. Performance was good, but diagonal clipping made the side verifier unreliable.

### V5

Branch: `phase5-opening-diffraction-test-v5`

Head: `595b4736d3f861e5d67c8b6a64f2cf9297a7f4fe`

It used split lower/upper aperture waypoints. Near sound improved, but the replacement model had the wrong acoustic shape.

### V6

Source commit:

`f72c106cd94108a418cb6d64cdeb4eb09a68de58`

V6 changed the concept from replacement to a bounded secondary two-band aperture-energy contribution. It retained the direct 17-ray result and added energy from an opening. V6 exposed that the portal contribution did not fall with distance because explicit aperture spreading was missing.

### V7 — rejected

Source commit:

`ae3c4ef55173a5be527f114e18af8de8bc43d315`

V7 bundled spreading with a leg-attenuation rewrite. The user reported: `previous iteration was way better, what did u change?`

**V7 is rejected. Do not revive its legLow/legHigh rewrite.**

### V7.1 — accepted and frozen

V7.1 kept V6 leg/spectral math and added only aperture spreading.

Frozen V7.1 commit:

`ffcf5f6e05d85b69f1f1dff8cfae1b082b71604d`

Frozen refs:

- `phase5-diffraction-v7-1-runtime-approved`
- `archive-phase5-diffraction-v7-1-runtime-approved`

Both must point to exactly:

`ffcf5f6e05d85b69f1f1dff8cfae1b082b71604d`

Accepted V7.1 JAR SHA-256:

`30d457c2a52672f893b1076938e2fdea3f41759173dfd843ff652bd490692101`

V7.1 explicit spreading formula:

```java
spread = apertureSpreadScale / (apertureSpreadScale + apertureDistance);
coupling = portalCoupling * activation * horizonFade * spread;
lowAmp = coupling * pathGain * lowDiff;
highAmp = coupling * pathGain * pathCutoff * highDiff;
```

Default `aperture_spread_scale = 3`.

V6 leg/spectral math remains:

```java
legRaw = max(0, lower) + max(0, upper);
rawRatio = legRaw / raw;
pathGain = pow(gain, rawRatio);
pathCutoff = pow(cutoff, rawRatio);
```

Important accepted limitation:

- Opening discovery has a hard radius 8 limit.
- Runtime test showed the transition from roughly radius 8 to 10 could feel a little sudden.
- The user accepted the imperfection because acoustic work was frozen.
- **Do not add radius smoothing or another opening algorithm now.**

Important accepted 3-deep-hole note:

The user said the 3-block-deep hole felt a little more muffled than before but accepted the cost. In the final example the portal was still technically active:

- raw = 5.90625
- center = 6
- directCutoff = 0.002026
- directGain = 0.412327
- horizontal = 7.859
- directDistance = 8.081
- route = 11.381
- delta = 3.300
- openingRadius = 0
- spread = 1
- portalLow = 0.185
- portalHigh = 0.140
- cutoff changed about 0.002 -> 0.309
- gain changed about 0.412 -> 0.452

Do **not** describe that test as “diffraction inactive”; telemetry showed the portal contribution was active.

---

## 6. Pre-performance V7.1 benchmark results

The user performed a benchmark with tests 1–6 and intentionally skipped the 12-speaker tests.

A `beta9 extra/perf window=10.xs` line describes the **preceding** ten seconds. A metric window is valid for a marked test only if its entire preceding interval is inside the test's START/END markers.

### Test 1 — 1 source, diffraction OFF, stationary

Strict contained window:

- acoustic CPU: 2.1 ms/s
- SPR load: 0.9 ms/s
- directReal 74
- directReuses 90
- movementResets 0
- sprCalls 10, avg 1.074 ms, max 1.314 ms
- occlusionPaths 90
- portalPaths 0

### Test 2 — 1 source, diffraction ON, stationary

Strict contained window:

- acoustic CPU: 2.6 ms/s
- SPR load: 1.3 ms/s
- sprCalls 10, avg 1.047 ms, max 1.370 ms
- occlusionPaths 94
- portalPaths 4
- portalAvg 0.005 ms, max 0.009 ms
- portalLower 2
- portalCross 2
- topology scans 9
- topology block checks 1764

### Test 3 — 1 source, diffraction ON, moving

Three strict contained windows:

1. acoustic 5.8 ms/s; SPR 2.9 ms/s; movementResets 200; sprCalls 78; portalPaths 28; scans 52; checks 9833
2. acoustic 7.6 ms/s; SPR 4.2 ms/s; movementResets 197; sprCalls 80; portalPaths 29; scans 54; checks 10393
3. acoustic 14.5 ms/s; SPR 12.0 ms/s; movementResets 182; sprCalls 71; SPR max 10.106 ms; portalPaths 20; scans 43; checks 8099

The third window is an SPR timing spike/outlier. Do not call 14.5 ms/s the normal one-source-moving baseline. Typical first two windows were 5.8–7.6 acoustic and 2.9–4.2 SPR.

### Test 4 — 4 sources, diffraction OFF, stationary

Two strict windows:

- 3.8 acoustic / 2.5 SPR ms/s
- 5.6 acoustic / 4.3 SPR ms/s

Average about:

- acoustic 4.7 ms/s
- SPR 3.4 ms/s

### Test 5 — 4 sources, diffraction ON, stationary

One strict contained window:

- acoustic 5.2 ms/s
- SPR 3.6 ms/s
- sprCalls 40, avg 0.971 ms, max 1.972 ms
- portalPaths 10
- portalAvg 0.008 ms, max 0.053 ms
- portalLower 2
- portalCross 8
- topology scans 9
- topology checks 1764

Critical scaling conclusion:

- 1-source ON stationary scans/checks = 9 / 1764
- 4-source ON stationary scans/checks = 9 / 1764

Therefore the opening topology scan is listener-shared and does **not** multiply by source count.

Lower-leg count stayed 2 -> 2, also listener-shared.

Cross-leg count scaled 2 -> 8, correctly per-source.

Portal ray CPU was effectively negligible.

### Test 6 — 4 sources, diffraction ON, moving

Three strict windows:

1. acoustic 14.4 ms/s; SPR 6.8; scans 55; checks 10404; portalPaths 44
2. acoustic 13.7 ms/s; SPR 6.9; scans 57; checks 10952; portalPaths 39
3. acoustic 14.6 ms/s; SPR 7.4; scans 61; checks 11725; portalPaths 36

Average:

- acoustic about 14.23 ms/s
- SPR about 7.03 ms/s

Important conclusion:

**Moving cost is dominated by the existing progressive/direct/SPR machinery, not the V7.1 portal rays.** Portal timing was only roughly 0.011–0.018 ms/s in those moving windows.

The 12-speaker test was skipped and is not required to answer whether V7.1 diffraction has a bad source-count scaling problem. It does not appear to.

---

## 7. Performance pass after V7.1 freeze

The user then said: `freeze and do performance`.

Performance branch:

`phase5-v7-1-performance-pass`

It was created from the exact frozen V7.1 commit:

`ffcf5f6e05d85b69f1f1dff8cfae1b082b71604d`

Final clean performance branch head:

`962eab8b052466ca984496a7dec0767dc65803f4`

The assistant originally considered a small multi-cell topology cache, then deliberately rejected it because returning to A->B->A could reuse older topology state and subtly alter sound timing. That would violate the frozen-acoustics rule.

Only behavior-neutral housekeeping/allocation changes were kept:

- `CACHE_PRUNE_INTERVAL_NS = 1_000_000_000L`
- cache pruning is normally at most once per second
- pruning still happens immediately when the existing soft limits are exceeded
- stale lookup TTL/recheck rules are unchanged
- `clear()` resets prune timing
- avoid allocating candidate lists when the branch can immediately return no candidates
- implicit-open-top uses `new ArrayList<>(1)`
- explicit candidate list pre-sizes to `topology.candidates.length`
- no radius/ray/coupling/spread/HF50/geometry/timing changes

Existing soft limits remain:

- lower leg cache: 128
- cross leg cache: 512

Successful validation run:

`33957036689`

Performance test JAR SHA-256:

`b7945374fc95935a3c951e660efb660bbcc777744395e81849b5abf680592b41`

Class count: 81.

The performance pass did **not** change topology scan counts or block-check counts by design. It targets map traversal/allocation/housekeeping overhead. Do not falsely expect scans/checks themselves to decrease.

A dedicated user runtime comparison of this performance candidate was not completed before the lifecycle/state audit. The later lifecycle candidate contains this performance pass, so future runtime logs should be judged against the pre-performance V7.1 benchmark values above while remembering the expected improvement is modest/noisy rather than dramatic.

---

## 8. Lifecycle/state audit and fixes completed immediately before this handoff

The user gave the assistant permission to make middle-ground implementation choices. The chosen lifecycle architecture was:

- one authoritative blocking teardown for normal sound-engine stop/reload flow;
- a dedicated blocking emergency teardown before OpenAL destruction;
- remove redundant destroy/reload injections rather than keep multiple competing teardown paths.

Working branch:

`phase5-v7-1-lifecycle-state-finish`

Performance baseline for this branch:

`962eab8b052466ca984496a7dec0767dc65803f4`

### Proven lifecycle bug 1: world/dimension identity

Old logic could treat `Minecraft.level != null` as enough continuity even after the actual `ClientLevel` object changed. That risks carrying session state between disconnect/rejoin or dimension/world replacement.

Fix in `CompatAudioManager`:

- add `private static ClientLevel trackedLevel;`
- on transition from a tracked level to `null`, clear tracked level, invalidate the session, and stop compat sources
- when `trackedLevel != current level`, invalidate and stop old-session sources before accepting the new level

This is a correctness fix, not acoustic tuning.

### Proven lifecycle bug 2: asynchronous teardown vs SoundEngine shutdown

The previous lifecycle path could schedule compat cleanup asynchronously and then vanilla could continue destroying/reloading the sound executor/OpenAL state. Cleanup racing behind sound-engine teardown is unsafe.

Fix:

- `resetForSoundEngine()` calls `invalidateSession()` then uses blocking sound-thread execution
- `stopAllCompatSources()` calls `invalidateSession()` then uses blocking sound-thread execution
- `onSoundThreadBlocking(Runnable)` uses `soundExecutor().executeBlocking(task)`

Current lifecycle mixin hooks are exactly:

- `pause` HEAD -> `pauseCompatSources()`
- `resume` HEAD -> `resumeCompatSources()`
- `stopAll` HEAD -> `stopAllCompatSources()`
- `emergencyShutdown` HEAD -> `resetForSoundEngine()`

Redundant direct hooks for `destroy` and `reload` were removed. Normal destroy/reload flows are expected to pass through the authoritative stopAll teardown instead of invoking another compat teardown path.

### Proven lifecycle bug 3: failed-start partial registration leak

`startSource()` registers a generated OpenAL source with `EnvironmentSmoother.register(sourceId)` before all later source configuration/apply/play steps have succeeded.

If setup throws after registration but before the source reaches `ACTIVE`, simply deleting the OpenAL source would leave compat-owned per-source registration state behind.

Fix in the `finally` block for `!installed`:

```java
try { EnvironmentSmoother.unregister(sourceId); } catch (Throwable ignored) {}
```

This happens before stopping/detaching/deleting the raw OpenAL source.

`EnvironmentSmoother.unregister` also removes the associated progressive occlusion, position, SoundPhysicsBridge source registration, sync-source membership and private EFX state, so this closes the discovered partial-start stale-state path.

### Lifecycle source commits and final clean branch head

Initial two-file lifecycle source fix:

`24e41b49f8ccd5b32e484b90def8f50c3767c8d9`

Failed-start cleanup source commit:

`cfcc122da3be03171377f14717ae30b4c6bbb696`

Final clean branch head before this documentation-only handoff update:

`be03d30efe98ca03bdf27764bcea567df5ef3875`

Compared with performance baseline `962eab8...`, that final clean production tree changes only:

1. `src/main/java/dev/cchqphysics/compat/audio/CompatAudioManager.java`
   - 32 additions, 12 deletions
2. `src/main/java/dev/cchqphysics/compat/mixin/SoundEngineLifecycleMixin.java`
   - 0 additions, 10 deletions

No acoustic/config/Lua files differ in that lifecycle pass.

### Lifecycle validation

Successful validation run:

`33962380234`

Job:

`101296383519`

Passed:

- Java 21 setup
- Gradle wrapper validation
- ancestry/frozen-ref checks
- lifecycle-fix assertions
- exact `&` eligibility assertion
- no changes to frozen acoustic files compared with performance baseline
- no Lua changes
- reconstruction classpath verification
- resource wiring verification
- clean
- Java 21 compile
- JAR build
- JAR inspection
- artifact upload

Artifact ID:

`9968357487`

Artifact ZIP digest:

`sha256:3668d1fe38b64591ba6854e8937290ee4544c22bca12565682a0c7abd1394541`

Built JAR name inside artifact:

`cchq_soundphysics_compat-0.1.0-beta11-phase5-v7-1-performance-test.jar`

Built JAR SHA-256:

`6d0fa98ee6c76d23a3e0764501d16dc5c993149e0de77181cdab6fc0a9abdc18`

Class count:

81

Build metadata explicitly recorded:

- `world_identity_reset=true`
- `normal_stopall_blocking=true`
- `destroy_reload_redundant_hooks_removed=true`
- `emergency_shutdown_blocking=true`
- `failed_start_unregister=true`
- `acoustic_model_changes=false`
- `lua_changes=false`
- `game_launch_performed=false`

Important naming note:

`gradle.properties` still has:

`mod_version=0.1.0-beta11-phase5-v7-1-performance-test`

That is intentionally not release-cleaned yet. A runtime startup string saying `performance-test` does **not** mean the lifecycle fixes are missing; the lifecycle candidate was built from the lifecycle branch but retains the old internal test version until release cleanup.

### CI false starts during lifecycle audit

Do not misinterpret the failed workflow attempts as source failures.

One early validation guard was overly strict and was corrected.

Another workflow attempted `clean compileJava jar` in one Gradle invocation and Gradle 9.2.1 reported an execution-plan deadlock (`Unable to make progress running work`). The source had already passed invariants. The final validator split classpath/resource/clean/compile/jar into separate invocations and succeeded completely in run `33962380234`.

The final successful validation is the authority.

---

## 9. Current source-vs-documentation heads after this handoff

Before the documentation update requested at chat shutdown, the clean lifecycle code head was:

`be03d30efe98ca03bdf27764bcea567df5ef3875`

The documentation commit(s) made for this handoff will advance `phase5-v7-1-lifecycle-state-finish` beyond that SHA. Therefore:

- use `be03d30e...` as the exact **clean lifecycle source checkpoint**;
- use the latest branch head as the **documentation head**;
- verify any later source diff against `be03d30e...` if you need to prove no code changed during handoff documentation.

`main` must remain untouched unless the user explicitly asks for a merge.

The immutable V7.1 acoustic archive must remain:

`archive-phase5-diffraction-v7-1-runtime-approved` -> `ffcf5f6e05d85b69f1f1dff8cfae1b082b71604d`

---

## 10. Exact runtime test that was pending when the chat stopped

The lifecycle candidate has **not yet been runtime-tested by the user** after the new lifecycle fixes.

Do not claim lifecycle runtime success yet.

When the user is ready, test the JAR whose SHA is:

`6d0fa98ee6c76d23a3e0764501d16dc5c993149e0de77181cdab6fc0a9abdc18`

Run one concrete lifecycle/state sequence:

1. Start a speaker.
2. Stop it.
3. Start it again.
4. While playing, pause Minecraft and resume.
5. Trigger a normal stop-all path and restart playback.
6. Trigger the Minecraft sound/resource reload path while playback has been active.
7. Disconnect to menu, rejoin, and play again.
8. Change dimension after playback has been active, then play again in the new dimension.
9. Leave four speakers playing for several minutes while moving around.
10. Save/upload both `latest.log` and `debug.log` from that exact run.

What to look for:

- crash
- OpenAL/EFX errors
- stuck or permanently missing playback after restart/reload
- source surviving a stop/disconnect/world change when it should not
- source/state count accumulating after repeated stop/start/rejoin
- stale sync groups
- stale cache/source IDs after dimension/world transition
- repeated private EFX failure
- obvious performance regression

Do **not** require source-ID manual diagnostics unless there is no other way to isolate a new bug.

When logs are uploaded, first identify:

- exact compat JAR/version loaded
- duplicate compat JARs (there should be only one)
- CC:HQ, SPR, NeoForge and Java versions
- full timestamp sequence of play/stop/reload/disconnect/rejoin/dimension actions if markers exist
- all relevant lifecycle warnings/errors
- performance windows only when their preceding interval belongs to the relevant test period

If runtime is clean and the user reports no sound regression, accept lifecycle/state and proceed to release cleanup/final audit. Do not reopen acoustic design.

---

## 11. What comes after the pending lifecycle runtime test

If the lifecycle candidate passes runtime, the remaining work is release finishing, not another feature phase.

Recommended order:

1. **Release cleanup**
   - replace `phase5-v7-1-performance-test` internal version/name with final release naming chosen for this mod;
   - normalize config filenames if needed without changing values/behavior;
   - remove experimental/test wording from startup logs where appropriate;
   - ensure no temporary validation helper/workflow files remain in the production tree;
   - update README/status/known-limitations docs.

2. **Final source audit**
   - prove HF50 files/math unchanged;
   - prove V7.1 diffraction files/math unchanged from frozen behavior except already-approved performance housekeeping inherited from `962eab...`;
   - prove exact `&` eligibility invariant;
   - prove no Lua changes;
   - prove no PCM/OpenAL timing/source-position tuning changes;
   - prove lifecycle fixes are present;
   - prove Java 21 build and resource wiring.

3. **Final packaging inspection**
   - one expected JAR;
   - expected class count/resources;
   - record final SHA-256;
   - record source commit and build run;
   - `game_launch_performed=false` for CI metadata; separately record the user's runtime validation result.

4. **Final immutable source/archive checkpoint**
   - create a new final release/archive ref only after validation;
   - do not move existing archives;
   - do not merge to `main` unless explicitly requested.

5. Optional final stress test only if needed:
   - multi-source stationary/moving
   - diffraction off/on
   - rapid start/stop/restart
   - pause/resume
   - resource reload
   - disconnect/rejoin
   - dimension transition
   - sustained playback

Do not add more diffraction radius logic, smoothing, reflection changes, HF tweaks, adaptive quality, room model work, new cache semantics, or future Beta12/Beta13 features during finishing.

---

## 12. Known accepted limitations that should be documented, not “fixed” now

1. V7.1 opening discovery has a hard configured radius ceiling of 8, so the transition outside the opening search radius can be noticeable.
2. The 3-deep-hole result can be slightly more muffled than an earlier prototype even while portal energy is active. The user accepted this.
3. Movement is still much more expensive than stationary playback because the existing progressive direct/SPR machinery refreshes heavily while moving. V7.1 portal ray work itself is tiny.
4. Performance housekeeping optimization is deliberately small because more aggressive topology caching risked changing acoustic timing/state.
5. The current internal version still says `phase5-v7-1-performance-test` until release cleanup.

These are not invitations for another acoustic iteration.

---

## 13. Useful exact refs/checkpoints

Historical/frozen refs that should not be casually moved:

- `phase4-hotfix3-parity` -> `79eed29767343ee34022e8f6268b386f75e84c9f`
- `archive-phase4-hotfix3-parity` -> same
- `phase5-test-candidate-1` -> `44612192d875e43ecef66ca51798cab7adb17020`
- `archive-phase5-hf50-config-runtime-approved` -> `62d3a7a0a176c901402b913946d98f3cb455a8f4`
- `archive-phase5-sync-hf50-configurable-validated` -> `56786cd447f5c65e08a9bc9556b3f08b709e2118`
- `archive-phase5-diffraction-v3-runtime-approved` -> `b1b97fc7050163e70fbb039f7b76e51eef3c50d0`
- `phase5-diffraction-v7-1-runtime-approved` -> `ffcf5f6e05d85b69f1f1dff8cfae1b082b71604d`
- `archive-phase5-diffraction-v7-1-runtime-approved` -> same

Performance pass clean head:

`962eab8b052466ca984496a7dec0767dc65803f4`

Lifecycle clean source checkpoint:

`be03d30efe98ca03bdf27764bcea567df5ef3875`

Current lifecycle candidate JAR SHA:

`6d0fa98ee6c76d23a3e0764501d16dc5c993149e0de77181cdab6fc0a9abdc18`

---

## 14. Next-chat behavior

When starting a new chat:

1. Read this entire document first.
2. Read `RECONSTRUCTION_STATUS.md` and `docs/BETA11_RECONSTRUCTION_HANDOFF.md` only after this file; they are updated to point here.
3. Inspect the current branch before changing anything.
4. Do not reconstruct Phases 1–4 again.
5. Do not propose new acoustic features.
6. Do not ask the user to re-explain why V7.1/HF50 were accepted.
7. If the user uploads lifecycle runtime logs, analyze those logs before coding.
8. If the runtime test passes, proceed directly to release cleanup/final audit.
9. If a real lifecycle/stability bug appears, make the narrowest behavior-neutral correctness fix that solves it, then rebuild and retest.
10. Continue making reasonable middle-ground choices yourself until the user explicitly revokes that instruction.

**Exact next state:** implementation is paused; documentation handoff is being completed; lifecycle candidate awaits user runtime validation.