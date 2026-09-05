from pathlib import Path


def patch(path, replacements):
    p = Path(path)
    text = p.read_text()
    for old, new in replacements:
        assert old in text, f"{path}: missing expected text: {old[:120]!r}"
        text = text.replace(old, new, 1)
    p.write_text(text)


patch('build.gradle', [
    ('''    // The tested Hotfix3 directly invokes SPR members which are private in the
    // published SPR JAR and are widened by this mod's access transformer. Keep
    // the untouched SPR artifact for runtime, but preprocess an isolated copy
    // for javac so source compilation sees the same accessibility contract.''',
     '''    // The compat invokes SPR members widened by this mod's access transformer.
    // Keep the upstream SPR artifact untouched at runtime, while javac uses an
    // isolated access-transformed copy with the required accessibility contract.'''),
    ('    // Exact SPR release used by the tested Beta11 Hotfix3 environment.',
     '    // Pinned SPR release used by the supported runtime environment.'),
    ("    description = 'Apply the Hotfix3 access transformer to an isolated SPR copy used only by javac.'",
     "    description = 'Apply the compat access transformer to an isolated SPR copy used only by javac.'"),
    ("    description = 'Resolve and print the reconstruction compile classpath without compiling incomplete Phase 3 sources.'",
     "    description = 'Resolve and print the release compile classpath and verify the transformed SPR dependency.'"),
    ("    notCompatibleWithConfigurationCache('This audit task intentionally resolves and enumerates the live dependency graph.')",
     "    notCompatibleWithConfigurationCache('This verification task intentionally resolves and enumerates the live dependency graph.')"),
    ("    description = 'Exercise Gradle resource processing and verify NeoForge mixin/access-transformer registration.'",
     "    description = 'Verify processed NeoForge resources, mixin registration and access-transformer wiring.'"),
    ("    notCompatibleWithConfigurationCache('This reconstruction audit intentionally inspects processed files and source topology.')",
     "    notCompatibleWithConfigurationCache('This verification task intentionally inspects processed files and source topology.')"),
    ('            println "Phase 3 source boundary: ${missingSourceMixins.size()} configured mixin source files are not reconstructed yet: ${missingSourceMixins.join(\', \')}"',
     '            println "Source wiring warning: ${missingSourceMixins.size()} configured mixin source files are missing: ${missingSourceMixins.join(\', \')}"'),
])

patch('src/main/java/dev/cchqphysics/compat/config/ExtendedClientConfig.java', [
    ('                + " beta9DirectReuse=" + beta9DirectReuseEnabled()',
     '                + " directReuse=" + beta9DirectReuseEnabled()'),
    ('                + " beta9RoomBackoff=" + beta9RoomBackoffEnabled()',
     '                + " stableRoomSlowdown=" + beta9RoomBackoffEnabled()'),
    ('                + " beta9Adaptive=" + beta9AdaptiveControllerEnabled()',
     '                + " loadAwareScheduling=" + beta9AdaptiveControllerEnabled()'),
    ('                + " beta9MaxFactor=" + beta9MaxRoomFactor()',
     '                + " maxRoomSlowdown=" + beta9MaxRoomFactor()'),
    ('                + " beta9MaxRoomMs=" + beta9MaxRoomIntervalNs() / 1_000_000L',
     '                + " maxRoomMs=" + beta9MaxRoomIntervalNs() / 1_000_000L'),
    ('                + " beta10RayCache=" + beta10RayCacheEnabled()',
     '                + " occlusionRayReuse=" + beta10RayCacheEnabled()'),
    ('                + " beta11RoomRayMemo=" + beta11RoomRayMemoEnabled()',
     '                + " roomRayReuse=" + beta11RoomRayMemoEnabled()'),
])

patch('src/main/java/dev/cchqphysics/compat/audio/AcousticCapture.java', [
    ('beta9 capture identity mismatch for OpenAL source {}; capture disabled for safety',
     'acoustic capture identity mismatch for OpenAL source {}; capture disabled for safety'),
    ('beta9 capture stack mismatch; dropping captured acoustic state safely',
     'acoustic capture stack mismatch; dropping captured state safely'),
    ('beta9 acoustic capture was reached from a non-owner thread; falling back instead of capturing',
     'acoustic capture was reached from a non-owner thread; falling back instead of capturing'),
])

patch('src/main/java/dev/cchqphysics/compat/audio/PositionStabilizer.java', [
    ('LOGGER.debug("beta2 position source={} occlusion={} reflected={} offset={}"',
     'LOGGER.debug("position source={} occlusion={} reflected={} offset={}"'),
])

