from pathlib import Path


def replace_once(text, old, new, label):
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected exactly 1 match, got {count}")
    return text.replace(old, new, 1)

VERSION_OLD = "0.1.0-beta11-phase5-diffraction-v6-energy-test"
VERSION_NEW = "0.1.0-beta11-phase5-diffraction-v7-1-spreading-only-test"
CONFIG_OLD = "cchq_soundphysics_compat-diffraction-v6-energy-test.toml"
CONFIG_NEW = "cchq_soundphysics_compat-diffraction-v7-1-spreading-only-test.toml"

# Idempotence guard.
gradle = Path("gradle.properties")
if VERSION_NEW in gradle.read_text():
    print("V7.1 already patched")
    raise SystemExit(0)

# --- VerticalDiffractionRelief: exact V6 spectral/transmission math + spreading + telemetry only. ---
p = Path("src/main/java/dev/cchqphysics/compat/audio/VerticalDiffractionRelief.java")
s = p.read_text()
s = replace_once(s, "Experimental V6 opening diffraction model.", "Experimental V7.1 spreading-only opening diffraction model.", "class comment")

# Add spreading to the V6 coupling; leave rawRatio/pathGain/pathCutoff EXACTLY intact.
s = replace_once(s,
'''            double lowDiff = diffractionStrength(candidate.delta, DiffractionConfig.lowDeltaScale());
            double highDiff = diffractionStrength(candidate.delta, DiffractionConfig.highDeltaScale());
            double activation = smoothStep(clamp01(raw / Math.max(1.0E-6D, DiffractionConfig.portalActivationRaw())));
            double coupling = DiffractionConfig.portalCoupling() * activation * candidate.horizonFade;

            double lowAmplitude = coupling * pathGain * lowDiff;
            double highAmplitude = coupling * pathGain * pathCutoff * highDiff;
            double quality = candidate.horizonFade / (1.0D + candidate.delta + 0.5D * legRaw);

            verified.add(new VerifiedPortal(candidate, lower.value, upper.value, legRaw,
                    lowDiff, highDiff, lowAmplitude, highAmplitude,
                    Math.max(1.0E-9D, quality), lower.cached && upper.cached));''',
'''            double lowDiff = diffractionStrength(candidate.delta, DiffractionConfig.lowDeltaScale());
            double highDiff = diffractionStrength(candidate.delta, DiffractionConfig.highDeltaScale());
            double spread = apertureSpreading(candidate.apertureDistance, DiffractionConfig.apertureSpreadScale());
            double activation = smoothStep(clamp01(raw / Math.max(1.0E-6D, DiffractionConfig.portalActivationRaw())));
            double coupling = DiffractionConfig.portalCoupling() * activation * candidate.horizonFade * spread;

            double lowAmplitude = coupling * pathGain * lowDiff;
            double highAmplitude = coupling * pathGain * pathCutoff * highDiff;
            double quality = candidate.horizonFade * spread / (1.0D + candidate.delta + 0.5D * legRaw);

            verified.add(new VerifiedPortal(candidate, lower.value, upper.value, legRaw,
                    lowDiff, highDiff, spread, lowAmplitude, highAmplitude,
                    Math.max(1.0E-9D, quality), lower.cached && upper.cached));''', "V6 amplitude plus spread")

s = replace_once(s, '? "applied-portal-energy-v6" : "portal-energy-negligible"', '? "applied-portal-energy-v7-1" : "portal-energy-negligible"', "reason version")
s = replace_once(s,
'''        snapshot.lowDiff = best.lowDiff;
        snapshot.highDiff = best.highDiff;
        snapshot.portalLowAmplitude = Math.sqrt(portalLowEnergy);''',
'''        snapshot.lowDiff = best.lowDiff;
        snapshot.highDiff = best.highDiff;
        snapshot.spread = best.spread;
        snapshot.portalLowAmplitude = Math.sqrt(portalLowEnergy);''', "snapshot spread")

# Add true listener->aperture distance for explicit roof openings. Keep implicit open-top spread=1 to preserve V6 open-top behavior.
s = replace_once(s,
'''            result.add(new PortalCandidate(key, waypoint, waypoint, 0.0D,
                    directDistance, route, delta, 1.0D));''',
'''            result.add(new PortalCandidate(key, waypoint, waypoint, 0.0D, 0.0D,
                    directDistance, route, delta, 1.0D));''', "implicit aperture distance")
