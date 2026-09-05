package dev.cchqphysics.compat.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Keeps extreme clarity differences between synchronized copies from becoming distracting. */
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
        builder.push("synchronized_clarity_balance");
        ENABLED = builder.comment(
                "Reduces extreme muffling differences between synchronized copies of the same sound.",
                "Only the direct low-pass cutoff is adjusted; volume, position, playback timing and reverb sends are unchanged.",
                "Recommended: ON.")
                .define("enabled", true);
        DARK_SOURCE_CUTOFF = builder.comment(
                "How muffled a synchronized copy must be before it can receive clarity correction.",
                "Cutoff uses a 0 to 1 scale: 0 = extremely muffled, 1 = clear.",
                "Higher = more copies can qualify. Lower = correction is limited to more heavily muffled copies.")
                .defineInRange("muffled_copy_threshold", 0.35D, 0.0D, 1.0D);
        PEER_CLEAR_CUTOFF = builder.comment(
                "How clear another synchronized copy must be before it can act as the reference for correction.",
                "Higher = requires a clearer comparison copy. Lower = allows correction with a less-clear peer.")
                .defineInRange("clear_copy_threshold", 0.75D, 0.0D, 1.0D);
        MIN_PEER_GAP = builder.comment(
                "Minimum clarity difference required between the muffled copy and the clearest synchronized copy.",
                "Higher = only large mismatches are corrected. Lower = smaller differences can be corrected too.")
                .defineInRange("minimum_difference", 0.40D, 0.0D, 1.0D);
        CLARITY_FLOOR_RATIO = builder.comment(
                "How strongly an eligible muffled copy is moved toward the clearest synchronized copy.",
                "0 = no correction. 0.5 = halfway toward the peer. 1 = fully match the peer before the safety cap.")
                .defineInRange("correction_strength", 0.50D, 0.0D, 1.0D);
        MAX_CUTOFF_LIFT = builder.comment(
                "Maximum amount the clarity correction may raise one copy's cutoff.",
                "Higher = allows a larger correction. Lower = caps the change more aggressively.")
                .defineInRange("maximum_clarity_increase", 0.55D, 0.0D, 1.0D);
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
        return "syncBalance=" + enabled()
                + " muffledThreshold=" + darkSourceCutoff()
                + " clearThreshold=" + peerClearCutoff()
                + " minimumDifference=" + minPeerGap()
                + " correctionStrength=" + clarityFloorRatio()
                + " maxClarityIncrease=" + maxCutoffLift();
    }
}
