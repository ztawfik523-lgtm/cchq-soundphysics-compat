from pathlib import Path

SRC = Path('src/main/java/dev/cchqphysics/compat/audio/VerticalDiffractionRelief.java')
PROPS = Path('gradle.properties')

text = SRC.read_text()
orig = text

# Exact frozen-V7.1 guards. Fail instead of guessing if the source drifted.
required = [
    'private static final int LOWER_CACHE_SOFT_LIMIT = 128;',
    'private static final int CROSS_CACHE_SOFT_LIMIT = 512;',
    'private static OpeningTopology topologyCache;',
    'topologyCache = null;',
    'List<PortalCandidate> result = new ArrayList<>();\n        double directDistance = source.distanceTo(listener);',
    'topologyCache = result;\n            pruneCaches(now);',
    'private static void pruneCaches(long now) {',
    'double rawRatio = Math.max(0.0D, legRaw / Math.max(1.0E-6D, raw));',
    'double pathGain = Math.pow(clampFilter(gain), rawRatio);',
    'double pathCutoff = Math.pow(clampFilter(cutoff), rawRatio);',
    'double spread = apertureSpreading(candidate.apertureDistance, DiffractionConfig.apertureSpreadScale());',
]
for marker in required:
    assert marker in text, f'missing frozen V7.1 marker: {marker}'

text = text.replace(
    '    private static final int CROSS_CACHE_SOFT_LIMIT = 512;\n'
    '    private static final double SOURCE_RECHECK_DISTANCE_SQ = 0.01D;',
    '    private static final int CROSS_CACHE_SOFT_LIMIT = 512;\n'
    '    private static final long CACHE_PRUNE_INTERVAL_NS = 1_000_000_000L;\n'
    '    private static final double SOURCE_RECHECK_DISTANCE_SQ = 0.01D;',
    1,
)

text = text.replace(
    '    private static OpeningTopology topologyCache;\n',
    '    private static OpeningTopology topologyCache;\n'
    '    private static long lastPruneNs;\n',
    1,
)

text = text.replace(
    '        topologyCache = null;\n'
    '    }',
    '        topologyCache = null;\n'
    '        lastPruneNs = 0L;\n'
    '    }',
    1,
)

old_candidates = '''        List<PortalCandidate> result = new ArrayList<>();
        double directDistance = source.distanceTo(listener);

        if (topology.barrierY == Integer.MIN_VALUE) {'''
new_candidates = '''        double directDistance = source.distanceTo(listener);

        if (topology.barrierY == Integer.MIN_VALUE) {
            List<PortalCandidate> result = new ArrayList<>(1);'''
assert old_candidates in text
text = text.replace(old_candidates, new_candidates, 1)

old_after_implicit = '''            result.add(new PortalCandidate(key, waypoint, waypoint, 0.0D, 0.0D,
                    directDistance, route, delta, 1.0D));
            return result;
        }

        if (source.y <= topology.barrierY + 0.5D) return result;

        double lowerY = topology.barrierY - 0.25D;'''
new_after_implicit = '''            result.add(new PortalCandidate(key, waypoint, waypoint, 0.0D, 0.0D,
                    directDistance, route, delta, 1.0D));
            return result;
        }

        if (source.y <= topology.barrierY + 0.5D || topology.candidates.length == 0) {
            return List.of();
        }

        List<PortalCandidate> result = new ArrayList<>(topology.candidates.length);
        double lowerY = topology.barrierY - 0.25D;'''
assert old_after_implicit in text
text = text.replace(old_after_implicit, new_after_implicit, 1)

text = text.replace(
    '            topologyCache = result;\n'
    '            pruneCaches(now);',
    '            topologyCache = result;\n'
    '            maybePruneCaches(now);',
    1,
)

old_prune = '''    private static void pruneCaches(long now) {
        long staleNs = Math.max(1L, DiffractionConfig.openingRayCacheNs()) * 2L;
        LOWER_LEG_CACHE.entrySet().removeIf(entry -> now - entry.getValue().verifiedNs > staleNs);
        CROSS_LEG_CACHE.entrySet().removeIf(entry -> now - entry.getValue().verifiedNs > staleNs);
        trimOldestLower();
        trimOldestCross();
    }
'''
new_prune = '''    private static void maybePruneCaches(long now) {
        boolean overSoftLimit = LOWER_LEG_CACHE.size() > LOWER_CACHE_SOFT_LIMIT
                || CROSS_LEG_CACHE.size() > CROSS_CACHE_SOFT_LIMIT;
        if (!overSoftLimit && now - lastPruneNs < CACHE_PRUNE_INTERVAL_NS) return;
        lastPruneNs = now;
        pruneCaches(now);
    }

    private static void pruneCaches(long now) {
        if (LOWER_LEG_CACHE.isEmpty() && CROSS_LEG_CACHE.isEmpty()) return;
        long staleNs = Math.max(1L, DiffractionConfig.openingRayCacheNs()) * 2L;
        LOWER_LEG_CACHE.entrySet().removeIf(entry -> now - entry.getValue().verifiedNs > staleNs);
        CROSS_LEG_CACHE.entrySet().removeIf(entry -> now - entry.getValue().verifiedNs > staleNs);
        trimOldestLower();
        trimOldestCross();
    }
'''
assert old_prune in text
text = text.replace(old_prune, new_prune, 1)

assert text != orig
# Acoustic invariants must remain byte-for-byte present.
for marker in required[-4:]:
    assert marker in text, f'acoustic invariant lost: {marker}'
assert 'CACHE_PRUNE_INTERVAL_NS = 1_000_000_000L' in text
assert 'maybePruneCaches(now);' in text
assert 'new ArrayList<>(topology.candidates.length)' in text
SRC.write_text(text)

props = PROPS.read_text()
old_version = 'mod_version=0.1.0-beta11-phase5-diffraction-v7-1-spreading-only-test'
new_version = 'mod_version=0.1.0-beta11-phase5-v7-1-performance-test'
assert old_version in props, 'unexpected mod_version; refusing to patch'
PROPS.write_text(props.replace(old_version, new_version, 1))

print('V7.1 performance-only patch applied with frozen acoustic markers preserved.')
