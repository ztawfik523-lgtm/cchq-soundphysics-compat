# Beta11 Hotfix3 source reconstruction status

Authoritative runtime baseline: `cchq_soundphysics_compat-0.1.0-beta11-hotfix3.jar`

Authoritative Hotfix3 SHA-256:
`83500f182fc9829aa1a5a51fbfa11ba6cdfb645699b25d1c445167666dabc1ef`

Current extension branch: `phase5-test-extended`

Hotfix3 remains the behavioral authority for the frozen parity build. Phase 5 now contains an intentionally extended test candidate whose **defaults preserve the Phase 4 values**, while non-default Advanced Runtime controls are explicitly allowed to diverge for diagnosis/tuning.

## Canonical five-phase status

| Phase | Status |
| --- | --- |
| Phase 1 — Freeze and inventory binary baseline | **COMPLETE / JAR-RECHECKED** |
| Phase 2 — Reconstruct build project | **COMPLETE / JAR-RECHECKED** |
| Phase 3 — Reconstruct every Java class | **COMPLETE / RECHECKED** |
| Phase 4 — Structural and behavioral equivalence audit | **COMPLETE / RECHECKED** |
| Phase 5 — Runtime validation and source handover | **IN PROGRESS — VERIFIED TEST CANDIDATE READY / AWAITING USER RUNTIME TEST** |

Canonical plan: `docs/RECONSTRUCTION_PHASES.md`.

---

## Phase 1 — COMPLETE / JAR-RECHECKED

The exact Hotfix3 JAR independently confirms the frozen baseline:

- whole-JAR SHA-256 exactly matches the frozen authority;
- 75 ZIP entries, 65 files and 60 Java-21 classfiles;
- all frozen entry fingerprints were recorded;
- five source-relevant runtime resources were frozen byte-for-byte.

Evidence:

- `docs/baseline/BETA11_HOTFIX3_INVENTORY.md`
- `docs/baseline/HOTFIX3_SHA256SUMS.txt`
- `docs/baseline/PHASE1_FINAL_VERIFICATION.md`
- `docs/baseline/PHASE1_JAR_RECHECK_2026-09-04.md`
- `docs/PHASE1_HOTFIX3_BYTECODE_AUDIT.md`

## Phase 2 — COMPLETE / JAR-RECHECKED

Pinned build environment remains:

- Java 21
- Minecraft 1.21.1
- NeoForge 21.1.248
- ModDevGradle 2.0.144
- Gradle wrapper 9.2.1
- CC:Tweaked 1.120.2
- tested SPR artifact `qyVF9oeo-Dd2tmpsk.jar`
- tested HQ Speakers artifact `ygA78R8l-u5PEI5Ax.jar`
- Cloth Config optional UI dependency

Hotfix3 source calls SPR members widened by this mod's access transformer. The reconstruction keeps untouched SPR at runtime while `prepareSprCompileJar` creates an isolated access-transformed compile copy for javac. AT CLI 10.0.6 is build-only.

See `docs/PHASE2_BUILD_AUDIT.md`.

## Phase 3 — COMPLETE / RECHECKED

All authored Java source was reconstructed and the clean-build closure gate reconciled exact **60/60 Hotfix3 class topology** before Phase 4 began.

Final Phase 3 recheck of the Phase 4 parity source:

- head: `98e7dedb7ecf6fda22008b084b6bb41956edff78`
- run: `33924056330`
- job: `101188553632`
- result: **SUCCESS**

Evidence: `docs/PHASE3_FINAL_VERIFICATION.md`.

## Phase 4 — COMPLETE / RECHECKED / FROZEN

Final verification record:

`docs/PHASE4_FINAL_VERIFICATION.md`

Frozen branch:

`phase4-hotfix3-parity`

Frozen branch head:

`79eed29767343ee34022e8f6268b386f75e84c9f`

Final audited Phase 4 code/build head:

`98e7dedb7ecf6fda22008b084b6bb41956edff78`

Phase 4 established:

- 60/60 class paths exact;
- 60/60 structural ABI exact;
- 69/69 compiled `ConstantValue` entries exact;
- zero bootstrap/string-concat recipe mismatches;
- 11/11 Mixin/accessor semantic annotation sets reconciled;
- 5/5 non-class Hotfix3 resources byte-for-byte exact;
- 550-method method/control-flow audit;
- no unresolved proven Hotfix3 behavioral discrepancy.

The Phase 4 freeze is deliberately kept separate from all Phase 5 extensions.

---

# Phase 5 — IN PROGRESS

## Scope decision

