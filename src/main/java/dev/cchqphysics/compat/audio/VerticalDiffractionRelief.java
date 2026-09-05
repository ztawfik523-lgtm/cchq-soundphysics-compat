package dev.cchqphysics.compat.audio;

import dev.cchqphysics.compat.config.ClientConfig;
import dev.cchqphysics.compat.config.DiffractionConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Experimental open-top / steep-elevation diffraction relief.
 *
 * The normal 17 straight rays remain authoritative. Only after they already
 * report severe occlusion does this probe one two-segment route above the lower
 * endpoint. The lower endpoint must have an almost-clear vertical escape leg,
 * which deliberately rejects sealed floors/ceilings.
 */
final class VerticalDiffractionRelief {
    private static final Map<Integer, PhysicalPosition> POSITIONS = new HashMap<>();
    private static final Map<Integer, Snapshot> LAST = new HashMap<>();

    private VerticalDiffractionRelief() {}

    static synchronized void updateSource(int sourceId, double x, double y, double z) {
        POSITIONS.put(sourceId, new PhysicalPosition(x, y, z));
    }

    static synchronized void unregister(int sourceId) {
        POSITIONS.remove(sourceId);
        LAST.remove(sourceId);
    }

    static synchronized void clear() {
        POSITIONS.clear();
        LAST.clear();
    }

    static float[] adjust(int sourceId, float cutoff, float gain) {
        if (!DiffractionConfig.enabled() || !ClientConfig.progressiveOcclusion()) {
            return pair(cutoff, gain);
        }

        final PhysicalPosition physical;
        synchronized (VerticalDiffractionRelief.class) {
            physical = POSITIONS.get(sourceId);
        }
        if (physical == null) {
            record(sourceId, Snapshot.skipped("no-source-position"));
            return pair(cutoff, gain);
        }

        Vec3 listener;
        try {
            listener = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        } catch (Throwable ignored) {
            listener = null;
        }
        if (listener == null) {
            record(sourceId, Snapshot.skipped("no-listener"));
            return pair(cutoff, gain);
        }

        double raw = ProgressiveOcclusionModel.currentRawOcclusion(sourceId);
        double center = ProgressiveOcclusionModel.currentCenterOcclusion(sourceId);
        if (!Double.isFinite(raw) || raw < DiffractionConfig.rawOcclusionGate()) {
            record(sourceId, Snapshot.gated(raw, center, physical, listener, "raw-gate"));
            return pair(cutoff, gain);
        }

        double dx = physical.x - listener.x;
        double dy = physical.y - listener.y;
        double dz = physical.z - listener.z;
        double vertical = Math.abs(dy);
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        double slope = vertical / Math.max(1.0E-6D, horizontal);

        if (vertical < DiffractionConfig.minVerticalSeparation()) {
            record(sourceId, Snapshot.gated(raw, center, physical, listener, "vertical-gate"));
            return pair(cutoff, gain);
        }
        if (horizontal < DiffractionConfig.minHorizontalSeparation()) {
            record(sourceId, Snapshot.gated(raw, center, physical, listener, "near-vertical-safety"));
            return pair(cutoff, gain);
        }
        if (horizontal > DiffractionConfig.maxHorizontalSeparation()) {
            record(sourceId, Snapshot.gated(raw, center, physical, listener, "horizontal-range"));
            return pair(cutoff, gain);
        }
        if (slope < DiffractionConfig.minVerticalHorizontalRatio()) {
            record(sourceId, Snapshot.gated(raw, center, physical, listener, "slope-gate"));
            return pair(cutoff, gain);
        }

        Vec3 source = new Vec3(physical.x, physical.y, physical.z);
        boolean sourceIsLower = physical.y < listener.y;
        Vec3 lower = sourceIsLower ? source : listener;
        Vec3 upper = sourceIsLower ? listener : source;
        double escapeY = Math.max(physical.y, listener.y) + DiffractionConfig.escapeClearance();
        Vec3 waypoint = new Vec3(lower.x, escapeY, lower.z);

        final double verticalLeg;
        try {
            verticalLeg = runOcclusion(lower, waypoint);
        } catch (Throwable throwable) {
            record(sourceId, Snapshot.failed(raw, center, physical, listener, "vertical-ray-failed"));
            return pair(cutoff, gain);
        }

        if (!Double.isFinite(verticalLeg) || verticalLeg > DiffractionConfig.verticalOpenGate()) {
            Snapshot snapshot = Snapshot.gated(raw, center, physical, listener, "sealed-vertical-leg");
            snapshot.verticalLeg = verticalLeg;
            snapshot.escapeY = escapeY;
            record(sourceId, snapshot);
            return pair(cutoff, gain);
        }

        final double crossLeg;
        try {
            crossLeg = runOcclusion(upper, waypoint);
        } catch (Throwable throwable) {
            record(sourceId, Snapshot.failed(raw, center, physical, listener, "cross-ray-failed"));
            return pair(cutoff, gain);
        }

        double candidateRaw = Math.max(0.0D, verticalLeg)
                + Math.max(0.0D, crossLeg)
                + DiffractionConfig.diffractionPenalty();
        double improvement = raw - candidateRaw;
        if (!Double.isFinite(candidateRaw) || improvement < DiffractionConfig.minRawImprovement()) {
            Snapshot snapshot = Snapshot.gated(raw, center, physical, listener, "insufficient-improvement");
            snapshot.verticalLeg = verticalLeg;
            snapshot.crossLeg = crossLeg;
            snapshot.candidateRaw = candidateRaw;
            snapshot.escapeY = escapeY;
            record(sourceId, snapshot);
            return pair(cutoff, gain);
        }

        double rawRatio = clamp01(candidateRaw / Math.max(1.0E-6D, raw));
        float adjustedCutoff = (float) Math.pow(clampFilter(cutoff), rawRatio);
        float adjustedGain = (float) Math.pow(clampFilter(gain), rawRatio);
        adjustedCutoff = Math.max(cutoff, clamp01f(adjustedCutoff));
        adjustedGain = Math.max(gain, clamp01f(adjustedGain));

        Snapshot snapshot = Snapshot.gated(raw, center, physical, listener, "applied");
        snapshot.applied = true;
        snapshot.verticalLeg = verticalLeg;
        snapshot.crossLeg = crossLeg;
        snapshot.candidateRaw = candidateRaw;
        snapshot.escapeY = escapeY;
        snapshot.inputCutoff = cutoff;
        snapshot.outputCutoff = adjustedCutoff;
        snapshot.inputGain = gain;
        snapshot.outputGain = adjustedGain;
        record(sourceId, snapshot);
        return pair(adjustedCutoff, adjustedGain);
    }

