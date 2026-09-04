from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def replace(path: str, old: str, new: str, count: int = 1) -> None:
    p = ROOT / path
    text = p.read_text(encoding="utf-8")
    actual = text.count(old)
    if actual != count:
        raise SystemExit(f"{path}: expected {count} occurrence(s), found {actual}: {old[:100]!r}")
    p.write_text(text.replace(old, new, count), encoding="utf-8")

# ---------------------------------------------------------------------------
# Main mod: install the client-only debug command registration.
# ---------------------------------------------------------------------------
p = "src/main/java/dev/cchqphysics/compat/CCHQSoundPhysicsCompat.java"
replace(p,
'''import dev.cchqphysics.compat.config.ClientConfig;
''',
'''import dev.cchqphysics.compat.audio.DebugCommands;
import dev.cchqphysics.compat.config.ClientConfig;
''')
replace(p,
'''        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigScreenFactory::create);
''',
'''        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigScreenFactory::create);
        DebugCommands.init();
''')

# ---------------------------------------------------------------------------
# Extended config: normalized ranges + Beta9 room/backoff controls.
# ---------------------------------------------------------------------------
p = "src/main/java/dev/cchqphysics/compat/config/ExtendedClientConfig.java"
replace(p,
'''    private static final ModConfigSpec.BooleanValue PRIVATE_EFX;
    private static final ModConfigSpec.BooleanValue BETA9_DIRECT_REUSE;
''',
'''    private static final ModConfigSpec.BooleanValue PRIVATE_EFX;
    private static final ModConfigSpec.BooleanValue BETA9_DIRECT_REUSE;
    private static final ModConfigSpec.BooleanValue BETA9_ROOM_BACKOFF;
    private static final ModConfigSpec.BooleanValue BETA9_ADAPTIVE_CONTROLLER;
    private static final ModConfigSpec.IntValue BETA9_RECENT_MOVEMENT_MS;
    private static final ModConfigSpec.DoubleValue BETA9_LISTENER_MOVE_DISTANCE;
    private static final ModConfigSpec.DoubleValue BETA9_MAX_ROOM_FACTOR;
    private static final ModConfigSpec.IntValue BETA9_MAX_ROOM_INTERVAL_MS;
''')
replace(p,
'''        BETA9_DIRECT_REUSE = builder
                .comment("Enable exact whole-direct-result reuse when source/environment inputs are unchanged. Hotfix3 = true.")
                .define("beta9_direct_reuse", true);
        BETA10_RAY_CACHE = builder
''',
'''        BETA9_DIRECT_REUSE = builder
                .comment("Enable exact whole-direct-result reuse when source/environment inputs are unchanged. Hotfix3 = true.")
                .define("beta9_direct_reuse", true);
        BETA9_ROOM_BACKOFF = builder
                .comment("Enable stable/relevance room-interval backoff. Hotfix3 = true.")
                .define("beta9_room_backoff", true);
        BETA9_ADAPTIVE_CONTROLLER = builder
                .comment("Enable load-pressure contribution to room backoff. Hotfix3 = true.")
                .define("beta9_adaptive_controller", true);
        BETA9_RECENT_MOVEMENT_MS = builder
                .comment("Window after listener movement during which stability backoff is suppressed. Hotfix3 = 400 ms.")
                .defineInRange("beta9_recent_movement_ms", 400, 0, 5000);
        BETA9_LISTENER_MOVE_DISTANCE = builder
                .comment("Listener movement that resets Beta9 stability state. Hotfix3 = 0.05 blocks.")
                .defineInRange("beta9_listener_move_distance", 0.05D, 0.0D, 4.0D);
        BETA9_MAX_ROOM_FACTOR = builder
                .comment("Maximum combined room-interval backoff multiplier. Hotfix3 = 2.0.")
                .defineInRange("beta9_max_room_factor", 2.0D, 1.0D, 6.0D);
        BETA9_MAX_ROOM_INTERVAL_MS = builder
                .comment("Absolute ceiling for a backed-off room interval. Hotfix3 = 1500 ms.")
                .defineInRange("beta9_max_room_interval_ms", 1500, 50, 10000);
        BETA10_RAY_CACHE = builder
''')
replace(p,
'''    public static long roomSlotNs() { return i(ROOM_SLOT_MS, 50) * 1_000_000L; }
    public static long minHardStaleNs() { return i(MIN_HARD_STALE_MS, 500) * 1_000_000L; }
    public static long maxHardStaleNs() { return i(MAX_HARD_STALE_MS, 2000) * 1_000_000L; }
''',
'''    public static long roomSlotNs() { return i(ROOM_SLOT_MS, 50) * 1_000_000L; }
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
''')
replace(p,
'''    public static long syncPartialFlushNs() { return i(SYNC_PARTIAL_FLUSH_MS, 100) * 1_000_000L; }
    public static long syncStaleGroupNs() { return i(SYNC_STALE_GROUP_MS, 5000) * 1_000_000L; }

    public static boolean privateEfxEnabled() { return b(PRIVATE_EFX, true); }
    public static boolean beta9DirectReuseEnabled() { return b(BETA9_DIRECT_REUSE, true); }
''',
'''    public static long syncPartialFlushNs() { return i(SYNC_PARTIAL_FLUSH_MS, 100) * 1_000_000L; }
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
''')
replace(p,
'''                + " beta9DirectReuse=" + beta9DirectReuseEnabled()
                + " beta10RayCache=" + beta10RayCacheEnabled()
''',
'''                + " beta9DirectReuse=" + beta9DirectReuseEnabled()
                + " beta9RoomBackoff=" + beta9RoomBackoffEnabled()
                + " beta9Adaptive=" + beta9AdaptiveControllerEnabled()
                + " beta9MaxFactor=" + beta9MaxRoomFactor()
                + " beta9MaxRoomMs=" + beta9MaxRoomIntervalNs() / 1_000_000L
                + " beta10RayCache=" + beta10RayCacheEnabled()
''')

