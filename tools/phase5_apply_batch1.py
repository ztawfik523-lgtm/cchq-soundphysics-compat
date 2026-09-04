from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def replace(path: str, old: str, new: str, count: int = 1) -> None:
    p = ROOT / path
    text = p.read_text(encoding="utf-8")
    actual = text.count(old)
    if actual != count:
        raise SystemExit(f"{path}: expected {count} occurrence(s), found {actual}: {old[:80]!r}")
    p.write_text(text.replace(old, new, count), encoding="utf-8")


def replace_all(path: str, old: str, new: str) -> None:
    p = ROOT / path
    text = p.read_text(encoding="utf-8")
    if old not in text:
        raise SystemExit(f"{path}: token not found: {old}")
    p.write_text(text.replace(old, new), encoding="utf-8")

# ---------------------------------------------------------------------------
# SoundPhysicsBridge: expose scheduler/sentinel constants with parity defaults.
# ---------------------------------------------------------------------------
p = "src/main/java/dev/cchqphysics/compat/audio/SoundPhysicsBridge.java"
replace(p,
        "import dev.cchqphysics.compat.config.ClientConfig;\n",
        "import dev.cchqphysics.compat.config.ClientConfig;\nimport dev.cchqphysics.compat.config.ExtendedClientConfig;\n")
replace(p,
'''    private static final long ROOM_SLOT_NS = 50_000_000L;
    private static final long MIN_HARD_STALE_NS = 500_000_000L;
    private static final long MAX_HARD_STALE_NS = 2_000_000_000L;
    private static final long RECENT_SOURCE_NS = 1_000_000_000L;
    private static final double TELEPORT_SQ = 16.0D;
    private static final double SOURCE_MOVE_URGENT_SQ = 0.01D;
    private static final double SENTINEL_MOVE_SQ = 0.0025D;
    private static final double SENTINEL_RAW_OCCLUDED = 0.075D;
    private static final double SENTINEL_REARM_CENTER = 0.12D;
    private static final double SENTINEL_OPEN_CENTER = 0.035D;
    private static final double SENTINEL_CENTER_DROP = 0.15D;
    private static final double CONFIRM_RAW_DROP = 0.035D;
    private static final float CONFIRM_CUTOFF_RISE = 0.055F;
    private static final long CLEAR_TRIGGER_COOLDOWN_NS = 300_000_000L;
''',
'''    // Phase 5 exposes the former Hotfix3 scheduler/sentinel constants through
    // ExtendedClientConfig. Its defaults are byte-for-byte behavioral parity values.
''')
replacements = {
    "SOURCE_MOVE_URGENT_SQ": "ExtendedClientConfig.sourceMoveUrgentSq()",
    "ROOM_SLOT_NS": "ExtendedClientConfig.roomSlotNs()",
    "MIN_HARD_STALE_NS": "ExtendedClientConfig.minHardStaleNs()",
    "MAX_HARD_STALE_NS": "ExtendedClientConfig.maxHardStaleNs()",
    "RECENT_SOURCE_NS": "ExtendedClientConfig.recentSourceNs()",
    "TELEPORT_SQ": "ExtendedClientConfig.teleportDistanceSq()",
    "SENTINEL_MOVE_SQ": "ExtendedClientConfig.sentinelMoveSq()",
    "SENTINEL_RAW_OCCLUDED": "ExtendedClientConfig.sentinelRawOccluded()",
    "SENTINEL_REARM_CENTER": "ExtendedClientConfig.sentinelRearmCenter()",
    "SENTINEL_OPEN_CENTER": "ExtendedClientConfig.sentinelOpenCenter()",
    "SENTINEL_CENTER_DROP": "ExtendedClientConfig.sentinelCenterDrop()",
    "CONFIRM_RAW_DROP": "ExtendedClientConfig.confirmRawDrop()",
    "CONFIRM_CUTOFF_RISE": "ExtendedClientConfig.confirmCutoffRise()",
    "CLEAR_TRIGGER_COOLDOWN_NS": "ExtendedClientConfig.clearTriggerCooldownNs()",
}
for old, new in replacements.items():
    replace_all(p, old, new)

