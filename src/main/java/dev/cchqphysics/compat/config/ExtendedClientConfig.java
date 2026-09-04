package dev.cchqphysics.compat.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Phase-5 test/extended controls.
 *
 * <p>Every default is intentionally the Phase-4 / Hotfix3-equivalent value so
 * simply installing the extended build does not change the verified acoustic
 * behavior. These controls exist to make real-game validation and diagnosis
 * easier without destroying the frozen parity branch.</p>
 */
public final class ExtendedClientConfig {
    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.IntValue ROOM_SLOT_MS;
    private static final ModConfigSpec.IntValue MIN_HARD_STALE_MS;
    private static final ModConfigSpec.IntValue MAX_HARD_STALE_MS;
    private static final ModConfigSpec.IntValue RECENT_SOURCE_MS;
    private static final ModConfigSpec.DoubleValue TELEPORT_DISTANCE;
    private static final ModConfigSpec.DoubleValue SOURCE_MOVE_URGENT_DISTANCE;

    private static final ModConfigSpec.DoubleValue SENTINEL_MOVE_DISTANCE;
    private static final ModConfigSpec.DoubleValue SENTINEL_RAW_OCCLUDED;
    private static final ModConfigSpec.DoubleValue SENTINEL_REARM_CENTER;
    private static final ModConfigSpec.DoubleValue SENTINEL_OPEN_CENTER;
    private static final ModConfigSpec.DoubleValue SENTINEL_CENTER_DROP;
    private static final ModConfigSpec.DoubleValue CONFIRM_RAW_DROP;
    private static final ModConfigSpec.DoubleValue CONFIRM_CUTOFF_RISE;
    private static final ModConfigSpec.IntValue CLEAR_TRIGGER_COOLDOWN_MS;

    private static final ModConfigSpec.BooleanValue PRIVATE_EFX;
    private static final ModConfigSpec.BooleanValue BETA10_RAY_CACHE;
    private static final ModConfigSpec.BooleanValue BETA11_ROOM_RAY_MEMO;
    private static final ModConfigSpec.IntValue PERFORMANCE_REPORT_MS;

    private static final ModConfigSpec.BooleanValue LOG_SOURCE_LIFECYCLE;
    private static final ModConfigSpec.BooleanValue LOG_ROOM_SCHEDULER;
    private static final ModConfigSpec.BooleanValue LOG_SENTINEL;
    private static final ModConfigSpec.BooleanValue LOG_EFX;
    private static final ModConfigSpec.BooleanValue LOG_CACHE;
    private static final ModConfigSpec.BooleanValue LOG_SYNC;
    private static final ModConfigSpec.BooleanValue LOG_TRANSITIONS;
    private static final ModConfigSpec.BooleanValue LOG_CONFIG;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("scheduler");
        ROOM_SLOT_MS = builder
                .comment("Minimum global room-scheduler slot. Hotfix3 = 50 ms.")
                .defineInRange("room_slot_ms", 50, 10, 500);
        MIN_HARD_STALE_MS = builder
                .comment("Minimum room-target hard-stale threshold. Hotfix3 = 500 ms.")
                .defineInRange("min_hard_stale_ms", 500, 100, 5000);
        MAX_HARD_STALE_MS = builder
                .comment("Maximum room-target hard-stale threshold. Hotfix3 = 2000 ms.")
                .defineInRange("max_hard_stale_ms", 2000, 250, 10000);
        RECENT_SOURCE_MS = builder
                .comment("How recently a source must have been seen to remain scheduler-eligible. Hotfix3 = 1000 ms.")
                .defineInRange("recent_source_ms", 1000, 100, 10000);
        TELEPORT_DISTANCE = builder
                .comment("Listener movement in blocks treated as a teleport, forcing room refreshes. Hotfix3 = 4.0 blocks.")
                .defineInRange("teleport_distance", 4.0D, 0.5D, 64.0D);
        SOURCE_MOVE_URGENT_DISTANCE = builder
                .comment("Speaker movement in blocks that marks its room state urgent. Hotfix3 = 0.1 blocks.")
                .defineInRange("source_move_urgent_distance", 0.10D, 0.0D, 8.0D);
        builder.pop();

        builder.push("sentinel");
        SENTINEL_MOVE_DISTANCE = builder
                .comment("Minimum listener movement in blocks before the clearing sentinel samples. Hotfix3 = 0.05.")
                .defineInRange("move_distance", 0.05D, 0.0D, 4.0D);
        SENTINEL_RAW_OCCLUDED = builder
                .comment("Raw progressive-occlusion level that can arm clearing detection. Hotfix3 = 0.075.")
                .defineInRange("raw_occluded", 0.075D, 0.0D, 4.0D);
        SENTINEL_REARM_CENTER = builder
                .comment("Center-ray occlusion used to re-arm clearing detection. Hotfix3 = 0.12.")
                .defineInRange("rearm_center", 0.12D, 0.0D, 4.0D);
        SENTINEL_OPEN_CENTER = builder
                .comment("Center-ray value treated as effectively open. Hotfix3 = 0.035.")
                .defineInRange("open_center", 0.035D, 0.0D, 4.0D);
        SENTINEL_CENTER_DROP = builder
                .comment("Required center-ray drop for a clearing candidate. Hotfix3 = 0.15.")
                .defineInRange("center_drop", 0.15D, 0.0D, 4.0D);
        CONFIRM_RAW_DROP = builder
                .comment("Required raw-occlusion drop to confirm a clearing transition. Hotfix3 = 0.035.")
                .defineInRange("confirm_raw_drop", 0.035D, 0.0D, 4.0D);
        CONFIRM_CUTOFF_RISE = builder
                .comment("Required direct-cutoff rise to confirm a clearing transition. Hotfix3 = 0.055.")
                .defineInRange("confirm_cutoff_rise", 0.055D, 0.0D, 1.0D);
        CLEAR_TRIGGER_COOLDOWN_MS = builder
                .comment("Cooldown between confirmed clearing triggers. Hotfix3 = 300 ms.")
                .defineInRange("clear_trigger_cooldown_ms", 300, 0, 5000);
        builder.pop();

