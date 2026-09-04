package dev.cchqphysics.compat.audio;

import com.sonicether.soundphysics.SoundPhysics;
import dev.cchqphysics.compat.config.ClientConfig;
import dev.cchqphysics.compat.config.ExtendedClientConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.EXTEfx;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Hotfix3 exact direct-ray reuse and bit-identical OpenAL write suppression layer. */
public final class Beta10Optimizer {
    private static final int RAY_CACHE_SIZE = 2048;
    private static final int RAY_CACHE_MASK = 2047;
    private static final int RAY_PROBES = 6;
    private static final long REPORT_NS = 10_000_000_000L;
    private static final long DEBUG_REFRESH_NS = 250_000_000L;
    private static final float AUDIBLE_EPSILON = 1.0E-4F;
    private static final byte OWNER_NONE = 0;
    private static final byte OWNER_DIRECT = 1;
    private static final byte OWNER_SPR = 2;

    private static final ThreadLocal<Context> CONTEXT = new ThreadLocal<>();

    private static final boolean[] rayUsed = new boolean[RAY_CACHE_SIZE];
    private static final long[] rayAx = new long[RAY_CACHE_SIZE];
    private static final long[] rayAy = new long[RAY_CACHE_SIZE];
    private static final long[] rayAz = new long[RAY_CACHE_SIZE];
    private static final long[] rayBx = new long[RAY_CACHE_SIZE];
    private static final long[] rayBy = new long[RAY_CACHE_SIZE];
    private static final long[] rayBz = new long[RAY_CACHE_SIZE];
    private static final double[] rayValue = new double[RAY_CACHE_SIZE];
    private static final byte[] rayOwner = new byte[RAY_CACHE_SIZE];

    private static Object scopeClone;
    private static long scopeCloneTick = Long.MIN_VALUE;
    private static long scopeConfig = Long.MIN_VALUE;

    private static final Set<Integer> activeSources = new HashSet<>();
    private static final Set<Integer> inaudibleSources = new HashSet<>();
    private static boolean hadEligibleSources;
    private static final Map<Integer, FilterState> filterStates = new HashMap<>();
    private static final Map<Integer, SourceAlState> sourceAlStates = new HashMap<>();

    private static long reportStartNs = System.nanoTime();
    private static long rayHits;
    private static long rayMisses;
    private static long rayActualNs;
    private static long directRayHits;
    private static long directRayMisses;
    private static long sprRayHits;
    private static long sprRayMisses;
    private static long directToSprHits;
    private static long filterWrites;
    private static long filterSkips;
    private static long sourceWrites;
    private static long sourceSkips;
    private static long controllerIdleResets;
    private static int maxActiveSources;

    private static long debugLastReadNs;
    private static boolean debugAllowsCache;
    private static boolean debugReflectionFailed;
    private static Object sprConfig;
    private static Field renderOcclusionField;
    private static Field occlusionLoggingField;
    private static Method configEntryGet;

    private static Class<?> stampClass;
    private static Field stampReusableField;
    private static Field stampCloneField;
    private static Field stampTickField;
    private static Field stampConfigField;

    /* Retained because these fields are present in Hotfix3; normal source can reset Beta9 directly. */
    @SuppressWarnings("unused") private static boolean resetReflectionReady;
    @SuppressWarnings("unused") private static Field beta9AdaptiveField, beta9ReportMinField, beta9ReportMaxField;
    @SuppressWarnings("unused") private static Field beta9PressureField, beta9HealthyField;
    @SuppressWarnings("unused") private static Field beta9CtrlAcousticField, beta9CtrlSprField, beta9CtrlQueueField;
    @SuppressWarnings("unused") private static Field beta9CtrlQueueMaxField, beta9CtrlQueueSamplesField, beta9CtrlSprCallsField;
    @SuppressWarnings("unused") private static Field beta9LastAcousticField, beta9LastSprField, beta9LastQueueAvgField, beta9LastQueueMaxField;
    @SuppressWarnings("unused") private static Field beta9ControlStartField;