replace(p,
'''        STATES.put(sourceId, new SourceState(sourceId, generation));
        if (rrCursor >= STATES.size()) rrCursor = 0;
''',
'''        STATES.put(sourceId, new SourceState(sourceId, generation));
        if (rrCursor >= STATES.size()) rrCursor = 0;
        DebugDiagnostics.source("register source={} generation={} tracked={}", sourceId, generation, STATES.size());
''')
replace(p,
'''        AcousticCapture.unregister(sourceId);
        if (rrCursor >= Math.max(1, STATES.size())) rrCursor = 0;
''',
'''        AcousticCapture.unregister(sourceId);
        if (rrCursor >= Math.max(1, STATES.size())) rrCursor = 0;
        DebugDiagnostics.source("unregister source={} tracked={}", sourceId, STATES.size());
''')
replace(p,
'''                state.urgent = true;
                state.roomStamp = null;
''',
'''                state.urgent = true;
                state.roomStamp = null;
                DebugDiagnostics.room("source={} movement marked room urgent", sourceId);
''', count=1)
replace(p,
'''            PerformanceStats.recordSentinelCandidate(confirmed);
''',
'''            PerformanceStats.recordSentinelCandidate(confirmed);
            DebugDiagnostics.sentinel("source={} oldRaw={} newRaw={} previousCenter={} center={} priorCutoff={} newCutoff={} confirmed={}",
                    state.sourceId, oldRaw, newRaw, previousCenter, center, priorCutoff, direct[0], confirmed);
''')
replace(p,
'''        if (reuse) {
            PerformanceStats.recordRoomReuse();
            return;
        }
''',
'''        if (reuse) {
            PerformanceStats.recordRoomReuse();
            DebugDiagnostics.room("source={} reused room stamp", state.sourceId);
            return;
        }
''')
replace(p,
'''        PerformanceStats.recordRoomRefresh();
        if (immediate) {
''',
'''        PerformanceStats.recordRoomRefresh();
        DebugDiagnostics.room("source={} refreshed room stableStamp={} immediate={} urgentCleared=true", state.sourceId, stableStamp, immediate);
        if (immediate) {
''')
replace(p,
'''                PerformanceStats.recordTransitionLatency(System.nanoTime() - transitionDetected);
''',
'''                long latency = System.nanoTime() - transitionDetected;
                PerformanceStats.recordTransitionLatency(latency);
                DebugDiagnostics.transition("source={} clearing transition room-applied latencyMs={}", state.sourceId, latency / 1_000_000.0D);
''')
replace(p,
'''        RoomEnvironmentAccess.reset();
    }
''',
'''        RoomEnvironmentAccess.reset();
        DebugDiagnostics.source("cleared all compat source ids/state");
    }
''', count=1)
replace(p,
'''                    state.urgent = true;
                    state.roomStamp = null;
''',
'''                    state.urgent = true;
                    state.roomStamp = null;
                    DebugDiagnostics.room("source={} listener teleport forced room urgent", state.sourceId);
''', count=1)

# ---------------------------------------------------------------------------
# EnvironmentSmoother: private-EFX diagnostic switch + targeted logs.
# ---------------------------------------------------------------------------
p = "src/main/java/dev/cchqphysics/compat/audio/EnvironmentSmoother.java"
replace(p,
        "import dev.cchqphysics.compat.config.ClientConfig;\n",
        "import dev.cchqphysics.compat.config.ClientConfig;\nimport dev.cchqphysics.compat.config.ExtendedClientConfig;\n")
replace(p,
'''        synchronized (EnvironmentSmoother.class) {
            state = STATES.get(sourceId);
            if (state == null) return false;
        }
        float[] adjusted = ProgressiveOcclusionModel.adjust(sourceId, directCutoff, directGain);
''',
'''        synchronized (EnvironmentSmoother.class) {
            state = STATES.get(sourceId);
            if (state == null) return false;
        }
        if (!ExtendedClientConfig.privateEfxEnabled()) {
            DebugDiagnostics.efx("source={} private EFX disabled; using native SPR fallback", sourceId);
            return false;
        }
        float[] adjusted = ProgressiveOcclusionModel.adjust(sourceId, directCutoff, directGain);
''')
replace(p,
'''            state.privateEfxReady = true;
            LOGGER.debug("beta1 isolated EFX source={} directFilter={} sends={}/{}/{}/{} maxAux={}",
''',
'''            state.privateEfxReady = true;
            DebugDiagnostics.efx("source={} created private EFX directFilter={} maxAux={}", sourceId, state.directFilter, state.maxAuxSends);
            LOGGER.debug("beta1 isolated EFX source={} directFilter={} sends={}/{}/{}/{} maxAux={}",
''')
replace(p,
'''            state.privateEfxFailed = true;
        }
''',
'''            state.privateEfxFailed = true;
            DebugDiagnostics.efx("source={} private EFX failed; native fallback reason={}", sourceId, throwable.toString());
        }
''')
replace(p,
'''        state.privateEfxReady = false;
        drainAlErrors();
''',
'''        state.privateEfxReady = false;
        DebugDiagnostics.efx("source={} destroyed private EFX", sourceId);
        drainAlErrors();
''')

