package dev.cchqphysics.compat.audio;

import dev.cchqphysics.compat.config.ClientConfig;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.openal.AL10;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

final class PositionStabilizer {
    private static final Logger LOGGER = LoggerFactory.getLogger("CC:HQ Sound Physics Compat");
    private static final Map<Integer, State> STATES = new HashMap<>();
    private static final int AL_POSITION = 4100;
    private static final double REDIRECT_OCCLUSION_THRESHOLD = 0.45D;
    private static final double REFLECTED_BLEND = 0.35D;
    private static final double MAX_REDIRECT_OFFSET = 2.5D;
    private static final double REDIRECT_ALPHA = 0.22D;
    private static final double CLEAR_ALPHA = 0.28D;
    private static final double FLIP_TO_CENTER_ALPHA = 0.35D;

    private PositionStabilizer() {}

    static synchronized void register(int sourceId) {
        STATES.put(sourceId, new State());
    }

    static synchronized void unregister(int sourceId) {
        STATES.remove(sourceId);
        ReflectionDiagnostics.clearSourceOverride(sourceId);
    }

    static synchronized void clear() {
        STATES.clear();
        ReflectionDiagnostics.clearAllSourceOverrides();
    }

    static void updateAndApply(int sourceId, double sourceX, double sourceY, double sourceZ, Vec3 reflected, double occlusion) {
        final State state;
        synchronized (PositionStabilizer.class) {
            state = STATES.computeIfAbsent(sourceId, ignored -> new State());
        }

        double targetX = sourceX;
        double targetY = sourceY;
        double targetZ = sourceZ;
        boolean requestedRedirect = reflected != null && occlusion >= ClientConfig.reflectionThreshold();
        boolean redirect = ReflectionDiagnostics.redirectEnabled(sourceId) && requestedRedirect;
        if (redirect) {
            double dx = (reflected.x - sourceX) * ClientConfig.reflectionBlend();
            double dy = (reflected.y - sourceY) * ClientConfig.reflectionBlend();
            double dz = (reflected.z - sourceZ) * ClientConfig.reflectionBlend();
            double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (length > ClientConfig.maxReflectionOffset() && length > 1.0E-6D) {
                double scale = ClientConfig.maxReflectionOffset() / length;
                dx *= scale;
                dy *= scale;
                dz *= scale;
            }
            targetX += dx;
            targetY += dy;
            targetZ += dz;
        }

        synchronized (PositionStabilizer.class) {
            state.sourceX = sourceX;
            state.sourceY = sourceY;
            state.sourceZ = sourceZ;
            state.occlusion = occlusion;
            state.requestedRedirect = requestedRedirect;
            state.redirectActive = redirect;
            state.haveReflected = reflected != null;
            if (reflected != null) {
                state.reflectedX = reflected.x;
                state.reflectedY = reflected.y;
                state.reflectedZ = reflected.z;
            }

            if (!state.initialized) {
                state.x = sourceX;
                state.y = sourceY;
                state.z = sourceZ;
                state.initialized = true;
            }

            double oldDx = state.x - sourceX;
            double oldDy = state.y - sourceY;
            double oldDz = state.z - sourceZ;
            double newDx = targetX - sourceX;
            double newDy = targetY - sourceY;
            double newDz = targetZ - sourceZ;
            double oldSq = oldDx * oldDx + oldDy * oldDy + oldDz * oldDz;
            double newSq = newDx * newDx + newDy * newDy + newDz * newDz;
            double dot = oldDx * newDx + oldDy * newDy + oldDz * newDz;

            final double alpha;
            if (!redirect) {
                alpha = ClientConfig.clearPositionAlpha();
            } else if (oldSq > 0.04D && newSq > 0.04D && dot < 0.0D) {
                targetX = sourceX;
                targetY = sourceY;
                targetZ = sourceZ;
                alpha = ClientConfig.flipToCenterAlpha();
            } else {
                alpha = ClientConfig.redirectAlpha();
            }

            state.x += (targetX - state.x) * alpha;
            state.y += (targetY - state.y) * alpha;
            state.z += (targetZ - state.z) * alpha;

            long now = System.nanoTime();
            double offset = Math.sqrt(
                    (state.x - sourceX) * (state.x - sourceX)
                            + (state.y - sourceY) * (state.y - sourceY)
                            + (state.z - sourceZ) * (state.z - sourceZ));
            if (now - state.lastLogNs > 2_000_000_000L
                    && (offset > 0.15D || occlusion >= ClientConfig.reflectionThreshold())) {
                LOGGER.debug("beta2 position source={} occlusion={} reflected={} offset={}",
                        sourceId, round2(occlusion), redirect, round2(offset));
                state.lastLogNs = now;
            }
        }
        applyCurrent(sourceId, state);
    }

