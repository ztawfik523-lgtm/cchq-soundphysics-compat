package dev.cchqphysics.compat.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Experimental synchronized-copy spectral compensation. */
public final class SpectralMixConfig {
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.BooleanValue ENABLED;
    private static final ModConfigSpec.DoubleValue PEER_CLEAR_CUTOFF;
    private static final ModConfigSpec.DoubleValue CLARITY_FLOOR_RATIO;
    private static final ModConfigSpec.DoubleValue MAX_CUTOFF_LIFT;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("synchronized_spectral_mix");
        ENABLED = builder.comment(
                "Experimental: reduce excessive mud when synchronized copies are differently occluded.",
                "OFF preserves the already validated Phase-5 candidate behavior.",
                "Never changes source gain, source position, or reverb-send filters.")
                .define("enabled", false);
        PEER_CLEAR_CUTOFF = builder.comment(
                "At least one synchronized peer must be this clear before compensation is allowed.")
                .defineInRange("peer_clear_cutoff", 0.65D, 0.0D, 1.0D);
        CLARITY_FLOOR_RATIO = builder.comment(
                "Fraction of the clearest peer cutoff used as a conservative floor for very dark copies.")
                .defineInRange("clarity_floor_ratio", 0.18D, 0.0D, 0.75D);
        MAX_CUTOFF_LIFT = builder.comment(
                "Absolute cap on how much direct cutoff may be raised by compensation.")
                .defineInRange("max_cutoff_lift", 0.12D, 0.0D, 0.75D);
        builder.pop();
        SPEC = builder.build();
    }

    private SpectralMixConfig() {}

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
    public static double peerClearCutoff() { return d(PEER_CLEAR_CUTOFF, 0.65D); }
    public static double clarityFloorRatio() { return d(CLARITY_FLOOR_RATIO, 0.18D); }
    public static double maxCutoffLift() { return d(MAX_CUTOFF_LIFT, 0.12D); }
    public static void setEnabled(boolean value) { ENABLED.set(value); }
    public static void setPeerClearCutoff(double value) { PEER_CLEAR_CUTOFF.set(value); }
    public static void setClarityFloorRatio(double value) { CLARITY_FLOOR_RATIO.set(value); }
    public static void setMaxCutoffLift(double value) { MAX_CUTOFF_LIFT.set(value); }
    public static void save() { SPEC.save(); }

    public static String summary() {
        return "spectralMix=" + enabled()
                + " spectralPeerClearCutoff=" + peerClearCutoff()
                + " spectralFloorRatio=" + clarityFloorRatio()
                + " spectralMaxCutoffLift=" + maxCutoffLift();
    }
}
