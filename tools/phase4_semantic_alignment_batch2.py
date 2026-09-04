from pathlib import Path


def replace_exact(path: str, old: str, new: str) -> None:
    p = Path(path)
    text = p.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{path}: expected exactly one match, found {count}")
    p.write_text(text.replace(old, new, 1), encoding="utf-8")


replace_exact(
    "src/main/java/dev/cchqphysics/compat/audio/CompatAudioManager.java",
    '''                double distanceSquared = dx * dx + dy * dy + dz * dz;\n                double maxDistanceSquared = AttenuationBridge.maxDistanceSquared(active.audio);\n                Beta9Optimizer.updateDistance(active.sourceId, distanceSquared, maxDistanceSquared);\n                if (distanceSquared > maxDistanceSquared) {\n                    gain = 0.0F;\n                }''',
    '''                boolean outside = dx * dx + dy * dy + dz * dz\n                        > AttenuationBridge.maxDistanceSquared(active.audio);\n                Beta9Optimizer.updateDistance(active.sourceId,\n                        dx * dx + dy * dy + dz * dz,\n                        AttenuationBridge.maxDistanceSquared(active.audio));\n                if (outside) {\n                    gain = 0.0F;\n                }''',
)
replace_exact(
    "src/main/java/dev/cchqphysics/compat/audio/CompatAudioManager.java",
    "                SoundPhysicsBridge.apply(active.sourceId, entry.getKey(), active.audio.x(), active.audio.y(), active.audio.z());",
    "                SoundPhysicsBridge.apply(active.sourceId, active.audio.source(), active.audio.x(), active.audio.y(), active.audio.z());",
)

replace_exact(
    "src/main/java/dev/cchqphysics/compat/audio/Beta10Optimizer.java",
    '''    private static void maybeReportLocked(long now) {\n        long elapsed = now - reportStartNs;\n        if (elapsed < REPORT_NS) return;\n        double seconds = elapsed / 1.0E9D;\n        long total = rayHits + rayMisses;\n        double hitRate = total == 0L ? 0.0D : 100.0D * rayHits / total;\n        String report = String.format(Locale.ROOT,\n                "[CC:HQ Sound Physics Compat] beta10 window=%.1fs rayHit=%d rayMiss=%d hitRate=%.1f%% actualRay=%.2fms/s directHit=%d directMiss=%d sprHit=%d sprMiss=%d directToSpr=%d active=%d eligible=%d maxActive=%d filterWrites=%d filterSkips=%d sourceWrites=%d sourceSkips=%d idleResets=%d",\n                seconds, rayHits, rayMisses, hitRate, (rayActualNs / 1_000_000.0D) / seconds,\n                directRayHits, directRayMisses, sprRayHits, sprRayMisses, directToSprHits,\n                activeSources.size(), activeSources.size() - inaudibleSources.size(), maxActiveSources,\n                filterWrites, filterSkips, sourceWrites, sourceSkips, controllerIdleResets);\n        SoundPhysicsBridge.beta9Log(report);\n        reportStartNs = now;\n        rayHits = rayMisses = rayActualNs = 0L;\n        directRayHits = directRayMisses = sprRayHits = sprRayMisses = directToSprHits = 0L;\n        filterWrites = filterSkips = sourceWrites = sourceSkips = controllerIdleResets = 0L;\n        maxActiveSources = activeSources.size();\n    }''',
    '''    private static void maybeReportLocked(long now) {\n        long elapsed = now - reportStartNs;\n        if (elapsed < REPORT_NS) return;\n        if (!ClientConfig.diagnosticsEnabled()) {\n            reportStartNs = now;\n            rayHits = rayMisses = rayActualNs = 0L;\n            directRayHits = directRayMisses = sprRayHits = sprRayMisses = directToSprHits = 0L;\n            filterWrites = filterSkips = sourceWrites = sourceSkips = controllerIdleResets = 0L;\n            maxActiveSources = activeSources.size();\n            return;\n        }\n        double seconds = Math.max(0.001D, elapsed / 1.0E9D);\n        long total = rayHits + rayMisses;\n        double hitRate = total == 0L ? 0.0D : rayHits * 100.0D / total;\n        String report = String.format(Locale.ROOT,\n                "[CC:HQ Sound Physics Compat] beta11 direct-ray window=%.1fs active=%d eligible=%d maxActive=%d rayHit=%d (%.1f/s) rayMiss=%d (%.1f/s) hitRate=%.1f%% actualRay=%.1fms/s direct=%d/%d spr=%d/%d directToSpr=%d filterWrite=%d filterSkip=%d sourceWrite=%d sourceSkip=%d idleResets=%d",\n                seconds, activeSources.size(), Math.max(0, activeSources.size() - inaudibleSources.size()), maxActiveSources,\n                rayHits, rayHits / seconds, rayMisses, rayMisses / seconds, hitRate,\n                (rayActualNs / 1_000_000.0D) / seconds, directRayHits, directRayMisses, sprRayHits, sprRayMisses,\n                directToSprHits, filterWrites, filterSkips, sourceWrites, sourceSkips, controllerIdleResets);\n        SoundPhysicsBridge.beta9Log(report);\n        reportStartNs = now;\n        rayHits = rayMisses = rayActualNs = 0L;\n        directRayHits = directRayMisses = sprRayHits = sprRayMisses = directToSprHits = 0L;\n        filterWrites = filterSkips = sourceWrites = sourceSkips = controllerIdleResets = 0L;\n        maxActiveSources = activeSources.size();\n    }''',
)

