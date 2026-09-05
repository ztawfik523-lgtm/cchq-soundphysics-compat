# CC:HQ Sound Physics Compat

Compatibility layer between **CC:HQ Speakers** and **Sound Physics Remastered (SPR)** for Minecraft 1.21.1 / NeoForge.

## Current state

The Beta11 Hotfix3 source reconstruction is complete. The project is now in Phase 5 finalization: stable acoustic behavior has been established, a small performance housekeeping pass has been validated, and the current lifecycle/state fixes are awaiting user runtime validation.

For a fresh session, read:

1. `docs/NEXT_CHAT_HANDOFF_2026-09-06.md` — detailed current handoff
2. `RECONSTRUCTION_STATUS.md` — compact current status
3. `docs/BETA11_RECONSTRUCTION_HANDOFF.md` — durable reconstruction/Phase 5 background

Older phase documents remain useful as audit history.

---

## Stable acoustic reference: V7.1

The current known-good sound reference is **V7.1**.

Frozen commit:

`ffcf5f6e05d85b69f1f1dff8cfae1b082b71604d`

Frozen refs:

- `phase5-diffraction-v7-1-runtime-approved`
- `archive-phase5-diffraction-v7-1-runtime-approved`

Accepted V7.1 JAR SHA-256:

`30d457c2a52672f893b1076938e2fdea3f41759173dfd843ff652bd490692101`

V7.1 is the stable comparison point for later performance/lifecycle finishing work. It contains the approved opening/diffraction behavior and the previously approved synchronized-speaker HF50 behavior.

HF50 accepted source:

`62d3a7a0a176c901402b913946d98f3cb455a8f4`

HF50 JAR SHA-256:

`fe894a42eebeea37e77f63e9acf65df22bdac72897fb6ac1eb9def198dcd032a`

---

## Current working branch

`phase5-v7-1-lifecycle-state-finish`

Clean lifecycle source checkpoint before documentation-only handoff commits:

`be03d30efe98ca03bdf27764bcea567df5ef3875`

This branch descends from the performance pass, which descends from frozen V7.1.

The lifecycle pass changes only:

- `CompatAudioManager.java`
- `SoundEngineLifecycleMixin.java`

relative to the clean performance baseline.

The fixes cover:

- real `ClientLevel` identity changes across disconnect/rejoin and dimension/world replacement;
- blocking sound-thread teardown before normal stop-all/emergency sound-engine destruction continues;
- cleanup of compat registrations when source startup fails after registration but before installation.

Successful lifecycle CI run:

`33962380234`

Lifecycle candidate JAR SHA-256:

`6d0fa98ee6c76d23a3e0764501d16dc5c993149e0de77181cdab6fc0a9abdc18`

Class count: 81.

The lifecycle candidate still reports internal version `0.1.0-beta11-phase5-v7-1-performance-test`; final release naming cleanup comes after runtime lifecycle validation.

---

## Performance pass

Branch:

`phase5-v7-1-performance-pass`

Clean head:

`962eab8b052466ca984496a7dec0767dc65803f4`

The benchmark showed that V7.1 portal-ray CPU is already tiny and that movement cost is mainly the existing progressive/direct/SPR refresh work. The performance pass therefore stays small and behavior-neutral:

- prune cache housekeeping at most once per second under normal cache size;
- still prune immediately when existing soft limits are exceeded;
- keep cache validity/TTL/recheck rules unchanged;
- avoid unnecessary candidate-list allocation;
- pre-size candidate lists.

Successful validation run:

`33957036689`

Performance JAR SHA-256:

`b7945374fc95935a3c951e660efb660bbcc777744395e81849b5abf680592b41`

---

## Benchmark summary

User runtime benchmark of frozen V7.1 covered one and four sources; the 12-speaker section was skipped.

Representative four-source, diffraction-ON moving windows:

- 14.4 acoustic / 6.8 SPR ms/s
- 13.7 acoustic / 6.9 SPR ms/s
- 14.6 acoustic / 7.4 SPR ms/s

Key result:

