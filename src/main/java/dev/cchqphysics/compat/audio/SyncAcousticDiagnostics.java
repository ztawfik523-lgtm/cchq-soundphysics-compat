package dev.cchqphysics.compat.audio;

import org.lwjgl.openal.AL10;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Read-only Phase-5 diagnostic for the two remaining synchronized-playback hypotheses:
 * playback-cursor skew and per-source acoustic-send differences.
 *
 * <p>This class never writes source offsets, playback state, gain, position, or EFX.
 * Cursor reads are scheduled onto Minecraft's sound thread by {@link IssueADiagnostics}.</p>
 */
final class SyncAcousticDiagnostics {
    private static final int AL_SEC_OFFSET = 0x1024;
    private static final int AL_SAMPLE_OFFSET = 0x1025;

    private SyncAcousticDiagnostics() {}

    static void dumpPlaybackCursors() {
        int[] sourceIds = EnvironmentSmoother.debugSourceIds();
        if (sourceIds.length == 0) {
            SoundPhysicsBridge.beta9Log("[phase5/syncdiag/timing] sources=0");
            return;
        }

        ArrayList<Integer> ordered = new ArrayList<>(sourceIds.length);
        for (int sourceId : sourceIds) ordered.add(sourceId);
        ordered.sort(Comparator.naturalOrder());

        long beginNs = System.nanoTime();
        Map<Integer, CursorRead> first = readPass(ordered);
        Collections.reverse(ordered);
        Map<Integer, CursorRead> second = readPass(ordered);
        long endNs = System.nanoTime();
        long midpointNs = beginNs + (endNs - beginNs) / 2L;

        ArrayList<CursorEstimate> estimates = new ArrayList<>();
        for (int sourceId : sourceIds) {
            CursorRead a = first.get(sourceId);
            CursorRead b = second.get(sourceId);
            if (a == null || b == null) continue;
            if (a.bufferId != b.bufferId || a.sampleRate != b.sampleRate || a.sampleRate <= 0) continue;

            double frameA = toMidpointFrames(a, midpointNs);
            double frameB = toMidpointFrames(b, midpointNs);
            double secA = toMidpointSeconds(a, midpointNs);
            double secB = toMidpointSeconds(b, midpointNs);
            double estimatedFrame = (frameA + frameB) * 0.5D;
            double estimatedSec = (secA + secB) * 0.5D;
            double rawAverageFrame = (a.sampleOffset + b.sampleOffset) * 0.5D;
            int state = b.state;

            estimates.add(new CursorEstimate(sourceId, state, b.bufferId, b.sampleRate,
                    rawAverageFrame, estimatedFrame, estimatedSec));
        }

        if (estimates.isEmpty()) {
            SoundPhysicsBridge.beta9Log("[phase5/syncdiag/timing] readableSources=0 querySpanUs=" + round3((endNs - beginNs) / 1000.0D));
            return;
        }

        estimates.sort(Comparator.comparingInt(CursorEstimate::sourceId));
        for (CursorEstimate estimate : estimates) {
            SoundPhysicsBridge.beta9Log("[phase5/syncdiag/cursor] source=" + estimate.sourceId
                    + " state=" + stateName(estimate.state)
                    + " buffer=" + estimate.bufferId
                    + " rate=" + estimate.sampleRate
                    + " rawFrame=" + round3(estimate.rawAverageFrame)
                    + " midpointFrame=" + round3(estimate.midpointFrame)
                    + " midpointSec=" + round6(estimate.midpointSec));
        }

        Map<Integer, List<CursorEstimate>> byBuffer = new TreeMap<>();
        for (CursorEstimate estimate : estimates) {
            if (estimate.bufferId == 0) continue;
            if (estimate.state != AL10.AL_PLAYING && estimate.state != AL10.AL_PAUSED) continue;
            byBuffer.computeIfAbsent(estimate.bufferId, ignored -> new ArrayList<>()).add(estimate);
        }

        double querySpanUs = (endNs - beginNs) / 1000.0D;
        for (Map.Entry<Integer, List<CursorEstimate>> entry : byBuffer.entrySet()) {
            List<CursorEstimate> group = entry.getValue();
            if (group.size() < 2) continue;
            int sampleRate = group.get(0).sampleRate;
            boolean sameRate = group.stream().allMatch(sample -> sample.sampleRate == sampleRate);
            if (!sameRate || sampleRate <= 0) continue;

            double minRaw = Double.POSITIVE_INFINITY;
            double maxRaw = Double.NEGATIVE_INFINITY;
            double minMid = Double.POSITIVE_INFINITY;
            double maxMid = Double.NEGATIVE_INFINITY;
            double minSec = Double.POSITIVE_INFINITY;
            double maxSec = Double.NEGATIVE_INFINITY;
            StringBuilder sources = new StringBuilder();
            for (CursorEstimate estimate : group) {
                minRaw = Math.min(minRaw, estimate.rawAverageFrame);
                maxRaw = Math.max(maxRaw, estimate.rawAverageFrame);
                minMid = Math.min(minMid, estimate.midpointFrame);
                maxMid = Math.max(maxMid, estimate.midpointFrame);
                minSec = Math.min(minSec, estimate.midpointSec);
                maxSec = Math.max(maxSec, estimate.midpointSec);
                if (!sources.isEmpty()) sources.append(',');
                sources.append(estimate.sourceId).append(':').append(round3(estimate.midpointFrame));
            }

            double rawSpreadFrames = maxRaw - minRaw;
            double midpointSpreadFrames = maxMid - minMid;
            double midpointSpreadMs = midpointSpreadFrames * 1000.0D / sampleRate;
            double secSpreadMs = (maxSec - minSec) * 1000.0D;
            SoundPhysicsBridge.beta9Log("[phase5/syncdiag/timing] buffer=" + entry.getKey()
                    + " count=" + group.size()
                    + " rate=" + sampleRate
                    + " querySpanUs=" + round3(querySpanUs)
                    + " rawSpreadFrames=" + round3(rawSpreadFrames)
                    + " midpointSpreadFrames=" + round3(midpointSpreadFrames)
                    + " midpointSpreadMs=" + round6(midpointSpreadMs)
                    + " secSpreadMs=" + round6(secSpreadMs)
                    + " sources=" + sources);
        }
    }

