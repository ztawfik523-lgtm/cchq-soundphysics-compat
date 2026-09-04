# CC:HQ Sound Physics Compat

Compatibility layer between **CC:HQ Speakers** and **Sound Physics Remastered (SPR)** for Minecraft 1.21.1 / NeoForge.

## Authoritative baseline

**Beta11 Hotfix3** remains the behavioral authority for the frozen reconstruction.

- Minecraft 1.21.1
- NeoForge 21.1.248
- Java 21
- CC:Tweaked 1.120.2
- CC:HQ Speakers tested artifact `ygA78R8l-u5PEI5Ax.jar`
- Sound Physics Remastered 1.21.1-1.5.1
- client-only compatibility mod

Authoritative Hotfix3 SHA-256:

`83500f182fc9829aa1a5a51fbfa11ba6cdfb645699b25d1c445167666dabc1ef`

## Reconstruction status

- Phase 1 — **COMPLETE / JAR-RECHECKED**
- Phase 2 — **COMPLETE / JAR-RECHECKED**
- Phase 3 — **COMPLETE / RECHECKED**
- Phase 4 — **COMPLETE / RECHECKED / FROZEN**
- Phase 5 — **IN PROGRESS — VERIFIED TEST CANDIDATE READY / AWAITING USER RUNTIME TEST**

Detailed status: `RECONSTRUCTION_STATUS.md`

## Frozen Phase 4 parity build

The exact reconstruction proved in Phase 4 is preserved separately and is not modified by Phase 5 feature/debug work.

- branch: `phase4-hotfix3-parity`
- frozen branch head: `79eed29767343ee34022e8f6268b386f75e84c9f`
- final audited code/build head: `98e7dedb7ecf6fda22008b084b6bb41956edff78`
- final verification: `docs/PHASE4_FINAL_VERIFICATION.md`

Phase 4 established 60/60 class-path and structural-ABI agreement, exact compiled constants/bootstrap recipes, reconciled Mixin/accessor metadata, byte-exact non-class Hotfix3 resources, and a 550-method control-flow review with no unresolved proven Hotfix3 behavior discrepancy.

## Phase 5 test candidate

Phase 5 deliberately leaves the actual Minecraft/audio test to the user. The automated preparation adds diagnostics and tunable controls, then performs build/static verification only.

Working branch:

`phase5-test-extended`

Frozen test candidate:

`phase5-test-candidate-1`

Verified candidate commit:

`44612192d875e43ecef66ca51798cab7adb17020`

Verified JAR:

`cchq_soundphysics_compat-0.1.0-beta11-phase5-test.jar`

SHA-256:

`6d782812f7915de1870a8b5ae0f619556e7ec1d24ef2eaffa7b5b225aa00bd93`

Verification run `33927205360` — **SUCCESS**.

No Minecraft launch was performed by that workflow.

Full preparation/test guide:

`docs/PHASE5_TEST_BUILD_PREP.md`

## Phase 5 configuration

The original Hotfix3 `ClientConfig.java` remains unchanged. Phase 5 adds a second client config:

`cchq_soundphysics_compat-advanced.toml`

All behavior-changing advanced options default to the values already verified in Phase 4. Non-default values intentionally diverge from Hotfix3 and exist for diagnosis/tuning.

New controls cover:

- room scheduler slot/staleness/recent-source thresholds;
- listener teleport and source-movement urgency thresholds;
- clearing-sentinel thresholds and cooldown;
- synchronized-start partial-flush and stale-group timers;
- private per-source EFX enable/fallback;
- Beta9 direct reuse, stable/relevance room backoff and adaptive load contribution;
- Beta9 movement/backoff bounds;
- Beta10 exact direct-ray cache;
- Beta11 same-clone room-ray memo;
- performance-report cadence;
- targeted logging categories.

Cloth Config exposes these under **Advanced Runtime** and **Debug & Validation**.

## Client-only diagnostic commands

- `/cchqphysics status`
- `/cchqphysics dump`
- `/cchqphysics refresh_rooms`
- `/cchqphysics reset_caches`
- `/cchqphysics reset_efx`
- `/cchqphysics config`

Reset/refresh commands queue their work and consume it on the existing scheduler sound-thread executor path rather than modifying OpenAL from the command callback.

## Frozen acoustic/runtime invariants

The default Phase 5 configuration preserves the verified Hotfix3 values/rules, including:

- no Lua-side changes;
- approved `SoundSource.BLOCKS` distance behavior;
- center + 8 inner + 8 outer progressive direct geometry;
- per-source private EFX isolation;
- direct/aux EFX reattachment on actual environment application;
- no private EFX before PLAYING/PAUSED eligibility;
- `PositionStabilizer` behavior;
- no replacement/cancellation of SPR `calculateOcclusion()`;
- no worker-thread SPR geometry raycasts;
- strict source identity/generation lifetime semantics;
- physics scheduling does not intentionally modify PCM/OpenAL playback position/clock/offset;
- Hotfix3 synchronized-group `alSourcePlayv` and 100 ms incomplete-group grace;
- Beta10 exact direct reuse / bit-identical OpenAL write suppression;
- Beta11 same-clone room-ray memo scope.

## Build-project note

Hotfix3 directly calls SPR members widened by this mod's access transformer. The build therefore keeps the untouched tested SPR artifact at runtime while producing an isolated access-transformed SPR copy for javac.

The Phase 5 branch disables Gradle configuration cache because the existing `prepareSprCompileJar` execution-time preprocessing task is intentionally not configuration-cache compatible. This does not change runtime behavior.

## Development roadmap

Do not begin post-reconstruction optimization work until Phase 5 runtime validation is complete and the maintained source is accepted as authoritative.

Planned later work remains:

1. Beta11.1 exact cleanup/performance housekeeping.
2. Beta12 persistent progressive room state.
3. Beta12.x acoustic work scheduler.
4. Beta13 sparse adaptive room map.

## Repository rule

Do not merge the reconstruction/Phase 5 work to `main` until the real-game Phase 5 validation is complete and reviewed.
