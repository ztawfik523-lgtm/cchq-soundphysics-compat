# CC:HQ Sound Physics Compat — next-chat handoff

**Handoff date:** 2026-09-06 (Africa/Cairo)

This is the current continuation document for the project. A new chat should read this file first, then use the older phase documents as historical evidence when a specific earlier decision needs to be checked.

Repository:

`https://github.com/ztawfik523-lgtm/cchq-soundphysics-compat`

Current working branch:

`phase5-v7-1-lifecycle-state-finish`

The previous chat stopped here because the conversation was reaching its context limit. No further implementation was intended after the lifecycle/state source audit. The next chat should continue from this exact state instead of reconstructing the project history from scratch.

---

## 1. How to work with the user on this project

The user currently wants the assistant to make routine implementation and tradeoff decisions itself. The preferred decision style is a practical middle ground: prioritize correctness and maintainability, but avoid unnecessary overengineering and avoid aggressive changes whose benefit is uncertain.

The user can reverse this preference at any time.

The project is now in finishing mode. The active priorities are:

- performance work where there is evidence it is useful;
- correctness and bug fixes;
- lifecycle/state stability;
- runtime validation;
- cleanup, packaging and release finishing.

The user is not looking for another round of acoustic experimentation right now. The sound behavior that matters has already reached a stable reference.

The user also prefers concrete instructions over abstract plans. When runtime testing is needed, give an exact sequence of actions. When logs are uploaded, reconstruct the whole relevant run before drawing conclusions.

The user strongly prefers automatic/global diagnostics over workflows that require manually identifying OpenAL source IDs.

The assistant cannot hear the game. Subjective sound verdicts come from the user; source, CI and logs provide the technical evidence.

---

## 2. The most important project state: V7.1 is the frozen stable acoustic reference

The stable acoustic branch/reference is **V7.1**.

Frozen V7.1 commit:

`ffcf5f6e05d85b69f1f1dff8cfae1b082b71604d`

Frozen refs:

- `phase5-diffraction-v7-1-runtime-approved`
- `archive-phase5-diffraction-v7-1-runtime-approved`

Both point to exactly:

`ffcf5f6e05d85b69f1f1dff8cfae1b082b71604d`

Accepted V7.1 JAR SHA-256:

`30d457c2a52672f893b1076938e2fdea3f41759173dfd843ff652bd490692101`

Treat V7.1 as the known-good sound baseline. Later performance and lifecycle branches are descendants of this reference and are intended to preserve its acoustic result while improving housekeeping and state correctness.

This wording is intentional: V7.1 is a stable frozen reference, not a reason to treat every future source change as forbidden. If a future correctness issue genuinely requires revisiting something, evaluate it from evidence and keep the stable reference available for comparison.

The currently accepted synchronized-speaker HF50 behavior is also part of the stable sound result.

Accepted HF50 source commit:

`62d3a7a0a176c901402b913946d98f3cb455a8f4`

Accepted HF50 JAR SHA-256:

`fe894a42eebeea37e77f63e9acf65df22bdac72897fb6ac1eb9def198dcd032a`

Approved HF50 defaults:

- enabled = true
- dark_source_cutoff = 0.35
- peer_clear_cutoff = 0.75
- min_peer_gap = 0.40
- clarity_floor_ratio = 0.50
- max_cutoff_lift = 0.55

User verdict on HF50 was: `all good, lets move on`.

---

## 3. Original Beta11 Hotfix3 reconstruction baseline

Original authoritative historical artifact:

`cchq_soundphysics_compat-0.1.0-beta11-hotfix3.jar`

SHA-256:

`83500f182fc9829aa1a5a51fbfa11ba6cdfb645699b25d1c445167666dabc1ef`

Pinned environment:

- Java 21
- Minecraft 1.21.1
- NeoForge 21.1.248
- ModDevGradle 2.0.144
- Gradle 9.2.1
- CC:Tweaked 1.120.2
- CC:HQ Speakers runtime artifact: Modrinth project `ygA78R8l`, version `u5PEI5Ax`
- HQ runtime reports `1.1.4-neoforge-1.21.1`
- Sound Physics Remastered 1.21.1-1.5.1 / Modrinth version `Dd2tmpsk`
- Cloth Config 15.0.140 / file `5729127`

