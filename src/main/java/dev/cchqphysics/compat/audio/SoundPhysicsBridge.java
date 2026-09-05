package dev.cchqphysics.compat.audio;

import com.sonicether.soundphysics.SoundPhysics;
import dev.cchqphysics.compat.config.ClientConfig;
import dev.cchqphysics.compat.config.ExtendedClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.openal.AL10;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Sound-thread-owned SPR room scheduler and exact room-state reuse bridge. */
final class SoundPhysicsBridge {
    private static final ConcurrentHashMap<UUID, ResourceLocation> SOUND_IDS = new ConcurrentHashMap<>();
    private static final Map<Integer, SourceState> STATES = new LinkedHashMap<>();

    private static final int AL_SOURCE_STATE = 4112;
    private static final int AL_PLAYING = 4114;
    private static final int AL_PAUSED = 4115;
    private static final int AL_MAX_DISTANCE = 4131;

    // Scheduler and fast-clearing thresholds are exposed through the advanced
    // client configuration. Release defaults are the tuned runtime values.

    private static int rrCursor;
    private static boolean haveSchedulerListener;
    private static double schedulerListenerX;
    private static double schedulerListenerY;
    private static double schedulerListenerZ;

    private SoundPhysicsBridge() {}

    static boolean available() {
        return true;
    }

    static synchronized void registerSource(int sourceId) {
        AcousticCapture.register(sourceId);
        long generation = AcousticCapture.currentGeneration(sourceId);
        STATES.put(sourceId, new SourceState(sourceId, generation));
        if (rrCursor >= STATES.size()) rrCursor = 0;
        DebugDiagnostics.source("register source={} generation={} tracked={}", sourceId, generation, STATES.size());
    }

    static synchronized void unregisterSource(int sourceId) {
        STATES.remove(sourceId);
        AcousticCapture.unregister(sourceId);
        VerticalDiffractionRelief.unregister(sourceId);
        if (rrCursor >= Math.max(1, STATES.size())) rrCursor = 0;
        DebugDiagnostics.source("unregister source={} tracked={}", sourceId, STATES.size());
    }

    static void apply(int sourceId, UUID uuid, double x, double y, double z) {
        VerticalDiffractionRelief.updateSource(sourceId, x, y, z);
        ProgressiveOcclusionModel.updateSource(sourceId, x, y, z);
        AcousticCapture.bindIdentity(sourceId, uuid);
        long now = System.nanoTime();

        final SourceState state;
        synchronized (SoundPhysicsBridge.class) {
            state = STATES.get(sourceId);
            if (state == null) return;
            if (state.uuid == null) state.uuid = uuid;
            if (!uuid.equals(state.uuid) || state.generation != AcousticCapture.currentGeneration(sourceId)) return;

            if (state.hasPosition && distanceSq(x, y, z, state.x, state.y, state.z) >= ExtendedClientConfig.sourceMoveUrgentSq()) {
                state.urgent = true;
                state.roomStamp = null;
                DebugDiagnostics.room("source={} movement marked room urgent", sourceId);
            }
            state.x = x;
            state.y = y;
            state.z = z;
            state.hasPosition = true;
            state.lastSeenNs = now;
            state.inRange = insidePhysicsRange(x, y, z);
        }

        if (!state.inRange) {
            synchronized (SoundPhysicsBridge.class) {
                state.playing = false;
            }
            PositionStabilizer.releaseToOriginal(sourceId, x, y, z);
            return;
        }

        final int alState;
        try {
            alState = AL10.alGetSourcei(sourceId, AL_SOURCE_STATE);
        } catch (Throwable throwable) {
            runImmediateReference(sourceId, uuid, x, y, z);
            return;
        }

        boolean playing = alState == AL_PLAYING || alState == AL_PAUSED;
        synchronized (SoundPhysicsBridge.class) {
            state.playing = playing;
        }
        if (!playing) {
            runImmediateReference(sourceId, uuid, x, y, z);
            return;
        }

        float baseCutoff = 1.0F;
        float baseGain = 1.0F;
        synchronized (SoundPhysicsBridge.class) {
            AcousticCapture.Result room = state.room;
            if (room != null) {
                baseCutoff = room.directCutoff();
                baseGain = room.directGain();
            }
        }

        float[] direct = ProgressiveOcclusionModel.independentDirectActive()
                ? ProgressiveOcclusionModel.adjust(sourceId, baseCutoff, baseGain)
                : new float[]{baseCutoff, baseGain};
        synchronized (SoundPhysicsBridge.class) {
            state.directCutoff = direct[0];
            state.directGain = direct[1];
        }

        long applyInterval = Math.max(ExtendedClientConfig.roomSlotNs(), ClientConfig.fullSprIntervalNs());
        final boolean due;
        synchronized (SoundPhysicsBridge.class) {
            due = now - state.lastApplyNs >= applyInterval;
            if (due) state.lastApplyNs = now;
        }
        if (!due) {
            PositionStabilizer.reapply(sourceId, x, y, z);
            return;
        }
        applyStoredState(state);
    }

