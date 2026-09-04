package dev.cchqphysics.compat.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Optional post-parity synchronized multi-speaker mixing controls.
 *
 * <p>The feature is OFF by default. Therefore the Phase-4/Hotfix3 acoustic
 * behavior and the already-runtime-validated Phase-5 candidate remain the
 * default behavior until the user explicitly enables this feature.</p>
 */
public final class MixClientConfig {
    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.BooleanValue OCCLUDED_SYNC_SUPPRESSION;
    private static final ModConfigSpec.DoubleValue SUPPRESSION_STRENGTH;
    private static final ModConfigSpec.DoubleValue OCCLUSION_THRESHOLD;
    private static final ModConfigSpec.DoubleValue MINIMUM_GAIN_FACTOR;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("synchronized_mix");
        OCCLUDED_SYNC_SUPPRESSION = builder
                .comment(
                        "Optional: reduce the source gain of occluded copies when multiple compat sources are playing the same synchronized payload.",
                        "OFF preserves Hotfix3 / Phase-4 behavior exactly. Clear sources are never suppressed by this feature.")
                .define("occluded_source_suppression", false);
        SUPPRESSION_STRENGTH = builder
                .comment(
                        "Exponential suppression strength applied to occlusion above the threshold when the feature is enabled.",
                        "Suggested starting value = 0.55. Higher values make clear synchronized speakers dominate more strongly.")
                .defineInRange("suppression_strength", 0.55D, 0.0D, 3.0D);
        OCCLUSION_THRESHOLD = builder
                .comment(
                        "Raw progressive occlusion below this value receives no extra synchronized-mix attenuation.",
                        "0.075 matches the Hotfix3 clearing-sentinel occluded threshold.")
                .defineInRange("occlusion_threshold", 0.075D, 0.0D, 4.0D);
        MINIMUM_GAIN_FACTOR = builder
                .comment(
                        "Lower bound for extra synchronized-mix attenuation. 0.30 means an occluded copy keeps at least 30% of its ordinary distance gain.",
                        "This preserves some blocked-source room/reverb character instead of muting the source.")
                .defineInRange("minimum_gain_factor", 0.30D, 0.0D, 1.0D);
        builder.pop();
        SPEC = builder.build();
    }

    private MixClientConfig() {}

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

    public static boolean enabled() { return b(OCCLUDED_SYNC_SUPPRESSION, false); }
    public static double strength() { return d(SUPPRESSION_STRENGTH, 0.55D); }
    public static double threshold() { return d(OCCLUSION_THRESHOLD, 0.075D); }
    public static double minimumGainFactor() { return d(MINIMUM_GAIN_FACTOR, 0.30D); }

    public static void setEnabled(boolean value) { OCCLUDED_SYNC_SUPPRESSION.set(value); }
    public static void setStrength(double value) { SUPPRESSION_STRENGTH.set(value); }
    public static void setThreshold(double value) { OCCLUSION_THRESHOLD.set(value); }
    public static void setMinimumGainFactor(double value) { MINIMUM_GAIN_FACTOR.set(value); }

    public static void save() { SPEC.save(); }

    public static String summary() {
        return "syncOccludedSuppression=" + enabled()
                + " syncSuppressionStrength=" + strength()
                + " syncOcclusionThreshold=" + threshold()
                + " syncMinimumGainFactor=" + minimumGainFactor();
    }
}