# ---------------------------------------------------------------------------
# SoundPhysicsBridge: consume safe command requests on sound thread + snapshots.
# ---------------------------------------------------------------------------
p = "src/main/java/dev/cchqphysics/compat/audio/SoundPhysicsBridge.java"
replace(p,
'''    static void schedulerTick() {
        long now = System.nanoTime();
''',
'''    static void schedulerTick() {
        DebugControl.consumeSoundThreadRequests();
        long now = System.nanoTime();
''')
replace(p,
'''    private static synchronized double updateSchedulerListener(Vec3 camera) {
''',
'''    static synchronized void debugForceRoomRefreshNow() {
        for (SourceState state : STATES.values()) {
            if (state.playing && state.inRange) {
                state.urgent = true;
                state.roomStamp = null;
                state.lastRoomNs = 0L;
            }
        }
    }

    static synchronized String debugSummary() {
        int playing = 0;
        int inRange = 0;
        int urgent = 0;
        int withRoom = 0;
        for (SourceState state : STATES.values()) {
            if (state.playing) playing++;
            if (state.inRange) inRange++;
            if (state.urgent) urgent++;
            if (state.room != null) withRoom++;
        }
        return "sources=" + STATES.size() + " playing=" + playing + " inRange=" + inRange
                + " urgent=" + urgent + " rooms=" + withRoom;
    }

    private static synchronized double updateSchedulerListener(Vec3 camera) {
''')

