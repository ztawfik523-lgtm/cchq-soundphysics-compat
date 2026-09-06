# CC:HQ Sound Physics Compat — transport / jukebox architecture investigation

**Date:** 2026-09-06 (Africa/Cairo)

This document records the project direction after the user's Gate-1 lifecycle test and review of `jukebox_v8.lua`.

## User direction — important override

Do **not** treat V7.1 as a permanent design ceiling.

- V7.1 remains a frozen known-good acoustic reference/checkpoint.
- It is useful for comparison and regression diagnosis.
- It does not prohibit new features, acoustic changes, lifecycle redesigns, rewrites, or replacement architectures.
- A future design should be evaluated on evidence and user listening, not rejected merely because it differs from V7.1.
- The user is explicitly open to rewriting the jukebox script from scratch if that gives a better system.

The old handoff wording that emphasized finishing/conservatism described the previous phase, not a permanent project rule.

## Gate 1 result

Candidate tested:

`4b3c8c52cc00a37274d5829cff93933d6e548b733b83918fcc5570ab8d6ad3c5`

Runtime/log review found the lifecycle goals clean:

- play/stop/restart clean;
- game pause/resume clean;
- resource/sound reload teardown/restart clean;
- disconnect/rejoin teardown/restart clean;
- synchronized four-speaker playback clean in the run;
- balanced compat-source/private-EFX create/destroy lifetimes;
- no relevant compat OpenAL/EFX cleanup errors;
- clean final shutdown.

**Gate 1 lifecycle verdict: PASS.**

The dimension test exposed a transport limitation: the old client source is correctly destroyed on dimension replacement, but the Lua program can remain logically active without a new HQ whole-file payload being sent when the listener returns. This is not stale-source leakage; it is missing session/resync semantics.

## What Jukebox v8 is really doing

`jukebox_v8.lua` is not merely a UI. It implements missing transport behavior itself:

- duration parsing for MP3/MP2/WAV/Ogg/AIFF/AU/MP4/M4A/AAC;
- natural-end/auto-next timing;
- repeat logic;
- pause as stop + replay from 0:00;
- volume change as debounced stop + replay from 0:00;
- hard-coded 12-tick synchronized-start lead;
- persistent logical playback state independent of CC:HQ `speakIsPlaying()`;
- queue/settings persistence and recovery.

Those are symptoms of an API gap. A stronger architecture can move transport semantics into Java/OpenAL and leave Lua responsible for application/UI/queue policy.

## Concrete Jukebox v8 issues/opportunities

Even before a transport rewrite, the script has a few real design issues:

1. `useAllSpeakers` is inferred from `speakerCount > 1` plus existence of `speakStopAll`, not from whether the selected format's actual `*All` playback method exists. `speakBytes()` may fall back to one speaker while the UI still reports synchronized-all topology.
2. `MULTI_SPEAKER_SYNC_DELAY = 12/20` duplicates an internal HQ constant. It will silently go stale if HQ changes the lead.
3. Auto-next/repeat correctness depends on Lua media parsers. Unsupported/unusual containers can leave the jukebox stuck after the client has actually finished.
4. Repeat-track resends/restarts the file instead of using OpenAL looping.
5. Pause and live volume are fake because the whole-file API has no transport controls.
6. URL repeat/replay re-enters the fetch path on later queue cycles; a managed blob/session cache could avoid repeated transfer/decoding.
7. The script owns one clock while the client OpenAL source owns another. The two can drift or disagree after reloads/lifecycle transitions.

The good parts worth retaining conceptually are the bounded download handling, persistent-state backup rotation, queue/shuffle policy, topology reacquisition, and lightweight redraw strategy.

## Important CC:HQ source facts

Review of HQ source establishes:

- `speakIsPlaying()` reports server queue/stream state, not the lifetime of a dispatched whole-file client source.
- `speakVolume()` changes stored default volume; it does not mutate an already-playing whole-file source.
- `setLooping()` stores a boolean; it does not implement whole-file replay.
- whole-file payload size is capped at 8 MiB.
- synchronized `speak*All` uses a 12-tick lead.
- group enqueue returns are OR-reduced, so `true` means at least one member accepted, not necessarily transactional all-member acceptance.
- start packets are sent only to players in the same `ServerLevel` and within a fixed 32-block radius at dispatch.
- stop packets use the same nearby/same-level delivery model.
- the HQ peripheral provider cache is keyed by dimension + block position.
- each peripheral instance also has a random `speakerSource` UUID used in packets.