Runtime uses the untouched SPR artifact. Compilation uses an isolated access-transformed SPR copy produced by `prepareSprCompileJar`, because the reconstructed compat source directly accesses members widened by the access transformer.

Reconstruction phases 1–4 are complete.

Phase 4 final audited code/build head:

`98e7dedb7ecf6fda22008b084b6bb41956edff78`

Phase 4 established:

- 60/60 class paths exact
- 60/60 structural ABI exact
- 69/69 `ConstantValue` entries exact
- zero bootstrap/string-concat recipe mismatches
- 11/11 configured Mixin/accessor annotation sets reconciled
- 5/5 runtime resources byte-exact
- 550 methods audited
- no unresolved proven Hotfix3 behavior drift at closure

Historical Phase 4 refs:

- `phase4-hotfix3-parity` -> `79eed29767343ee34022e8f6268b386f75e84c9f`
- `archive-phase4-hotfix3-parity` -> same

The reconstruction itself is no longer the active task.

---

## 4. Core behavior carried forward from the approved implementation

These facts are useful when reviewing later changes because they describe the architecture that the stable builds rely on:

- HQ whole-file playback is intercepted at the compat layer; Lua behavior was not changed for the acoustic fixes.
- Distance behavior uses `SoundSource.BLOCKS`.
- Direct occlusion uses the progressive 17-path geometry: center + 8 inner + 8 outer paths.
- The approved refresh model uses full 17-path and alternating/partial 9-path updates as established in the Beta11 implementation.
- Private EFX state is per OpenAL source so one source does not contaminate another through SPR's global filter state.
- Actual environment applications reattach the direct and auxiliary EFX state.
- Private EFX setup waits until the source is in a PLAYING/PAUSED-eligible state.
- SPR's own `calculateOcclusion()` remains part of the pipeline; the compat work integrates around it rather than replacing the whole calculation.
- SPR world/geometry raycasts remain on the appropriate sound-thread-owned path rather than worker decode threads.
- Source lifetime uses generation/identity tracking so old asynchronous work cannot install into a newer source lifetime.
- The synchronized-start coordinator uses `alSourcePlayv` for complete groups and retains the Hotfix3 partial-group grace / pending-INITIAL protection.
- Physics work is separate from PCM playback position; the implementation does not intentionally reschedule buffer cursors or alter OpenAL playback clocks as part of acoustic updates.
- Beta10 exact direct reuse and bit-identical OpenAL write suppression remain part of the performance model.
- Beta11 room-ray memoization remains exact and same-clone; cross-clone reuse is telemetry rather than an enabled semantic cache.
- The eligibility expression currently uses the deliberate bitwise form:

  `Beta9Optimizer.isAudibleAndRecord(state.sourceId) & beta9EligibleReal(state, now)`

That expression was repeatedly checked during later CI because both sides are expected to run for the intended accounting/eligibility behavior.

---

## 5. Diffraction history and why V7.1 became the stable one

The original elevation/opening problem was a geometry limitation: straight source-to-listener rays could cross terrain and produce extreme muffling even when a plausible path existed around a nearby opening or edge.

The versions below matter because they explain what was learned and prevent a new chat from mistaking rejected experiments for the current design.

### V3

Runtime-approved historical checkpoint:

`b1b97fc7050163e70fbb039f7b76e51eef3c50d0`

V3 improved the target cases and demonstrated that opening relief could help, but its logic was too special-case to become the final model.

### V4

Branch:

`phase5-opening-diffraction-test-v4`

Head:

`053af2cca0dd84e98956fec6a860cb46e860630e`

V4 introduced a cheap listener-centered topology scan and up to two SPR-verified portal candidates. Its performance characteristics were good, but diagonal clipping made one side-verification shape unreliable.

### V5

Branch:

`phase5-opening-diffraction-test-v5`

Head:

`595b4736d3f861e5d67c8b6a64f2cf9297a7f4fe`

