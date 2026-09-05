package dev.cchqphysics.compat.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Experimental vertical/open-top diffraction relief for severe straight-ray over-occlusion. */
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

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("vertical_diffraction_test");

        ENABLED = builder.comment(
                "Experimental Phase-5 open-top diffraction relief.",
                "OFF by default. Enable only while validating the steep-elevation / open-pit over-occlusion case.",
                "Never changes source position, playback timing, synchronized starts, reflection routing, or reverb sends.")
                .define("enabled", false);

        MIN_VERTICAL_SEPARATION = builder.comment(
                "Minimum absolute ear-to-source Y separation before the diffraction probe is considered.",
                "Player camera/ear height means a visually 2-3 block-deep hole has a smaller acoustic Y delta than its floor depth.")
                .defineInRange("min_vertical_separation", 0.75D, 0.0D, 32.0D);

        MIN_HORIZONTAL_SEPARATION = builder.comment(
                "Minimum horizontal separation. This deliberately excludes nearly vertical floor/ceiling cases.")
                .defineInRange("min_horizontal_separation", 1.5D, 0.0D, 32.0D);

        MAX_HORIZONTAL_SEPARATION = builder.comment(
                "Maximum horizontal separation for this narrowly-scoped test path.")
                .defineInRange("max_horizontal_separation", 12.0D, 0.5D, 64.0D);

        MIN_VERTICAL_HORIZONTAL_RATIO = builder.comment(
                "Minimum |dy| / horizontal-distance ratio.",
                "Relaxed after runtime evidence showed a 3-block-deep open pit measured only ~1.88 Y over ~7.25 horizontal (slope ~0.26).")
                .defineInRange("min_vertical_horizontal_ratio", 0.10D, 0.0D, 8.0D);

        RAW_OCCLUSION_GATE = builder.comment(
                "Normal progressive raw occlusion must already be at least this high before extra diffraction rays are allowed.")
                .defineInRange("raw_occlusion_gate", 3.0D, 0.0D, 16.0D);

        ESCAPE_CLEARANCE = builder.comment(
                "Waypoint height above the higher endpoint, in blocks.")
                .defineInRange("escape_clearance", 1.5D, 0.25D, 8.0D);

        VERTICAL_OPEN_GATE = builder.comment(
                "The vertical escape leg from the lower endpoint must be this clear or clearer.",
                "This is the sealed-roof/floor safety gate: a real blocking ceiling should fail it.")
                .defineInRange("vertical_open_gate", 0.25D, 0.0D, 4.0D);

        DIFFRACTION_PENALTY = builder.comment(
                "Extra synthetic raw-occlusion penalty added even when the two-segment route is fully clear.",
                "1.0 intentionally keeps an open-edge route somewhat muffled instead of making it perfectly clear.")
                .defineInRange("diffraction_penalty", 1.0D, 0.0D, 8.0D);

        MIN_RAW_IMPROVEMENT = builder.comment(
                "Candidate route must improve raw occlusion by at least this much before relief is applied.")
                .defineInRange("min_raw_improvement", 1.0D, 0.0D, 16.0D);

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
                + " minImprove=" + minRawImprovement();
    }
}
