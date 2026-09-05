# Phase 5 Issue A — verified diagnostic build record

Date: 2026-09-05

Status: **STATIC/BUILD VERIFIED — AWAITING USER A/B LISTENING TEST**

## Source and branches

- working branch: `phase5-issue-a-reflection-diagnostics`
- earlier diagnostic snapshot: `phase5-issue-a-test-candidate` at `33eef5c00198b2f1891cc78340f94ece4e1c7bfc`
- reviewed frozen test branch: `phase5-issue-a-test-candidate-2`
- exact reviewed/test source commit: `973f1df7dad886fb0f5fffd4264015fecac2e786`
- known-good baseline candidate: `44612192d875e43ecef66ca51798cab7adb17020`
- frozen Phase 4 parity head: `79eed29767343ee34022e8f6268b386f75e84c9f`

The earlier `phase5-issue-a-test-candidate` is intentionally retained as historical evidence of the first diagnostic draft rather than force-moved.

## Final reviewed verification

Workflow: `Phase 5 Issue A reflection diagnostics verification`

- run: `33935819269`
- job: `101223434623`
- result: **SUCCESS**

The verifier confirmed:

- Phase 4 and known-good Phase 5 refs remain unchanged;
- build identity is `0.1.0-beta11-phase5-issuea-test`;
- all 35 advanced/debug Phase-5 parity defaults remain correct;
- original Hotfix3 `ClientConfig.java` is unchanged from frozen Phase 4;
- unrelated acoustic/runtime core remains byte-for-byte unchanged from known-good candidate-1, including `ExtendedClientConfig`, `CompatAudioManager`, `DistanceBridge`, `EnvironmentSmoother`, `ProgressiveOcclusionModel`, `SoundPhysicsBridge`, `SyncStartCoordinator`, `Beta9Optimizer`, `Beta10Optimizer`, and `Beta11RoomRayCache`;
- global reflection redirection defaults to **ON**;
- per-source ON/OFF/AUTO override support is present;
- per-source overrides are cleared when their OpenAL source unregisters;
- no reflection-based Issue-A private-state introspection remains;
- the reflection diagnostic is not persisted in config;
- clean Java 21 compilation succeeds;
- JAR packaging succeeds;
- exactly 67 classfiles are present;
- no Spectral Mix V1 or V2 code is included.

The first reviewed CI attempt failed only on a Java syntax error in the initially nested Brigadier command-builder expression. Command registration was rewritten into named builders (`root`, `reflection`, `source`, `sourceId`), after which the final run above passed. No acoustic/runtime core change was made to fix that compile error.

## Artifact

GitHub Actions artifact:

- name: `cchq-phase5-issue-a-reflection-diagnostics`
- artifact id: `9960138065`
- artifact digest: `sha256:26f2a427795f8fd69e4c9459d165cd748f57471dadc27bdb54c96dda162e4989`

JAR:

- filename: `cchq_soundphysics_compat-0.1.0-beta11-phase5-issuea-test.jar`
- SHA-256: `d649f14cdce89db21a79c396dbdecca681daf3d0389dc794a7ad52929f8c8451`
- packaged classfiles: **67**

The two Issue-A support classes added over the known-good 65-class candidate are:

- `dev/cchqphysics/compat/audio/ReflectionDiagnostics.class`
- `dev/cchqphysics/compat/audio/IssueADiagnostics.class`

Build metadata explicitly records:

- `source_commit=973f1df7dad886fb0f5fffd4264015fecac2e786`
- `known_good_candidate=44612192d875e43ecef66ca51798cab7adb17020`
- `phase4_frozen=79eed29767343ee34022e8f6268b386f75e84c9f`
- `reflection_redirect_global_default=true`
- `per_source_override=true`
- `reflective_issue_a_introspection=false`
- `spectral_mix_v1_included=false`
- `spectral_mix_v2_included=false`
- `game_launch_performed=false`

## Review changes incorporated before final verification

External code review correctly identified three weaknesses in the first diagnostic draft:

1. the global-only reflection toggle was too blunt for a correlated synchronized group;
2. reflection-based access to private diagnostic state was fragile;
3. the frozen-base branch inherited stale Phase-5 documentation and obsolete one-shot mutation scripts.

The reviewed candidate therefore:

- supports per-source ON/OFF/AUTO reflection overrides in addition to global A/B;
- clears per-source overrides on source unregister;
- uses compile-checked package-local dump paths instead of reflection into private state;
- removes inherited `phase5_apply_batch1..4.py` scripts;
- carries current Phase-5 README/status/Issue-A documentation.

## Runtime purpose

This build is not yet a new acoustic candidate. It is a diagnostic derivative of the known-good candidate.

Use the matrix in `docs/PHASE5_ISSUE_A_REFLECTION_DIAGNOSTICS.md`:

1. standalone speaker ON/OFF;
2. synchronized group global ON/OFF;
3. synchronized group with only one redirected source overridden OFF.

Return `latest.log` plus the subjective result. If reflection A/B does not track the coloration, the next diagnostic step is typed room/reverb-send telemetry rather than changing acoustic defaults by guesswork.
