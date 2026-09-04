# Phase 4 start audit — Beta11 Hotfix3 structural and behavioral equivalence

Authoritative runtime baseline:

`cchq_soundphysics_compat-0.1.0-beta11-hotfix3.jar`

SHA-256:

`83500f182fc9829aa1a5a51fbfa11ba6cdfb645699b25d1c445167666dabc1ef`

Branch:

`beta11-source-reconstruction`

## Status

**Phase 3 — RECHECKED / COMPLETE.**

**Phase 4 — IN PROGRESS.**

Phase 4 is an equivalence audit only. Do not optimize, redesign, begin Beta11.1/B, or make runtime-validation claims here.

## Phase 3 recheck before opening Phase 4

The Phase 3 hard closure gate automatically reran on the then-current fully documented branch head:

- head: `70d37a3e6b072a6e215cecf3c4299b96e0276968`
- workflow run: `33867785411`
- job: `101006475065`
- result: **SUCCESS**

Every hard Phase 3 step passed again:

- complete Java 21 source build;
- exact 60/60 Hotfix3 class-path topology reconciliation;
- source-relevant processed-resource reconciliation;
- closure summary.

Therefore Phase 3 remains closed under its source-completeness/build/topology definition. Phase 4 is intentionally stricter and is already finding lower-level structural drift that Phase 3 was never intended to prove away.

## Phase 4 whole-project structural ABI layer

A deterministic classfile structural fingerprint tool was added:

- `tools/class_abi.py`
- commit `115375d76df09dcc9ab9f468f892a294a5810192`

It fingerprints, for every compat class:

- class identity and Java major version;
- class access flags, superclass and interfaces;
- field names, descriptors, access flags and ConstantValue attributes;
- method names, descriptors and access flags.

It deliberately does not treat bytecode identity as structural equivalence. Method bodies/control flow, annotations, Mixin metadata, OpenAL ordering and behavioral invariants are audited separately in Phase 4.

The exact Hotfix3 60-class structural baseline is frozen in:

- `docs/baseline/HOTFIX3_STRUCTURAL_ABI_SHA256.txt`
- commit `7eda5a4ef95bc3cd547a5914227e304634ad0a7b`

The CI gate is:

- `.github/workflows/phase4-structural-abi.yml`
- commit `36fa51b90496bc4cac6de6fe947e4ea0bb45244b`

It performs a clean reconstruction compile, emits reconstructed structural fingerprints, and hard-diffs them against all 60 Hotfix3 fingerprints.

### Current CI infrastructure issue

The first two structural-ABI attempts did **not** reach ABI comparison:

- run `33869660406`
- run `33869841129`

Both failed during NeoForge artifact resolution because `maven.neoforged.net` returned HTTP `502 Bad Gateway` while Gradle requested:

`net.neoforged:minecraft-dependencies:1.21.1`

This is an external repository outage, not evidence of either an ABI match or mismatch. Phase 4 structural ABI remains pending until a run reaches the comparison step.

## First exact structural/Mixin discrepancies found and corrected

Phase 4 immediately found details that compile success and class-path topology cannot detect.

### 1. `HQSpeakerClientHandlerMixin`

Exact Hotfix3 classfile metadata requires:

- target `com.tom.hqspeaker.client.HQSpeakerClientHandler`, `remap=false`;
- injection method descriptor `receive(Lcom/tom/hqspeaker/network/HQSpeakerAudioPacket;)V`;
- `@At(value="HEAD", remap=false)`;
- `cancellable=true`, injection `remap=false`;
- first payload parameter annotated `@Coerce`.

The Hotfix3 class also contains:

`private static boolean cchqphysics$reportedHook;`

and the callback sets it on first entry before delegating to `CompatAudioManager.tryHandleAudioPayload(...)`.

The Phase 3 reconstruction had used only bare method name `receive` and omitted `cchqphysics$reportedHook`.

Corrected in commit:

`3a3cb6c9fdb383ea72e5b2b5dce80c7a3c926987`

### 2. `HQSpeakerStopPacketMixin`

Exact Hotfix3 injection descriptor is:

`handle(Lcom/tom/hqspeaker/network/HQSpeakerStopPacket;Lnet/neoforged/neoforge/network/handling/IPayloadContext;)V`

with `@At(value="HEAD", remap=false)`, injection `remap=false`, and both intercepted parameters annotated `@Coerce`.