        builder.push("features");
        PRIVATE_EFX = builder
                .comment("Use compat-owned per-source EFX filters. Disable only for diagnosis; native SPR fallback is then used. Hotfix3 = true.")
                .define("private_efx", true);
        BETA10_RAY_CACHE = builder
                .comment("Enable exact direct/SPR occlusion-ray reuse when the room stamp is reusable. Hotfix3 = true.")
                .define("beta10_ray_cache", true);
        BETA11_ROOM_RAY_MEMO = builder
                .comment("Enable same-clone memoization for SPR environment/bounce raycasts. Hotfix3 = true.")
                .define("beta11_room_ray_memo", true);
        PERFORMANCE_REPORT_MS = builder
                .comment("Period for compat performance reports when diagnostics are enabled. Hotfix3 = 10000 ms.")
                .defineInRange("performance_report_ms", 10000, 1000, 60000);
        builder.pop();

        builder.push("debug");
        LOG_SOURCE_LIFECYCLE = builder.comment("Log compat source register/unregister and source-identity lifecycle events.")
                .define("source_lifecycle", false);
        LOG_ROOM_SCHEDULER = builder.comment("Log room scheduler selection/reuse/refresh decisions. Can be noisy.")
                .define("room_scheduler", false);
        LOG_SENTINEL = builder.comment("Log clearing-sentinel arm/candidate/confirmation decisions.")
                .define("sentinel", false);
        LOG_EFX = builder.comment("Log private EFX create/apply/destroy/fallback events.")
                .define("efx", false);
        LOG_CACHE = builder.comment("Log cache scope resets and cache feature-state decisions. Avoids per-ray spam.")
                .define("cache", false);
        LOG_SYNC = builder.comment("Log synchronized-start grouping/flush decisions.")
                .define("sync", false);
        LOG_TRANSITIONS = builder.comment("Log detected open/blocked acoustic transition timing.")
                .define("transitions", false);
        LOG_CONFIG = builder.comment("Log the effective advanced/debug configuration at startup and when requested by diagnostics.")
                .define("config", false);
        builder.pop();

        SPEC = builder.build();
    }

    private ExtendedClientConfig() {}

    private static boolean b(ModConfigSpec.BooleanValue value, boolean fallback) {
        try { return value.get(); } catch (Throwable ignored) { return fallback; }
    }

    private static int i(ModConfigSpec.IntValue value, int fallback) {
        try {
            Integer result = value.get();
            return result == null ? fallback : result;
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    private static double d(ModConfigSpec.DoubleValue value, double fallback) {
        try {
            Double result = value.get();
            return result == null ? fallback : result;
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    public static long roomSlotNs() { return i(ROOM_SLOT_MS, 50) * 1_000_000L; }
    public static long minHardStaleNs() { return i(MIN_HARD_STALE_MS, 500) * 1_000_000L; }
    public static long maxHardStaleNs() { return i(MAX_HARD_STALE_MS, 2000) * 1_000_000L; }
    public static long recentSourceNs() { return i(RECENT_SOURCE_MS, 1000) * 1_000_000L; }

    public static double teleportDistanceSq() {
        double value = d(TELEPORT_DISTANCE, 4.0D);
        return value * value;
    }

    public static double sourceMoveUrgentSq() {
        double value = d(SOURCE_MOVE_URGENT_DISTANCE, 0.10D);
        return value * value;
    }

    public static double sentinelMoveSq() {
        double value = d(SENTINEL_MOVE_DISTANCE, 0.05D);
        return value * value;
    }

    public static double sentinelRawOccluded() { return d(SENTINEL_RAW_OCCLUDED, 0.075D); }
    public static double sentinelRearmCenter() { return d(SENTINEL_REARM_CENTER, 0.12D); }
    public static double sentinelOpenCenter() { return d(SENTINEL_OPEN_CENTER, 0.035D); }
    public static double sentinelCenterDrop() { return d(SENTINEL_CENTER_DROP, 0.15D); }
    public static double confirmRawDrop() { return d(CONFIRM_RAW_DROP, 0.035D); }
    public static float confirmCutoffRise() { return (float) d(CONFIRM_CUTOFF_RISE, 0.055D); }
    public static long clearTriggerCooldownNs() { return i(CLEAR_TRIGGER_COOLDOWN_MS, 300) * 1_000_000L; }

    public static boolean privateEfxEnabled() { return b(PRIVATE_EFX, true); }
    public static boolean beta10RayCacheEnabled() { return b(BETA10_RAY_CACHE, true); }
    public static boolean beta11RoomRayMemoEnabled() { return b(BETA11_ROOM_RAY_MEMO, true); }
    public static long performanceReportNs() { return i(PERFORMANCE_REPORT_MS, 10000) * 1_000_000L; }

    public static boolean logSourceLifecycle() { return b(LOG_SOURCE_LIFECYCLE, false); }
    public static boolean logRoomScheduler() { return b(LOG_ROOM_SCHEDULER, false); }
    public static boolean logSentinel() { return b(LOG_SENTINEL, false); }
    public static boolean logEfx() { return b(LOG_EFX, false); }
    public static boolean logCache() { return b(LOG_CACHE, false); }
    public static boolean logSync() { return b(LOG_SYNC, false); }
    public static boolean logTransitions() { return b(LOG_TRANSITIONS, false); }
    public static boolean logConfig() { return b(LOG_CONFIG, false); }
}
