package dev.cchqphysics.compat.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Configurable synchronized-copy direct-HF compensation using the validated final defaults. */
public final class SpectralMixConfig {
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.BooleanValue ENABLED;
    private static final ModConfigSpec.DoubleValue DARK_SOURCE_CUTOFF;
    private static final ModConfigSpec.DoubleValue PEER_CLEAR_CUTOFF;
    private static final ModConfigSpec.DoubleValue MIN_PEER_GAP;
    private static final ModConfigSpec.DoubleValue CLARITY_FLOOR_RATIO;
    private static final ModConfigSpec.DoubleValue MAX_CUTOFF_LIFT;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("synchronized_spectral_mix");
        ENABLED = builder.comment(
                "Reduce harsh spectral skew between synchronized copies of the same audio.",
                "Changes only direct low-pass cutoff; never source gain, source position, playback timing, or reverb-send filters.",
                "Enabled by default because this compensation passed runtime listening validation.")
                .define("enabled", true);
        DARK_SOURCE_CUTOFF = builder.comment(
                "Only synchronized sources at or below this intrinsic direct cutoff are eligible for HF lift.",
                "0.35 is the validated conservative gate.")
                .defineInRange("dark_source_cutoff", 0.35D, 0.0D, 1.0D);
        PEER_CLEAR_CUTOFF = builder.comment(
                "At least one synchronized peer must be this clear before compensation is allowed.",
                "0.75 is the validated default.")
                .defineInRange("peer_clear_cutoff", 0.75D, 0.0D, 1.0D);
        MIN_PEER_GAP = builder.comment(
                "Minimum cutoff difference between the dark source and clearest synchronized peer before compensation is allowed.",
                "0.40 prevents small normal acoustic differences from being flattened.")
                .defineInRange("min_peer_gap", 0.40D, 0.0D, 1.0D);
        CLARITY_FLOOR_RATIO = builder.comment(
                "Blend fraction toward the clearest synchronized peer after all gates pass.",
                "0.50 is the validated listening balance. 0 disables lift; 1.0 would fully match the peer before the max-lift cap.")
                .defineInRange("clarity_floor_ratio", 0.50D, 0.0D, 1.0D);
        MAX_CUTOFF_LIFT = builder.comment(
                "Absolute safety cap on how much direct cutoff may be raised by the synchronized correction.",
                "0.55 is the validated cap.")
                .defineInRange("max_cutoff_lift", 0.55D, 0.0D, 1.0D);
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
    public static double darkSourceCutoff() { return d(DARK_SOURCE_CUTOFF, 0.35D); }
    public static double peerClearCutoff() { return d(PEER_CLEAR_CUTOFF, 0.75D); }
    public static double minPeerGap() { return d(MIN_PEER_GAP, 0.40D); }
    public static double clarityFloorRatio() { return d(CLARITY_FLOOR_RATIO, 0.50D); }
    public static double maxCutoffLift() { return d(MAX_CUTOFF_LIFT, 0.55D); }

    public static void setEnabled(boolean value) { ENABLED.set(value); }
    public static void setDarkSourceCutoff(double value) { DARK_SOURCE_CUTOFF.set(value); }
    public static void setPeerClearCutoff(double value) { PEER_CLEAR_CUTOFF.set(value); }
    public static void setMinPeerGap(double value) { MIN_PEER_GAP.set(value); }
    public static void setClarityFloorRatio(double value) { CLARITY_FLOOR_RATIO.set(value); }
    public static void setMaxCutoffLift(double value) { MAX_CUTOFF_LIFT.set(value); }
    public static void save() { SPEC.save(); }

    public static String summary() {
        return "syncHf50=" + enabled()
                + " darkSourceGate=" + darkSourceCutoff()
                + " clearPeerCutoff=" + peerClearCutoff()
                + " minGapGate=" + minPeerGap()
                + " peerBlend=" + clarityFloorRatio()
                + " maxCutoffLift=" + maxCutoffLift();
    }
}
