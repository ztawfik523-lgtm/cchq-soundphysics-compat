# CC:HQ Sound Physics Compat

Compatibility layer between **CC:HQ Speakers** and **Sound Physics Remastered (SPR)** for Minecraft 1.21.1 / NeoForge.

## Current known-good baseline

**Beta11 Hotfix3**

- Minecraft 1.21.1
- NeoForge 21.1.248
- Java 21
- CC:Tweaked 1.120.2
- CC:HQ Speakers 1.1.4-neoforge-1.21.1
- Sound Physics Remastered 1.21.1-1.5.1
- Client-only compatibility mod

Baseline artifact SHA-256:

`83500f182fc9829aa1a5a51fbfa11ba6cdfb645699b25d1c445167666dabc1ef`

This repository is being reconstructed from the tested Beta11 Hotfix3 artifact so future work can move away from direct JAR/bytecode patching and back to normal source-level development.

## Frozen acoustic/runtime invariants

The tested baseline preserves these rules:

- HQ whole-file playback is intercepted without Lua-side changes.
- Decode is off-thread; stereo/multichannel audio is downmixed to mono PCM for positional OpenAL playback.
- Shared OpenAL buffers/refcounts are used for synchronized speakers playing the same payload.
- Synchronized groups use `alSourcePlayv`; incomplete groups receive a short grace period so valid sources are not stranded in `AL_INITIAL`.
- Distance behavior remains based on the approved `SoundSource.BLOCKS` curve.
- Direct occlusion uses the approved progressive 17-path model: center + 8 inner + 8 outer, with adaptive 9-path partial refreshes.
- Private per-source EFX is used to prevent SPR global-filter cross-source contamination.
- **Every actual environment application must reattach direct/aux EFX. Do not optimize EFX attachment away.**
- Private EFX is not created before the OpenAL source reaches PLAYING/PAUSED eligibility.
- `PositionStabilizer` behavior is preserved.
- Do not inject/cancel/replace SPR `calculateOcclusion()`.
- No worker-thread SPR world/geometry raycasts.
- Source lifetime identity is source id + monotonic generation + speaker UUID.
- Physics scheduling must not alter PCM sample position, OpenAL playback clock, buffer offset, or sync-group timing.

## Beta11 additions retained in Hotfix3

- Beta10 exact direct-occlusion sharing remains active for HQ-owned SPR calls.
- Exact same-clone room/bounce `RaycastUtils.rayCast` memoization for SPR `evaluateEnvironment`.
- Diagnostics can report room-ray hits, misses, same-clone savings, and cross-clone reuse potential.
- Batched PCM mono conversion writes.
- OpenAL `alSourcePlayv` synchronized group start.
- Hotfix3 protects incomplete sync groups during a short grace window and starts the sources that actually arrived.

## Development roadmap

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
   - fairness, age ceilings, and bounded per-tick acoustic work.

4. **Beta13 — Sparse Adaptive Room Map (D)**
   - room-only spatial memory;
   - direct occlusion remains exact/current;
   - sparse adaptive listener cells across the speaker's audible range.

Later: optional HQ enhanced/music spatial mode. Adaptive quality reduction (lower ray/bounce counts) is shelved.

## Repository state

The binary Beta11 Hotfix3 artifact is the authoritative behavioral baseline. Source reconstruction is intentionally tracked separately until it can be verified against that artifact; no decompiled/reconstructed source should be assumed equivalent merely because it compiles.
