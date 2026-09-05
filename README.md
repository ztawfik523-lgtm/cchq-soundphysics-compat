# CC:HQ Sound Physics Compat

Client-side compatibility layer between **CC:HQ Speakers** and **Sound Physics Remastered** for Minecraft 1.21.1 / NeoForge.

The mod routes CC:HQ whole-file speaker playback through Sound Physics Remastered while preserving CC:HQ playback timing and source lifetime behavior. It adds positional obstruction, stable reflected direction, synchronized-copy clarity balancing, and opening-aware vertical sound for cases where a nearby real opening should affect what reaches the listener.

## Release candidate

Current branch identity: **0.1.0-beta11-rc1**

Supported/tested stack:

- Minecraft **1.21.1**
- NeoForge **21.1.248**
- Java **21**
- CC:Tweaked **1.120.2**
- CC:HQ Speakers Modrinth artifact `ygA78R8l / u5PEI5Ax`
- Sound Physics Remastered **1.21.1-1.5.1**
- Cloth Config **15.0.140+** is optional but recommended for the in-game settings screen

This is a **client-only** compatibility mod.

## What it does

### Positional wall obstruction

CC:HQ whole-file audio is treated as real positional audio instead of bypassing Sound Physics. Direct obstruction uses a 17-path model:

- 1 exact speaker-to-listener path
- 8 inner surrounding paths
- 8 outer surrounding paths

This makes increasing wall thickness progressively darker/quieter instead of behaving like a single binary blocked/unblocked ray.

### Stable reflected direction

Sound Physics can bend apparent sound direction toward reflected routes. The compat smooths those virtual positions so long-running speakers do not rapidly jump between directions while still preserving reflected positioning.

### Synchronized sound balance

When synchronized copies of the same sound reach the listener with an extreme clarity mismatch, the most heavily muffled copy can receive a bounded direct-clarity correction. This changes only the direct low-pass cutoff; it does **not** change source gain, source position, synchronized start timing, or reverb sends.

### Openings & vertical sound

For speakers above the listener, a real nearby ceiling/open-top opening can contribute a secondary acoustic path. This helps tunnels, shafts and lower floors respond to actual openings instead of relying only on straight-line speaker-to-listener obstruction.

The normal direct obstruction result remains authoritative; an opening adds a bounded contribution rather than replacing the direct path.

### Performance safeguards

The compat includes:

- adaptive 17-path refreshes
- exact direct-result reuse when inputs are unchanged
- occlusion-ray reuse
- same-world-snapshot room/bounce ray reuse
- stable room-update slowdown under safe conditions
- per-source isolated OpenAL filters
- bounded opening verification and cached opening paths

## Installation

Install the following client-side mods for Minecraft 1.21.1 / NeoForge:

1. CC:Tweaked
2. CC:HQ Speakers
3. Sound Physics Remastered
4. CC:HQ Sound Physics Compat
5. Cloth Config API, optional but recommended for the config screen

Do not replace or modify Sound Physics Remastered for this compat. The normal upstream SPR JAR is used at runtime.

## Configuration

With Cloth Config installed, open the mod's config screen from your normal mod/config UI.

Main categories include:

- **General** — enable/disable CC:HQ interception
- **Distance & Range** — audible reach
- **Occlusion & Muffling** — wall obstruction strength and 17-probe tuning
- **Direction & Reflections** — reflected-position stabilization
- **Smoothing** — how quickly acoustic changes are applied
- **Performance** — update intervals and adaptive probing
- **Openings & Vertical Sound** — opening strength, influence distance and advanced opening behavior
- **Synchronized Sound Balance** — synchronized-copy clarity correction
- **Advanced / Troubleshooting** — low-level scheduler/cache/filter controls
- **Debug & Validation** — targeted diagnostics

The release uses clean config files:

- `cchq_soundphysics_compat-client.toml`
- `cchq_soundphysics_compat-advanced.toml`
- `cchq_soundphysics_compat-sync-balance.toml`
- `cchq_soundphysics_compat-openings.toml`

See **[docs/CONFIGURATION.md](docs/CONFIGURATION.md)** for a plain-English explanation of every important option and what increasing/decreasing it does.

## Diagnostic commands

Client commands are available under `/cchqphysics`:

- `/cchqphysics status` — compact runtime status
- `/cchqphysics dump` — write a detailed compat snapshot to `latest.log`
- `/cchqphysics diffraction on|off|status` — runtime-only toggle/status for opening-aware sound; the command name is retained for diagnostic compatibility
- `/cchqphysics refresh_rooms` — queue a room/reverb refresh
- `/cchqphysics reset_caches` — safely reset acoustic caches on the sound thread
- `/cchqphysics reset_efx` — recreate compat-owned private filters
- `/cchqphysics config` — print effective advanced/opening/sync configuration

These commands are client-side and do not mutate server state.

## Behavioral guarantees

The release candidate intentionally preserves these rules:

- no Lua-side changes
- `SoundSource.BLOCKS` distance behavior
- no intentional PCM sample-position, OpenAL playback-clock or buffer-offset changes
- synchronized groups still use batched OpenAL starts and partial-group grace
- no private compat filter creation before a source is PLAYING/PAUSED eligible
- required direct/aux filter reattachments are not optimized away
- no replacement/cancellation of SPR's normal `calculateOcclusion()` flow
- no worker-thread SPR world/geometry raycasts
- strict source lifetime/generation handling
- opening-aware sound never creates a second playback source or changes source position/timing
- synchronized clarity balance changes only direct cutoff

## Known limitations and validation scope

See **[docs/KNOWN_LIMITATIONS.md](docs/KNOWN_LIMITATIONS.md)**.

In particular, opening discovery currently has a finite maximum search radius of 8 blocks, and the final multi-source performance benchmark covered 1- and 4-source cases; a 12-source stress run has not yet been completed for this release candidate.

## Building from source

Requires Java 21.

```text
./gradlew clean build
```

The build keeps the upstream Sound Physics Remastered runtime artifact untouched. For compilation only, `prepareSprCompileJar` creates an isolated access-transformed copy so javac sees the members used by the compatibility layer.

Useful verification tasks:

```text
./gradlew verifyReconstructionClasspath
./gradlew verifyResourceWiring
```

The historical task name `verifyReconstructionClasspath` is retained for build compatibility; it now validates the release compile classpath.

## Development and audit history

This source was reconstructed and audited against the previous known-good binary before the current release candidate was prepared. The detailed bytecode, structural ABI, mixin and runtime-validation records are intentionally retained under `docs/` and the frozen Git refs for traceability, but they are no longer the primary user documentation.

The current release branch should be judged by the release configuration, final CI, and runtime validation—not by the old phase-status documents.

## License

MIT