    private Beta10Optimizer() {}

    static synchronized void registerSource(int sourceId) {
        boolean wasEmpty = activeSources.isEmpty();
        activeSources.add(sourceId);
        inaudibleSources.remove(sourceId);
        sourceAlStates.remove(sourceId);
        if (activeSources.size() > maxActiveSources) maxActiveSources = activeSources.size();
        if (wasEmpty) {
            resetBeta9Controller();
            controllerIdleResets++;
        }
        hadEligibleSources = true;
        Beta9Optimizer.registerSource(sourceId);
        maybeReport();
    }

    static synchronized void unregisterSource(int sourceId) {
        Beta9Optimizer.unregisterSource(sourceId);
        activeSources.remove(sourceId);
        inaudibleSources.remove(sourceId);
        sourceAlStates.remove(sourceId);
        checkEligibilityReset();
        maybeReport();
    }

    static synchronized void updateAudibility(int sourceId, float gain) {
        Beta9Optimizer.updateAudibility(sourceId, gain);
        if (gain > AUDIBLE_EPSILON) inaudibleSources.remove(sourceId);
        else if (activeSources.contains(sourceId)) inaudibleSources.add(sourceId);
        checkEligibilityReset();
    }

    private static void checkEligibilityReset() {
        boolean eligible = !activeSources.isEmpty() && activeSources.size() > inaudibleSources.size();
        if (!eligible && hadEligibleSources) {
            resetBeta9Controller();
            controllerIdleResets++;
            hadEligibleSources = false;
        } else if (eligible) {
            hadEligibleSources = true;
        }
    }

    static synchronized void recordQueueDelay(long delayNs) {
        if (!activeSources.isEmpty() && activeSources.size() > inaudibleSources.size()) {
            Beta9Optimizer.recordQueueDelay(delayNs);
        }
        maybeReport();
    }

    static float[] adjustDirect(int sourceId, float cutoff, float gain) {
        Context previous = CONTEXT.get();
        Context context = contextFor(sourceId, OWNER_DIRECT, previous);
        CONTEXT.set(context);
        try {
            return Beta9Optimizer.adjustDirect(sourceId, cutoff, gain);
        } finally {
            restore(previous);
        }
    }

    static Vec3 processSound(int sourceId, double x, double y, double z, SoundSource category, ResourceLocation sound) {
        Context previous = CONTEXT.get();
        Context context = contextFor(sourceId, OWNER_SPR, previous);
        CONTEXT.set(context);
        try {
            return SoundPhysics.processSound(sourceId, x, y, z, category, sound);
        } finally {
            restore(previous);
        }
    }

    private static Context contextFor(int sourceId, byte owner, Context previous) {
        Object stamp = SoundPhysicsBridge.beta9CaptureStamp(sourceId);
        StampInfo info = readStamp(stamp);
        boolean cacheable = info != null && info.reusable
                && ExtendedClientConfig.beta10RayCacheEnabled()
                && debugAllowsRayCache();
        return new Context(sourceId, owner, stamp, info, cacheable, previous);
    }

    private static void restore(Context previous) {
        if (previous == null) CONTEXT.remove();
        else CONTEXT.set(previous);
    }

    static double runOcclusionDirect(Vec3 from, Vec3 to) {
        return runOcclusionCached(from, to, OWNER_DIRECT);
    }

    public static double runOcclusionSpr(Vec3 from, Vec3 to) {
        Context context = CONTEXT.get();
        if (context == null || context.owner != OWNER_SPR) return SoundPhysics.runOcclusion(from, to);
        return runOcclusionCached(from, to, OWNER_SPR);
    }

