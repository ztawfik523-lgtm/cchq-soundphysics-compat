package dev.cchqphysics.compat.audio;

import com.sonicether.soundphysics.utils.RaycastUtils;
import dev.cchqphysics.compat.config.ClientConfig;
import dev.cchqphysics.compat.config.ExtendedClientConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;

/** Exact cache for SPR's static environment/bounce raycasts during HQ-owned processSound calls. */
public final class Beta11RoomRayCache {
    private static final int SIZE = 8192;
    private static final int MASK = SIZE - 1;
    private static final int PROBES = 8;
    private static final long REPORT_NS = 10_000_000_000L;

    private static final CacheBank BANK_A = new CacheBank();
    private static final CacheBank BANK_B = new CacheBank();
    private static CacheBank current = BANK_A;
    private static CacheBank previous = BANK_B;
    private static Object scopeGetter;

    private static long hits;
    private static long misses;
    private static long actualRayNs;
    private static long crossCloneWouldReuse;
    private static long scopeResets;
    private static long reportStartNs = System.nanoTime();
    private static int operationsUntilDiagRefresh = 1;
    private static boolean diagnostics;

    private Beta11RoomRayCache() {}

    public static BlockHitResult rayCast(BlockGetter level, Vec3 from, Vec3 to, BlockPos ignore) {
        if (!ExtendedClientConfig.beta11RoomRayMemoEnabled()
                || !Beta10Optimizer.beta11RoomCacheActive() || level == null) {
            return RaycastUtils.rayCast(level, from, to, ignore);
        }

        if (level != scopeGetter) rotateScope(level);

        long ax = Double.doubleToLongBits(from.x);
        long ay = Double.doubleToLongBits(from.y);
        long az = Double.doubleToLongBits(from.z);
        long bx = Double.doubleToLongBits(to.x);
        long by = Double.doubleToLongBits(to.y);
        long bz = Double.doubleToLongBits(to.z);
        int ip = ignore == null ? 0 : 1;
        int ix = ignore == null ? 0 : ignore.getX();
        int iy = ignore == null ? 0 : ignore.getY();
        int iz = ignore == null ? 0 : ignore.getZ();

        int slot = current.find(ax, ay, az, bx, by, bz, ip, ix, iy, iz);
        if (slot >= 0) {
            hits++;
            maybeDiagnostics();
            return current.result[slot];
        }

        if (diagnostics && previous.find(ax, ay, az, bx, by, bz, ip, ix, iy, iz) >= 0) {
            crossCloneWouldReuse++;
        }

        long start = diagnostics ? System.nanoTime() : 0L;
        BlockHitResult result = RaycastUtils.rayCast(level, from, to, ignore);
        if (diagnostics) actualRayNs += System.nanoTime() - start;
        misses++;
        current.put(ax, ay, az, bx, by, bz, ip, ix, iy, iz, result);
        maybeDiagnostics();
        return result;
    }

    private static void rotateScope(Object getter) {
        CacheBank oldPrevious = previous;
        previous = current;
        current = oldPrevious;
        current.clear();
        scopeGetter = getter;
        scopeResets++;
        DebugDiagnostics.cache("beta11 room-ray scope rotated entriesPrevious={}", previous.entries);
    }

    private static void maybeDiagnostics() {
        if (--operationsUntilDiagRefresh <= 0) {
            operationsUntilDiagRefresh = 512;
            diagnostics = ClientConfig.diagnosticsEnabled();
        }
        if (!diagnostics) return;
        long now = System.nanoTime();
        long elapsed = now - reportStartNs;
        if (elapsed < ExtendedClientConfig.performanceReportNs()) return;
        double seconds = elapsed / 1_000_000_000.0;
        long total = hits + misses;
        double hitRate = total == 0 ? 0.0 : (100.0 * hits / total);
        SoundPhysicsBridge.beta9Log(String.format(java.util.Locale.ROOT,
                "[CC:HQ Sound Physics Compat] beta11 room-ray window=%.1fs hit=%d (%.1f/s) miss=%d (%.1f/s) hitRate=%.1f%% actualRay=%.2fms/s crossCloneWouldReuse=%d scopeResets=%d entries=%d",
                seconds, hits, hits / seconds, misses, misses / seconds, hitRate,
                (actualRayNs / 1_000_000.0) / seconds, crossCloneWouldReuse, scopeResets, current.entries));
        hits = misses = actualRayNs = crossCloneWouldReuse = scopeResets = 0L;
        reportStartNs = now;
    }

