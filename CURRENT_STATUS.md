# Current project status — 2026-09-06

This file is the current operational status for the Beta11 reconstruction/finalization line. Older Phase 1–5 documents remain useful historical evidence, but several of them predate the completed Phase 5 acoustic/runtime work and must not be used as the current state by themselves.

## Non-negotiable direction

- No more acoustic features or tuning.
- V7.1 sound behavior is frozen.
- Remaining work is performance, lifecycle/stability, bug fixing, validation, cleanup and release finishing only.
- Do not merge to `main` without explicit user instruction.
- Do not move existing archive refs.
- No Lua changes.
- Preserve `Beta9Optimizer.isAudibleAndRecord(sourceId) & beta9EligibleReal(...)` with `&`.
- Do not claim a Minecraft/audio runtime test unless one was actually performed by the user/runtime environment.

## Frozen acoustic baseline

Frozen commit:

`ffcf5f6e05d85b69f1f1dff8cfae1b082b71604d`

Frozen refs:

- `phase5-diffraction-v7-1-runtime-approved`
- `archive-phase5-diffraction-v7-1-runtime-approved`

The archive ref must remain immutable.

Approved V7.1 runtime JAR SHA-256:

`30d457c2a52672f893b1076938e2fdea3f41759173dfd843ff652bd490692101`

Approved HF50 behavior is also frozen and must not be changed.

## Performance pass

Clean performance head:

`962eab8b052466ca984496a7dec0767dc65803f4`

Performance changes were deliberately limited to housekeeping/allocation behavior in `VerticalDiffractionRelief` and version identity. No multi-cell topology cache was added because it could change acoustic reuse timing.

The 1- and 4-source benchmark showed:

- portal topology scanning does not multiply by source count;
- listener-side portal leg work is shared;
- source-side portal work scales per source as intended;
- portal-ray CPU cost is negligible relative to the existing progressive/SPR movement workload;
- 4-source moving baseline was roughly 13.7–14.6 ms/s acoustic and 6.8–7.4 ms/s SPR in strict fully-contained windows.

The performance candidate still needs the user's final runtime spot-check; no later assistant-side work should claim that spot-check happened unless logs are supplied.

## Lifecycle base

Clean lifecycle base used for release hardening:

`be03d30efe98ca03bdf27764bcea567df5ef3875`

The lifecycle line keeps `pause`, `resume`, `stopAll` and `emergencyShutdown` integration. Redundant direct `destroy` and `reload` injections were removed; the actual reset path is retained through `stopAll` / `emergencyShutdown` and `CompatAudioManager.invalidateSession()`.

`invalidateSession()` clears session epoch, ready decode starts, queued decoder tasks, decoded cache map, source generations, sync coordinator state and SoundPhysics source IDs.

`SoundPhysicsBridge.clearSourceIds()` clears source state plus `PositionStabilizer`, `VerticalDiffractionRelief`, `AcousticCapture`, performance stats, room scheduler state and room-environment reflection/introspection state.

## Release-hardening branch

Branch:

`phase5-v7-1-release-hardening`

Current clean head when this file was written:

`e22dbc6241555a9b17f3f2e7f570f7b943df47d4`

The production-tree delta from the clean lifecycle base is only:

- `src/main/java/dev/cchqphysics/compat/audio/CompatAudioManager.java`

Temporary patch helpers/workflows were removed after validation.

### Decode/lifetime hardening now present

- Single decoder still has one worker, but pending decode work is bounded to 8 queued tasks instead of an unbounded executor queue.
- Queue overload falls back to normal CC:HQ handling instead of accumulating unlimited work.
- Session invalidation clears queued decoder tasks.
- Decode completion checks session epoch before enqueueing a start.
- Decode completion checks the current source generation before enqueueing a start.
- Failed decode entries are removed from the decode cache.
- Ready playback metadata no longer retains a second copy/reference to the original encoded payload byte array.
- Completed decoded cache is limited by both entry count (4) and a 128 MiB decoded-byte budget.
- Completed-cache trimming now runs every 20 client ticks rather than every 200 ticks.
- Native PCM/stream fallback only hands off/stops an existing compat-owned source; native-only packets do not create generation-map entries.
- Native stop packets do not create compat generation entries for sources the compat never owned.
- OpenAL upload staging now uses explicitly freed native memory.
- Failed OpenAL buffer uploads delete the newly-generated buffer where possible.
- Source teardown attempts all cleanup steps independently so one unregister/OpenAL failure does not skip buffer release.
- `alGenBuffers()` and `alGenSources()` result `0` is rejected before compat state registration, avoiding stale source-0 state on OpenAL allocation failure.

