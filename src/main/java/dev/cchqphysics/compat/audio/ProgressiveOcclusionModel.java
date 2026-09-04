package dev.cchqphysics.compat.audio;

import com.sonicether.soundphysics.SoundPhysics;
import dev.cchqphysics.compat.config.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class ProgressiveOcclusionModel {
    private static final Logger LOGGER = LoggerFactory.getLogger("CC:HQ Sound Physics Compat");
    private static final Map<Integer, State> STATES = new HashMap<>();

    private static volatile long lastConfigReadNs;
    private static volatile float cachedBlockAbsorption = 1.0F;
    private static volatile float cachedMaxOcclusion = 16.0F;
    private static volatile boolean cachedStrictOcclusion;

    private static int applyOverrideSource = Integer.MIN_VALUE;
    private static float applyOverrideCutoff = 1.0F;
    private static float applyOverrideGain = 1.0F;

    private ProgressiveOcclusionModel() {}

    public static synchronized void register(int sourceId) {
        Beta10Optimizer.registerSource(sourceId);
        STATES.put(sourceId, new State());
    }

    public static synchronized void unregister(int sourceId) {
        Beta10Optimizer.unregisterSource(sourceId);
        STATES.remove(sourceId);
        if (applyOverrideSource == sourceId) applyOverrideSource = Integer.MIN_VALUE;
    }

    public static boolean independentDirectActive() {
        if (!ClientConfig.progressiveOcclusion()) return false;
        try {
            refreshConfig();
            return !cachedStrictOcclusion;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static synchronized boolean beginApplyOverride(int sourceId, float cutoff, float gain) {
        if (applyOverrideSource != Integer.MIN_VALUE) return false;
        applyOverrideSource = sourceId;
        applyOverrideCutoff = cutoff;
        applyOverrideGain = gain;
        return true;
    }

    public static synchronized void endApplyOverride(int sourceId) {
        if (applyOverrideSource == sourceId) applyOverrideSource = Integer.MIN_VALUE;
    }

    public static synchronized void updateSource(int sourceId, double x, double y, double z) {
        Beta9Optimizer.updateSource(sourceId, x, y, z);
        State state = STATES.get(sourceId);
        if (state == null) {
            state = new State();
            STATES.put(sourceId, state);
        }
        if (!state.hasSource || x != state.sourceX || y != state.sourceY || z != state.sourceZ) {
            state.ringsValid = false;
            state.sourceRingsValid = false;
        }
        state.sourceX = x;
        state.sourceY = y;
        state.sourceZ = z;
        state.hasSource = true;
    }

    public static synchronized double currentRawOcclusion(int sourceId) {
        State state = STATES.get(sourceId);
        return state != null && state.valid ? state.rawOcclusion : 0.0D;
    }

    public static synchronized double currentCenterOcclusion(int sourceId) {
        State state = STATES.get(sourceId);
        return state != null && state.valid ? state.centerOcclusion : Double.NaN;
    }

    public static synchronized long lastEvaluationNs(int sourceId) {
        State state = STATES.get(sourceId);
        return state != null && state.valid ? state.lastCalcNs : 0L;
    }

    public static double sampleCenterSentinel(int sourceId, Vec3 listener) throws Exception {
        Beta9Optimizer.beginSentinelTimer();
        final double x;
        final double y;
        final double z;
        synchronized (ProgressiveOcclusionModel.class) {
            State state = STATES.get(sourceId);
            if (state == null || !state.hasSource) {
                Beta9Optimizer.endSentinelTimer();
                return Double.NaN;
            }
            x = state.sourceX;
            y = state.sourceY;
            z = state.sourceZ;
        }
        PerformanceStats.recordSentinelPath();
        double value = SoundPhysics.runOcclusion(new Vec3(x, y, z), listener);
        Beta9Optimizer.endSentinelTimer();
        return value;
    }

    public static float[] forceAdjust(int sourceId, float directCutoff, float directGain) {
        Beta9Optimizer.invalidateDirect(sourceId);
        return adjustInternal(sourceId, directCutoff, directGain, true);
    }

    static float[] beta9AdjustReal(int sourceId, float directCutoff, float directGain) {
        return adjustInternal(sourceId, directCutoff, directGain, false);
    }

    private static float[] adjustInternal(int sourceId, float directCutoff, float directGain, boolean force) {
        synchronized (ProgressiveOcclusionModel.class) {
            if (applyOverrideSource == sourceId) return pair(applyOverrideCutoff, applyOverrideGain);
        }

        if (!ClientConfig.progressiveOcclusion()) {
            synchronized (ProgressiveOcclusionModel.class) {
                State state = STATES.get(sourceId);
                if (state != null) state.ringsValid = false;
            }
            return pair(directCutoff, directGain);
        }

        final State state;
        synchronized (ProgressiveOcclusionModel.class) {
            state = STATES.get(sourceId);
            if (state == null || !state.hasSource) return pair(directCutoff, directGain);
        }

        try {
            refreshConfig();
            if (cachedStrictOcclusion) {
                synchronized (ProgressiveOcclusionModel.class) {
                    state.ringsValid = false;
                }
                return pair(directCutoff, directGain);
            }

            Vec3 listener = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
            if (listener == null) return pair(directCutoff, directGain);
            long now = System.nanoTime();

            final boolean moved;
            synchronized (ProgressiveOcclusionModel.class) {
                moved = !state.hasListener || distanceSq(listener.x, listener.y, listener.z,
                        state.listenerX, state.listenerY, state.listenerZ) >= ClientConfig.occlusionMoveEpsilonSq();
                if (!force && state.valid && now - state.lastCalcNs < ClientConfig.occlusionMinIntervalNs()) {
                    return pair(state.cutoff, state.gain);
                }
                if (!force && !moved && state.valid
                        && now - state.lastCalcNs < ClientConfig.occlusionStationaryIntervalNs()) {
                    return pair(state.cutoff, state.gain);
                }
            }

            Vec3 source = new Vec3(state.sourceX, state.sourceY, state.sourceZ);
            double center = invokeRunOcclusion(source, listener);
            int paths = 1;
            double innerVariation = ClientConfig.innerVariation();
            double outerVariation = ClientConfig.outerVariation();
            ensureSourceRingPositions(state, source, innerVariation, outerVariation);

            boolean adaptive = ClientConfig.adaptiveProbeCache();
            final boolean fullRefresh;
            synchronized (ProgressiveOcclusionModel.class) {
                fullRefresh = !adaptive || !state.ringsValid
                        || Math.abs(center - state.centerAtLastFull) >= ClientConfig.probeCenterDelta()
                        || !state.hasFullListener
                        || distanceSq(listener.x, listener.y, listener.z,
                        state.fullListenerX, state.fullListenerY, state.fullListenerZ)
                        >= ClientConfig.probeFullRefreshDistanceSq();
            }

            double innerSum;
            double outerSum;
            if (fullRefresh) {
                innerSum = sampleRing(state.innerSourcePositions, listener, innerVariation);
                outerSum = sampleRing(state.outerSourcePositions, listener, outerVariation);
                paths += 16;
                synchronized (ProgressiveOcclusionModel.class) {
                    state.innerSum = innerSum;
                    state.outerSum = outerSum;
                    state.ringsValid = true;
                    state.refreshInnerNext = true;
                    state.centerAtLastFull = center;
                    state.fullListenerX = listener.x;
                    state.fullListenerY = listener.y;
                    state.fullListenerZ = listener.z;
                    state.hasFullListener = true;
                }
            } else {
                final boolean refreshInner;
                synchronized (ProgressiveOcclusionModel.class) {
                    refreshInner = state.refreshInnerNext;
                }
                if (refreshInner) {
                    innerSum = sampleRing(state.innerSourcePositions, listener, innerVariation);
                    paths += 8;
                    synchronized (ProgressiveOcclusionModel.class) {
                        state.innerSum = innerSum;
                        outerSum = state.outerSum;
                        state.refreshInnerNext = false;
                    }
                } else {
                    outerSum = sampleRing(state.outerSourcePositions, listener, outerVariation);
                    paths += 8;
                    synchronized (ProgressiveOcclusionModel.class) {
                        state.outerSum = outerSum;
                        innerSum = state.innerSum;
                        state.refreshInnerNext = true;
                    }
                }
            }

            PerformanceStats.recordProgressiveEvaluation(paths, fullRefresh);

            double c = clamp01(center);
            double smooth = c * c * (3.0D - 2.0D * c);
            double ringScale = ClientConfig.openCenterRingScale()
                    + ClientConfig.openCenterRingComplement() * smooth;
            double centerWeight = ClientConfig.centerWeight();
            double innerWeight = ClientConfig.innerWeight();
            double outerWeight = ClientConfig.outerWeight();
            double numerator = center * centerWeight
                    + innerSum * innerWeight * ringScale
                    + outerSum * outerWeight * ringScale;
            double denominator = centerWeight + 8.0D * innerWeight + 8.0D * outerWeight;
            double raw = numerator / denominator;
            double cutoffOcclusion = Math.min(cachedMaxOcclusion, raw * ClientConfig.cutoffOcclusionScale());
            double gainOcclusion = Math.min(cachedMaxOcclusion, raw * ClientConfig.gainOcclusionScale());
            double absorptionScale = Math.max(0.0D, cachedBlockAbsorption) * 3.0D;
            float cutoff = (float) Math.exp(-cutoffOcclusion * absorptionScale);
            float gain = (float) Math.exp(-gainOcclusion * Math.max(0.0D, cachedBlockAbsorption) * 0.3D);

            synchronized (ProgressiveOcclusionModel.class) {
                state.listenerX = listener.x;
                state.listenerY = listener.y;
                state.listenerZ = listener.z;
                state.hasListener = true;
                state.lastCalcNs = now;
                state.centerOcclusion = center;
                state.rawOcclusion = raw;
                state.cutoffOcclusion = cutoffOcclusion;
                state.gainOcclusion = gainOcclusion;
                state.cutoff = cutoff;
                state.gain = gain;
                state.valid = true;

                if (LOGGER.isDebugEnabled()
                        && (Double.isNaN(state.lastLoggedRaw)
                        || Math.abs(raw - state.lastLoggedRaw) >= 0.20D
                        || now - state.lastLogNs > 2_000_000_000L)) {
                    LOGGER.debug("beta9 progressive source={} center={} ringScale={} raw={} cutoffOcc={} gainOcc={} cutoff={} gain={} moved={} paths={} full={} weights=center:{},inner:{},outer:{}",
                            sourceId, round3(center), round3(ringScale), round3(raw), round3(cutoffOcclusion),
                            round3(gainOcclusion), round3(cutoff), round3(gain), moved, paths, fullRefresh,
                            centerWeight, innerWeight, outerWeight);
                    state.lastLoggedRaw = raw;
                    state.lastLogNs = now;
                }
            }
            return pair(cutoff, gain);
        } catch (Throwable throwable) {
            synchronized (ProgressiveOcclusionModel.class) {
                if (!state.failureLogged) {
                    state.failureLogged = true;
                    LOGGER.warn("beta9 progressive occlusion disabled for source {} after safe fallback", sourceId, throwable);
                }
            }
            return pair(directCutoff, directGain);
        }
    }

    private static void ensureSourceRingPositions(State state, Vec3 source, double innerVariation, double outerVariation) {
        synchronized (ProgressiveOcclusionModel.class) {
            if (state.sourceRingsValid
                    && Double.compare(state.cachedInnerVariation, innerVariation) == 0
                    && Double.compare(state.cachedOuterVariation, outerVariation) == 0) {
                return;
            }
            state.innerSourcePositions = makeRing(source, innerVariation);
            state.outerSourcePositions = makeRing(source, outerVariation);
            state.cachedInnerVariation = innerVariation;
            state.cachedOuterVariation = outerVariation;
            state.sourceRingsValid = true;
            state.ringsValid = false;
        }
    }

    private static Vec3[] makeRing(Vec3 center, double variation) {
        Vec3[] result = new Vec3[8];
        int index = 0;
        for (int x = -1; x <= 1; x += 2) {
            for (int y = -1; y <= 1; y += 2) {
                for (int z = -1; z <= 1; z += 2) {
                    result[index++] = center.add(x * variation, y * variation, z * variation);
                }
            }
        }
        return result;
    }

    private static double sampleRing(Vec3[] sourcePositions, Vec3 listener, double variation) throws Exception {
        double sum = 0.0D;
        int index = 0;
        for (int x = -1; x <= 1; x += 2) {
            for (int y = -1; y <= 1; y += 2) {
                for (int z = -1; z <= 1; z += 2) {
                    Vec3 listenerProbe = listener.add(x * variation, y * variation, z * variation);
                    sum += invokeRunOcclusion(sourcePositions[index++], listenerProbe);
                }
            }
        }
        return sum;
    }

    private static double invokeRunOcclusion(Vec3 from, Vec3 to) throws Exception {
        PerformanceStats.recordOcclusionPath();
        return Beta10Optimizer.runOcclusionDirect(from, to);
    }

    private static float[] pair(float cutoff, float gain) {
        return new float[]{cutoff, gain};
    }

    private static double clamp01(double value) {
        return Math.max(0.0D, Math.min(1.0D, value));
    }

    private static double distanceSq(double ax, double ay, double az, double bx, double by, double bz) {
        double dx = ax - bx;
        double dy = ay - by;
        double dz = az - bz;
        return dx * dx + dy * dy + dz * dz;
    }

    private static String round3(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }

    private static void refreshConfig() {
        long now = System.nanoTime();
        if (now - lastConfigReadNs < 1_000_000_000L) return;
        synchronized (ProgressiveOcclusionModel.class) {
            now = System.nanoTime();
            if (now - lastConfigReadNs < 1_000_000_000L) return;
            float blockAbsorption = 1.0F;
            float maxOcclusion = 16.0F;
            boolean strictOcclusion = false;
            try {
                Class<?> mod = Class.forName("com.sonicether.soundphysics.SoundPhysicsMod");
                Object config = mod.getField("CONFIG").get(null);
                blockAbsorption = readFloat(config, "blockAbsorption", blockAbsorption);
                maxOcclusion = readFloat(config, "maxOcclusion", maxOcclusion);
                strictOcclusion = readBoolean(config, "strictOcclusion", strictOcclusion);
            } catch (Throwable ignored) {}
            cachedBlockAbsorption = Math.max(0.0F, blockAbsorption);
            cachedMaxOcclusion = Math.max(0.1F, maxOcclusion);
            cachedStrictOcclusion = strictOcclusion;
            lastConfigReadNs = now;
        }
    }

    private static float readFloat(Object config, String name, float fallback) throws Exception {
        Object entry = config.getClass().getField(name).get(config);
        Object value = entry.getClass().getMethod("get").invoke(entry);
        return value instanceof Number number ? number.floatValue() : fallback;
    }

    private static boolean readBoolean(Object config, String name, boolean fallback) throws Exception {
        Object entry = config.getClass().getField(name).get(config);
        Object value = entry.getClass().getMethod("get").invoke(entry);
        return value instanceof Boolean bool ? bool : fallback;
    }

    public static float[] adjust(int sourceId, float directCutoff, float directGain) {
        return Beta10Optimizer.adjustDirect(sourceId, directCutoff, directGain);
    }

    static int beta9ApplyOverrideSource() {
        return applyOverrideSource;
    }

    static final class State {
        boolean hasSource;
        double sourceX;
        double sourceY;
        double sourceZ;
        boolean hasListener;
        double listenerX;
        double listenerY;
        double listenerZ;
        boolean valid;
        long lastCalcNs;
        double centerOcclusion;
        double rawOcclusion;
        double cutoffOcclusion;
        double gainOcclusion;
        float cutoff;
        float gain;
        long lastLogNs;
        double lastLoggedRaw = Double.NaN;
        boolean failureLogged;
        boolean sourceRingsValid;
        Vec3[] innerSourcePositions;
        Vec3[] outerSourcePositions;
        double cachedInnerVariation;
        double cachedOuterVariation;
        boolean ringsValid;
        double innerSum;
        double outerSum;
        boolean refreshInnerNext;
        double centerAtLastFull;
        boolean hasFullListener;
        double fullListenerX;
        double fullListenerY;
        double fullListenerZ;
    }
}
