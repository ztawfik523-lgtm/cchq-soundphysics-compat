package dev.cchqphysics.compat.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Runtime-approved V7.1 spreading-only aperture-energy model. */
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
        builder.push("portal_diffraction");

        ENABLED = builder.comment(
                "Runtime-approved V7.1 spreading-only aperture-energy diffraction model.",
                "Enabled by default after the V7.1 listening and performance validation pass.",
                "A verified opening only adds a bounded secondary energy contribution; it never replaces the direct path.",
                "Never changes source position, playback timing, synchronized starts, reflection routing, or reverb sends.")
                .define("enabled", true);

        MIN_SOURCE_ABOVE_LISTENER = builder.comment(
                "Source must be at least this far above the listener before the portal model is considered.",
                "This keeps the approved elevation correction out of unrelated same-height room acoustics.")
                .defineInRange("min_source_above_listener", 0.25D, 0.0D, 8.0D);

        ESCAPE_CLEARANCE = builder.comment(
                "Height above a detected roof plane used for the source-side aperture waypoint.",
                "For open-top geometry the same clearance is used by the implicit listener-column aperture.")
                .defineInRange("escape_clearance", 1.5D, 0.25D, 8.0D);

        OPENING_SEARCH_RADIUS = builder.comment(
                "Maximum listener-side roof-opening search radius in blocks.",
                "Discovery uses cheap BlockState reads from SPR's cloned level, not SPR occlusion rays.",
                "The last part of this radius is smoothly faded so there is no hard audible edge.")
                .defineInRange("search_radius", 8.0D, 1.0D, 8.0D);

        PORTAL_COUPLING = builder.comment(
                "Maximum amplitude coupling of the secondary aperture path before diffraction and leg losses.",
                "Conservative default: an opening may add clarity/loudness but cannot become a second full-strength direct source.")
                .defineInRange("portal_coupling", 0.25D, 0.0D, 1.0D);

        PORTAL_ACTIVATION_RAW = builder.comment(
                "Progressive raw occlusion at which portal contribution reaches full activation.",
                "Below this value activation follows a smoothstep, preventing openings from boosting already-clear direct sound.")
                .defineInRange("portal_activation_raw", 2.0D, 0.25D, 8.0D);

        LOW_DELTA_SCALE = builder.comment(
                "Path-length-difference scale for the low-band diffraction approximation.",
                "Larger values make low-frequency aperture energy persist farther into the acoustic shadow.")
                .defineInRange("low_delta_scale", 4.0D, 0.10D, 32.0D);

        HIGH_DELTA_SCALE = builder.comment(
                "Path-length-difference scale for the high-band diffraction approximation.",
                "Smaller than the low-band scale so highs attenuate more strongly as the detour worsens.")
                .defineInRange("high_delta_scale", 1.5D, 0.05D, 32.0D);

        APERTURE_SPREAD_SCALE = builder.comment(
                "Softened inverse-distance amplitude scale for explicit roof-opening leakage into the listener enclosure.",
                "This multiplies the exact V6 portal amplitude only; it does not alter V6 portal-leg spectral/transmission math.",
                "Implicit open-top geometry uses zero aperture distance to preserve the approved V6 open-top result.")
                .defineInRange("aperture_spread_scale", 3.0D, 0.25D, 16.0D);

        HORIZON_FADE_START_RATIO = builder.comment(
                "Fraction of search radius where an artificial scan-horizon fade begins.",
                "This exists only to prevent a portal contribution from disappearing abruptly at the finite search radius.")
                .defineInRange("horizon_fade_start_ratio", 0.75D, 0.0D, 0.99D);

        CANDIDATE_SEPARATION = builder.comment(
                "Minimum horizontal separation between the two expensive verified candidates.",
                "Prevents adjacent cells of the same one-block neighborhood from consuming both verification slots.")
                .defineInRange("candidate_separation", 2.0D, 0.0D, 8.0D);

        SELECTION_HYSTERESIS = builder.comment(
                "Cheap path-score advantage required before replacing the previously preferred aperture candidate.",
                "Reduces candidate-selection flicker when two openings have nearly equal path lengths.")
                .defineInRange("selection_hysteresis", 0.35D, 0.0D, 4.0D);

        OPENING_SCAN_INTERVAL_MS = builder.comment(
                "Minimum interval between shared listener-side topology scans.",
                "The scan reads only cached BlockStates from SPR's thread-safe level clone.")
                .defineInRange("scan_interval_ms", 1000.0D, 100.0D, 5000.0D);

        OPENING_LEG_RECHECK_DISTANCE = builder.comment(
                "Listener movement required before re-running a cached listener-to-aperture SPR leg.",
                "Path-length attenuation itself still updates from geometry whenever the direct environment is applied.")
                .defineInRange("leg_recheck_distance", 0.75D, 0.10D, 4.0D);

        OPENING_RAY_CACHE_MS = builder.comment(
                "Maximum age of verified aperture SPR legs while endpoints remain stable.",
                "Stationary scenes should therefore approach zero extra aperture SPR calls between rechecks.")
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
    public static void save() { SPEC.save(); }

    public static String summary() {
        return "diffraction=" + enabled()
                + " portalV7_1=true"
                + " minSourceAbove=" + minSourceAboveListener()
                + " clearance=" + escapeClearance()
                + " openingRadius=" + openingSearchRadius()
                + " portalCoupling=" + portalCoupling()
                + " portalActivationRaw=" + portalActivationRaw()
                + " lowDeltaScale=" + lowDeltaScale()
                + " highDeltaScale=" + highDeltaScale()
                + " apertureSpreadScale=" + apertureSpreadScale()
                + " horizonFadeStart=" + horizonFadeStartRatio()
                + " candidateSeparation=" + Math.sqrt(candidateSeparationSq())
                + " selectionHysteresis=" + selectionHysteresis()
                + " openingScanMs=" + (openingScanIntervalNs() / 1_000_000L)
                + " openingLegRecheck=" + Math.sqrt(openingLegRecheckDistanceSq())
                + " openingRayCacheMs=" + (openingRayCacheNs() / 1_000_000L);
    }
}