### Validation evidence

Final OpenAL-ID hardening source commit:

`206aa953aa89e63fecf2f520a2509188557c8f92`

Validation workflow run:

`33964401477` — success through classpath verification, resource wiring, clean, Java 21 compile, JAR build, JAR inspection and artifact upload.

Artifact ID:

`9968976151`

Artifact ZIP digest:

`sha256:d69c9a907a21544f3fd5b7b179895a4fff613dfe7900bb4c897f009e7a883678`

Built JAR SHA-256:

`cf126e2e15e4fbe0a389915eb4b799b798d046df1bfccbf1fa053af97d3afa54`

Build metadata explicitly recorded:

- `invalid_al_buffer_id_guard=true`
- `invalid_al_source_id_guard=true`
- `acoustic_model_changes=false`
- `lua_changes=false`
- `game_launch_performed=false`

## Release-surface audit findings

These are not all safe to change automatically because some are behavior/config migration decisions.

### BLOCKER — diffraction default is still OFF

`DiffractionConfig.ENABLED` defaults to `false` and the startup text explicitly says V7.1 diffraction is OFF by default.

The approved V7.1 listening tests were performed with diffraction enabled at runtime. Therefore a fresh install does not automatically reproduce the full approved V7.1 opening behavior.

This requires an explicit release decision:

1. Keep diffraction opt-in / OFF by default. Safer compatibility/default behavior, but users do not get the approved opening fix automatically.
2. Make diffraction ON by default. Fresh installs get the approved V7.1 behavior, but this changes default acoustic behavior for every user.

Do not choose this silently.

### BLOCKER — release identity is inconsistent

Current release-facing identities do not agree:

- Gradle/JAR version: `0.1.0-beta11-phase5-v7-1-performance-test`
- `CCHQSoundPhysicsCompat.VERSION`: `0.1.0-beta11-phase5-diffraction-v7-1-spreading-only-test`
- `neoforge.mods.toml` version/description: older V7.1 experimental-test wording

Before final packaging, one final release/candidate version must be chosen and applied consistently.

### BLOCKER/DECISION — diffraction config namespace/file is experimental

Current registered file:

`cchq_soundphysics_compat-diffraction-v7-1-spreading-only-test.toml`

Current TOML section:

`portal_diffraction_v7_1_spreading_only_test`

Renaming these is cleaner for release, but it can stop existing user/test values from being loaded unless a migration/compatibility path is deliberately preserved. Do not rename blindly.

### Cleanup — user-facing experimental wording remains

Examples still present in runtime/menu/log surfaces:

- `Experimental V7.1 spreading-only...`
- `Phase-5 V7.1...`
- `diffraction test`
- `[phase5/dump]`, `[phase5/config]`, `[phase5/source]`
- config-screen Phase 5 testing descriptions

These are release-polish issues, not acoustic-model changes, but should be cleaned only in the final packaging pass so runtime test logs remain easy to compare until then.

### Historical repository material

Old Phase 1–5 docs, patch tools and historical CI workflows are provenance. They should not be mass-deleted just to make the repository look clean. Release artifacts can remain clean without destroying reconstruction history.

## Static audit conclusion

No additional acoustic-model change is justified.

The concrete release-grade problems found and fixed in this hardening pass were decode backpressure/session staleness, encoded/decoded memory retention, native-memory/OpenAL failure cleanup, generation-map pollution on native-only traffic, and invalid OpenAL object-ID handling.

The remaining unresolved items are primarily runtime validation and release-policy/packaging decisions, not another engineering feature pass.

## Next actions

When the user is ready to test:

1. Runtime spot-check the current hardening candidate with normal playback and 4 moving speakers; compare performance/no regressions.
2. Lifecycle matrix: play→stop→play, pause/resume, stopAll, sound/resource reload, disconnect/rejoin, dimension change.
3. Short soak for accumulating state/errors/memory trend.
4. Fix only proven bugs.
5. Resolve the explicit diffraction-default decision.
6. Choose final version/config migration policy and clean release-facing experimental wording.
7. Run final Java 21 build/JAR audit, record SHA/class/resources, then create a new immutable final archive ref.
8. `main` remains untouched unless explicitly requested.

Current `main` remains at its original baseline commit and has not been merged with Phase 5 work.
