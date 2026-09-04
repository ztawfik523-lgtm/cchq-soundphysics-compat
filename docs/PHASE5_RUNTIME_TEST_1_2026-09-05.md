# Phase 5 runtime test 1 — 2026-09-05

Status: **RUNTIME PASS / PHASE 5 STILL OPEN**

This record captures the first user-run Minecraft test of the verified Phase 5 candidate. It is runtime evidence, not a Phase 5 closure record.

## Tested candidate

- branch frozen for the tested source: `phase5-test-candidate-1`
- tested source commit: `44612192d875e43ecef66ca51798cab7adb17020`
- artifact version: `0.1.0-beta11-phase5-test`
- JAR SHA-256: `6d782812f7915de1870a8b5ae0f619556e7ec1d24ef2eaffa7b5b225aa00bd93`
- Minecraft 1.21.1
- NeoForge 21.1.248
- CC:Tweaked 1.120.2
- CC:HQ Speakers 1.1.4-neoforge-1.21.1
- Sound Physics Remastered 1.21.1-1.5.1
- Cloth Config 15.0.140

## Returned logs

User supplied:

- `latest(20260904-231443).log`
  - SHA-256: `8be446400feace0c3883e00dd24440921b72506019d82e9c6dd44048740b6076`
- `debug(4).log`
  - SHA-256: `ca055b96316163916fdaf77313dc63f1104aef8f8488eea08a2ad3b1ba68f661`

The client exited normally after the single-player server saved and stopped. No crash or compat exception is present in the supplied run.

## Startup / Mixin result

PASS.

Runtime discovery identifies the actual test JAR and mod metadata as `0.1.0-beta11-phase5-test`.

The advanced config is registered and the compat initializer reports Phase 5 controls available.

The debug log shows `cchq_soundphysics_compat.mixins.json` preparing 11 mixins and shows the important runtime applications, including:

- `HQSpeakerClientHandlerMixin`
- `HQSpeakerStopPacketMixin`
- `SoundPhysicsEnvironmentMixin`
- `SoundPhysicsPositionMixin`
- `SoundPhysicsOcclusionMemoMixin`
- `SoundPhysicsRoomRayMemoMixin`
- Minecraft tick/scheduler injections

No `MixinApplyError`, `InvalidInjectionException`, injection failure, compat ERROR, or compat crash was found.

The runtime warnings observed are unrelated existing/environment warnings: missing refmaps for other mods, an optional JetBrains annotation class, HQ speaker model resources, vanilla sound events/shader warning, offline user type, and NeoForge's expected saved-world mod-version-change warning from Hotfix3 to the Phase 5 test version.

## OpenAL / EFX result

PASS for the scenarios exercised.

Sound Physics reports:

- OpenAL initialized successfully
- EFX extension recognized
- maximum auxiliary sends = 4

The compat created private isolated EFX successfully for all observed active HQ sources:

- 12-source stress phase: 12 isolated EFX source states created
- later 2-source phase: 2 isolated EFX source states created
- total observed isolated EFX creations: 14
- private EFX failures: 0
- native SPR fallbacks caused by compat EFX failure: 0

Most importantly, every reported performance window preserved the Hotfix3 invariant:

`efxApplies == efxReattachPasses`

There was no window in which an actual environment application omitted its required EFX reattachment.

## Source/load scenarios exercised

The first run contains two clear source-count phases.

### 12 active speakers

Telemetry reports:

- `active=12`
- `eligible=12`
- `maxActive=12`

User scenario markers include:

- standing still
- moving
- open into thick wall
- along doorway
- into open
- through doorway

The stress run remained alive and continued producing progressive direct, room, sentinel, EFX and cache telemetry throughout movement and obstruction changes.

A short early stress spike was observed:

- maximum reported SPR call: 18.548 ms
- maximum scheduler queue sample: 94.902 ms
- scheduler coalescing occurred in only a few windows (maximum 4 in one 10 s report)

This spike did not become a persistent stall. Later 12-source moving windows generally showed sub-1 ms average SPR calls and low-single-digit-ms queue averages/maxima outside isolated spikes.

The Beta9 adaptive controller reacted during the stationary/high-source period: load factor rose into approximately `1.13–1.25`, stable/adaptive backoffs were exercised, and controller-down adjustments were recorded. Under movement/transition pressure it returned to `1.00`.

