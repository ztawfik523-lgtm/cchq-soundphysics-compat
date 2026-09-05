package dev.cchqphysics.compat.audio;

import com.sonicether.soundphysics.utils.LevelAccessUtils;
import com.sonicether.soundphysics.world.ClientLevelProxy;
import dev.cchqphysics.compat.config.ClientConfig;
import dev.cchqphysics.compat.config.DiffractionConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Experimental vertical/opening diffraction relief.
 *
 * The runtime-approved V3 direct open-top route is preserved first. V4 adds a
 * performance-conscious nearby-opening fallback for the listener-below-source
 * case. Opening discovery uses only read-only BlockState checks against SPR's
 * cloned level. Expensive SPR occlusion is reserved for at most two candidate
 * openings, with the listener->opening leg shared across sources and both legs
 * cached aggressively.
 */
final class VerticalDiffractionRelief {
    private static final Map<Integer, PhysicalPosition> POSITIONS = new HashMap<>();
    private static final Map<Integer, Snapshot> LAST = new HashMap<>();
    private static final Map<OpeningLegKey, LegCache> LOWER_LEG_CACHE = new HashMap<>();
    private static final Map<CrossLegKey, LegCache> CROSS_LEG_CACHE = new HashMap<>();

    private static final int OPENING_VERIFY_CANDIDATES = 2;
    private static final int OPENING_TOPOLOGY_CANDIDATE_LIMIT = 8;
    private static final int OPENING_MAX_CONFIG_RADIUS = 8;
    private static final int OPENING_VERTICAL_SCAN_BLOCKS = 8;
    private static final double SOURCE_RECHECK_DISTANCE_SQ = 0.01D;
    private static final Offset[] OPENING_OFFSETS = buildOffsets(OPENING_MAX_CONFIG_RADIUS);

    private static OpeningTopology topologyCache;

    private VerticalDiffractionRelief() {}

    static synchronized void updateSource(int sourceId, double x, double y, double z) {
        POSITIONS.put(sourceId, new PhysicalPosition(x, y, z));
    }

    static synchronized void unregister(int sourceId) {
        POSITIONS.remove(sourceId);
        LAST.remove(sourceId);
        CROSS_LEG_CACHE.entrySet().removeIf(entry -> entry.getKey().sourceId == sourceId);
    }

