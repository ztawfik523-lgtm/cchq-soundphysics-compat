package dev.cchqphysics.compat.audio;

/**
 * Cross-thread request bridge for Phase-5 diagnostics.
 *
 * <p>Client commands run on the client thread, while OpenAL/SPR state belongs
 * to the sound-thread-owned scheduler. Commands only set flags here; the real
 * invalidation/reset work is consumed from {@link SoundPhysicsBridge#schedulerTick()}.</p>
 */
final class DebugControl {
    private static volatile boolean roomRefreshRequested;
    private static volatile boolean cacheResetRequested;

    private DebugControl() {}

    static void requestRoomRefresh() {
        roomRefreshRequested = true;
    }

    static void requestCacheReset() {
        cacheResetRequested = true;
    }

    static void consumeSoundThreadRequests() {
        if (cacheResetRequested) {
            cacheResetRequested = false;
            Beta9Optimizer.debugResetCaches();
            Beta10Optimizer.debugResetCaches();
            Beta11RoomRayCache.clear();
            ProgressiveOcclusionModel.debugInvalidateCaches();
            SoundPhysicsBridge.debugForceRoomRefreshNow();
            DebugDiagnostics.cache("manual cache reset consumed on sound thread");
        }
        if (roomRefreshRequested) {
            roomRefreshRequested = false;
            SoundPhysicsBridge.debugForceRoomRefreshNow();
            DebugDiagnostics.room("manual room refresh consumed on sound thread");
        }
    }

    static String compactStatus() {
        return SoundPhysicsBridge.debugSummary()
                + " | " + EnvironmentSmoother.debugSummary()
                + " | " + Beta9Optimizer.debugSummary()
                + " | " + Beta10Optimizer.debugSummary()
                + " | " + Beta11RoomRayCache.debugSummary()
                + " | " + SyncStartCoordinator.debugSummary();
    }
}