V5 split the aperture into lower/upper waypoints. It sounded clearer near the target opening but behaved too much like a replacement path rather than a physically bounded secondary contribution.

### V6

Source:

`f72c106cd94108a418cb6d64cdeb4eb09a68de58`

V6 established the important model shape that V7.1 keeps:

- direct progressive 17-ray result remains primary;
- an opening contributes bounded secondary two-band energy;
- at most two verified portals are used;
- the opening topology search is bounded;
- listener and source leg caching/hysteresis are used.

V6 exposed one remaining flaw: explicit openings did not lose enough contribution with listener distance because no aperture spreading term was present.

### V7

Source:

`ae3c4ef55173a5be527f114e18af8de8bc43d315`

V7 bundled the needed spreading idea with a leg attenuation rewrite. The user immediately preferred the previous iteration and asked what had changed. That version is historical/rejected and is useful mainly as evidence that the extra leg rewrite was not desirable.

### V7.1

V7.1 kept the V6 leg/spectral math and added only the explicit spreading term. That combination produced the accepted result and is the frozen stable reference.

V6/V7.1 leg math:

```java
legRaw = max(0, lower) + max(0, upper);
rawRatio = legRaw / raw;
pathGain = pow(gain, rawRatio);
pathCutoff = pow(cutoff, rawRatio);
```

V7.1 spreading:

```java
spread = apertureSpreadScale / (apertureSpreadScale + apertureDistance);
coupling = portalCoupling * activation * horizonFade * spread;
lowAmp = coupling * pathGain * lowDiff;
highAmp = coupling * pathGain * pathCutoff * highDiff;
```

Default:

`aperture_spread_scale = 3`

Important runtime observations from the accepted V7.1 test:

- near/opening positions behaved much better than the original straight-ray result;
- explicit portal contribution falls with aperture distance;
- the topology search has a radius ceiling of 8;
- moving from roughly radius 8 to a point around 10 blocks away could feel a little sudden because the opening search stops finding that candidate beyond the ceiling;
- the user noticed the 3-block-deep-hole case was slightly more muffled than an earlier prototype but accepted it.

The 3-deep final telemetry is useful because the portal contribution was still active even though the user perceived the result as somewhat darker:

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
- final cutoff about 0.002 -> 0.309
- final gain about 0.412 -> 0.452

That telemetry means the correct description is “active portal contribution with a slightly darker overall impression,” not “diffraction was inactive.”

---

## 6. Performance benchmark of frozen V7.1

The user ran a marked benchmark covering one and four sources. The planned 12-speaker section was skipped.

Interpretation rule used in the analysis:

A `beta9 extra/perf window=10.xs` line describes the preceding ten seconds. For a marked test, only windows whose entire preceding interval fits inside the START/END markers were treated as strict test windows.

### Test 1 — 1 source, diffraction OFF, stationary

Strict window:

- acoustic CPU 2.1 ms/s
- SPR load 0.9 ms/s
- directReal 74
- directReuses 90
- movementResets 0
- sprCalls 10, avg 1.074 ms, max 1.314 ms
- occlusionPaths 90
- portalPaths 0

### Test 2 — 1 source, diffraction ON, stationary

Strict window:

- acoustic CPU 2.6 ms/s
- SPR load 1.3 ms/s
- sprCalls 10, avg 1.047 ms, max 1.370 ms
- occlusionPaths 94
- portalPaths 4
- portalAvg 0.005 ms, max 0.009 ms
- portalLower 2
- portalCross 2
- topology scans 9
- topology block checks 1764

### Test 3 — 1 source, diffraction ON, moving

Strict windows:

1. acoustic 5.8 ms/s; SPR 2.9 ms/s; movementResets 200; sprCalls 78; portalPaths 28; scans 52; checks 9833
2. acoustic 7.6 ms/s; SPR 4.2 ms/s; movementResets 197; sprCalls 80; portalPaths 29; scans 54; checks 10393
3. acoustic 14.5 ms/s; SPR 12.0 ms/s; movementResets 182; sprCalls 71; SPR max 10.106 ms; portalPaths 20; scans 43; checks 8099

