from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def replace(path: str, old: str, new: str, count: int = 1) -> None:
    p = ROOT / path
    text = p.read_text(encoding="utf-8")
    actual = text.count(old)
    if actual != count:
        raise SystemExit(f"{path}: expected {count} occurrence(s), found {actual}: {old[:100]!r}")
    p.write_text(text.replace(old, new, count), encoding="utf-8")

# Extended config: add sync timings and Beta9 direct-reuse diagnostic switch.
p = "src/main/java/dev/cchqphysics/compat/config/ExtendedClientConfig.java"
replace(p,
'''    private static final ModConfigSpec.BooleanValue PRIVATE_EFX;
    private static final ModConfigSpec.BooleanValue BETA10_RAY_CACHE;
''',
'''    private static final ModConfigSpec.IntValue SYNC_PARTIAL_FLUSH_MS;
    private static final ModConfigSpec.IntValue SYNC_STALE_GROUP_MS;

    private static final ModConfigSpec.BooleanValue PRIVATE_EFX;
    private static final ModConfigSpec.BooleanValue BETA9_DIRECT_REUSE;
    private static final ModConfigSpec.BooleanValue BETA10_RAY_CACHE;
''')
replace(p,
'''        builder.pop();

        builder.push("features");
        PRIVATE_EFX = builder
''',
'''        builder.pop();

        builder.push("sync");
        SYNC_PARTIAL_FLUSH_MS = builder
                .comment("Grace before an incomplete synchronized-start group is flushed. Hotfix3 = 100 ms.")
                .defineInRange("partial_flush_ms", 100, 0, 2000);
        SYNC_STALE_GROUP_MS = builder
                .comment("Age after which abandoned sync groups are discarded. Hotfix3 = 5000 ms.")
                .defineInRange("stale_group_ms", 5000, 250, 30000);
        builder.pop();

        builder.push("features");
        PRIVATE_EFX = builder
''')
replace(p,
'''        BETA10_RAY_CACHE = builder
                .comment("Enable exact direct/SPR occlusion-ray reuse when the room stamp is reusable. Hotfix3 = true.")
''',
'''        BETA9_DIRECT_REUSE = builder
                .comment("Enable exact whole-direct-result reuse when source/environment inputs are unchanged. Hotfix3 = true.")
                .define("beta9_direct_reuse", true);
        BETA10_RAY_CACHE = builder
                .comment("Enable exact direct/SPR occlusion-ray reuse when the room stamp is reusable. Hotfix3 = true.")
''')
replace(p,
'''    public static boolean privateEfxEnabled() { return b(PRIVATE_EFX, true); }
    public static boolean beta10RayCacheEnabled() { return b(BETA10_RAY_CACHE, true); }
''',
'''    public static long syncPartialFlushNs() { return i(SYNC_PARTIAL_FLUSH_MS, 100) * 1_000_000L; }
    public static long syncStaleGroupNs() { return i(SYNC_STALE_GROUP_MS, 5000) * 1_000_000L; }

    public static boolean privateEfxEnabled() { return b(PRIVATE_EFX, true); }
    public static boolean beta9DirectReuseEnabled() { return b(BETA9_DIRECT_REUSE, true); }
    public static boolean beta10RayCacheEnabled() { return b(BETA10_RAY_CACHE, true); }
''')
replace(p,
'''    public static boolean logConfig() { return b(LOG_CONFIG, false); }
}
''',
'''    public static boolean logConfig() { return b(LOG_CONFIG, false); }

    public static String summary() {
        return "roomSlotMs=" + roomSlotNs() / 1_000_000L
                + " minHardStaleMs=" + minHardStaleNs() / 1_000_000L
                + " maxHardStaleMs=" + maxHardStaleNs() / 1_000_000L
                + " recentSourceMs=" + recentSourceNs() / 1_000_000L
                + " syncPartialMs=" + syncPartialFlushNs() / 1_000_000L
                + " syncStaleMs=" + syncStaleGroupNs() / 1_000_000L
                + " privateEfx=" + privateEfxEnabled()
                + " beta9DirectReuse=" + beta9DirectReuseEnabled()
                + " beta10RayCache=" + beta10RayCacheEnabled()
                + " beta11RoomRayMemo=" + beta11RoomRayMemoEnabled()
                + " perfReportMs=" + performanceReportNs() / 1_000_000L;
    }
}
''')

