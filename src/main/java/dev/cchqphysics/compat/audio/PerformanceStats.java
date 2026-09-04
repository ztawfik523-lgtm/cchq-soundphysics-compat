package dev.cchqphysics.compat.audio;

import dev.cchqphysics.compat.config.ClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

final class PerformanceStats {
    private static final Logger LOGGER = LoggerFactory.getLogger("CC:HQ Sound Physics Compat");
    private static final long WINDOW_NS = 10_000_000_000L;

    private static long windowStartNs = System.nanoTime();
    private static long sprCalls;
    private static long sprTotalNs;
    private static long sprMaxNs;
    private static long occlusionPaths;
    private static long efxApplies;
    private static long efxReattachPasses;
    private static long progressiveEvals;
    private static long progressiveFullRefreshes;
    private static long progressivePartialRefreshes;
    private static long progressiveSavedPaths;
    private static long roomRefreshes;
    private static long roomReuses;
    private static long applyPasses;
    private static long sentinelPaths;
    private static long sentinelCandidates;
    private static long sentinelConfirmed;
    private static long immediateDirectApplies;
    private static long immediateRoomApplies;
    private static long schedulerQueueSamples;
    private static long schedulerQueueTotalNs;
    private static long schedulerQueueMaxNs;
    private static final AtomicLong schedulerCoalesced = new AtomicLong();
    private static long ageSamples;
    private static long directAgeTotalNs;
    private static long directAgeMaxNs;
    private static long roomAgeTotalNs;
    private static long roomAgeMaxNs;
    private static long applyGapTotalNs;
    private static long applyGapMaxNs;
    private static long transitionSamples;
    private static long transitionTotalNs;
    private static long transitionMaxNs;

    private PerformanceStats() {}

    static void recordSpr(long elapsedNs) {
        Beta9Optimizer.recordSprTime(elapsedNs);
        sprCalls++;
        long value = Math.max(0L, elapsedNs);
        sprTotalNs += value;
        if (value > sprMaxNs) sprMaxNs = value;
        maybeReport();
    }

    static void recordOcclusionPath() { occlusionPaths++; }

    static void recordSentinelPath() {
        sentinelPaths++;
        occlusionPaths++;
    }

    static void recordSentinelCandidate(boolean confirmed) {
        sentinelCandidates++;
        if (confirmed) sentinelConfirmed++;
    }

    static void recordEfxApply(boolean reattached) {
        efxApplies++;
        if (reattached) efxReattachPasses++;
    }

    static void recordRoomRefresh() { roomRefreshes++; }
    static void recordRoomReuse() { roomReuses++; }
    static void recordApplyPass() { applyPasses++; }
    static void recordImmediateDirectApply() { immediateDirectApplies++; }
    static void recordImmediateRoomApply() { immediateRoomApplies++; }

    static void recordSchedulerQueue(long delayNs) {
        Beta10Optimizer.recordQueueDelay(delayNs);
        long value = Math.max(0L, delayNs);
        schedulerQueueSamples++;
        schedulerQueueTotalNs += value;
        if (value > schedulerQueueMaxNs) schedulerQueueMaxNs = value;
        maybeReport();
    }

    static void recordSchedulerCoalesced() {
        schedulerCoalesced.incrementAndGet();
    }

    static void recordTargetAges(long directAgeNs, long roomAgeNs, long applyGapNs) {
        long direct = Math.max(0L, directAgeNs);
        long room = Math.max(0L, roomAgeNs);
        long apply = Math.max(0L, applyGapNs);
        ageSamples++;
        directAgeTotalNs += direct;
        roomAgeTotalNs += room;
        applyGapTotalNs += apply;
        if (direct > directAgeMaxNs) directAgeMaxNs = direct;
        if (room > roomAgeMaxNs) roomAgeMaxNs = room;
        if (apply > applyGapMaxNs) applyGapMaxNs = apply;
    }

    static void recordTransitionLatency(long latencyNs) {
        long value = Math.max(0L, latencyNs);
        transitionSamples++;
        transitionTotalNs += value;
        if (value > transitionMaxNs) transitionMaxNs = value;
    }

    static void recordProgressiveEvaluation(int paths, boolean fullRefresh) {
        progressiveEvals++;
        if (fullRefresh) progressiveFullRefreshes++;
        else progressivePartialRefreshes++;
        progressiveSavedPaths += Math.max(0, 17 - paths);
    }

    static void reset() {
        windowStartNs = System.nanoTime();
        schedulerCoalesced.set(0L);
        resetCounters();
    }

