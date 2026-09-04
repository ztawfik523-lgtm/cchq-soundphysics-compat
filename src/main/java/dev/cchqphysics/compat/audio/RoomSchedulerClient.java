package dev.cchqphysics.compat.audio;

import java.util.concurrent.atomic.AtomicBoolean;

public final class RoomSchedulerClient {
    private static final AtomicBoolean QUEUED = new AtomicBoolean();

    private RoomSchedulerClient() {
    }

    public static void clientTick() {
        long queuedAt = System.nanoTime();
        if (!QUEUED.compareAndSet(false, true)) {
            PerformanceStats.recordSchedulerCoalesced();
            return;
        }

        try {
            CompatAudioManager.beta10OnSoundThread(() -> {
                try {
                    PerformanceStats.recordSchedulerQueue(System.nanoTime() - queuedAt);
                    SoundPhysicsBridge.schedulerTick();
                } finally {
                    QUEUED.set(false);
                }
            });
        } catch (Throwable ignored) {
            QUEUED.set(false);
        }
    }

    public static void reset() {
        QUEUED.set(false);
    }
}
