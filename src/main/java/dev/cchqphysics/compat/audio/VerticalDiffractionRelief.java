package dev.cchqphysics.compat.audio;

import dev.cchqphysics.compat.config.ClientConfig;
import dev.cchqphysics.compat.config.DiffractionConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Experimental vertical/opening diffraction relief.
 *
 * V3 behavior is preserved first: severe steep-elevation cases may use the
 * validated direct two-segment open-top route above the lower endpoint.
 *
 * V4 adds one narrow fallback for the case where that direct vertical escape is
 * blocked but a nearby aperture exists. It probes a small set of waypoints on
 * the same escape plane around the lower endpoint. A route is accepted only if
 * the lower-endpoint -> waypoint leg is almost clear and the complete detour is
 * materially better than the normal progressive raw occlusion. The detour gets
 * an explicit distance penalty so an opening helps more as the listener moves
 * closer to it.
 */
final class VerticalDiffractionRelief {
    private static final Map<Integer, PhysicalPosition> POSITIONS = new HashMap<>();
    private static final Map<Integer, Snapshot> LAST = new HashMap<>();
    private static final Map<Integer, OpeningCache> OPENING_CACHE = new HashMap<>();

    private static final int OPENING_DIRECTIONS = 12;

    private VerticalDiffractionRelief() {}

    static synchronized void updateSource(int sourceId, double x, double y, double z) {
        POSITIONS.put(sourceId, new PhysicalPosition(x, y, z));
    }

    static synchronized void unregister(int sourceId) {
        POSITIONS.remove(sourceId);
        LAST.remove(sourceId);
        OPENING_CACHE.remove(sourceId);
    }

