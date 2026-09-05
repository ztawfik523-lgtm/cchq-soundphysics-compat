package dev.cchqphysics.compat.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Advanced runtime, performance and diagnostic controls.
 *
 * <p>Defaults are the release-tuned values. These settings are intentionally
 * separated from ordinary acoustic controls because most users should leave
 * them unchanged unless diagnosing performance or compatibility problems.</p>
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

    private static final ModConfigSpec.IntValue SYNC_PARTIAL_FLUSH_MS;
    private static final ModConfigSpec.IntValue SYNC_STALE_GROUP_MS;

    private static final ModConfigSpec.BooleanValue PRIVATE_EFX;
    private static final ModConfigSpec.BooleanValue BETA9_DIRECT_REUSE;
    private static final ModConfigSpec.BooleanValue BETA9_ROOM_BACKOFF;
    private static final ModConfigSpec.BooleanValue BETA9_ADAPTIVE_CONTROLLER;
    private static final ModConfigSpec.IntValue BETA9_RECENT_MOVEMENT_MS;
    private static final ModConfigSpec.DoubleValue BETA9_LISTENER_MOVE_DISTANCE;
    private static final ModConfigSpec.DoubleValue BETA9_MAX_ROOM_FACTOR;
    private static final ModConfigSpec.IntValue BETA9_MAX_ROOM_INTERVAL_MS;
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

        builder.push("room_updates");
        ROOM_SLOT_MS = builder
                .comment("Base spacing between room/reverb scheduling opportunities. Lower = more responsive but more CPU. Default: 50 ms.")
                .defineInRange("room_slot_ms", 50, 10, 500);
        MIN_HARD_STALE_MS = builder
                .comment("Earliest age at which an old room result may be forced fresh. Lower = fresher room updates but more work. Default: 500 ms.")
                .defineInRange("min_hard_stale_ms", 500, 100, 5000);
        MAX_HARD_STALE_MS = builder
                .comment("Longest a room result may remain stale before a forced refresh. Lower = fresher but more CPU. Default: 2000 ms.")
                .defineInRange("max_hard_stale_ms", 2000, 250, 10000);
        RECENT_SOURCE_MS = builder
                .comment("How long a recently active speaker stays eligible for room updates. Higher keeps inactive/recent speakers tracked longer. Default: 1000 ms.")
                .defineInRange("recent_source_ms", 1000, 100, 10000);
        TELEPORT_DISTANCE = builder
                .comment("Listener movement treated as a teleport and forcing room refreshes. Lower = smaller jumps trigger a full refresh. Default: 4 blocks.")
                .defineInRange("teleport_distance", 4.0D, 0.5D, 64.0D);
        SOURCE_MOVE_URGENT_DISTANCE = builder
                .comment("Speaker movement that makes its room state urgent. Lower = more sensitive to small source movement. Default: 0.1 blocks.")
                .defineInRange("source_move_urgent_distance", 0.10D, 0.0D, 8.0D);
        builder.pop();

        builder.push("clearing_detection");
        SENTINEL_MOVE_DISTANCE = builder
                .comment("Minimum listener movement before fast blocked-to-clear detection checks again. Lower = more sensitive. Default: 0.05 blocks.")
                .defineInRange("move_distance", 0.05D, 0.0D, 4.0D);
        SENTINEL_RAW_OCCLUDED = builder
                .comment("Obstruction level required before fast clearing detection can arm. Higher = requires a more blocked starting state. Default: 0.075.")
                .defineInRange("raw_occluded", 0.075D, 0.0D, 4.0D);
        SENTINEL_REARM_CENTER = builder
                .comment("Center-path obstruction needed to re-arm fast clearing detection after it has fired. Higher = requires stronger blockage. Default: 0.12.")
                .defineInRange("rearm_center", 0.12D, 0.0D, 4.0D);
        SENTINEL_OPEN_CENTER = builder
                .comment("Center-path obstruction treated as effectively open. Higher = more paths count as clear. Default: 0.035.")
                .defineInRange("open_center", 0.035D, 0.0D, 4.0D);
        SENTINEL_CENTER_DROP = builder
                .comment("Required improvement in the center path before a possible blocked-to-clear transition is considered. Lower = more sensitive. Default: 0.15.")
                .defineInRange("center_drop", 0.15D, 0.0D, 4.0D);
        CONFIRM_RAW_DROP = builder
                .comment("Required overall obstruction improvement to confirm a blocked-to-clear transition. Lower = easier to confirm. Default: 0.035.")
                .defineInRange("confirm_raw_drop", 0.035D, 0.0D, 4.0D);
        CONFIRM_CUTOFF_RISE = builder
                .comment("Required clarity/cutoff improvement to confirm a blocked-to-clear transition. Lower = easier to confirm. Default: 0.055.")
                .defineInRange("confirm_cutoff_rise", 0.055D, 0.0D, 1.0D);
        CLEAR_TRIGGER_COOLDOWN_MS = builder
                .comment("Minimum time between confirmed fast-clearing triggers. Higher prevents repeated triggers for longer. Default: 300 ms.")
                .defineInRange("clear_trigger_cooldown_ms", 300, 0, 5000);
        builder.pop();

        builder.push("synchronized_start");
        SYNC_PARTIAL_FLUSH_MS = builder
                .comment("How long an incomplete synchronized group waits for missing members before starting anyway. Higher = more waiting; lower = faster partial starts. Default: 100 ms.")
                .defineInRange("partial_flush_ms", 100, 0, 2000);
        SYNC_STALE_GROUP_MS = builder
                .comment("How long an abandoned pending synchronized group is kept before cleanup. Higher keeps pending state longer. Default: 5000 ms.")
                .defineInRange("stale_group_ms", 5000, 250, 30000);
        builder.pop();

        builder.push("optimizations");
        PRIVATE_EFX = builder
                .comment("Use isolated per-source OpenAL filters owned by the compat. Recommended: ON. Turn OFF only to diagnose filter-routing problems.")
                .define("private_source_filters", true);
        BETA9_DIRECT_REUSE = builder
                .comment("Reuse an unchanged direct acoustic result instead of recalculating it. ON saves CPU without changing the result.")
                .define("reuse_direct_acoustics", true);
        BETA9_ROOM_BACKOFF = builder
                .comment("Update stable or less-relevant room/reverb state less often. ON saves CPU; movement and stale-state rules still force refreshes.")
                .define("stable_room_slowdown", true);
        BETA9_ADAPTIVE_CONTROLLER = builder
                .comment("Allow current acoustic load to slow non-urgent room updates within the configured limits. ON reduces spikes under load.")
                .define("load_aware_room_scheduling", true);
        BETA9_RECENT_MOVEMENT_MS = builder
                .comment("Time after listener movement during which room updates stay responsive instead of slowing down. Higher keeps the fast mode longer. Default: 400 ms.")
                .defineInRange("movement_hold_ms", 400, 0, 5000);
        BETA9_LISTENER_MOVE_DISTANCE = builder
                .comment("Listener movement that resets stable-room tracking. Lower = more sensitive to movement. Default: 0.05 blocks.")
                .defineInRange("stability_reset_distance", 0.05D, 0.0D, 4.0D);
        BETA9_MAX_ROOM_FACTOR = builder
                .comment("Maximum amount stable/load-aware scheduling may slow room updates. 2.0 means at most twice the base interval.")
                .defineInRange("max_room_slowdown", 2.0D, 1.0D, 6.0D);
        BETA9_MAX_ROOM_INTERVAL_MS = builder
                .comment("Absolute longest room-update interval allowed after slowdown. Lower = fresher rooms; higher = cheaper. Default: 1500 ms.")
                .defineInRange("max_room_interval_ms", 1500, 50, 10000);
        BETA10_RAY_CACHE = builder
                .comment("Reuse identical obstruction ray results when their inputs have not changed. ON saves CPU without changing the reused result.")
                .define("reuse_occlusion_rays", true);
        BETA11_ROOM_RAY_MEMO = builder
                .comment("Reuse identical room/bounce ray results within the same Sound Physics world snapshot. ON saves repeated raycasts.")
                .define("reuse_room_rays", true);
        PERFORMANCE_REPORT_MS = builder
                .comment("How often performance diagnostics are printed while enabled. Lower = more frequent logs. Default: 10000 ms.")
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
    public static long minHardStaleNs() {
        int a = i(MIN_HARD_STALE_MS, 500);
        int b = i(MAX_HARD_STALE_MS, 2000);
        return Math.min(a, b) * 1_000_000L;
    }
    public static long maxHardStaleNs() {
        int a = i(MIN_HARD_STALE_MS, 500);
        int b = i(MAX_HARD_STALE_MS, 2000);
        return Math.max(a, b) * 1_000_000L;
    }
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

    public static long syncPartialFlushNs() { return i(SYNC_PARTIAL_FLUSH_MS, 100) * 1_000_000L; }
    public static long syncStaleGroupNs() {
        long partial = syncPartialFlushNs();
        long stale = i(SYNC_STALE_GROUP_MS, 5000) * 1_000_000L;
        return Math.max(partial, stale);
    }

    public static boolean privateEfxEnabled() { return b(PRIVATE_EFX, true); }
    public static boolean beta9DirectReuseEnabled() { return b(BETA9_DIRECT_REUSE, true); }
    public static boolean beta9RoomBackoffEnabled() { return b(BETA9_ROOM_BACKOFF, true); }
    public static boolean beta9AdaptiveControllerEnabled() { return b(BETA9_ADAPTIVE_CONTROLLER, true); }
    public static long beta9RecentMovementNs() { return i(BETA9_RECENT_MOVEMENT_MS, 400) * 1_000_000L; }
    public static double beta9ListenerMoveSq() {
        double value = d(BETA9_LISTENER_MOVE_DISTANCE, 0.05D);
        return value * value;
    }
    public static double beta9MaxRoomFactor() { return d(BETA9_MAX_ROOM_FACTOR, 2.0D); }
    public static long beta9MaxRoomIntervalNs() { return i(BETA9_MAX_ROOM_INTERVAL_MS, 1500) * 1_000_000L; }
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

    public static String summary() {
        return "roomSlotMs=" + roomSlotNs() / 1_000_000L
                + " minHardStaleMs=" + minHardStaleNs() / 1_000_000L
                + " maxHardStaleMs=" + maxHardStaleNs() / 1_000_000L
                + " recentSourceMs=" + recentSourceNs() / 1_000_000L
                + " syncPartialMs=" + syncPartialFlushNs() / 1_000_000L
                + " syncStaleMs=" + syncStaleGroupNs() / 1_000_000L
                + " privateEfx=" + privateEfxEnabled()
                + " directReuse=" + beta9DirectReuseEnabled()
                + " stableRoomSlowdown=" + beta9RoomBackoffEnabled()
                + " loadAwareScheduling=" + beta9AdaptiveControllerEnabled()
                + " maxRoomSlowdown=" + beta9MaxRoomFactor()
                + " maxRoomMs=" + beta9MaxRoomIntervalNs() / 1_000_000L
                + " occlusionRayReuse=" + beta10RayCacheEnabled()
                + " roomRayReuse=" + beta11RoomRayMemoEnabled()
                + " perfReportMs=" + performanceReportNs() / 1_000_000L;
    }
}
