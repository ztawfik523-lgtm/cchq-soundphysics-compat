package dev.cchqphysics.compat.audio;

/**
 * Runtime-only A/B control for diagnosing reflected-position coloration.
 *
 * <p>The default is deliberately {@code true}, which is the known-good
 * Phase-5/Hotfix3 behavior. This is not persisted to config: every launch
 * starts with reflection redirection enabled.</p>
 */
final class ReflectionDiagnostics {
    private static volatile boolean redirectEnabled = true;

    private ReflectionDiagnostics() {}

    static boolean redirectEnabled() {
        return redirectEnabled;
    }

    static void setRedirectEnabled(boolean enabled) {
        redirectEnabled = enabled;
        SoundPhysicsBridge.beta9Log("[phase5/issue-a] reflectionRedirect=" + enabled);
    }

    static String status() {
        return "reflectionRedirect=" + redirectEnabled;
    }
}
