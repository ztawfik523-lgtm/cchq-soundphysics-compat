# Maintained source handover

## Purpose

CC:HQ Sound Physics Compat bridges CC:HQ whole-file speaker playback into Sound Physics Remastered (SPR) as real positional OpenAL sources on Minecraft 1.21.1 / NeoForge.

The historical Beta11 Hotfix3 JAR was used as the behavioral authority during reconstruction. The maintained source now exists independently of that binary. The frozen parity branches remain available as historical evidence and should not be used for ongoing feature work.

## Branch map

### Frozen Hotfix3 parity

- `phase4-hotfix3-parity`
- `archive-phase4-hotfix3-parity`
- branch head: `79eed29767343ee34022e8f6268b386f75e84c9f`
- final audited Phase-4 code/build commit: `98e7dedb7ecf6fda22008b084b6bb41956edff78`

These branches represent the reconstructed Hotfix3 behavior before Phase-5 convenience/config/debug extensions.

### Runtime-tested extended candidate

- `phase5-test-candidate-1`
- commit: `44612192d875e43ecef66ca51798cab7adb17020`

This candidate passed clean CI and multiple real Windows/OpenAL/Minecraft runtime tests. It contains the advanced config and diagnostics but not the optional synchronized occluded-source suppression feature.

### Finalization branch

- `phase5-finalization`

This branch adds the final optional synchronized multi-speaker mixing feature. The feature defaults OFF, so the already-validated Phase-5 behavior remains the default. Phase 5 must not be formally marked complete until the user performs the final feature-specific listening test.

## Major subsystems

### `CompatAudioManager`

Owns compat playback lifecycle:

- intercepts supported HQ whole-file payloads;
- decodes off-thread;
- shares decoded/OpenAL buffers;
- creates positional OpenAL sources;
- applies the approved distance curve;
- registers/unregisters acoustic state;
- routes synchronized starts through `SyncStartCoordinator`;
- updates source distance/audibility;
- invokes `SoundPhysicsBridge` on the sound-thread path;
- cleans up sources/buffers on stop, reload, disconnect and sound-engine reset.

Do not move OpenAL mutations onto arbitrary worker threads.

### `HQPayloadView`

Reflection/shape bridge for HQ Speakers packets. Packet-shape failures must fall back safely rather than crash playback.

### `AudioDecoder`

Decodes supported whole-file formats and converts multichannel/stereo input to mono PCM16 for positional OpenAL playback.

### `SyncStartCoordinator`

Groups OpenAL sources by HQ sync metadata and starts each complete group with `alSourcePlayv`. Incomplete declared groups receive the Hotfix3 grace period and then all sources that actually arrived are started together.

Observed HQ runtime quirk: in user tests, declared expected group size appeared one larger than the compat audio-source count (`2/3`, `4/5`). No physical speaker was observed missing. The partial-flush behavior therefore remains important and should not be removed casually.

### `SoundPhysicsBridge`

Coordinates SPR processing, room scheduling, direct/room state, source stamps, transition detection, listener teleport invalidation, and EFX/native fallback behavior.

### `ProgressiveOcclusionModel`

Approved direct-occlusion model:

- center path;
- 8 inner-ring paths;
- 8 outer-ring paths;
- full refresh = 17 paths;
- adaptive partial refresh = center + one 8-path ring;
- current Hotfix3 weighting and open-center ring scaling.

Do not replace the geometry with an approximation under the parity/default preset.

### `EnvironmentSmoother`

Owns private per-source EFX isolation and smoothed application of direct cutoff/gain and room/reverb parameters.

Important invariant: every real environment application must reattach required direct/aux EFX. Runtime telemetry verified `efxApplies == efxReattachPasses` across the stress tests.

`/cchqphysics reset_efx` intentionally tears down and recreates private filters. The user observed a tiny quiet static artifact for a fraction of a second only when explicitly running that debug command. Normal playback did not exhibit that artifact.

### `Beta9Optimizer`

Controls whole-direct reuse, audibility/distance relevance, room stability/backoff and adaptive load pressure.

### `Beta10Optimizer`

Provides exact direct/SPR ray sharing and bit-identical OpenAL write suppression.

### `Beta11RoomRayCache`

