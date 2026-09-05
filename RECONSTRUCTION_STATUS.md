# Beta11 Hotfix3 source reconstruction status

Authoritative runtime baseline: `cchq_soundphysics_compat-0.1.0-beta11-hotfix3.jar`

Authoritative Hotfix3 SHA-256:
`83500f182fc9829aa1a5a51fbfa11ba6cdfb645699b25d1c445167666dabc1ef`

Current investigation branch:
`phase5-sync-acoustic-diagnostics`

Current handoff addendum:
`docs/CHATGPT_HANDOFF_2026-09-05_SYNC_DIAG.md`

Original full handoff/history:
`docs/CHATGPT_HANDOFF_2026-09-05.md`

Hotfix3 remains the behavioral authority for the frozen parity baseline. Phase 5 is active and has substantial real-game runtime validation. It is **not complete** because synchronized multi-speaker coloration still needs causal resolution and the separate elevation/diffraction decision remains open.

## Canonical five-phase status

| Phase | Status |
| --- | --- |
| Phase 1 — Freeze and inventory binary baseline | **COMPLETE / JAR-RECHECKED** |
| Phase 2 — Reconstruct build project | **COMPLETE / JAR-RECHECKED** |
| Phase 3 — Reconstruct every Java class | **COMPLETE / RECHECKED** |
| Phase 4 — Structural and behavioral equivalence audit | **COMPLETE / RECHECKED / FROZEN** |
| Phase 5 — Runtime validation and source handover | **IN PROGRESS — CORE RUNTIME PASSED / REFLECTION STRONGLY WEAKENED / SYNC+ACOUSTIC DIAGNOSTIC VERIFIED / AWAITING USER TEST** |

## Frozen authorities

- `phase4-hotfix3-parity` → `79eed29767343ee34022e8f6268b386f75e84c9f`
- `archive-phase4-hotfix3-parity` → same frozen head
- final audited Phase-4 code/build commit exists: `98e7dedb7ecf6fda22008b084b6bb41956edff78`
- `phase5-test-candidate-1` → `44612192d875e43ecef66ca51798cab7adb17020`
- reviewed Issue-A runtime source `phase5-issue-a-test-candidate-2` → `973f1df7dad886fb0f5fffd4264015fecac2e786`
- V2 experiment `phase5-mix-v2-test-candidate` → `ab1e1e70a13ebb6f3dadd30581b069f06a15142a`
- rejected V1 history `phase5-final-feature-test-candidate` → `323d0e34651ae086dcd96ebe608b3149f5f0d73a`

No active experiment changes those refs.

## Phase 1–4 closure

Phase 4 established 60/60 class paths, 60/60 structural ABI, 69/69 ConstantValue parity, zero bootstrap/string-concat recipe mismatches, 11/11 Mixin/accessor semantic annotation reconciliation, 5/5 runtime resources byte-exact, and 550 audited methods with no unresolved proven Hotfix3 behavioral discrepancy.

Pinned stack remains Java 21 / Minecraft 1.21.1 / NeoForge 21.1.248 / ModDevGradle 2.0.144 / Gradle 9.2.1 / CC:Tweaked 1.120.2 / SPR 1.21.1-1.5.1 / Cloth Config 15.0.140, with runtime using untouched SPR and compile using the isolated AT-transformed copy.

## Phase 5 known-good runtime evidence

The user has already established clean startup, required Mixins, OpenAL + SPR EFX readiness, four auxiliary sends, private per-source EFX, lifecycle cleanup, movement and doorway transitions, stress playback, cache/room reset survival, synchronized audible starts, and generally correct known-good acoustics.

The known-good rollback authority remains commit `44612192d875e43ecef66ca51798cab7adb17020` with JAR SHA-256 `6d782812f7915de1870a8b5ae0f619556e7ec1d24ef2eaffa7b5b225aa00bd93`.

## Synchronized-mix history

V1 whole-source amplitude suppression is rejected because it distorted spatial weighting even when spectral balance seemed improved.