# Beta9: allow diagnostic disabling of whole direct-result reuse while preserving default true.
p = "src/main/java/dev/cchqphysics/compat/audio/Beta9Optimizer.java"
replace(p,
'''            if (meta != null && meta.haveSource && entry != null
                    && entry.inputCutoffBits == Float.floatToIntBits(inputCutoff)
''',
'''            if (ExtendedClientConfig.beta9DirectReuseEnabled()
                    && meta != null && meta.haveSource && entry != null
                    && entry.inputCutoffBits == Float.floatToIntBits(inputCutoff)
''')

# Sync coordinator: expose Hotfix3 timers and add gated sync diagnostics.
p = "src/main/java/dev/cchqphysics/compat/audio/SyncStartCoordinator.java"
replace(p,
'''package dev.cchqphysics.compat.audio;

import org.lwjgl.openal.AL10;
''',
'''package dev.cchqphysics.compat.audio;

import dev.cchqphysics.compat.config.ExtendedClientConfig;
import org.lwjgl.openal.AL10;
''')
replace(p,
'''    private static final long PARTIAL_FLUSH_NS = 100_000_000L;
    private static final long STALE_GROUP_NS = 5_000_000_000L;
''',
'''    // Phase 5 exposes the two Hotfix3 sync timers through ExtendedClientConfig.
''')
replace(p,
'''        GROUPS.entrySet().removeIf(entry -> now - entry.getValue().createdNs > STALE_GROUP_NS);
''',
'''        GROUPS.entrySet().removeIf(entry -> now - entry.getValue().createdNs > ExtendedClientConfig.syncStaleGroupNs());
''')
replace(p,
'''        if (groupId == null || expected <= 1) {
            AL10.alSourcePlay(sourceId);
            return;
        }

        Group group = GROUPS.computeIfAbsent(groupId, ignored -> new Group(expected));
''',
'''        if (groupId == null || expected <= 1) {
            DebugDiagnostics.sync("source={} immediate start (no sync group)", sourceId);
            AL10.alSourcePlay(sourceId);
            return;
        }

        Group group = GROUPS.get(groupId);
        if (group == null) {
            group = new Group(expected);
            GROUPS.put(groupId, group);
            DebugDiagnostics.sync("group={} created expected={}", groupId, expected);
        }
''')
replace(p,
'''        if (!group.sources.contains(sourceId)) {
            group.sources.add(sourceId);
        }
        if (group.sources.size() >= group.expected) {
''',
'''        if (!group.sources.contains(sourceId)) {
            group.sources.add(sourceId);
            DebugDiagnostics.sync("group={} queued source={} count={}/{}", groupId, sourceId, group.sources.size(), group.expected);
        }
        if (group.sources.size() >= group.expected) {
''')
replace(p,
'''            if (!group.sources.isEmpty() && now - group.createdNs >= PARTIAL_FLUSH_NS) {
                playVector(group.sources);
                iterator.remove();
            }
''',
'''            if (!group.sources.isEmpty() && now - group.createdNs >= ExtendedClientConfig.syncPartialFlushNs()) {
                DebugDiagnostics.sync("group={} partial flush count={}/{} ageMs={}", entry.getKey(), group.sources.size(), group.expected,
                        (now - group.createdNs) / 1_000_000.0D);
                playVector(group.sources);
                iterator.remove();
            }
''')
replace(p,
'''    private static void startAndRemove(UUID groupId, Group group) {
        playVector(group.sources);
        GROUPS.remove(groupId);
    }
''',
'''    private static void startAndRemove(UUID groupId, Group group) {
        DebugDiagnostics.sync("group={} complete start count={}/{}", groupId, group.sources.size(), group.expected);
        playVector(group.sources);
        GROUPS.remove(groupId);
    }
''')
replace(p,
'''    static synchronized void clear() {
        GROUPS.clear();
    }
''',
'''    static synchronized void clear() {
        DebugDiagnostics.sync("clear pending sync groups count={}", GROUPS.size());
        GROUPS.clear();
    }
''')
replace(p,
'''            group.sources.remove((Integer) sourceId);
''',
'''            if (group.sources.remove((Integer) sourceId)) {
                DebugDiagnostics.sync("removed source={} from pending group remaining={}/{}", sourceId, group.sources.size(), group.expected);
            }
''')

