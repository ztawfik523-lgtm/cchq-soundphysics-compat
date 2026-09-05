package dev.cchqphysics.compat.audio;

import dev.cchqphysics.compat.config.ClientConfig;
import dev.cchqphysics.compat.config.ExtendedClientConfig;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;
import org.lwjgl.openal.EXTEfx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Smooths captured SPR environments while isolating mutable EFX filters per OpenAL source. */
public final class EnvironmentSmoother {
    private static final Logger LOGGER = LoggerFactory.getLogger("CC:HQ Sound Physics Compat");
    private static final Map<Integer, State> STATES = new HashMap<>();
    private static final int AL_SOURCE_STATE = 4112;
    private static final int AL_PLAYING = 4114;
    private static final int AL_PAUSED = 4115;
    private static final int AL_DIRECT_FILTER = 131077;
    private static final int AL_AUXILIARY_SEND_FILTER = 131078;
    private static final int AL_AIR_ABSORPTION_FACTOR = 131079;
    private static final int AL_FILTER_TYPE = 32769;
    private static final int AL_FILTER_LOWPASS = 1;
    private static final int AL_LOWPASS_GAIN = 1;
    private static final int AL_LOWPASS_GAINHF = 2;
    private static final int AL_NO_ERROR = 0;
    private static final float MUFFLE_ALPHA = 0.30F;
    private static final float CLEAR_LOG_ALPHA = 0.18F;
    private static final float CLEAR_GAIN_LOG_ALPHA = 0.16F;
    private static final float REVERB_ALPHA = 0.22F;
    private static volatile long lastConfigReadNs;
    private static volatile float cachedAirAbsorption = 1.0F;

    private EnvironmentSmoother() {}

    public static synchronized void register(int sourceId) {
        STATES.put(sourceId, new State());
        ProgressiveOcclusionModel.register(sourceId);
        PositionStabilizer.register(sourceId);
        SoundPhysicsBridge.registerSource(sourceId);
    }

    public static synchronized void unregister(int sourceId) {
        State state = STATES.remove(sourceId);
        ProgressiveOcclusionModel.unregister(sourceId);
        PositionStabilizer.unregister(sourceId);
        SoundPhysicsBridge.unregisterSource(sourceId);
        SyncStartCoordinator.removeSource(sourceId);
        if (state != null) destroyPrivateEfx(sourceId, state);
    }

