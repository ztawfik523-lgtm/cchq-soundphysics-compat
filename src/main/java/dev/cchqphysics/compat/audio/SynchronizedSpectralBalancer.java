package dev.cchqphysics.compat.audio;

import dev.cchqphysics.compat.config.ClientConfig;
import dev.cchqphysics.compat.config.SpectralMixConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

/**
 * Direct-clarity balancing for synchronized copies of the same sound.
 * This class never changes OpenAL source gain or position and never touches
 * reverb-send filters or playback timing.
 */
final class SynchronizedSpectralBalancer {
    private static final Logger LOGGER = LoggerFactory.getLogger("CC:HQ Sound Physics Compat");

    private SynchronizedSpectralBalancer() {}

    static float adjustDirectCutoff(int sourceId, float baseCutoff) {
        if (!SpectralMixConfig.enabled() || !ClientConfig.progressiveOcclusion()) return baseCutoff;
        if (!Float.isFinite(baseCutoff) || baseCutoff > SpectralMixConfig.darkSourceCutoff()) return baseCutoff;

        double peerMax = clearestPeerCutoff(sourceId);
        if (!Double.isFinite(peerMax) || peerMax < SpectralMixConfig.peerClearCutoff()) return baseCutoff;

        double gap = peerMax - baseCutoff;
        if (gap < SpectralMixConfig.minPeerGap()) return baseCutoff;

        double requested = baseCutoff + gap * SpectralMixConfig.clarityFloorRatio();
        double capped = Math.min(requested, baseCutoff + SpectralMixConfig.maxCutoffLift());
        double adjusted = Math.max(baseCutoff, capped);
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
            LOGGER.info("[dump] sync-balance activeGroupedSources=0 {}", SpectralMixConfig.summary());
            return;
        }
        for (int sourceId : sources) {
            double base = ProgressiveOcclusionModel.currentCutoff(sourceId);
            double peerMax = clearestPeerCutoff(sourceId);
            double gap = Double.isFinite(base) && Double.isFinite(peerMax) ? peerMax - base : Double.NaN;
            float adjusted = Double.isFinite(base) ? adjustDirectCutoff(sourceId, (float) base) : Float.NaN;
            LOGGER.info("[dump] sync-balance source={} peers={} intrinsicCutoff={} clearestPeer={} gap={} adjustedCutoff={} delta={} enabled={} muffledThreshold={} clearThreshold={} differenceThreshold={} correctionStrength={} maxIncrease={}",
                    sourceId, SyncStartCoordinator.livePeerSources(sourceId).length,
                    round3(base), round3(peerMax), round3(gap), round3(adjusted),
                    Double.isFinite(base) && Float.isFinite(adjusted) ? round3(adjusted - base) : "nan",
                    SpectralMixConfig.enabled(),
                    round3(SpectralMixConfig.darkSourceCutoff()),
                    round3(SpectralMixConfig.peerClearCutoff()),
                    round3(SpectralMixConfig.minPeerGap()),
                    round3(SpectralMixConfig.clarityFloorRatio()),
                    round3(SpectralMixConfig.maxCutoffLift()));
        }
    }

    private static String round3(double value) {
        return Double.isFinite(value) ? String.format(Locale.ROOT, "%.3f", value) : "nan";
    }
}