    static synchronized void clear() {
        POSITIONS.clear();
        LAST.clear();
        LOWER_LEG_CACHE.clear();
        CROSS_LEG_CACHE.clear();
        topologyCache = null;
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

        // A cheap shared roof scan can prove that the V3 direct vertical leg
        // is blocked before we spend an SPR occlusion call on it. Open-top V3
        // behavior is unchanged because no barrier is found in that geometry.
        OpeningTopology listenerTopology = sourceIsLower ? null : getOpeningTopology(listener);
        boolean directKnownBlocked = listenerTopology != null
                && listenerTopology.barrierY != Integer.MIN_VALUE
                && source.y > listenerTopology.barrierY + 0.5D;

        // Preserve the runtime-approved V3 direct open-top route exactly for
        // the geometry it was designed to handle.
        if (!directKnownBlocked
                && raw >= DiffractionConfig.rawOcclusionGate()
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
                            "applied-direct-open-top", 0.0D, 0, 2, false);
                }
            }
        }

        // Nearby-opening leakage is deliberately listener-side only. This keeps
        // the new behavior narrow: listener below a speaker with a nearby roof
        // aperture. The already validated V3 route remains symmetric above.
        if (sourceIsLower) {
            record(sourceId, Snapshot.gated(raw, center, physical, listener, "opening-listener-not-lower"));
            return pair(cutoff, gain);
        }
        if (raw < DiffractionConfig.openingRawOcclusionGate()) {
            record(sourceId, Snapshot.gated(raw, center, physical, listener, "opening-raw-gate"));
            return pair(cutoff, gain);
        }

        OpeningProbe opening = probeNearbyOpening(sourceId, listener, source);
        if (opening == null || !Double.isFinite(opening.candidateRaw)) {
            Snapshot snapshot = Snapshot.gated(raw, center, physical, listener,
                    opening == null ? "nearby-opening-not-found" : opening.reason);
            if (opening != null) {
                snapshot.escapeY = opening.waypointY;
                snapshot.openingRadius = opening.radius;
                snapshot.openingBlockChecks = opening.blockChecks;
                snapshot.openingSprRays = opening.sprRays;
                snapshot.openingCached = opening.cached;
            }
            record(sourceId, snapshot);
            return pair(cutoff, gain);
        }

        double improvement = raw - opening.candidateRaw;
        if (improvement < DiffractionConfig.openingMinRawImprovement()) {
            Snapshot snapshot = Snapshot.gated(raw, center, physical, listener, "nearby-opening-insufficient-improvement");
            snapshot.escapeY = opening.waypointY;
            snapshot.verticalLeg = opening.lowerLeg;
            snapshot.crossLeg = opening.crossLeg;
            snapshot.candidateRaw = opening.candidateRaw;
            snapshot.openingRadius = opening.radius;
            snapshot.openingBlockChecks = opening.blockChecks;
            snapshot.openingSprRays = opening.sprRays;
            snapshot.openingCached = opening.cached;
            record(sourceId, snapshot);
            return pair(cutoff, gain);
        }

        return applyCandidate(sourceId, cutoff, gain, raw, center, physical, listener,
                opening.waypointY, opening.lowerLeg, opening.crossLeg, opening.candidateRaw,
                "applied-nearby-opening-v5", opening.radius, opening.blockChecks,
                opening.sprRays, opening.cached);
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
                                          int openingBlockChecks,
                                          int openingSprRays,
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
        snapshot.openingBlockChecks = openingBlockChecks;
        snapshot.openingSprRays = openingSprRays;
        snapshot.openingCached = openingCached;
        snapshot.inputCutoff = cutoff;
        snapshot.outputCutoff = adjustedCutoff;
        snapshot.inputGain = gain;
        snapshot.outputGain = adjustedGain;
        record(sourceId, snapshot);
        return pair(adjustedCutoff, adjustedGain);
    }

    private static OpeningProbe probeNearbyOpening(int sourceId, Vec3 listener, Vec3 source) {
        OpeningTopology topology = getOpeningTopology(listener);
        if (topology == null) {
            return new OpeningProbe("nearby-opening-no-level-proxy", Double.NaN,
                    Double.NaN, Double.NaN, Double.NaN, Double.NaN,
                    0, 0, false);
        }
        if (topology.barrierY == Integer.MIN_VALUE) {
            return new OpeningProbe("nearby-opening-no-ceiling-barrier", Double.NaN,
                    Double.NaN, Double.NaN, Double.NaN, Double.NaN,
                    topology.blockChecks, 0, topology.cached);
        }
        if (source.y <= topology.barrierY + 0.5D) {
            return new OpeningProbe("nearby-opening-barrier-not-between", Double.NaN,
                    Double.NaN, Double.NaN, Double.NaN, topology.barrierY + DiffractionConfig.escapeClearance(),
                    topology.blockChecks, 0, topology.cached);
        }
        if (topology.candidates.length == 0) {
            return new OpeningProbe("nearby-opening-not-found", Double.NaN,
                    Double.NaN, Double.NaN, Double.NaN, topology.barrierY + DiffractionConfig.escapeClearance(),
                    topology.blockChecks, 0, topology.cached);
        }

        int[] selected = selectCandidates(topology.candidates, listener, source);
        OpeningProbe best = null;
        int sprRays = 0;
        boolean allCached = topology.cached;

        for (int index : selected) {
            if (index < 0) continue;
            OpeningCandidate candidate = topology.candidates[index];
            // V5: verify each side of the aperture without forcing a diagonal ray through the roof plane.
            double lowerWaypointY = topology.barrierY - 0.25D;
            double upperWaypointY = topology.barrierY + Math.max(1.0D, DiffractionConfig.escapeClearance());
            Vec3 lowerWaypoint = new Vec3(candidate.x + 0.5D, lowerWaypointY, candidate.z + 0.5D);
            Vec3 upperWaypoint = new Vec3(candidate.x + 0.5D, upperWaypointY, candidate.z + 0.5D);
            double radius = horizontalDistance(listener, lowerWaypoint);

            LegSample lowerLeg;
            try {
                lowerLeg = sampleLowerLeg(candidate, topology.barrierY, listener, lowerWaypoint);
            } catch (Throwable throwable) {
                continue;
            }
            if (!lowerLeg.cached) sprRays++;
            allCached &= lowerLeg.cached;
            if (!Double.isFinite(lowerLeg.value)
                    || lowerLeg.value > DiffractionConfig.openingLegGate()) {
                continue;
            }

            LegSample crossLeg;
            try {
                crossLeg = sampleCrossLeg(sourceId, candidate, topology.barrierY, source, upperWaypoint);
            } catch (Throwable throwable) {
                continue;
            }
            if (!crossLeg.cached) sprRays++;
            allCached &= crossLeg.cached;
            if (!Double.isFinite(crossLeg.value)) continue;

            double candidateRaw = Math.max(0.0D, lowerLeg.value)
                    + Math.max(0.0D, crossLeg.value)
                    + DiffractionConfig.openingBasePenalty()
                    + radius * DiffractionConfig.openingDistancePenalty();
            if (!Double.isFinite(candidateRaw)) continue;

            OpeningProbe probe = new OpeningProbe("nearby-opening-candidate-v5", candidateRaw,
                    lowerLeg.value, crossLeg.value, radius, upperWaypointY,
                    topology.blockChecks, sprRays, allCached);
            if (best == null || probe.candidateRaw < best.candidateRaw) {
                best = probe;
            }
        }

        if (best == null) {
            return new OpeningProbe("nearby-opening-unverified", Double.NaN,
                    Double.NaN, Double.NaN, Double.NaN,
                    topology.barrierY + DiffractionConfig.escapeClearance(),
                    topology.blockChecks, sprRays, allCached);
        }
        return best;
    }

    private static OpeningTopology getOpeningTopology(Vec3 listener) {
        long now = System.nanoTime();
        int baseX = floor(listener.x);
        int baseY = floor(listener.y);
        int baseZ = floor(listener.z);
        int startY = baseY + 1;
        int scanTopY = baseY + OPENING_VERTICAL_SCAN_BLOCKS;

        synchronized (VerticalDiffractionRelief.class) {
            OpeningTopology cached = topologyCache;
            if (cached != null
                    && cached.baseX == baseX
                    && cached.baseY == baseY
                    && cached.baseZ == baseZ
                    && cached.scanTopY == scanTopY
                    && now - cached.scanNs < DiffractionConfig.openingScanIntervalNs()) {
                return cached.asCached();
            }
        }

        final ClientLevelProxy level;
        try {
            level = LevelAccessUtils.getClientLevelProxy(Minecraft.getInstance());
        } catch (Throwable ignored) {
            return null;
        }
        if (level == null) return null;

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int blockChecks = 0;
        int barrierY = Integer.MIN_VALUE;
        for (int y = startY; y <= scanTopY; y++) {
            blockChecks++;
            if (!isAir(level, cursor, baseX, y, baseZ)) {
                barrierY = y;
                break;
            }
        }

        if (barrierY == Integer.MIN_VALUE) {
            OpeningTopology result = new OpeningTopology(baseX, baseY, baseZ, scanTopY,
                    now, barrierY, new OpeningCandidate[0], blockChecks, false);
            synchronized (VerticalDiffractionRelief.class) {
                topologyCache = result;
            }
            return result;
        }

        double maxRadius = DiffractionConfig.openingSearchRadius();
        List<OpeningCandidate> candidates = new ArrayList<>(OPENING_TOPOLOGY_CANDIDATE_LIMIT);
        for (Offset offset : OPENING_OFFSETS) {
            int x = baseX + offset.dx;
            int z = baseZ + offset.dz;
            double cx = x + 0.5D;
            double cz = z + 0.5D;
            double radius = Math.sqrt((cx - listener.x) * (cx - listener.x)
                    + (cz - listener.z) * (cz - listener.z));
            if (radius > maxRadius) continue;

            blockChecks++;
            if (!isAir(level, cursor, x, barrierY, z)) continue;
            blockChecks++;
            if (!isAir(level, cursor, x, barrierY + 1, z)) continue;

            candidates.add(new OpeningCandidate(x, z, radius));
            if (candidates.size() >= OPENING_TOPOLOGY_CANDIDATE_LIMIT) break;
        }
        candidates.sort(Comparator.comparingDouble(OpeningCandidate::radius));

        OpeningTopology result = new OpeningTopology(baseX, baseY, baseZ, scanTopY,
                now, barrierY, candidates.toArray(new OpeningCandidate[0]), blockChecks, false);
        synchronized (VerticalDiffractionRelief.class) {
            topologyCache = result;
            if (LOWER_LEG_CACHE.size() > 64) LOWER_LEG_CACHE.clear();
            if (CROSS_LEG_CACHE.size() > 256) CROSS_LEG_CACHE.clear();
        }
        return result;
    }

    private static boolean isAir(ClientLevelProxy level,
                                 BlockPos.MutableBlockPos cursor,
                                 int x,
                                 int y,
                                 int z) {
        cursor.set(x, y, z);
        return level.getBlockState(cursor).isAir();
    }

    private static int[] selectCandidates(OpeningCandidate[] candidates, Vec3 listener, Vec3 source) {
        int first = -1;
        int second = -1;
        double firstScore = Double.POSITIVE_INFINITY;
        double secondScore = Double.POSITIVE_INFINITY;

        for (int i = 0; i < candidates.length; i++) {
            OpeningCandidate candidate = candidates[i];
            double cx = candidate.x + 0.5D;
            double cz = candidate.z + 0.5D;
            double listenerDistance = Math.sqrt((cx - listener.x) * (cx - listener.x)
                    + (cz - listener.z) * (cz - listener.z));
            double sourceDistance = Math.sqrt((cx - source.x) * (cx - source.x)
                    + (cz - source.z) * (cz - source.z));
            double score = listenerDistance + sourceDistance * 0.10D;
            if (score < firstScore) {
                second = first;
                secondScore = firstScore;
                first = i;
                firstScore = score;
            } else if (score < secondScore) {
                second = i;
                secondScore = score;
            }
        }

        return OPENING_VERIFY_CANDIDATES <= 1 ? new int[]{first} : new int[]{first, second};
    }

    private static LegSample sampleLowerLeg(OpeningCandidate candidate,
                                            int barrierY,
                                            Vec3 listener,
                                            Vec3 waypoint) throws Exception {
        long now = System.nanoTime();
        OpeningLegKey key = new OpeningLegKey(candidate.x, barrierY, candidate.z);
        synchronized (VerticalDiffractionRelief.class) {
            LegCache cached = LOWER_LEG_CACHE.get(key);
            if (cached != null
                    && now - cached.verifiedNs < DiffractionConfig.openingRayCacheNs()
                    && distanceSq(listener.x, listener.y, listener.z,
                    cached.fromX, cached.fromY, cached.fromZ)
                    <= DiffractionConfig.openingLegRecheckDistanceSq()
                    && distanceSq(waypoint.x, waypoint.y, waypoint.z,
                    cached.toX, cached.toY, cached.toZ) <= 1.0E-6D) {
                return new LegSample(cached.value, true);
            }
        }

        double value = runOcclusion(listener, waypoint);
        synchronized (VerticalDiffractionRelief.class) {
            LOWER_LEG_CACHE.put(key, new LegCache(now,
                    listener.x, listener.y, listener.z,
                    waypoint.x, waypoint.y, waypoint.z,
                    value));
        }
        return new LegSample(value, false);
    }

    private static LegSample sampleCrossLeg(int sourceId,
                                            OpeningCandidate candidate,
                                            int barrierY,
                                            Vec3 source,
                                            Vec3 waypoint) throws Exception {
        long now = System.nanoTime();
        CrossLegKey key = new CrossLegKey(sourceId, candidate.x, barrierY, candidate.z);
        synchronized (VerticalDiffractionRelief.class) {
            LegCache cached = CROSS_LEG_CACHE.get(key);
            if (cached != null
                    && now - cached.verifiedNs < DiffractionConfig.openingRayCacheNs()
                    && distanceSq(source.x, source.y, source.z,
                    cached.fromX, cached.fromY, cached.fromZ) <= SOURCE_RECHECK_DISTANCE_SQ
                    && distanceSq(waypoint.x, waypoint.y, waypoint.z,
                    cached.toX, cached.toY, cached.toZ) <= 1.0E-6D) {
                return new LegSample(cached.value, true);
            }
        }

        double value = runOcclusion(source, waypoint);
        synchronized (VerticalDiffractionRelief.class) {
            CROSS_LEG_CACHE.put(key, new LegCache(now,
                    source.x, source.y, source.z,
                    waypoint.x, waypoint.y, waypoint.z,
                    value));
        }
        return new LegSample(value, false);
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
                    + " openingBlockChecks=" + s.openingBlockChecks
                    + " openingSprRays=" + s.openingSprRays
                    + " openingCached=" + s.openingCached
                    + " cutoff=" + r3(s.inputCutoff) + "->" + r3(s.outputCutoff)
                    + " gain=" + r3(s.inputGain) + "->" + r3(s.outputGain));
        }
    }

    private static synchronized void record(int sourceId, Snapshot snapshot) {
        LAST.put(sourceId, snapshot);
    }

    private static Offset[] buildOffsets(int radius) {
        List<Offset> offsets = new ArrayList<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx == 0 && dz == 0) continue;
                int distanceSq = dx * dx + dz * dz;
                if (distanceSq > radius * radius) continue;
                offsets.add(new Offset(dx, dz, distanceSq));
            }
        }
        offsets.sort(Comparator.comparingInt(Offset::distanceSq));
        return offsets.toArray(new Offset[0]);
    }

    private static double horizontalDistance(Vec3 a, Vec3 b) {
        double dx = a.x - b.x;
        double dz = a.z - b.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    private static double distanceSq(double ax, double ay, double az,
                                     double bx, double by, double bz) {
        double dx = ax - bx;
        double dy = ay - by;
        double dz = az - bz;
        return dx * dx + dy * dy + dz * dz;
    }

    private static int floor(double value) {
        return (int) Math.floor(value);
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

    private record Offset(int dx, int dz, int distanceSq) {}

    private record OpeningCandidate(int x, int z, double radius) {}

    private record OpeningLegKey(int x, int y, int z) {}

    private record CrossLegKey(int sourceId, int x, int y, int z) {}

    private record LegCache(long verifiedNs,
                            double fromX,
                            double fromY,
                            double fromZ,
                            double toX,
                            double toY,
                            double toZ,
                            double value) {}

    private record LegSample(double value, boolean cached) {}

    private record OpeningTopology(int baseX,
                                   int baseY,
                                   int baseZ,
                                   int scanTopY,
                                   long scanNs,
                                   int barrierY,
                                   OpeningCandidate[] candidates,
                                   int blockChecks,
                                   boolean cached) {
        OpeningTopology asCached() {
            return new OpeningTopology(baseX, baseY, baseZ, scanTopY, scanNs,
                    barrierY, candidates, 0, true);
        }
    }

    private record OpeningProbe(String reason,
                                double candidateRaw,
                                double lowerLeg,
                                double crossLeg,
                                double radius,
                                double waypointY,
                                int blockChecks,
                                int sprRays,
                                boolean cached) {}

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
        int openingBlockChecks;
        int openingSprRays;
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
