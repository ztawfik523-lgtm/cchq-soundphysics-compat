# CC:HQ Sound Physics Compat — current project status

> **Start here in a new chat:**
>
> 1. `docs/NEXT_CHAT_HANDOFF_2026-09-06.md` — detailed project/history handoff
> 2. `docs/LIFECYCLE_SOURCE_AUDIT_2026-09-06.md` — superseding lifecycle source/JAR checkpoint
> 3. `docs/RELEASE_CLEANUP_AUDIT_2026-09-06.md` — release-cleanup inventory
> 4. `docs/RELEASE_POLICY_LOCK_2026-09-06.md` — locked final-release decisions and mandatory Gate-2 smoke test
> 5. `docs/FINAL_RELEASE_EXECUTION_PLAN_2026-09-06.md` — exact post-lifecycle release patch/CI plan
>
> This file is the compact status view.

Repository:

`https://github.com/ztawfik523-lgtm/cchq-soundphysics-compat`

Current working branch:

`phase5-v7-1-lifecycle-state-finish`

Current project direction: reconstruction is complete and the project is in finalization. The stable acoustic reference is **V7.1**; current work is lifecycle runtime validation followed by the already-planned final release transformation, final CI and a short release smoke test.

---

## Phase status

| Phase | Status |
| --- | --- |
| Phase 1 — freeze and inventory binary baseline | **COMPLETE / JAR-RECHECKED** |
| Phase 2 — reconstruct build project | **COMPLETE / JAR-RECHECKED** |
| Phase 3 — reconstruct Java source | **COMPLETE / RECHECKED** |
| Phase 4 — structural and behavioral equivalence audit | **COMPLETE / RECHECKED** |
| Phase 5 — runtime fixes, stable acoustic baseline, performance/lifecycle finalization | **IN FINALIZATION** |

Phase 5 already contains user-approved HF50 behavior, user-tested/frozen V7.1 diffraction, a CI-validated performance housekeeping pass, and an audited lifecycle/state pass. The audited lifecycle candidate still needs the user's runtime lifecycle test.

---

## Original Hotfix3 baseline

Artifact:

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
- CC:HQ Speakers `ygA78R8l/u5PEI5Ax`, runtime `1.1.4-neoforge-1.21.1`
- Sound Physics Remastered 1.21.1-1.5.1 / `Dd2tmpsk`
- Cloth Config 15.0.140

Phase 4 final audited code/build:

`98e7dedb7ecf6fda22008b084b6bb41956edff78`

Final Phase 4 evidence:

- 60/60 class paths exact
- 60/60 structural ABI exact
- 69/69 constant values exact
- zero bootstrap/string-concat recipe mismatches
- 11/11 configured Mixin/accessor annotation sets reconciled
- 5/5 runtime resources byte-exact
- 550 methods audited
- no unresolved proven behavior discrepancy at closure

---

## Stable acoustic reference — V7.1

V7.1 is the frozen, stable sound baseline for the current project.

Commit:

`ffcf5f6e05d85b69f1f1dff8cfae1b082b71604d`

Refs:

- `phase5-diffraction-v7-1-runtime-approved`
- `archive-phase5-diffraction-v7-1-runtime-approved`

Accepted JAR SHA-256:

`30d457c2a52672f893b1076938e2fdea3f41759173dfd843ff652bd490692101`

The stable behavior includes the previously approved synchronized-speaker HF50 fix.

HF50 source:

`62d3a7a0a176c901402b913946d98f3cb455a8f4`

HF50 JAR SHA-256:

`fe894a42eebeea37e77f63e9acf65df22bdac72897fb6ac1eb9def198dcd032a`

Known characteristics of V7.1:

- bounded opening discovery radius of 8;
- opening contribution falls with explicit aperture spreading;
- the radius boundary can produce a noticeable transition beyond the search range;
- the accepted 3-deep-hole result is a little darker than one earlier prototype while portal contribution is still active;
- moving playback is substantially more expensive than stationary playback because progressive direct/SPR work refreshes heavily during movement.

---

## V7.1 performance benchmark result

The user benchmarked one and four sources; the planned 12-speaker section was skipped.

Representative four-source, diffraction-ON, moving windows:

- 14.4 acoustic / 6.8 SPR ms/s
- 13.7 acoustic / 6.9 SPR ms/s
- 14.6 acoustic / 7.4 SPR ms/s

Average:

- acoustic about 14.23 ms/s
- SPR about 7.03 ms/s

Important scaling observations:

- listener topology scans do not multiply by speaker count in the tested stationary case;
- listener-side lower portal leg is shared;
- source-side cross leg scales per source as expected;
- measured portal SPR-ray CPU is tiny;
- movement cost is mainly the existing progressive/direct/SPR machinery.

---

## Performance housekeeping pass

Branch:

`phase5-v7-1-performance-pass`

Clean head:

`962eab8b052466ca984496a7dec0767dc65803f4`

Changes are intentionally small and behavior-neutral:

- cache pruning normally runs at most once per second;
- soft-limit overflow still prunes immediately;
- stale lookup TTL/recheck semantics remain unchanged;
- avoid candidate-list allocations on immediate-empty paths;
- pre-size candidate lists.

Successful validation run:

`33957036689`

JAR SHA-256:

`b7945374fc95935a3c951e660efb660bbcc777744395e81849b5abf680592b41`

Class count: 81.

---

## Audited lifecycle/state finalization pass

Current branch:

`phase5-v7-1-lifecycle-state-finish`

Performance base:

`962eab8b052466ca984496a7dec0767dc65803f4`

