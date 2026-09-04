package dev.cchqphysics.compat.audio;

import dev.cchqphysics.compat.config.ExtendedClientConfig;
import org.lwjgl.openal.AL10;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Sound-thread-owned synchronized-start coordinator reconstructed from Beta11 Hotfix3 bytecode. */
final class SyncStartCoordinator {
    private static final Map<UUID, Group> GROUPS = new HashMap<>();
    // Phase 5 exposes the two Hotfix3 sync timers through ExtendedClientConfig.

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
        DebugDiagnostics.sync("clear pending sync groups count={}", GROUPS.size());
        GROUPS.clear();
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
