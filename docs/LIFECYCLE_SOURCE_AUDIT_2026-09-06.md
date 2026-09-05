# CC:HQ Sound Physics Compat — lifecycle source audit continuation

**Date:** 2026-09-06 (Africa/Cairo)

This document records the source-only finishing audit performed after `docs/NEXT_CHAT_HANDOFF_2026-09-06.md` was written. It supersedes that handoff only for the lifecycle candidate/source checkpoint and runtime-candidate hash below. The older handoff remains the detailed project history and V7.1/performance reference.

Repository: `https://github.com/ztawfik523-lgtm/cchq-soundphysics-compat`

Working branch: `phase5-v7-1-lifecycle-state-finish`

## Stable references remain unchanged

Frozen V7.1 acoustic reference:

`ffcf5f6e05d85b69f1f1dff8cfae1b082b71604d`

Performance baseline:

`962eab8b052466ca984496a7dec0767dc65803f4`

Previous lifecycle source checkpoint:

`be03d30efe98ca03bdf27764bcea567df5ef3875`

The source audit deliberately did not change the frozen acoustic/config implementation. CI compared the named diffraction, HF50/spectral, progressive occlusion, environment smoothing, sync coordinator, position, Beta9/Beta10/Beta11 cache and primary config files against the performance baseline and passed. Lua remained unchanged.

## Source owners traced during the audit

The audit traced the lifecycle cleanup fan-out rather than treating `CompatAudioManager` in isolation.

`EnvironmentSmoother.unregister(sourceId)` is the central per-source teardown path and reaches the associated progressive/Beta optimizer state, position state, SoundPhysicsBridge registration, synchronized-source membership and private EFX cleanup. SoundPhysicsBridge cleanup in turn removes its source state and related capture/diffraction state. This supports the intended model that one unregister closes the compat-owned state for one OpenAL source lifetime.

The existing session invalidation ordering was reviewed. Global bridge/sync clears before per-source unregister are currently safe because the relevant clear/unregister operations are idempotent or become no-ops for already-cleared structures, while the remaining per-source optimizer state is still removed by `EnvironmentSmoother.unregister`.

## Concrete issues found and fixed

### 1. OpenAL source allocation could register source ID 0

Before this audit, source creation performed `alGenSources()` and immediately registered the returned ID with compat state. If allocation returned OpenAL's null object ID `0`, or an allocation error was pending, later failure cleanup skipped unregister because the failure path intentionally only unregisters non-zero source IDs.

The audited source now:

- checks the OpenAL error immediately after `alGenSources()`;
- rejects source ID `0` before any compat registration;
- only then calls `EnvironmentSmoother.register(sourceId)`.

This prevents Java-side compat state from ever being created for OpenAL source 0.

### 2. Active-source teardown could stop early after one cleanup failure

Previously, `EnvironmentSmoother.unregister(active.sourceId)` ran before the raw OpenAL teardown block. If unregister unexpectedly threw, source stop/detach/delete and buffer release could be skipped. Similarly, one failing OpenAL cleanup call could prevent later cleanup operations.

The audited teardown now treats each operation independently as best-effort:

- unregister compat state;
- stop the OpenAL source;
- detach its buffer;
- delete the OpenAL source;
- release the compat buffer reference.

A failure in one step is logged and no longer prevents the remaining cleanup steps.

Source commit for these two hardening changes:

`fb2665620ff977c4e251da416679f0ef5789d724`

### 3. Pause/resume could break a synchronized group that was still assembling

This was the most important source-audit finding.

`SyncStartCoordinator.sourceState()` intentionally maps an actual OpenAL `AL_INITIAL` source to synthetic `AL_PAUSED` while that source is pending inside an incomplete synchronized-start group. That protects the pending source from ordinary cleanup before the group starts.

The previous `resumeCompatSources()` used that synthetic state. Therefore, if Minecraft pause/resume happened while a synchronized group was still assembling, a pending `AL_INITIAL` source could be mistaken for a genuinely paused source and started individually. That bypassed the synchronized group start.

The audited pause/resume path now:

