# Phase 5 synchronized timing + acoustic-send diagnostic — build record

Date: 2026-09-05

Status: **STATIC / COMPILE / BUILD VERIFIED — AWAITING USER RUNTIME TEST**

## Source identity

Branch:

`phase5-sync-acoustic-diagnostics`

Exact runtime-source commit:

`95bd4b06b78786d4f7b1ad33b665f4685e45a54b`

Source tree:

`a12abefcff2cf75cad5d0b9779ec6d4e333e1cc5`

Build identity:

`0.1.0-beta11-phase5-syncdiag-test`

Base working/docs commit:

`88e78a89a16d1e396ab72707796e3cb197feb9dc`

Frozen authorities rechecked by CI:

- Phase 4: `79eed29767343ee34022e8f6268b386f75e84c9f`
- known-good Phase 5: `44612192d875e43ecef66ca51798cab7adb17020`
- reviewed Issue A: `973f1df7dad886fb0f5fffd4264015fecac2e786`

## Scope

The build measures two hypotheses in the same synchronized playback while keeping their conclusions separate:

1. actual OpenAL playback-cursor skew / micro-desync;
2. per-source direct + reverb-send differences across correlated synchronized copies.

The source commit changes no maintained playback algorithm. CI explicitly diff-checks these behavior-core files against reviewed Issue A:

- `CompatAudioManager.java`
- `DistanceBridge.java`
- `ProgressiveOcclusionModel.java`
- `PositionStabilizer.java`
- `ReflectionDiagnostics.java`
- `SoundPhysicsBridge.java`
- `SyncStartCoordinator.java`
- `Beta9Optimizer.java`
- `Beta10Optimizer.java`
- `Beta11RoomRayCache.java`

`EnvironmentSmoother.java` only adds dump output for already-stored typed `r0..r3` / `h0..h3` state plus a sorted source-ID snapshot helper. `IssueADiagnostics.java` only schedules the read-only cursor snapshot on the existing sound-thread executor. `SyncAcousticDiagnostics.java` performs OpenAL reads only.

## Verification workflow

Workflow:

`Phase 5 synchronized timing and acoustic diagnostics verification`

Run:

`33939999239`

Job:

`101235407189`

Head SHA:

`95bd4b06b78786d4f7b1ad33b665f4685e45a54b`

Result:

**SUCCESS**

Successful gates included:

- Java 21 setup / wrapper validation;
- exact frozen-ref assertions;
- diagnostic identity assertions;
- Phase-5 parity-default audit;
- original Hotfix3 `ClientConfig.java` unchanged;
- acoustic + sync behavior core unchanged from reviewed Issue A;
- diagnostic-only cursor/EFX assertions;
- clean `compileJava`;
- clean JAR build;
- JAR inspection;
- artifact upload.

## Artifact

Artifact name:

`cchq-phase5-sync-acoustic-diagnostics`

Artifact id:

`9961502178`

Artifact digest:

`sha256:d721d21f164ed9ce7652f9973e9f824b932d82a7e2d719be44a50c88a3ee1373`

JAR:

`cchq_soundphysics_compat-0.1.0-beta11-phase5-syncdiag-test.jar`

Independently rechecked JAR SHA-256:

`1910778a12219f84e5ad5a71449e353e99f89ef572fb599a3bc79bc568fcdb9e`

Class count:

`70`

Embedded metadata:

```text
source_commit=95bd4b06b78786d4f7b1ad33b665f4685e45a54b
known_good_candidate=44612192d875e43ecef66ca51798cab7adb17020
issue_a_reviewed=973f1df7dad886fb0f5fffd4264015fecac2e786
phase4_frozen=79eed29767343ee34022e8f6268b386f75e84c9f
audio_behavior_mutation=false
typed_reverb_send_telemetry=true
openal_cursor_telemetry=true
reflection_diagnostics_retained=true
spectral_mix_v1_included=false
spectral_mix_v2_included=false
game_launch_performed=false
```

## Runtime status

No game launch or listening conclusion is claimed by this build record. Runtime validation belongs to the user.

Required user test:

- reproduce the synchronized coloration;
- do not move;
- run `/cchqphysics dump` three times about 1–2 seconds apart while the coloration remains audible;
- return `latest.log` plus a statement that the coloration was/was not audible during each snapshot.

The same snapshots carry both timing and acoustic-send evidence.
