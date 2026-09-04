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
# EFX runtime toggle: detach private filters immediately when switched off,
# and provide a sound-thread reset/retry operation.
# ---------------------------------------------------------------------------
p = "src/main/java/dev/cchqphysics/compat/audio/EnvironmentSmoother.java"
replace(p,
'''        if (!ExtendedClientConfig.privateEfxEnabled()) {
            DebugDiagnostics.efx("source={} private EFX disabled; using native SPR fallback", sourceId);
            return false;
        }
''',
'''        if (!ExtendedClientConfig.privateEfxEnabled()) {
            if (state.privateEfxReady || state.directFilter != 0
                    || state.sendFilters[0] != 0 || state.sendFilters[1] != 0
                    || state.sendFilters[2] != 0 || state.sendFilters[3] != 0) {
                destroyPrivateEfx(sourceId, state);
            }
            // Turning the feature off is also an explicit retry boundary: if it
            // is enabled again later, allow a fresh isolated-EFX setup attempt.
            state.privateEfxFailed = false;
            state.failureLogged = false;
            DebugDiagnostics.efx("source={} private EFX disabled; detached compat filters and using native SPR fallback", sourceId);
            return false;
        }
''')
replace(p,
'''    static synchronized String debugSummary() {
''',
'''    static synchronized void debugResetEfx() {
        for (Map.Entry<Integer, State> entry : STATES.entrySet()) {
            int sourceId = entry.getKey();
            State state = entry.getValue();
            destroyPrivateEfx(sourceId, state);
            state.privateEfxFailed = false;
            state.failureLogged = false;
        }
        DebugDiagnostics.efx("manual private EFX reset completed for {} tracked sources", STATES.size());
    }

    static synchronized void debugDumpEfx() {
        for (Map.Entry<Integer, State> entry : STATES.entrySet()) {
            State state = entry.getValue();
            SoundPhysicsBridge.beta9Log("[phase5/source-efx] source=" + entry.getKey()
                    + " initialized=" + state.initialized
                    + " ready=" + state.privateEfxReady
                    + " failed=" + state.privateEfxFailed
                    + " directFilter=" + state.directFilter
                    + " maxAux=" + state.maxAuxSends
                    + " cutoff=" + round3(state.cutoff)
                    + " gain=" + round3(state.gain));
        }
    }

    static synchronized String debugSummary() {
''')

# ---------------------------------------------------------------------------
# Cross-thread control queue: add EFX reset flag.
# ---------------------------------------------------------------------------
p = "src/main/java/dev/cchqphysics/compat/audio/DebugControl.java"
replace(p,
'''    private static volatile boolean roomRefreshRequested;
    private static volatile boolean cacheResetRequested;
''',
'''    private static volatile boolean roomRefreshRequested;
    private static volatile boolean cacheResetRequested;
    private static volatile boolean efxResetRequested;
''')
replace(p,
'''    static void requestCacheReset() {
        cacheResetRequested = true;
    }

    static void consumeSoundThreadRequests() {
''',
'''    static void requestCacheReset() {
        cacheResetRequested = true;
    }

    static void requestEfxReset() {
        efxResetRequested = true;
    }

    static void consumeSoundThreadRequests() {
        if (efxResetRequested) {
            efxResetRequested = false;
            EnvironmentSmoother.debugResetEfx();
        }
''')

# ---------------------------------------------------------------------------
# Detailed source dump, intentionally read-only.
# ---------------------------------------------------------------------------
p = "src/main/java/dev/cchqphysics/compat/audio/SoundPhysicsBridge.java"
replace(p,
'''    static synchronized String debugSummary() {
''',
'''    static synchronized void debugDumpSources() {
        long now = System.nanoTime();
        for (SourceState state : STATES.values()) {
            double raw = ProgressiveOcclusionModel.currentRawOcclusion(state.sourceId);
            double center = ProgressiveOcclusionModel.currentCenterOcclusion(state.sourceId);
            long roomAgeMs = state.lastRoomNs == 0L ? -1L : Math.max(0L, now - state.lastRoomNs) / 1_000_000L;
            long seenAgeMs = state.lastSeenNs == 0L ? -1L : Math.max(0L, now - state.lastSeenNs) / 1_000_000L;
            beta9Log("[phase5/source] source=" + state.sourceId
                    + " generation=" + state.generation
                    + " uuid=" + state.uuid
                    + " playing=" + state.playing
                    + " inRange=" + state.inRange
                    + " urgent=" + state.urgent
                    + " room=" + (state.room != null)
                    + " roomAgeMs=" + roomAgeMs
                    + " seenAgeMs=" + seenAgeMs
                    + " raw=" + raw
                    + " center=" + center
                    + " directCutoff=" + state.directCutoff
                    + " directGain=" + state.directGain
                    + " pos=" + state.x + "," + state.y + "," + state.z);
        }
    }

    static synchronized String debugSummary() {
''')

# ---------------------------------------------------------------------------
# Commands: reset EFX and include per-source detail in dump.
# ---------------------------------------------------------------------------
p = "src/main/java/dev/cchqphysics/compat/audio/DebugCommands.java"
replace(p,
'''                            LOGGER.info("[phase5/dump] {}", DebugControl.compactStatus());
                            context.getSource().sendSuccess(
''',
'''                            LOGGER.info("[phase5/dump] {}", DebugControl.compactStatus());
                            SoundPhysicsBridge.debugDumpSources();
                            EnvironmentSmoother.debugDumpEfx();
                            context.getSource().sendSuccess(
''')
replace(p,
'''                .then(Commands.literal("config")
''',
'''                .then(Commands.literal("reset_efx")
                        .executes(context -> {
                            DebugControl.requestEfxReset();
                            context.getSource().sendSuccess(
                                    () -> Component.literal("CC:HQ Physics private EFX reset queued on the sound thread"), false);
                            return 1;
                        }))
                .then(Commands.literal("config")
''')

# Config screen discoverability.
p = "src/main/java/dev/cchqphysics/compat/config/ClothConfigScreen.java"
replace(p,
'''                        t("Client commands: /cchqphysics status | dump | refresh_rooms | reset_caches | config"))
''',
'''                        t("Client commands: /cchqphysics status | dump | refresh_rooms | reset_caches | reset_efx | config"))
''')

print("Phase 5 batch 4 EFX hardening/detail dump patch applied successfully")
