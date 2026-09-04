package dev.cchqphysics.compat.audio;

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
    private static final long PARTIAL_FLUSH_NS = 100_000_000L;
    private static final long STALE_GROUP_NS = 5_000_000_000L;

    private SyncStartCoordinator() {
    }

    static synchronized void play(int sourceId, HQPayloadView.Audio audio) {
        long now = System.nanoTime();
        flushExpired(now);
        GROUPS.entrySet().removeIf(entry -> now - entry.getValue().createdNs > STALE_GROUP_NS);

        UUID groupId = audio.syncGroupId();
        int expected = audio.syncGroupSize();
        if (groupId == null || expected <= 1) {
            AL10.alSourcePlay(sourceId);
            return;
        }

        Group group = GROUPS.computeIfAbsent(groupId, ignored -> new Group(expected));
        if (expected > group.expected) {
            group.expected = expected;
        }
        if (!group.sources.contains(sourceId)) {
            group.sources.add(sourceId);
        }
        if (group.sources.size() >= group.expected) {
            startAndRemove(groupId, group);
        }
    }

    static synchronized int sourceState(int sourceId, int param) {
        flushExpired(System.nanoTime());
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
            if (!group.sources.isEmpty() && now - group.createdNs >= PARTIAL_FLUSH_NS) {
                playVector(group.sources);
                iterator.remove();
            }
        }
    }

    private static void startAndRemove(UUID groupId, Group group) {
        playVector(group.sources);
        GROUPS.remove(groupId);
    }

    private static void playVector(List<Integer> sources) {
        int[] ids = new int[sources.size()];
        for (int i = 0; i < sources.size(); i++) {
            ids[i] = sources.get(i);
        }
        AL10.alSourcePlayv(ids);
    }

    static synchronized void clear() {
        GROUPS.clear();
    }

    static synchronized void removeSource(int sourceId) {
        Iterator<Map.Entry<UUID, Group>> iterator = GROUPS.entrySet().iterator();
        while (iterator.hasNext()) {
            Group group = iterator.next().getValue();
            group.sources.remove((Integer) sourceId);
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