    static void reapply(int sourceId, double sourceX, double sourceY, double sourceZ) {
        final State state;
        synchronized (PositionStabilizer.class) {
            state = STATES.get(sourceId);
            if (state == null || !state.initialized) {
                try {
                    AL10.alSource3f(sourceId, AL_POSITION, (float) sourceX, (float) sourceY, (float) sourceZ);
                } catch (Throwable ignored) {}
                return;
            }
            state.sourceX = sourceX;
            state.sourceY = sourceY;
            state.sourceZ = sourceZ;
        }
        applyCurrent(sourceId, state);
    }

    static void releaseToOriginal(int sourceId, double sourceX, double sourceY, double sourceZ) {
        final State state;
        synchronized (PositionStabilizer.class) {
            state = STATES.computeIfAbsent(sourceId, ignored -> new State());
            state.sourceX = sourceX;
            state.sourceY = sourceY;
            state.sourceZ = sourceZ;
            state.requestedRedirect = false;
            state.redirectActive = false;
            if (!state.initialized) {
                state.x = sourceX;
                state.y = sourceY;
                state.z = sourceZ;
                state.initialized = true;
            } else {
                state.x += (sourceX - state.x) * ClientConfig.clearPositionAlpha();
                state.y += (sourceY - state.y) * ClientConfig.clearPositionAlpha();
                state.z += (sourceZ - state.z) * ClientConfig.clearPositionAlpha();
            }
        }
        applyCurrent(sourceId, state);
    }

    static synchronized void debugDumpAll() {
        for (Integer sourceId : STATES.keySet()) {
            SoundPhysicsBridge.beta9Log("[phase5/issue-a/position] " + debugSnapshot(sourceId));
        }
    }

    static synchronized String debugSnapshot(int sourceId) {
        State state = STATES.get(sourceId);
        if (state == null || !state.initialized) return "positionState=missing " + ReflectionDiagnostics.sourceStatus(sourceId);
        double dx = state.x - state.sourceX;
        double dy = state.y - state.sourceY;
        double dz = state.z - state.sourceZ;
        double offset = Math.sqrt(dx * dx + dy * dy + dz * dz);
        String reflected = state.haveReflected
                ? round2(state.reflectedX) + "," + round2(state.reflectedY) + "," + round2(state.reflectedZ)
                : "none";
        return "real=" + round2(state.sourceX) + "," + round2(state.sourceY) + "," + round2(state.sourceZ)
                + " applied=" + round2(state.x) + "," + round2(state.y) + "," + round2(state.z)
                + " reflected=" + reflected
                + " offset=" + round2(offset)
                + " occlusion=" + round2(state.occlusion)
                + " requestedRedirect=" + state.requestedRedirect
                + " redirectActive=" + state.redirectActive
                + " " + ReflectionDiagnostics.sourceStatus(sourceId);
    }

    private static void applyCurrent(int sourceId, State state) {
        try {
            AL10.alSource3f(sourceId, AL_POSITION, (float) state.x, (float) state.y, (float) state.z);
        } catch (Throwable ignored) {}
    }

    private static String round2(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static final class State {
        boolean initialized;
        double x;
        double y;
        double z;
        double sourceX;
        double sourceY;
        double sourceZ;
        boolean haveReflected;
        double reflectedX;
        double reflectedY;
        double reflectedZ;
        double occlusion;
        boolean requestedRedirect;
        boolean redirectActive;
        long lastLogNs;
    }
}
