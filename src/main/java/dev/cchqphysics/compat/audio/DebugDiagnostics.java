package dev.cchqphysics.compat.audio;

import dev.cchqphysics.compat.config.ExtendedClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Targeted diagnostics that can be enabled without global DEBUG logging. */
final class DebugDiagnostics {
    private static final Logger LOGGER = LoggerFactory.getLogger("CC:HQ Sound Physics Compat");

    private DebugDiagnostics() {}

    static void source(String format, Object... args) {
        if (ExtendedClientConfig.logSourceLifecycle()) info("source", format, args);
    }

    static void room(String format, Object... args) {
        if (ExtendedClientConfig.logRoomScheduler()) info("room", format, args);
    }

    static void sentinel(String format, Object... args) {
        if (ExtendedClientConfig.logSentinel()) info("sentinel", format, args);
    }

    static void efx(String format, Object... args) {
        if (ExtendedClientConfig.logEfx()) info("efx", format, args);
    }

    static void cache(String format, Object... args) {
        if (ExtendedClientConfig.logCache()) info("cache", format, args);
    }

    static void sync(String format, Object... args) {
        if (ExtendedClientConfig.logSync()) info("sync", format, args);
    }

    static void transition(String format, Object... args) {
        if (ExtendedClientConfig.logTransitions()) info("transition", format, args);
    }

    private static void info(String category, String format, Object... args) {
        LOGGER.info("[{}] " + format, prepend(category, args));
    }

    private static Object[] prepend(String category, Object[] args) {
        Object[] result = new Object[args.length + 1];
        result[0] = category;
        System.arraycopy(args, 0, result, 1, args.length);
        return result;
    }
}
