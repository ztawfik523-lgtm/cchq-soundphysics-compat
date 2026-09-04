package dev.cchqphysics.compat.audio;

import dev.cchqphysics.compat.config.ExtendedClientConfig;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Hotfix3 adaptive direct/room controller reconstructed from the authoritative classfile. */
final class Beta9Optimizer {
    private static final Map<Integer, SourceMeta> META = new HashMap<>();
    private static final Map<Integer, DirectEntry> DIRECT = new HashMap<>();
    private static final ThreadLocal<PendingDirect> PENDING = new ThreadLocal<>();
    private static final ThreadLocal<long[]> TIMERS = ThreadLocal.withInitial(() -> new long[2]);

    private static final long REPORT_NS = 10_000_000_000L;
    private static final long CONTROL_NS = 1_000_000_000L;
    // Phase 5 exposes the Hotfix3 movement/backoff values through ExtendedClientConfig.

    private static long reportStartNs = System.nanoTime();
    private static long controlStartNs = reportStartNs;

    private static long directReal;
    private static long directReuse;
    private static long directTotalNs;
    private static long directMaxNs;
    private static long directRealAgeSamples;
    private static long directRealAgeTotalNs;
    private static long directRealAgeMaxNs;
    private static long directValidationSamples;
    private static long directValidationGapTotalNs;
    private static long directValidationGapMaxNs;
    private static long sentinelCalls;
    private static long sentinelTotalNs;
    private static long sentinelMaxNs;
    private static long efxPasses;
    private static long efxTotalNs;
    private static long efxMaxNs;
    private static long inaudibleSkips;
    private static long stableBackoffs;
    private static long relevanceBackoffs;
    private static long adaptiveBackoffs;
    private static long roomObservations;
    private static long roomStableObservations;
    private static long movementResets;

    private static long ctrlAcousticNs;
    private static long ctrlSprNs;
    private static long ctrlQueueNs;
    private static long ctrlQueueMaxNs;
    private static long ctrlQueueSamples;
    private static long ctrlSprCalls;

    private static double adaptiveFactor = 1.0D;
    private static double reportMinAdaptive = 1.0D;
    private static double reportMaxAdaptive = 1.0D;
    private static int pressureWindows;
    private static int healthyWindows;
    private static long controllerUps;
    private static long controllerDowns;
    private static double lastAcousticMsPerSec;
    private static double lastQueueAvgMs;
    private static double lastQueueMaxMs;
    private static double lastSprMsPerSec;
    private static long lastListenerMoveNs;

    private Beta9Optimizer() {}

    static synchronized void registerSource(int sourceId) {
        META.put(sourceId, new SourceMeta());
        DIRECT.remove(sourceId);
    }

    static synchronized void unregisterSource(int sourceId) {
        META.remove(sourceId);
        DIRECT.remove(sourceId);
        PendingDirect pending = PENDING.get();
        if (pending != null && pending.sourceId == sourceId) PENDING.remove();
    }

    static synchronized void updateSource(int sourceId, double x, double y, double z) {
        SourceMeta meta = META.computeIfAbsent(sourceId, ignored -> new SourceMeta());
        boolean changed = !meta.haveSource || !same(meta.x, x) || !same(meta.y, y) || !same(meta.z, z);
        if (changed) {
            DIRECT.remove(sourceId);
            meta.stableCount = 0;
            meta.haveRoom = false;
        }
        meta.x = x;
        meta.y = y;
        meta.z = z;
        meta.haveSource = true;
    }

    static synchronized void updateAudibility(int sourceId, float gain) {
        SourceMeta meta = META.computeIfAbsent(sourceId, ignored -> new SourceMeta());
        boolean wasAudible = !meta.audibleKnown || meta.audible;
        meta.gain = Math.max(0.0F, gain);
        meta.audibleKnown = true;
        meta.audible = gain > 1.0E-4F;
        if (wasAudible && !meta.audible) meta.stableCount = 0;
        maybeReportAndControl(System.nanoTime());
    }