The Phase 3 source used only the bare method name.

Corrected in commit:

`e9240528965c1fc0a31af22fb80a65b42720205e`

### 3. `SyncStartCoordinator.removeSource`

Hotfix3 iterates `GROUPS.values().iterator()` while removing the source from groups and deleting empty groups. The reconstructed implementation used `entrySet().iterator()`; its observable behavior was equivalent for this operation, but the source shape was unnecessarily different from the authoritative bytecode.

Aligned to Hotfix3 in commit:

`7ac821eaa2dfe73dec7703ec1ba4d7fcf9761acc`

## Exact checks already passing in the opening Phase 4 pass

These were compared against the exact Hotfix3 classfile and required no correction:

### SPR Mixin redirects

`SoundPhysicsRoomRayMemoMixin`:

- redirects inside `evaluateEnvironment`;
- exact `RaycastUtils.rayCast(BlockGetter, Vec3, Vec3, BlockPos)` descriptor;
- `remap=false`;
- `require=2`.

`SoundPhysicsOcclusionMemoMixin`:

- redirects inside `calculateOcclusion`;
- exact `SoundPhysics.runOcclusion(Vec3, Vec3)D` target;
- `remap=false`;
- `require=1`.

This confirms the reconstruction still does **not** cancel or replace SPR `calculateOcclusion()`.

`SoundPhysicsEnvironmentMixin` and `SoundPhysicsPositionMixin` retain their exact method descriptors and HEAD/cancellable/remap behavior.

The sound-engine lifecycle source retains all six Hotfix3 HEAD hooks: pause, resume, stopAll, destroy, emergencyShutdown and reload.

### `SyncStartCoordinator`

Exact constants and core ordering were rechecked:

- `PARTIAL_FLUSH_NS = 100_000_000L`;
- `STALE_GROUP_NS = 5_000_000_000L`;
- direct non-group playback uses `alSourcePlay`;
- complete and expired-partial groups use one `alSourcePlayv(int[])`;
- pending `AL_INITIAL` sources are reported as paused for lifecycle protection;
- expired partial groups start arrived sources together and are removed.

### `EnvironmentSmoother`

Exact bytecode ordering confirms:

- private EFX is not created until OpenAL state is PLAYING or PAUSED;
- each successful environment apply updates/suppresses filter parameter writes as allowed;
- every successful apply still executes auxiliary `alSource3i(... AL_AUXILIARY_SEND_FILTER ...)` attachment calls;
- every successful apply still executes direct `alSourcei(... AL_DIRECT_FILTER ...)` attachment;
- air-absorption source state is then applied and OpenAL errors checked.

Therefore the frozen mandatory-EFX-reattachment invariant remains present.

### `SoundPhysicsBridge` opening structural check

The exact Hotfix3 constants checked so far agree with reconstructed source, including:

- `ROOM_SLOT_NS = 50_000_000L`;
- hard-stale range `500_000_000L` to `2_000_000_000L`;
- recent-source window `1_000_000_000L`;
- teleport/source-move/sentinel thresholds;
- clear confirmation thresholds;
- `CLEAR_TRIGGER_COOLDOWN_NS = 300_000_000L`.

The exact static collection types also match:

- `SOUND_IDS = new ConcurrentHashMap`;
- `STATES = new LinkedHashMap`.

`available()` is exactly the unconditional true gate in Hotfix3.

## Phase 4 remaining audit order

1. Get the 60-class structural ABI CI gate through the external NeoForge dependency endpoint and reconcile every difference it reports.
2. Finish exact annotation/Mixin metadata comparison for all 11 configured mixin/accessor classes.
3. Audit `SoundPhysicsBridge` scheduler, source stamps, room reuse, sentinel transition and fairness control flow.
4. Audit `Beta9Optimizer` and `Beta10Optimizer` direct-ray/cache ownership, stamp gates, controller state and OpenAL write suppression.
5. Audit `EnvironmentSmoother`, `PositionStabilizer`, `ProgressiveOcclusionModel`, distance and reflection formulas/order.
6. Audit `CompatAudioManager`, decode/source lifetime, synchronized start and sound-engine teardown ordering.
7. Audit config defaults/ranges and `ClothConfigScreen` labels/tooltips.
8. Close Phase 4 only after all proven discrepancies are corrected and all structural/behavioral audit gates are green.

Phase 5 must not begin until Phase 4 closes.
