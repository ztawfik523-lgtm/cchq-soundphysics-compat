package dev.cchqphysics.compat.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Experimental vertical/opening diffraction relief for straight-ray over-occlusion. */
public final class DiffractionConfig {
    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.BooleanValue ENABLED;
    private static final ModConfigSpec.DoubleValue MIN_VERTICAL_SEPARATION;
    private static final ModConfigSpec.DoubleValue MIN_HORIZONTAL_SEPARATION;
    private static final ModConfigSpec.DoubleValue MAX_HORIZONTAL_SEPARATION;
    private static final ModConfigSpec.DoubleValue MIN_VERTICAL_HORIZONTAL_RATIO;
    private static final ModConfigSpec.DoubleValue RAW_OCCLUSION_GATE;
    private static final ModConfigSpec.DoubleValue ESCAPE_CLEARANCE;
    private static final ModConfigSpec.DoubleValue VERTICAL_OPEN_GATE;
    private static final ModConfigSpec.DoubleValue DIFFRACTION_PENALTY;
    private static final ModConfigSpec.DoubleValue MIN_RAW_IMPROVEMENT;

    private static final ModConfigSpec.DoubleValue OPENING_RAW_OCCLUSION_GATE;
    private static final ModConfigSpec.DoubleValue OPENING_SEARCH_RADIUS;
    private static final ModConfigSpec.DoubleValue OPENING_LEG_GATE;
    private static final ModConfigSpec.DoubleValue OPENING_BASE_PENALTY;
    private static final ModConfigSpec.DoubleValue OPENING_DISTANCE_PENALTY;
    private static final ModConfigSpec.DoubleValue OPENING_MIN_RAW_IMPROVEMENT;
    private static final ModConfigSpec.DoubleValue OPENING_SCAN_INTERVAL_MS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("vertical_diffraction_test");

        ENABLED = builder.comment(
                "Experimental Phase-5 vertical/opening diffraction relief.",
                "OFF by default. Enable only while validating elevation and nearby-opening leakage behavior.",
                "Never changes source position, playback timing, synchronized starts, reflection routing, or reverb sends.")
                .define("enabled", false);

        MIN_VERTICAL_SEPARATION = builder.comment(
                "Minimum absolute ear-to-source Y separation before the diffraction probe is considered.",
                "Player camera/ear height means a visually 2-3 block-deep hole has a smaller acoustic Y delta than its floor depth.")
                .defineInRange("min_vertical_separation", 0.75D, 0.0D, 32.0D);

        MIN_HORIZONTAL_SEPARATION = builder.comment(
                "Minimum source/listener horizontal separation for the original direct open-top probe only.",
                "Nearby-opening search may still run below this value so a side opening can matter while standing under a ceiling.")
                .defineInRange("min_horizontal_separation", 1.5D, 0.0D, 32.0D);

        MAX_HORIZONTAL_SEPARATION = builder.comment(
                "Maximum source/listener horizontal separation for this narrowly-scoped test path.")
                .defineInRange("max_horizontal_separation", 12.0D, 0.5D, 64.0D);

        MIN_VERTICAL_HORIZONTAL_RATIO = builder.comment(
                "Minimum |dy| / horizontal-distance ratio.",
                "Relaxed after runtime evidence showed a 3-block-deep open pit measured only ~1.88 Y over ~7.25 horizontal.")
                .defineInRange("min_vertical_horizontal_ratio", 0.10D, 0.0D, 8.0D);

        RAW_OCCLUSION_GATE = builder.comment(
                "Normal progressive raw occlusion required by the original direct open-top probe.")
                .defineInRange("raw_occlusion_gate", 3.0D, 0.0D, 16.0D);

        ESCAPE_CLEARANCE = builder.comment(
                "Waypoint height above the higher endpoint, in blocks.")
                .defineInRange("escape_clearance", 1.5D, 0.25D, 8.0D);

        VERTICAL_OPEN_GATE = builder.comment(
                "A candidate escape/opening leg must be this clear or clearer.")
                .defineInRange("vertical_open_gate", 0.25D, 0.0D, 4.0D);

        DIFFRACTION_PENALTY = builder.comment(
                "Synthetic raw-occlusion penalty for the already-validated direct open-top route.")
                .defineInRange("diffraction_penalty", 1.0D, 0.0D, 8.0D);

        MIN_RAW_IMPROVEMENT = builder.comment(
                "Direct open-top route must improve raw occlusion by at least this much.")
                .defineInRange("min_raw_improvement", 1.0D, 0.0D, 16.0D);

        builder.push("nearby_opening");

        OPENING_RAW_OCCLUSION_GATE = builder.comment(
                "Lower raw-occlusion gate for a verified nearby opening.",
                "This lets a real aperture soften even a one- or two-block ceiling case without making a fully sealed ceiling transparent.")
                .defineInRange("raw_occlusion_gate", 1.25D, 0.0D, 16.0D);