    static synchronized void updateDistance(int sourceId, double distanceSq, double maxDistanceSq) {
        SourceMeta meta = META.computeIfAbsent(sourceId, ignored -> new SourceMeta());
        if (Double.isFinite(distanceSq) && Double.isFinite(maxDistanceSq) && maxDistanceSq > 1.0E-9D) {
            meta.distanceRatio = Math.sqrt(Math.max(0.0D, distanceSq) / maxDistanceSq);
            meta.haveDistance = true;
        }
    }

    static synchronized boolean isAudible(int sourceId) {
        SourceMeta meta = META.get(sourceId);
        return meta == null || !meta.audibleKnown || meta.audible;
    }

    static synchronized boolean isAudibleAndRecord(int sourceId) {
        boolean audible = isAudible(sourceId);
        if (!audible) inaudibleSkips++;
        maybeReportAndControl(System.nanoTime());
        return audible;
    }

    static synchronized void onListenerMovement(double movementSq) {
        long now = System.nanoTime();
        if (Double.isFinite(movementSq) && movementSq >= ExtendedClientConfig.beta9ListenerMoveSq()) {
            lastListenerMoveNs = now;
            boolean hadStable = false;
            for (SourceMeta meta : META.values()) {
                if (meta.stableCount != 0) hadStable = true;
                meta.stableCount = 0;
            }
            if (hadStable || !META.isEmpty()) movementResets++;
        }
        maybeReportAndControl(now);
    }

    static float[] adjustDirect(int sourceId, float directCutoff, float directGain) {
        if (ProgressiveOcclusionModel.beta9ApplyOverrideSource() == sourceId) {
            long start = System.nanoTime();
            float[] result = ProgressiveOcclusionModel.beta9AdjustReal(sourceId, directCutoff, directGain);
            recordDirectStandalone(start, true);
            return result;
        }
        float[] cached = beginDirect(sourceId, directCutoff, directGain);
        if (cached != null) return cached;
        return finishDirect(sourceId, ProgressiveOcclusionModel.beta9AdjustReal(sourceId, directCutoff, directGain));
    }

    static synchronized void invalidateDirect(int sourceId) {
        DIRECT.remove(sourceId);
        PendingDirect pending = PENDING.get();
        if (pending != null && pending.sourceId == sourceId) PENDING.remove();
    }

    static float[] beginDirect(int sourceId, float inputCutoff, float inputGain) {
        long startNs = System.nanoTime();
        Object stamp = SoundPhysicsBridge.beta9CaptureStamp(sourceId);
        synchronized (Beta9Optimizer.class) {
            SourceMeta meta = META.get(sourceId);
            DirectEntry entry = DIRECT.get(sourceId);
            if (ExtendedClientConfig.beta9DirectReuseEnabled()
                    && meta != null && meta.haveSource && entry != null
                    && entry.inputCutoffBits == Float.floatToIntBits(inputCutoff)
                    && entry.inputGainBits == Float.floatToIntBits(inputGain)
                    && same(meta.x, entry.x) && same(meta.y, entry.y) && same(meta.z, entry.z)
                    && SoundPhysicsBridge.beta9SameStamp(entry.stamp, stamp)) {
                long now = System.nanoTime();
                long elapsed = now - startNs;
                directReuse++;
                directTotalNs += elapsed;
                ctrlAcousticNs += elapsed;
                directMaxNs = Math.max(directMaxNs, elapsed);
                if (entry.lastRealNs != 0L) {
                    long age = Math.max(0L, now - entry.lastRealNs);
                    directRealAgeSamples++;
                    directRealAgeTotalNs += age;
                    directRealAgeMaxNs = Math.max(directRealAgeMaxNs, age);
                }
                if (entry.lastValidatedNs != 0L) {
                    long gap = Math.max(0L, now - entry.lastValidatedNs);
                    directValidationSamples++;
                    directValidationGapTotalNs += gap;
                    directValidationGapMaxNs = Math.max(directValidationGapMaxNs, gap);
                }
                entry.lastValidatedNs = now;
                PENDING.remove();
                maybeReportAndControl(now);
                return entry.result;
            }

            PendingDirect pending = new PendingDirect();
            pending.sourceId = sourceId;
            pending.startNs = startNs;
            pending.startStamp = stamp;
            pending.inputCutoffBits = Float.floatToIntBits(inputCutoff);
            pending.inputGainBits = Float.floatToIntBits(inputGain);
            PENDING.set(pending);
            return null;
        }
    }