The automated Phase 5 preparation does **not** launch Minecraft and does not attempt subjective audio validation.

The user will perform the real Minecraft/NeoForge/CC:HQ/SPR test. Before that handoff, Phase 5 has been used to make the candidate much easier to diagnose and tune while retaining parity defaults.

## Extended working branch

`phase5-test-extended`

## Frozen test candidate

`phase5-test-candidate-1`

Verified candidate commit:

`44612192d875e43ecef66ca51798cab7adb17020`

The candidate branch points to that exact verification input so later documentation/work cannot silently change the JAR being tested.

## Final automated verification

Workflow:

`.github/workflows/phase5-verify.yml`

Run:

`33927205360`

Result:

**SUCCESS**

Verified artifact:

- name: `cchq-phase5-verified-test-build`
- artifact ID: `9957268423`
- artifact digest: `sha256:7d5134c8d4f96a22effdbd0071fc57d29c3a66d9705c856c3a55c2f4edbfb0e9`
- JAR: `cchq_soundphysics_compat-0.1.0-beta11-phase5-test.jar`
- JAR SHA-256: `6d782812f7915de1870a8b5ae0f619556e7ec1d24ef2eaffa7b5b225aa00bd93`
- packaged files: 70
- packaged classfiles: 65
- exactly five new support classfiles over the Hotfix3 60-class topology

The verifier successfully checked:

- `phase4-hotfix3-parity` still points to `79eed297...`;
- Phase 5 filename/Java/NeoForge metadata use `0.1.0-beta11-phase5-test`;
- all 35 advanced/debug defaults have the intended values;
- the original Hotfix3 `ClientConfig.java` is unchanged from the frozen parity branch;
- clean Java 21 compilation;
- clean JAR build;
- required Phase 5 support classes are packaged;
- final JAR SHA generation;
- `game_launch_performed=false`.

## Phase 5 additions

A second client config is registered:

`cchq_soundphysics_compat-advanced.toml`

The original `cchq_soundphysics_compat-client.toml` front-panel config remains intact.

New **Advanced Runtime** controls include:

- room scheduler slot/stale/recent-source thresholds;
- teleport and source-movement urgency thresholds;
- all clearing-sentinel trigger/confirmation thresholds;
- synchronized-start partial-flush and stale-group timers;
- private per-source EFX enable/fallback switch;
- Beta9 whole-direct reuse;
- Beta9 room backoff and adaptive controller;
- Beta9 movement window/threshold and maximum backoff bounds;
- Beta10 exact ray cache;
- Beta11 same-clone room-ray memo;
- performance-report cadence.

All of those controls default to the verified Hotfix3 values.

Safety normalization prevents:

- inverted minimum/maximum hard-stale windows;
- a stale sync-group timeout shorter than the partial-flush grace.

Private EFX disabling actively detaches compat-owned filters before native-SPR fallback; re-enabling can retry private-EFX setup.

## Debugging retained and expanded

Targeted INFO-level categories, all default OFF:

- source lifecycle;
- room scheduler;
- clearing sentinel;
- private EFX/fallback;
- cache scope;
- synchronized starts;
- transition timing;
- startup effective-config summary.

Client-only commands:

- `/cchqphysics status`
- `/cchqphysics dump`
- `/cchqphysics refresh_rooms`
- `/cchqphysics reset_caches`
- `/cchqphysics reset_efx`
- `/cchqphysics config`

The mutating commands queue requests rather than touching OpenAL in their command callbacks. `RoomSchedulerClient.clientTick()` submits scheduler work through `CompatAudioManager.beta10OnSoundThread(...)`, and `SoundPhysicsBridge.schedulerTick()` consumes the queued reset/refresh operations there.

Detailed handoff/testing instructions:

`docs/PHASE5_TEST_BUILD_PREP.md`

## What has NOT been performed

Still intentionally not done:

- launching Minecraft with the candidate;
- runtime Mixin application confirmation from a real client log;
- actual CC:HQ playback test;
- actual SPR direct occlusion/reverb listening test;
- actual OpenAL/EFX hardware/runtime confirmation;
- synchronized-start observation in game;
- disconnect/world-reload/stress confirmation in game;
- final release/source handover closure.

## Exact next step

**User runtime validation of `phase5-test-candidate-1` / the verified Phase 5 JAR.**

Leave all acoustic/advanced controls at defaults for the first test. Use `/cchqphysics dump` and the targeted debug toggles if a discrepancy occurs, then return the relevant logs/configs and observation for analysis.

Phase 5 must remain open until that runtime evidence is reviewed.