patch('src/main/java/dev/cchqphysics/compat/audio/EnvironmentSmoother.java', [
    ('LOGGER.debug("beta1 env source={} nativeCutoff={} targetCutoff={} appliedCutoff={} nativeGain={} targetGain={} appliedGain={} isolated={}"',
     'LOGGER.debug("environment source={} nativeCutoff={} targetCutoff={} appliedCutoff={} nativeGain={} targetGain={} appliedGain={} isolated={}"'),
    ('SoundPhysicsBridge.beta9Log("[phase5/source-efx] source=" + entry.getKey()',
     'SoundPhysicsBridge.beta9Log("[dump/source-filter] source=" + entry.getKey()'),
    ('LOGGER.debug("beta1 isolated EFX source={} directFilter={} sends={}/{}/{}/{} maxAux={}"',
     'LOGGER.debug("isolated filter source={} directFilter={} sends={}/{}/{}/{} maxAux={}"'),
    ('LOGGER.warn("beta1 isolated EFX failed for source {}; falling back to native SPR"',
     'LOGGER.warn("isolated filter setup failed for source {}; falling back to native SPR"'),
])

patch('src/main/java/dev/cchqphysics/compat/audio/ProgressiveOcclusionModel.java', [
    ('LOGGER.debug("beta9 progressive source={} center={} ringScale={} raw={} cutoffOcc={} gainOcc={} cutoff={} gain={} moved={} paths={} full={} weights=center:{},inner:{},outer:{}"',
     'LOGGER.debug("progressive occlusion source={} center={} ringScale={} raw={} cutoffOcc={} gainOcc={} cutoff={} gain={} moved={} paths={} full={} weights=center:{},inner:{},outer:{}"'),
    ('LOGGER.warn("beta9 progressive occlusion disabled for source {} after safe fallback"',
     'LOGGER.warn("progressive occlusion disabled for source {} after safe fallback"'),
])

patch('src/main/java/dev/cchqphysics/compat/audio/SoundPhysicsBridge.java', [
    ('''    // Phase 5 exposes the former Hotfix3 scheduler/sentinel constants through
    // ExtendedClientConfig. Its defaults are the verified Phase-4 parity values.''',
     '''    // Scheduler and fast-clearing thresholds are exposed through the advanced
    // client configuration. Release defaults are the tuned runtime values.'''),
    ('beta9Log("[phase5/source] source=" + state.sourceId',
     'beta9Log("[dump/source] source=" + state.sourceId'),
])

patch('src/main/java/dev/cchqphysics/compat/audio/RoomSchedulerClient.java', [
    ('/** Sound-thread room scheduler client; comment-only verification touch for Phase 4 semantic batch 4. */',
     '/** Sound-thread room scheduler client. */'),
])

patch('src/main/java/dev/cchqphysics/compat/audio/SyncStartCoordinator.java', [
    ('/** Sound-thread-owned synchronized-start coordinator reconstructed from Beta11 Hotfix3 bytecode. */',
     '/** Sound-thread-owned synchronized-start coordinator. */'),
    ('    // Phase 5 exposes the two Hotfix3 sync timers through ExtendedClientConfig.',
     '    // Synchronized-start grace and cleanup timers are exposed through advanced config.'),
])

patch('src/main/java/dev/cchqphysics/compat/audio/CompatAudioManager.java', [
    ('/** Client-owned CC:HQ whole-file playback bridge reconstructed against Beta11 Hotfix3 bytecode. */',
     '/** Client-owned bridge for CC:HQ whole-file positional playback. */'),
])

patch('src/main/java/dev/cchqphysics/compat/audio/Beta9Optimizer.java', [
    ('/** Hotfix3 adaptive direct/room controller reconstructed from the authoritative classfile. */',
     '/** Adaptive direct-result reuse and room-update controller. */'),
    ('    // Phase 5 exposes the Hotfix3 movement/backoff values through ExtendedClientConfig.',
     '    // Movement and room-slowdown thresholds are exposed through advanced config.'),
    ('return "beta9Meta=" + META.size() + " directCache=" + DIRECT.size()',
     'return "optimizerMeta=" + META.size() + " directCache=" + DIRECT.size()'),
    ('String report = "[CC:HQ Sound Physics Compat] beta9 extra window=" + round1(seconds)',
     'String report = "[CC:HQ Sound Physics Compat] optimizer window=" + round1(seconds)'),
])