    static float[] finishDirect(int sourceId, float[] result) {
        Object endStamp = SoundPhysicsBridge.beta9CaptureStamp(sourceId);
        long now = System.nanoTime();
        synchronized (Beta9Optimizer.class) {
            PendingDirect pending = PENDING.get();
            PENDING.remove();
            if (pending != null && pending.sourceId == sourceId) {
                long elapsed = now - pending.startNs;
                directReal++;
                directTotalNs += elapsed;
                ctrlAcousticNs += elapsed;
                directMaxNs = Math.max(directMaxNs, elapsed);
                SourceMeta meta = META.get(sourceId);
                if (result != null && result.length >= 2 && meta != null && meta.haveSource
                        && SoundPhysicsBridge.beta9SameStamp(pending.startStamp, endStamp)) {
                    DirectEntry entry = new DirectEntry();
                    entry.x = meta.x;
                    entry.y = meta.y;
                    entry.z = meta.z;
                    entry.inputCutoffBits = pending.inputCutoffBits;
                    entry.inputGainBits = pending.inputGainBits;
                    entry.stamp = endStamp;
                    entry.result = result;
                    entry.lastRealNs = now;
                    entry.lastValidatedNs = now;
                    DIRECT.put(sourceId, entry);
                }
            }
            maybeReportAndControl(now);
            return result;
        }
    }

    private static synchronized void recordDirectStandalone(long startNs, boolean real) {
        long now = System.nanoTime();
        long elapsed = Math.max(0L, now - startNs);
        if (real) directReal++;
        directTotalNs += elapsed;
        ctrlAcousticNs += elapsed;
        directMaxNs = Math.max(directMaxNs, elapsed);
        maybeReportAndControl(now);
    }

    static synchronized void observeRoom(int sourceId, AcousticCapture.Result result) {
        SourceMeta meta = META.computeIfAbsent(sourceId, ignored -> new SourceMeta());
        roomObservations++;
        if (result == null || !result.environmentCaptured()) {
            meta.stableCount = 0;
            meta.haveRoom = false;
            return;
        }

        double rx = Double.NaN;
        double ry = Double.NaN;
        double rz = Double.NaN;
        boolean haveReflected = false;
        try {
            Object reflected = result.reflectedWrite();
            if (reflected != null) {
                var xField = reflected.getClass().getField("x");
                var yField = reflected.getClass().getField("y");
                var zField = reflected.getClass().getField("z");
                rx = ((Number) xField.get(reflected)).doubleValue();
                ry = ((Number) yField.get(reflected)).doubleValue();
                rz = ((Number) zField.get(reflected)).doubleValue();
                haveReflected = true;
            }
        } catch (Throwable ignored) {
        }
        float wetEnergy = maxAbs(result.r0(), result.r1(), result.r2(), result.r3());

        boolean stable = meta.haveRoom
                && maxAbsDiff(meta.r0, result.r0(), meta.r1, result.r1(), meta.r2, result.r2(), meta.r3, result.r3()) <= 0.0125F
                && maxAbsDiff(meta.h0, result.h0(), meta.h1, result.h1(), meta.h2, result.h2(), meta.h3, result.h3()) <= 0.025F
                && Math.abs(meta.roomDirectCutoff - result.directCutoff()) <= 0.015F
                && Math.abs(meta.roomDirectGain - result.directGain()) <= 0.015F
                && reflectedStable(meta, haveReflected, rx, ry, rz);

        boolean recentlyMoved = lastListenerMoveNs != 0L
                && System.nanoTime() - lastListenerMoveNs < ExtendedClientConfig.beta9RecentMovementNs();
        if (stable && !recentlyMoved) {
            meta.stableCount = Math.min(1000, meta.stableCount + 1);
            roomStableObservations++;
        } else {
            meta.stableCount = 0;
        }

        meta.haveRoom = true;
        meta.r0 = result.r0(); meta.r1 = result.r1(); meta.r2 = result.r2(); meta.r3 = result.r3();
        meta.h0 = result.h0(); meta.h1 = result.h1(); meta.h2 = result.h2(); meta.h3 = result.h3();
        meta.roomDirectCutoff = result.directCutoff();
        meta.roomDirectGain = result.directGain();
        meta.wetEnergy = wetEnergy;
        meta.haveReflected = haveReflected;
        meta.reflectedX = rx; meta.reflectedY = ry; meta.reflectedZ = rz;
        maybeReportAndControl(System.nanoTime());
    }