replace_exact(
    "src/main/java/dev/cchqphysics/compat/audio/ProgressiveOcclusionModel.java",
    '''        synchronized (ProgressiveOcclusionModel.class) {\n            State state = STATES.get(sourceId);\n            if (state == null || !state.hasSource) {\n                Beta9Optimizer.endSentinelTimer();\n                return Double.NaN;\n            }\n            x = state.sourceX;\n            y = state.sourceY;\n            z = state.sourceZ;\n        }\n        PerformanceStats.recordSentinelPath();''',
    '''        boolean haveSource;\n        synchronized (ProgressiveOcclusionModel.class) {\n            State state = STATES.get(sourceId);\n            haveSource = state != null && state.hasSource;\n            if (haveSource) {\n                x = state.sourceX;\n                y = state.sourceY;\n                z = state.sourceZ;\n            } else {\n                x = y = z = 0.0D;\n            }\n        }\n        if (!haveSource) {\n            Beta9Optimizer.endSentinelTimer();\n            return Double.NaN;\n        }\n        PerformanceStats.recordSentinelPath();''',
)

replace_exact(
    "src/main/java/dev/cchqphysics/compat/audio/EnvironmentSmoother.java",
    '''                float reverbAlpha = ClientConfig.reverbAlpha();\n                state.r0 = approach(state.r0, r0, reverbAlpha);\n                state.r1 = approach(state.r1, r1, reverbAlpha);\n                state.r2 = approach(state.r2, r2, reverbAlpha);\n                state.r3 = approach(state.r3, r3, reverbAlpha);\n                state.h0 = approach(state.h0, h0, reverbAlpha);\n                state.h1 = approach(state.h1, h1, reverbAlpha);\n                state.h2 = approach(state.h2, h2, reverbAlpha);\n                state.h3 = approach(state.h3, h3, reverbAlpha);''',
    '''                state.r0 = approach(state.r0, r0, ClientConfig.reverbAlpha());\n                state.r1 = approach(state.r1, r1, ClientConfig.reverbAlpha());\n                state.r2 = approach(state.r2, r2, ClientConfig.reverbAlpha());\n                state.r3 = approach(state.r3, r3, ClientConfig.reverbAlpha());\n                state.h0 = approach(state.h0, h0, ClientConfig.reverbAlpha());\n                state.h1 = approach(state.h1, h1, ClientConfig.reverbAlpha());\n                state.h2 = approach(state.h2, h2, ClientConfig.reverbAlpha());\n                state.h3 = approach(state.h3, h3, ClientConfig.reverbAlpha());''',
)

print("Phase 4 semantic alignment batch 2 applied")
