# Phase 5 elevation diffraction build record

Status: build/CI verified, runtime listening not yet performed.

## Exact runtime source

- branch: `phase5-elevation-diffraction-test`
- runtime source commit: `01761f3bc385dcc0c88c1cb3f21795518db01f3c`
- approved HF50 configurable runtime baseline: `62d3a7a0a176c901402b913946d98f3cb455a8f4`

## Artifact

- JAR: `cchq_soundphysics_compat-0.1.0-beta11-phase5-diffraction-test.jar`
- SHA-256: `69ff2d9c50a1bf881cde2a8a690c4bf903eda6565174bfb3e7c5c92a13b40982`
- classfiles: 71
- GitHub Actions run: `33944797099`
- artifact id: `9962995928`
- artifact ZIP digest: `sha256:9c75fa4999d69856fa7fc546db4282bb265142e713ff2a8b87379dceb6882cba`

## CI assertions passed

- Java 21 / reconstruction classpath
- resource wiring
- clean compile
- JAR build
- artifact inventory and identity
- Phase-4 / known-good / HF50 baseline refs exact
- `SynchronizedSpectralBalancer`, `SpectralMixConfig`, `ProgressiveOcclusionModel`, `SyncStartCoordinator`, `PositionStabilizer`, Beta9, Beta10, Beta11, `CompatAudioManager`, and `ClientConfig` unchanged from the runtime-approved HF50 source
- narrow integration only: one direct-target call in `EnvironmentSmoother`, physical-source update/unregister/clear hooks in `SoundPhysicsBridge`
- diffraction defaults OFF
- no source AL gain mutation
- no source position mutation
- no reflection mutation
- no reverb-send mutation
- no playback timing or synchronized-start mutation

## Runtime test required

Use one speaker if possible to keep HF50 irrelevant to this isolation.

1. reproduce the steep open-top over-muffling position
2. `/cchqphysics diffraction off`
3. `/cchqphysics dump`
4. listen baseline
5. `/cchqphysics diffraction on`
6. hold the same position for a few seconds
7. `/cchqphysics dump`
8. compare sound
9. optional but valuable negative control: a sealed floor/ceiling or ordinary wall with diffraction ON

Expected useful log line prefix: `[phase5/diffraction]`.

Do not promote this experiment before user listening + dump evidence.
