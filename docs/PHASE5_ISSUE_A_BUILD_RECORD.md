# Phase 5 Issue A — verified diagnostic build record

Date: 2026-09-05

## Source and branch

- working branch: `phase5-issue-a-reflection-diagnostics`
- frozen test source branch: `phase5-issue-a-test-candidate`
- exact tested source commit: `33eef5c00198b2f1891cc78340f94ece4e1c7bfc`
- known-good baseline candidate: `44612192d875e43ecef66ca51798cab7adb17020`
- frozen Phase 4 parity head: `79eed29767343ee34022e8f6268b386f75e84c9f`

## Verification

Workflow: `Phase 5 Issue A reflection diagnostics verification`

- run: `33935114299`
- job: `101221406295`
- result: **SUCCESS**

The verifier confirmed:

- Phase 4 and known-good Phase 5 refs remain unchanged;
- build identity is `0.1.0-beta11-phase5-issuea-test`;
- runtime reflection redirection defaults to **ON**;
- `ClientConfig`, `ExtendedClientConfig`, `CompatAudioManager`, `DistanceBridge`, `EnvironmentSmoother`, `ProgressiveOcclusionModel`, `SoundPhysicsBridge`, and `SyncStartCoordinator` are unchanged from known-good candidate-1;
- clean Java 21 compilation succeeds;
- JAR packaging succeeds;
- expected Issue A diagnostic classes are packaged;
- exactly 67 classfiles are present;
- no Spectral Mix V1 or V2 code is included in this build.

## Artifact

GitHub Actions artifact:

- name: `cchq-phase5-issue-a-reflection-diagnostics`
- artifact id: `9959912444`
- artifact digest: `sha256:a68bbcf7d36fccc8e435483da94d1dbba0c15ace0764e1725edf48831be68f75`

JAR:

- filename: `cchq_soundphysics_compat-0.1.0-beta11-phase5-issuea-test.jar`
- size: `173525` bytes
- SHA-256: `8259d5c40671cb2c8a0b36eaa5987f33a6e061714b109630e32fe6f918347d47`

Build metadata:

- `reflection_redirect_default=true`
- `spectral_mix_v1_included=false`
- `spectral_mix_v2_included=false`
- `game_launch_performed=false`

## Runtime purpose

This build is not a new acoustic candidate. It is a diagnostic derivative of the known-good candidate whose only runtime behavioral A/B control is reflected-position redirection.

Use the exact procedure in `docs/PHASE5_ISSUE_A_REFLECTION_DIAGNOSTICS.md` and return `latest.log` after ON → OFF → ON testing with `/cchqphysics dump` at each state.
