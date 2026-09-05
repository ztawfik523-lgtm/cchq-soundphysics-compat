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
 * Experimental V7.1 spreading-only opening diffraction model.
 *
 * Unlike V3-V5, an alternate opening route never replaces the normal direct
 * occlusion result. A verified aperture contributes a bounded secondary energy
 * path. Path-length difference controls a low/high diffraction loss while the
 * two SPR legs retain their own obstruction loss. The contributions are energy
 * combined without adding playback sources, phase offsets, timing changes, or
 * position changes.
 */
final class VerticalDiffractionRelief {
    private static final Map<Integer, PhysicalPosition> POSITIONS = new HashMap<>();
    private static final Map<Integer, Snapshot> LAST = new HashMap<>();
    private static final Map<OpeningLegKey, LegCache> LOWER_LEG_CACHE = new HashMap<>();
    private static final Map<CrossLegKey, LegCache> CROSS_LEG_CACHE = new HashMap<>();
    private static final Map<Integer, CandidateKey> PRIMARY_BY_SOURCE = new HashMap<>();

    private static final int OPENING_VERIFY_CANDIDATES = 2;
    private static final int OPENING_MAX_CONFIG_RADIUS = 8;
    private static final int OPENING_VERTICAL_SCAN_BLOCKS = 8;
    private static final int LOWER_CACHE_SOFT_LIMIT = 128;
    private static final int CROSS_CACHE_SOFT_LIMIT = 512;
    private static final long CACHE_PRUNE_INTERVAL_NS = 1_000_000_000L;
    private static final double SOURCE_RECHECK_DISTANCE_SQ = 0.01D;
    private static final Offset[] OPENING_OFFSETS = buildOffsets(OPENING_MAX_CONFIG_RADIUS);

    private static OpeningTopology topologyCache;
    private static long lastPruneNs;

    private VerticalDiffractionRelief() {}

    static synchronized void updateSource(int sourceId, double x, double y, double z) {
        POSITIONS.put(sourceId, new PhysicalPosition(x, y, z));
    }

    static synchronized void unregister(int sourceId) {
        POSITIONS.remove(sourceId);
        LAST.remove(sourceId);
        PRIMARY_BY_SOURCE.remove(sourceId);
        CROSS_LEG_CACHE.entrySet().removeIf(entry -> entry.getKey().sourceId == sourceId);
    }