s = replace_once(s,
'''            result.add(new PortalCandidate(key, lowerWaypoint, upperWaypoint, opening.radius,
                    directDistance, route, delta, fade));''',
'''            result.add(new PortalCandidate(key, lowerWaypoint, upperWaypoint, opening.radius,
                    listener.distanceTo(lowerWaypoint), directDistance, route, delta, fade));''', "explicit aperture distance")

# Portal-specific telemetry, no scheduling/cache changes.
s = replace_once(s, "double value = runOcclusion(listener, candidate.lowerWaypoint);", "double value = runOcclusion(listener, candidate.lowerWaypoint, true);", "lower telemetry call")
s = replace_once(s, "double value = runOcclusion(source, candidate.upperWaypoint);", "double value = runOcclusion(source, candidate.upperWaypoint, false);", "cross telemetry call")
s = replace_once(s,
'''    private static double runOcclusion(Vec3 from, Vec3 to) throws Exception {
        PerformanceStats.recordOcclusionPath();
        return Beta10Optimizer.runOcclusionDirect(from, to);
    }''',
'''    private static double runOcclusion(Vec3 from, Vec3 to, boolean lowerLeg) throws Exception {
        long start = System.nanoTime();
        try {
            return Beta10Optimizer.runOcclusionDirect(from, to);
        } finally {
            PerformanceStats.recordPortalOcclusionPath(System.nanoTime() - start, lowerLeg);
        }
    }''', "portal telemetry wrapper")
s = replace_once(s,
'''        OpeningTopology result = new OpeningTopology(baseX, baseY, baseZ, scanTopY,
                now, barrierY, openings.toArray(new OpeningCandidate[0]), blockChecks, false);''',
'''        OpeningTopology result = new OpeningTopology(baseX, baseY, baseZ, scanTopY,
                now, barrierY, openings.toArray(new OpeningCandidate[0]), blockChecks, false);
        PerformanceStats.recordPortalTopologyScan(blockChecks);''', "topology telemetry")

s = replace_once(s,
'''                    + " lowDiff=" + r3(s.lowDiff)
                    + " highDiff=" + r3(s.highDiff)
                    + " portalLow=" + r3(s.portalLowAmplitude)''',
'''                    + " lowDiff=" + r3(s.lowDiff)
                    + " highDiff=" + r3(s.highDiff)
                    + " spread=" + r3(s.spread)
                    + " portalLow=" + r3(s.portalLowAmplitude)''', "debug spread")

s = replace_once(s,
'''    private static double diffractionStrength(double delta, double scale) {
        double safeScale = Math.max(1.0E-6D, scale);
        return 1.0D / Math.sqrt(1.0D + Math.max(0.0D, delta) / safeScale);
    }''',
'''    private static double diffractionStrength(double delta, double scale) {
        double safeScale = Math.max(1.0E-6D, scale);
        return 1.0D / Math.sqrt(1.0D + Math.max(0.0D, delta) / safeScale);
    }

    private static double apertureSpreading(double distance, double scale) {
        double safeScale = Math.max(1.0E-6D, scale);
        return safeScale / (safeScale + Math.max(0.0D, distance));
    }''', "spreading helper")

s = replace_once(s,
'''    private record PortalCandidate(CandidateKey key,
                                   Vec3 lowerWaypoint,
                                   Vec3 upperWaypoint,
                                   double radius,
                                   double directDistance,''',
'''    private record PortalCandidate(CandidateKey key,
                                   Vec3 lowerWaypoint,
                                   Vec3 upperWaypoint,
                                   double radius,
                                   double apertureDistance,
                                   double directDistance,''', "candidate record")
s = replace_once(s,
'''    private record VerifiedPortal(PortalCandidate candidate,
                                  double lowerLeg,
                                  double upperLeg,
                                  double legRaw,
                                  double lowDiff,
                                  double highDiff,
                                  double lowAmplitude,''',
'''    private record VerifiedPortal(PortalCandidate candidate,
                                  double lowerLeg,
                                  double upperLeg,
                                  double legRaw,
                                  double lowDiff,
                                  double highDiff,
                                  double spread,
                                  double lowAmplitude,''', "verified record")
s = replace_once(s,
'''        double lowDiff = Double.NaN;
        double highDiff = Double.NaN;
        double portalLowAmplitude = Double.NaN;''',
'''        double lowDiff = Double.NaN;
        double highDiff = Double.NaN;
        double spread = Double.NaN;
        double portalLowAmplitude = Double.NaN;''', "snapshot field")