- queries the actual OpenAL source state directly;
- pauses only sources that are actually `AL_PLAYING`;
- resumes only sources that are actually `AL_PAUSED`;
- uses `alSourcePausev` / `alSourcePlayv` for the eligible compat sources, avoiding unnecessary per-source resume skew;
- leaves pending `AL_INITIAL` synchronized sources untouched so `SyncStartCoordinator` remains authoritative for their start.

Source commit:

`2a6a2f4ecd9e2faad51de9818797f5a16c14b0f7`

Treat this as the new clean audited production-source checkpoint. Documentation/CI-helper cleanup commits may advance the branch beyond it without changing production Java.

## Deliberately unchanged observations

The manager's two-tick maintenance block contains a `periodic` expression that is always true inside the enclosing even-tick condition, making the alternate cleanup-only branch unreachable there. The exact same timing logic exists in frozen V7.1. It was therefore left unchanged: simplifying it would alter established refresh timing and cross from lifecycle cleanup into stable acoustic/performance behavior without runtime evidence justifying that change.

Other speculative cases considered but not changed include a very narrow packet/world-identity transition window, stop-packet session identity, and telemetry-only reset races. None had enough concrete evidence to justify additional finishing risk.

## Validation of the audited source

A temporary CI validator was added only for this audit and removed after success.

Successful run:

`33996243988`

Job:

`101387190106`

Validated:

- Java 21 / Gradle wrapper;
- performance-base ancestry;
- frozen V7.1 refs;
- existing lifecycle invariants;
- source-allocation zero/error guard;
- best-effort active teardown;
- only `CompatAudioManager.java` and `SoundEngineLifecycleMixin.java` differ from the performance baseline among production Java source;
- frozen acoustic/config files unchanged;
- deliberate bitwise eligibility expression unchanged;
- no Lua changes;
- reconstruction compile classpath;
- resource wiring;
- clean build;
- Java 21 compile;
- JAR build;
- 81-class JAR inspection;
- artifact upload.

Audit artifact ID:

`9978159512`

Artifact ZIP digest:

`sha256:113e7ffdace7a7bde87cc13455dc7e3f74095434d03d3103cc380e431a3943d4`

Audited lifecycle candidate JAR SHA-256:

`4b3c8c52cc00a37274d5829cff93933d6e548b733b83918fcc5570ab8d6ad3c5`

Class count:

`81`

Built filename remains:

`cchq_soundphysics_compat-0.1.0-beta11-phase5-v7-1-performance-test.jar`

The old lifecycle candidate SHA `6d0fa98ee6c76d23a3e0764501d16dc5c993149e0de77181cdab6fc0a9abdc18` is now superseded. Any future user lifecycle runtime test should use the audited `4b3c8c52...` candidate instead.

The temporary workflow used to validate this audit was removed after the successful run so it does not remain in the production tree.

## Runtime status

User runtime lifecycle validation is still **pending**. The user explicitly chose to postpone the test for now. Source/CI validation is clean, but this candidate should not be called runtime-approved until the user runs the lifecycle sequence and provides the logs/listening result.

When the user is ready, use the same lifecycle sequence already documented in the main handoff, but use candidate SHA:

`4b3c8c52cc00a37274d5829cff93933d6e548b733b83918fcc5570ab8d6ad3c5`

After a clean runtime result, proceed directly into release naming/presentation cleanup, final packaging/source audit and final stable/archive checkpoint.

## Release cleanup debt intentionally left for later

Do not normalize release naming before the runtime gate unless the user explicitly decides otherwise. Current development-facing naming is inconsistent and still contains test terminology in several places, including:

- Gradle artifact version: `0.1.0-beta11-phase5-v7-1-performance-test`;
- Java `VERSION`: `0.1.0-beta11-phase5-diffraction-v7-1-spreading-only-test`;
- raw `neoforge.mods.toml` version/description still describing the V7.1 diffraction experiment;
- diffraction config filename/startup wording still containing V7.1 test terminology.

This is release-presentation debt, not evidence of acoustic or lifecycle source mismatch. Clean it up as a dedicated release step after runtime lifecycle acceptance, then rebuild and perform the final artifact/hash audit.