patch('src/main/java/dev/cchqphysics/compat/audio/Beta10Optimizer.java', [
    ('/** Hotfix3 exact direct-ray reuse and bit-identical OpenAL write suppression layer. */',
     '/** Exact direct-ray reuse and bit-identical OpenAL write suppression layer. */'),
    ('    /* Retained because these fields are present in Hotfix3; normal source can reset Beta9 directly. */',
     '    /* Reflection handles for resetting the adaptive controller without changing its public surface. */'),
    ('DebugDiagnostics.cache("beta10 ray scope reset cloneTick={} config={}"',
     'DebugDiagnostics.cache("occlusion-ray scope reset cloneTick={} config={}"'),
    ('"[CC:HQ Sound Physics Compat] beta11 direct-ray window=%.1fs active=%d eligible=%d maxActive=%d rayHit=%d (%.1f/s) rayMiss=%d (%.1f/s) hitRate=%.1f%% actualRay=%.1fms/s direct=%d/%d spr=%d/%d directToSpr=%d filterWrite=%d filterSkip=%d sourceWrite=%d sourceSkip=%d idleResets=%d"',
     '"[CC:HQ Sound Physics Compat] direct-ray cache window=%.1fs active=%d eligible=%d maxActive=%d rayHit=%d (%.1f/s) rayMiss=%d (%.1f/s) hitRate=%.1f%% actualRay=%.1fms/s direct=%d/%d spr=%d/%d directToSpr=%d filterWrite=%d filterSkip=%d sourceWrite=%d sourceSkip=%d idleResets=%d"'),
    ('return "beta10Active=" + activeSources.size() + " inaudible=" + inaudibleSources.size()',
     'return "directRayActive=" + activeSources.size() + " inaudible=" + inaudibleSources.size()'),
])

patch('src/main/java/dev/cchqphysics/compat/audio/Beta11RoomRayCache.java', [
    ('DebugDiagnostics.cache("beta11 room-ray scope rotated entriesPrevious={}"',
     'DebugDiagnostics.cache("room-ray scope rotated entriesPrevious={}"'),
    ('"[CC:HQ Sound Physics Compat] beta11 room-ray window=%.1fs hit=%d (%.1f/s) miss=%d (%.1f/s) hitRate=%.1f%% actualRay=%.2fms/s crossCloneWouldReuse=%d scopeResets=%d entries=%d"',
     '"[CC:HQ Sound Physics Compat] room-ray cache window=%.1fs hit=%d (%.1f/s) miss=%d (%.1f/s) hitRate=%.1f%% actualRay=%.2fms/s crossCloneWouldReuse=%d scopeResets=%d entries=%d"'),
    ('return "beta11Entries=" + current.entries + " hit=" + hits + " miss=" + misses',
     'return "roomRayEntries=" + current.entries + " hit=" + hits + " miss=" + misses'),
])

# Acoustic-critical file: intentionally change comments/diagnostic strings only.
patch('src/main/java/dev/cchqphysics/compat/audio/VerticalDiffractionRelief.java', [
    ('''/**
 * Experimental V7.1 spreading-only opening diffraction model.
 *
 * Unlike V3-V5, an alternate opening route never replaces the normal direct
 * occlusion result. A verified aperture contributes a bounded secondary energy
 * path. Path-length difference controls a low/high diffraction loss while the
 * two SPR legs retain their own obstruction loss. The contributions are energy
 * combined without adding playback sources, phase offsets, timing changes, or
 * position changes.
 */''',
     '''/**
 * Opening-aware vertical sound model.
 *
 * A verified opening contributes a bounded secondary acoustic path while the
 * normal direct occlusion result remains authoritative. Indirect route length
 * and obstruction determine how much low/high-frequency energy carries through
 * the opening. No extra playback source, phase offset, timing change or source
 * position change is introduced.
 */'''),
    ('? "applied-portal-energy-v7-1" : "portal-energy-negligible"',
     '? "applied-opening-energy" : "opening-energy-negligible"'),
    ('SoundPhysicsBridge.beta9Log("[phase5/diffraction] no snapshots " + DiffractionConfig.summary());',
     'SoundPhysicsBridge.beta9Log("[dump/openings] no snapshots " + DiffractionConfig.summary());'),
    ('SoundPhysicsBridge.beta9Log("[phase5/diffraction] source=" + entry.getKey()',
     'SoundPhysicsBridge.beta9Log("[dump/openings] source=" + entry.getKey()'),
])

# Guard the frozen opening model. These are the exact tested formulas/constants.
v = Path('src/main/java/dev/cchqphysics/compat/audio/VerticalDiffractionRelief.java').read_text()
critical = [
    'private static final int OPENING_VERIFY_CANDIDATES = 2;',
    'private static final int OPENING_MAX_CONFIG_RADIUS = 8;',
    'private static final long CACHE_PRUNE_INTERVAL_NS = 1_000_000_000L;',
    'double rawRatio = Math.max(0.0D, legRaw / Math.max(1.0E-6D, raw));',
    'double pathGain = Math.pow(clampFilter(gain), rawRatio);',
    'double pathCutoff = Math.pow(clampFilter(cutoff), rawRatio);',
    'double spread = apertureSpreading(candidate.apertureDistance, DiffractionConfig.apertureSpreadScale());',
    'double coupling = DiffractionConfig.portalCoupling() * activation * candidate.horizonFade * spread;',
    'double quality = candidate.horizonFade * spread / (1.0D + candidate.delta + 0.5D * legRaw);',
]
for marker in critical:
    assert marker in v, f'opening acoustic invariant missing: {marker}'

print('Release surface cleanup applied; acoustic formulas preserved.')