V2 is frozen but not accepted. It changes neither source gain nor source position nor reverb-send filters; it only provides a bounded direct-cutoff lift for extremely dark synchronized copies with a clear peer. V2 JAR SHA-256 remains `bba8d93e696403ae857dd155db2969c7591886aa1e8734b0b949f1a749c8319c`.

The hard-to-name coloration existed before V2 was enabled, so V2 is not assumed to be its cause.

## Issue A reflection result

Issue-A verified source:

`973f1df7dad886fb0f5fffd4264015fecac2e786`

Issue-A JAR SHA-256:

`d649f14cdce89db21a79c396dbdecca681daf3d0389dc794a7ad52929f8c8451`

The user tested a standalone source with a real 2.50-block active reflection redirect. Reflection ON/OFF did not create/remove the unwanted coloration; reflection ON improved perceived spatial direction. A synchronized reproduction snapshot also showed zero active reflected offsets across the four sources.

Conclusion for current work: reflection is strongly weakened/exonerated for the reproduced coloration. Preserve it unless new evidence contradicts this result.

## Current two hypotheses

1. **Micro-desync** — one correlated synchronized source may be slightly ahead/behind the others at the actual OpenAL playback cursor.
2. **Correlated acoustic mix** — the sources may be time-aligned but carry materially different direct filter and room/reverb-send state.

These are measured in the same test session to preserve an identical reproduction state, but conclusions remain separate. If both appear, timing is isolated first.

## Current diagnostic build

Branch:

`phase5-sync-acoustic-diagnostics`

Exact runtime-source commit:

`95bd4b06b78786d4f7b1ad33b665f4685e45a54b`

Identity:

`0.1.0-beta11-phase5-syncdiag-test`

Verification:

- workflow run `33939999239`
- job `101235407189`
- **SUCCESS**
- artifact id `9961502178`
- JAR SHA-256 `1910778a12219f84e5ad5a71449e353e99f89ef572fb599a3bc79bc568fcdb9e`
- 70 classfiles

The diagnostic retains known-good timing/acoustic behavior and adds read-only telemetry only:

- applied typed `r0..r3` / `h0..h3` values in the EFX dump;
- sound-thread `AL_SAMPLE_OFFSET` + `AL_SEC_OFFSET` cursor queries;
- attached-buffer/sample-rate information;
- ascending/descending query passes with midpoint normalization;
- per-shared-buffer cursor-spread summary.

CI proves the core sync/acoustic implementation files remain unchanged from reviewed Issue A. Neither V1 nor V2 is included.

See:

- `docs/PHASE5_SYNC_ACOUSTIC_DIAGNOSTICS.md`
- `docs/PHASE5_SYNC_ACOUSTIC_BUILD_RECORD.md`
- `docs/CHATGPT_HANDOFF_2026-09-05_SYNC_DIAG.md`

## Exact next runtime action

With the synchronized problem setup playing and the coloration audible:

1. do not move;
2. run `/cchqphysics dump`;
3. repeat twice more about 1–2 seconds apart without restarting playback;
4. return `latest.log` and state whether coloration remained audible through those snapshots.

No reflection toggle or per-source ID selection is needed.

Do not claim a synchronization or acoustic cause until those measurements are returned.

## Separate elevation / diffraction issue

The open-top-hole / vertical-separation excessive muffling remains a distinct straight-ray/diffraction limitation. Do not mix it into the current synchronized-coloration experiment.

## Remaining closure work

1. Interpret the combined timing + acoustic-send runtime snapshots.
2. Isolate the causal path without speculative acoustic changes.
3. Decide whether V2 belongs in maintained behavior or remains experimental.
4. Decide whether to address elevation/diffraction before closure or record it as a known limitation.
5. Freeze and verify the final maintained candidate.
6. Finalize runtime report, build/source handover, supported stack/hash, README/status.
7. Mark Phase 5 **COMPLETE / RECHECKED** only after those decisions are frozen.

Do not merge to `main` without explicit user approval.