    static synchronized void clear() {
        POSITIONS.clear();
        LAST.clear();
        LOWER_LEG_CACHE.clear();
        CROSS_LEG_CACHE.clear();
        PRIMARY_BY_SOURCE.clear();
        topologyCache = null;
        lastPruneNs = 0L;
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

        final Vec3 listener;
        try {
            listener = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        } catch (Throwable ignored) {
            record(sourceId, Snapshot.skipped("no-listener"));
            return pair(cutoff, gain);
        }
        if (listener == null) {
            record(sourceId, Snapshot.skipped("no-listener"));
            return pair(cutoff, gain);
        }

        double raw = ProgressiveOcclusionModel.currentRawOcclusion(sourceId);
        double center = ProgressiveOcclusionModel.currentCenterOcclusion(sourceId);
        Vec3 source = new Vec3(physical.x, physical.y, physical.z);
        double vertical = physical.y - listener.y;
        double horizontal = horizontalDistance(source, listener);
        if (!Double.isFinite(raw) || raw <= 1.0E-6D) {
            record(sourceId, Snapshot.gated(raw, center, vertical, horizontal, "raw-clear"));
            return pair(cutoff, gain);
        }
        if (vertical <= DiffractionConfig.minSourceAboveListener()) {
            record(sourceId, Snapshot.gated(raw, center, vertical, horizontal, "source-not-above-listener"));
            return pair(cutoff, gain);
        }

        OpeningTopology topology = getOpeningTopology(listener);
        if (topology == null) {
            record(sourceId, Snapshot.gated(raw, center, vertical, horizontal, "portal-no-level-proxy"));
            return pair(cutoff, gain);
        }

        List<PortalCandidate> candidates = buildPortalCandidates(topology, listener, source);
        if (candidates.isEmpty()) {
            Snapshot snapshot = Snapshot.gated(raw, center, vertical, horizontal,
                    topology.barrierY == Integer.MIN_VALUE ? "portal-open-top-unavailable" : "portal-opening-not-found");
            snapshot.blockChecks = topology.blockChecks;
            snapshot.cached = topology.cached;
            record(sourceId, snapshot);
            return pair(cutoff, gain);
        }

        PortalCandidate[] selected = selectCandidates(sourceId, candidates);
        List<VerifiedPortal> verified = new ArrayList<>(OPENING_VERIFY_CANDIDATES);
        int sprRays = 0;
        boolean allCached = topology.cached;

        for (PortalCandidate candidate : selected) {
            if (candidate == null || candidate.horizonFade <= 1.0E-5D) continue;

            final LegSample lower;
            final LegSample upper;
            try {
                lower = sampleLowerLeg(candidate, listener);
                if (!lower.cached) sprRays++;
                upper = sampleCrossLeg(sourceId, candidate, source);
                if (!upper.cached) sprRays++;
            } catch (Throwable ignored) {
                continue;
            }
            allCached &= lower.cached && upper.cached;
            if (!Double.isFinite(lower.value) || !Double.isFinite(upper.value)) continue;

            double legRaw = Math.max(0.0D, lower.value) + Math.max(0.0D, upper.value);
            double rawRatio = Math.max(0.0D, legRaw / Math.max(1.0E-6D, raw));
            double pathGain = Math.pow(clampFilter(gain), rawRatio);
            double pathCutoff = Math.pow(clampFilter(cutoff), rawRatio);

            double lowDiff = diffractionStrength(candidate.delta, DiffractionConfig.lowDeltaScale());
            double highDiff = diffractionStrength(candidate.delta, DiffractionConfig.highDeltaScale());
            double spread = apertureSpreading(candidate.apertureDistance, DiffractionConfig.apertureSpreadScale());
            double activation = smoothStep(clamp01(raw / Math.max(1.0E-6D, DiffractionConfig.portalActivationRaw())));
            double coupling = DiffractionConfig.portalCoupling() * activation * candidate.horizonFade * spread;

            double lowAmplitude = coupling * pathGain * lowDiff;
            double highAmplitude = coupling * pathGain * pathCutoff * highDiff;
            double quality = candidate.horizonFade * spread / (1.0D + candidate.delta + 0.5D * legRaw);

            verified.add(new VerifiedPortal(candidate, lower.value, upper.value, legRaw,
                    lowDiff, highDiff, spread, lowAmplitude, highAmplitude,
                    Math.max(1.0E-9D, quality), lower.cached && upper.cached));
        }

        if (verified.isEmpty()) {
            Snapshot snapshot = Snapshot.gated(raw, center, vertical, horizontal, "portal-unverified");
            snapshot.blockChecks = topology.blockChecks;
            snapshot.sprRays = sprRays;
            snapshot.cached = allCached;
            record(sourceId, snapshot);
            return pair(cutoff, gain);
        }

        double qualitySum = 0.0D;
        for (VerifiedPortal portal : verified) qualitySum += portal.quality;

        double portalLowEnergy = 0.0D;
        double portalHighEnergy = 0.0D;
        for (VerifiedPortal portal : verified) {
            double weight = portal.quality / Math.max(1.0E-12D, qualitySum);
            portalLowEnergy += weight * portal.lowAmplitude * portal.lowAmplitude;
            portalHighEnergy += weight * portal.highAmplitude * portal.highAmplitude;
        }

        double directLow = clamp01(gain);
        double directHigh = directLow * clamp01(cutoff);
        double combinedLow = Math.sqrt(directLow * directLow + portalLowEnergy);
        double combinedHigh = Math.sqrt(directHigh * directHigh + portalHighEnergy);

        float adjustedGain = Math.max(gain, clamp01f((float) combinedLow));
        float adjustedCutoff = Math.max(cutoff,
                clamp01f((float) (combinedHigh / Math.max(1.0E-12D, combinedLow))));

        VerifiedPortal best = verified.stream()
                .max(Comparator.comparingDouble(VerifiedPortal::quality))
                .orElse(verified.get(0));
        Snapshot snapshot = Snapshot.gated(raw, center, vertical, horizontal,
                adjustedGain > gain + 1.0E-4F || adjustedCutoff > cutoff + 1.0E-4F
                        ? "applied-portal-energy-v7-1" : "portal-energy-negligible");
        snapshot.applied = adjustedGain > gain + 1.0E-4F || adjustedCutoff > cutoff + 1.0E-4F;
        snapshot.directDistance = best.candidate.directDistance;
        snapshot.routeDistance = best.candidate.routeDistance;
        snapshot.delta = best.candidate.delta;
        snapshot.openingRadius = best.candidate.radius;
        snapshot.lowerLeg = best.lowerLeg;
        snapshot.crossLeg = best.upperLeg;
        snapshot.lowDiff = best.lowDiff;
        snapshot.highDiff = best.highDiff;
        snapshot.spread = best.spread;
        snapshot.portalLowAmplitude = Math.sqrt(portalLowEnergy);
        snapshot.portalHighAmplitude = Math.sqrt(portalHighEnergy);
        snapshot.candidateCount = candidates.size();
        snapshot.verifiedCount = verified.size();
        snapshot.blockChecks = topology.blockChecks;
        snapshot.sprRays = sprRays;
        snapshot.cached = allCached;
        snapshot.inputCutoff = cutoff;
        snapshot.outputCutoff = adjustedCutoff;
        snapshot.inputGain = gain;
        snapshot.outputGain = adjustedGain;
        record(sourceId, snapshot);
        return pair(adjustedCutoff, adjustedGain);
    }

