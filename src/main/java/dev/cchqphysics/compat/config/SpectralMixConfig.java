package dev.cchqphysics.compat.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Experimental synchronized-copy direct-HF compensation. */
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
                "Phase-5 HF50 candidate: reduce painful spectral skew between synchronized copies.",
                "Changes only direct low-pass cutoff; never source gain, source position, playback timing, or reverb-send filters.",
                "Enabled by default in this isolated test build because the user-selected 50% dose is what is under validation.")
                .define("enabled", true);
        PEER_CLEAR_CUTOFF = builder.comment(
                "At least one synchronized peer must be this clear before compensation is allowed.")
                .defineInRange("peer_clear_cutoff", 0.75D, 0.0D, 1.0D);
        CLARITY_FLOOR_RATIO = builder.comment(
                "Blend fraction toward the clearest synchronized peer after the dark-source and disparity gates pass.",
                "0.50 reproduces the user-preferred HF dose approximately when the clearest peer is near 1.0.")
                .defineInRange("clarity_floor_ratio", 0.50D, 0.0D, 0.75D);
        MAX_CUTOFF_LIFT = builder.comment(
                "Absolute safety cap on how much direct cutoff may be raised by the synchronized correction.")
                .defineInRange("max_cutoff_lift", 0.55D, 0.0D, 0.75D);
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

    public static boolean enabled() { return b(ENABLED, true); }
    public static double peerClearCutoff() { return d(PEER_CLEAR_CUTOFF, 0.75D); }
    public static double clarityFloorRatio() { return d(CLARITY_FLOOR_RATIO, 0.50D); }
    public static double maxCutoffLift() { return d(MAX_CUTOFF_LIFT, 0.55D); }
    public static void setEnabled(boolean value) { ENABLED.set(value); }
    public static void setPeerClearCutoff(double value) { PEER_CLEAR_CUTOFF.set(value); }
    public static void setClarityFloorRatio(double value) { CLARITY_FLOOR_RATIO.set(value); }
    public static void setMaxCutoffLift(double value) { MAX_CUTOFF_LIFT.set(value); }
    public static void save() { SPEC.save(); }

    public static String summary() {
        return "syncHf50=" + enabled()
                + " clearPeerCutoff=" + peerClearCutoff()
                + " peerBlend=" + clarityFloorRatio()
                + " maxCutoffLift=" + maxCutoffLift()
                + " darkSourceGate=0.35"
                + " minGapGate=0.40";
    }
}