    private static double runOcclusionCached(Vec3 from, Vec3 to, byte owner) {
        Context context = CONTEXT.get();
        if (context == null || !context.cacheable || context.stamp == null || context.stampInfo == null
                || context.sourceId == Integer.MIN_VALUE) {
            long start = System.nanoTime();
            double value = SoundPhysics.runOcclusion(from, to);
            recordMiss(owner, System.nanoTime() - start);
            return value;
        }

        Object startStamp = context.stamp;
        StampInfo startInfo = context.stampInfo;
        long ax = Double.doubleToLongBits(from.x);
        long ay = Double.doubleToLongBits(from.y);
        long az = Double.doubleToLongBits(from.z);
        long bx = Double.doubleToLongBits(to.x);
        long by = Double.doubleToLongBits(to.y);
        long bz = Double.doubleToLongBits(to.z);

        synchronized (Beta10Optimizer.class) {
            prepareRayScope(startInfo);
            int slot = findRaySlot(ax, ay, az, bx, by, bz);
            if (slot >= 0) {
                rayHits++;
                if (owner == OWNER_DIRECT) directRayHits++;
                else sprRayHits++;
                if (owner == OWNER_SPR && rayOwner[slot] == OWNER_DIRECT) directToSprHits++;
                maybeReportLocked(System.nanoTime());
                return rayValue[slot];
            }
        }

        long start = System.nanoTime();
        double value = SoundPhysics.runOcclusion(from, to);
        long elapsed = System.nanoTime() - start;
        Object endStamp = SoundPhysicsBridge.beta9CaptureStamp(context.sourceId);
        boolean same = SoundPhysicsBridge.beta9SameStamp(startStamp, endStamp);
        synchronized (Beta10Optimizer.class) {
            recordMissLocked(owner, elapsed);
            StampInfo endInfo = readStamp(endStamp);
            if (same && endInfo != null && endInfo.reusable) {
                prepareRayScope(endInfo);
                putRay(ax, ay, az, bx, by, bz, value, owner);
            }
            maybeReportLocked(System.nanoTime());
        }
        return value;
    }

    private static void prepareRayScope(StampInfo info) {
        if (scopeClone != info.cloneIdentity || scopeCloneTick != info.cloneTick || scopeConfig != info.configFingerprint) {
            Arrays.fill(rayUsed, false);
            Arrays.fill(rayOwner, OWNER_NONE);
            scopeClone = info.cloneIdentity;
            scopeCloneTick = info.cloneTick;
            scopeConfig = info.configFingerprint;
            DebugDiagnostics.cache("beta10 ray scope reset cloneTick={} config={}", info.cloneTick, info.configFingerprint);
        }
    }

    private static int findRaySlot(long ax, long ay, long az, long bx, long by, long bz) {
        int base = mixIndex(ax, ay, az, bx, by, bz);
        for (int probe = 0; probe < RAY_PROBES; probe++) {
            int slot = (base + probe) & RAY_CACHE_MASK;
            if (!rayUsed[slot]) return -1;
            if (rayAx[slot] == ax && rayAy[slot] == ay && rayAz[slot] == az
                    && rayBx[slot] == bx && rayBy[slot] == by && rayBz[slot] == bz) return slot;
        }
        return -1;
    }

    private static void putRay(long ax, long ay, long az, long bx, long by, long bz, double value, byte owner) {
        int base = mixIndex(ax, ay, az, bx, by, bz);
        int chosen = -1;
        for (int probe = 0; probe < RAY_PROBES; probe++) {
            int slot = (base + probe) & RAY_CACHE_MASK;
            if (!rayUsed[slot] || (rayAx[slot] == ax && rayAy[slot] == ay && rayAz[slot] == az
                    && rayBx[slot] == bx && rayBy[slot] == by && rayBz[slot] == bz)) {
                chosen = slot;
                break;
            }
        }
        if (chosen < 0) chosen = base;
        rayUsed[chosen] = true;
        rayAx[chosen] = ax; rayAy[chosen] = ay; rayAz[chosen] = az;
        rayBx[chosen] = bx; rayBy[chosen] = by; rayBz[chosen] = bz;
        rayValue[chosen] = value;
        rayOwner[chosen] = owner;
    }

    private static int mixIndex(long ax, long ay, long az, long bx, long by, long bz) {
        long h = 0x9E3779B97F4A7C15L;
        h = mixOne(h, ax); h = mixOne(h, ay); h = mixOne(h, az);
        h = mixOne(h, bx); h = mixOne(h, by); h = mixOne(h, bz);
        return ((int) (h ^ (h >>> 32))) & RAY_CACHE_MASK;
    }