    static void schedulerTick() {
        DebugControl.consumeSoundThreadRequests();
        long now = System.nanoTime();
        boolean independent = ProgressiveOcclusionModel.independentDirectActive();
        Vec3 camera = currentCameraPosition();
        double movementSq = updateSchedulerListener(camera);
        Beta9Optimizer.onListenerMovement(movementSq);

        if (!independent) {
            List<SourceState> due = new ArrayList<>();
            synchronized (SoundPhysicsBridge.class) {
                long interval = Math.max(ExtendedClientConfig.roomSlotNs(), ClientConfig.fullSprIntervalNs());
                for (SourceState state : STATES.values()) {
                    if (eligible(state, now)
                            && (state.room == null || now - state.lastRoomNs >= interval)) {
                        due.add(state);
                    }
                }
            }
            for (SourceState state : due) runScheduledRoom(state, now);
            return;
        }

        if (camera != null && movementSq >= ExtendedClientConfig.sentinelMoveSq() && movementSq < ExtendedClientConfig.teleportDistanceSq()) {
            runClearingSentinel(now, camera);
        }

        SourceState selected = selectBalancedRoomSource(now);
        if (selected != null) runScheduledRoom(selected, now);
    }

    private static void runClearingSentinel(long now, Vec3 camera) {
        List<SourceState> eligible = new ArrayList<>();
        synchronized (SoundPhysicsBridge.class) {
            for (SourceState state : STATES.values()) {
                if (eligible(state, now)) eligible.add(state);
            }
        }

        for (SourceState state : eligible) {
            double oldRaw = ProgressiveOcclusionModel.currentRawOcclusion(state.sourceId);
            double oldCenter = ProgressiveOcclusionModel.currentCenterOcclusion(state.sourceId);

            final boolean armed;
            final long lastTrigger;
            double previousCenter;
            synchronized (SoundPhysicsBridge.class) {
                SourceState current = STATES.get(state.sourceId);
                if (current != state) continue;
                if (now - state.lastClearTriggerNs >= ExtendedClientConfig.clearTriggerCooldownNs()
                        && (oldRaw >= ExtendedClientConfig.sentinelRawOccluded()
                        || (!Double.isNaN(oldCenter) && oldCenter >= ExtendedClientConfig.sentinelRearmCenter()))) {
                    state.sentinelArmed = true;
                }
                armed = state.sentinelArmed;
                lastTrigger = state.lastClearTriggerNs;
                previousCenter = state.hasSentinelCenter ? state.sentinelCenter : oldCenter;
            }

            if (!armed && oldRaw < ExtendedClientConfig.sentinelRawOccluded()) continue;
            if (now - lastTrigger < ExtendedClientConfig.clearTriggerCooldownNs()) continue;

            final double center;
            try {
                center = ProgressiveOcclusionModel.sampleCenterSentinel(state.sourceId, camera);
            } catch (Throwable throwable) {
                CompatAudioManager.logOnce("beta9-sentinel", String.valueOf(throwable));
                continue;
            }
            if (Double.isNaN(center)) continue;
            if (Double.isNaN(previousCenter)) previousCenter = center;

            boolean candidate = armed && (previousCenter - center >= ExtendedClientConfig.sentinelCenterDrop()
                    || (previousCenter >= ExtendedClientConfig.sentinelRearmCenter() && center <= ExtendedClientConfig.sentinelOpenCenter()));

            synchronized (SoundPhysicsBridge.class) {
                SourceState current = STATES.get(state.sourceId);
                if (current != state) continue;
                state.sentinelCenter = center;
                state.hasSentinelCenter = true;
                if (center >= ExtendedClientConfig.sentinelRearmCenter()
                        && now - state.lastClearTriggerNs >= ExtendedClientConfig.clearTriggerCooldownNs()) {
                    state.sentinelArmed = true;
                }
            }
            if (!candidate) continue;

            float baseCutoff = 1.0F;
            float baseGain = 1.0F;
            final float priorCutoff;
            synchronized (SoundPhysicsBridge.class) {
                AcousticCapture.Result room = state.room;
                if (room != null) {
                    baseCutoff = room.directCutoff();
                    baseGain = room.directGain();
                }
                priorCutoff = state.directCutoff;
            }

            float[] direct = ProgressiveOcclusionModel.forceAdjust(state.sourceId, baseCutoff, baseGain);
            double newRaw = ProgressiveOcclusionModel.currentRawOcclusion(state.sourceId);
            boolean confirmed = oldRaw - newRaw >= ExtendedClientConfig.confirmRawDrop()
                    || direct[0] - priorCutoff >= ExtendedClientConfig.confirmCutoffRise()
                    || (oldRaw >= ExtendedClientConfig.sentinelRawOccluded() && newRaw <= ExtendedClientConfig.sentinelOpenCenter());
            PerformanceStats.recordSentinelCandidate(confirmed);
            DebugDiagnostics.sentinel("source={} oldRaw={} newRaw={} previousCenter={} center={} priorCutoff={} newCutoff={} confirmed={}",
                    state.sourceId, oldRaw, newRaw, previousCenter, center, priorCutoff, direct[0], confirmed);

            boolean immediate = false;
            synchronized (SoundPhysicsBridge.class) {
                SourceState current = STATES.get(state.sourceId);
                if (current != state || state.generation != AcousticCapture.currentGeneration(state.sourceId)) continue;
                state.directCutoff = direct[0];
                state.directGain = direct[1];
                if (confirmed) {
                    state.urgent = true;
                    state.roomStamp = null;
                    state.lastClearTriggerNs = now;
                    state.transitionDetectedNs = now;
                    state.applyImmediatelyAfterRoom = true;
                    state.sentinelArmed = false;
                    state.lastApplyNs = now;
                    immediate = true;
                } else if (center <= ExtendedClientConfig.sentinelOpenCenter()) {
                    state.sentinelArmed = false;
                }
            }

            if (immediate) {
                applyStoredState(state);
                PerformanceStats.recordImmediateDirectApply();
            }
        }
    }