The third window contained a large SPR timing spike. The first two windows are a better representation of normal one-source movement for this run.

### Test 4 — 4 sources, diffraction OFF, stationary

Strict windows:

- 3.8 acoustic / 2.5 SPR ms/s
- 5.6 acoustic / 4.3 SPR ms/s

Average about:

- acoustic 4.7 ms/s
- SPR 3.4 ms/s

### Test 5 — 4 sources, diffraction ON, stationary

Strict window:

- acoustic 5.2 ms/s
- SPR 3.6 ms/s
- sprCalls 40, avg 0.971 ms, max 1.972 ms
- portalPaths 10
- portalAvg 0.008 ms, max 0.053 ms
- portalLower 2
- portalCross 8
- topology scans 9
- topology checks 1764

The important scaling result is that topology scans/checks were 9/1764 with one stationary source and still 9/1764 with four stationary sources. The topology work is listener-shared rather than multiplied by source count.

The listener-side lower leg also stayed shared. The per-source cross leg scaled from 2 to 8, matching the intended architecture.

Measured portal-ray CPU was negligible.

### Test 6 — 4 sources, diffraction ON, moving

Strict windows:

1. acoustic 14.4 ms/s; SPR 6.8; scans 55; checks 10404; portalPaths 44
2. acoustic 13.7 ms/s; SPR 6.9; scans 57; checks 10952; portalPaths 39
3. acoustic 14.6 ms/s; SPR 7.4; scans 61; checks 11725; portalPaths 36

Average:

- acoustic about 14.23 ms/s
- SPR about 7.03 ms/s

The benchmark conclusion was that moving cost is mainly the existing progressive/direct/SPR refresh machinery. V7.1 portal-ray time was tiny, roughly 0.011–0.018 ms/s in the four-source moving windows.

The skipped 12-speaker test would still be useful as a broad stress test, but it is not needed to establish the architectural scaling result above.

---

## 7. Performance housekeeping pass derived from V7.1

Branch:

`phase5-v7-1-performance-pass`

Created from the frozen V7.1 commit:

`ffcf5f6e05d85b69f1f1dff8cfae1b082b71604d`

Final clean performance head:

`962eab8b052466ca984496a7dec0767dc65803f4`

The pass intentionally stayed small because the benchmark showed that diffraction rays were already cheap.

Changes kept in `VerticalDiffractionRelief`:

- `CACHE_PRUNE_INTERVAL_NS = 1_000_000_000L`
- cache housekeeping normally prunes at most once per second;
- existing soft-limit overflow still triggers immediate pruning;
- stale-cache validity rules, TTLs and recheck thresholds are unchanged;
- `clear()` resets prune timing;
- branches that can return no candidates avoid allocating a list first;
- implicit open-top creates an `ArrayList` sized for one candidate;
- explicit candidate lists are pre-sized to the topology candidate count.

A multi-cell topology cache was considered and intentionally left out because re-entering an older cell could reuse older topology state and subtly change acoustic timing. That was not a good trade for a finishing/performance pass.

Successful validation run:

`33957036689`

Performance JAR SHA-256:

`b7945374fc95935a3c951e660efb660bbcc777744395e81849b5abf680592b41`

Class count:

81

This pass targets map traversal/allocation/housekeeping overhead. It does not change the topology algorithm, so topology scan and block-check counters are not expected to fall simply because this patch is present.

A dedicated user-side runtime benchmark of the isolated performance JAR was not completed before lifecycle work. The lifecycle candidate described below includes this performance pass.

---

## 8. Lifecycle/state audit completed immediately before the handoff

Current branch:

`phase5-v7-1-lifecycle-state-finish`

Performance base:

`962eab8b052466ca984496a7dec0767dc65803f4`

Clean lifecycle source checkpoint before documentation-only commits:

`be03d30efe98ca03bdf27764bcea567df5ef3875`

Compared with the performance base, that clean source checkpoint changes only:

1. `src/main/java/dev/cchqphysics/compat/audio/CompatAudioManager.java`
2. `src/main/java/dev/cchqphysics/compat/mixin/SoundEngineLifecycleMixin.java`

Three concrete lifecycle/state issues were found and fixed.

### 8.1 ClientLevel/world identity transition

Previous logic could treat “a level exists” as continuity even when the actual `ClientLevel` object had changed. That could carry session-owned state across disconnect/rejoin or dimension/world replacement.

The manager now tracks the actual `ClientLevel` instance. When the tracked level disappears or changes identity, the previous compat session is invalidated and its sources are stopped before the new level becomes the active session.

This is state correctness; it does not modify acoustic equations.

### 8.2 Sound-engine teardown ordering

The previous cleanup path could enqueue compat teardown while vanilla continued into sound-engine executor/OpenAL shutdown. That creates a race where compat EFX/source cleanup may run too late.

The chosen lifecycle shape is:

- normal `stopAll` performs compat teardown through blocking sound-thread execution;
- `emergencyShutdown` performs its own blocking compat teardown before OpenAL destruction;
- redundant direct `destroy` and `reload` injections were removed so normal sound shutdown/reload has one authoritative cleanup path rather than overlapping paths.

`onSoundThreadBlocking(Runnable)` uses the sound executor's `executeBlocking(task)` path.

Current lifecycle mixin hooks are:

- `pause` HEAD -> `pauseCompatSources()`
- `resume` HEAD -> `resumeCompatSources()`
- `stopAll` HEAD -> `stopAllCompatSources()`
- `emergencyShutdown` HEAD -> `resetForSoundEngine()`

### 8.3 Failed-start partial registration cleanup

`startSource()` creates an OpenAL source and registers compat state before every later source-configuration step has necessarily completed.

If setup fails after registration but before installation into `ACTIVE`, the raw AL source used to be deleted while some compat-owned registrations could remain.

The `!installed` failure path now calls:

```java
try { EnvironmentSmoother.unregister(sourceId); } catch (Throwable ignored) {}
```

before stopping/detaching/deleting the raw OpenAL source.

`EnvironmentSmoother.unregister()` fans out to the associated per-source state owners, including progressive occlusion, position state, SoundPhysicsBridge registration, sync-source membership and private EFX cleanup. That closes the discovered failed-start stale-state path.

Lifecycle source commits:

- initial lifecycle source fix: `24e41b49f8ccd5b32e484b90def8f50c3767c8d9`
- failed-start cleanup source commit: `cfcc122da3be03171377f14717ae30b4c6bbb696`
- clean source checkpoint after temporary CI helper removal: `be03d30efe98ca03bdf27764bcea567df5ef3875`

---

## 9. Lifecycle CI validation

Successful validation run:

`33962380234`

Job:

`101296383519`

Passed steps included:

- Java 21 setup
- Gradle wrapper validation
- ancestry and frozen-ref checks
- lifecycle-fix assertions
- exact `&` eligibility assertion
- comparison proving the named acoustic/config files remained unchanged from the performance baseline
- Lua diff check
- reconstruction compile classpath verification
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

Lifecycle candidate JAR SHA-256:

`6d0fa98ee6c76d23a3e0764501d16dc5c993149e0de77181cdab6fc0a9abdc18`

Class count:

81

Build metadata recorded:

- `world_identity_reset=true`
- `normal_stopall_blocking=true`
- `destroy_reload_redundant_hooks_removed=true`
- `emergency_shutdown_blocking=true`
- `failed_start_unregister=true`
- `acoustic_model_changes=false`
- `lua_changes=false`
- `game_launch_performed=false`

Internal-version note:

The built lifecycle candidate still uses:

`0.1.0-beta11-phase5-v7-1-performance-test`

That is naming debt only. The lifecycle source fixes are present in the artifact built from the lifecycle branch. Final release naming has not been done yet.

There were earlier CI false starts while writing the validator. One guard was too strict. Another validator invocation combined Gradle `clean compileJava jar` in a way that caused Gradle 9.2.1 to report an execution-plan deadlock. The final validation split those stages into separate invocations and completed successfully. Run `33962380234` is the useful validation result.