# Critical V7.1 isolation invariant: V6 transmission hack MUST still be present and V7 leg model MUST NOT be present.
for marker in ["double rawRatio =", "Math.pow(clampFilter(gain), rawRatio)", "Math.pow(clampFilter(cutoff), rawRatio)"]:
    if marker not in s:
        raise SystemExit(f"V7.1 invariant failed: missing exact V6 transmission marker: {marker}")
for forbidden in ["legTransmission(", "lowLegScale()", "highLegScale()", "double legLow", "double legHigh"]:
    if forbidden in s:
        raise SystemExit(f"V7.1 invariant failed: V7 leg rewrite leaked in: {forbidden}")
p.write_text(s)

# --- DiffractionConfig: add ONLY aperture spread scale; preserve V6 delta/transmission config. ---
p = Path("src/main/java/dev/cchqphysics/compat/config/DiffractionConfig.java")
s = p.read_text()
s = replace_once(s, "/** Experimental V6 aperture-energy diffraction model. */", "/** Experimental V7.1 spreading-only aperture-energy model. */", "config comment")
s = replace_once(s, "private static final ModConfigSpec.DoubleValue HIGH_DELTA_SCALE;", "private static final ModConfigSpec.DoubleValue HIGH_DELTA_SCALE;\n    private static final ModConfigSpec.DoubleValue APERTURE_SPREAD_SCALE;", "spread field")
s = replace_once(s, 'builder.push("portal_diffraction_v6_test");', 'builder.push("portal_diffraction_v7_1_spreading_only_test");', "config root")
s = replace_once(s, '"Experimental Phase-5 V6 aperture-energy diffraction model."', '"Experimental Phase-5 V7.1 spreading-only aperture-energy model."', "config title")
s = replace_once(s,
'''        HIGH_DELTA_SCALE = builder.comment(
                "Path-length-difference scale for the high-band diffraction approximation.",
                "Smaller than the low-band scale so highs attenuate more strongly as the detour worsens.")
                .defineInRange("high_delta_scale", 1.5D, 0.05D, 32.0D);

        HORIZON_FADE_START_RATIO''',
'''        HIGH_DELTA_SCALE = builder.comment(
                "Path-length-difference scale for the high-band diffraction approximation.",
                "Smaller than the low-band scale so highs attenuate more strongly as the detour worsens.")
                .defineInRange("high_delta_scale", 1.5D, 0.05D, 32.0D);

        APERTURE_SPREAD_SCALE = builder.comment(
                "Softened inverse-distance amplitude scale for explicit roof-opening leakage into the listener enclosure.",
                "This multiplies the exact V6 portal amplitude only; it does not alter V6 portal-leg spectral/transmission math.",
                "Implicit open-top geometry uses zero aperture distance in this isolated test to preserve the V6 open-top result.")
                .defineInRange("aperture_spread_scale", 3.0D, 0.25D, 16.0D);

        HORIZON_FADE_START_RATIO''', "spread config")
s = replace_once(s, "public static double highDeltaScale() { return d(HIGH_DELTA_SCALE, 1.5D); }", "public static double highDeltaScale() { return d(HIGH_DELTA_SCALE, 1.5D); }\n    public static double apertureSpreadScale() { return d(APERTURE_SPREAD_SCALE, 3.0D); }", "spread getter")
s = replace_once(s, '+ " portalV6=true"', '+ " portalV7_1=true"', "summary version")
s = replace_once(s, '+ " highDeltaScale=" + highDeltaScale()\n                + " horizonFadeStart="', '+ " highDeltaScale=" + highDeltaScale()\n                + " apertureSpreadScale=" + apertureSpreadScale()\n                + " horizonFadeStart="', "summary spread")
for forbidden in ["low_leg_scale", "high_leg_scale", "lowLegScale", "highLegScale"]:
    if forbidden in s:
        raise SystemExit(f"V7.1 invariant failed: unwanted V7 leg config present: {forbidden}")
p.write_text(s)

