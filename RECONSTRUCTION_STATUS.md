# Beta11 Hotfix3 source reconstruction status

Historical authoritative runtime baseline:

`cchq_soundphysics_compat-0.1.0-beta11-hotfix3.jar`

SHA-256:

`83500f182fc9829aa1a5a51fbfa11ba6cdfb645699b25d1c445167666dabc1ef`

The reconstruction is no longer missing source or build structure. Phases 1–4 are closed, and the Phase-5 runtime core has passed. Formal Phase-5 closure is waiting only for the final user listening test of one optional post-parity mixing feature.

## Canonical five-phase status

| Phase | Status |
| --- | --- |
| Phase 1 — Freeze and inventory binary baseline | **COMPLETE / JAR-RECHECKED** |
| Phase 2 — Reconstruct build project | **COMPLETE / JAR-RECHECKED** |
| Phase 3 — Reconstruct every Java class | **COMPLETE / RECHECKED** |
| Phase 4 — Structural and behavioral equivalence audit | **COMPLETE / RECHECKED** |
| Phase 5 — Runtime validation and source handover | **RUNTIME CORE PASSED / FINAL OPTIONAL-FEATURE RETEST PENDING** |

Canonical plan: `docs/RECONSTRUCTION_PHASES.md`.

## Phase 1 — COMPLETE / JAR-RECHECKED

The exact Hotfix3 JAR was independently rechecked:

- SHA-256 matches the frozen authority;
- 75 ZIP entries;
- 65 files;
- 60 Java-21 classfiles;
- all frozen runtime resources fingerprinted.

Evidence is under `docs/baseline/` plus `docs/PHASE1_HOTFIX3_BYTECODE_AUDIT.md`.

## Phase 2 — COMPLETE / JAR-RECHECKED

Pinned build baseline:

- Java 21
- Minecraft 1.21.1
- NeoForge 21.1.248
- ModDevGradle 2.0.144
- Gradle 9.2.1
- CC:Tweaked 1.120.2
- SPR pinned Modrinth version ID `Dd2tmpsk`
- HQ Speakers pinned Modrinth project/version IDs `ygA78R8l` / `u5PEI5Ax`
- Cloth Config 15.0.140

The build keeps untouched SPR at runtime and creates an isolated access-transformed SPR copy for javac because Hotfix3 calls SPR members widened by this compat mod's access transformer.

See `docs/PHASE2_BUILD_AUDIT.md` and `docs/BUILD_FROM_SOURCE.md`.

## Phase 3 — COMPLETE / RECHECKED

All authored Java source is present and compiles. The final Phase-3 closure/recheck established exact 60-class Hotfix3 source topology before the Phase-5 support classes were intentionally added.

Evidence: `docs/PHASE3_FINAL_VERIFICATION.md`.

## Phase 4 — COMPLETE / RECHECKED

Frozen Hotfix3-equivalent source branches:

- `phase4-hotfix3-parity`
- `archive-phase4-hotfix3-parity`

Both point to:

`79eed29767343ee34022e8f6268b386f75e84c9f`

Final audited Phase-4 code/build commit:

`98e7dedb7ecf6fda22008b084b6bb41956edff78`

Final Phase-4 results include:

- 60/60 class paths exact;
- 60/60 structural ABI exact;
- 69/69 compiled constants exact;
- zero bootstrap/string-concat recipe mismatches;
- 11/11 Mixin/accessor semantic annotation sets reconciled;
- 5/5 packaged non-class resources byte-for-byte exact;
- 550-method control-flow/body audit completed;
- no unresolved proven Hotfix3 semantic discrepancy.

Final record: `docs/PHASE4_FINAL_VERIFICATION.md`.

The Phase-4 branches are historical parity anchors and must not be modified by later feature work.

## Phase 5 — RUNTIME CORE PASSED / FINAL OPTIONAL-FEATURE RETEST PENDING

### Runtime-tested extended candidate

Frozen branch:

`phase5-test-candidate-1`

Commit:

`44612192d875e43ecef66ca51798cab7adb17020`

Version:

`0.1.0-beta11-phase5-test`

Read-only verification run:

`33927205360` — **SUCCESS**

JAR SHA-256:

`6d782812f7915de1870a8b5ae0f619556e7ec1d24ef2eaffa7b5b225aa00bd93`

Real user runtime testing passed startup, Mixins/accessors, OpenAL/EFX initialization, HQ interception, 12-source stress, 2/4-source playback, movement, thick-wall occlusion, doorway transitions, clearing sentinel behavior, Beta9/Beta10/Beta11 telemetry, private EFX isolation, mandatory EFX reattachment, diagnostic reset commands, source cleanup and synchronized partial-group starts.

The user reported correct normal acoustics and synchronized starts. `/cchqphysics reset_efx` caused only a short quiet static during the explicit debug reset itself; normal playback was unaffected.

Detailed evidence: `docs/PHASE5_RUNTIME_VALIDATION.md`.

### Final optional-feature test candidate

Working branch:

`phase5-finalization`

Verified source commit:

`323d0e34651ae086dcd96ebe608b3149f5f0d73a`

Version:

`0.1.0-beta11-phase5-final-test`

Read-only verification run:

`33931215077` — **SUCCESS**

Artifact:

- name: `cchq-phase5-final-verified-build`
- ID: `9958632289`
- digest: `sha256:aa859c7bc3c7ab3671b022e3c97848465bbec2d1812ece0fe87977402c6ad2fa`

JAR SHA-256:

`8bfea798256758fa35af65b99fe4434d0c5a940f7dfbb12df5a7f7e8dcaf7d70`

The final-test build contains 66 classfiles. The post-candidate runtime addition is `MixClientConfig`, which controls optional synchronized occluded-source suppression.

The feature:

- defaults **OFF**;
- only considers sources with the same sync group and exact payload key;
- uses each source's own progressive raw occlusion;
- leaves clear sources untouched;
- applies only extra source-gain attenuation to blocked synchronized copies;
- retains a configurable blocked-source gain floor;
- does not share EFX state or choose a global master speaker.

Default tuning when enabled:

- strength `0.55`
- raw-occlusion threshold `0.075`
- minimum extra gain factor `0.30`

The read-only final verifier confirmed:

- both permanent Phase-4 refs unchanged;
- candidate-1 ref unchanged;
- original `ClientConfig.java` unchanged from Phase 4;
- all existing advanced defaults still Hotfix3-equivalent;
- new mixing feature OFF by default;
- compile-time SPR access-transform contract;
- Mixin/resource/access-transformer wiring;
- clean Java-21 compile;
- final JAR build and expected 66-class topology.

### Phase-5 handover work already completed

Completed before the final listening test:

- consolidated runtime validation report — `docs/PHASE5_RUNTIME_VALIDATION.md`;
- permanent Phase-4 archival ref;
- frozen runtime-tested candidate-1;
- one-shot Phase-5 source-patch tooling removed from finalization branch;
- source-mutating finalization workflow retired;
- read-only final verifier retained;
- runtime diagnostics retained;
- reproducible source build instructions — `docs/BUILD_FROM_SOURCE.md`;
- maintained-source handover — `docs/SOURCE_HANDOVER.md`;
- final-test JAR/hash and tested mod stack documented;
- README updated for the current maintainability state.

### Exact remaining closure gate

The user must run the final-test JAR, enable:

**Advanced Runtime → Synchronized multi-speaker mixing → Reduce occluded synchronized copies**

with the default tuning, reproduce the previously observed mixed blocked/clear synchronized-speaker scene, and verify:

- clear speakers dominate the summed mix more naturally;
- no new timing, dropout, crackle, position or EFX issue appears;
- `/cchqphysics dump` reports the feature-active per-source mixing factors.

After that successful test, only documentation/ref finalization remains:

1. change Phase 5 to **COMPLETE / RECHECKED**;
2. add the final feature-test evidence to `docs/PHASE5_RUNTIME_VALIDATION.md`;
3. freeze the final maintained-source candidate branch at the final closure head;
4. record the final branch/head and release-candidate JAR hash in README/status/handover.

No merge to `main` is authorized unless explicitly requested by the user.