---

## 10. What is validated versus what is still pending

### Already established

- Original Hotfix3 reconstruction and Phase 4 equivalence audit are complete.
- HF50 behavior was user-approved.
- V7.1 diffraction behavior was user-tested and frozen as the stable acoustic reference.
- The one/four-source performance benchmark established the main scaling behavior.
- The performance housekeeping source pass builds and passes CI.
- The lifecycle/state source fixes build and pass CI.
- The lifecycle candidate contains the performance housekeeping pass plus the lifecycle fixes.

### Still pending

The lifecycle candidate has not yet received the user's runtime lifecycle/stability run after the new teardown/world/failure-path fixes.

Therefore the source/CI state is good, but runtime lifecycle acceptance is still the next meaningful checkpoint.

---

## 11. Exact runtime test to resume with in the next chat

Candidate SHA-256:

`6d0fa98ee6c76d23a3e0764501d16dc5c993149e0de77181cdab6fc0a9abdc18`

Use one lifecycle/state run rather than redoing every old acoustic test:

1. Start a speaker.
2. Stop it.
3. Start it again.
4. While it is playing, pause Minecraft and resume.
5. Exercise the normal stop-all path, then restart playback.
6. Exercise the Minecraft sound/resource reload path after playback has been active, then restart playback.
7. Disconnect to the menu, rejoin the world/server, then start playback again.
8. Change dimension after playback has been active, then start playback again in the new dimension.
9. Run four speakers for several minutes while moving around.
10. Upload `latest.log` and `debug.log` from that exact run.

Useful runtime evidence to extract from the logs:

- exact compat JAR/version loaded;
- confirmation that only one compat JAR is present;
- Java / NeoForge / CC:Tweaked / HQ / SPR versions;
- the sequence of stop/restart/reload/rejoin/dimension actions from timestamps or markers;
- lifecycle warnings or OpenAL/EFX errors;
- whether source/state counts accumulate across repeated lifetimes;
- stale sync groups or stale source IDs after world changes;
- repeated private EFX failure;
- any obvious performance regression.

For performance windows, remember that a 10-second `beta9 extra/perf` line describes the preceding interval. Match the whole interval to the relevant test segment rather than assigning it based only on the line timestamp.

The user should also report whether playback behavior subjectively remained normal through the lifecycle actions. The assistant should describe that as the user's listening result, not as something it personally verified.

---

## 12. Likely path after a clean lifecycle runtime run

If the runtime lifecycle test is clean, the project is effectively at release finishing.

A sensible next sequence is:

1. **Release naming and cleanup**
   - replace the `phase5-v7-1-performance-test` internal version with the intended final release name/version;
   - normalize experimental/test startup wording;
   - review config filenames for release presentation while keeping values/behavior stable;
   - confirm temporary CI helpers are absent from the production tree.

2. **Final source audit**
   - compare against frozen V7.1 and the clean performance/lifecycle checkpoints;
   - confirm HF50 and V7.1 acoustic equations still match the approved behavior;
   - confirm the eligibility `&` expression;
   - confirm lifecycle fixes remain present;
   - confirm Java 21 build and resource wiring.

3. **Final packaging inspection**
   - expected JAR count;
   - expected classes/resources;
   - final SHA-256;
   - final source commit and CI run;
   - separately record CI build validation and the user's runtime validation.

4. **Final stable/archive checkpoint**
   - create a final release/archive reference after the release candidate is accepted;
   - leave the existing V7.1 frozen reference intact as the acoustic comparison point;
   - `main` is still untouched, so any merge can remain an explicit release decision later.

A final 12-speaker or broader stress run is optional if the user wants extra release confidence, but the known performance architecture does not currently show a diffraction source-count scaling problem.

---

## 13. Known limitations and context worth carrying forward

These are best treated as known characteristics of the stable V7.1 result rather than unfinished mystery bugs:

