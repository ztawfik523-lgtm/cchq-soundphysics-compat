# Beta11 Hotfix3 Reconstruction Guide

This document is the durable handoff for reconstructing the tested **CC:HQ Sound Physics Compat Beta11 Hotfix3** source tree. It is intentionally detailed so each bounded automation/run can start from GitHub state rather than relying on conversational memory.

## 1. Authority and goal

Authoritative tested artifact:

`cchq_soundphysics_compat-0.1.0-beta11-hotfix3.jar`

SHA-256:

`83500f182fc9829aa1a5a51fbfa11ba6cdfb645699b25d1c445167666dabc1ef`

Repository:

`ztawfik523-lgtm/cchq-soundphysics-compat`

Reconstruction branch:

`beta11-source-reconstruction`

The goal is **not** to reproduce the lost original `.java` files character-for-character. The goal is a complete, readable, rebuildable source project whose runtime behavior matches the tested Hotfix3 baseline closely enough to become the safe development base for Beta11.1.

Do not declare reconstruction complete just because it compiles. Compilation is only one checkpoint.

## 2. Runtime/dependency target

- Minecraft 1.21.1
- NeoForge 21.1.248
- Java 21
- CC:Tweaked 1.120.2
- CC:HQ Speakers 1.1.4-neoforge-1.21.1
- Sound Physics Remastered 1.21.1-1.5.1
- Compat is client-only

The user tests in a heavy ATM10 environment eventually, but reconstruction correctness should be established in the lightweight source/build/test setup first. Avoid requiring repeated ATM10 launches during reconstruction.

## 3. Reconstruction rules

Every bounded reconstruction run must follow these rules:

1. Read the latest branch state and `RECONSTRUCTION_STATUS.md` first.
2. Treat the tested Hotfix3 artifact/known bytecode behavior as authoritative.
3. Reconstruct only the bounded area assigned to the run.
4. Do **not** optimize while reconstructing.
5. Do **not** silently improve, simplify, or redesign behavior.
6. If evidence is insufficient, record the uncertainty/TODO instead of guessing.
7. Preserve method descriptors, constants, mixin targets, OpenAL ordering, lifecycle ordering, and cache/scheduler semantics unless the baseline proves otherwise.
8. Commit the completed bounded work before ending the run.
9. Update `RECONSTRUCTION_STATUS.md` with what is verified, what is reconstructed, and what remains.
10. Do not merge this branch into `main` until baseline audit + build + correctness verification are complete.

## 4. Frozen acoustic/runtime invariants

These are non-negotiable unless the Hotfix3 baseline proves a different detail.

### Playback / decoding

- Intercept CC:HQ high-level whole-file playback without Lua-side changes.
- Decode off-thread.
- Convert stereo/multichannel decoded audio to mono PCM for positional OpenAL playback.
- Shared OpenAL buffer/refcount behavior is preserved for synchronized speakers using the same payload.
- Physics scheduling must never change PCM sample position, OpenAL playback clock, buffer offset, or sync-group timing.
- Strict source lifetime identity is source id + monotonic generation + speaker UUID.

### Synchronization

- Full synchronized groups use `AL10.alSourcePlayv(int[])`.
- Hotfix3 fixes the latent incomplete-group bug where metadata can declare 12 speakers but only 11 sources arrive.
- Incomplete groups get a 100 ms grace period (`PARTIAL_FLUSH_NS = 100_000_000L`).
- During that grace period, pending `AL_INITIAL` sources must be treated as alive by lifecycle maintenance rather than destroyed.
- After grace, all sources that actually arrived are started together with one `playv`.
- `removeSource` / `clear` behavior must remain correct.

### Direct occlusion

Approved direct model:

- 17 conceptual paths total:
  - 1 center
  - 8 inner
  - 8 outer
- First/full refresh uses all 17.
- Progressive refresh alternates 9-path work:
  - center + inner while reusing outer, then
  - center + outer while reusing inner.
- Exact weighting formula remains unchanged.
- Beta10 exact direct `runOcclusion` result sharing/cache remains part of the Hotfix3 baseline.
- Direct path is already cheap and must not be degraded during reconstruction.

### SPR integration

