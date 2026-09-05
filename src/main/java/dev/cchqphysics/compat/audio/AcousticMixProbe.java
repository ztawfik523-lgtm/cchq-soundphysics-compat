package dev.cchqphysics.compat.audio;

import java.util.HashMap;
import java.util.Map;

/** Runtime-only source-specific acoustic A/B overrides for Phase-5 diagnosis. */
final class AcousticMixProbe {
    enum Mode {
        AUTO("auto"),
        SENDS_OFF("sends_off"),
        HF_LIFT_25("hf_lift_25"),
        HF_LIFT_50("hf_lift_50"),
        HF_LIFT_75("hf_lift_75"),
        DIRECT_HF_BYPASS("direct_hf_bypass");

        private final String wireName;

        Mode(String wireName) {
            this.wireName = wireName;
        }

        String wireName() {
            return wireName;
        }

        float directHfLiftFraction() {
            return switch (this) {
                case HF_LIFT_25 -> 0.25F;
                case HF_LIFT_50 -> 0.50F;
                case HF_LIFT_75 -> 0.75F;
                case DIRECT_HF_BYPASS -> 1.00F;
                default -> 0.00F;
            };
        }
    }

    private static final Map<Integer, Mode> MODES = new HashMap<>();

    private AcousticMixProbe() {}

    static synchronized void setMode(int sourceId, Mode mode) {
        if (mode == null || mode == Mode.AUTO) {
            MODES.remove(sourceId);
            SoundPhysicsBridge.beta9Log("[phase5/acoustic-probe] source=" + sourceId + " mode=auto");
            return;
        }
        MODES.put(sourceId, mode);
        SoundPhysicsBridge.beta9Log("[phase5/acoustic-probe] source=" + sourceId + " mode=" + mode.wireName());
    }

    static synchronized void clearAll() {
        if (!MODES.isEmpty()) {
            SoundPhysicsBridge.beta9Log("[phase5/acoustic-probe] cleared all overrides count=" + MODES.size());
            MODES.clear();
        }
    }

    static synchronized Mode modeFor(int sourceId) {
        return MODES.getOrDefault(sourceId, Mode.AUTO);
    }

    static synchronized void clearSource(int sourceId) {
        MODES.remove(sourceId);
    }

    static synchronized String sourceStatus(int sourceId) {
        return "acousticProbe source=" + sourceId + " mode=" + modeFor(sourceId).wireName();
    }

    static synchronized String status() {
        return "acousticProbeOverrides=" + MODES.size();
    }
}