    private static Map<Integer, CursorRead> readPass(List<Integer> sourceIds) {
        Map<Integer, CursorRead> reads = new HashMap<>();
        for (int sourceId : sourceIds) {
            try {
                if (!AL10.alIsSource(sourceId)) continue;
                int state = AL10.alGetSourcei(sourceId, AL10.AL_SOURCE_STATE);
                int bufferId = AL10.alGetSourcei(sourceId, AL10.AL_BUFFER);
                int sampleRate = bufferId == 0 ? 0 : AL10.alGetBufferi(bufferId, AL10.AL_FREQUENCY);
                int sampleOffset = AL10.alGetSourcei(sourceId, AL_SAMPLE_OFFSET);
                float secOffset = AL10.alGetSourcef(sourceId, AL_SEC_OFFSET);
                long readNs = System.nanoTime();
                reads.put(sourceId, new CursorRead(sourceId, state, bufferId, sampleRate, sampleOffset, secOffset, readNs));
            } catch (Throwable t) {
                SoundPhysicsBridge.beta9Log("[phase5/syncdiag/cursor] source=" + sourceId + " readFailed=" + t.getClass().getSimpleName());
            }
        }
        return reads;
    }

    private static double toMidpointFrames(CursorRead read, long midpointNs) {
        return read.sampleOffset - ((read.readNs - midpointNs) * (double) read.sampleRate / 1_000_000_000.0D);
    }

    private static double toMidpointSeconds(CursorRead read, long midpointNs) {
        return read.secOffset - ((read.readNs - midpointNs) / 1_000_000_000.0D);
    }

    private static String stateName(int state) {
        if (state == AL10.AL_PLAYING) return "PLAYING";
        if (state == AL10.AL_PAUSED) return "PAUSED";
        if (state == AL10.AL_STOPPED) return "STOPPED";
        if (state == AL10.AL_INITIAL) return "INITIAL";
        return Integer.toString(state);
    }

    private static String round3(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }

    private static String round6(double value) {
        return String.format(Locale.ROOT, "%.6f", value);
    }

    private record CursorRead(int sourceId, int state, int bufferId, int sampleRate,
                              int sampleOffset, float secOffset, long readNs) {}

    private record CursorEstimate(int sourceId, int state, int bufferId, int sampleRate,
                                  double rawAverageFrame, double midpointFrame, double midpointSec) {}
}