    public static boolean intercept(int sourceId,
                                    float r0, float r1, float r2, float r3,
                                    float h0, float h1, float h2, float h3,
                                    float directCutoff, float directGain) {
        final State state;
        synchronized (EnvironmentSmoother.class) {
            state = STATES.get(sourceId);
            if (state == null) return false;
        }
        if (!ExtendedClientConfig.privateEfxEnabled()) {
            if (state.privateEfxReady || state.directFilter != 0
                    || state.sendFilters[0] != 0 || state.sendFilters[1] != 0
                    || state.sendFilters[2] != 0 || state.sendFilters[3] != 0) {
                destroyPrivateEfx(sourceId, state);
            }
            // Turning the feature off is also an explicit retry boundary: if it
            // is enabled again later, allow a fresh isolated-EFX setup attempt.
            state.privateEfxFailed = false;
            state.failureLogged = false;
            DebugDiagnostics.efx("source={} private EFX disabled; detached compat filters and using native SPR fallback", sourceId);
            return false;
        }
        float[] adjusted = ProgressiveOcclusionModel.adjust(sourceId, directCutoff, directGain);
        adjusted = VerticalDiffractionRelief.adjust(sourceId, adjusted[0], adjusted[1]);
        float targetCutoff = SynchronizedSpectralBalancer.adjustDirectCutoff(sourceId, adjusted[0]);
        float targetGain = adjusted[1];
        long now = System.nanoTime();
        synchronized (EnvironmentSmoother.class) {
            if (!state.initialized) {
                state.r0 = r0; state.r1 = r1; state.r2 = r2; state.r3 = r3;
                state.h0 = h0; state.h1 = h1; state.h2 = h2; state.h3 = h3;
                state.cutoff = targetCutoff;
                state.gain = targetGain;
                state.initialized = true;
            } else {
                state.r0 = approach(state.r0, r0, ClientConfig.reverbAlpha());
                state.r1 = approach(state.r1, r1, ClientConfig.reverbAlpha());
                state.r2 = approach(state.r2, r2, ClientConfig.reverbAlpha());
                state.r3 = approach(state.r3, r3, ClientConfig.reverbAlpha());
                state.h0 = approach(state.h0, h0, ClientConfig.reverbAlpha());
                state.h1 = approach(state.h1, h1, ClientConfig.reverbAlpha());
                state.h2 = approach(state.h2, h2, ClientConfig.reverbAlpha());
                state.h3 = approach(state.h3, h3, ClientConfig.reverbAlpha());
                state.cutoff = targetCutoff < state.cutoff
                        ? approach(state.cutoff, targetCutoff, ClientConfig.muffleAlpha())
                        : approachLog(state.cutoff, targetCutoff, ClientConfig.clearCutoffAlpha());
                state.gain = targetGain < state.gain
                        ? approach(state.gain, targetGain, ClientConfig.muffleAlpha())
                        : approachLog(state.gain, targetGain, ClientConfig.clearGainAlpha());
            }
            if (Math.abs(targetCutoff - state.lastLoggedTargetCutoff) >= 0.15F || now - state.lastLogNs > 2_000_000_000L) {
                LOGGER.debug("beta1 env source={} nativeCutoff={} targetCutoff={} appliedCutoff={} nativeGain={} targetGain={} appliedGain={} isolated={}",
                        sourceId, round3(directCutoff), round3(targetCutoff), round3(state.cutoff),
                        round3(directGain), round3(targetGain), round3(state.gain), state.privateEfxReady);
                state.lastLoggedTargetCutoff = targetCutoff;
                state.lastLogNs = now;
            }
        }
        final int alState;
        try { alState = AL10.alGetSourcei(sourceId, AL_SOURCE_STATE); }
        catch (Throwable ignored) { return false; }
        if (alState != AL_PLAYING && alState != AL_PAUSED) return false;
        if (state.privateEfxFailed) return false;
        try {
            if (!state.privateEfxReady && !createPrivateEfx(sourceId, state)) return false;
            applyPrivateEfx(sourceId, state);
            return true;
        } catch (Throwable t) {
            failPrivateEfx(sourceId, state, t);
            return false;
        }
    }

    static synchronized void debugResetEfx() {
        for (Map.Entry<Integer, State> entry : STATES.entrySet()) {
            int sourceId = entry.getKey();
            State state = entry.getValue();
            destroyPrivateEfx(sourceId, state);
            state.privateEfxFailed = false;
            state.failureLogged = false;
        }
        DebugDiagnostics.efx("manual private EFX reset completed for {} tracked sources", STATES.size());
    }

    static synchronized void debugDumpEfx() {
        for (Map.Entry<Integer, State> entry : STATES.entrySet()) {
            State state = entry.getValue();
            SoundPhysicsBridge.beta9Log("[phase5/source-efx] source=" + entry.getKey()
                    + " initialized=" + state.initialized
                    + " ready=" + state.privateEfxReady
                    + " failed=" + state.privateEfxFailed
                    + " directFilter=" + state.directFilter
                    + " maxAux=" + state.maxAuxSends
                    + " cutoff=" + round3(state.cutoff)
                    + " gain=" + round3(state.gain));
        }
    }

    static synchronized String debugSummary() {
        int initialized = 0;
        int ready = 0;
        int failed = 0;
        for (State state : STATES.values()) {
            if (state.initialized) initialized++;
            if (state.privateEfxReady) ready++;
            if (state.privateEfxFailed) failed++;
        }
        return "envStates=" + STATES.size() + " initialized=" + initialized + " efxReady=" + ready + " efxFailed=" + failed;
    }