    private static List<PortalCandidate> buildPortalCandidates(OpeningTopology topology,
                                                                Vec3 listener,
                                                                Vec3 source) {
        double directDistance = source.distanceTo(listener);

        if (topology.barrierY == Integer.MIN_VALUE) {
            List<PortalCandidate> result = new ArrayList<>(1);
            // Open-top geometry is represented as an implicit aperture in the
            // listener's vertical column. It uses the same energy model as all
            // explicit roof openings; there is no separate acoustic shortcut.
            double y = Math.max(source.y, listener.y) + DiffractionConfig.escapeClearance();
            Vec3 waypoint = new Vec3(listener.x, y, listener.z);
            double route = listener.distanceTo(waypoint) + source.distanceTo(waypoint);
            double delta = Math.max(0.0D, route - directDistance);
            CandidateKey key = new CandidateKey(floor(listener.x), floor(y), floor(listener.z), true);
            result.add(new PortalCandidate(key, waypoint, waypoint, 0.0D, 0.0D,
                    directDistance, route, delta, 1.0D));
            return result;
        }

        if (source.y <= topology.barrierY + 0.5D || topology.candidates.length == 0) {
            return List.of();
        }

        List<PortalCandidate> result = new ArrayList<>(topology.candidates.length);
        double lowerY = topology.barrierY - 0.25D;
        double upperY = topology.barrierY + Math.max(1.0D, DiffractionConfig.escapeClearance());
        for (OpeningCandidate opening : topology.candidates) {
            Vec3 lowerWaypoint = new Vec3(opening.x + 0.5D, lowerY, opening.z + 0.5D);
            Vec3 upperWaypoint = new Vec3(opening.x + 0.5D, upperY, opening.z + 0.5D);
            double route = listener.distanceTo(lowerWaypoint)
                    + lowerWaypoint.distanceTo(upperWaypoint)
                    + source.distanceTo(upperWaypoint);
            double delta = Math.max(0.0D, route - directDistance);
            double fade = horizonFade(opening.radius, DiffractionConfig.openingSearchRadius(),
                    DiffractionConfig.horizonFadeStartRatio());
            if (fade <= 1.0E-5D) continue;
            CandidateKey key = new CandidateKey(opening.x, topology.barrierY, opening.z, false);
            result.add(new PortalCandidate(key, lowerWaypoint, upperWaypoint, opening.radius,
                    listener.distanceTo(lowerWaypoint), directDistance, route, delta, fade));
        }
        return result;
    }

