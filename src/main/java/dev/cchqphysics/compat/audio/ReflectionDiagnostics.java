package dev.cchqphysics.compat.audio;

import java.util.HashMap;
import java.util.Map;

/**
 * Runtime-only A/B controls for diagnosing reflected-position coloration.
 *
 * <p>The global default is deliberately {@code true}, which is the known-good
 * Phase-5/Hotfix3 behavior. Nothing here is persisted: every launch starts with
 * reflection redirection enabled and with no per-source overrides.</p>
 *
 * <p>Per-source overrides exist only so a single member of a synchronized group
 * can be isolated while all other sources remain on the known-good path.</p>
 */
final class ReflectionDiagnostics {
    private static boolean globalRedirectEnabled = true;
    private static final Map<Integer, Boolean> SOURCE_OVERRIDES = new HashMap<>();

    private ReflectionDiagnostics() {}

    static synchronized boolean redirectEnabled(int sourceId) {
        Boolean override = SOURCE_OVERRIDES.get(sourceId);
        return override != null ? override : globalRedirectEnabled;
    }

    static synchronized void setGlobalRedirectEnabled(boolean enabled) {
        globalRedirectEnabled = enabled;
        SoundPhysicsBridge.beta9Log("[phase5/issue-a] reflectionRedirectGlobal=" + enabled
                + " sourceOverrides=" + SOURCE_OVERRIDES.size());
    }

    static synchronized void setSourceOverride(int sourceId, Boolean enabled) {
        if (enabled == null) {
            SOURCE_OVERRIDES.remove(sourceId);
            SoundPhysicsBridge.beta9Log("[phase5/issue-a] reflectionRedirect source=" + sourceId
                    + " override=auto effective=" + redirectEnabled(sourceId));
        } else {
            SOURCE_OVERRIDES.put(sourceId, enabled);
            SoundPhysicsBridge.beta9Log("[phase5/issue-a] reflectionRedirect source=" + sourceId
                    + " override=" + enabled + " effective=" + enabled);
        }
    }

    static synchronized void clearSourceOverride(int sourceId) {
        SOURCE_OVERRIDES.remove(sourceId);
    }

    static synchronized void clearAllSourceOverrides() {
        SOURCE_OVERRIDES.clear();
    }

    static synchronized String status() {
        return "reflectionRedirectGlobal=" + globalRedirectEnabled
                + " sourceOverrides=" + SOURCE_OVERRIDES.size();
    }

    static synchronized String sourceStatus(int sourceId) {
        Boolean override = SOURCE_OVERRIDES.get(sourceId);
        String overrideText = override == null ? "auto" : override.toString();
        return "source=" + sourceId
                + " reflectionOverride=" + overrideText
                + " reflectionEffective=" + redirectEnabled(sourceId);
    }
}