# --- PerformanceStats: copy only V7 attribution telemetry. ---
p = Path("src/main/java/dev/cchqphysics/compat/audio/PerformanceStats.java")
s = p.read_text()
s = replace_once(s, "private static long occlusionPaths;\n    private static long efxApplies;", "private static long occlusionPaths;\n    private static long portalOcclusionPaths;\n    private static long portalOcclusionTotalNs;\n    private static long portalOcclusionMaxNs;\n    private static long portalLowerPaths;\n    private static long portalCrossPaths;\n    private static long portalTopologyScans;\n    private static long portalBlockChecks;\n    private static long efxApplies;", "perf fields")
s = replace_once(s, "static void recordOcclusionPath() { occlusionPaths++; }", '''static void recordOcclusionPath() { occlusionPaths++; }

    static void recordPortalOcclusionPath(long elapsedNs, boolean lowerLeg) {
        occlusionPaths++;
        portalOcclusionPaths++;
        long value = Math.max(0L, elapsedNs);
        portalOcclusionTotalNs += value;
        if (value > portalOcclusionMaxNs) portalOcclusionMaxNs = value;
        if (lowerLeg) portalLowerPaths++;
        else portalCrossPaths++;
    }

    static void recordPortalTopologyScan(int blockChecks) {
        portalTopologyScans++;
        portalBlockChecks += Math.max(0, blockChecks);
    }''', "perf methods")
s = replace_once(s, "double sprAvgMs = sprCalls == 0L ? 0.0D : (sprTotalNs / 1_000_000.0D) / sprCalls;", "double sprAvgMs = sprCalls == 0L ? 0.0D : (sprTotalNs / 1_000_000.0D) / sprCalls;\n            double portalAvgMs = portalOcclusionPaths == 0L ? 0.0D : (portalOcclusionTotalNs / 1_000_000.0D) / portalOcclusionPaths;", "portal avg")
s = replace_once(s, "applyPasses={} ({}/s) occlusionPaths={} ({}/s) progressive={}", "applyPasses={} ({}/s) occlusionPaths={} ({}/s) portalPaths={} ({}/s) portalAvg={}ms portalMax={}ms portalLower={} portalCross={} portalScans={} portalBlockChecks={} progressive={}", "perf format")
s = replace_once(s,
'''                    applyPasses, round1(applyPasses / seconds),
                    occlusionPaths, round1(occlusionPaths / seconds),
                    progressivePasses,''',
'''                    applyPasses, round1(applyPasses / seconds),
                    occlusionPaths, round1(occlusionPaths / seconds),
                    portalOcclusionPaths, round1(portalOcclusionPaths / seconds),
                    round3(portalAvgMs), round3(portalOcclusionMaxNs / 1_000_000.0D),
                    portalLowerPaths, portalCrossPaths, portalTopologyScans, portalBlockChecks,
                    progressivePasses,''', "perf args")
s = replace_once(s, "occlusionPaths = 0L;\n        efxApplies = 0L;", "occlusionPaths = 0L;\n        portalOcclusionPaths = 0L;\n        portalOcclusionTotalNs = 0L;\n        portalOcclusionMaxNs = 0L;\n        portalLowerPaths = 0L;\n        portalCrossPaths = 0L;\n        portalTopologyScans = 0L;\n        portalBlockChecks = 0L;\n        efxApplies = 0L;", "perf reset")
p.write_text(s)

# --- Identity / fresh config filename. ---
p = Path("gradle.properties")
s = p.read_text()
s = replace_once(s, VERSION_OLD, VERSION_NEW, "gradle version")
p.write_text(s)

p = Path("src/main/java/dev/cchqphysics/compat/CCHQSoundPhysicsCompat.java")
s = p.read_text()
s = replace_once(s, VERSION_OLD, VERSION_NEW, "java version")
s = replace_once(s, CONFIG_OLD, CONFIG_NEW, "config filename")
s = s.replace("V6 aperture-energy test", "V7.1 spreading-only aperture-energy test")
s = s.replace("V6 aperture-energy diffraction", "V7.1 spreading-only aperture-energy diffraction")
p.write_text(s)

p = Path("src/main/resources/META-INF/neoforge.mods.toml")
s = p.read_text()
s = replace_once(s, VERSION_OLD, VERSION_NEW, "toml version")
# Keep description semantically narrow; don't depend on exact old sentence ordering.
s = s.replace("V6 retires the V3-V5 behavior", "V7.1 preserves V6 portal spectral/transmission math and adds only explicit-aperture distance spreading; V6 retires the V3-V5 behavior")
s = s.replace("Aperture strength is driven by source->aperture->listener path-length difference, verified leg occlusion, and a smooth finite-search fade.", "Aperture strength uses the exact V6 path-length/leg-transmission model, multiplied by a softened inverse-distance spreading term for explicit roof openings and the existing smooth finite-search fade.")
p.write_text(s)

print("V7.1 spreading-only patch applied")