Memoizes exact SPR room/bounce ray results only within the current cloned `BlockGetter` scope. Cross-clone reuse remains telemetry-only; do not enable cross-clone reuse without a new correctness proof.

## Configuration

### Normal config

`cchq_soundphysics_compat-client.toml`

Contains user-facing acoustics, distance/range, progressive occlusion, direction/reflection stabilization, smoothing and base performance controls.

### Advanced/debug config

`cchq_soundphysics_compat-advanced.toml`

Exposes scheduler, sentinel, sync, cache, EFX and targeted validation settings. Defaults preserve the verified Hotfix3 values; diagnostic categories default OFF.

### Optional synchronized-mix config

`cchq_soundphysics_compat-mixing.toml`

Contains the post-parity synchronized multi-speaker feature. Default is OFF.

When enabled, a source is eligible for extra attenuation only if:

1. it belongs to a non-null HQ sync group;
2. at least one other active compat source has the same sync-group ID;
3. that peer also has the exact same decoded payload key;
4. the source's own progressive raw occlusion is above the configured threshold.

The attenuation is source-local and calculated from that source's own occlusion. Clear synchronized copies are untouched. A configurable minimum factor prevents blocked sources being completely removed from the room mix.

The feature does not choose a global/master source and does not share EFX state between speakers.

## Client diagnostic commands

- `/cchqphysics status` — compact aggregate state
- `/cchqphysics dump` — config + source/acoustic/EFX + synchronized-mix snapshot to log
- `/cchqphysics refresh_rooms` — force fresh room consideration
- `/cchqphysics reset_caches` — safely invalidate optimization caches
- `/cchqphysics reset_efx` — destroy/retry private EFX state
- `/cchqphysics config` — print effective advanced + mixing config

Mutating debug requests are queued onto the existing sound-thread path rather than directly changing OpenAL from the client command callback.

## Runtime evidence already obtained

Real user testing on Windows 10 / Microsoft OpenJDK 21.0.7 / AMD RX590 / OpenAL Soft validated:

- NeoForge startup;
- compat mod discovery/version identity;
- configured Mixins and accessors applying;
- SPR EFX recognition and four auxiliary sends;
- HQ playback interception;
- 12 concurrent compat sources;
- 2-source and 4-source scenarios;
- stationary and moving listeners;
- thick-wall obstruction;
- doorway/open transitions;
- clearing sentinel activity;
- Beta9 load/backoff behavior;
- Beta10 direct-ray reuse;
- Beta11 room-ray memo telemetry;
- private EFX creation/isolation;
- required EFX reattachment invariant;
- cache reset, room refresh and EFX reset debug commands;
- source cleanup on disconnect/shutdown;
- synchronized partial-group start behavior;
- user-reported correct direction, wall muffling, reverb and synchronized starts under the parity/default behavior.

The remaining runtime task before formal Phase-5 closure is a short listening test with the optional synchronized-mix feature enabled in the same multi-speaker/wall scene that motivated it.

## Critical invariants for future maintenance

- No Lua-side behavior is required for the compat.
- Preserve `SoundSource.BLOCKS` volume behavior unless intentionally versioned.
- No private EFX creation before an OpenAL source is PLAYING/PAUSED eligible.
- Required EFX direct/aux attachments must not be optimized away.
- Preserve strict source identity/generation handling.
- Physics work must not intentionally change PCM sample position, OpenAL playback clock or buffer offset.
- Do not cancel/replace SPR `calculateOcclusion()` wholesale.
- Do not perform SPR world/geometry raycasts on arbitrary worker threads.
- Preserve verifier-safe normal Java source; do not reproduce historical hand-patched operand-stack bytecode merely for byte-shape identity.
- Any future optimization that changes acoustic behavior must be opt-in/versioned or separately validated rather than silently altering the parity/default preset.

## Build

See `docs/BUILD_FROM_SOURCE.md` for the reproducible clean build and the compile-time SPR access-transform contract.

## Historical reconstruction evidence

The reconstruction/audit evidence remains in the repository for traceability, including:

- Phase-1 binary inventory and hashes;
- Phase-2 build audit;
- Phase-3 source closure;
- Phase-4 ABI/behavioral verification;
- Phase-5 runtime-test records.

These are evidence, not prerequisites for ordinary future development.