    private static synchronized SourceState selectBalancedRoomSource(long now) {
        List<SourceState> eligible = new ArrayList<>();
        for (SourceState state : STATES.values()) {
            if (eligible(state, now)) eligible.add(state);
        }
        int count = eligible.size();
        if (count == 0) return null;
        if (rrCursor >= count) rrCursor = 0;

        long baseInterval = Math.max(ExtendedClientConfig.roomSlotNs(), ClientConfig.fullSprIntervalNs());
        long fairShare = Math.max(baseInterval, ExtendedClientConfig.roomSlotNs() * count);
        long hardStale = Math.max(ExtendedClientConfig.minHardStaleNs(), fairShare * 2L);
        hardStale = Math.min(ExtendedClientConfig.maxHardStaleNs(), hardStale);

        int index = findCandidate(eligible, now, hardStale, Candidate.STALE);
        if (index < 0) index = findCandidate(eligible, now, fairShare, Candidate.URGENT);
        if (index < 0) index = findCandidate(eligible, now, fairShare, Candidate.DUE);
        if (index < 0) return null;

        SourceState selected = eligible.get(index);
        rrCursor = (index + 1) % count;
        return selected;
    }

    private static int findCandidate(List<SourceState> states, long now, long threshold, Candidate candidate) {
        int count = states.size();
        for (int offset = 0; offset < count; offset++) {
            int index = (rrCursor + offset) % count;
            SourceState state = states.get(index);
            long age = state.room == null ? Long.MAX_VALUE : Math.max(0L, now - state.lastRoomNs);
            boolean matches = switch (candidate) {
                case STALE -> state.room == null || age >= threshold;
                case URGENT -> state.urgent && age >= ExtendedClientConfig.roomSlotNs();
                case DUE -> age >= Beta9Optimizer.roomInterval(state.sourceId, threshold, state.room);
            };
            if (matches) return index;
        }
        return -1;
    }