# ---------------------------------------------------------------------------
# Beta10 exact ray cache: add explicit Phase-5 diagnostic switch.
# ---------------------------------------------------------------------------
p = "src/main/java/dev/cchqphysics/compat/audio/Beta10Optimizer.java"
replace(p,
        "import dev.cchqphysics.compat.config.ClientConfig;\n",
        "import dev.cchqphysics.compat.config.ClientConfig;\nimport dev.cchqphysics.compat.config.ExtendedClientConfig;\n")
replace(p,
'''        boolean cacheable = info != null && info.reusable && debugAllowsRayCache();
''',
'''        boolean cacheable = info != null && info.reusable
                && ExtendedClientConfig.beta10RayCacheEnabled()
                && debugAllowsRayCache();
''')
replace(p,
'''        if (elapsed < REPORT_NS) return;
''',
'''        if (elapsed < ExtendedClientConfig.performanceReportNs()) return;
''')
replace(p,
'''            scopeConfig = info.configFingerprint;
        }
''',
'''            scopeConfig = info.configFingerprint;
            DebugDiagnostics.cache("beta10 ray scope reset cloneTick={} config={}", info.cloneTick, info.configFingerprint);
        }
''', count=1)

# ---------------------------------------------------------------------------
# Beta11 room-ray memo: independent toggle + diagnostic interval.
# ---------------------------------------------------------------------------
p = "src/main/java/dev/cchqphysics/compat/audio/Beta11RoomRayCache.java"
replace(p,
        "import dev.cchqphysics.compat.config.ClientConfig;\n",
        "import dev.cchqphysics.compat.config.ClientConfig;\nimport dev.cchqphysics.compat.config.ExtendedClientConfig;\n")
replace(p,
'''        if (!Beta10Optimizer.beta11RoomCacheActive() || level == null) {
''',
'''        if (!ExtendedClientConfig.beta11RoomRayMemoEnabled()
                || !Beta10Optimizer.beta11RoomCacheActive() || level == null) {
''')
replace(p,
'''        scopeGetter = getter;
        scopeResets++;
''',
'''        scopeGetter = getter;
        scopeResets++;
        DebugDiagnostics.cache("beta11 room-ray scope rotated entriesPrevious={}", previous.entries);
''')
replace(p,
'''        if (elapsed < REPORT_NS) return;
''',
'''        if (elapsed < ExtendedClientConfig.performanceReportNs()) return;
''')

# ---------------------------------------------------------------------------
# Existing performance telemetry: configurable report period only.
# ---------------------------------------------------------------------------
p = "src/main/java/dev/cchqphysics/compat/audio/PerformanceStats.java"
replace(p,
        "import dev.cchqphysics.compat.config.ClientConfig;\n",
        "import dev.cchqphysics.compat.config.ClientConfig;\nimport dev.cchqphysics.compat.config.ExtendedClientConfig;\n")
replace(p,
'''        if (elapsed < WINDOW_NS) return;
''',
'''        if (elapsed < ExtendedClientConfig.performanceReportNs()) return;
''')

# Beta9 report cadence only; adaptive controller timing remains Hotfix3.
p = "src/main/java/dev/cchqphysics/compat/audio/Beta9Optimizer.java"
replace(p,
        "package dev.cchqphysics.compat.audio;\n\nimport net.minecraft.world.phys.Vec3;\n",
        "package dev.cchqphysics.compat.audio;\n\nimport dev.cchqphysics.compat.config.ExtendedClientConfig;\nimport net.minecraft.world.phys.Vec3;\n")
replace(p,
'''        if (elapsed < REPORT_NS) return;
''',
'''        if (elapsed < ExtendedClientConfig.performanceReportNs()) return;
''')

print("Phase 5 batch 1 source wiring applied successfully")