    private static boolean createPrivateEfx(int sourceId, State state) {
        drainAlErrors();
        try {
            SprLayout layout = readSprLayout();
            if (layout == null || layout.maxAuxSends <= 0) throw new IllegalStateException("SPR auxiliary EFX layout unavailable");
            state.maxAuxSends = Math.min(4, layout.maxAuxSends);
            System.arraycopy(layout.auxSlots, 0, state.auxSlots, 0, 4);
            state.directFilter = newLowpassFilter();
            for (int i = 0; i < 4; i++) state.sendFilters[i] = newLowpassFilter();
            int error = AL10.alGetError();
            if (error != AL_NO_ERROR) throw new IllegalStateException("OpenAL error creating isolated EFX: " + error);
            state.privateEfxReady = true;
            DebugDiagnostics.efx("source={} created private EFX directFilter={} maxAux={}", sourceId, state.directFilter, state.maxAuxSends);
            LOGGER.debug("beta1 isolated EFX source={} directFilter={} sends={}/{}/{}/{} maxAux={}",
                    sourceId, state.directFilter, state.sendFilters[0], state.sendFilters[1], state.sendFilters[2], state.sendFilters[3], state.maxAuxSends);
            return true;
        } catch (Throwable t) {
            failPrivateEfx(sourceId, state, t);
            return false;
        }
    }

    private static int newLowpassFilter() {
        int filter = Beta10Optimizer.alGenFilter();
        if (filter == 0) throw new IllegalStateException("alGenFilters returned 0");
        EXTEfx.alFilteri(filter, AL_FILTER_TYPE, AL_FILTER_LOWPASS);
        return filter;
    }

    private static void applyPrivateEfx(int sourceId, State state) {
        refreshAirAbsorption();
        drainAlErrors();
        float[] gains = {state.r0, state.r1, state.r2, state.r3};
        float[] cutoffs = {state.h0, state.h1, state.h2, state.h3};
        for (int i = 0; i < 4; i++) {
            int requiredSends = 4 - i;
            if (state.maxAuxSends < requiredSends) continue;
            int sendIndex = 3 - i;
            int filter = state.sendFilters[i];
            Beta10Optimizer.alFilterfMaybe(filter, AL_LOWPASS_GAIN, clamp01(gains[i]));
            Beta10Optimizer.alFilterfMaybe(filter, AL_LOWPASS_GAINHF, clamp01(cutoffs[i]));
            AL11.alSource3i(sourceId, AL_AUXILIARY_SEND_FILTER, state.auxSlots[i], sendIndex, filter);
        }
        Beta10Optimizer.alFilterfMaybe(state.directFilter, AL_LOWPASS_GAIN, clamp01(state.gain));
        Beta10Optimizer.alFilterfMaybe(state.directFilter, AL_LOWPASS_GAINHF, clamp01(state.cutoff));
        AL11.alSourcei(sourceId, AL_DIRECT_FILTER, state.directFilter);
        AL11.alSourcef(sourceId, AL_AIR_ABSORPTION_FACTOR, cachedAirAbsorption);
        int error = AL10.alGetError();
        if (error != AL_NO_ERROR) throw new IllegalStateException("OpenAL error applying isolated EFX: " + error);
        PerformanceStats.recordEfxApply(true);
    }

    private static void failPrivateEfx(int sourceId, State state, Throwable throwable) {
        synchronized (EnvironmentSmoother.class) {
            if (!state.failureLogged) {
                state.failureLogged = true;
                LOGGER.warn("beta1 isolated EFX failed for source {}; falling back to native SPR", sourceId, throwable);
            }
            state.privateEfxFailed = true;
            DebugDiagnostics.efx("source={} private EFX failed; native fallback reason={}", sourceId, throwable.toString());
        }
        destroyPrivateEfx(sourceId, state);
        drainAlErrors();
    }

    private static void destroyPrivateEfx(int sourceId, State state) {
        if (!state.privateEfxReady && state.directFilter == 0 && state.sendFilters[0] == 0 && state.sendFilters[1] == 0 && state.sendFilters[2] == 0 && state.sendFilters[3] == 0) return;
        try {
            drainAlErrors();
            AL11.alSourcei(sourceId, AL_DIRECT_FILTER, 0);
            for (int i = 0; i < 4; i++) {
                int requiredSends = 4 - i;
                if (state.maxAuxSends >= requiredSends) AL11.alSource3i(sourceId, AL_AUXILIARY_SEND_FILTER, 0, 3 - i, 0);
            }
        } catch (Throwable ignored) {}
        tryDeleteFilter(state.directFilter);
        for (int filter : state.sendFilters) tryDeleteFilter(filter);
        state.directFilter = 0;
        for (int i = 0; i < 4; i++) state.sendFilters[i] = 0;
        state.privateEfxReady = false;
        DebugDiagnostics.efx("source={} destroyed private EFX", sourceId);
        drainAlErrors();
    }

