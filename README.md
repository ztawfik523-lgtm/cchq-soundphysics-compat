# CC:HQ Sound Physics Compat

Compatibility layer between **CC:HQ Speakers** and **Sound Physics Remastered (SPR)** for Minecraft 1.21.1 / NeoForge.

## Current project state

The Beta11 Hotfix3 binary has been fully reconstructed into maintainable Java source and statically audited. Core Phase-5 runtime validation has also passed in the real Minecraft/NeoForge/OpenAL environment.

Canonical status:

- Phase 1 — **COMPLETE / JAR-RECHECKED**
- Phase 2 — **COMPLETE / JAR-RECHECKED**
- Phase 3 — **COMPLETE / RECHECKED**
- Phase 4 — **COMPLETE / RECHECKED**
- Phase 5 — **RUNTIME CORE PASSED / FINAL OPTIONAL-FEATURE RETEST PENDING**

Only one short listening test remains before Phase 5 can be marked **COMPLETE / RECHECKED**: the new optional synchronized multi-speaker mixing feature.

See:

- `RECONSTRUCTION_STATUS.md`
- `docs/PHASE4_FINAL_VERIFICATION.md`
- `docs/PHASE5_RUNTIME_VALIDATION.md`
- `docs/SOURCE_HANDOVER.md`
- `docs/BUILD_FROM_SOURCE.md`

## Frozen Hotfix3 authority

Historical authoritative JAR:

`cchq_soundphysics_compat-0.1.0-beta11-hotfix3.jar`

SHA-256:

`83500f182fc9829aa1a5a51fbfa11ba6cdfb645699b25d1c445167666dabc1ef`

The reconstructed Hotfix3-equivalent source is permanently anchored by:

- `phase4-hotfix3-parity`
- `archive-phase4-hotfix3-parity`
- head: `79eed29767343ee34022e8f6268b386f75e84c9f`
- final audited Phase-4 code/build commit: `98e7dedb7ecf6fda22008b084b6bb41956edff78`

Those branches should remain historical references, not normal development branches.

## Runtime-tested extended candidate

`phase5-test-candidate-1`

Commit:

`44612192d875e43ecef66ca51798cab7adb17020`

JAR SHA-256:

`6d782812f7915de1870a8b5ae0f619556e7ec1d24ef2eaffa7b5b225aa00bd93`

This candidate passed clean CI plus real Minecraft/OpenAL tests including 12 concurrent sources, movement, thick-wall occlusion, doorway transitions, source cleanup, private EFX isolation, cache/reset diagnostics and synchronized partial-group starts.

## Final optional-feature test candidate

Working branch:

`phase5-finalization`

Verified code commit:

`323d0e34651ae086dcd96ebe608b3149f5f0d73a`

Version:

`0.1.0-beta11-phase5-final-test`

Read-only verification run:

`33931215077` — **SUCCESS**

JAR SHA-256:

`8bfea798256758fa35af65b99fe4434d0c5a940f7dfbb12df5a7f7e8dcaf7d70`

This final-test build adds one post-parity feature: **optional attenuation of occluded synchronized copies of the same payload**. It is **OFF by default**, so installing this build without enabling it retains the already-runtime-validated behavior.

## Supported/tested stack

- Java 21
- Minecraft 1.21.1
- NeoForge 21.1.248
- Gradle wrapper 9.2.1
- ModDevGradle 2.0.144
- CC:Tweaked 1.120.2
- CC:HQ Speakers pinned Modrinth project/version IDs `ygA78R8l` / `u5PEI5Ax`
- CC:HQ runtime reports internal version `1.1.4-neoforge-1.21.1`
- Sound Physics Remastered 1.21.1-1.5.1
- Cloth Config 15.0.140
- client-only compatibility mod

## Core design invariants

- HQ whole-file audio is bridged into real positional OpenAL sources without Lua changes.
- Decode remains off-thread; OpenAL mutation remains on the sound-thread path.
- Distance behavior uses the approved `SoundSource.BLOCKS` curve.
- Progressive direct occlusion preserves center + 8 inner + 8 outer geometry.
- Private per-source EFX isolates speakers from SPR global-filter cross-source contamination.
- Every actual environment application must reattach required direct/aux EFX.
- No private EFX before PLAYING/PAUSED eligibility.
- `PositionStabilizer` behavior is preserved.
- Do not cancel/replace SPR `calculateOcclusion()` wholesale.
- No arbitrary worker-thread SPR world/geometry raycasts.
- Source lifetime generation/identity remains strict.
- Physics scheduling must not intentionally alter PCM sample position, OpenAL playback clock or buffer offset.
- Hotfix3 partial synchronized-group grace remains preserved.
- Beta10 exact direct reuse/write suppression remains available.
- Beta11 room-ray memoization remains exact and same-clone only; cross-clone reuse is telemetry-only.

## Config and diagnostics

The maintained extended source uses:

- `cchq_soundphysics_compat-client.toml` — normal acoustics/user controls
- `cchq_soundphysics_compat-advanced.toml` — scheduler/cache/EFX/debug controls with Hotfix3-equivalent defaults
- `cchq_soundphysics_compat-mixing.toml` — optional synchronized multi-speaker mixing feature, default OFF

Client commands:

- `/cchqphysics status`
- `/cchqphysics dump`
- `/cchqphysics refresh_rooms`
- `/cchqphysics reset_caches`
- `/cchqphysics reset_efx`
- `/cchqphysics config`

## Build from source

Canonical clean build:

```text
./gradlew --no-configuration-cache clean jar
```

On Windows:

```text
gradlew.bat --no-configuration-cache clean jar
```

The build automatically creates an access-transformed compile-only SPR copy while keeping the untouched pinned SPR artifact at runtime. See `docs/BUILD_FROM_SOURCE.md` for the exact contract.

## Development roadmap after Phase 5 closes

Potential later work remains separate from reconstruction closure:

1. **Beta11.1 — exact cleanup / allocation and cache efficiency**
2. **Beta12 — persistent progressive room state**
3. **Beta12.x — bounded acoustic work scheduler**
4. **Beta13 — sparse adaptive room map**

Do not merge the finalization work to `main` until Phase 5 is formally closed and the user explicitly chooses to merge it.