    static synchronized long roomInterval(int sourceId, long baseIntervalNs, AcousticCapture.Result room) {
        long now = System.nanoTime();
        SourceMeta meta = META.get(sourceId);
        if (meta == null || baseIntervalNs <= 0L) return baseIntervalNs;

        boolean recentlyMoved = lastListenerMoveNs != 0L && now - lastListenerMoveNs < ExtendedClientConfig.beta9RecentMovementNs();
        if (!ExtendedClientConfig.beta9RoomBackoffEnabled()) {
            maybeReportAndControl(now);
            return baseIntervalNs;
        }
        double stableFactor = 1.0D;
        if (!recentlyMoved) {
            if (meta.stableCount >= 10) stableFactor = 2.5D;
            else if (meta.stableCount >= 6) stableFactor = 1.75D;
            else if (meta.stableCount >= 3) stableFactor = 1.25D;
        }

        double relevanceFactor = 1.0D;
        if (meta.haveDistance) {
            if (meta.distanceRatio > 0.85D) relevanceFactor = 1.7D;
            else if (meta.distanceRatio > 0.65D) relevanceFactor = 1.35D;
            else if (meta.distanceRatio > 0.35D) relevanceFactor = 1.15D;
        }
        if (meta.gain < 0.25F) relevanceFactor *= 1.15D;
        if (meta.wetEnergy >= 0.15F) relevanceFactor = Math.min(relevanceFactor, 1.15D);
        else if (meta.wetEnergy >= 0.05F) relevanceFactor = Math.min(relevanceFactor, 1.35D);

        double adaptive = ExtendedClientConfig.beta9AdaptiveControllerEnabled() ? adaptiveFactor : 1.0D;
        if (stableFactor > 1.0D) stableBackoffs++;
        if (relevanceFactor > 1.0D) relevanceBackoffs++;
        if (adaptive > 1.001D) adaptiveBackoffs++;

        double environmentalFactor = Math.max(stableFactor, relevanceFactor);
        double totalFactor = Math.min(ExtendedClientConfig.beta9MaxRoomFactor(),
                Math.max(1.0D, environmentalFactor * adaptive));
        long result;
        if (totalFactor <= 1.0D) {
            result = baseIntervalNs;
        } else {
            double scaled = baseIntervalNs * totalFactor;
            result = scaled >= Long.MAX_VALUE ? Long.MAX_VALUE : (long) scaled;
            result = Math.min(result, ExtendedClientConfig.beta9MaxRoomIntervalNs());
        }
        maybeReportAndControl(now);
        return Math.max(baseIntervalNs, result);
    }