# ---------------------------------------------------------------------------
# Beta9: expose high-level adaptive room controls and safe cache reset/status.
# ---------------------------------------------------------------------------
p = "src/main/java/dev/cchqphysics/compat/audio/Beta9Optimizer.java"
replace(p,
'''    private static final long RECENT_MOVEMENT_NS = 400_000_000L;
    private static final double LISTENER_MOVE_SQ = 0.0025D;
''',
'''    // Phase 5 exposes the Hotfix3 movement/backoff values through ExtendedClientConfig.
''')
replace(p,
'''        if (Double.isFinite(movementSq) && movementSq >= LISTENER_MOVE_SQ) {
''',
'''        if (Double.isFinite(movementSq) && movementSq >= ExtendedClientConfig.beta9ListenerMoveSq()) {
''')
replace(p,
'''                && System.nanoTime() - lastListenerMoveNs < RECENT_MOVEMENT_NS;
''',
'''                && System.nanoTime() - lastListenerMoveNs < ExtendedClientConfig.beta9RecentMovementNs();
''')
replace(p,
'''        boolean recentlyMoved = lastListenerMoveNs != 0L && now - lastListenerMoveNs < RECENT_MOVEMENT_NS;
        double stableFactor = 1.0D;
''',
'''        boolean recentlyMoved = lastListenerMoveNs != 0L && now - lastListenerMoveNs < ExtendedClientConfig.beta9RecentMovementNs();
        if (!ExtendedClientConfig.beta9RoomBackoffEnabled()) {
            maybeReportAndControl(now);
            return baseIntervalNs;
        }
        double stableFactor = 1.0D;
''')
replace(p,
'''        double adaptive = adaptiveFactor;
''',
'''        double adaptive = ExtendedClientConfig.beta9AdaptiveControllerEnabled() ? adaptiveFactor : 1.0D;
''')
replace(p,
'''        double totalFactor = Math.min(2.0D, Math.max(1.0D, environmentalFactor * adaptive));
''',
'''        double totalFactor = Math.min(ExtendedClientConfig.beta9MaxRoomFactor(),
                Math.max(1.0D, environmentalFactor * adaptive));
''')
replace(p,
'''            result = Math.min(result, 1_500_000_000L);
''',
'''            result = Math.min(result, ExtendedClientConfig.beta9MaxRoomIntervalNs());
''')
replace(p,
'''    private static boolean reflectedStable(SourceMeta meta, boolean have, double x, double y, double z) {
''',
'''    static synchronized void debugResetCaches() {
        DIRECT.clear();
        PENDING.remove();
        for (SourceMeta meta : META.values()) {
            meta.stableCount = 0;
            meta.haveRoom = false;
        }
        adaptiveFactor = 1.0D;
        reportMinAdaptive = 1.0D;
        reportMaxAdaptive = 1.0D;
        pressureWindows = 0;
        healthyWindows = 0;
        ctrlAcousticNs = ctrlSprNs = ctrlQueueNs = ctrlQueueMaxNs = ctrlQueueSamples = ctrlSprCalls = 0L;
        lastAcousticMsPerSec = lastSprMsPerSec = lastQueueAvgMs = lastQueueMaxMs = 0.0D;
        controlStartNs = System.nanoTime();
    }

    static synchronized String debugSummary() {
        return "beta9Meta=" + META.size() + " directCache=" + DIRECT.size()
                + " load=" + round2(adaptiveFactor) + " directReal=" + directReal + " directReuse=" + directReuse;
    }

    private static boolean reflectedStable(SourceMeta meta, boolean have, double x, double y, double z) {
''')

