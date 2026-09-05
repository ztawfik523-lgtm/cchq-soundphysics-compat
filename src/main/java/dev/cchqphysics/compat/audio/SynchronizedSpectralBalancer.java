package dev.cchqphysics.compat.audio;

import dev.cchqphysics.compat.config.ClientConfig;
import dev.cchqphysics.compat.config.SpectralMixConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

/**
 * Spectral-only synchronized-copy experiment. This class never changes
 * OpenAL source gain or position and never touches reverb-send filters.
 */
final class SynchronizedSpectralBalancer {
    private static final Logger LOGGER = LoggerFactory.getLogger("CC:HQ Sound Physics Compat");

    private SynchronizedSpectralBalancer() {}

    static float adjustDirectCutoff(int sourceId, float baseCutoff) {
        if (!SpectralMixConfig.enabled() || !ClientConfig.progressiveOcclusion()) return baseCutoff;
        double peerMax = clearestPeerCutoff(sourceId);
        if (!Double.isFinite(peerMax) || peerMax < SpectralMixConfig.peerClearCutoff()) return baseCutoff;
        double floor = peerMax * SpectralMixConfig.clarityFloorRatio();
        double cappedFloor = Math.min(floor, baseCutoff + SpectralMixConfig.maxCutoffLift());
        double adjusted = Math.max(baseCutoff, cappedFloor);
        return (float) Math.max(0.0D, Math.min(1.0D, adjusted));
    }

    private static double clearestPeerCutoff(int sourceId) {
        int[] peers = SyncStartCoordinator.livePeerSources(sourceId);
        double max = Double.NaN;
        for (int peer : peers) {
            double cutoff = ProgressiveOcclusionModel.currentCutoff(peer);
            if (!Double.isFinite(cutoff)) continue;
            if (!Double.isFinite(max) || cutoff > max) max = cutoff;
        }
        return max;
    }

    static void debugDump() {
        int[] sources = SyncStartCoordinator.liveGroupedSources();
        if (sources.length == 0) {
            LOGGER.info("[phase5/dump] spectral activeGroupedSources=0 {}", SpectralMixConfig.summary());
            return;
        }
        for (int sourceId : sources) {
            double base = ProgressiveOcclusionModel.currentCutoff(sourceId);
            double peerMax = clearestPeerCutoff(sourceId);
            float adjusted = Double.isFinite(base) ? adjustDirectCutoff(sourceId, (float) base) : Float.NaN;
            LOGGER.info("[phase5/dump] spectral source={} peers={} intrinsicCutoff={} clearestPeer={} adjustedCutoff={} delta={} enabled={}",
                    sourceId, SyncStartCoordinator.livePeerSources(sourceId).length,
                    round3(base), round3(peerMax), round3(adjusted),
                    Double.isFinite(base) && Float.isFinite(adjusted) ? round3(adjusted - base) : "nan",
                    SpectralMixConfig.enabled());
        }
    }

    private static String round3(double value) {
        return Double.isFinite(value) ? String.format(Locale.ROOT, "%.3f", value) : "nan";
    }
}