    static synchronized void clear() {
        POSITIONS.clear();
        LAST.clear();
        OPENING_CACHE.clear();
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
        if (!Double.isFinite(raw)) {
            record(sourceId, Snapshot.gated(raw, center, physical, listener, "raw-invalid"));
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

        // Preserve the runtime-approved V3 direct open-top route exactly for
        // the geometry it was designed to handle.
        if (raw >= DiffractionConfig.rawOcclusionGate()
                && horizontal >= DiffractionConfig.minHorizontalSeparation()) {
            Vec3 directWaypoint = new Vec3(lower.x, escapeY, lower.z);
            final double directVerticalLeg;
            try {
                directVerticalLeg = runOcclusion(lower, directWaypoint);
            } catch (Throwable throwable) {
                Snapshot snapshot = Snapshot.gated(raw, center, physical, listener, "direct-vertical-ray-failed");
                snapshot.escapeY = escapeY;
                record(sourceId, snapshot);
                return pair(cutoff, gain);
            }

            if (Double.isFinite(directVerticalLeg)
                    && directVerticalLeg <= DiffractionConfig.verticalOpenGate()) {
                final double directCrossLeg;
                try {
                    directCrossLeg = runOcclusion(upper, directWaypoint);
                } catch (Throwable throwable) {
                    Snapshot snapshot = Snapshot.gated(raw, center, physical, listener, "direct-cross-ray-failed");
                    snapshot.escapeY = escapeY;
                    snapshot.verticalLeg = directVerticalLeg;
                    record(sourceId, snapshot);
                    return pair(cutoff, gain);
                }

                double candidateRaw = Math.max(0.0D, directVerticalLeg)
                        + Math.max(0.0D, directCrossLeg)
                        + DiffractionConfig.diffractionPenalty();
                double improvement = raw - candidateRaw;
                if (Double.isFinite(candidateRaw)
                        && improvement >= DiffractionConfig.minRawImprovement()) {
                    return applyCandidate(sourceId, cutoff, gain, raw, center, physical, listener,
                            escapeY, directVerticalLeg, directCrossLeg, candidateRaw,
                            "applied-direct-open-top", 0.0D, 2, false);
                }
            }
        }

        // V4 fallback: when a direct escape above the listener/lower endpoint
        // is blocked, look for a nearby aperture. This deliberately has a lower
        // raw gate because a one-block ceiling can be only moderately occluded.
        if (raw < DiffractionConfig.openingRawOcclusionGate()) {
            record(sourceId, Snapshot.gated(raw, center, physical, listener, "opening-raw-gate"));
            return pair(cutoff, gain);
        }

        OpeningProbe opening = probeNearbyOpening(sourceId, lower, upper, escapeY);
        if (opening == null || !Double.isFinite(opening.candidateRaw)) {
            Snapshot snapshot = Snapshot.gated(raw, center, physical, listener,
                    opening == null ? "nearby-opening-not-found" : opening.reason);
            snapshot.escapeY = escapeY;
            if (opening != null) {
                snapshot.openingRadius = opening.radius;
                snapshot.openingRays = opening.rays;
                snapshot.openingCached = opening.cached;
            }
            record(sourceId, snapshot);
            return pair(cutoff, gain);
        }

        double improvement = raw - opening.candidateRaw;
        if (improvement < DiffractionConfig.openingMinRawImprovement()) {
            Snapshot snapshot = Snapshot.gated(raw, center, physical, listener, "nearby-opening-insufficient-improvement");
            snapshot.escapeY = escapeY;
            snapshot.verticalLeg = opening.lowerLeg;
            snapshot.crossLeg = opening.crossLeg;
            snapshot.candidateRaw = opening.candidateRaw;
            snapshot.openingRadius = opening.radius;
            snapshot.openingRays = opening.rays;
            snapshot.openingCached = opening.cached;
            record(sourceId, snapshot);
            return pair(cutoff, gain);
        }

        return applyCandidate(sourceId, cutoff, gain, raw, center, physical, listener,
                escapeY, opening.lowerLeg, opening.crossLeg, opening.candidateRaw,
                "applied-nearby-opening", opening.radius, opening.rays, opening.cached);
    }

    private static float[] applyCandidate(int sourceId,
                                          float cutoff,
                                          float gain,
                                          double raw,
                                          double center,
                                          PhysicalPosition physical,
                                          Vec3 listener,
                                          double escapeY,
                                          double verticalLeg,
                                          double crossLeg,
                                          double candidateRaw,
                                          String reason,
                                          double openingRadius,
                                          int openingRays,
                                          boolean openingCached) {
        double rawRatio = clamp01(candidateRaw / Math.max(1.0E-6D, raw));
        float adjustedCutoff = (float) Math.pow(clampFilter(cutoff), rawRatio);
        float adjustedGain = (float) Math.pow(clampFilter(gain), rawRatio);
        adjustedCutoff = Math.max(cutoff, clamp01f(adjustedCutoff));
        adjustedGain = Math.max(gain, clamp01f(adjustedGain));

        Snapshot snapshot = Snapshot.gated(raw, center, physical, listener, reason);
        snapshot.applied = true;
        snapshot.verticalLeg = verticalLeg;
        snapshot.crossLeg = crossLeg;
        snapshot.candidateRaw = candidateRaw;
        snapshot.escapeY = escapeY;
        snapshot.openingRadius = openingRadius;
        snapshot.openingRays = openingRays;
        snapshot.openingCached = openingCached;
        snapshot.inputCutoff = cutoff;
        snapshot.outputCutoff = adjustedCutoff;
        snapshot.inputGain = gain;
        snapshot.outputGain = adjustedGain;
        record(sourceId, snapshot);
        return pair(adjustedCutoff, adjustedGain);
    }

    private static OpeningProbe probeNearbyOpening(int sourceId, Vec3 lower, Vec3 upper, double escapeY) {
        long now = System.nanoTime();
        synchronized (VerticalDiffractionRelief.class) {
            OpeningCache cached = OPENING_CACHE.get(sourceId);
            if (cached != null && now - cached.scanNs < DiffractionConfig.openingScanIntervalNs()) {
                return cached.result == null ? null : cached.result.asCached();
            }
        }

        double maxRadius = DiffractionConfig.openingSearchRadius();
        OpeningProbe best = null;
        int rays = 0;

        int wholeRadii = Math.max(1, (int) Math.ceil(maxRadius));
        for (int radiusIndex = 1; radiusIndex <= wholeRadii; radiusIndex++) {
            double radius = Math.min(maxRadius, radiusIndex);
            if (radius <= 0.0D) continue;

            OpeningProbe bestOnRing = null;
            for (int direction = 0; direction < OPENING_DIRECTIONS; direction++) {
                double angle = (Math.PI * 2.0D * direction) / OPENING_DIRECTIONS;
                Vec3 waypoint = new Vec3(
                        lower.x + Math.cos(angle) * radius,
                        escapeY,
                        lower.z + Math.sin(angle) * radius);

                final double lowerLeg;
                try {
                    lowerLeg = runOcclusion(lower, waypoint);
                    rays++;
                } catch (Throwable throwable) {
                    continue;
                }
                if (!Double.isFinite(lowerLeg) || lowerLeg > DiffractionConfig.openingLegGate()) {
                    continue;
                }

                final double crossLeg;
                try {
                    crossLeg = runOcclusion(upper, waypoint);
                    rays++;
                } catch (Throwable throwable) {
                    continue;
                }
                if (!Double.isFinite(crossLeg)) continue;

                double candidateRaw = Math.max(0.0D, lowerLeg)
                        + Math.max(0.0D, crossLeg)
                        + DiffractionConfig.openingBasePenalty()
                        + radius * DiffractionConfig.openingDistancePenalty();
                if (!Double.isFinite(candidateRaw)) continue;

                OpeningProbe candidate = new OpeningProbe(
                        "nearby-opening-candidate", candidateRaw, lowerLeg, crossLeg, radius, rays, false);
                if (bestOnRing == null || candidate.candidateRaw < bestOnRing.candidateRaw) {
                    bestOnRing = candidate;
                }
            }

            if (bestOnRing != null) {
                best = new OpeningProbe(bestOnRing.reason, bestOnRing.candidateRaw,
                        bestOnRing.lowerLeg, bestOnRing.crossLeg, bestOnRing.radius, rays, false);
                // Prefer the nearest ring that has a genuinely open escape. The
                // explicit radius penalty then makes this route stronger as the
                // listener approaches the same aperture.
                break;
            }

            if (radius >= maxRadius) break;
        }

        synchronized (VerticalDiffractionRelief.class) {
            OPENING_CACHE.put(sourceId, new OpeningCache(now, best));
        }
        if (best == null) {
            return new OpeningProbe("nearby-opening-not-found", Double.NaN,
                    Double.NaN, Double.NaN, Double.NaN, rays, false);
        }
        return best;
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
                    + " openingRadius=" + r3(s.openingRadius)
                    + " openingRays=" + s.openingRays
                    + " openingCached=" + s.openingCached
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

    private record OpeningCache(long scanNs, OpeningProbe result) {}

    private record OpeningProbe(String reason,
                                double candidateRaw,
                                double lowerLeg,
                                double crossLeg,
                                double radius,
                                int rays,
                                boolean cached) {
        OpeningProbe asCached() {
            return new OpeningProbe(reason, candidateRaw, lowerLeg, crossLeg, radius, rays, true);
        }
    }

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
        double openingRadius = Double.NaN;
        int openingRays;
        boolean openingCached;
        double inputCutoff = Double.NaN;
        double outputCutoff = Double.NaN;
        double inputGain = Double.NaN;
        double outputGain = Double.NaN;

        static Snapshot skipped(String reason) {
            Snapshot s = new Snapshot();
            s.reason = reason;
            return s;
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
