# CC:HQ Sound Physics Compat

Compatibility layer between **CC:HQ Speakers** and **Sound Physics Remastered (SPR)** for Minecraft 1.21.1 / NeoForge.

## Authoritative baseline

**Beta11 Hotfix3** remains the behavioral authority for the frozen reconstruction.

- Minecraft 1.21.1
- NeoForge 21.1.248
- Java 21
- CC:Tweaked 1.120.2
- CC:HQ Speakers tested artifact `ygA78R8l-u5PEI5Ax.jar` (runtime mod version reports `1.1.4-neoforge-1.21.1`)
- Sound Physics Remastered 1.21.1-1.5.1
- client-only compatibility mod

Authoritative Hotfix3 SHA-256:

`83500f182fc9829aa1a5a51fbfa11ba6cdfb645699b25d1c445167666dabc1ef`

## Reconstruction status

- Phase 1 — **COMPLETE / JAR-RECHECKED**
- Phase 2 — **COMPLETE / JAR-RECHECKED**
- Phase 3 — **COMPLETE / RECHECKED**
- Phase 4 — **COMPLETE / RECHECKED / FROZEN**
- Phase 5 — **IN PROGRESS — CORE RUNTIME PASSED / ISSUE A DIAGNOSTICS IN PROGRESS**

Detailed status: `RECONSTRUCTION_STATUS.md`

## Frozen Phase 4 parity build

The exact Hotfix3 reconstruction is preserved separately and is not modified by Phase-5 diagnostics/experiments.

- branch: `phase4-hotfix3-parity`
- archive ref: `archive-phase4-hotfix3-parity`
- frozen branch head: `79eed29767343ee34022e8f6268b386f75e84c9f`
- final audited code/build head: `98e7dedb7ecf6fda22008b084b6bb41956edff78`
- final verification: `docs/PHASE4_FINAL_VERIFICATION.md`

Phase 4 established 60/60 class-path and structural-ABI agreement, exact compiled constants/bootstrap recipes, reconciled Mixin/accessor metadata, byte-exact non-class Hotfix3 resources, and a 550-method control-flow review with no unresolved proven Hotfix3 behavior discrepancy.

## Known-good Phase 5 runtime candidate

Frozen test candidate:

`phase5-test-candidate-1`

Verified candidate commit:

`44612192d875e43ecef66ca51798cab7adb17020`

Verified JAR:

`cchq_soundphysics_compat-0.1.0-beta11-phase5-test.jar`

SHA-256:

`6d782812f7915de1870a8b5ae0f619556e7ec1d24ef2eaffa7b5b225aa00bd93`

That candidate received real-game validation covering startup/Mixins, OpenAL/EFX, multi-source stress, movement/doorway transitions, cache telemetry, debug reset commands, synchronized playback, cleanup and shutdown. The user reported the general known-good acoustics sounded correct.

## Current investigation — Issue A

Branch:

`phase5-issue-a-reflection-diagnostics`

Build identity:

`0.1.0-beta11-phase5-issuea-test`

Issue A investigates an intermittent hard-to-name perceived coloration described as possible extra reverb, treble, spatial smear or altered music timbre. The report was observed before Spectral Mix V2 was enabled, so this branch is based directly on the known-good candidate and contains neither synchronized-mix experiment.

Leading hypothesis:

an occluded synchronized copy may be spatially redirected toward SPR's reflected point while correlated copies remain bright/direct, producing a perceptual interaction that sounds reverb-like or phasey.

This is still a hypothesis.

The diagnostic build keeps reflection redirection ON by default every launch and adds runtime-only A/B controls:

- `/cchqphysics reflection_redirect on`
- `/cchqphysics reflection_redirect off`
- `/cchqphysics reflection_redirect status`
- `/cchqphysics reflection_redirect source <sourceId> on`
- `/cchqphysics reflection_redirect source <sourceId> off`
- `/cchqphysics reflection_redirect source <sourceId> auto`
- `/cchqphysics reflection_redirect source <sourceId> status`

