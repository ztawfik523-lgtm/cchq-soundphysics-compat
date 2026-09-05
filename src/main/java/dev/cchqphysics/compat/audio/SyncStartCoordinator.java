package dev.cchqphysics.compat.audio;

import dev.cchqphysics.compat.config.ExtendedClientConfig;
import org.lwjgl.openal.AL10;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Sound-thread-owned synchronized-start coordinator. */
final class SyncStartCoordinator {
    private static final Map<UUID, Group> GROUPS = new HashMap<>();
    private static final Map<Integer, UUID> LIVE_SOURCE_GROUPS = new HashMap<>();
    private static final Map<UUID, List<Integer>> LIVE_GROUP_SOURCES = new HashMap<>();
    // Synchronized-start grace and cleanup timers are exposed through advanced config.

    private SyncStartCoordinator() {
    }

    static synchronized void play(int sourceId, HQPayloadView.Audio audio) {
        long now = System.nanoTime();
        flushExpired(now);
        GROUPS.entrySet().removeIf(entry -> now - entry.getValue().createdNs > ExtendedClientConfig.syncStaleGroupNs());

        UUID groupId = audio.syncGroupId();
        int expected = audio.syncGroupSize();
        if (groupId == null || expected <= 1) {
            DebugDiagnostics.sync("source={} immediate start (no sync group)", sourceId);
            AL10.alSourcePlay(sourceId);
            return;
        }

        rememberLiveGroup(sourceId, groupId);
        Group group = GROUPS.get(groupId);
        if (group == null) {
            group = new Group(expected);
            GROUPS.put(groupId, group);
            DebugDiagnostics.sync("group={} created expected={}", groupId, expected);
        }
        if (expected > group.expected) {
            group.expected = expected;
        }
        if (!group.sources.contains(sourceId)) {
            group.sources.add(sourceId);
            DebugDiagnostics.sync("group={} queued source={} count={}/{}", groupId, sourceId, group.sources.size(), group.expected);
        }
        if (group.sources.size() >= group.expected) {
            startAndRemove(groupId, group);
        }
    }

    static synchronized int sourceState(int sourceId, int param) {
        long now = System.nanoTime();
        flushExpired(now);
        int state = AL10.alGetSourcei(sourceId, param);
        if (param == AL10.AL_SOURCE_STATE && state == AL10.AL_INITIAL && isPending(sourceId)) {
            return AL10.AL_PAUSED;
        }
        return state;
    }

    private static boolean isPending(int sourceId) {
        for (Group group : GROUPS.values()) {
            if (group.sources.contains(sourceId)) {
                return true;
            }
        }
        return false;
    }

    private static void flushExpired(long now) {
        Iterator<Map.Entry<UUID, Group>> iterator = GROUPS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Group> entry = iterator.next();
            Group group = entry.getValue();
            if (!group.sources.isEmpty() && now - group.createdNs >= ExtendedClientConfig.syncPartialFlushNs()) {
                DebugDiagnostics.sync("group={} partial flush count={}/{} ageMs={}", entry.getKey(), group.sources.size(), group.expected,
                        (now - group.createdNs) / 1_000_000.0D);
                playVector(group.sources);
                iterator.remove();
            }
        }
    }

    private static void startAndRemove(UUID groupId, Group group) {
        DebugDiagnostics.sync("group={} complete start count={}/{}", groupId, group.sources.size(), group.expected);
        playVector(group.sources);
        GROUPS.remove(groupId);
    }

    private static void playVector(List<Integer> sources) {
        int[] ids = new int[sources.size()];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = sources.get(i);
        }
        AL10.alSourcePlayv(ids);
    }

    static synchronized void clear() {
        DebugDiagnostics.sync("clear pending sync groups count={} liveGroups={}", GROUPS.size(), LIVE_GROUP_SOURCES.size());
        GROUPS.clear();
        LIVE_SOURCE_GROUPS.clear();
        LIVE_GROUP_SOURCES.clear();
    }

    static synchronized void removeSource(int sourceId) {
        Iterator<Group> iterator = GROUPS.values().iterator();
        while (iterator.hasNext()) {
            Group group = iterator.next();
            if (group.sources.remove((Integer) sourceId)) {
                DebugDiagnostics.sync("removed source={} from pending group remaining={}/{}", sourceId, group.sources.size(), group.expected);
            }
            if (group.sources.isEmpty()) {
                iterator.remove();
            }
        }

        UUID liveGroupId = LIVE_SOURCE_GROUPS.remove(sourceId);
        if (liveGroupId != null) {
            List<Integer> live = LIVE_GROUP_SOURCES.get(liveGroupId);
            if (live != null) {
                live.remove((Integer) sourceId);
                if (live.isEmpty()) LIVE_GROUP_SOURCES.remove(liveGroupId);
            }
        }
    }

    static synchronized int[] livePeerSources(int sourceId) {
        UUID groupId = LIVE_SOURCE_GROUPS.get(sourceId);
        if (groupId == null) return new int[0];
        List<Integer> sources = LIVE_GROUP_SOURCES.get(groupId);
        if (sources == null || sources.size() <= 1) return new int[0];

        int[] result = new int[sources.size() - 1];
        int index = 0;
        for (int id : sources) {
            if (id != sourceId) result[index++] = id;
        }
        if (index == result.length) return result;

        int[] trimmed = new int[index];
        System.arraycopy(result, 0, trimmed, 0, index);
        return trimmed;
    }

    static synchronized int[] liveGroupedSources() {
        int[] result = new int[LIVE_SOURCE_GROUPS.size()];
        int index = 0;
        for (int sourceId : LIVE_SOURCE_GROUPS.keySet()) result[index++] = sourceId;
        return result;
    }

    private static void rememberLiveGroup(int sourceId, UUID groupId) {
        UUID previous = LIVE_SOURCE_GROUPS.put(sourceId, groupId);
        if (previous != null && !previous.equals(groupId)) {
            List<Integer> old = LIVE_GROUP_SOURCES.get(previous);
            if (old != null) {
                old.remove((Integer) sourceId);
                if (old.isEmpty()) LIVE_GROUP_SOURCES.remove(previous);
            }
        }

        List<Integer> live = LIVE_GROUP_SOURCES.computeIfAbsent(groupId, ignored -> new ArrayList<>());
        if (!live.contains(sourceId)) live.add(sourceId);
    }

    static synchronized String debugSummary() {
        int pendingSources = 0;
        for (Group group : GROUPS.values()) pendingSources += group.sources.size();
        return "syncGroups=" + GROUPS.size() + " pendingSources=" + pendingSources
                + " liveSyncGroups=" + LIVE_GROUP_SOURCES.size() + " liveGroupedSources=" + LIVE_SOURCE_GROUPS.size();
    }

    private static final class Group {
        int expected;
        final List<Integer> sources = new ArrayList<>();
        final long createdNs = System.nanoTime();

        Group(int expected) {
            this.expected = expected;
        }
    }
}
