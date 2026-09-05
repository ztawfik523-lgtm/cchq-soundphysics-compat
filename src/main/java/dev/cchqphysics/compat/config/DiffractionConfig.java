package dev.cchqphysics.compat.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Opening-aware vertical sound controls. */
public final class DiffractionConfig {
    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.BooleanValue ENABLED;
    private static final ModConfigSpec.DoubleValue MIN_SOURCE_ABOVE_LISTENER;
    private static final ModConfigSpec.DoubleValue ESCAPE_CLEARANCE;
    private static final ModConfigSpec.DoubleValue OPENING_SEARCH_RADIUS;
    private static final ModConfigSpec.DoubleValue PORTAL_COUPLING;
    private static final ModConfigSpec.DoubleValue PORTAL_ACTIVATION_RAW;
    private static final ModConfigSpec.DoubleValue LOW_DELTA_SCALE;
    private static final ModConfigSpec.DoubleValue HIGH_DELTA_SCALE;
    private static final ModConfigSpec.DoubleValue APERTURE_SPREAD_SCALE;
    private static final ModConfigSpec.DoubleValue HORIZON_FADE_START_RATIO;
    private static final ModConfigSpec.DoubleValue CANDIDATE_SEPARATION;
    private static final ModConfigSpec.DoubleValue SELECTION_HYSTERESIS;
    private static final ModConfigSpec.DoubleValue OPENING_SCAN_INTERVAL_MS;
    private static final ModConfigSpec.DoubleValue OPENING_LEG_RECHECK_DISTANCE;
    private static final ModConfigSpec.DoubleValue OPENING_RAY_CACHE_MS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("openings");

        ENABLED = builder.comment(
                "Lets blocked sound use a real nearby opening as a secondary path instead of relying only on the straight speaker-to-listener path.",
                "This mainly helps tunnels, shafts and lower floors sound less unnaturally sealed when an opening exists.",
                "It does not change playback timing, source position, synchronized starts or reverb routing.")
                .define("enabled", true);

        MIN_SOURCE_ABOVE_LISTENER = builder.comment(
                "Minimum vertical separation, in blocks, before opening-aware sound is considered.",
                "Higher = this feature is restricted to more clearly above/below situations.",
                "Lower = it can activate in shallower height differences.")
                .defineInRange("minimum_vertical_separation", 0.25D, 0.0D, 8.0D);

        ESCAPE_CLEARANCE = builder.comment(
                "How far above the detected ceiling/opening the source-side route point is placed, in blocks.",
                "Higher = the route clears thicker roof edges more aggressively.",
                "Lower = the route stays closer to the opening plane.")
                .defineInRange("opening_clearance", 1.5D, 0.25D, 8.0D);

        OPENING_SEARCH_RADIUS = builder.comment(
                "How far around the listener, in blocks, the mod looks for a usable ceiling opening.",
                "Higher = openings can be found farther away but scanning costs more CPU.",
                "Lower = cheaper scans, but distant openings are ignored.")
                .defineInRange("search_radius", 8.0D, 1.0D, 8.0D);

        PORTAL_COUPLING = builder.comment(
                "Overall strength of the opening effect.",
                "Higher = a usable opening makes blocked sound clearer/louder more strongly.",
                "Lower = the opening effect is subtler. 0 disables its contribution without disabling detection.")
                .defineInRange("effect_strength", 0.25D, 0.0D, 1.0D);

        PORTAL_ACTIVATION_RAW = builder.comment(
                "How blocked the normal direct sound must become before the opening effect reaches full strength.",
                "Higher = the opening needs stronger direct obstruction before reaching full effect.",
                "Lower = the opening reaches full effect sooner.")
                .defineInRange("full_effect_occlusion", 2.0D, 0.25D, 8.0D);

        LOW_DELTA_SCALE = builder.comment(
                "How well bass carries through an indirect opening route.",
                "Higher = more low-frequency sound survives a longer detour around the opening.",
                "Lower = bass falls off more strongly when the route is indirect.")
                .defineInRange("bass_carry", 4.0D, 0.10D, 32.0D);

        HIGH_DELTA_SCALE = builder.comment(
                "How well clarity/high frequencies carry through an indirect opening route.",
                "Higher = sound through an opening stays brighter/clearer on longer detours.",
                "Lower = highs fade more strongly, making indirect sound darker.")
                .defineInRange("clarity_carry", 1.5D, 0.05D, 32.0D);

        APERTURE_SPREAD_SCALE = builder.comment(
                "Controls how quickly an opening's effect weakens as you move away from it inside the enclosed area.",
                "Higher = the opening remains noticeable farther away.",
                "Lower = the effect fades sooner with distance.")
                .defineInRange("influence_distance", 3.0D, 0.25D, 16.0D);

        HORIZON_FADE_START_RATIO = builder.comment(
                "Where the opening effect starts fading near the outer edge of the search radius, as a fraction from 0 to 1.",
                "Higher = the effect stays stronger until closer to the search limit.",
                "Lower = it begins fading earlier.")
                .defineInRange("edge_fade_start", 0.75D, 0.0D, 0.99D);

        CANDIDATE_SEPARATION = builder.comment(
                "Minimum horizontal spacing, in blocks, between the two openings that may be fully checked.",
                "Higher = prefers openings that are farther apart instead of checking adjacent cells of the same hole.",
                "Lower = allows two nearby opening cells to use both verification slots.")
                .defineInRange("opening_separation", 2.0D, 0.0D, 8.0D);