- topology scan work is listener-shared in the tested stationary case;
- listener lower-leg work is shared;
- source cross-leg work scales per source as designed;
- portal SPR-ray timing is negligible;
- moving cost is dominated by the main progressive/direct/SPR system rather than the V7.1 portal rays.

Full numbers and the strict 10-second-window interpretation are in `docs/NEXT_CHAT_HANDOFF_2026-09-06.md`.

---

## Original Beta11 Hotfix3 reconstruction

Historical authoritative artifact:

`cchq_soundphysics_compat-0.1.0-beta11-hotfix3.jar`

SHA-256:

`83500f182fc9829aa1a5a51fbfa11ba6cdfb645699b25d1c445167666dabc1ef`

Pinned environment:

- Minecraft 1.21.1
- NeoForge 21.1.248
- Java 21
- ModDevGradle 2.0.144
- Gradle 9.2.1
- CC:Tweaked 1.120.2
- CC:HQ Speakers `ygA78R8l/u5PEI5Ax`
- Sound Physics Remastered 1.21.1-1.5.1 / `Dd2tmpsk`
- Cloth Config 15.0.140

Runtime SPR remains untouched. The reconstructed build compiles against an isolated access-transformed SPR copy because the exact compat source calls members widened by the access transformer.

Phase 4 final audited code/build head:

`98e7dedb7ecf6fda22008b084b6bb41956edff78`

Phase 4 closure established:

- 60/60 class paths exact
- 60/60 structural ABI exact
- 69/69 `ConstantValue` entries exact
- zero bootstrap/string-concat recipe mismatches
- 11/11 configured Mixin/accessor semantic annotation sets reconciled
- 5/5 runtime resources byte-exact
- 550 methods audited
- no unresolved proven Hotfix3 behavior drift

---

## Architecture carried into the stable build

The current stable/finishing branches retain these core behaviors:

- HQ whole-file positional playback through OpenAL;
- mono downmix for positional playback while preserving the source payload workflow;
- shared OpenAL buffers/refcounts for synchronized speakers using the same payload;
- synchronized group start via `alSourcePlayv` with Hotfix3 partial-group grace and pending-INITIAL handling;
- `SoundSource.BLOCKS` distance behavior;
- progressive 17-path direct geometry: center + 8 inner + 8 outer;
- approved full/partial direct refresh behavior;
- private per-source EFX;
- direct/aux EFX reattachment on actual environment applications;
- PLAYING/PAUSED eligibility before private EFX creation;
- SPR `calculateOcclusion()` remains part of the normal acoustic pipeline;
- world/geometry SPR raycasts stay on the intended sound-thread-owned path;
- strict source generation/lifetime handling;
- Beta10 exact direct reuse and bit-identical OpenAL write suppression;
- Beta11 exact same-clone room-ray memoization with cross-clone reuse as telemetry;
- deliberate bitwise eligibility expression:

  `Beta9Optimizer.isAudibleAndRecord(state.sourceId) & beta9EligibleReal(state, now)`

---

## Known characteristics of V7.1

- Opening discovery is bounded to radius 8.
- Leaving that radius can produce a noticeable transition.
- The accepted 3-deep-hole case is somewhat darker than one earlier prototype while portal energy remains active.
- Movement remains much more expensive than stationary playback because progressive/direct/SPR work refreshes heavily while moving.

These are documented properties of the current stable sound reference.

---

## Immediate next step

The current lifecycle candidate is source/CI validated and still needs the user's runtime lifecycle run.

Test sequence:

1. play -> stop -> play
2. pause -> resume
3. stopAll -> restart
4. sound/resource reload -> restart
5. disconnect -> rejoin -> restart
6. dimension change -> restart
7. four speakers for several minutes while moving
8. upload `latest.log` and `debug.log`

If that run is clean, the project moves naturally to release cleanup and final audit: final version naming, presentation cleanup, final source comparison, final JAR/resource/hash inspection, and a final stable/archive checkpoint.

`main` remains untouched at this handoff; an eventual merge can be treated as a separate release decision.