    private static PortalCandidate[] selectCandidates(int sourceId, List<PortalCandidate> candidates) {
        if (candidates.isEmpty()) return new PortalCandidate[0];
        candidates.sort(Comparator.comparingDouble(VerticalDiffractionRelief::cheapScore));

        PortalCandidate first = candidates.get(0);
        CandidateKey previous;
        synchronized (VerticalDiffractionRelief.class) {
            previous = PRIMARY_BY_SOURCE.get(sourceId);
        }
        if (previous != null) {
            for (PortalCandidate candidate : candidates) {
                if (!candidate.key.equals(previous)) continue;
                if (cheapScore(candidate) <= cheapScore(first) + DiffractionConfig.selectionHysteresis()) {
                    first = candidate;
                }
                break;
            }
        }

        PortalCandidate second = null;
        double minSeparationSq = DiffractionConfig.candidateSeparationSq();
        for (PortalCandidate candidate : candidates) {
            if (candidate == first) continue;
            if (horizontalDistanceSq(candidate.lowerWaypoint, first.lowerWaypoint) < minSeparationSq) continue;
            second = candidate;
            break;
        }

        synchronized (VerticalDiffractionRelief.class) {
            PRIMARY_BY_SOURCE.put(sourceId, first.key);
        }
        return second == null || OPENING_VERIFY_CANDIDATES <= 1
                ? new PortalCandidate[]{first}
                : new PortalCandidate[]{first, second};
    }

    private static double cheapScore(PortalCandidate candidate) {
        // Path-length difference is the primary geometric term. The tiny fade
        // term only avoids preferring a candidate at the artificial scan edge.
        return candidate.delta + (1.0D - candidate.horizonFade) * 0.25D;
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

        List<OpeningCandidate> openings = new ArrayList<>();
        if (barrierY != Integer.MIN_VALUE) {
            double maxRadius = Math.min(OPENING_MAX_CONFIG_RADIUS, DiffractionConfig.openingSearchRadius());
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
                openings.add(new OpeningCandidate(x, z, radius));
            }
        }