    private static long mixOne(long h, long v) {
        v ^= v >>> 33;
        v *= 0xff51afd7ed558ccdl;
        v ^= v >>> 33;
        v *= 0xc4ceb9fe1a85ec53l;
        v ^= v >>> 33;
        return h ^ (v + 0x9E3779B97F4A7C15L + (h << 6) + (h >>> 2));
    }

    private static void recordMiss(byte owner, long elapsedNs) {
        synchronized (Beta10Optimizer.class) {
            recordMissLocked(owner, elapsedNs);
            maybeReportLocked(System.nanoTime());
        }
    }

    private static void recordMissLocked(byte owner, long elapsedNs) {
        rayMisses++;
        rayActualNs += Math.max(0L, elapsedNs);
        if (owner == OWNER_DIRECT) directRayMisses++;
        else sprRayMisses++;
    }

    static synchronized int alGenFilter() {
        int filter = EXTEfx.alGenFilters();
        filterStates.remove(filter);
        return filter;
    }

    static synchronized void alDeleteFilter(int filter) {
        filterStates.remove(filter);
        EXTEfx.alDeleteFilters(filter);
    }

    static synchronized void alFilterfMaybe(int filter, int param, float value) {
        int bits = Float.floatToIntBits(value);
        FilterState state = filterStates.computeIfAbsent(filter, ignored -> new FilterState());
        if (param == 1) {
            if (state.gainKnown && state.gainBits == bits) {
                filterSkips++;
                maybeReportLocked(System.nanoTime());
                return;
            }
            EXTEfx.alFilterf(filter, param, value);
            state.gainKnown = true;
            state.gainBits = bits;
        } else if (param == 2) {
            if (state.hfKnown && state.hfBits == bits) {
                filterSkips++;
                maybeReportLocked(System.nanoTime());
                return;
            }
            EXTEfx.alFilterf(filter, param, value);
            state.hfKnown = true;
            state.hfBits = bits;
        } else {
            EXTEfx.alFilterf(filter, param, value);
        }
        filterWrites++;
        maybeReportLocked(System.nanoTime());
    }

    static synchronized void alSourcefStable(int sourceId, int param, float value) {
        SourceAlState state = sourceAlStates.computeIfAbsent(sourceId, ignored -> new SourceAlState());
        int bits = Float.floatToIntBits(value);
        if (state.gainKnown && state.gainParam == param && state.gainBits == bits) {
            sourceSkips++;
            maybeReportLocked(System.nanoTime());
            return;
        }
        AL10.alSourcef(sourceId, param, value);
        state.gainKnown = true;
        state.gainParam = param;
        state.gainBits = bits;
        sourceWrites++;
        maybeReportLocked(System.nanoTime());
    }

    static synchronized void alSource3fStable(int sourceId, int param, float x, float y, float z) {
        SourceAlState state = sourceAlStates.computeIfAbsent(sourceId, ignored -> new SourceAlState());
        int xb = Float.floatToIntBits(x), yb = Float.floatToIntBits(y), zb = Float.floatToIntBits(z);
        if (state.posKnown && state.posParam == param && state.xBits == xb && state.yBits == yb && state.zBits == zb) {
            sourceSkips++;
            maybeReportLocked(System.nanoTime());
            return;
        }
        AL10.alSource3f(sourceId, param, x, y, z);
        state.posKnown = true;
        state.posParam = param;
        state.xBits = xb; state.yBits = yb; state.zBits = zb;
        sourceWrites++;
        maybeReportLocked(System.nanoTime());
    }

