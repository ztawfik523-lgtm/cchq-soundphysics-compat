# Phase 1 Hotfix3 bytecode audit

This document records exact Hotfix3 bytecode findings from an early bounded reconstruction pass. The findings remain authoritative evidence for the classes described here, but the old ad-hoc pass numbering at the end of the original document is obsolete. Current work follows the five canonical phases in `docs/RECONSTRUCTION_PHASES.md`.

Authoritative artifact:

`cchq_soundphysics_compat-0.1.0-beta11-hotfix3.jar`

SHA-256:

`83500f182fc9829aa1a5a51fbfa11ba6cdfb645699b25d1c445167666dabc1ef`

The artifact was verified against this hash before inspection with `javap -p -c -s` / `javap -p -v`. The exact JAR was independently reverified again on 2026-09-04; see `docs/baseline/PHASE1_JAR_RECHECK_2026-09-04.md`.

## `SyncStartCoordinator` exact Hotfix3 shape

Fields:

```text
private static final Map<UUID, Group> GROUPS
private static final long PARTIAL_FLUSH_NS = 100_000_000L
private static final long STALE_GROUP_NS = 5_000_000_000L
```

Methods/descriptors:

```text
static synchronized void play(int, HQPayloadView.Audio)
static synchronized int sourceState(int, int)
private static boolean isPending(int)
private static void flushExpired(long)
private static void startAndRemove(UUID, Group)
private static void playVector(List<Integer>)
static synchronized void clear()
static synchronized void removeSource(int)
```

Nested authored class:

```text
Group
  int expected
  final List<Integer> sources = new ArrayList<>()
  final long createdNs = System.nanoTime()
```

Behavior:

- `play` first flushes expired partial groups, then removes groups older than 5 seconds.
- Null/non-grouped or declared size <= 1 starts with scalar `AL10.alSourcePlay(sourceId)`.
- Grouped sources are deduplicated in the group's list; expected size may grow but is never shrunk.
- Complete groups launch through one `AL10.alSourcePlayv(int[])`.
- Incomplete groups launch through one `playv` once `now - createdNs >= 100_000_000L`.
- `sourceState` performs the partial-group flush, reads actual OpenAL state, and maps pending `AL_INITIAL` to `AL_PAUSED` only for `AL_SOURCE_STATE`.
- `removeSource` scans pending groups and drops empty groups.
- `clear` clears pending groups.

The older reconstructed `GroupState`/reverse-pending-map API was not an exact Hotfix3 reconstruction and was replaced.

## `CompatAudioManager` exact findings

The Hotfix3 manager contains these behavior-bearing details:

- `MAX_DECODE_CACHE_ENTRIES = 4`.
- listener coordinates `lastListenerX/Y/Z` initialize to `Double.NaN` and reset to NaN on session invalidation.
- interception is gated by `ClientConfig.enabled()`.
- `_STREAM` and `PCM_S16LE` payload formats fall back instead of taking the whole-file bridge.
- decode identity is `format + ':' + shortHash(data)`.
- completed decode entries are age-cleaned and capped to four entries.
- immediately after `AL10.alGenSources()`, Hotfix3 calls `EnvironmentSmoother.register(sourceId)`.
- OpenAL reference/max distance come from `AttenuationBridge.referenceDistance(audio)` / `maxDistance(audio)`.
- initial SPR processing occurs before synchronized/native start.
- start is delegated to `SyncStartCoordinator.play(sourceId, audio)`, followed by `checkAl("start source")`; active-source installation follows.
- maintenance/finished-source cleanup query state through `SyncStartCoordinator.sourceState(sourceId, AL_SOURCE_STATE)`.
- maintenance computes listener distance, calls `Beta9Optimizer.updateDistance`, gates beyond `AttenuationBridge.maxDistanceSquared`, writes gain through `Beta10Optimizer.alSourcefStable`, and calls `Beta10Optimizer.updateAudibility`.
- audible sources retain direct `AL10.alSource3f(... AL_POSITION ...)` before `SoundPhysicsBridge.apply(...)`.
- normal active-source destruction begins with `EnvironmentSmoother.unregister(sourceId)` before OpenAL stop/detach/delete.
- `stopAllSources()` clears active/native buffers and `Beta11RoomRayCache`; it does not directly perform the session-wide bridge reset.
- `invalidateSession()` clears ready/decode/generation state, calls `SyncStartCoordinator.clear()` and `SoundPhysicsBridge.clearSourceIds()`, then resets listener coordinates to NaN.
- pause/resume use `SyncStartCoordinator.sourceState`, preserving pending-INITIAL protection.
- package-private `beta10OnSoundThread(Runnable)` remains a narrow scheduler seam.

## Important baseline oddity retained

The Hotfix3 `startSource` failure cleanup after `EnvironmentSmoother.register(sourceId)` does not visibly call `EnvironmentSmoother.unregister` in the bytecode's `finally` cleanup path. Reconstruction must not silently redesign this during baseline recovery; any cleanup improvement belongs to later, separately justified development.

## Current relevance

The exact findings above have already been incorporated into reconstructed `SyncStartCoordinator` and `CompatAudioManager` source on `beta11-source-reconstruction`.

Current canonical state is:

- Phase 1 — complete / JAR-rechecked;
- Phase 2 — complete / JAR-rechecked;
- Phase 3 — in progress;
- remaining top-level authored gaps: `SoundPhysicsBridge` and `ClothConfigScreen`.

Do not interpret the historical statement that `SoundPhysicsBridge`/`Beta10Optimizer` were the "next bounded phase" as current status. `Beta10Optimizer` and the surrounding helper stack have since been reconstructed; `SoundPhysicsBridge` is now the principal remaining runtime class.