    static void beginSentinelTimer() { TIMERS.get()[0] = System.nanoTime(); }
    static void endSentinelTimer() { recordSentinelTime(TIMERS.get()[0]); }
    static void beginEfxTimer() { TIMERS.get()[1] = System.nanoTime(); }
    static void endEfxTimer() { recordEfxTime(TIMERS.get()[1]); }

    static synchronized void recordSentinelTime(long startNs) {
        long now = System.nanoTime();
        long elapsed = startNs == 0L ? 0L : Math.max(0L, now - startNs);
        sentinelCalls++;
        sentinelTotalNs += elapsed;
        ctrlAcousticNs += elapsed;
        sentinelMaxNs = Math.max(sentinelMaxNs, elapsed);
        maybeReportAndControl(now);
    }

    static synchronized void recordEfxTime(long startNs) {
        long now = System.nanoTime();
        long elapsed = startNs == 0L ? 0L : Math.max(0L, now - startNs);
        efxPasses++;
        efxTotalNs += elapsed;
        ctrlAcousticNs += elapsed;
        efxMaxNs = Math.max(efxMaxNs, elapsed);
        maybeReportAndControl(now);
    }

    static synchronized void recordSprTime(long elapsedNs) {
        long elapsed = Math.max(0L, elapsedNs);
        ctrlSprNs += elapsed;
        ctrlAcousticNs += elapsed;
        ctrlSprCalls++;
        maybeReportAndControl(System.nanoTime());
    }

    static synchronized void recordQueueDelay(long delayNs) {
        long delay = Math.max(0L, delayNs);
        ctrlQueueNs += delay;
        ctrlQueueSamples++;
        ctrlQueueMaxNs = Math.max(ctrlQueueMaxNs, delay);
        maybeReportAndControl(System.nanoTime());
    }

    private static void updateController(long now) {
        long elapsedNs = now - controlStartNs;
        if (elapsedNs < CONTROL_NS) return;
        double seconds = Math.max(0.001D, elapsedNs / 1.0E9D);
        lastAcousticMsPerSec = (ctrlAcousticNs / 1_000_000.0D) / seconds;
        lastSprMsPerSec = (ctrlSprNs / 1_000_000.0D) / seconds;
        lastQueueAvgMs = ctrlQueueSamples == 0L ? 0.0D : (ctrlQueueNs / 1_000_000.0D) / ctrlQueueSamples;
        lastQueueMaxMs = ctrlQueueMaxNs / 1_000_000.0D;

        boolean severe = lastAcousticMsPerSec > 100.0D
                || lastQueueAvgMs > 4.0D
                || (lastQueueAvgMs > 1.0D && lastQueueMaxMs > 25.0D);
        boolean pressure = lastAcousticMsPerSec > 62.0D
                || lastQueueAvgMs > 1.5D
                || (lastQueueAvgMs > 0.75D && lastQueueMaxMs > 12.0D);
        boolean healthy = lastAcousticMsPerSec < 42.0D
                && lastQueueAvgMs < 0.75D
                && lastQueueMaxMs < 8.0D;

        if (severe) {
            double before = adaptiveFactor;
            adaptiveFactor = Math.min(2.0D, adaptiveFactor + 0.25D);
            if (adaptiveFactor > before + 1.0E-9D) controllerUps++;
            pressureWindows = 0;
            healthyWindows = 0;
        } else if (pressure) {
            pressureWindows++;
            healthyWindows = 0;
            if (pressureWindows >= 2) {
                double before = adaptiveFactor;
                adaptiveFactor = Math.min(2.0D, adaptiveFactor + 0.12D);
                if (adaptiveFactor > before + 1.0E-9D) controllerUps++;
                pressureWindows = 0;
            }
        } else if (healthy) {
            healthyWindows++;
            pressureWindows = Math.max(0, pressureWindows - 1);
            if (healthyWindows >= 3) {
                double before = adaptiveFactor;
                adaptiveFactor = Math.max(1.0D, adaptiveFactor - 0.04D);
                if (adaptiveFactor < before - 1.0E-9D) controllerDowns++;
                healthyWindows = 0;
            }
        } else {
            pressureWindows = Math.max(0, pressureWindows - 1);
            healthyWindows = 0;
        }

        reportMinAdaptive = Math.min(reportMinAdaptive, adaptiveFactor);
        reportMaxAdaptive = Math.max(reportMaxAdaptive, adaptiveFactor);
        ctrlSprCalls = ctrlQueueSamples = ctrlQueueMaxNs = ctrlQueueNs = ctrlSprNs = ctrlAcousticNs = 0L;
        controlStartNs = now;
    }

