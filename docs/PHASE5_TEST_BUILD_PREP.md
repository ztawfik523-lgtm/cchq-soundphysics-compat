# Phase 5 test-build preparation

Status: **PREPARED / AWAITING USER RUNTIME VALIDATION**

Phase 5 is intentionally **not complete**. This record covers the automated preparation work only. No Minecraft client was launched by CI or by the reconstruction workflow, and no subjective audio/runtime claim is made here.

## Frozen Phase 4 authority

The verified Hotfix3-equivalent reconstruction remains frozen separately:

- branch: `phase4-hotfix3-parity`
- frozen branch head: `79eed29767343ee34022e8f6268b386f75e84c9f`
- final audited Phase 4 code/build head: `98e7dedb7ecf6fda22008b084b6bb41956edff78`
- authoritative Hotfix3 SHA-256: `83500f182fc9829aa1a5a51fbfa11ba6cdfb645699b25d1c445167666dabc1ef`

Phase 5 extensions do not modify that branch.

## Verified Phase 5 candidate

- working branch: `phase5-test-extended`
- frozen test candidate branch: `phase5-test-candidate-1`
- verified candidate commit: `44612192d875e43ecef66ca51798cab7adb17020`
- verification workflow: `.github/workflows/phase5-verify.yml`
- verification run: `33927205360`
- result: **SUCCESS**
- artifact: `cchq-phase5-verified-test-build`
- artifact ID: `9957268423`
- artifact digest: `sha256:7d5134c8d4f96a22effdbd0071fc57d29c3a66d9705c856c3a55c2f4edbfb0e9`
- JAR: `cchq_soundphysics_compat-0.1.0-beta11-phase5-test.jar`
- JAR SHA-256: `6d782812f7915de1870a8b5ae0f619556e7ec1d24ef2eaffa7b5b225aa00bd93`
- packaged files: 70
- packaged classfiles: 65
- additional classfiles over Hotfix3: exactly 5

The five intentional new classfiles are:

- `dev/cchqphysics/compat/audio/DebugCommands.class`
- `dev/cchqphysics/compat/audio/DebugControl.class`
- `dev/cchqphysics/compat/audio/DebugDiagnostics.class`
- `dev/cchqphysics/compat/config/ExtendedClientConfig.class`
- `dev/cchqphysics/compat/config/ExtendedClientConfigAccess.class`

The packaged NeoForge metadata identifies this build as `0.1.0-beta11-phase5-test`, so it cannot be mistaken for the frozen Hotfix3 parity artifact in logs or the Mods screen.

## What the pure verifier proves

Run `33927205360` performed no source mutation and no game launch. It successfully checked:

1. the frozen Phase 4 branch still points to `79eed29767343ee34022e8f6268b386f75e84c9f`;
2. Gradle, Java and `neoforge.mods.toml` all identify the extended artifact as `0.1.0-beta11-phase5-test`;
3. all **35** new advanced/debug defaults are the intended Hotfix3-equivalent values or diagnostics-off defaults;
4. the original `ClientConfig.java` is unchanged from the frozen Phase 4 branch;
5. the complete Phase 5 source clean-compiles on Java 21;
6. the test JAR builds successfully;
7. all five intended Phase 5 support classes are packaged;
8. the packaged NeoForge metadata contains the Phase 5 test version;
9. a SHA-256 and build-info record are emitted with the artifact;
10. `game_launch_performed=false` is explicitly recorded.

## Added configuration

Phase 5 retains the existing Hotfix3 front-panel `ClientConfig` and adds a second client spec:

`cchq_soundphysics_compat-advanced.toml`

All new behavior-changing controls default to the values already verified in Phase 4. Leaving them untouched preserves the verified scheduler/acoustic constants. Moving them away from defaults intentionally changes behavior and is for validation/tuning only.

### Scheduler controls

- room scheduler slot — 50 ms
- minimum hard-stale time — 500 ms
- maximum hard-stale time — 2000 ms
- recent-source eligibility window — 1000 ms
- listener teleport threshold — 4.0 blocks
- source-movement urgent threshold — 0.10 blocks

The minimum/maximum hard-stale getters self-normalize if their configured order is inverted.

### Clearing-sentinel controls

- movement trigger — 0.05 blocks
- raw-occluded threshold — 0.075
- re-arm center threshold — 0.12
- open-center threshold — 0.035
- center-drop trigger — 0.15
- confirm raw drop — 0.035
- confirm cutoff rise — 0.055
- clear-trigger cooldown — 300 ms

### Sync controls