    private static StampInfo readStamp(Object stamp) {
        if (stamp == null) return null;
        try {
            synchronized (Beta10Optimizer.class) {
                Class<?> clazz = stamp.getClass();
                if (stampClass != clazz) {
                    stampClass = clazz;
                    stampReusableField = clazz.getDeclaredField("reusable");
                    stampCloneField = clazz.getDeclaredField("cloneIdentity");
                    stampTickField = clazz.getDeclaredField("cloneTick");
                    stampConfigField = clazz.getDeclaredField("configFingerprint");
                    stampReusableField.setAccessible(true);
                    stampCloneField.setAccessible(true);
                    stampTickField.setAccessible(true);
                    stampConfigField.setAccessible(true);
                }
                return new StampInfo(stampReusableField.getBoolean(stamp), stampCloneField.get(stamp),
                        stampTickField.getLong(stamp), stampConfigField.getLong(stamp));
            }
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static synchronized void resetBeta9Controller() {
        try {
            if (!resetReflectionReady) {
                Class<?> owner = Beta9Optimizer.class;
                beta9AdaptiveField = privateField(owner, "adaptiveFactor");
                beta9ReportMinField = privateField(owner, "reportMinAdaptive");
                beta9ReportMaxField = privateField(owner, "reportMaxAdaptive");
                beta9PressureField = privateField(owner, "pressureWindows");
                beta9HealthyField = privateField(owner, "healthyWindows");
                beta9CtrlAcousticField = privateField(owner, "ctrlAcousticNs");
                beta9CtrlSprField = privateField(owner, "ctrlSprNs");
                beta9CtrlQueueField = privateField(owner, "ctrlQueueNs");
                beta9CtrlQueueMaxField = privateField(owner, "ctrlQueueMaxNs");
                beta9CtrlQueueSamplesField = privateField(owner, "ctrlQueueSamples");
                beta9CtrlSprCallsField = privateField(owner, "ctrlSprCalls");
                beta9LastAcousticField = privateField(owner, "lastAcousticMsPerSec");
                beta9LastSprField = privateField(owner, "lastSprMsPerSec");
                beta9LastQueueAvgField = privateField(owner, "lastQueueAvgMs");
                beta9LastQueueMaxField = privateField(owner, "lastQueueMaxMs");
                beta9ControlStartField = privateField(owner, "controlStartNs");
                resetReflectionReady = true;
            }
            beta9AdaptiveField.setDouble(null, 1.0D);
            beta9ReportMinField.setDouble(null, 1.0D);
            beta9ReportMaxField.setDouble(null, 1.0D);
            beta9PressureField.setInt(null, 0);
            beta9HealthyField.setInt(null, 0);
            beta9CtrlAcousticField.setLong(null, 0L);
            beta9CtrlSprField.setLong(null, 0L);
            beta9CtrlQueueField.setLong(null, 0L);
            beta9CtrlQueueMaxField.setLong(null, 0L);
            beta9CtrlQueueSamplesField.setLong(null, 0L);
            beta9CtrlSprCallsField.setLong(null, 0L);
            beta9LastAcousticField.setDouble(null, 0.0D);
            beta9LastSprField.setDouble(null, 0.0D);
            beta9LastQueueAvgField.setDouble(null, 0.0D);
            beta9LastQueueMaxField.setDouble(null, 0.0D);
            beta9ControlStartField.setLong(null, System.nanoTime());
        } catch (Throwable ignored) {
        }
    }

    @SuppressWarnings("unused")
    private static Field privateField(Class<?> owner, String name) throws Exception {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static boolean debugAllowsRayCache() {
        long now = System.nanoTime();
        synchronized (Beta10Optimizer.class) {
            if (debugReflectionFailed) return false;
            if (now - debugLastReadNs < DEBUG_REFRESH_NS) return debugAllowsCache;
            debugLastReadNs = now;
            try {
                if (sprConfig == null) {
                    Class<?> mod = Class.forName("com.sonicether.soundphysics.SoundPhysicsMod");
                    Field configField = mod.getField("CONFIG");
                    sprConfig = configField.get(null);
                    Class<?> configClass = sprConfig.getClass();
                    renderOcclusionField = configClass.getField("renderOcclusion");
                    occlusionLoggingField = configClass.getField("occlusionLogging");
                }
                Object render = renderOcclusionField.get(sprConfig);
                Object logging = occlusionLoggingField.get(sprConfig);
                if (configEntryGet == null || !configEntryGet.getDeclaringClass().isInstance(render)) {
                    configEntryGet = render.getClass().getMethod("get");
                }
                boolean renderEnabled = Boolean.TRUE.equals(configEntryGet.invoke(render));
                Method loggingGet = logging.getClass() == render.getClass() ? configEntryGet : logging.getClass().getMethod("get");
                boolean loggingEnabled = Boolean.TRUE.equals(loggingGet.invoke(logging));
                debugAllowsCache = !renderEnabled && !loggingEnabled;
            } catch (Throwable ignored) {
                debugReflectionFailed = true;
                debugAllowsCache = false;
            }
            return debugAllowsCache;
        }
    }

    private static synchronized void maybeReport() {
        maybeReportLocked(System.nanoTime());
    }

    private static void maybeReportLocked(long now) {
        long elapsed = now - reportStartNs;
        if (elapsed < ExtendedClientConfig.performanceReportNs()) return;
        if (!ClientConfig.diagnosticsEnabled()) {
            reportStartNs = now;
            rayHits = rayMisses = rayActualNs = 0L;
            directRayHits = directRayMisses = sprRayHits = sprRayMisses = directToSprHits = 0L;
            filterWrites = filterSkips = sourceWrites = sourceSkips = controllerIdleResets = 0L;
            maxActiveSources = activeSources.size();
            return;
        }
        double seconds = Math.max(0.001D, elapsed / 1.0E9D);
        long total = rayHits + rayMisses;
        double hitRate = total == 0L ? 0.0D : rayHits * 100.0D / total;
        String report = String.format(Locale.ROOT,
                "[CC:HQ Sound Physics Compat] beta11 direct-ray window=%.1fs active=%d eligible=%d maxActive=%d rayHit=%d (%.1f/s) rayMiss=%d (%.1f/s) hitRate=%.1f%% actualRay=%.1fms/s direct=%d/%d spr=%d/%d directToSpr=%d filterWrite=%d filterSkip=%d sourceWrite=%d sourceSkip=%d idleResets=%d",
                seconds, activeSources.size(), Math.max(0, activeSources.size() - inaudibleSources.size()), maxActiveSources,
                rayHits, rayHits / seconds, rayMisses, rayMisses / seconds, hitRate,
                (rayActualNs / 1_000_000.0D) / seconds, directRayHits, directRayMisses, sprRayHits, sprRayMisses,
                directToSprHits, filterWrites, filterSkips, sourceWrites, sourceSkips, controllerIdleResets);
        SoundPhysicsBridge.beta9Log(report);
        reportStartNs = now;
        rayHits = rayMisses = rayActualNs = 0L;
        directRayHits = directRayMisses = sprRayHits = sprRayMisses = directToSprHits = 0L;
        filterWrites = filterSkips = sourceWrites = sourceSkips = controllerIdleResets = 0L;
        maxActiveSources = activeSources.size();
    }

    static synchronized long[] beta10StatsForTest() {
        return new long[]{rayHits, rayMisses, directToSprHits, filterWrites, filterSkips,
                sourceWrites, sourceSkips, activeSources.size(), inaudibleSources.size()};
    }

    public static boolean beta11RoomCacheActive() {
        Context context = CONTEXT.get();
        if (context != null && context.owner == OWNER_SPR) return context.cacheable;
        return false;
    }

    private static final class FilterState {
        boolean gainKnown;
        boolean hfKnown;
        int gainBits;
        int hfBits;
    }

    private static final class SourceAlState {
        boolean gainKnown;
        boolean posKnown;
        int gainParam;
        int gainBits;
        int posParam;
        int xBits;
        int yBits;
        int zBits;
    }

    private record StampInfo(boolean reusable, Object cloneIdentity, long cloneTick, long configFingerprint) {}
    private record Context(int sourceId, byte owner, Object stamp, StampInfo stampInfo, boolean cacheable, Context previous) {}
}