    private static void runScheduledRoom(SourceState state, long now) {
        synchronized (SoundPhysicsBridge.class) {
            SourceState current = STATES.get(state.sourceId);
            if (current != state
                    || state.generation != AcousticCapture.currentGeneration(state.sourceId)
                    || state.uuid == null
                    || !state.playing
                    || !state.inRange) return;
        }

        try {
            int alState = AL10.alGetSourcei(state.sourceId, AL_SOURCE_STATE);
            if (alState != AL_PLAYING && alState != AL_PAUSED) return;
        } catch (Throwable throwable) {
            return;
        }

        RoomStamp startStamp = RoomEnvironmentAccess.capture(state.sourceId);
        boolean reuse = false;
        synchronized (SoundPhysicsBridge.class) {
            SourceState current = STATES.get(state.sourceId);
            if (current != state) return;
            if (!state.urgent
                    && state.room != null
                    && state.roomStamp != null
                    && startStamp.reusable
                    && state.roomStamp.sameInputs(startStamp)
                    && sameDouble(state.roomSourceX, state.x)
                    && sameDouble(state.roomSourceY, state.y)
                    && sameDouble(state.roomSourceZ, state.z)) {
                state.lastRoomNs = now;
                reuse = true;
            }
        }
        if (reuse) {
            PerformanceStats.recordRoomReuse();
            DebugDiagnostics.room("source={} reused room stamp", state.sourceId);
            return;
        }

        ResourceLocation id = soundId(state.uuid);
        boolean capturing = AcousticCapture.begin(state.sourceId, state.uuid);
        if (!capturing) return;

        long started = System.nanoTime();
        final Vec3 reflected;
        try {
            reflected = Beta10Optimizer.processSound(
                    state.sourceId, state.x, state.y, state.z, SoundSource.BLOCKS, id);
        } catch (Throwable throwable) {
            PerformanceStats.recordSpr(System.nanoTime() - started);
            AcousticCapture.end(state.sourceId, state.uuid);
            CompatAudioManager.logOnce("spr-scheduled", String.valueOf(throwable));
            return;
        }
        PerformanceStats.recordSpr(System.nanoTime() - started);

        AcousticCapture.Result captured = AcousticCapture.end(state.sourceId, state.uuid);
        if (captured == null || !captured.environmentCaptured()) {
            CompatAudioManager.logOnce("beta9-capture-miss",
                    "Scheduled SPR room capture completed without a setEnvironment write; keeping prior target");
            return;
        }

        RoomStamp endStamp = RoomEnvironmentAccess.capture(state.sourceId);
        boolean stableStamp = startStamp.reusable && endStamp.reusable && startStamp.sameInputs(endStamp);

        final boolean immediate;
        final long transitionDetected;
        synchronized (SoundPhysicsBridge.class) {
            SourceState current = STATES.get(state.sourceId);
            if (current != state || state.generation != AcousticCapture.currentGeneration(state.sourceId)) return;

            state.room = captured;
            Beta9Optimizer.observeRoom(state.sourceId, captured);
            state.reflected = reflected;
            state.lastRoomNs = now;
            state.urgent = false;
            if (stableStamp) {
                state.roomStamp = endStamp;
                state.roomSourceX = state.x;
                state.roomSourceY = state.y;
                state.roomSourceZ = state.z;
            } else {
                state.roomStamp = null;
            }

            immediate = state.applyImmediatelyAfterRoom;
            transitionDetected = state.transitionDetectedNs;
            if (immediate) {
                state.applyImmediatelyAfterRoom = false;
                state.lastApplyNs = System.nanoTime();
            }
        }

        PerformanceStats.recordRoomRefresh();
        DebugDiagnostics.room("source={} refreshed room stableStamp={} immediate={} urgentCleared=true", state.sourceId, stableStamp, immediate);
        if (immediate) {
            applyStoredState(state);
            PerformanceStats.recordImmediateRoomApply();
            if (transitionDetected > 0L) {
                long latency = System.nanoTime() - transitionDetected;
                PerformanceStats.recordTransitionLatency(latency);
                DebugDiagnostics.transition("source={} clearing transition room-applied latencyMs={}", state.sourceId, latency / 1_000_000.0D);
            }
        }
    }

    private static void applyStoredState(SourceState state) {
        Beta9Optimizer.beginEfxTimer();

        final AcousticCapture.Result room;
        final Vec3 reflected;
        final float directCutoff;
        final float directGain;
        final long lastRoomNs;
        final long lastAppliedNs;
        synchronized (SoundPhysicsBridge.class) {
            room = state.room;
            reflected = state.reflected;
            directCutoff = state.directCutoff;
            directGain = state.directGain;
            lastRoomNs = state.lastRoomNs;
            lastAppliedNs = state.lastAppliedNs;
        }

        long now = System.nanoTime();
        long directEvalNs = ProgressiveOcclusionModel.lastEvaluationNs(state.sourceId);
        PerformanceStats.recordTargetAges(
                directEvalNs > 0L ? now - directEvalNs : 0L,
                lastRoomNs > 0L ? now - lastRoomNs : 0L,
                lastAppliedNs > 0L ? now - lastAppliedNs : 0L);

        if (room != null) {
            boolean override = ProgressiveOcclusionModel.beginApplyOverride(state.sourceId, directCutoff, directGain);
            try {
                applyCapturedEnvironment(state.sourceId, room);
            } finally {
                if (override) ProgressiveOcclusionModel.endApplyOverride(state.sourceId);
            }
        }

        try {
            double raw = ProgressiveOcclusionModel.currentRawOcclusion(state.sourceId);
            PositionStabilizer.updateAndApply(state.sourceId, state.x, state.y, state.z, reflected, raw);
        } catch (Throwable throwable) {
            CompatAudioManager.logOnce("spr-position", String.valueOf(throwable));
            PositionStabilizer.reapply(state.sourceId, state.x, state.y, state.z);
        }

        synchronized (SoundPhysicsBridge.class) {
            if (STATES.get(state.sourceId) == state) state.lastAppliedNs = now;
        }
        PerformanceStats.recordApplyPass();
        Beta9Optimizer.endEfxTimer();
    }

    private static void runImmediateReference(int sourceId, UUID uuid, double x, double y, double z) {
        ResourceLocation id = soundId(uuid);
        long started = System.nanoTime();
        final Vec3 reflected;
        try {
            reflected = Beta10Optimizer.processSound(sourceId, x, y, z, SoundSource.BLOCKS, id);
        } catch (Throwable throwable) {
            PerformanceStats.recordSpr(System.nanoTime() - started);
            CompatAudioManager.logOnce("spr-invoke", String.valueOf(throwable));
            PositionStabilizer.reapply(sourceId, x, y, z);
            return;
        }
        PerformanceStats.recordSpr(System.nanoTime() - started);

        try {
            double raw = ProgressiveOcclusionModel.currentRawOcclusion(sourceId);
            PositionStabilizer.updateAndApply(sourceId, x, y, z, reflected, raw);
        } catch (Throwable throwable) {
            PositionStabilizer.reapply(sourceId, x, y, z);
        }
    }

