package dev.cchqphysics.compat.audio;

import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Sound-thread-owned capture layer used to turn one SPR evaluation into reusable acoustic state. */
public final class AcousticCapture {
    private static final Logger LOGGER = LoggerFactory.getLogger("CC:HQ Sound Physics Compat");
    private static final Map<Integer, Registration> REGISTRATIONS = new HashMap<>();
    private static final ArrayDeque<Context> CAPTURE_STACK = new ArrayDeque<>();
    private static long nextGeneration = 1L;
    private static Thread ownerThread;
    private static int nativeFallbackSource = Integer.MIN_VALUE;
    private static boolean wrongThreadLogged;
    private static boolean identityMismatchLogged;
    private static boolean stackMismatchLogged;

    private AcousticCapture() {}

    public static synchronized void register(int sourceId) {
        Thread current = Thread.currentThread();
        if (ownerThread == null) ownerThread = current;
        REGISTRATIONS.put(sourceId, new Registration(nextGeneration++, null));
        CAPTURE_STACK.removeIf(c -> c.sourceId == sourceId);
        if (nativeFallbackSource == sourceId) nativeFallbackSource = Integer.MIN_VALUE;
    }

    public static synchronized void unregister(int sourceId) {
        REGISTRATIONS.remove(sourceId);
        CAPTURE_STACK.removeIf(c -> c.sourceId == sourceId);
        if (nativeFallbackSource == sourceId) nativeFallbackSource = Integer.MIN_VALUE;
    }

    public static synchronized void clear() {
        REGISTRATIONS.clear();
        CAPTURE_STACK.clear();
        nativeFallbackSource = Integer.MIN_VALUE;
        ownerThread = null;
        wrongThreadLogged = false;
        identityMismatchLogged = false;
        stackMismatchLogged = false;
    }

    public static synchronized boolean bindIdentity(int sourceId, UUID sourceUuid) {
        Registration registration = REGISTRATIONS.get(sourceId);
        if (registration == null) return false;
        if (registration.sourceUuid == null) {
            registration.sourceUuid = sourceUuid;
            return true;
        }
        boolean matches = registration.sourceUuid.equals(sourceUuid);
        if (!matches && !identityMismatchLogged) {
            identityMismatchLogged = true;
            LOGGER.warn("acoustic capture identity mismatch for OpenAL source {}; capture disabled for safety", sourceId);
        }
        return matches;
    }

    public static synchronized boolean begin(int sourceId, UUID sourceUuid) {
        if (!onOwnerThread()) return false;
        Registration registration = REGISTRATIONS.get(sourceId);
        if (registration == null) return false;
        if (registration.sourceUuid == null || !registration.sourceUuid.equals(sourceUuid)) return false;
        CAPTURE_STACK.push(new Context(sourceId, registration.generation, sourceUuid));
        return true;
    }

    public static synchronized Result end(int sourceId, UUID sourceUuid) {
        if (CAPTURE_STACK.isEmpty()) return null;
        if (!onOwnerThread()) return null;
        Context context = CAPTURE_STACK.peek();
        if (!context.matches(sourceId, sourceUuid) || !generationStillCurrent(context)) {
            if (!stackMismatchLogged) {
                stackMismatchLogged = true;
                LOGGER.warn("acoustic capture stack mismatch; dropping captured state safely");
            }
            CAPTURE_STACK.removeIf(c -> c.matches(sourceId, sourceUuid));
            return null;
        }
        CAPTURE_STACK.pop();
        return context.toResult();
    }

    public static synchronized boolean captureEnvironment(int sourceId,
                                                           float r0, float r1, float r2, float r3,
                                                           float h0, float h1, float h2, float h3,
                                                           float directCutoff, float directGain) {
        if (CAPTURE_STACK.isEmpty()) return false;
        if (!onOwnerThread()) return false;
        Context context = CAPTURE_STACK.peek();
        if (context.sourceId != sourceId || !generationStillCurrent(context)) return false;
        context.r0 = r0; context.r1 = r1; context.r2 = r2; context.r3 = r3;
        context.h0 = h0; context.h1 = h1; context.h2 = h2; context.h3 = h3;
        context.directCutoff = directCutoff;
        context.directGain = directGain;
        context.environmentWrites++;
        return true;
    }

    public static synchronized boolean captureSoundPos(int sourceId, Vec3 position) {
        if (CAPTURE_STACK.isEmpty()) return false;
        if (!onOwnerThread()) return false;
        Context context = CAPTURE_STACK.peek();
        if (context.sourceId != sourceId || !generationStillCurrent(context)) return false;
        context.reflectedWrite = position;
        context.positionWrites++;
        return true;
    }

    public static synchronized boolean beginNativeEnvironmentFallback(int sourceId) {
        if (!onOwnerThread()) return false;
        if (nativeFallbackSource != Integer.MIN_VALUE) return false;
        nativeFallbackSource = sourceId;
        return true;
    }

    public static synchronized void endNativeEnvironmentFallback(int sourceId) {
        if (nativeFallbackSource == sourceId) nativeFallbackSource = Integer.MIN_VALUE;
    }

    public static synchronized boolean shouldBypassEnvironment(int sourceId) {
        return nativeFallbackSource == sourceId && onOwnerThread();
    }

    public static synchronized long currentGeneration(int sourceId) {
        Registration registration = REGISTRATIONS.get(sourceId);
        return registration == null ? -1L : registration.generation;
    }

    private static boolean generationStillCurrent(Context context) {
        Registration registration = REGISTRATIONS.get(context.sourceId);
        return registration != null
                && registration.generation == context.generation
                && registration.sourceUuid != null
                && registration.sourceUuid.equals(context.sourceUuid);
    }

    private static boolean onOwnerThread() {
        Thread current = Thread.currentThread();
        if (ownerThread == null) {
            ownerThread = current;
            return true;
        }
        if (ownerThread == current) return true;
        if (!wrongThreadLogged) {
            wrongThreadLogged = true;
            LOGGER.warn("acoustic capture was reached from a non-owner thread; falling back instead of capturing");
        }
        return false;
    }

    private static final class Registration {
        final long generation;
        UUID sourceUuid;
        Registration(long generation, UUID sourceUuid) {
            this.generation = generation;
            this.sourceUuid = sourceUuid;
        }
    }

    private static final class Context {
        final int sourceId;
        final long generation;
        final UUID sourceUuid;
        float r0, r1, r2, r3;
        float h0, h1, h2, h3;
        float directCutoff, directGain;
        Vec3 reflectedWrite;
        int environmentWrites, positionWrites;

        Context(int sourceId, long generation, UUID sourceUuid) {
            this.sourceId = sourceId;
            this.generation = generation;
            this.sourceUuid = sourceUuid;
        }

        boolean matches(int sourceId, UUID sourceUuid) {
            return this.sourceId == sourceId && this.sourceUuid.equals(sourceUuid);
        }

        Result toResult() {
            return new Result(sourceId, generation, sourceUuid, environmentWrites > 0,
                    r0, r1, r2, r3, h0, h1, h2, h3, directCutoff, directGain,
                    reflectedWrite, environmentWrites, positionWrites);
        }
    }

    public record Result(int sourceId, long generation, UUID sourceUuid, boolean environmentCaptured,
                         float r0, float r1, float r2, float r3,
                         float h0, float h1, float h2, float h3,
                         float directCutoff, float directGain,
                         Vec3 reflectedWrite, int environmentWrites, int positionWrites) {}
}