    private static void tryDeleteFilter(int filter) {
        if (filter == 0) return;
        try { Beta10Optimizer.alDeleteFilter(filter); } catch (Throwable ignored) {}
    }

    private static SprLayout readSprLayout() {
        try {
            Class<?> soundPhysics = Class.forName("com.sonicether.soundphysics.SoundPhysics");
            int[] slots = {readStaticInt(soundPhysics, "auxFXSlot0"), readStaticInt(soundPhysics, "auxFXSlot1"), readStaticInt(soundPhysics, "auxFXSlot2"), readStaticInt(soundPhysics, "auxFXSlot3")};
            int maxAuxSends = readStaticInt(soundPhysics, "maxAuxSends");
            if (maxAuxSends <= 0) return null;
            for (int i = 0; i < Math.min(4, maxAuxSends); i++) if (maxAuxSends >= 4 && slots[i] == 0) return null;
            return new SprLayout(slots, maxAuxSends);
        } catch (Throwable ignored) { return null; }
    }

    private static int readStaticInt(Class<?> owner, String name) throws Exception {
        try { return owner.getField(name).getInt(null); }
        catch (NoSuchFieldException publicMissing) {
            Field field = owner.getDeclaredField(name);
            if (!field.trySetAccessible()) throw publicMissing;
            return field.getInt(null);
        }
    }

    private static void refreshAirAbsorption() {
        long now = System.nanoTime();
        if (now - lastConfigReadNs < 1_000_000_000L) return;
        synchronized (EnvironmentSmoother.class) {
            now = System.nanoTime();
            if (now - lastConfigReadNs < 1_000_000_000L) return;
            float value = 1.0F;
            try {
                Class<?> mod = Class.forName("com.sonicether.soundphysics.SoundPhysicsMod");
                Object config = mod.getField("CONFIG").get(null);
                Object entry = config.getClass().getField("airAbsorption").get(config);
                Object result = entry.getClass().getMethod("get").invoke(entry);
                if (result instanceof Number number) value = number.floatValue();
            } catch (Throwable ignored) {}
            cachedAirAbsorption = Math.max(0.0F, Math.min(10.0F, value));
            lastConfigReadNs = now;
        }
    }

    private static void drainAlErrors() {
        try { for (int i = 0; i < 8 && AL10.alGetError() != AL_NO_ERROR; i++) { } }
        catch (Throwable ignored) {}
    }

    private static float approach(float current, float target, float alpha) {
        if (Math.abs(target - current) < 5.0E-4F) return target;
        return current + (target - current) * alpha;
    }

    private static float approachLog(float current, float target, float alpha) {
        if (Math.abs(target - current) < 5.0E-4F) return target;
        double a = Math.log(Math.max(1.0E-6F, current));
        double b = Math.log(Math.max(1.0E-6F, target));
        return (float) Math.exp(a + (b - a) * alpha);
    }

    private static float clamp01(float value) { return Math.max(0.0F, Math.min(1.0F, value)); }
    private static String round3(float value) { return String.format(Locale.ROOT, "%.3f", value); }

    private static final class State {
        boolean initialized;
        float r0, r1, r2, r3, h0, h1, h2, h3;
        float cutoff = 1.0F;
        float gain = 1.0F;
        long lastLogNs;
        float lastLoggedTargetCutoff = Float.NaN;
        boolean failureLogged, privateEfxReady, privateEfxFailed;
        int directFilter;
        final int[] sendFilters = new int[4];
        final int[] auxSlots = new int[4];
        int maxAuxSends;
    }

    private record SprLayout(int[] auxSlots, int maxAuxSends) {}
}
