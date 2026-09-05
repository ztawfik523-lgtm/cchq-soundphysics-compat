package dev.cchqphysics.compat.audio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.Locale;
import java.util.Map;

/** Focused diagnostic snapshot for the Phase-5 reflected-position/coloration investigation. */
final class IssueADiagnostics {
    private static final Logger LOGGER = LoggerFactory.getLogger("CC:HQ Sound Physics Compat");

    private IssueADiagnostics() {}

    static void dump() {
        LOGGER.info("[phase5/issue-a] {}", ReflectionDiagnostics.status());
        dumpBridgeState();
        dumpSmoothedEnvironment();
    }

    private static void dumpBridgeState() {
        try {
            Field statesField = SoundPhysicsBridge.class.getDeclaredField("STATES");
            statesField.setAccessible(true);
            synchronized (SoundPhysicsBridge.class) {
                Map<?, ?> states = (Map<?, ?>) statesField.get(null);
                for (Object state : states.values()) {
                    int sourceId = readInt(state, "sourceId");
                    Object roomObject = readObject(state, "room");
                    String roomSummary = "room=none";
                    if (roomObject instanceof AcousticCapture.Result room) {
                        roomSummary = "roomDirectCutoff=" + f(room.directCutoff())
                                + " roomDirectGain=" + f(room.directGain())
                                + " roomGain=[" + f(room.r0()) + "," + f(room.r1()) + "," + f(room.r2()) + "," + f(room.r3()) + "]"
                                + " roomHF=[" + f(room.h0()) + "," + f(room.h1()) + "," + f(room.h2()) + "," + f(room.h3()) + "]";
                    }
                    LOGGER.info("[phase5/issue-a/source] source={} {} {}",
                            sourceId, PositionStabilizer.debugSnapshot(sourceId), roomSummary);
                }
            }
        } catch (Throwable throwable) {
            LOGGER.warn("[phase5/issue-a] failed to dump source/reflection state", throwable);
        }
    }

    private static void dumpSmoothedEnvironment() {
        try {
            Field statesField = EnvironmentSmoother.class.getDeclaredField("STATES");
            statesField.setAccessible(true);
            synchronized (EnvironmentSmoother.class) {
                Map<?, ?> states = (Map<?, ?>) statesField.get(null);
                for (Map.Entry<?, ?> entry : states.entrySet()) {
                    int sourceId = ((Number) entry.getKey()).intValue();
                    Object state = entry.getValue();
                    LOGGER.info("[phase5/issue-a/efx] source={} efxReady={} cutoff={} gain={} smoothedGain=[{},{},{},{}] smoothedHF=[{},{},{},{}]",
                            sourceId,
                            readBoolean(state, "privateEfxReady"),
                            f(readFloat(state, "cutoff")),
                            f(readFloat(state, "gain")),
                            f(readFloat(state, "r0")), f(readFloat(state, "r1")), f(readFloat(state, "r2")), f(readFloat(state, "r3")),
                            f(readFloat(state, "h0")), f(readFloat(state, "h1")), f(readFloat(state, "h2")), f(readFloat(state, "h3")));
                }
            }
        } catch (Throwable throwable) {
            LOGGER.warn("[phase5/issue-a] failed to dump smoothed EFX state", throwable);
        }
    }

    private static Object readObject(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static int readInt(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.getInt(target);
    }

    private static float readFloat(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.getFloat(target);
    }

    private static boolean readBoolean(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.getBoolean(target);
    }

    private static String f(float value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }
}