        OpeningTopology result = new OpeningTopology(baseX, baseY, baseZ, scanTopY,
                now, barrierY, openings.toArray(new OpeningCandidate[0]), blockChecks, false);
        PerformanceStats.recordPortalTopologyScan(blockChecks);
        synchronized (VerticalDiffractionRelief.class) {
            topologyCache = result;
            maybePruneCaches(now);
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

    private static LegSample sampleLowerLeg(PortalCandidate candidate, Vec3 listener) throws Exception {
        long now = System.nanoTime();
        OpeningLegKey key = new OpeningLegKey(candidate.key.x, candidate.key.y,
                candidate.key.z, candidate.key.implicit);
        synchronized (VerticalDiffractionRelief.class) {
            LegCache cached = LOWER_LEG_CACHE.get(key);
            if (cached != null
                    && now - cached.verifiedNs < DiffractionConfig.openingRayCacheNs()
                    && distanceSq(listener.x, listener.y, listener.z,
                    cached.fromX, cached.fromY, cached.fromZ)
                    <= DiffractionConfig.openingLegRecheckDistanceSq()
                    && distanceSq(candidate.lowerWaypoint.x, candidate.lowerWaypoint.y, candidate.lowerWaypoint.z,
                    cached.toX, cached.toY, cached.toZ) <= 1.0E-6D) {
                return new LegSample(cached.value, true);
            }
        }

        double value = runOcclusion(listener, candidate.lowerWaypoint, true);
        synchronized (VerticalDiffractionRelief.class) {
            LOWER_LEG_CACHE.put(key, new LegCache(now,
                    listener.x, listener.y, listener.z,
                    candidate.lowerWaypoint.x, candidate.lowerWaypoint.y, candidate.lowerWaypoint.z,
                    value));
        }
        return new LegSample(value, false);
    }

    private static LegSample sampleCrossLeg(int sourceId,
                                            PortalCandidate candidate,
                                            Vec3 source) throws Exception {
        long now = System.nanoTime();
        CrossLegKey key = new CrossLegKey(sourceId, candidate.key.x, candidate.key.y,
                candidate.key.z, candidate.key.implicit);
        synchronized (VerticalDiffractionRelief.class) {
            LegCache cached = CROSS_LEG_CACHE.get(key);
            if (cached != null
                    && now - cached.verifiedNs < DiffractionConfig.openingRayCacheNs()
                    && distanceSq(source.x, source.y, source.z,
                    cached.fromX, cached.fromY, cached.fromZ) <= SOURCE_RECHECK_DISTANCE_SQ
                    && distanceSq(candidate.upperWaypoint.x, candidate.upperWaypoint.y, candidate.upperWaypoint.z,
                    cached.toX, cached.toY, cached.toZ) <= 1.0E-6D) {
                return new LegSample(cached.value, true);
            }
        }

        double value = runOcclusion(source, candidate.upperWaypoint, false);
        synchronized (VerticalDiffractionRelief.class) {
            CROSS_LEG_CACHE.put(key, new LegCache(now,
                    source.x, source.y, source.z,
                    candidate.upperWaypoint.x, candidate.upperWaypoint.y, candidate.upperWaypoint.z,
                    value));
        }
        return new LegSample(value, false);
    }

    private static double runOcclusion(Vec3 from, Vec3 to, boolean lowerLeg) throws Exception {
        long start = System.nanoTime();
        try {
            return Beta10Optimizer.runOcclusionDirect(from, to);
        } finally {
            PerformanceStats.recordPortalOcclusionPath(System.nanoTime() - start, lowerLeg);
        }
    }

    private static void maybePruneCaches(long now) {
        boolean overSoftLimit = LOWER_LEG_CACHE.size() > LOWER_CACHE_SOFT_LIMIT
                || CROSS_LEG_CACHE.size() > CROSS_CACHE_SOFT_LIMIT;
        if (!overSoftLimit && now - lastPruneNs < CACHE_PRUNE_INTERVAL_NS) return;
        lastPruneNs = now;
        pruneCaches(now);
    }

    private static void pruneCaches(long now) {
        if (LOWER_LEG_CACHE.isEmpty() && CROSS_LEG_CACHE.isEmpty()) return;
        long staleNs = Math.max(1L, DiffractionConfig.openingRayCacheNs()) * 2L;
        LOWER_LEG_CACHE.entrySet().removeIf(entry -> now - entry.getValue().verifiedNs > staleNs);
        CROSS_LEG_CACHE.entrySet().removeIf(entry -> now - entry.getValue().verifiedNs > staleNs);
        trimOldestLower();
        trimOldestCross();
    }

    private static void trimOldestLower() {
        while (LOWER_LEG_CACHE.size() > LOWER_CACHE_SOFT_LIMIT) {
            OpeningLegKey oldest = null;
            long oldestNs = Long.MAX_VALUE;
            for (Map.Entry<OpeningLegKey, LegCache> entry : LOWER_LEG_CACHE.entrySet()) {
                if (entry.getValue().verifiedNs < oldestNs) {
                    oldestNs = entry.getValue().verifiedNs;
                    oldest = entry.getKey();
                }
            }
            if (oldest == null) break;
            LOWER_LEG_CACHE.remove(oldest);
        }
    }

    private static void trimOldestCross() {
        while (CROSS_LEG_CACHE.size() > CROSS_CACHE_SOFT_LIMIT) {
            CrossLegKey oldest = null;
            long oldestNs = Long.MAX_VALUE;
            for (Map.Entry<CrossLegKey, LegCache> entry : CROSS_LEG_CACHE.entrySet()) {
                if (entry.getValue().verifiedNs < oldestNs) {
                    oldestNs = entry.getValue().verifiedNs;
                    oldest = entry.getKey();
                }
            }
            if (oldest == null) break;
            CROSS_LEG_CACHE.remove(oldest);
        }
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
                    + " directDistance=" + r3(s.directDistance)
                    + " routeDistance=" + r3(s.routeDistance)
                    + " delta=" + r3(s.delta)
                    + " openingRadius=" + r3(s.openingRadius)
                    + " lowerLeg=" + r3(s.lowerLeg)
                    + " crossLeg=" + r3(s.crossLeg)
                    + " lowDiff=" + r3(s.lowDiff)
                    + " highDiff=" + r3(s.highDiff)
                    + " spread=" + r3(s.spread)
                    + " portalLow=" + r3(s.portalLowAmplitude)
                    + " portalHigh=" + r3(s.portalHighAmplitude)
                    + " candidates=" + s.candidateCount
                    + " verified=" + s.verifiedCount
                    + " openingBlockChecks=" + s.blockChecks
                    + " openingSprRays=" + s.sprRays
                    + " openingCached=" + s.cached
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

    private static double diffractionStrength(double delta, double scale) {
        double safeScale = Math.max(1.0E-6D, scale);
        return 1.0D / Math.sqrt(1.0D + Math.max(0.0D, delta) / safeScale);
    }

    private static double apertureSpreading(double distance, double scale) {
        double safeScale = Math.max(1.0E-6D, scale);
        return safeScale / (safeScale + Math.max(0.0D, distance));
    }

    private static double horizonFade(double radius, double maxRadius, double startRatio) {
        double max = Math.max(1.0E-6D, maxRadius);
        double start = max * clamp01(startRatio);
        if (radius <= start || start >= max - 1.0E-6D) return radius < max ? 1.0D : 0.0D;
        if (radius >= max) return 0.0D;
        return smoothStep(clamp01((max - radius) / (max - start)));
    }

    private static double smoothStep(double value) {
        double t = clamp01(value);
        return t * t * (3.0D - 2.0D * t);
    }

    private static double horizontalDistance(Vec3 a, Vec3 b) {
        double dx = a.x - b.x;
        double dz = a.z - b.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    private static double horizontalDistanceSq(Vec3 a, Vec3 b) {
        double dx = a.x - b.x;
        double dz = a.z - b.z;
        return dx * dx + dz * dz;
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

    private record CandidateKey(int x, int y, int z, boolean implicit) {}

    private record PortalCandidate(CandidateKey key,
                                   Vec3 lowerWaypoint,
                                   Vec3 upperWaypoint,
                                   double radius,
                                   double apertureDistance,
                                   double directDistance,
                                   double routeDistance,
                                   double delta,
                                   double horizonFade) {}

    private record OpeningLegKey(int x, int y, int z, boolean implicit) {}

    private record CrossLegKey(int sourceId, int x, int y, int z, boolean implicit) {}

    private record LegCache(long verifiedNs,
                            double fromX,
                            double fromY,
                            double fromZ,
                            double toX,
                            double toY,
                            double toZ,
                            double value) {}

    private record LegSample(double value, boolean cached) {}

    private record VerifiedPortal(PortalCandidate candidate,
                                  double lowerLeg,
                                  double upperLeg,
                                  double legRaw,
                                  double lowDiff,
                                  double highDiff,
                                  double spread,
                                  double lowAmplitude,
                                  double highAmplitude,
                                  double quality,
                                  boolean cached) {}

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
            return cached ? this : new OpeningTopology(baseX, baseY, baseZ, scanTopY,
                    scanNs, barrierY, candidates, 0, true);
        }
    }

    private static final class Snapshot {
        final String reason;
        final double raw;
        final double center;
        final double vertical;
        final double horizontal;
        boolean applied;
        double directDistance = Double.NaN;
        double routeDistance = Double.NaN;
        double delta = Double.NaN;
        double openingRadius = Double.NaN;
        double lowerLeg = Double.NaN;
        double crossLeg = Double.NaN;
        double lowDiff = Double.NaN;
        double highDiff = Double.NaN;
        double spread = Double.NaN;
        double portalLowAmplitude = Double.NaN;
        double portalHighAmplitude = Double.NaN;
        int candidateCount;
        int verifiedCount;
        int blockChecks;
        int sprRays;
        boolean cached;
        double inputCutoff = Double.NaN;
        double outputCutoff = Double.NaN;
        double inputGain = Double.NaN;
        double outputGain = Double.NaN;

        private Snapshot(String reason, double raw, double center, double vertical, double horizontal) {
            this.reason = reason;
            this.raw = raw;
            this.center = center;
            this.vertical = vertical;
            this.horizontal = horizontal;
        }

        static Snapshot skipped(String reason) {
            return new Snapshot(reason, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
        }

        static Snapshot gated(double raw, double center, double vertical, double horizontal, String reason) {
            return new Snapshot(reason, raw, center, vertical, horizontal);
        }
    }
}