    private static double runOcclusion(Vec3 from, Vec3 to) throws Exception {
        PerformanceStats.recordOcclusionPath();
        return Beta10Optimizer.runOcclusionDirect(from, to);
    }

    static synchronized void debugDump() {
        if (LAST.isEmpty()) {
            SoundPhysicsBridge.beta9Log("[phase5/diffraction] no snapshots " + DiffractionConfig.summary());
            return;
        }
        for (Map.Entry<Integer, Snapshot> entry : LAST.entrySet()) {
            Snapshot s = entry.getValue();
            SoundPhysicsBridge.beta9Log("[phase5/diffraction] source=" + entry.getKey()
                    + " reason=" + s.reason
                    + " applied=" + s.applied
                    + " raw=" + r3(s.raw)
                    + " center=" + r3(s.center)
                    + " vertical=" + r3(s.vertical)
                    + " horizontal=" + r3(s.horizontal)
                    + " slope=" + r3(s.slope)
                    + " escapeY=" + r3(s.escapeY)
                    + " verticalLeg=" + r3(s.verticalLeg)
                    + " crossLeg=" + r3(s.crossLeg)
                    + " candidateRaw=" + r3(s.candidateRaw)
                    + " cutoff=" + r3(s.inputCutoff) + "->" + r3(s.outputCutoff)
                    + " gain=" + r3(s.inputGain) + "->" + r3(s.outputGain));
        }
    }

    private static synchronized void record(int sourceId, Snapshot snapshot) {
        LAST.put(sourceId, snapshot);
    }

    private static float[] pair(float cutoff, float gain) {
        return new float[]{cutoff, gain};
    }

    private static double clamp01(double value) {
        return Math.max(0.0D, Math.min(1.0D, value));
    }

    private static float clamp01f(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    private static double clampFilter(float value) {
        return Math.max(1.0E-20D, Math.min(1.0D, value));
    }

    private static String r3(double value) {
        return Double.isFinite(value) ? String.format(Locale.ROOT, "%.3f", value) : "nan";
    }

    private record PhysicalPosition(double x, double y, double z) {}

    private static final class Snapshot {
        String reason;
        boolean applied;
        double raw = Double.NaN;
        double center = Double.NaN;
        double vertical = Double.NaN;
        double horizontal = Double.NaN;
        double slope = Double.NaN;
        double escapeY = Double.NaN;
        double verticalLeg = Double.NaN;
        double crossLeg = Double.NaN;
        double candidateRaw = Double.NaN;
        double inputCutoff = Double.NaN;
        double outputCutoff = Double.NaN;
        double inputGain = Double.NaN;
        double outputGain = Double.NaN;

        static Snapshot skipped(String reason) {
            Snapshot s = new Snapshot();
            s.reason = reason;
            return s;
        }

        static Snapshot failed(double raw, double center, PhysicalPosition source, Vec3 listener, String reason) {
            return gated(raw, center, source, listener, reason);
        }

        static Snapshot gated(double raw, double center, PhysicalPosition source, Vec3 listener, String reason) {
            Snapshot s = new Snapshot();
            s.reason = reason;
            s.raw = raw;
            s.center = center;
            if (source != null && listener != null) {
                double dx = source.x - listener.x;
                double dy = source.y - listener.y;
                double dz = source.z - listener.z;
                s.vertical = Math.abs(dy);
                s.horizontal = Math.sqrt(dx * dx + dz * dz);
                s.slope = s.vertical / Math.max(1.0E-6D, s.horizontal);
            }
            return s;
        }
    }
}