- Main SPR processing target is `SoundPhysics.processSound(int sourceId, double x, double y, double z, SoundSource category, ResourceLocation sound)`.
- The 7-argument `auxOnly=true` form still performs native direct occlusion first; do not assume it bypasses direct work.
- Do **not** inject/cancel/replace SPR `calculateOcclusion()`; the earlier alpha13 approach caused severe sound-thread stalls after geometry changes.
- Do not reintroduce the earlier alpha14/15 invoker approach.
- No worker-thread SPR world/geometry raycasts.
- SPR listener uses current camera position.
- Safe cached clone comes from `CachingClientLevel.sound_physics_remastered$getCachedClone()`.
- If safe clone access is unavailable, exact room reuse must bypass rather than use unsafe world access.

### Room/bounce cache in Beta11

- Beta11 redirects the two environment/bounce `RaycastUtils.rayCast` callsites inside SPR `evaluateEnvironment`.
- Cache is exact only.
- 8192 slots, 8 probes, current/previous banks.
- Key uses exact double bits for from/to xyz + ignored block.
- Value stores the actual `BlockHitResult`.
- Cache is scoped to actual `BlockGetter` identity.
- Safe clone replacement rotates banks.
- Actual cross-clone reuse is disabled in Hotfix3; `crossCloneWouldReuse` is telemetry only.
- Shared-airspace/listener-dependent ray work stays live.
- Cache is HQ-context-only; ordinary non-HQ SPR sounds must remain unaffected.

### EFX

This is one of the most important invariants in the entire project.

- Use private per-source EFX to avoid SPR global mutable-filter contamination across sources.
- **Every actual environment application must reattach direct/aux EFX.**
- Parameter-write suppression is allowed, but attachment itself must not be optimized away from an environment apply.
- Beta2 tried attach-once behavior and broke muffling; do not repeat it.
- Do not create private EFX before a source is PLAYING/PAUSED eligible.

### Position / distance

- Preserve `PositionStabilizer` semantics.
- Preserve the approved distance curve using `SoundSource.BLOCKS`.
- SPR may move the apparent reflected source position; preserve Hotfix3 position restoration/stabilization behavior.

## 5. Known Hotfix3 runtime result

Hotfix3 is the current known-good candidate.

Verified behavior from the last runtime test:

- 11-speaker playback remained `active=11 eligible=11 maxActive=11` instead of collapsing to zero.
- Later 7-speaker group remained `active=7 eligible=7 maxActive=7`.
- Repeated stop/play torture test showed private EFX filter IDs being reused rather than leaking monotonically.
- No compat exceptions, VerifyErrors, OpenAL errors, or EFX failures were observed.
- EFX invariant held: `efxApplies == efxReattachPasses` throughout active windows.
- Camera-only movement did not spuriously wake geometry scheduling (`movementResets=0`).
- Direct cache/write suppression remained effective.
- Room cache works inside a clone, but stationary clone replacement roughly once per second prevents much same-clone stationary reuse.
- `crossCloneWouldReuse` telemetry showed many repeated exact room ray keys across successive clones, but actual cross-clone reuse remains disabled.
- One transition tail reached roughly 303 ms, but queue was not clogged and user reported correct sound; do not redesign scheduler solely because of one sample.

Important performance interpretation:

- Hotfix3/Beta11 stationary direct ray CPU is already tiny (roughly sub-millisecond per second in tested windows).
- Room/environment geometry is the larger future target.
- Verbose SPR profiling contaminates timing; do not use verbose-profile startup spikes as proof that prewarming is needed.

## 6. Important historical failures to avoid

### Original Beta11 VerifyError

Original Beta11 crashed on Singleplayer with:

`java.lang.VerifyError: Expecting a stackmap frame at branch target 27`

in `Beta10Optimizer.beta11RoomCacheActive()`.

Cause: a branch-containing injected method lacked a valid `StackMapTable`.

Hotfix3 contains the verifier-safe fix. Source reconstruction should express the working Java logic normally so javac generates correct frames.

### Incomplete sync group no-sound bug

With the first fixed Beta11, HQ metadata declared `syncGroupSize=12` while only 11 OpenAL sources arrived.

Observed sequence:

- ready 11/12
- initial physics/rays appeared briefly
- group never started
- sources remained `AL_INITIAL`
- maintenance considered only PLAYING/PAUSED alive
- INITIAL sources were destroyed
- no sound; rays disappeared

Hotfix3 behavior described in section 4 must be preserved.