    private static void maybeReport() {
        long now = System.nanoTime();
        long elapsed = now - windowStartNs;
        if (elapsed < WINDOW_NS) return;

        boolean diagnostics = ClientConfig.diagnosticsEnabled();
        boolean debug = LOGGER.isDebugEnabled();
        if (diagnostics || debug) {
            double seconds = elapsed / 1.0E9D;
            double sprAvgMs = sprCalls == 0L ? 0.0D : (sprTotalNs / 1_000_000.0D) / sprCalls;
            double queueAvgMs = schedulerQueueSamples == 0L ? 0.0D : (schedulerQueueTotalNs / 1_000_000.0D) / schedulerQueueSamples;
            double directAgeAvgMs = ageSamples == 0L ? 0.0D : (directAgeTotalNs / 1_000_000.0D) / ageSamples;
            double roomAgeAvgMs = ageSamples == 0L ? 0.0D : (roomAgeTotalNs / 1_000_000.0D) / ageSamples;
            double applyGapAvgMs = ageSamples == 0L ? 0.0D : (applyGapTotalNs / 1_000_000.0D) / ageSamples;
            double transitionAvgMs = transitionSamples == 0L ? 0.0D : (transitionTotalNs / 1_000_000.0D) / transitionSamples;
            long coalesced = schedulerCoalesced.getAndSet(0L);

            String format = "beta9 perf window={}s sprCalls={} ({}/s) sprAvg={}ms sprMax={}ms roomRefreshes={} ({}/s) roomReuses={} ({}/s) applyPasses={} ({}/s) occlusionPaths={} ({}/s) progressive={} full={} partial={} savedPaths={} sentinelPaths={} candidates={} confirmed={} immediateDirect={} immediateRoom={} queueAvg={}ms queueMax={}ms coalesced={} directAgeAvg={}ms directAgeMax={}ms roomAgeAvg={}ms roomAgeMax={}ms applyGapAvg={}ms applyGapMax={}ms transitionAvg={}ms transitionMax={}ms efxApplies={} ({}/s) efxReattachPasses={}";
            Object[] args = {
                    round1(seconds),
                    sprCalls, round1(sprCalls / seconds), round3(sprAvgMs), round3(sprMaxNs / 1_000_000.0D),
                    roomRefreshes, round1(roomRefreshes / seconds),
                    roomReuses, round1(roomReuses / seconds),
                    applyPasses, round1(applyPasses / seconds),
                    occlusionPaths, round1(occlusionPaths / seconds),
                    progressiveEvals, progressiveFullRefreshes, progressivePartialRefreshes, progressiveSavedPaths,
                    sentinelPaths, sentinelCandidates, sentinelConfirmed, immediateDirectApplies, immediateRoomApplies,
                    round3(queueAvgMs), round3(schedulerQueueMaxNs / 1_000_000.0D), coalesced,
                    round3(directAgeAvgMs), round3(directAgeMaxNs / 1_000_000.0D),
                    round3(roomAgeAvgMs), round3(roomAgeMaxNs / 1_000_000.0D),
                    round3(applyGapAvgMs), round3(applyGapMaxNs / 1_000_000.0D),
                    round3(transitionAvgMs), round3(transitionMaxNs / 1_000_000.0D),
                    efxApplies, round1(efxApplies / seconds), efxReattachPasses
            };
            if (diagnostics) LOGGER.info(format, args);
            else LOGGER.debug(format, args);
        }

        windowStartNs = now;
        resetCounters();
    }

    private static void resetCounters() {
        sprCalls = sprTotalNs = sprMaxNs = 0L;
        occlusionPaths = 0L;
        efxApplies = efxReattachPasses = 0L;
        progressiveEvals = progressiveFullRefreshes = progressivePartialRefreshes = progressiveSavedPaths = 0L;
        roomRefreshes = roomReuses = applyPasses = 0L;
        sentinelPaths = sentinelCandidates = sentinelConfirmed = 0L;
        immediateDirectApplies = immediateRoomApplies = 0L;
        schedulerQueueSamples = schedulerQueueTotalNs = schedulerQueueMaxNs = 0L;
        ageSamples = directAgeTotalNs = directAgeMaxNs = roomAgeTotalNs = roomAgeMaxNs = applyGapTotalNs = applyGapMaxNs = 0L;
        transitionSamples = transitionTotalNs = transitionMaxNs = 0L;
    }

    private static String round1(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private static String round3(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }
}