# ---------------------------------------------------------------------------
# Beta10: clear only optimization metadata, not active source ownership.
# ---------------------------------------------------------------------------
p = "src/main/java/dev/cchqphysics/compat/audio/Beta10Optimizer.java"
replace(p,
'''    public static boolean beta11RoomCacheActive() {
''',
'''    static synchronized void debugResetCaches() {
        Arrays.fill(rayUsed, false);
        Arrays.fill(rayOwner, OWNER_NONE);
        scopeClone = null;
        scopeCloneTick = Long.MIN_VALUE;
        scopeConfig = Long.MIN_VALUE;
        filterStates.clear();
        sourceAlStates.clear();
        debugLastReadNs = 0L;
        debugAllowsCache = false;
        debugReflectionFailed = false;
        sprConfig = null;
        renderOcclusionField = null;
        occlusionLoggingField = null;
        configEntryGet = null;
    }

    static synchronized String debugSummary() {
        int used = 0;
        for (boolean value : rayUsed) if (value) used++;
        return "beta10Active=" + activeSources.size() + " inaudible=" + inaudibleSources.size()
                + " rayEntries=" + used + " rayHit=" + rayHits + " rayMiss=" + rayMisses
                + " filterSkip=" + filterSkips + " sourceSkip=" + sourceSkips;
    }

    public static boolean beta11RoomCacheActive() {
''')

# ---------------------------------------------------------------------------
# Beta11: make manual reset/snapshot synchronized.
# ---------------------------------------------------------------------------
p = "src/main/java/dev/cchqphysics/compat/audio/Beta11RoomRayCache.java"
replace(p, "    static void clear() {\n", "    static synchronized void clear() {\n")
replace(p, "    static long[] statsForTest() {\n", "    static synchronized long[] statsForTest() {\n")
replace(p,
'''    private static final class CacheBank {
''',
'''    static synchronized String debugSummary() {
        return "beta11Entries=" + current.entries + " hit=" + hits + " miss=" + misses
                + " crossCloneTelemetry=" + crossCloneWouldReuse + " scopeResets=" + scopeResets;
    }

    private static final class CacheBank {
''')

# ---------------------------------------------------------------------------
# Progressive direct cache invalidation without dropping source positions.
# ---------------------------------------------------------------------------
p = "src/main/java/dev/cchqphysics/compat/audio/ProgressiveOcclusionModel.java"
replace(p,
'''    public static double sampleCenterSentinel(int sourceId, Vec3 listener) throws Exception {
''',
'''    static synchronized void debugInvalidateCaches() {
        for (State state : STATES.values()) {
            state.valid = false;
            state.ringsValid = false;
            state.sourceRingsValid = false;
        }
        applyOverrideSource = Integer.MIN_VALUE;
    }

    public static double sampleCenterSentinel(int sourceId, Vec3 listener) throws Exception {
''')

# ---------------------------------------------------------------------------
# Environment and sync snapshots (read-only).
# ---------------------------------------------------------------------------
p = "src/main/java/dev/cchqphysics/compat/audio/EnvironmentSmoother.java"
replace(p,
'''    private static boolean createPrivateEfx(int sourceId, State state) {
''',
'''    static synchronized String debugSummary() {
        int initialized = 0;
        int ready = 0;
        int failed = 0;
        for (State state : STATES.values()) {
            if (state.initialized) initialized++;
            if (state.privateEfxReady) ready++;
            if (state.privateEfxFailed) failed++;
        }
        return "envStates=" + STATES.size() + " initialized=" + initialized + " efxReady=" + ready + " efxFailed=" + failed;
    }

    private static boolean createPrivateEfx(int sourceId, State state) {
''')

p = "src/main/java/dev/cchqphysics/compat/audio/SyncStartCoordinator.java"
replace(p,
'''    private static final class Group {
''',
'''    static synchronized String debugSummary() {
        int pendingSources = 0;
        for (Group group : GROUPS.values()) pendingSources += group.sources.size();
        return "syncGroups=" + GROUPS.size() + " pendingSources=" + pendingSources;
    }

    private static final class Group {
''')