The fixed 32-block packet radius is a broader issue than the observed dimension case: a listener can miss a start or stop while outside HQ's packet radius, even though this compat can make sound audible/processable beyond 32 blocks.

## CC:Tweaked API finding

CC:Tweaked's `GenericSource` API is **not** a clean way to add methods onto this existing HQ normal peripheral. CC:Tweaked documents generic peripherals as incompatible with explicitly provided normal peripherals.

A native-looking extension of the existing `speaker` peripheral therefore requires one of:

- a Mixin which merges public `@LuaFunction` methods into HQ's runtime peripheral class;
- a deliberate wrapper/replacement peripheral provider;
- or a separate/global API rather than methods on `speaker`.

CC:Tweaked discovers `@LuaFunction` methods reflectively from the runtime class (`Class#getMethods`), so Mixin-added public annotated methods are a plausible and relatively direct route, but must be validated against the exact pinned runtime JAR.

## Architecture options — intentionally not chosen here

These have meaningful tradeoffs, so the user should choose the scope.

### Option A — rewrite the Lua jukebox only

Build Jukebox v9 from scratch around a cleaner internal adapter, but keep the existing HQ transport.

Pros:

- smallest Java/mod risk;
- no server-side compat requirement;
- works with existing HQ unchanged.

Cannot truly solve:

- real pause/resume at current sample position;
- seek;
- live volume without replay;
- dimension/rejoin/range resync;
- missed stop packets;
- authoritative whole-file state;
- exact client-decoded duration;
- transactional group start.

### Option B — managed transport layered on CC:HQ

Keep HQ's speaker block/peripheral and much of its established payload path, but add a server/common session registry plus client transport mirror in this compat.

Potential capabilities:

- true OpenAL pause/resume;
- seek;
- live volume;
- real looping;
- exact playback position/state;
- exact duration from decoded PCM;
- natural end events;
- sound-engine reload rehydration;
- dimension/rejoin/range resync;
- stale-stop prevention;
- synchronized group state;
- all-or-none managed group start;
- capability discovery so Lua does not hard-code internals.

A strong compatibility variant can also observe legacy HQ `enqueue(...)`/`speakStop()` calls, so old scripts gain session/resync improvements automatically while a rewritten Jukebox uses the richer explicit API.

### Option C — full whole-file transport rewrite

Use HQ mainly as the physical speaker/peripheral integration and own the complete whole-file transport protocol in this mod.

Potential additional benefits:

- content-addressed blob transfer/cache;
- send identical compressed audio once per client instead of once per synchronized speaker;
- persistent compressed cache across dimension/sound-engine recreation;
- bounded decoded-PCM LRU cache;
- preload next track;
- near-gapless transitions;
- fades/crossfades;
- richer per-listener range/dimension negotiation;
- freedom from HQ's fixed whole-file packet semantics.

This has the highest capability ceiling and largest implementation/test surface.

## A promising hybrid design (B+)

There is a particularly attractive middle design worth considering separately from a total rewrite:

1. Mixin into HQ's private whole-file `enqueue(...)` path to observe successful legacy starts and maintain a server logical-session registry.
2. Mixin/augment stop so the registry is invalidated authoritatively.
3. Add explicit transport methods for new scripts (`pause`, `resume`, `seek`, `setVolume`, `state`, etc.).
4. Keep using HQ's existing compressed-byte format support initially.
5. Add a small optional compat protocol for session snapshots/resync/control.
6. Make the client understand a logical timeline/offset so a resent session resumes at the correct point instead of from 0:00.
7. Later evolve blob transport/deduplication without forcing another Lua API redesign.

This would give legacy scripts automatic correctness improvements and let Jukebox v9 use modern features, without requiring the entire payload layer to be replaced on day one.

## Internal session model if Option B/B+/C is chosen

Use a logical session distinct from the ephemeral OpenAL source.

Suggested identity/state:

- session UUID;
- stable speaker key `(dimension ResourceKey, BlockPos)` or synchronized member set;
- current HQ source UUID mapping;
- content hash / blob id;
- format;
- volume;
- start game tick / timeline origin;
- base playback offset;
- paused state;
- duration when known;
- loop state;
- revision/generation;
- source coordinates and optional dynamic-position revision.

