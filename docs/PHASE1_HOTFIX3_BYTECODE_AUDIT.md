# Phase 1 Hotfix3 bytecode audit

This audit records the exact Phase 1 evidence obtained from the authoritative tested artifact during the bounded Phase 2 scheduled run. The run correctly fell back to Phase 1 because the previously reconstructed Phase 1 source did not match the Hotfix3 bytecode closely enough.

Authoritative artifact:

`cchq_soundphysics_compat-0.1.0-beta11-hotfix3.jar`

SHA-256:

`83500f182fc9829aa1a5a51fbfa11ba6cdfb645699b25d1c445167666dabc1ef`

The artifact was verified locally against this hash before inspection with `javap -p -c -s` / `javap -p -v`.

## SyncStartCoordinator exact Hotfix3 shape

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
- `sourceState` performs the partial-group flush, reads the actual OpenAL state, and maps pending `AL_INITIAL` to `AL_PAUSED` only for `AL_SOURCE_STATE`. This is the Hotfix3 lifecycle protection.
- `removeSource` scans pending groups and drops empty groups.
- `clear` clears pending groups.

The older reconstructed `GroupState`/reverse-pending-map API was therefore not an exact Hotfix3 reconstruction and was replaced.

## CompatAudioManager exact Phase 1 findings

The Hotfix3 manager contains these behavior-bearing details that were missing or different in the earlier reconstruction:

- `MAX_DECODE_CACHE_ENTRIES = 4`.
- listener coordinates `lastListenerX/Y/Z` initialize to `Double.NaN` and reset to NaN on session invalidation.
- interception is gated by `ClientConfig.enabled()`.
- `_STREAM` and `PCM_S16LE` payload formats fall back instead of taking the whole-file bridge.
- decode identity is `format + ':' + shortHash(data)`, not sync-group/source/start-tick identity.
- completed decode entries are age-cleaned and then capped to the four oldest/most-recently-used completed entries as represented in Hotfix3.
- immediately after `AL10.alGenSources()`, Hotfix3 calls `EnvironmentSmoother.register(sourceId)`.
- OpenAL reference/max distance come from `AttenuationBridge.referenceDistance(audio)` / `maxDistance(audio)` rather than fixed 1/1024 values.
- initial SPR processing occurs before synchronized/native start.
- start is delegated to `SyncStartCoordinator.play(sourceId, audio)`, followed by `checkAl("start source")`; the active-source entry is installed afterward.
- maintenance and finished-source cleanup query state through `SyncStartCoordinator.sourceState(sourceId, AL_SOURCE_STATE)`.
- maintenance computes listener distance, calls `Beta9Optimizer.updateDistance`, gates beyond `AttenuationBridge.maxDistanceSquared`, writes gain through `Beta10Optimizer.alSourcefStable`, and calls `Beta10Optimizer.updateAudibility`.
- audible sources retain direct `AL10.alSource3f(... AL_POSITION ...)` before `SoundPhysicsBridge.apply(...)`.
- normal active-source destruction begins with `EnvironmentSmoother.unregister(sourceId)` before OpenAL stop/detach/delete. The smoother owns downstream acoustic/sync teardown in the baseline.
- `stopAllSources()` clears active/native buffers and `Beta11RoomRayCache`; it does not directly perform the session-wide bridge reset.
- `invalidateSession()` clears ready/decode/generation state, calls `SyncStartCoordinator.clear()` and `SoundPhysicsBridge.clearSourceIds()`, then resets listener coordinates to NaN.
- pause/resume use `SyncStartCoordinator.sourceState`, preserving the pending-INITIAL protection consistently.
- package-private `beta10OnSoundThread(Runnable)` remains a narrow scheduler seam.

## Important baseline oddity retained

The Hotfix3 `startSource` failure cleanup after `EnvironmentSmoother.register(sourceId)` does not visibly call `EnvironmentSmoother.unregister` in the bytecode's `finally` cleanup path. Reconstruction should not silently redesign this during baseline recovery; any lifecycle cleanup improvement belongs to a separately justified later change, not reconstruction.

## Result of this bounded run

Because this exact bytecode evidence proved that the existing Phase 1 source was incomplete, the run did **not** proceed into Phase 2. `SyncStartCoordinator` and `CompatAudioManager` were repaired first, in accordance with the reconstruction handoff rule.

Phase 2 (`SoundPhysicsBridge`, `Beta10Optimizer`, `AcousticCapture`, `EnvironmentSmoother`, EFX/direct-cache integration) remains the next bounded phase.
