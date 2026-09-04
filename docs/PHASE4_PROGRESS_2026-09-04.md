# Phase 4 progress — 2026-09-04

Authoritative runtime baseline:

`cchq_soundphysics_compat-0.1.0-beta11-hotfix3.jar`

SHA-256:

`83500f182fc9829aa1a5a51fbfa11ba6cdfb645699b25d1c445167666dabc1ef`

Branch:

`beta11-source-reconstruction`

## Final status

- Phase 1 — **COMPLETE / JAR-RECHECKED**
- Phase 2 — **COMPLETE / JAR-RECHECKED**
- Phase 3 — **COMPLETE / RECHECKED**
- Phase 4 — **COMPLETE / RECHECKED**
- Phase 5 — **NOT STARTED**

Phase 4 remained an equivalence audit only. No Beta11.1/B optimization and no Phase 5 runtime validation was performed.

Final verification record:

`docs/PHASE4_FINAL_VERIFICATION.md`

## Final audited code/build head

`98e7dedb7ecf6fda22008b084b6bb41956edff78`

The final code/build change in Phase 4 configured the Gradle `jar` task to use the already-authoritative `src/main/resources/META-INF/MANIFEST.MF`. This eliminated the last packaging-only mismatch: Gradle had previously replaced Hotfix3's exact manifest with a generated minimal manifest that omitted `Created-By: 21.0.11 (Debian)`.

The manifest correction changed packaging only. All 60 compiled classfiles in the final export are byte-for-byte identical to those in the previously audited `ed7db4e8e9c4ca897fc7a1e399c4b5f58f9422aa` export.

## Final clean gates

All final gates ran from `98e7dedb7ecf6fda22008b084b6bb41956edff78` and completed successfully:

| Gate | Run | Job | Result |
| --- | ---: | ---: | --- |
| Phase 2 reconstruction classpath | `33924056408` | `101188553565` | **SUCCESS** |
| Phase 2 finish/build/resource | `33924056328` | `101188553502` | **SUCCESS** |
| Phase 3 source closure | `33924056330` | `101188553632` | **SUCCESS** |
| Phase 4 structural ABI | `33924056396` | `101188553422` | **SUCCESS** |
| Phase 4 structural export | `33924056370` | `101188553458` | **SUCCESS** |

## Final rebuilt evidence

Structural export:

- artifact id: `9956169844`
- artifact digest: `sha256:eabb3f3cfd54bcd113c5b3af5a018ee740dcbec0fd5167d817511e32ef5c9215`
- rebuilt JAR SHA-256: `efd8c44fec7e0446d97e8e60a99e811a4e57be1f83782a55c2fa74dc8bc09bf6`

Independent final recheck against the exact uploaded Hotfix3 JAR:

- files: **65 vs 65**;
- classfiles: **60 vs 60**;
- class path list: **60/60 exact**;
- missing/extra files: **0 / 0**;
- structural ABI: **60/60 exact**;
- compiled `ConstantValue` entries: **69/69 exact**;
- bootstrap method argument/string-concat recipe mismatches: **0**;
- configured Mixin/accessor semantic annotation audit: **11/11 reconciled**;
- non-class packaged files: **5/5 byte-for-byte exact**.

Exact non-class files:

- `META-INF/MANIFEST.MF`
- `META-INF/accesstransformer.cfg`
- `META-INF/neoforge.mods.toml`
- `assets/cchq_soundphysics_compat/lang/en_us.json`
- `cchq_soundphysics_compat.mixins.json`

## Method-body/control-flow closure

The direct Hotfix3-vs-rebuilt review covered **550 methods**.

Normalized comparison result:

- **478** instruction-equivalent methods;
- **72** raw normalized differences;
- residual differences confined to **13** classes.

Those 13 classes were individually reviewed. The residual differences are compiler/source-shape representations such as logger overload/varargs selection, saved locals versus repeated field access, equivalent branch forms, monitor/try-finally layout, constant-pool instruction width, and the verifier-safe normal-Java representation of Hotfix3's hand-patched `CompatAudioManager` operand-stack shape.

No residual raw bytecode difference remained classified as a proven semantic discrepancy.

## Mixin/annotation closure

`docs/PHASE4_MIXIN_ANNOTATION_AUDIT.md` records the configured Mixin/accessor result:

**11/11 semantic annotation sets reconciled.**

The historical Hotfix3 singleton-array annotation-container encoding remains documented as a classfile-shape artifact. Normal Java emits the legal one-element array container with the same semantic value. Phase 4 did not bytecode-patch source merely to reproduce that historical container representation.

## Proven discrepancies corrected during Phase 4

Phase 4 corrections include:

1. exact HQ receive injection descriptor/metadata, `@Coerce`, cancellable/remap behavior, and reported-hook field;
2. exact HQ stop injection descriptor/metadata/coerced parameters;
3. exact SyncStartCoordinator source-removal iteration and later source-state/play-vector source shape;
4. removal of the non-Hotfix3 Beta9 reset helper;
5. exact Beta9 registration, unknown-audibility transition, and invalid-distance semantics;
6. restoration of Beta10's reflection-based private Beta9 controller reset;
7. exact `ProgressiveOcclusionModel.State` access/constructor ABI;
8. exact EnvironmentSmoother OpenAL error textual formatting;
9. exact ClientConfigAccess reflection-failure diagnostic text;
10. exact Hotfix3 manifest packaging in the rebuilt JAR.

Each correction was followed by compile/topology/ABI revalidation as applicable.

## Frozen invariants at closure

The final Phase 4 audit found no unresolved proven conflict with the frozen Hotfix3 invariants:

- no Lua changes;
- approved `SoundSource.BLOCKS` distance behavior;
- center + 8 inner + 8 outer progressive direct geometry;
- private per-source EFX isolation;
- mandatory direct/aux EFX reattachment on actual environment applications;
- no private EFX before PLAYING/PAUSED;
- `PositionStabilizer` behavior;
- no cancellation/replacement of SPR `calculateOcclusion()`;
- no worker-thread SPR world/geometry raycasts;
- strict source lifetime identity;
- physics scheduling does not intentionally change PCM/OpenAL playback timing;
- Hotfix3 100 ms partial-sync grace and pending-INITIAL protection;
- Beta10 exact direct reuse and bit-identical OpenAL write suppression;
- Beta11 same-clone room-ray cache scope.

## Phase 4 closure

All items previously listed as "still to audit" are now closed:

1. all 11 configured Mixin/accessor annotations — **DONE**;
2. SoundPhysicsBridge scheduler/stamp/reuse/sentinel/fairness audit — **DONE**;
3. Beta9/Beta10 cache/controller/ray/write-suppression audit — **DONE**;
4. progressive occlusion/position/attenuation/EFX formula/order audit — **DONE**;
5. playback/decode/source-lifetime/sync/lifecycle audit — **DONE**;
6. config/default/range/UI reconstruction audit — **DONE**;
7. final hard build/topology/ABI gates — **PASS**;
8. final durable verification record — **CREATED**.

Therefore **Phase 4 is COMPLETE / RECHECKED**.

## Explicit stop boundary

Phase 5 is **NOT STARTED**.

No Minecraft runtime launch, real Mixin application test, OpenAL/EFX playback validation, real CC:HQ + SPR integration run, runtime sync measurement, runtime room-cache validation, or source handover was performed as part of this closure.