    private static void applyCapturedEnvironment(int sourceId, AcousticCapture.Result result) {
        boolean handled = false;
        try {
            handled = EnvironmentSmoother.intercept(sourceId,
                    result.r0(), result.r1(), result.r2(), result.r3(),
                    result.h0(), result.h1(), result.h2(), result.h3(),
                    result.directCutoff(), result.directGain());
        } catch (Throwable throwable) {
            CompatAudioManager.logOnce("beta9-captured-efx", String.valueOf(throwable));
        }
        if (handled) return;

        boolean fallback = AcousticCapture.beginNativeEnvironmentFallback(sourceId);
        try {
            SoundPhysics.setEnvironment(sourceId,
                    result.r0(), result.r1(), result.r2(), result.r3(),
                    result.h0(), result.h1(), result.h2(), result.h3(),
                    result.directCutoff(), result.directGain());
        } catch (Throwable throwable) {
            CompatAudioManager.logOnce("beta9-native-efx-fallback", String.valueOf(throwable));
        } finally {
            if (fallback) AcousticCapture.endNativeEnvironmentFallback(sourceId);
        }
    }

    static synchronized void clearSourceIds() {
        SOUND_IDS.clear();
        STATES.clear();
        rrCursor = 0;
        haveSchedulerListener = false;
        PositionStabilizer.clear();
        VerticalDiffractionRelief.clear();
        AcousticCapture.clear();
        PerformanceStats.reset();
        RoomSchedulerClient.reset();
        RoomEnvironmentAccess.reset();
        DebugDiagnostics.source("cleared all compat source ids/state");
    }

    static synchronized void debugForceRoomRefreshNow() {
        for (SourceState state : STATES.values()) {
            if (state.playing && state.inRange) {
                state.urgent = true;
                state.roomStamp = null;
                state.lastRoomNs = 0L;
            }
        }
    }

    static synchronized void debugDumpSources() {
        long now = System.nanoTime();
        for (SourceState state : STATES.values()) {
            double raw = ProgressiveOcclusionModel.currentRawOcclusion(state.sourceId);
            double center = ProgressiveOcclusionModel.currentCenterOcclusion(state.sourceId);
            long roomAgeMs = state.lastRoomNs == 0L ? -1L : Math.max(0L, now - state.lastRoomNs) / 1_000_000L;
            long seenAgeMs = state.lastSeenNs == 0L ? -1L : Math.max(0L, now - state.lastSeenNs) / 1_000_000L;
            beta9Log("[dump/source] source=" + state.sourceId
                    + " generation=" + state.generation
                    + " uuid=" + state.uuid
                    + " playing=" + state.playing
                    + " inRange=" + state.inRange
                    + " urgent=" + state.urgent
                    + " room=" + (state.room != null)
                    + " roomAgeMs=" + roomAgeMs
                    + " seenAgeMs=" + seenAgeMs
                    + " raw=" + raw
                    + " center=" + center
                    + " directCutoff=" + state.directCutoff
                    + " directGain=" + state.directGain
                    + " pos=" + state.x + "," + state.y + "," + state.z);
        }
    }

    static synchronized String debugSummary() {
        int playing = 0;
        int inRange = 0;
        int urgent = 0;
        int withRoom = 0;
        for (SourceState state : STATES.values()) {
            if (state.playing) playing++;
            if (state.inRange) inRange++;
            if (state.urgent) urgent++;
            if (state.room != null) withRoom++;
        }
        return "sources=" + STATES.size() + " playing=" + playing + " inRange=" + inRange
                + " urgent=" + urgent + " rooms=" + withRoom;
    }

    private static synchronized double updateSchedulerListener(Vec3 camera) {
        if (camera == null) return 0.0D;
        double movementSq = haveSchedulerListener
                ? distanceSq(camera.x, camera.y, camera.z,
                schedulerListenerX, schedulerListenerY, schedulerListenerZ)
                : 0.0D;

        if (haveSchedulerListener && movementSq >= ExtendedClientConfig.teleportDistanceSq()) {
            for (SourceState state : STATES.values()) {
                if (state.playing && state.inRange) {
                    state.urgent = true;
                    state.roomStamp = null;
                    DebugDiagnostics.room("source={} listener teleport forced room urgent", state.sourceId);
                }
            }
        }

        schedulerListenerX = camera.x;
        schedulerListenerY = camera.y;
        schedulerListenerZ = camera.z;
        haveSchedulerListener = true;
        return movementSq;
    }