        OPENING_SEARCH_RADIUS = builder.comment(
                "Maximum horizontal distance, in blocks, searched around the lower endpoint for a nearby opening.",
                "The V4 probe intentionally searches only nearby apertures; farther openings are ignored until the listener moves closer.")
                .defineInRange("search_radius", 3.0D, 1.0D, 8.0D);

        OPENING_LEG_GATE = builder.comment(
                "Listener/lower-endpoint to opening waypoint must be this clear or clearer.")
                .defineInRange("opening_leg_gate", 0.25D, 0.0D, 4.0D);

        OPENING_BASE_PENALTY = builder.comment(
                "Base diffraction penalty for routing through a nearby opening.")
                .defineInRange("base_penalty", 0.75D, 0.0D, 8.0D);

        OPENING_DISTANCE_PENALTY = builder.comment(
                "Additional raw penalty per block of horizontal detour to the opening.",
                "This is what makes the sound progressively clearer as the listener approaches the aperture.")
                .defineInRange("distance_penalty_per_block", 0.20D, 0.0D, 4.0D);

        OPENING_MIN_RAW_IMPROVEMENT = builder.comment(
                "Nearby-opening route must beat the normal raw occlusion by at least this amount.")
                .defineInRange("min_raw_improvement", 0.25D, 0.0D, 16.0D);

        OPENING_SCAN_INTERVAL_MS = builder.comment(
                "Minimum interval between nearby-opening scans for a source.",
                "The scan uses SPR occlusion rays on the sound-thread path and is deliberately rate-limited.")
                .defineInRange("scan_interval_ms", 250.0D, 50.0D, 2000.0D);

        builder.pop();
        builder.pop();
        SPEC = builder.build();
    }

    private DiffractionConfig() {}

    private static boolean b(ModConfigSpec.BooleanValue value, boolean fallback) {
        try { return value.get(); } catch (Throwable ignored) { return fallback; }
    }

    private static double d(ModConfigSpec.DoubleValue value, double fallback) {
        try {
            Double result = value.get();
            return result == null ? fallback : result;
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    public static boolean enabled() { return b(ENABLED, false); }
    public static double minVerticalSeparation() { return d(MIN_VERTICAL_SEPARATION, 0.75D); }
    public static double minHorizontalSeparation() { return d(MIN_HORIZONTAL_SEPARATION, 1.5D); }
    public static double maxHorizontalSeparation() { return d(MAX_HORIZONTAL_SEPARATION, 12.0D); }
    public static double minVerticalHorizontalRatio() { return d(MIN_VERTICAL_HORIZONTAL_RATIO, 0.10D); }
    public static double rawOcclusionGate() { return d(RAW_OCCLUSION_GATE, 3.0D); }
    public static double escapeClearance() { return d(ESCAPE_CLEARANCE, 1.5D); }
    public static double verticalOpenGate() { return d(VERTICAL_OPEN_GATE, 0.25D); }
    public static double diffractionPenalty() { return d(DIFFRACTION_PENALTY, 1.0D); }
    public static double minRawImprovement() { return d(MIN_RAW_IMPROVEMENT, 1.0D); }

    public static double openingRawOcclusionGate() { return d(OPENING_RAW_OCCLUSION_GATE, 1.25D); }
    public static double openingSearchRadius() { return d(OPENING_SEARCH_RADIUS, 3.0D); }
    public static double openingLegGate() { return d(OPENING_LEG_GATE, 0.25D); }
    public static double openingBasePenalty() { return d(OPENING_BASE_PENALTY, 0.75D); }
    public static double openingDistancePenalty() { return d(OPENING_DISTANCE_PENALTY, 0.20D); }
    public static double openingMinRawImprovement() { return d(OPENING_MIN_RAW_IMPROVEMENT, 0.25D); }
    public static long openingScanIntervalNs() {
        return Math.max(50L, Math.round(d(OPENING_SCAN_INTERVAL_MS, 250.0D))) * 1_000_000L;
    }

    public static void setEnabled(boolean value) { ENABLED.set(value); }
    public static void save() { SPEC.save(); }

    public static String summary() {
        return "diffraction=" + enabled()
                + " minY=" + minVerticalSeparation()
                + " minXZ=" + minHorizontalSeparation()
                + " maxXZ=" + maxHorizontalSeparation()
                + " minSlope=" + minVerticalHorizontalRatio()
                + " rawGate=" + rawOcclusionGate()
                + " clearance=" + escapeClearance()
                + " verticalOpenGate=" + verticalOpenGate()
                + " penalty=" + diffractionPenalty()
                + " minImprove=" + minRawImprovement()
                + " openingRawGate=" + openingRawOcclusionGate()
                + " openingRadius=" + openingSearchRadius()
                + " openingLegGate=" + openingLegGate()
                + " openingBasePenalty=" + openingBasePenalty()
                + " openingDistancePenalty=" + openingDistancePenalty()
                + " openingMinImprove=" + openingMinRawImprovement()
                + " openingScanMs=" + (openingScanIntervalNs() / 1_000_000L);
    }
}