Transport position can be derived server-side from game ticks:

- paused: `baseOffset`;
- playing: `baseOffset + (currentGameTick - startTick) / 20.0`;
- looping: modulo duration.

Seek changes base offset + revision. Pause freezes the offset. Resume creates a new timeline origin.

## OpenAL-side features we can exploit

Because compat already owns a static OpenAL source/buffer for whole-file playback:

- true pause is `alSourcePause` and preserves the sample position;
- resume is `alSourcePlay`/vector play;
- seek can use OpenAL sample/second offset on the static source;
- live volume can update gain without restarting the buffer;
- looping can use OpenAL looping for a track session;
- synchronized sources sharing a buffer can seek to the same offset and start via vector `alSourcePlayv`.

The current `ActiveSource` model stores immutable packet audio, so live transport controls would require a deliberate refactor to mutable logical-session state. That is acceptable under the user's new direction.

## Important lifecycle redesign for true pause

Current game pause/resume logic resumes all actual compat sources in `AL_PAUSED` state. That is correct for today's model because there is no separate user-pause concept.

If the transport API gains true user pause, that logic must change so Minecraft resume does not accidentally resume a user-paused track.

Track separate pause ownership, e.g.:

- `transportPaused`;
- `pausedByGame` (or a set of sources paused by the game-pause hook).

Game resume must only resume sources which the game pause hook itself paused and whose transport remains unpaused.

## Network/resync model

A server-authoritative registry can fix the 32-block/dimension problem.

Possible scalable approach:

- server stores active logical sessions;
- client announces/requests session reconciliation on join, dimension change, sound-engine rebuild, and meaningful movement/range-boundary changes;
- server replies only with sessions relevant to that dimension/range;
- content is keyed by full hash;
- client requests/downloads a blob only if it does not already have it;
- resync descriptors carry current logical offset/state so OpenAL reconstruction is correct;
- stop/replacement increments revision so stale packets cannot resurrect an old track.

For a private mod this can be implemented with conservative bounded caches rather than an internet-scale cache system.

## Lua API shape — names not locked

If methods are merged into the normal speaker peripheral, a compact explicit API could be:

- `cchqCapabilities()`
- `cchqPlay(bytes, options)`
- `cchqPlayAll(bytes, options)`
- `cchqStop()` / `cchqStopAll()`
- `cchqPause()` / `cchqPauseAll()`
- `cchqResume()` / `cchqResumeAll()`
- `cchqSeek(seconds)` / `cchqSeekAll(seconds)`
- `cchqSetVolume(volume)` / `cchqSetVolumeAll(volume)`
- `cchqState()` / `cchqStateAll()`

`options` can carry format, volume, loop, optional duration hint, and future flags.

Capabilities should be versioned and feature-detected rather than exposing internal constants. Potential flags: `truePause`, `seek`, `liveVolume`, `managedLoop`, `exactDuration`, `resync`, `transactionalSync`, `events`, `preload`.

A rewritten Jukebox can have two transport adapters:

- managed adapter when this API exists;
- legacy HQ adapter as fallback.

The managed path should remove duplicated Lua duration/clock/replay hacks instead of keeping them as a second authority.

## Script rewrite direction if managed transport is selected

Jukebox v9 should be rebuilt around clear modules, not patched line-by-line from v8:

1. `Library/Queue` — queue entries, shuffle/repeat policy, persistence.
2. `MediaLoader` — local/URL byte acquisition and size enforcement.
3. `Transport` — capability-based managed/legacy adapters.
4. `PlayerState` — driven by transport state/events rather than inferred timers on managed path.
5. `UI` — stateless-ish rendering from player/queue state.
6. `Controller` — event loop and commands.

On the managed path:

- no hard-coded 12-tick delay;
- no Lua media-duration parser as authoritative clock;
- pause is true pause;
- volume does not restart;
- repeat-track can be native;
- auto-next reacts to managed end events/state;
- position display uses transport state;
- dimension/reload recovery is transparent to the script.

## Common/server refactor implication

The current compat is client-focused. A server-aware transport means refactoring the mod so server-safe transport/network code loads on dedicated servers while SPR/OpenAL/config GUI code remains client-only.

That implies dedicated-server CI/runtime loading becomes mandatory for a transport branch.

This is a real feature branch, separate from the already-validated cleanup-only `0.1.0` prep checkpoint.
