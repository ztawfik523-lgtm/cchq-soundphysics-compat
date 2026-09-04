# CC:HQ Sound Physics Compat

Compatibility layer between **CC:HQ Speakers** and **Sound Physics Remastered (SPR)** for Minecraft 1.21.1 / NeoForge.

## Current known-good baseline

**Beta11 Hotfix3**

- Minecraft 1.21.1
- NeoForge 21.1.248
- Java 21
- CC:Tweaked 1.120.2
- CC:HQ Speakers tested artifact `ygA78R8l-u5PEI5Ax.jar`
- Sound Physics Remastered 1.21.1-1.5.1
- client-only compatibility mod

Authoritative baseline artifact SHA-256:

`83500f182fc9829aa1a5a51fbfa11ba6cdfb645699b25d1c445167666dabc1ef`

The exact Hotfix3 JAR was independently reverified on 2026-09-04 and remains the behavioral authority through runtime validation.

## Reconstruction state

Work is isolated on `beta11-source-reconstruction`.

Canonical status:

- Phase 1 — **complete / JAR-rechecked**
- Phase 2 — **complete / JAR-rechecked**
- Phase 3 — **complete**
- Phase 4 — **next / not started**
- Phase 5 — not started

Phase 3 final source gate:

- workflow run `33867207760` — **success**;
- clean `compileJava` — pass;
- reconstructed JAR task — pass;
- Hotfix3 compiled class topology — **60 expected / 60 reconstructed / no path diff**;
- all five source-relevant processed resources — pass.

Final Phase 3 authored closures:

- `SoundPhysicsBridge.java` — commit `91d70508a04001da788ac7520e09955d5f753b09`;
- `ClothConfigScreen.java` — commit `d336bdea9d39be801360b1f286d67f29d6333772`.

See:

- `RECONSTRUCTION_STATUS.md`
- `docs/BETA11_RECONSTRUCTION_HANDOFF.md`
- `docs/RECONSTRUCTION_PHASES.md`
- `docs/PHASE3_FINAL_VERIFICATION.md`

## Build-project note

Hotfix3 directly calls SPR members widened by this mod's access transformer. The reconstructed build therefore:

- keeps the untouched tested SPR artifact at runtime;
- creates an isolated access-transformed SPR copy for javac;
- compiles against `sound-physics-remastered-at.jar`;
- explicitly rejects the raw private-member SPR JAR from compileClasspath.

The JAR-backed Phase 2 recheck succeeded in GitHub Actions runs `33864425672` and `33864425687`.

## Frozen acoustic/runtime invariants

The tested baseline preserves these rules:

- HQ whole-file playback is intercepted without Lua-side changes.
- Decode is off-thread; stereo/multichannel audio is downmixed to mono PCM for positional OpenAL playback.
- Shared OpenAL buffers/refcounts are used for synchronized speakers playing the same payload.
- Synchronized groups use `alSourcePlayv`; incomplete groups receive the Hotfix3 100 ms grace period so arrived sources are not stranded in `AL_INITIAL`.
- Distance behavior remains based on the approved `SoundSource.BLOCKS` curve.
- Direct occlusion uses the approved progressive 17-path model: center + 8 inner + 8 outer, with adaptive 9-path partial refreshes.
- Private per-source EFX prevents SPR global-filter cross-source contamination.
- **Every actual environment application must reattach direct/aux EFX. Do not optimize EFX attachment away.**
- Private EFX is not created before the OpenAL source reaches PLAYING/PAUSED eligibility.
- `PositionStabilizer` behavior is preserved.
- Do not inject/cancel/replace SPR `calculateOcclusion()`.
- `SoundPhysicsOcclusionMemoMixin` redirects the internal `runOcclusion(...)` call instead of replacing the whole calculation.
- No worker-thread SPR world/geometry raycasts.
- Source lifetime identity/generation semantics remain strict.
- Physics scheduling must not alter PCM sample position, OpenAL playback clock, buffer offset or sync-group timing.
- Beta10 exact direct reuse and bit-identical OpenAL write suppression remain intact.
- Beta11 room-ray cache remains same-clone/exact; cross-clone reuse is telemetry-only.

## Beta11 additions retained in Hotfix3

- Beta10 exact direct-occlusion reuse for HQ-owned SPR calls.
- Exact same-clone room/bounce ray memoization in SPR `evaluateEnvironment`.
- Room-ray diagnostics including cross-clone reuse-potential telemetry.
- Batched PCM mono conversion writes.
- OpenAL `alSourcePlayv` synchronized group start.
- Hotfix3 pending-INITIAL protection and partial-group grace behavior.

## Next work: Phase 4

Phase 4 is a structural and behavioral equivalence audit, not feature development. It must compare reconstructed output against the exact Hotfix3 JAR for class/method descriptors, constants, mixins, OpenAL ordering, source lifecycle, sync logic, direct/room caches, EFX behavior, scheduler/stamp/sentinel semantics, position handling and config/resources.

Do not treat the Phase 3 green build as runtime equivalence proof.

## Development roadmap

Do not start this roadmap until Phases 4–5 close and reconstructed source becomes authoritative.

1. **Beta11.1 — exact cleanup (B)**
   - remove redundant decode/probe work safely;
   - reduce whole-track PCM copies where practical;
   - replace fixed-entry decoded cache with byte-budgeted LRU;
   - add a short-lived byte-budgeted warm OpenAL buffer cache;
   - remove repeated sound-thread allocation churn where it matters;
   - make diagnostics-off hot paths genuinely cheap;
   - add focused hash/decode/downmix/upload timing instrumentation.

2. **Beta12 — Persistent Progressive Room (C1)**
   - persistent temporal source-centered room/bounce state;
   - budgeted subset refresh using the current SPR clone;
   - current listener/shared-airspace work stays fresh;
   - change detection can force urgent/full room refresh.

3. **Beta12.x — Acoustic work scheduler (C2)**
   - schedule room branch/ray work instead of whole-room jobs;
   - fairness, age ceilings and bounded per-tick acoustic work.

4. **Beta13 — Sparse Adaptive Room Map (D)**
   - room-only spatial memory;
   - direct occlusion remains exact/current;
   - sparse adaptive listener cells across the speaker's audible range.

Later: optional HQ enhanced/music spatial mode. Adaptive quality reduction remains shelved.

## Repository rule

Do not merge `beta11-source-reconstruction` to `main` until Phase 4 structural/behavioral auditing and Phase 5 runtime validation are complete.