    static synchronized void debugResetCaches() {
        DIRECT.clear();
        PENDING.remove();
        for (SourceMeta meta : META.values()) {
            meta.stableCount = 0;
            meta.haveRoom = false;
        }
        adaptiveFactor = 1.0D;
        reportMinAdaptive = 1.0D;
        reportMaxAdaptive = 1.0D;
        pressureWindows = 0;
        healthyWindows = 0;
        ctrlAcousticNs = ctrlSprNs = ctrlQueueNs = ctrlQueueMaxNs = ctrlQueueSamples = ctrlSprCalls = 0L;
        lastAcousticMsPerSec = lastSprMsPerSec = lastQueueAvgMs = lastQueueMaxMs = 0.0D;
        controlStartNs = System.nanoTime();
    }

    static synchronized String debugSummary() {
        return "beta9Meta=" + META.size() + " directCache=" + DIRECT.size()
                + " load=" + round2(adaptiveFactor) + " directReal=" + directReal + " directReuse=" + directReuse;
    }

    private static boolean reflectedStable(SourceMeta meta, boolean have, double x, double y, double z) {
        if (!meta.haveReflected && !have) return true;
        if (meta.haveReflected != have) return false;
        double dx = meta.reflectedX - x;
        double dy = meta.reflectedY - y;
        double dz = meta.reflectedZ - z;
        return dx * dx + dy * dy + dz * dz <= 0.04D;
    }

    private static float maxAbs(float a, float b, float c, float d) {
        return Math.max(Math.max(Math.abs(a), Math.abs(b)), Math.max(Math.abs(c), Math.abs(d)));
    }

    private static float maxAbsDiff(float a0, float b0, float a1, float b1,
                                    float a2, float b2, float a3, float b3) {
        return Math.max(Math.max(Math.abs(a0 - b0), Math.abs(a1 - b1)),
                Math.max(Math.abs(a2 - b2), Math.abs(a3 - b3)));
    }

    private static boolean same(double a, double b) {
        return Double.doubleToLongBits(a) == Double.doubleToLongBits(b);
    }