### 2 active speakers

After the 12-source phase ended, telemetry reached `active=0`, then a new test began with:

- `active=2`
- `eligible=2`
- `maxActive=2`

User markers include:

- two speakers only
- standing
- moving
- into thick wall
- along doorway
- through doorway
- standing in doorway

The two-source case shows much lower steady acoustic CPU cost. Representative windows are roughly:

- acoustic load around 2 ms/s while stable
- SPR load around 1.5–1.6 ms/s while stable
- movement/transition windows rising modestly, generally around 3–7 ms/s acoustic load

No scheduler coalescing is reported in these later representative windows.

## Progressive direct / transition behavior

PASS at the instrumentation level.

The logs show live progressive-occlusion values changing per source, including fully open paths, partial obstruction, and a strongly blocked source. Position redirection/reflection telemetry is also present.

During doorway/wall movement the clearing sentinel is active and produces candidates/confirmed transitions. Representative reported transition latency after the two-speaker movement tests includes averages around 17–38 ms and maxima commonly around 50–101 ms. A heavier 12-speaker doorway window reached a larger maximum transition value (350.672 ms), but the run continued normally and later low-source tests returned to substantially lower values.

No exception or invalid acoustic state accompanies these transitions.

## Direct-cache behavior

PASS functionally; useful tuning evidence collected.

The Beta10 direct/SPR exact-ray cache is active. Hit rate depends strongly on listener/source stability:

- stable periods show meaningful exact reuse
- moving/rapidly changing 12-source periods drop to low single-digit hit percentages
- direct-to-SPR sharing remains active

Bit-identical OpenAL suppression is also clearly exercised: many filter/source writes are skipped in stable windows, while real writes resume during movement/transition windows.

## Beta11 room-ray memo behavior

PASS for same-clone correctness; strong future-optimization signal observed.

The same-clone room-ray cache works, with hit rate varying heavily by movement/scene state. Observed windows range from effectively 0% during rapidly changing scopes to roughly 77–81% in favorable later two-source doorway windows.

`crossCloneWouldReuse` is frequently very high and often close to the miss count. This is telemetry only in the current build, as required by the Hotfix3 invariant; no unsafe cross-clone reuse is enabled. The result is useful evidence for the later persistent-room roadmap.

## Cleanup / stop behavior

PASS at the observable level.

The 12-source phase transitions to `active=0` without a compat exception. A subsequent two-source playback phase starts normally and creates fresh isolated EFX state. The game then performs a normal save/disconnect/server shutdown and reaches Minecraft `Stopping!` without a crash.

This is good evidence for ordinary stop/restart/source cleanup, although the targeted source-lifecycle debug category was not enabled, so this test does not provide a complete event-by-event lifetime trace.

## What this test does NOT prove

Phase 5 remains open because several requested validation surfaces were not explicitly exercised/logged:

1. synchronized-start correctness/timing is not proven;
2. `Sync grouping log` was not enabled, so there is no direct `alSourcePlayv`/partial-group runtime evidence in these logs;
3. `/cchqphysics dump` was not run, so the new detailed snapshot path is not yet runtime-validated;
4. `/cchqphysics status`, `refresh_rooms`, `reset_caches`, `reset_efx`, and `config` were not exercised;
5. the targeted Phase 5 source-lifecycle/EFX/cache/sentinel logging categories were mostly not enabled, so the first test relies primarily on existing diagnostics/performance telemetry;
6. subjective audio quality cannot be inferred from logs alone; user confirmation is still required for direction, muffling, room sound and sync perception.

## Current conclusion

**No code correction is justified by runtime test 1.**

The first real-game run validates startup, Mixin application, active CC:HQ source processing, progressive occlusion, reflected positioning, private per-source EFX, mandatory EFX reattachment, scheduler operation, exact caches, Beta11 same-clone room memoization, 12-source stress behavior, 2-source movement/doorway behavior, stop/restart, and clean shutdown without a compat exception.

Phase 5 should remain **IN PROGRESS** until synchronized starts and the Phase 5 diagnostic-command paths are explicitly tested and the user's subjective audio result is recorded.