    private static Vec3 currentCameraPosition() {
        try {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft == null || minecraft.gameRenderer == null
                    || minecraft.gameRenderer.getMainCamera() == null) return null;
            return minecraft.gameRenderer.getMainCamera().getPosition();
        } catch (Throwable throwable) {
            return null;
        }
    }

    private static boolean beta9EligibleReal(SourceState state, long now) {
        return state.uuid != null
                && state.hasPosition
                && state.playing
                && state.inRange
                && now - state.lastSeenNs <= ExtendedClientConfig.recentSourceNs()
                && state.generation == AcousticCapture.currentGeneration(state.sourceId);
    }

    private static ResourceLocation soundId(UUID uuid) {
        return SOUND_IDS.computeIfAbsent(uuid, value -> ResourceLocation.fromNamespaceAndPath(
                "cchq_soundphysics_compat",
                "hq_speaker/" + value.toString().replace("-", "")));
    }

    private static boolean insidePhysicsRange(double x, double y, double z) {
        try {
            Vec3 camera = currentCameraPosition();
            if (camera == null) return true;
            double max = AttenuationBridge.physicsMaxDistance();
            return distanceSq(camera.x, camera.y, camera.z, x, y, z) <= max * max;
        } catch (Throwable throwable) {
            return true;
        }
    }

    private static boolean sameDouble(double a, double b) {
        return Double.doubleToLongBits(a) == Double.doubleToLongBits(b);
    }

    private static double distanceSq(double ax, double ay, double az,
                                     double bx, double by, double bz) {
        double dx = ax - bx;
        double dy = ay - by;
        double dz = az - bz;
        return dx * dx + dy * dy + dz * dz;
    }

    static Object beta9CaptureStamp(int sourceId) {
        return RoomEnvironmentAccess.capture(sourceId);
    }

    static boolean beta9SameStamp(Object first, Object second) {
        return ((RoomStamp) first).sameInputs((RoomStamp) second);
    }

    static void beta9Log(String message) {
        LoggerFactory.getLogger("CC:HQ Sound Physics Compat").info(message);
    }

    private static boolean eligible(SourceState state, long now) {
        return Beta9Optimizer.isAudibleAndRecord(state.sourceId) & beta9EligibleReal(state, now);
    }

    private enum Candidate {
        STALE,
        URGENT,
        DUE
    }

    private static final class SourceState {
        final int sourceId;
        final long generation;
        UUID uuid;
        double x;
        double y;
        double z;
        boolean hasPosition;
        boolean playing;
        boolean inRange;
        boolean urgent = true;
        long lastSeenNs;
        long lastRoomNs;
        long lastApplyNs;
        long lastAppliedNs;
        AcousticCapture.Result room;
        Vec3 reflected;
        float directCutoff = 1.0F;
        float directGain = 1.0F;
        RoomStamp roomStamp;
        double roomSourceX;
        double roomSourceY;
        double roomSourceZ;
        boolean hasSentinelCenter;
        double sentinelCenter;
        boolean sentinelArmed;
        long lastClearTriggerNs;
        long transitionDetectedNs;
        boolean applyImmediatelyAfterRoom;

        SourceState(int sourceId, long generation) {
            this.sourceId = sourceId;
            this.generation = generation;
        }
    }

    private static final class RoomStamp {
        final boolean reusable;
        final Object cloneIdentity;
        final long cloneTick;
        final long configFingerprint;
        final double cameraX;
        final double cameraY;
        final double cameraZ;
        final double playerX;
        final double playerY;
        final double playerZ;
        final boolean playerUnderwater;
        final float alMaxDistance;

        private RoomStamp(boolean reusable, Object cloneIdentity, long cloneTick, long configFingerprint,
                          double cameraX, double cameraY, double cameraZ,
                          double playerX, double playerY, double playerZ,
                          boolean playerUnderwater, float alMaxDistance) {
            this.reusable = reusable;
            this.cloneIdentity = cloneIdentity;
            this.cloneTick = cloneTick;
            this.configFingerprint = configFingerprint;
            this.cameraX = cameraX;
            this.cameraY = cameraY;
            this.cameraZ = cameraZ;
            this.playerX = playerX;
            this.playerY = playerY;
            this.playerZ = playerZ;
            this.playerUnderwater = playerUnderwater;
            this.alMaxDistance = alMaxDistance;
        }

        static RoomStamp invalid() {
            return new RoomStamp(false, null, Long.MIN_VALUE, 0L,
                    0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, false, 0.0F);
        }

        boolean sameInputs(RoomStamp other) {
            if (other == null || !reusable || !other.reusable) return false;
            return cloneIdentity == other.cloneIdentity
                    && cloneTick == other.cloneTick
                    && configFingerprint == other.configFingerprint
                    && sameDouble(cameraX, other.cameraX)
                    && sameDouble(cameraY, other.cameraY)
                    && sameDouble(cameraZ, other.cameraZ)
                    && sameDouble(playerX, other.playerX)
                    && sameDouble(playerY, other.playerY)
                    && sameDouble(playerZ, other.playerZ)
                    && playerUnderwater == other.playerUnderwater
                    && Float.floatToIntBits(alMaxDistance) == Float.floatToIntBits(other.alMaxDistance);
        }
    }

    private static final class RoomEnvironmentAccess {
        private static Field mcLevelField;
        private static Field mcPlayerField;
        private static Method levelGetCloneMethod;
        private static Class<?> levelGetCloneOwner;
        private static Method cloneGetTickMethod;
        private static Class<?> cloneTickOwner;
        private static Method playerPositionMethod;
        private static Method playerUnderwaterMethod;
        private static Class<?> playerOwner;
        private static Field sprConfigField;
        private static Field sprReflectivityField;
        private static Field sprOcclusionField;
        private static Field sprSoundRateField;
        private static Field[] sprConfigEntries;
        private static Method reflectivityDefinitionsMethod;
        private static Method occlusionDefinitionsMethod;
        private static Method soundRateDefinitionsMethod;
        private static Class<?> reflectivityOwner;
        private static Class<?> occlusionOwner;
        private static Class<?> soundRateOwner;
        private static long lastConfigHashNs;
        private static long cachedConfigHash;
        private static boolean cachedUnsafe;
        private static boolean failureLogged;

        private RoomEnvironmentAccess() {}

        static synchronized void reset() {
            mcLevelField = mcPlayerField = null;
            levelGetCloneMethod = cloneGetTickMethod = null;
            levelGetCloneOwner = cloneTickOwner = null;
            playerPositionMethod = playerUnderwaterMethod = null;
            playerOwner = null;
            sprConfigField = sprReflectivityField = sprOcclusionField = sprSoundRateField = null;
            sprConfigEntries = null;
            reflectivityDefinitionsMethod = occlusionDefinitionsMethod = soundRateDefinitionsMethod = null;
            reflectivityOwner = occlusionOwner = soundRateOwner = null;
            lastConfigHashNs = 0L;
            cachedConfigHash = 0L;
            cachedUnsafe = false;
            failureLogged = false;
        }

        static RoomStamp capture(int sourceId) {
            try {
                Minecraft minecraft = Minecraft.getInstance();
                if (minecraft == null || minecraft.gameRenderer == null
                        || minecraft.gameRenderer.getMainCamera() == null) return RoomStamp.invalid();
                Vec3 camera = minecraft.gameRenderer.getMainCamera().getPosition();
                if (camera == null) return RoomStamp.invalid();

                if (mcLevelField == null) mcLevelField = minecraft.getClass().getField("level");
                if (mcPlayerField == null) mcPlayerField = minecraft.getClass().getField("player");
                Object level = mcLevelField.get(minecraft);
                Object player = mcPlayerField.get(minecraft);
                if (level == null || player == null) return RoomStamp.invalid();

                Class<?> levelClass = level.getClass();
                if (levelGetCloneMethod == null || levelGetCloneOwner == null
                        || !levelGetCloneOwner.isAssignableFrom(levelClass)) {
                    levelGetCloneMethod = levelClass.getMethod("sound_physics_remastered$getCachedClone");
                    levelGetCloneOwner = levelClass;
                }
                Object clone = levelGetCloneMethod.invoke(level);
                if (clone == null) return RoomStamp.invalid();

                Class<?> cloneClass = clone.getClass();
                if (cloneGetTickMethod == null || cloneTickOwner == null
                        || !cloneTickOwner.isAssignableFrom(cloneClass)) {
                    cloneGetTickMethod = cloneClass.getMethod("getTick");
                    cloneTickOwner = cloneClass;
                }
                long cloneTick = ((Number) cloneGetTickMethod.invoke(clone)).longValue();

                Class<?> playerClass = player.getClass();
                if (playerPositionMethod == null || playerOwner == null
                        || !playerOwner.isAssignableFrom(playerClass)) {
                    playerPositionMethod = playerClass.getMethod("position");
                    playerUnderwaterMethod = playerClass.getMethod("isUnderWater");
                    playerOwner = playerClass;
                }
                Vec3 playerPosition = (Vec3) playerPositionMethod.invoke(player);
                boolean underwater = Boolean.TRUE.equals(playerUnderwaterMethod.invoke(player));
                if (playerPosition == null) return RoomStamp.invalid();

                ConfigStamp config = configStamp();
                if (config.unsafeLevelAccess) return RoomStamp.invalid();
                float alMaxDistance = AL10.alGetSourcef(sourceId, AL_MAX_DISTANCE);
                return new RoomStamp(true, clone, cloneTick, config.fingerprint,
                        camera.x, camera.y, camera.z,
                        playerPosition.x, playerPosition.y, playerPosition.z,
                        underwater, alMaxDistance);
            } catch (Throwable throwable) {
                if (!failureLogged) {
                    failureLogged = true;
                    CompatAudioManager.logOnce("beta9-room-stamp",
                            "Stationary room reuse disabled after safe snapshot-introspection fallback: " + throwable);
                }
                return RoomStamp.invalid();
            }
        }

        private static ConfigStamp configStamp() throws Exception {
            long now = System.nanoTime();
            Class<?> mod = Class.forName("com.sonicether.soundphysics.SoundPhysicsMod");
            if (sprConfigField == null) sprConfigField = mod.getField("CONFIG");
            if (sprReflectivityField == null) sprReflectivityField = mod.getField("REFLECTIVITY_CONFIG");
            if (sprOcclusionField == null) sprOcclusionField = mod.getField("OCCLUSION_CONFIG");
            if (sprSoundRateField == null) sprSoundRateField = mod.getField("SOUND_RATE_CONFIG");

            Object config = sprConfigField.get(null);
            if (config == null) return new ConfigStamp(0L, true);

            Object unsafeHolder = config.getClass().getField("unsafeLevelAccess").get(config);
            Object unsafeValue = unsafeHolder == null ? null : unsafeHolder.getClass().getMethod("get").invoke(unsafeHolder);
            boolean unsafe = unsafeValue instanceof Boolean value && value;
            if (unsafe) {
                cachedUnsafe = true;
                return new ConfigStamp(cachedConfigHash, true);
            }
            if (now - lastConfigHashNs < 250_000_000L) {
                return new ConfigStamp(cachedConfigHash, false);
            }

            if (sprConfigEntries == null) {
                sprConfigEntries = Arrays.stream(config.getClass().getFields())
                        .filter(field -> !Modifier.isStatic(field.getModifiers()))
                        .sorted(Comparator.comparing(Field::getName))
                        .toArray(Field[]::new);
            }

            long hash = -3750763034362895579L;
            boolean unsafeFromEntries = false;
            for (Field field : sprConfigEntries) {
                Object holder = field.get(config);
                Object value = holder == null ? null : holder.getClass().getMethod("get").invoke(holder);
                hash = mix(hash, field.getName().hashCode());
                hash = mix(hash, stableHash(value));
                if ("unsafeLevelAccess".equals(field.getName()) && value instanceof Boolean bool) {
                    unsafeFromEntries = bool;
                }
            }

            Object reflectivity = sprReflectivityField.get(null);
            if (reflectivity != null) {
                Class<?> owner = reflectivity.getClass();
                if (reflectivityDefinitionsMethod == null || reflectivityOwner == null
                        || !reflectivityOwner.isAssignableFrom(owner)) {
                    reflectivityDefinitionsMethod = owner.getMethod("getBlockDefinitions");
                    reflectivityOwner = owner;
                }
                Object definitions = reflectivityDefinitionsMethod.invoke(reflectivity);
                hash = mix(hash, definitions == null ? 0L : definitions.hashCode());
            }

            Object occlusion = sprOcclusionField.get(null);
            if (occlusion != null) {
                Class<?> owner = occlusion.getClass();
                if (occlusionDefinitionsMethod == null || occlusionOwner == null
                        || !occlusionOwner.isAssignableFrom(owner)) {
                    occlusionDefinitionsMethod = owner.getMethod("getBlockDefinitions");
                    occlusionOwner = owner;
                }
                Object definitions = occlusionDefinitionsMethod.invoke(occlusion);
                hash = mix(hash, definitions == null ? 0L : definitions.hashCode());
            }

            Object soundRate = sprSoundRateField.get(null);
            if (soundRate != null) {
                Class<?> owner = soundRate.getClass();
                if (soundRateDefinitionsMethod == null || soundRateOwner == null
                        || !soundRateOwner.isAssignableFrom(owner)) {
                    soundRateDefinitionsMethod = owner.getMethod("getSoundRateConfig");
                    soundRateOwner = owner;
                }
                Object definitions = soundRateDefinitionsMethod.invoke(soundRate);
                hash = mix(hash, definitions == null ? 0L : definitions.hashCode());
            }

            cachedConfigHash = hash;
            cachedUnsafe = unsafeFromEntries;
            lastConfigHashNs = now;
            return new ConfigStamp(hash, unsafeFromEntries);
        }

        private static long mix(long hash, long value) {
            hash ^= value;
            return hash * 1099511628211L;
        }

        private static long stableHash(Object value) {
            if (value == null) return 0L;
            if (value instanceof Double number) return Double.doubleToLongBits(number);
            if (value instanceof Float number) return Float.floatToIntBits(number);
            if (value instanceof Long number) return number;
            if (value instanceof Integer number) return number.longValue();
            if (value instanceof Short number) return number.longValue();
            if (value instanceof Byte number) return number.longValue();
            if (value instanceof Boolean bool) return bool ? 1L : 0L;
            return value.hashCode();
        }

        private record ConfigStamp(long fingerprint, boolean unsafeLevelAccess) {}
    }
}