# Startup: optionally print effective advanced values.
p = "src/main/java/dev/cchqphysics/compat/CCHQSoundPhysicsCompat.java"
replace(p,
'''        LOGGER.info("CC:HQ Sound Physics Compat {} initialized; Phase 4 parity defaults preserved; Phase 5 advanced/debug controls available", VERSION);
''',
'''        LOGGER.info("CC:HQ Sound Physics Compat {} initialized; Phase 4 parity defaults preserved; Phase 5 advanced/debug controls available", VERSION);
        if (ExtendedClientConfig.logConfig()) {
            LOGGER.info("Phase 5 advanced config: {}", ExtendedClientConfig.summary());
        }
''')

# Cloth Config: add visible Advanced Runtime and Debug & Validation categories.
p = "src/main/java/dev/cchqphysics/compat/config/ClothConfigScreen.java"
replace(p,
'''        smoothing(builder, entries);
        performance(builder, entries);
        return builder.build();
''',
'''        smoothing(builder, entries);
        performance(builder, entries);
        advancedRuntime(builder, entries);
        debugValidation(builder, entries);
        return builder.build();
''')

insert = r'''
    private static void advancedRuntime(ConfigBuilder builder, ConfigEntryBuilder entries) {
        ConfigCategory category = builder.getOrCreateCategory(t("Advanced Runtime"));
        category.addEntry(entries.startTextDescription(
                        t("Phase 5 test controls. Every default below is the verified Hotfix3/Phase 4 value."))
                .setColor(DESCRIPTION)
                .build());
        category.addEntry(entries.startTextDescription(
                        t("Change one setting at a time while diagnosing. The frozen parity branch is not modified by these options."))
                .setColor(8374527)
                .build());

        category.addEntry(extendedBoolEntry(entries, "Private per-source EFX", "PRIVATE_EFX", true,
                "OFF bypasses compat-owned isolated filters and deliberately falls back to native SPR environment writes."));
        category.addEntry(extendedBoolEntry(entries, "Beta9 whole-direct reuse", "BETA9_DIRECT_REUSE", true,
                "OFF forces the progressive direct result to be recomputed instead of reusing an exact matching result."));
        category.addEntry(extendedBoolEntry(entries, "Beta10 exact ray cache", "BETA10_RAY_CACHE", true,
                "OFF disables exact direct/SPR occlusion-ray reuse."));
        category.addEntry(extendedBoolEntry(entries, "Beta11 room-ray memo", "BETA11_ROOM_RAY_MEMO", true,
                "OFF disables same-clone environment/bounce ray memoization."));

        SubCategoryBuilder scheduler = entries.startSubCategory(t("Room scheduler"))
                .setExpanded(false)
                .setTooltip(tip("Fairness, staleness and movement thresholds used by the Hotfix3 room scheduler."));
        scheduler.add(extendedIntervalEntry(entries, "Scheduler slot", "ROOM_SLOT_MS", 50, 10, 500,
                "Global minimum room scheduling slot. Hotfix3 = 50 ms."));
        scheduler.add(extendedIntervalEntry(entries, "Minimum hard stale", "MIN_HARD_STALE_MS", 500, 100, 5000,
                "Minimum age that can force a room target refresh. Hotfix3 = 500 ms."));
        scheduler.add(extendedIntervalEntry(entries, "Maximum hard stale", "MAX_HARD_STALE_MS", 2000, 250, 10000,
                "Maximum room-target staleness allowed by fairness scaling. Hotfix3 = 2000 ms."));
        scheduler.add(extendedIntervalEntry(entries, "Recent-source window", "RECENT_SOURCE_MS", 1000, 100, 10000,
                "How recently a source must have been observed to remain scheduler eligible."));
        scheduler.add(entries.startDoubleField(t("Teleport distance"), extDouble("TELEPORT_DISTANCE", 4.0D))
                .setDefaultValue(4.0D).setMin(0.5D).setMax(64.0D)
                .setTooltip(tip("Listener movement treated as a teleport, in blocks. Hotfix3 = 4.0."))
                .setSaveConsumer(value -> ExtendedClientConfigAccess.set("TELEPORT_DISTANCE", value)).build());
        scheduler.add(entries.startDoubleField(t("Urgent source movement"), extDouble("SOURCE_MOVE_URGENT_DISTANCE", 0.10D))
                .setDefaultValue(0.10D).setMin(0.0D).setMax(8.0D)
                .setTooltip(tip("Speaker movement that invalidates its room stamp and marks it urgent, in blocks. Hotfix3 = 0.1."))
                .setSaveConsumer(value -> ExtendedClientConfigAccess.set("SOURCE_MOVE_URGENT_DISTANCE", value)).build());
        category.addEntry(scheduler.build());

        SubCategoryBuilder sentinel = entries.startSubCategory(t("Clearing sentinel"))
                .setExpanded(false)
                .setTooltip(tip("Thresholds that detect a blocked-to-open transition before the normal room refresh catches up."));
        sentinel.add(entries.startDoubleField(t("Movement trigger"), extDouble("SENTINEL_MOVE_DISTANCE", 0.05D))
                .setDefaultValue(0.05D).setMin(0.0D).setMax(4.0D)
                .setSaveConsumer(value -> ExtendedClientConfigAccess.set("SENTINEL_MOVE_DISTANCE", value)).build());
        sentinel.add(entries.startDoubleField(t("Raw occluded threshold"), extDouble("SENTINEL_RAW_OCCLUDED", 0.075D))
                .setDefaultValue(0.075D).setMin(0.0D).setMax(4.0D)
                .setSaveConsumer(value -> ExtendedClientConfigAccess.set("SENTINEL_RAW_OCCLUDED", value)).build());
        sentinel.add(entries.startDoubleField(t("Re-arm center threshold"), extDouble("SENTINEL_REARM_CENTER", 0.12D))
                .setDefaultValue(0.12D).setMin(0.0D).setMax(4.0D)
                .setSaveConsumer(value -> ExtendedClientConfigAccess.set("SENTINEL_REARM_CENTER", value)).build());
        sentinel.add(entries.startDoubleField(t("Open center threshold"), extDouble("SENTINEL_OPEN_CENTER", 0.035D))
                .setDefaultValue(0.035D).setMin(0.0D).setMax(4.0D)
                .setSaveConsumer(value -> ExtendedClientConfigAccess.set("SENTINEL_OPEN_CENTER", value)).build());
        sentinel.add(entries.startDoubleField(t("Center drop trigger"), extDouble("SENTINEL_CENTER_DROP", 0.15D))
                .setDefaultValue(0.15D).setMin(0.0D).setMax(4.0D)
                .setSaveConsumer(value -> ExtendedClientConfigAccess.set("SENTINEL_CENTER_DROP", value)).build());
        sentinel.add(entries.startDoubleField(t("Confirm raw drop"), extDouble("CONFIRM_RAW_DROP", 0.035D))
                .setDefaultValue(0.035D).setMin(0.0D).setMax(4.0D)
                .setSaveConsumer(value -> ExtendedClientConfigAccess.set("CONFIRM_RAW_DROP", value)).build());
        sentinel.add(entries.startDoubleField(t("Confirm cutoff rise"), extDouble("CONFIRM_CUTOFF_RISE", 0.055D))
                .setDefaultValue(0.055D).setMin(0.0D).setMax(1.0D)
                .setSaveConsumer(value -> ExtendedClientConfigAccess.set("CONFIRM_CUTOFF_RISE", value)).build());
        sentinel.add(extendedIntervalEntry(entries, "Clear-trigger cooldown", "CLEAR_TRIGGER_COOLDOWN_MS", 300, 0, 5000,
                "Cooldown between confirmed clearing triggers."));
        category.addEntry(sentinel.build());

        SubCategoryBuilder sync = entries.startSubCategory(t("Synchronized starts"))
                .setExpanded(false)
                .setTooltip(tip("Hotfix3 group-start grace and abandoned-group cleanup timings."));
        sync.add(extendedIntervalEntry(entries, "Partial group flush", "SYNC_PARTIAL_FLUSH_MS", 100, 0, 2000,
                "Grace before an incomplete sync group is started anyway. Hotfix3 = 100 ms."));
        sync.add(extendedIntervalEntry(entries, "Stale group cleanup", "SYNC_STALE_GROUP_MS", 5000, 250, 30000,
                "Age after which an abandoned pending group is discarded. Hotfix3 = 5000 ms."));
        category.addEntry(sync.build());
    }

    private static void debugValidation(ConfigBuilder builder, ConfigEntryBuilder entries) {
        ConfigCategory category = builder.getOrCreateCategory(t("Debug & Validation"));
        category.addEntry(entries.startTextDescription(
                        t("Targeted INFO-level diagnostics for your real-game Phase 5 test. All are OFF by default."))
                .setColor(DESCRIPTION)
                .build());

        category.addEntry(extendedIntervalEntry(entries, "Performance report interval", "PERFORMANCE_REPORT_MS", 10000, 1000, 60000,
                "Controls the cadence of compat performance reports when Performance diagnostics is enabled."));
        category.addEntry(extendedBoolEntry(entries, "Source lifecycle log", "LOG_SOURCE_LIFECYCLE", false,
                "Register/unregister, identity and global-clear events."));
        category.addEntry(extendedBoolEntry(entries, "Room scheduler log", "LOG_ROOM_SCHEDULER", false,
                "Urgent selection, room reuse and room refresh decisions. Can be noisy."));
        category.addEntry(extendedBoolEntry(entries, "Clearing sentinel log", "LOG_SENTINEL", false,
                "Candidate and confirmation values for blocked-to-open detection."));
        category.addEntry(extendedBoolEntry(entries, "EFX lifecycle log", "LOG_EFX", false,
                "Private-filter create/destroy/failure and native-fallback decisions."));
        category.addEntry(extendedBoolEntry(entries, "Cache scope log", "LOG_CACHE", false,
                "Beta10/Beta11 cache-scope resets without per-ray spam."));
        category.addEntry(extendedBoolEntry(entries, "Sync grouping log", "LOG_SYNC", false,
                "Group creation, queueing, complete starts and partial flushes."));
        category.addEntry(extendedBoolEntry(entries, "Transition timing log", "LOG_TRANSITIONS", false,
                "Confirmed acoustic transition latency to room application."));
        category.addEntry(extendedBoolEntry(entries, "Startup config summary", "LOG_CONFIG", false,
                "Print the effective advanced configuration at next startup."));
    }

    private static AbstractConfigListEntry extendedBoolEntry(
            ConfigEntryBuilder entries, String label, String key, boolean defaultValue, String detail) {
        return entries.startBooleanToggle(t(label), extBool(key, defaultValue))
                .setDefaultValue(defaultValue)
                .setTooltip(tip(detail))
                .setSaveConsumer(value -> ExtendedClientConfigAccess.set(key, value))
                .build();
    }

    private static AbstractConfigListEntry extendedIntervalEntry(
            ConfigEntryBuilder entries, String label, String key, int defaultValue, int min, int max, String detail) {
        return entries.startIntSlider(t(label), extInt(key, defaultValue), min, max)
                .setDefaultValue(defaultValue)
                .setTextGetter(value -> t(value + " ms  •  " + hz(value) + " Hz"))
                .setTooltip(tip(detail))
                .setSaveConsumer(value -> ExtendedClientConfigAccess.set(key, value))
                .build();
    }

    private static boolean extBool(String name, boolean fallback) {
        return ExtendedClientConfigAccess.bool(name, fallback);
    }

    private static double extDouble(String name, double fallback) {
        return ExtendedClientConfigAccess.dbl(name, fallback);
    }

    private static int extInt(String name, int fallback) {
        return ExtendedClientConfigAccess.integer(name, fallback);
    }

'''
replace(p,
'''    private static AbstractConfigListEntry percentEntry(
''',
insert + '''    private static AbstractConfigListEntry percentEntry(
''')

print("Phase 5 batch 2 sync/config-screen patch applied successfully")