The per-source override exists specifically so one correlated member can be isolated while the rest of a synchronized group remains on the known-good behavior. Overrides are cleared when the OpenAL source unregisters.

`/cchqphysics dump` includes focused position snapshots showing real/applied/reflected position, offset, occlusion, requested-vs-active redirect state, and source override/effective state alongside the existing source/EFX dump.

Full test matrix and rationale:

`docs/PHASE5_ISSUE_A_REFLECTION_DIAGNOSTICS.md`

## Diagnostic implementation hygiene

An early Issue-A draft inspected private runtime state through Java reflection. That was removed after review. The current Issue-A dump uses compile-checked package-local diagnostic paths instead.

The branch also removes the inherited one-shot `phase5_apply_batch1..4.py` mutation scripts. Those were historical scaffolding from the frozen Phase-5 candidate, not maintained source requirements.

## Synchronized-mix experiments

### V1 — rejected

The first optional synchronized-mix feature applied extra whole-source gain suppression to occluded copies. It distorted spatial weighting and could make a clear off-axis source dominate left/right balance. That design is rejected.

### V2 — frozen experiment, not final

`phase5-mix-v2-test-candidate` preserves a spectral-only experiment that never changes source gain or position. Its build was technically clean, but the broader perceived-coloration report predates V2, so Issue A must be resolved separately before deciding whether V2 belongs in maintained source.

## Separate elevation / diffraction limitation

A distinct issue was discovered when the listener was only a few blocks horizontally from a speaker but several Y-levels below it in an open-topped hole. The existing straight speaker-to-listener occlusion rays can cross many terrain blocks and report extreme occlusion even though sound could plausibly travel around the rim.

That is tracked separately from Issue A. No diffraction/elevation correction is included in this branch.

## Phase 5 configuration and debug tools

The original Hotfix3 `ClientConfig.java` remains unchanged. Phase 5 adds:

`cchq_soundphysics_compat-advanced.toml`

Advanced behavior-changing options default to the verified Hotfix3 values and include scheduler/staleness thresholds, clearing-sentinel controls, sync timing, private EFX fallback, Beta9/Beta10/Beta11 cache/backoff controls, and targeted logging.

Existing client-only commands:

- `/cchqphysics status`
- `/cchqphysics dump`
- `/cchqphysics refresh_rooms`
- `/cchqphysics reset_caches`
- `/cchqphysics reset_efx`
- `/cchqphysics config`

Reset/refresh commands queue their work onto the existing sound-thread execution path rather than mutating OpenAL from the command callback.

## Frozen acoustic/runtime invariants

Default Phase-5 behavior preserves the verified Hotfix3 rules, including:

- no Lua-side changes;
- approved `SoundSource.BLOCKS` distance behavior;
- center + 8 inner + 8 outer progressive direct geometry;
- per-source private EFX isolation;
- direct/aux EFX reattachment on every actual environment application;
- no private EFX before PLAYING/PAUSED eligibility;
- no replacement/cancellation of SPR `calculateOcclusion()`;
- no worker-thread SPR geometry raycasts;
- strict source identity/generation lifetime semantics;
- physics scheduling does not intentionally modify PCM/OpenAL playback position/clock/offset;
- Hotfix3 synchronized-group `alSourcePlayv` and incomplete-group grace;
- Beta10 exact direct reuse / bit-identical OpenAL write suppression;
- Beta11 same-clone room-ray memo scope.

## Build-project note

Hotfix3 directly calls SPR members widened by this mod's access transformer. The build therefore keeps the untouched tested SPR artifact at runtime while producing an isolated access-transformed SPR copy for javac.

## Repository rule

Do not merge the reconstruction/Phase-5 work to `main` until the real-game Phase-5 validation is complete, the final maintained behavior is frozen, and the user explicitly approves the merge.