Audited production-source checkpoint:

`2a6a2f4ecd9e2faad51de9818797f5a16c14b0f7`

Documentation-only commits advance the branch beyond that SHA. Relative to the performance base, production Java changes remain limited to:

- `src/main/java/dev/cchqphysics/compat/audio/CompatAudioManager.java`
- `src/main/java/dev/cchqphysics/compat/mixin/SoundEngineLifecycleMixin.java`

Lifecycle/state fixes now cover:

1. actual `ClientLevel` identity invalidation across disconnect/rejoin and dimension/world replacement;
2. blocking sound-thread teardown before normal stop-all/emergency sound-engine destruction continues;
3. failed-start compat unregister before raw OpenAL deletion;
4. source allocation error/source-ID-0 guard before compat registration;
5. best-effort unregister/stop/detach/delete/buffer cleanup;
6. actual-state pause/resume so pending synchronized `AL_INITIAL` sources cannot be started individually by resume;
7. vector pause/resume for eligible compat sources.

Source-audit commits:

- active teardown/allocation hardening: `fb2665620ff977c4e251da416679f0ef5789d724`
- pause/resume sync preservation: `2a6a2f4ecd9e2faad51de9818797f5a16c14b0f7`

Successful audited validation run:

`33996243988`

Job:

`101387190106`

Artifact:

`9978159512`

Audited lifecycle candidate JAR SHA-256:

`4b3c8c52cc00a37274d5829cff93933d6e548b733b83918fcc5570ab8d6ad3c5`

Class count: 81.

The earlier candidate SHA `6d0fa98e...` is superseded.

CI proved the named frozen acoustic/config files unchanged from the performance baseline, no Lua changes, successful Java 21 compile/build, correct resource wiring and 81 packaged classes.

---

## Locked final-release policy

Release decisions are now resolved in `docs/RELEASE_POLICY_LOCK_2026-09-06.md`.

After the audited lifecycle candidate passes runtime:

- final private-mod version will be `0.1.0`;
- version identity will be centralized from `mod_version` so artifact/NeoForge/runtime reporting agree;
- accepted V7.1 diffraction will ship **ON by default**;
- the old test diffraction config will receive **no migration**;
- final diffraction config filename will be `cchq_soundphysics_compat-diffraction.toml`;
- final diffraction schema/root will be `portal_diffraction`;
- old `cchq_soundphysics_compat-diffraction-v7-1-spreading-only-test.toml` / `portal_diffraction_v7_1_spreading_only_test` are intentionally ignored;
- user-facing Beta/Phase/test/candidate terminology will be cleaned while persisted internal keys/log continuity remain where useful;
- the stale static manifest `Created-By: 21.0.11 (Debian)` line will be removed;
- HF50 values, V7.1 equations/parameters, lifecycle fixes and the deliberate eligibility bitwise `&` remain frozen.

The exact file-by-file implementation/CI checklist is already prepared in `docs/FINAL_RELEASE_EXECUTION_PLAN_2026-09-06.md`.

---

## Two runtime gates — do not collapse them

### Gate 1 — lifecycle candidate

The current candidate `4b3c8c52...` must first pass the full lifecycle runtime sequence.

### Gate 2 — final release smoke test

After Gate 1 passes, final release changes are applied and rebuilt. The resulting final JAR must then receive a **short targeted smoke test** before release freeze.

This second test is mandatory because the final build deliberately changes fresh-install behavior by:

- enabling V7.1 diffraction by default; and
- switching to a new diffraction config filename/root with no migration from the old beta config.

A successful Gate-1 lifecycle run does **not** validate those final config/default changes.

Gate 2 is intentionally short: verify new config generation/default ON, old beta config ignored, basic play-stop-play, one known V7.1 opening sanity check, synchronized playback plus pause/resume, and inspect logs. Do not redo the historical full acoustic campaign unless that smoke test exposes a regression.

---

## Current invariants worth checking during final audit

- `SoundSource.BLOCKS` distance behavior;
- center + 8 inner + 8 outer direct geometry;
- approved progressive full/partial refresh behavior;
- private per-source EFX and direct/aux reattachment on actual environment application;
- private EFX setup only once the source is PLAYING/PAUSED eligible;
- SPR `calculateOcclusion()` remains part of the normal pipeline;
- no worker decode thread owns SPR world/geometry raycasts;
- strict source generation/lifetime semantics;
- Hotfix3 partial sync grace and pending-INITIAL protection;
- Beta10 exact direct reuse / stable OpenAL write suppression;
- Beta11 exact same-clone room-ray memoization with cross-clone reuse as telemetry;
- exact eligibility expression:

  `Beta9Optimizer.isAudibleAndRecord(state.sourceId) & beta9EligibleReal(state, now)`

---

## Immediate next checkpoint

The audited lifecycle candidate is source/CI validated but has not yet received the user's runtime lifecycle test.

Candidate SHA-256:

`4b3c8c52cc00a37274d5829cff93933d6e548b733b83918fcc5570ab8d6ad3c5`

Gate-1 runtime sequence:

1. play -> stop -> play
2. pause -> resume
3. stopAll -> restart
4. sound/resource reload -> restart
5. disconnect -> rejoin -> restart
6. dimension change -> restart
7. four speakers playing for several minutes while moving
8. upload `latest.log` and `debug.log`

After a clean Gate-1 result, execute `docs/FINAL_RELEASE_EXECUTION_PLAN_2026-09-06.md`, run final CI, then perform the mandatory short Gate-2 smoke test before freezing the release.

`main` remains untouched; any eventual merge remains a separate release decision.