# ---------------------------------------------------------------------------
# Cloth Config: Beta9 backoff controls + command discoverability.
# ---------------------------------------------------------------------------
p = "src/main/java/dev/cchqphysics/compat/config/ClothConfigScreen.java"
replace(p,
'''        category.addEntry(extendedBoolEntry(entries, "Beta9 whole-direct reuse", "BETA9_DIRECT_REUSE", true,
                "OFF forces the progressive direct result to be recomputed instead of reusing an exact matching result."));
        category.addEntry(extendedBoolEntry(entries, "Beta10 exact ray cache", "BETA10_RAY_CACHE", true,
''',
'''        category.addEntry(extendedBoolEntry(entries, "Beta9 whole-direct reuse", "BETA9_DIRECT_REUSE", true,
                "OFF forces the progressive direct result to be recomputed instead of reusing an exact matching result."));
        category.addEntry(extendedBoolEntry(entries, "Beta9 room backoff", "BETA9_ROOM_BACKOFF", true,
                "OFF keeps room updates at the base scheduler interval instead of backing off stable/distant sources."));
        category.addEntry(extendedBoolEntry(entries, "Beta9 adaptive load controller", "BETA9_ADAPTIVE_CONTROLLER", true,
                "OFF removes CPU/queue-pressure contribution while retaining stable/relevance backoff."));
        category.addEntry(extendedBoolEntry(entries, "Beta10 exact ray cache", "BETA10_RAY_CACHE", true,
''')
replace(p,
'''        category.addEntry(scheduler.build());

        SubCategoryBuilder sentinel = entries.startSubCategory(t("Clearing sentinel"))
''',
'''        SubCategoryBuilder beta9 = entries.startSubCategory(t("Beta9 room backoff"))
                .setExpanded(false)
                .setTooltip(tip("High-level bounds around Hotfix3's adaptive room scheduling. Defaults reproduce Hotfix3."));
        beta9.add(extendedIntervalEntry(entries, "Recent movement window", "BETA9_RECENT_MOVEMENT_MS", 400, 0, 5000,
                "Stability backoff is suppressed for this long after listener movement."));
        beta9.add(entries.startDoubleField(t("Listener movement reset"), extDouble("BETA9_LISTENER_MOVE_DISTANCE", 0.05D))
                .setDefaultValue(0.05D).setMin(0.0D).setMax(4.0D)
                .setTooltip(tip("Movement in blocks that resets stable-room counters. Hotfix3 = 0.05."))
                .setSaveConsumer(value -> ExtendedClientConfigAccess.set("BETA9_LISTENER_MOVE_DISTANCE", value)).build());
        beta9.add(entries.startDoubleField(t("Maximum room backoff"), extDouble("BETA9_MAX_ROOM_FACTOR", 2.0D))
                .setDefaultValue(2.0D).setMin(1.0D).setMax(6.0D)
                .setTooltip(tip("Maximum combined interval multiplier. Hotfix3 = 2.0×."))
                .setSaveConsumer(value -> ExtendedClientConfigAccess.set("BETA9_MAX_ROOM_FACTOR", value)).build());
        beta9.add(extendedIntervalEntry(entries, "Maximum room interval", "BETA9_MAX_ROOM_INTERVAL_MS", 1500, 50, 10000,
                "Absolute ceiling after all Beta9 backoff. Hotfix3 = 1500 ms."));
        category.addEntry(beta9.build());
        category.addEntry(scheduler.build());

        SubCategoryBuilder sentinel = entries.startSubCategory(t("Clearing sentinel"))
''')
replace(p,
'''        category.addEntry(entries.startTextDescription(
                        t("Targeted INFO-level diagnostics for your real-game Phase 5 test. All are OFF by default."))
                .setColor(DESCRIPTION)
                .build());
''',
'''        category.addEntry(entries.startTextDescription(
                        t("Targeted INFO-level diagnostics for your real-game Phase 5 test. All are OFF by default."))
                .setColor(DESCRIPTION)
                .build());
        category.addEntry(entries.startTextDescription(
                        t("Client commands: /cchqphysics status | dump | refresh_rooms | reset_caches | config"))
                .setColor(8374527)
                .build());
''')

print("Phase 5 batch 3 debug commands/adaptive room patch applied successfully")
