package dev.cchqphysics.compat.audio;

import org.lwjgl.openal.AL10;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Sound-thread-owned coordinator for CC:HQ synchronized whole-file starts.
 *
 * <p>The Hotfix3 behavioral requirement is intentionally narrow: complete groups start with one
 * {@code alSourcePlayv}; incomplete groups are given a 100 ms grace period and then all sources
 * that actually arrived are started together. Sources waiting in a group remain AL_INITIAL and
 * must be treated as live by CompatAudioManager until this coordinator starts or removes them.</p>
 *
 * <p>NOTE: the exact Hotfix3 method descriptors/nested-field layout still require bytecode audit
 * once the complete reference JAR is staged. The behavior and constants here follow the tested
 * Hotfix3 handoff/invariants rather than guessed optimizations.</p>
 */
final class SyncStartCoordinator {
    static final long PARTIAL_FLUSH_NS = 100_000_000L;

    private static final Map<UUID, GroupState> GROUPS = new HashMap<>();
    private static final Map<Integer, UUID> PENDING_SOURCE_TO_GROUP = new HashMap<>();

    private SyncStartCoordinator() {
    }

    /**
     * Queue a source for synchronized start. Returns true when the source was accepted as pending.
     * A null group or a declared size <= 1 is not a synchronized group and is left to the caller.
     */
    static boolean addSource(UUID groupId, int expectedSize, int sourceId) {
        if (groupId == null || expectedSize <= 1 || sourceId == 0) {
            return false;
        }

        removeSource(sourceId);
        long now = System.nanoTime();
        GroupState group = GROUPS.get(groupId);
        if (group == null) {
            group = new GroupState(Math.max(1, expectedSize), now);
            GROUPS.put(groupId, group);
        } else if (expectedSize > group.expectedSize) {
            // A later packet may carry the authoritative declared group size. Never shrink it.
            group.expectedSize = expectedSize;
        }

        group.sourceIds.add(sourceId);
        PENDING_SOURCE_TO_GROUP.put(sourceId, groupId);

        if (group.sourceIds.size() >= group.expectedSize) {
            playAndRemove(groupId, group);
        }
        return true;
    }

    /** Start any incomplete group whose first-arrival grace period has expired. */
    static void flushExpired() {
        if (GROUPS.isEmpty()) {
            return;
        }
        long now = System.nanoTime();
        Iterator<Map.Entry<UUID, GroupState>> it = GROUPS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, GroupState> entry = it.next();
            GroupState group = entry.getValue();
            if (!group.sourceIds.isEmpty() && now - group.firstArrivalNs >= PARTIAL_FLUSH_NS) {
                int[] ids = toArray(group.sourceIds);
                clearPending(group.sourceIds);
                it.remove();
                AL10.alSourcePlayv(ids);
            }
        }
    }

    static boolean isPending(int sourceId) {
        return PENDING_SOURCE_TO_GROUP.containsKey(sourceId);
    }

    /** Remove a source from any not-yet-started group, e.g. stop/replacement before group launch. */
    static void removeSource(int sourceId) {
        UUID groupId = PENDING_SOURCE_TO_GROUP.remove(sourceId);
        if (groupId == null) {
            return;
        }
        GroupState group = GROUPS.get(groupId);
        if (group == null) {
            return;
        }
        group.sourceIds.remove((Integer) sourceId);
        if (group.sourceIds.isEmpty()) {
            GROUPS.remove(groupId);
        }
    }

    static void clear() {
        GROUPS.clear();
        PENDING_SOURCE_TO_GROUP.clear();
    }

    private static void playAndRemove(UUID groupId, GroupState group) {
        int[] ids = toArray(group.sourceIds);
        clearPending(group.sourceIds);
        GROUPS.remove(groupId);
        AL10.alSourcePlayv(ids);
    }

    private static void clearPending(List<Integer> sourceIds) {
        for (int sourceId : sourceIds) {
            PENDING_SOURCE_TO_GROUP.remove(sourceId);
        }
    }

    private static int[] toArray(List<Integer> sourceIds) {
        int[] ids = new int[sourceIds.size()];
        for (int i = 0; i < sourceIds.size(); i++) {
            ids[i] = sourceIds.get(i);
        }
        return ids;
    }

    /** Compiler-generated nested output is expected for this authored group-state class. */
    private static final class GroupState {
        int expectedSize;
        final long firstArrivalNs;
        final ArrayList<Integer> sourceIds = new ArrayList<>();

        GroupState(int expectedSize, long firstArrivalNs) {
            this.expectedSize = expectedSize;
            this.firstArrivalNs = firstArrivalNs;
        }
    }
}
