package dev.cchqphysics.compat.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ClientConfig {
    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.BooleanValue ENABLED;
    private static final ModConfigSpec.BooleanValue RANGE_SCALING;
    private static final ModConfigSpec.DoubleValue AUDIBLE_RANGE_MULTIPLIER;

    private static final ModConfigSpec.BooleanValue PROGRESSIVE_OCCLUSION;
    private static final ModConfigSpec.DoubleValue INNER_VARIATION;
    private static final ModConfigSpec.DoubleValue OUTER_VARIATION;
    private static final ModConfigSpec.DoubleValue CENTER_WEIGHT;
    private static final ModConfigSpec.DoubleValue INNER_WEIGHT;
    private static final ModConfigSpec.DoubleValue OUTER_WEIGHT;
    private static final ModConfigSpec.DoubleValue OPEN_CENTER_RING_SCALE;
    private static final ModConfigSpec.DoubleValue CUTOFF_OCCLUSION_SCALE;
    private static final ModConfigSpec.DoubleValue GAIN_OCCLUSION_SCALE;

    private static final ModConfigSpec.BooleanValue STABILIZE_REFLECTIONS;
    private static final ModConfigSpec.DoubleValue REFLECTION_THRESHOLD;
    private static final ModConfigSpec.DoubleValue REFLECTION_BLEND;
    private static final ModConfigSpec.DoubleValue MAX_REFLECTION_OFFSET;
    private static final ModConfigSpec.DoubleValue REDIRECT_ALPHA;
    private static final ModConfigSpec.DoubleValue CLEAR_POSITION_ALPHA;
    private static final ModConfigSpec.DoubleValue FLIP_TO_CENTER_ALPHA;

    private static final ModConfigSpec.DoubleValue MUFFLE_ALPHA;
    private static final ModConfigSpec.DoubleValue CLEAR_CUTOFF_ALPHA;
    private static final ModConfigSpec.DoubleValue CLEAR_GAIN_ALPHA;
    private static final ModConfigSpec.DoubleValue REVERB_ALPHA;

    private static final ModConfigSpec.IntValue FULL_SPR_UPDATE_MS;
    private static final ModConfigSpec.IntValue OCCLUSION_MIN_UPDATE_MS;
    private static final ModConfigSpec.IntValue OCCLUSION_STATIONARY_UPDATE_MS;
    private static final ModConfigSpec.DoubleValue OCCLUSION_MOVE_THRESHOLD;
    private static final ModConfigSpec.BooleanValue ADAPTIVE_PROBE_CACHE;
    private static final ModConfigSpec.DoubleValue PROBE_FULL_REFRESH_DISTANCE;
    private static final ModConfigSpec.DoubleValue PROBE_CENTER_DELTA;
    private static final ModConfigSpec.BooleanValue DIAGNOSTICS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("general");
        ENABLED = builder
                .comment("Enable interception of CC:HQ whole-file speaker audio. Disable to let CC:HQ handle it normally.")
                .translation("cchq_soundphysics_compat.configuration.general.enabled")
                .define("enabled", true);
        builder.pop();

        builder.push("distance");
        RANGE_SCALING = builder
                .comment("For volume values above 1, scale audible range instead of making near-field gain exceed 100%.")
                .translation("cchq_soundphysics_compat.configuration.distance.scale_range_above_one")
                .define("scale_range_above_one", true);
        AUDIBLE_RANGE_MULTIPLIER = builder
                .comment("Multiplier applied to the compat audible endpoint after SPR-derived range calculation. 1.0 is the validated default.")
                .translation("cchq_soundphysics_compat.configuration.distance.audible_range_multiplier")
                .defineInRange("audible_range_multiplier", 1.0D, 0.25D, 4.0D);
        builder.pop();

        builder.push("occlusion");
        PROGRESSIVE_OCCLUSION = builder
                .comment("Enable the speaker-only 17-probe progressive obstruction model layered on top of SPR.")
                .translation("cchq_soundphysics_compat.configuration.occlusion.progressive")
                .define("progressive", true);
        INNER_VARIATION = builder
                .comment("Inner probe-ring offset in blocks.")
                .translation("cchq_soundphysics_compat.configuration.occlusion.inner_variation")
                .defineInRange("inner_variation", 0.20D, 0.05D, 0.75D);
        OUTER_VARIATION = builder
                .comment("Outer probe-ring offset in blocks.")
                .translation("cchq_soundphysics_compat.configuration.occlusion.outer_variation")
                .defineInRange("outer_variation", 0.49D, 0.10D, 1.25D);
        CENTER_WEIGHT = builder
                .comment("Weight of the exact source-to-listener ray.")
                .translation("cchq_soundphysics_compat.configuration.occlusion.center_weight")
                .defineInRange("center_weight", 4.0D, 0.10D, 32.0D);
        INNER_WEIGHT = builder
                .comment("Weight of the eight inner-ring rays.")
                .translation("cchq_soundphysics_compat.configuration.occlusion.inner_weight")
                .defineInRange("inner_weight", 1.0D, 0.0D, 8.0D);
        OUTER_WEIGHT = builder
                .comment("Weight of the eight outer-ring rays.")
                .translation("cchq_soundphysics_compat.configuration.occlusion.outer_weight")
                .defineInRange("outer_weight", 0.5D, 0.0D, 8.0D);
        OPEN_CENTER_RING_SCALE = builder
                .comment("How much surrounding probes can influence a source whose exact center path is clear.")
                .translation("cchq_soundphysics_compat.configuration.occlusion.open_center_ring_scale")
                .defineInRange("open_center_ring_scale", 0.20D, 0.0D, 1.0D);
        CUTOFF_OCCLUSION_SCALE = builder
                .comment("Strength of progressive high-frequency muffling.")
                .translation("cchq_soundphysics_compat.configuration.occlusion.cutoff_scale")
                .defineInRange("cutoff_scale", 0.35D, 0.0D, 2.0D);
        GAIN_OCCLUSION_SCALE = builder
                .comment("Strength of progressive direct-volume attenuation.")
                .translation("cchq_soundphysics_compat.configuration.occlusion.gain_scale")
                .defineInRange("gain_scale", 0.50D, 0.0D, 2.0D);
        builder.pop();

        builder.push("direction");
        STABILIZE_REFLECTIONS = builder
                .comment("Stabilize SPR reflected/redirected source positions for long-running speakers.")
                .translation("cchq_soundphysics_compat.configuration.direction.stabilize_reflections")
                .define("stabilize_reflections", true);
        REFLECTION_THRESHOLD = builder
                .comment("Minimum progressive raw occlusion before reflected positioning is allowed.")
                .translation("cchq_soundphysics_compat.configuration.direction.reflection_threshold")
                .defineInRange("reflection_threshold", 0.45D, 0.0D, 16.0D);
        REFLECTION_BLEND = builder
                .comment("Fraction of SPR's reflected displacement to retain.")
                .translation("cchq_soundphysics_compat.configuration.direction.reflection_blend")
                .defineInRange("reflection_blend", 0.35D, 0.0D, 1.0D);
        MAX_REFLECTION_OFFSET = builder
                .comment("Maximum virtual-source displacement from the physical speaker, in blocks.")
                .translation("cchq_soundphysics_compat.configuration.direction.max_reflection_offset")
                .defineInRange("max_reflection_offset", 2.5D, 0.0D, 16.0D);
        REDIRECT_ALPHA = builder
                .comment("Smoothing factor while following a reflected position.")
                .translation("cchq_soundphysics_compat.configuration.direction.redirect_alpha")
                .defineInRange("redirect_alpha", 0.22D, 0.01D, 1.0D);
        CLEAR_POSITION_ALPHA = builder
                .comment("Smoothing factor while returning toward the real speaker position.")
                .translation("cchq_soundphysics_compat.configuration.direction.clear_alpha")
                .defineInRange("clear_alpha", 0.28D, 0.01D, 1.0D);
        FLIP_TO_CENTER_ALPHA = builder
                .comment("Smoothing factor used when a reflected route changes to the opposite side.")
                .translation("cchq_soundphysics_compat.configuration.direction.flip_to_center_alpha")
                .defineInRange("flip_to_center_alpha", 0.35D, 0.01D, 1.0D);
        builder.pop();

        builder.push("smoothing");
        MUFFLE_ALPHA = builder
                .comment("Direct-filter smoothing while obstruction increases.")
                .translation("cchq_soundphysics_compat.configuration.smoothing.muffle_alpha")
                .defineInRange("muffle_alpha", 0.30D, 0.01D, 1.0D);
        CLEAR_CUTOFF_ALPHA = builder
                .comment("Log-space cutoff smoothing while obstruction clears.")
                .translation("cchq_soundphysics_compat.configuration.smoothing.clear_cutoff_alpha")
                .defineInRange("clear_cutoff_alpha", 0.18D, 0.01D, 1.0D);
        CLEAR_GAIN_ALPHA = builder
                .comment("Log-space gain smoothing while obstruction clears.")
                .translation("cchq_soundphysics_compat.configuration.smoothing.clear_gain_alpha")
                .defineInRange("clear_gain_alpha", 0.16D, 0.01D, 1.0D);
        REVERB_ALPHA = builder
                .comment("Smoothing applied to SPR wet/reverb send targets.")
                .translation("cchq_soundphysics_compat.configuration.smoothing.reverb_alpha")
                .defineInRange("reverb_alpha", 0.22D, 0.01D, 1.0D);
        builder.pop();

        builder.push("performance");
        FULL_SPR_UPDATE_MS = builder
                .comment("Minimum interval between full SPR processSound evaluations for each compat speaker.")
                .translation("cchq_soundphysics_compat.configuration.performance.full_spr_update_ms")
                .defineInRange("full_spr_update_ms", 100, 50, 2000);
        OCCLUSION_MIN_UPDATE_MS = builder
                .comment("Minimum interval between progressive obstruction calculations while moving.")
                .translation("cchq_soundphysics_compat.configuration.performance.occlusion_min_update_ms")
                .defineInRange("occlusion_min_update_ms", 100, 50, 2000);
        OCCLUSION_STATIONARY_UPDATE_MS = builder
                .comment("Progressive obstruction refresh interval while the listener is effectively stationary.")
                .translation("cchq_soundphysics_compat.configuration.performance.occlusion_stationary_update_ms")
                .defineInRange("occlusion_stationary_update_ms", 200, 50, 5000);
        OCCLUSION_MOVE_THRESHOLD = builder
                .comment("Listener movement in blocks that counts as movement for progressive obstruction refreshes.")
                .translation("cchq_soundphysics_compat.configuration.performance.occlusion_move_threshold")
                .defineInRange("occlusion_move_threshold", 0.15D, 0.01D, 2.0D);
        ADAPTIVE_PROBE_CACHE = builder
                .comment("Reuse one exact 8-probe ring briefly while refreshing the other. Keeps the same 17-point model but reduces raycasts; full refreshes are forced around meaningful changes.")
                .translation("cchq_soundphysics_compat.configuration.performance.adaptive_probe_cache")
                .define("adaptive_probe_cache", true);
        PROBE_FULL_REFRESH_DISTANCE = builder
                .comment("Cumulative listener movement in blocks since the last full 17-probe refresh that forces both rings fresh again.")
                .translation("cchq_soundphysics_compat.configuration.performance.probe_full_refresh_distance")
                .defineInRange("probe_full_refresh_distance", 0.50D, 0.10D, 4.0D);
        PROBE_CENTER_DELTA = builder
                .comment("Change in center-path occlusion since the last full refresh that forces both probe rings fresh immediately.")
                .translation("cchq_soundphysics_compat.configuration.performance.probe_center_delta")
                .defineInRange("probe_center_delta", 0.20D, 0.01D, 4.0D);
        DIAGNOSTICS = builder
                .comment("Log one compact compat performance report roughly every 10 seconds while speakers are active.")
                .translation("cchq_soundphysics_compat.configuration.performance.diagnostics")
                .define("diagnostics", false);
        builder.pop();

        SPEC = builder.build();
    }

    private ClientConfig() {}

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

    private static int i(ModConfigSpec.IntValue value, int fallback) {
        try {
            Integer result = value.get();
            return result == null ? fallback : result;
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    public static boolean enabled() { return b(ENABLED, true); }
    public static boolean rangeScaling() { return b(RANGE_SCALING, true); }
    public static double audibleRangeMultiplier() { return d(AUDIBLE_RANGE_MULTIPLIER, 1.0D); }

    public static boolean progressiveOcclusion() { return b(PROGRESSIVE_OCCLUSION, true); }
    public static double innerVariation() { return d(INNER_VARIATION, 0.20D); }
    public static double outerVariation() { return d(OUTER_VARIATION, 0.49D); }
    public static double centerWeight() { return d(CENTER_WEIGHT, 4.0D); }
    public static double innerWeight() { return d(INNER_WEIGHT, 1.0D); }
    public static double outerWeight() { return d(OUTER_WEIGHT, 0.50D); }
    public static double openCenterRingScale() { return d(OPEN_CENTER_RING_SCALE, 0.20D); }
    public static double openCenterRingComplement() { return 1.0D - openCenterRingScale(); }
    public static double cutoffOcclusionScale() { return d(CUTOFF_OCCLUSION_SCALE, 0.35D); }
    public static double gainOcclusionScale() { return d(GAIN_OCCLUSION_SCALE, 0.50D); }

    public static double reflectionThreshold() {
        return b(STABILIZE_REFLECTIONS, true) ? d(REFLECTION_THRESHOLD, 0.45D) : Double.POSITIVE_INFINITY;
    }

    public static double reflectionBlend() {
        return b(STABILIZE_REFLECTIONS, true) ? d(REFLECTION_BLEND, 0.35D) : 0.0D;
    }

    public static double maxReflectionOffset() { return d(MAX_REFLECTION_OFFSET, 2.5D); }
    public static double redirectAlpha() { return d(REDIRECT_ALPHA, 0.22D); }
    public static double clearPositionAlpha() { return d(CLEAR_POSITION_ALPHA, 0.28D); }
    public static double flipToCenterAlpha() { return d(FLIP_TO_CENTER_ALPHA, 0.35D); }

    public static float muffleAlpha() { return (float) d(MUFFLE_ALPHA, 0.30D); }
    public static float clearCutoffAlpha() { return (float) d(CLEAR_CUTOFF_ALPHA, 0.18D); }
    public static float clearGainAlpha() { return (float) d(CLEAR_GAIN_ALPHA, 0.16D); }
    public static float reverbAlpha() { return (float) d(REVERB_ALPHA, 0.22D); }

    public static long fullSprIntervalNs() { return i(FULL_SPR_UPDATE_MS, 100) * 1_000_000L; }
    public static long occlusionMinIntervalNs() { return i(OCCLUSION_MIN_UPDATE_MS, 100) * 1_000_000L; }
    public static long occlusionStationaryIntervalNs() { return i(OCCLUSION_STATIONARY_UPDATE_MS, 200) * 1_000_000L; }

    public static double occlusionMoveEpsilonSq() {
        double value = d(OCCLUSION_MOVE_THRESHOLD, 0.15D);
        return value * value;
    }

    public static boolean adaptiveProbeCache() { return b(ADAPTIVE_PROBE_CACHE, true); }
    public static double probeFullRefreshDistance() { return d(PROBE_FULL_REFRESH_DISTANCE, 0.50D); }
    public static double probeCenterDelta() { return d(PROBE_CENTER_DELTA, 0.20D); }
    public static boolean diagnosticsEnabled() { return b(DIAGNOSTICS, false); }

    public static double probeFullRefreshDistanceSq() {
        double distance = probeFullRefreshDistance();
        return distance * distance;
    }
}