1. Opening discovery is bounded to radius 8, so leaving the search radius can produce a noticeable transition.
2. The accepted 3-deep-hole case is somewhat more muffled than an earlier prototype even though portal energy is active.
3. Movement costs considerably more than stationary playback because progressive direct/SPR work refreshes heavily during motion.
4. Portal SPR-ray CPU is very small; aggressive diffraction-ray optimization is unlikely to produce a large gain.
5. The performance housekeeping pass is intentionally modest because stronger topology caching could alter acoustic state timing.
6. The current lifecycle artifact still reports the old `phase5-v7-1-performance-test` internal version until release naming is cleaned up.

If a future runtime log reveals a concrete correctness problem, fix that problem from evidence. The stable V7.1 reference makes it easy to check whether a fix accidentally changes sound behavior.

---

## 14. Important refs and checkpoints

Original Hotfix3 artifact:

- SHA `83500f182fc9829aa1a5a51fbfa11ba6cdfb645699b25d1c445167666dabc1ef`

Phase 4:

- final audited code/build `98e7dedb7ecf6fda22008b084b6bb41956edff78`
- `phase4-hotfix3-parity` -> `79eed29767343ee34022e8f6268b386f75e84c9f`
- `archive-phase4-hotfix3-parity` -> same

HF50:

- accepted source `62d3a7a0a176c901402b913946d98f3cb455a8f4`
- `archive-phase5-hf50-config-runtime-approved` -> same
- `archive-phase5-sync-hf50-configurable-validated` -> `56786cd447f5c65e08a9bc9556b3f08b709e2118`

Diffraction history:

- `archive-phase5-diffraction-v3-runtime-approved` -> `b1b97fc7050163e70fbb039f7b76e51eef3c50d0`
- stable V7.1 `phase5-diffraction-v7-1-runtime-approved` -> `ffcf5f6e05d85b69f1f1dff8cfae1b082b71604d`
- stable V7.1 archive `archive-phase5-diffraction-v7-1-runtime-approved` -> same

Performance:

- clean performance head `962eab8b052466ca984496a7dec0767dc65803f4`
- performance JAR SHA `b7945374fc95935a3c951e660efb660bbcc777744395e81849b5abf680592b41`

Lifecycle:

- initial source fix `24e41b49f8ccd5b32e484b90def8f50c3767c8d9`
- failed-start cleanup `cfcc122da3be03171377f14717ae30b4c6bbb696`
- clean lifecycle source checkpoint before documentation-only commits `be03d30efe98ca03bdf27764bcea567df5ef3875`
- lifecycle JAR SHA `6d0fa98ee6c76d23a3e0764501d16dc5c993149e0de77181cdab6fc0a9abdc18`
- successful lifecycle CI run `33962380234`
- lifecycle artifact `9968357487`

Other historical ref:

- `phase5-test-candidate-1` -> `44612192d875e43ecef66ca51798cab7adb17020`

---

## 15. Branch/source interpretation after this documentation update

The clean code checkpoint for the lifecycle pass is:

`be03d30efe98ca03bdf27764bcea567df5ef3875`

Documentation updates made during this handoff advance the working branch past that SHA without intentionally changing production Java source.

When the next chat needs to distinguish “source state” from “documentation head,” use:

- `be03d30e...` as the clean lifecycle source checkpoint;
- the latest `phase5-v7-1-lifecycle-state-finish` head as the current documentation/working head;
- `ffcf5f6e...` as the stable frozen V7.1 acoustic reference.

`main` was still at its original repository initialization state during this work and has not been used as the Phase 5 working branch.

---

## 16. First action for the next chat

The next chat should begin by reading this file and checking the current `phase5-v7-1-lifecycle-state-finish` branch head.

If the user uploads lifecycle test logs, analyze those first.

If the user has not yet run the lifecycle candidate, give the exact runtime sequence from section 11 and continue from there.

If the lifecycle candidate passes, move into release cleanup/final audit rather than reopening the reconstruction or redesigning the acoustic model.

The key mental model is simple:

**Hotfix3 was reconstructed successfully. HF50 was approved. V7.1 is the frozen stable acoustic result. The performance and lifecycle branches are finishing passes on top of that stable result. The immediate unfinished item is user runtime validation of the lifecycle candidate, followed by release cleanup.**
