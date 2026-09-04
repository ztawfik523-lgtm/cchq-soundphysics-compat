# Phase 4 Mixin / annotation audit — Beta11 Hotfix3

Authoritative artifact SHA-256:

`83500f182fc9829aa1a5a51fbfa11ba6cdfb645699b25d1c445167666dabc1ef`

This audit compares `javap -v -p` annotation metadata from the exact Hotfix3 classfiles with the freshly rebuilt reconstruction for all 11 configured mixin/accessor classes.

## Result

**11/11 classes match in semantic annotation content.**

The compared values include:

- class-level `@Mixin` targets/value and `remap`;
- `@Inject` / `@Redirect` method selectors;
- nested `@At` value, target and `remap`;
- `cancellable`;
- `require`;
- `@Accessor` values;
- `@Coerce` parameter annotations.

There is one compiler-encoding distinction to keep explicit: several Hotfix3 annotations encode a singleton array-valued member as the single underlying value in the classfile, while normal javac source emits a one-element array container. The semantic values are identical after canonicalizing that singleton-container representation. Phase 4 does **not** require reproducing that historical low-level annotation container encoding by bytecode patching; runtime Mixin validation remains part of Phase 5.

## Exact semantic inventory

### `HQSpeakerClientHandlerMixin`

- `@Mixin(targets="com.tom.hqspeaker.client.HQSpeakerClientHandler", remap=false)`
- `@Inject` selector: `receive(Lcom/tom/hqspeaker/network/HQSpeakerAudioPacket;)V`
- `@At(value="HEAD", remap=false)`
- `cancellable=true`
- injection `remap=false`
- intercepted payload parameter: `@Coerce`

### `HQSpeakerStopPacketMixin`

- `@Mixin(targets="com.tom.hqspeaker.network.HQSpeakerStopPacket", remap=false)`
- `@Inject` selector: `handle(Lcom/tom/hqspeaker/network/HQSpeakerStopPacket;Lnet/neoforged/neoforge/network/handling/IPayloadContext;)V`
- `@At(value="HEAD", remap=false)`
- injection `remap=false`
- both intercepted HQ/context parameters: `@Coerce`

### `MinecraftMixin`

- `@Mixin(Minecraft.class)`
- `@Inject(method="tick", at=@At("TAIL"))`

### `MinecraftRoomSchedulerMixin`

- `@Mixin(Minecraft.class)`
- `@Inject(method="tick", at=@At("TAIL"))`

### `SoundEngineAccessor`

- `@Mixin(SoundEngine.class)`
- `@Accessor("executor")`

### `SoundEngineLifecycleMixin`

- `@Mixin(SoundEngine.class)`
- six HEAD injections:
  - `pause`
  - `resume`
  - `stopAll`
  - `destroy`
  - `emergencyShutdown`
  - `reload`

### `SoundManagerAccessor`

- `@Mixin(SoundManager.class)`
- `@Accessor("soundEngine")`

### `SoundPhysicsEnvironmentMixin`

- `@Mixin(targets="com.sonicether.soundphysics.SoundPhysics", remap=false)`
- selector: `setEnvironment(IFFFFFFFFFF)V`
- `@At("HEAD")`
- `cancellable=true`
- injection `remap=false`

### `SoundPhysicsOcclusionMemoMixin`

- `@Mixin(targets="com.sonicether.soundphysics.SoundPhysics", remap=false)`
- `@Redirect(method="calculateOcclusion", ...)`
- target: `Lcom/sonicether/soundphysics/SoundPhysics;runOcclusion(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;)D`
- nested `@At(value="INVOKE", remap=false)`
- redirect `remap=false`
- `require=1`

This preserves the frozen rule that the compat does **not** cancel or replace SPR `calculateOcclusion()`.

### `SoundPhysicsPositionMixin`

- `@Mixin(targets="com.sonicether.soundphysics.SoundPhysics", remap=false)`
- selector: `setSoundPos(ILnet/minecraft/world/phys/Vec3;)V`
- `@At("HEAD")`
- `cancellable=true`
- injection `remap=false`

### `SoundPhysicsRoomRayMemoMixin`

- `@Mixin(targets="com.sonicether.soundphysics.SoundPhysics", remap=false)`
- `@Redirect(method="evaluateEnvironment", ...)`
- target: `Lcom/sonicether/soundphysics/utils/RaycastUtils;rayCast(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/BlockHitResult;`
- nested `@At(value="INVOKE", remap=false)`
- redirect `remap=false`
- `require=2`

## Corrections that preceded this 11/11 pass

The semantic match depends on earlier Phase 4 fixes:

- `3a3cb6c9fdb383ea72e5b2b5dce80c7a3c926987` — exact HQ audio receive injection metadata and `cchqphysics$reportedHook`;
- `e9240528965c1fc0a31af22fb80a65b42720205e` — exact HQ stop injection descriptor/metadata.

## Phase 4 implication

The Mixin/accessor annotation layer is now considered **semantically reconciled** for all 11 configured classes. The audit now moves deeper into method-body/control-flow equivalence for the runtime audio, scheduler, cache and EFX classes.

Phase 5 must still verify that the reconstructed Mixin set applies successfully in the real runtime environment.
