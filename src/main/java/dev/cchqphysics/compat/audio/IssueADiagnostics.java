package dev.cchqphysics.compat.audio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Focused diagnostic snapshot for the Phase-5 reflected-position/coloration investigation. */
final class IssueADiagnostics {
    private static final Logger LOGGER = LoggerFactory.getLogger("CC:HQ Sound Physics Compat");

    private IssueADiagnostics() {}

    static void dump() {
        LOGGER.info("[phase5/issue-a] {}", ReflectionDiagnostics.status());
        SoundPhysicsBridge.debugDumpSources();
        PositionStabilizer.debugDumpAll();
        EnvironmentSmoother.debugDumpEfx();
    }
}
