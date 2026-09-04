# Reconstruction documentation sync — 2026-09-04

This file records the full documentation sweep performed after the exact Beta11 Hotfix3 JAR was supplied and Phases 1–2 were rechecked against it.

## Authority

Artifact:

`cchq_soundphysics_compat-0.1.0-beta11-hotfix3.jar`

SHA-256:

`83500f182fc9829aa1a5a51fbfa11ba6cdfb645699b25d1c445167666dabc1ef`

Branch:

`beta11-source-reconstruction`

## Current canonical state

- Phase 1 — **COMPLETE / JAR-RECHECKED**
- Phase 2 — **COMPLETE / JAR-RECHECKED**
- Phase 3 — **IN PROGRESS**
- Phase 4 — not started
- Phase 5 — not started

Known remaining top-level authored Phase 3 gaps:

1. `SoundPhysicsBridge`
2. `ClothConfigScreen`

The latest compile boundary is 17 javac errors, all references to the missing `SoundPhysicsBridge` class. The earlier SPR private-access errors are resolved by compile-time AT preprocessing.

## Phase 1 recheck facts

The exact JAR recheck confirms:

- 75 ZIP entries;
- 10 directories;
- 65 non-directory files;
- 60 Java-21 classfiles;
- all 65 per-entry SHA-256 fingerprints agree with `docs/baseline/HOTFIX3_SHA256SUMS.txt`;
- all five source-relevant resources agree with the exact JAR bytes and Git blob identities;
- manifest CRLF form remains exact.

During this documentation sweep, a temporary documentation-only error in `PHASE1_JAR_RECHECK_2026-09-04.md` was found: its resource checksum table did not match the actual supplied JAR. The whole-JAR hash and frozen `HOTFIX3_SHA256SUMS.txt` were correct. The recheck document was corrected from the actual JAR bytes and now agrees with `PHASE1_FINAL_VERIFICATION.md`.

## Phase 2 recheck facts

The exact reconstructed Hotfix3 source calls SPR members widened by the compat access transformer. The reconstructed build now correctly supplies that accessibility contract to javac:

- runtime uses untouched tested SPR;
- an isolated exact SPR compile copy is transformed with the exact Hotfix3 AT;
- javac uses `sound-physics-remastered-at.jar`;
- raw SPR is rejected from compileClasspath;
- AT CLI 10.0.6 is used only as the Java-21-capable preprocessing tool.

Verified CI:

- classpath run `33864425672` — success;
- finish-gate run `33864425687` — success;
- build commit `cef50d04fbb03b4f523961aeb95f2f0377856994`.

The full finish gate verifies wrapper/toolchain, 90-file compileClasspath, NeoForge artifacts, processed resources, all 11 mixins, AT registration and javac boundary classification.

## Phase 3 state recorded by this sync

Most source is present, including the playback/lifecycle stack, direct occlusion helpers, optimizers, config core and all configured mixin/accessor source counterparts.

`SoundPhysicsBridge` remains the principal runtime source and compile blocker. Its nested topology to recover intentionally is:

- `SoundPhysicsBridge$Candidate`
- `SoundPhysicsBridge$RoomEnvironmentAccess`
- `SoundPhysicsBridge$RoomEnvironmentAccess$ConfigStamp`
- `SoundPhysicsBridge$RoomStamp`
- `SoundPhysicsBridge$SourceState`

`ClothConfigScreen` remains the second top-level source gap and must be reconstructed before Phase 3 closure.

## Files synchronized

The following documentation was refreshed so it no longer carries stale assumptions such as "the JAR is unavailable", "11 classes remain", the obsolete ad-hoc phase numbering, or the pre-AT compile boundary:

- `README.md`
- `RECONSTRUCTION_STATUS.md`
- `docs/BETA11_RECONSTRUCTION_HANDOFF.md`
- `docs/PHASE1_HOTFIX3_BYTECODE_AUDIT.md`
- `docs/PHASE2_BUILD_AUDIT.md`
- `docs/PHASE3_START_AUDIT.md`
- `docs/RECONSTRUCTION_GUIDE.md`
- `docs/RECONSTRUCTION_PHASES.md`
- `docs/baseline/BETA11_HOTFIX3_INVENTORY.md`
- `docs/baseline/PHASE1_FINAL_VERIFICATION.md`
- `docs/baseline/PHASE1_JAR_RECHECK_2026-09-04.md`

`docs/baseline/HOTFIX3_SHA256SUMS.txt` was not changed because its frozen 65-entry fingerprint list was already correct.

## Frozen next step

Continue Phase 3 by reconstructing `SoundPhysicsBridge` from the exact Hotfix3 classfile/decompile. Do not add a compile-only stub. After it is reconstructed, run the compile gate, reconstruct `ClothConfigScreen`, reconcile the full Phase 1 class/nested-class inventory, then close Phase 3 only if the full project compiles and every meaningful binary class has an intentional source/compiler-generated explanation.

Do not begin Beta11.1/B optimization before Phases 4 and 5 also close.