### Alpha13 direct-occlusion replacement

Do not replace/cancel SPR `calculateOcclusion()`. That earlier strategy caused severe stalls after geometry changes.

### Beta2 EFX attach-once

Do not suppress EFX reattachment merely because parameters did not change. That broke muffling.

## 7. Reconstructed source already present

Check `RECONSTRUCTION_STATUS.md` for the latest exact list. As of this handoff, the branch already contains source for at least:

Original Beta11 build-input source:

- `AudioDecoder.java`
- `Beta11RoomRayCache.java`
- `SoundPhysicsRoomRayMemoMixin.java`

Reconstructed against Hotfix3 bytecode:

- `CCHQSoundPhysicsCompat.java`
- `DecodedAudio.java`
- `HQPayloadView.java`
- `DistanceBridge.java`
- `RoomSchedulerClient.java`
- `ConfigScreenFactory.java`
- `MinecraftMixin.java`
- `MinecraftRoomSchedulerMixin.java`
- `SoundEngineAccessor.java`
- `SoundManagerAccessor.java`
- `SoundPhysicsEnvironmentMixin.java`
- `SoundPhysicsPositionMixin.java`

Resources recovered from the tested artifact include:

- `META-INF/neoforge.mods.toml`
- `META-INF/accesstransformer.cfg`
- `cchq_soundphysics_compat.mixins.json`
- `assets/cchq_soundphysics_compat/lang/en_us.json`

The Java 21 / NeoForge project skeleton is provisional until a full compile succeeds.

## 8. Primary classes still requiring reconstruction/verification

Highest priority:

- `CompatAudioManager`
- `SyncStartCoordinator`
- `Beta10Optimizer`
- `SoundPhysicsBridge`
- `AcousticCapture`
- `EnvironmentSmoother`
- `ProgressiveOcclusionModel`
- `PositionStabilizer`
- `Beta9Optimizer`
- `PerformanceStats`
- `AttenuationBridge`

Remaining integration/config/mixins:

- `ClientConfig`
- `ClientConfigAccess`
- `ClothConfigScreen`
- `HQSpeakerClientHandlerMixin`
- `HQSpeakerStopPacketMixin`
- `SoundEngineLifecycleMixin`
- `SoundPhysicsOcclusionMemoMixin`

Also account for nested/synthetic classes present in the artifact. They do not all need hand-authored source if javac naturally regenerates them from the reconstructed outer/source structure, but the final class inventory must be understood.

## 9. Bounded reconstruction sequence

The scheduled sequence is intentionally split to reduce hallucination/rushing risk.

### Pass 1 — core playback and lifecycle

Focus:

- `CompatAudioManager`
- `SyncStartCoordinator` and nested group state
- source creation/start/stop/maintenance
- Hotfix3 pending-INITIAL protection
- room-cache teardown/lifecycle hooks

Stop after this bounded area is reconstructed and status is updated. Do not continue into optimizer/acoustic work in the same run unless required to resolve a compile-level type dependency and the behavior is already evidenced.

### Pass 2 — SPR/acoustic core

Focus:

- `SoundPhysicsBridge`
- `Beta10Optimizer`
- `AcousticCapture`
- environment application/EFX behavior
- verifier-safe source form of `beta11RoomCacheActive()`

Preserve the EFX reattachment invariant exactly.

### Pass 3 — direct/scheduler/helpers

Focus:

- `ProgressiveOcclusionModel`
- `PositionStabilizer`
- `EnvironmentSmoother`
- `Beta9Optimizer`
- `PerformanceStats`
- `AttenuationBridge`
- any scheduler/helper code not already reconstructed

Do not tune constants; reconstruct baseline constants.

### Pass 4 — remaining integration/config/mixins + inventory closure

Focus:

- `ClientConfig`
- `ClientConfigAccess`
- `ClothConfigScreen`
- `HQSpeakerClientHandlerMixin`
- `HQSpeakerStopPacketMixin`
- `SoundEngineLifecycleMixin`
- `SoundPhysicsOcclusionMemoMixin`
- remaining resources/config
- identify every artifact class/resource and explain whether it comes from authored source, compiler-generated nested/synthetic output, or dependency metadata.

Goal: no unexplained baseline classes/resources.

### Pass 5 — baseline audit only

No optimization work.

Audit:

- class and method inventory
- method descriptors
- constants
- mixin targets and descriptors
- config defaults
- scheduler thresholds
- cache sizes/probe counts/age constants
- sync grace behavior
- OpenAL call ordering
- private EFX lifecycle and mandatory reattachment
- distance/position behavior
- source lifetime/generation semantics
- room-cache ownership and safe-clone behavior
- stop/reload teardown

Fix only discrepancies justified by baseline evidence.

### Pass 6 — build system only

Complete/fix:

- Gradle wrapper/project files
- ModDevGradle/NeoForge configuration
- Java 21 toolchain
- repositories/dependencies
- compile/runtime dependency placement
- local/CI strategy for CC:HQ Speakers dependency
- resources/mixin processing
- GitHub Actions build if useful

Do not use this pass to redesign runtime code.

### Pass 7 — compile and inspect

- Build the project.
- Inspect compilation errors/warnings.
- Fix reconstruction/build mistakes only.
- Inspect produced JAR class/resource inventory.
- Check key descriptors/constants/mixin resources against baseline.
- Run available focused harnesses for decode/cache/sync if present or reconstructable.
- Report clearly whether the source is merely compiling or is ready to be considered the authoritative development base.

Do not begin Beta11.1/B optimization in this pass.

## 10. Definition of reconstruction complete

The branch can be considered a practical complete source baseline only when all of the following are true:

- Source tree accounts for all meaningful Hotfix3 compat classes/resources.
- `./gradlew build` succeeds in the intended environment/CI setup.
- Produced mod loads in the lightweight test environment.
- Whole-file HQ playback works.
- Synchronized full and partial groups work.
- Direct muffling/occlusion sounds correct.
- Private EFX lifecycle and mandatory reattachment are preserved.
- Stop/start/reload lifecycle works without leaks or dead sources.
- Direct and room cache behavior is consistent with Hotfix3 expectations.
- No VerifyError or mixin target failure occurs.
- No unexplained class/method/config discrepancies remain in the baseline audit.

Only after that should a Beta11.1/B branch be created.

## 11. Future roadmap after reconstruction — do not implement yet

### Beta11.1 / B — exact cleanup

Planned later, after baseline reconstruction is authoritative:

- remove redundant decode/probe work safely;
- reduce whole-track PCM copies where practical;
- replace the small fixed-entry decoded cache with byte-budgeted LRU;
- add a short-lived byte-budgeted warm OpenAL buffer cache;
- reduce repeated sound-thread allocation churn where it matters;
- make diagnostics-off paths genuinely cheap;
- add focused hash/decode/downmix/upload timing instrumentation;
- preserve Hotfix3 acoustics and sync semantics.

### Beta12 / C1 — Persistent Progressive Room

Later:

- temporal persistent room/bounce ray state across clone replacement;
- budgeted subset refresh with current clone;
- current listener/shared-airspace work stays fresh;
- changed-ray detection can trigger urgent/full room refresh.

This deliberately replaces the separate cross-clone exact-reuse project. Do not implement A1/A2/A3.

### Beta12.x / C2 — acoustic work scheduler

Later:

- schedule room branch/ray work instead of whole-room jobs;
- bounded work, fairness, max-age ceilings.

### Beta13 / D — sparse adaptive room map

Later:

- room-only spatial memory;
- direct path remains current/exact;
- sparse adaptive listener regions across audible range.

### Shelved

- A1/A2/A3 separate cross-clone reuse system
- E adaptive ray/bounce quality reduction

### Backlog

- F optional HQ enhanced/music spatial mode

## 12. Performance philosophy

The user’s target is ATM10-scale performance: **minimal incremental CPU cost from active HQ speakers in an already-heavy modpack without sacrificing approved acoustics**.

However, reconstruction is not the time to pursue performance changes. Preserve the tested baseline first; optimize only on a new branch after reconstruction is verified.

## 13. Reporting format for each bounded run

At the end of each run, report concisely:

- branch/commit(s) created;
- classes/resources reconstructed or verified;
- baseline evidence used;
- uncertainties/TODOs instead of guesses;
- whether `RECONSTRUCTION_STATUS.md` was updated;
- exact next prerequisite.

If a prerequisite from an earlier scheduled run is incomplete, continue that prerequisite rather than skipping ahead. Do not mark a later phase complete when its earlier dependency is not actually verified.