    static synchronized void clear() {
        BANK_A.clear();
        BANK_B.clear();
        current = BANK_A;
        previous = BANK_B;
        scopeGetter = null;
        hits = misses = actualRayNs = crossCloneWouldReuse = scopeResets = 0L;
        reportStartNs = System.nanoTime();
        operationsUntilDiagRefresh = 1;
        diagnostics = false;
    }

    static synchronized long[] statsForTest() {
        return new long[]{hits, misses, actualRayNs, crossCloneWouldReuse, scopeResets, current.entries};
    }

    static synchronized String debugSummary() {
        return "beta11Entries=" + current.entries + " hit=" + hits + " miss=" + misses
                + " crossCloneTelemetry=" + crossCloneWouldReuse + " scopeResets=" + scopeResets;
    }

    private static final class CacheBank {
        final boolean[] used = new boolean[SIZE];
        final long[] ax = new long[SIZE];
        final long[] ay = new long[SIZE];
        final long[] az = new long[SIZE];
        final long[] bx = new long[SIZE];
        final long[] by = new long[SIZE];
        final long[] bz = new long[SIZE];
        final int[] ip = new int[SIZE];
        final int[] ix = new int[SIZE];
        final int[] iy = new int[SIZE];
        final int[] iz = new int[SIZE];
        final BlockHitResult[] result = new BlockHitResult[SIZE];
        int entries;

        void clear() {
            Arrays.fill(used, false);
            Arrays.fill(result, null);
            entries = 0;
        }

        int find(long a0, long a1, long a2, long b0, long b1, long b2,
                 int p, int x, int y, int z) {
            int base = mixIndex(a0, a1, a2, b0, b1, b2, p, x, y, z);
            for (int probe = 0; probe < PROBES; probe++) {
                int slot = (base + probe) & MASK;
                if (!used[slot]) return -1;
                if (ax[slot] == a0 && ay[slot] == a1 && az[slot] == a2 &&
                        bx[slot] == b0 && by[slot] == b1 && bz[slot] == b2 &&
                        ip[slot] == p && ix[slot] == x && iy[slot] == y && iz[slot] == z) {
                    return slot;
                }
            }
            return -1;
        }

        void put(long a0, long a1, long a2, long b0, long b1, long b2,
                 int p, int x, int y, int z, BlockHitResult value) {
            int base = mixIndex(a0, a1, a2, b0, b1, b2, p, x, y, z);
            int chosen = base;
            for (int probe = 0; probe < PROBES; probe++) {
                int slot = (base + probe) & MASK;
                if (!used[slot]) { chosen = slot; break; }
                if (ax[slot] == a0 && ay[slot] == a1 && az[slot] == a2 &&
                        bx[slot] == b0 && by[slot] == b1 && bz[slot] == b2 &&
                        ip[slot] == p && ix[slot] == x && iy[slot] == y && iz[slot] == z) {
                    chosen = slot; break;
                }
            }
            if (!used[chosen]) entries++;
            used[chosen] = true;
            ax[chosen] = a0; ay[chosen] = a1; az[chosen] = a2;
            bx[chosen] = b0; by[chosen] = b1; bz[chosen] = b2;
            ip[chosen] = p; ix[chosen] = x; iy[chosen] = y; iz[chosen] = z;
            result[chosen] = value;
        }
    }

    private static int mixIndex(long a0, long a1, long a2, long b0, long b1, long b2,
                                int p, int x, int y, int z) {
        long h = 0x9E3779B97F4A7C15L;
        h = mixOne(h, a0); h = mixOne(h, a1); h = mixOne(h, a2);
        h = mixOne(h, b0); h = mixOne(h, b1); h = mixOne(h, b2);
        h = mixOne(h, (((long)p) << 63) ^ (((long)x) << 32) ^ (y & 0xffffffffL));
        h = mixOne(h, z);
        return ((int)(h ^ (h >>> 32))) & MASK;
    }

    private static long mixOne(long h, long v) {
        v ^= v >>> 33;
        v *= 0xff51afd7ed558ccdl;
        v ^= v >>> 33;
        v *= 0xc4ceb9fe1a85ec53l;
        v ^= v >>> 33;
        h ^= v + 0x9E3779B97F4A7C15L + (h << 6) + (h >>> 2);
        return h;
    }
}