    private static void maybeReportAndControl(long now) {
        updateController(now);
        long elapsed = now - reportStartNs;
        if (elapsed < ExtendedClientConfig.performanceReportNs()) return;
        double seconds = elapsed / 1.0E9D;
        String report = "[CC:HQ Sound Physics Compat] beta9 extra window=" + round1(seconds)
                + "s directReal=" + directReal + " (" + round1(directReal / seconds) + "/s)"
                + " directReuses=" + directReuse + " (" + round1(directReuse / seconds) + "/s)"
                + " directAvg=" + avgMs(directTotalNs, directReal + directReuse) + "ms"
                + " directMax=" + ms(directMaxNs) + "ms"
                + " realAgeAvg=" + avgMs(directRealAgeTotalNs, directRealAgeSamples) + "ms"
                + " realAgeMax=" + ms(directRealAgeMaxNs) + "ms"
                + " validateGapAvg=" + avgMs(directValidationGapTotalNs, directValidationSamples) + "ms"
                + " validateGapMax=" + ms(directValidationGapMaxNs) + "ms"
                + " sentinelAvg=" + avgMs(sentinelTotalNs, sentinelCalls) + "ms"
                + " sentinelMax=" + ms(sentinelMaxNs) + "ms"
                + " efxAvg=" + avgMs(efxTotalNs, efxPasses) + "ms"
                + " efxMax=" + ms(efxMaxNs) + "ms"
                + " inaudibleSkips=" + inaudibleSkips
                + " stableBackoffs=" + stableBackoffs
                + " relevanceBackoffs=" + relevanceBackoffs
                + " adaptiveBackoffs=" + adaptiveBackoffs
                + " movementResets=" + movementResets
                + " roomStable=" + roomStableObservations + "/" + roomObservations
                + " loadFactor=" + round2(adaptiveFactor)
                + " loadRange=" + round2(reportMinAdaptive) + "-" + round2(reportMaxAdaptive)
                + " acoustic=" + round1(lastAcousticMsPerSec) + "ms/s"
                + " sprLoad=" + round1(lastSprMsPerSec) + "ms/s"
                + " qCtrlAvg=" + round3(lastQueueAvgMs) + "ms"
                + " qCtrlMax=" + round3(lastQueueMaxMs) + "ms"
                + " ctrlUp=" + controllerUps + " ctrlDown=" + controllerDowns;
        SoundPhysicsBridge.beta9Log(report);

        reportStartNs = now;
        directReal = directReuse = directTotalNs = directMaxNs = 0L;
        directRealAgeSamples = directRealAgeTotalNs = directRealAgeMaxNs = 0L;
        directValidationSamples = directValidationGapTotalNs = directValidationGapMaxNs = 0L;
        sentinelCalls = sentinelTotalNs = sentinelMaxNs = 0L;
        efxPasses = efxTotalNs = efxMaxNs = 0L;
        inaudibleSkips = stableBackoffs = relevanceBackoffs = adaptiveBackoffs = 0L;
        movementResets = roomStableObservations = roomObservations = 0L;
        controllerUps = controllerDowns = 0L;
        reportMinAdaptive = reportMaxAdaptive = adaptiveFactor;
    }

    static synchronized double adaptiveFactorForTest() { return adaptiveFactor; }
    static synchronized int stableCountForTest(int sourceId) {
        SourceMeta meta = META.get(sourceId);
        return meta == null ? 0 : meta.stableCount;
    }

    private static String avgMs(long ns, long count) {
        return count <= 0L ? "0.000" : String.format(Locale.ROOT, "%.3f", (ns / 1_000_000.0D) / count);
    }
    private static String ms(long ns) { return String.format(Locale.ROOT, "%.3f", ns / 1_000_000.0D); }
    private static String round1(double value) { return String.format(Locale.ROOT, "%.1f", value); }
    private static String round2(double value) { return String.format(Locale.ROOT, "%.2f", value); }
    private static String round3(double value) { return String.format(Locale.ROOT, "%.3f", value); }

    private static final class SourceMeta {
        boolean haveSource;
        double x, y, z;
        boolean audibleKnown;
        boolean audible = true;
        float gain = 1.0F;
        boolean haveDistance;
        double distanceRatio;
        boolean haveRoom;
        int stableCount;
        float r0, r1, r2, r3;
        float h0, h1, h2, h3;
        float roomDirectCutoff, roomDirectGain;
        float wetEnergy;
        boolean haveReflected;
        double reflectedX, reflectedY, reflectedZ;
    }

    private static final class DirectEntry {
        double x, y, z;
        int inputCutoffBits, inputGainBits;
        Object stamp;
        float[] result;
        long lastRealNs, lastValidatedNs;
    }

    private static final class PendingDirect {
        int sourceId;
        long startNs;
        Object startStamp;
        int inputCutoffBits, inputGainBits;
    }
}