- incomplete synchronized-group grace — 100 ms
- stale-group cleanup — 5000 ms

The stale-group value is runtime-normalized so it cannot become shorter than the partial-flush grace.

### Feature / optimization controls

- private per-source EFX — ON
- Beta9 whole-direct-result reuse — ON
- Beta9 stable/relevance room backoff — ON
- Beta9 adaptive load controller — ON
- Beta9 recent-listener-movement window — 400 ms
- Beta9 listener-movement reset threshold — 0.05 blocks
- Beta9 maximum room-backoff factor — 2.0x
- Beta9 maximum backed-off room interval — 1500 ms
- Beta10 exact direct/SPR ray cache — ON
- Beta11 same-clone room-ray memo — ON
- performance report interval — 10000 ms

Disabling private EFX now actively detaches compat-owned filters before falling back to native SPR writes. Re-enabling is allowed to attempt a fresh private-EFX setup.

## Debug / validation logging

All targeted debug categories are OFF by default and emit at INFO level only when enabled:

- source lifecycle
- room scheduler
- clearing sentinel
- EFX lifecycle/fallback
- cache scope
- synchronized starts
- transition timing
- startup effective-config summary

These are exposed in Cloth Config under **Debug & Validation**. The low-level behavior controls are exposed under **Advanced Runtime**.

## Client-only diagnostic commands

Phase 5 registers client-side commands only; they do not mutate server state:

- `/cchqphysics status` — compact aggregate state
- `/cchqphysics dump` — config, aggregate state, per-source acoustic state and per-source EFX state to `latest.log`
- `/cchqphysics refresh_rooms` — queue all currently eligible compat sources for fresh room evaluation
- `/cchqphysics reset_caches` — clear optimization caches/stability state without unregistering active sources
- `/cchqphysics reset_efx` — detach/reset private EFX state and allow clean retry
- `/cchqphysics config` — display/log the effective advanced configuration

The mutating diagnostic requests are not executed in the command callback. `MinecraftRoomSchedulerMixin` invokes `RoomSchedulerClient.clientTick()`, which schedules `SoundPhysicsBridge.schedulerTick()` through `CompatAudioManager.beta10OnSoundThread(...)`; queued refresh/reset requests are consumed there on the existing sound-thread executor path.

## Recommended first user test

For the first real-game run:

1. remove/disable any previous compat JAR so only the Phase 5 test JAR is loaded;
2. leave all acoustic and advanced controls at their defaults;
3. enable `Startup config summary`, `Source lifecycle log`, `EFX lifecycle log`, `Transition timing log`, and `Sync grouping log` first;
4. start Minecraft normally and verify the Mods screen reports `0.1.0-beta11-phase5-test`;
5. test one ordinary HQ speaker in open air;
6. test the same source through a wall / around an opening;
7. move listener and speaker where practical;
8. stop/restart playback;
9. test multiple speakers and synchronized starts;
10. run `/cchqphysics dump` after a representative working case and again immediately after any bad case;
11. only if a problem appears, use the Advanced Runtime switches one subsystem at a time to isolate it.

Useful isolation sequence for an acoustic problem:

- `Private per-source EFX` OFF -> distinguishes isolated-EFX handling from native SPR fallback;
- `Beta9 whole-direct reuse` OFF -> rules out exact whole-result reuse;
- `Beta10 exact ray cache` OFF -> rules out exact direct-ray sharing;
- `Beta11 room-ray memo` OFF -> rules out same-clone room-ray memoization;
- `Beta9 room backoff` OFF -> forces the base room interval;
- `/cchqphysics reset_caches` after changing cache-related controls;
- `/cchqphysics refresh_rooms` after changing room-related controls;
- `/cchqphysics reset_efx` to retry private EFX without restarting.

## Evidence to return after the real-game test

If the build works, record that fact plus the scenarios exercised.

If anything is wrong, provide:

- `latest.log` and, if present, `debug.log`;
- `cchq_soundphysics_compat-client.toml`;
- `cchq_soundphysics_compat-advanced.toml`;
- what speaker/source scenario was running;
- whether the issue is startup, playback, distance, direct occlusion, reverb/room behavior, synchronization, movement, stopping/restarting, or cleanup;
- the output/log section from `/cchqphysics dump` near the failure;
- whether toggling one of the isolation controls changes the symptom.

## Phase boundary

This candidate is **ready for user runtime testing**, but Phase 5 remains open.

Phase 5 may only be closed after the user launches the candidate in the intended Minecraft/NeoForge/mod stack and the resulting runtime/audio evidence is reviewed. The automated preparation described here is not a substitute for that test.
