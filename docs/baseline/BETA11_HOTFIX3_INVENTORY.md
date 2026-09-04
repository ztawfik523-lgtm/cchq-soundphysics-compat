# Beta11 Hotfix3 binary inventory

Authoritative SHA-256:

`83500f182fc9829aa1a5a51fbfa11ba6cdfb645699b25d1c445167666dabc1ef`

## Top-level resources

- `META-INF/MANIFEST.MF`
- `META-INF/neoforge.mods.toml`
- `META-INF/accesstransformer.cfg`
- `cchq_soundphysics_compat.mixins.json`
- `assets/cchq_soundphysics_compat/lang/en_us.json`

## Main class

- `dev.cchqphysics.compat.CCHQSoundPhysicsCompat`

## Audio package

- `AcousticCapture`
- `AcousticCapture$Context`
- `AcousticCapture$Registration`
- `AcousticCapture$Result`
- `AttenuationBridge`
- `AudioDecoder`
- `Beta9Optimizer`
- `Beta9Optimizer$DirectEntry`
- `Beta9Optimizer$PendingDirect`
- `Beta9Optimizer$SourceMeta`
- `Beta10Optimizer`
- `Beta10Optimizer$Context`
- `Beta10Optimizer$FilterState`
- `Beta10Optimizer$SourceAlState`
- `Beta10Optimizer$StampInfo`
- `Beta11RoomRayCache`
- `Beta11RoomRayCache$CacheBank`
- `CompatAudioManager`
- `CompatAudioManager$ActiveSource`
- `CompatAudioManager$BufferRef`
- `CompatAudioManager$DecodeEntry`
- `CompatAudioManager$DecodeKey`
- `CompatAudioManager$StartRequest`
- `DecodedAudio`
- `DistanceBridge`
- `EnvironmentSmoother`
- `EnvironmentSmoother$SprLayout`
- `EnvironmentSmoother$State`
- `HQPayloadView`
- `HQPayloadView$Audio`
- `PerformanceStats`
- `PositionStabilizer`
- `PositionStabilizer$State`
- `ProgressiveOcclusionModel`
- `ProgressiveOcclusionModel$State`
- `RoomSchedulerClient`
- `SoundPhysicsBridge`
- `SoundPhysicsBridge$Candidate`
- `SoundPhysicsBridge$RoomEnvironmentAccess`
- `SoundPhysicsBridge$RoomEnvironmentAccess$ConfigStamp`
- `SoundPhysicsBridge$RoomStamp`
- `SoundPhysicsBridge$SourceState`
- `SyncStartCoordinator`
- `SyncStartCoordinator$Group`

## Mixin package

- `HQSpeakerClientHandlerMixin`
- `HQSpeakerStopPacketMixin`
- `MinecraftMixin`
- `MinecraftRoomSchedulerMixin`
- `SoundEngineAccessor`
- `SoundEngineLifecycleMixin`
- `SoundManagerAccessor`
- `SoundPhysicsEnvironmentMixin`
- `SoundPhysicsOcclusionMemoMixin`
- `SoundPhysicsPositionMixin`
- `SoundPhysicsRoomRayMemoMixin`

## Config package

- `ClientConfig`
- `ClientConfigAccess`
- `ClothConfigScreen`
- `ConfigScreenFactory`

## Counts

- 1 top-level main class
- 43 class files in `dev.cchqphysics.compat.audio` including nested classes
- 11 mixin/accessor class files
- 4 config class files
- 5 source-relevant top-level resources

This inventory is taken directly from the tested Hotfix3 JAR. The exact binary JAR remains the authority if this document and a future reconstructed source tree ever disagree.