        SELECTION_HYSTERESIS = builder.comment(
                "How much better a different opening must become before the mod switches to it.",
                "Higher = steadier selection with less switching between similar openings.",
                "Lower = reacts sooner when another opening becomes slightly better.")
                .defineInRange("switch_stability", 0.35D, 0.0D, 4.0D);

        OPENING_SCAN_INTERVAL_MS = builder.comment(
                "Minimum time between nearby-opening scans while staying in the same listener block.",
                "Lower = block changes are noticed sooner but use more CPU.",
                "Higher = cheaper scanning but slower reaction to changed geometry.")
                .defineInRange("scan_interval_ms", 1000.0D, 100.0D, 5000.0D);

        OPENING_LEG_RECHECK_DISTANCE = builder.comment(
                "How far the listener must move, in blocks, before a cached listener-to-opening path is checked again.",
                "Lower = more frequent geometry checks and higher CPU use.",
                "Higher = more reuse while moving.")
                .defineInRange("movement_recheck_distance", 0.75D, 0.10D, 4.0D);

        OPENING_RAY_CACHE_MS = builder.comment(
                "Maximum time a verified opening path may be reused while its endpoints remain stable.",
                "Higher = fewer extra Sound Physics checks in stable scenes.",
                "Lower = geometry is rechecked more often.")
                .defineInRange("ray_cache_ms", 5000.0D, 250.0D, 30000.0D);

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

    public static boolean enabled() { return b(ENABLED, true); }
    public static double minSourceAboveListener() { return d(MIN_SOURCE_ABOVE_LISTENER, 0.25D); }
    public static double escapeClearance() { return d(ESCAPE_CLEARANCE, 1.5D); }
    public static double openingSearchRadius() { return d(OPENING_SEARCH_RADIUS, 8.0D); }
    public static double portalCoupling() { return d(PORTAL_COUPLING, 0.25D); }
    public static double portalActivationRaw() { return d(PORTAL_ACTIVATION_RAW, 2.0D); }
    public static double lowDeltaScale() { return d(LOW_DELTA_SCALE, 4.0D); }
    public static double highDeltaScale() { return d(HIGH_DELTA_SCALE, 1.5D); }
    public static double apertureSpreadScale() { return d(APERTURE_SPREAD_SCALE, 3.0D); }
    public static double horizonFadeStartRatio() { return d(HORIZON_FADE_START_RATIO, 0.75D); }
    public static double selectionHysteresis() { return d(SELECTION_HYSTERESIS, 0.35D); }
    public static double candidateSeparationSq() {
        double value = Math.max(0.0D, d(CANDIDATE_SEPARATION, 2.0D));
        return value * value;
    }
    public static long openingScanIntervalNs() {
        return Math.max(100L, Math.round(d(OPENING_SCAN_INTERVAL_MS, 1000.0D))) * 1_000_000L;
    }
    public static double openingLegRecheckDistanceSq() {
        double distance = Math.max(0.10D, d(OPENING_LEG_RECHECK_DISTANCE, 0.75D));
        return distance * distance;
    }
    public static long openingRayCacheNs() {
        return Math.max(250L, Math.round(d(OPENING_RAY_CACHE_MS, 5000.0D))) * 1_000_000L;
    }

    public static void setEnabled(boolean value) { ENABLED.set(value); }
    public static void setMinSourceAboveListener(double value) { MIN_SOURCE_ABOVE_LISTENER.set(value); }
    public static void setEscapeClearance(double value) { ESCAPE_CLEARANCE.set(value); }
    public static void setOpeningSearchRadius(double value) { OPENING_SEARCH_RADIUS.set(value); }
    public static void setPortalCoupling(double value) { PORTAL_COUPLING.set(value); }
    public static void setPortalActivationRaw(double value) { PORTAL_ACTIVATION_RAW.set(value); }
    public static void setLowDeltaScale(double value) { LOW_DELTA_SCALE.set(value); }
    public static void setHighDeltaScale(double value) { HIGH_DELTA_SCALE.set(value); }
    public static void setApertureSpreadScale(double value) { APERTURE_SPREAD_SCALE.set(value); }
    public static void setHorizonFadeStartRatio(double value) { HORIZON_FADE_START_RATIO.set(value); }
    public static void setCandidateSeparation(double value) { CANDIDATE_SEPARATION.set(value); }
    public static void setSelectionHysteresis(double value) { SELECTION_HYSTERESIS.set(value); }
    public static void setOpeningScanIntervalMs(double value) { OPENING_SCAN_INTERVAL_MS.set(value); }
    public static void setOpeningLegRecheckDistance(double value) { OPENING_LEG_RECHECK_DISTANCE.set(value); }
    public static void setOpeningRayCacheMs(double value) { OPENING_RAY_CACHE_MS.set(value); }
    public static void save() { SPEC.save(); }

    public static String summary() {
        return "openings=" + enabled()
                + " minVertical=" + minSourceAboveListener()
                + " clearance=" + escapeClearance()
                + " searchRadius=" + openingSearchRadius()
                + " effectStrength=" + portalCoupling()
                + " fullEffectOcclusion=" + portalActivationRaw()
                + " bassCarry=" + lowDeltaScale()
                + " clarityCarry=" + highDeltaScale()
                + " influenceDistance=" + apertureSpreadScale()
                + " edgeFadeStart=" + horizonFadeStartRatio()
                + " openingSeparation=" + Math.sqrt(candidateSeparationSq())
                + " switchStability=" + selectionHysteresis()
                + " scanMs=" + (openingScanIntervalNs() / 1_000_000L)
                + " movementRecheck=" + Math.sqrt(openingLegRecheckDistanceSq())
                + " rayCacheMs=" + (openingRayCacheNs() / 1_000_000L);
    }
}
