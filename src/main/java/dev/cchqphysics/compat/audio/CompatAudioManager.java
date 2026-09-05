package dev.cchqphysics.compat.audio;

import dev.cchqphysics.compat.config.ClientConfig;
import dev.cchqphysics.compat.mixin.SoundEngineAccessor;
import dev.cchqphysics.compat.mixin.SoundManagerAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundEngineExecutor;
import org.lwjgl.openal.AL10;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/** Client-owned bridge for CC:HQ whole-file positional playback. */
public final class CompatAudioManager {
    private static final ExecutorService DECODER = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "CC-HQ SoundPhysics decoder");
        t.setDaemon(true);
        return t;
    });
    private static final ConcurrentLinkedQueue<StartRequest> READY = new ConcurrentLinkedQueue<>();
    private static final ConcurrentHashMap<DecodeKey, DecodeEntry> DECODES = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, AtomicInteger> GENERATIONS = new ConcurrentHashMap<>();
    private static final Set<String> LOGGED = ConcurrentHashMap.newKeySet();
    private static final int MAX_DECODE_CACHE_ENTRIES = 4;

    private static final Map<UUID, ActiveSource> ACTIVE = new HashMap<>();
    private static final Map<DecodeKey, BufferRef> BUFFERS = new HashMap<>();

    private static long clientTicks;
    private static double lastListenerX = Double.NaN;
    private static double lastListenerY = Double.NaN;
    private static double lastListenerZ = Double.NaN;
    private static final AtomicInteger SESSION_EPOCH = new AtomicInteger();
    private static boolean hadLevel;

    private CompatAudioManager() {
    }

    public static boolean tryHandleAudioPayload(Object payload) {
        if (!ClientConfig.enabled()) return false;
        if (!HQPayloadView.isAudioPayload(payload) || !SoundPhysicsBridge.available()) return false;
        if (Minecraft.getInstance().level == null) return false;

        final HQPayloadView.Audio audio;
        try {
            audio = HQPayloadView.audio(payload);
        } catch (Throwable t) {
            logOnce("packet-shape", "CC:HQ packet layout did not match the expected 1.1.4 layout; falling back to CC:HQ: " + t);
            return false;
        }

        if (audio.format().endsWith("_STREAM") || "PCM_S16LE".equals(audio.format())) return false;
        if (!AudioDecoder.canDecode(audio.format(), audio.data())) {
            logOnce("decoder-" + audio.format(), "No compatible decoder for CC:HQ format " + audio.format() + "; that format will use normal CC:HQ playback.");
            return false;
        }

        int generation = GENERATIONS.computeIfAbsent(audio.source(), ignored -> new AtomicInteger()).incrementAndGet();
        int epoch = SESSION_EPOCH.get();
        DecodeKey key = makeDecodeKey(audio);
        DecodeEntry entry = DECODES.compute(key, (k, old) -> {
            if (old != null) {
                old.lastUsedTick = clientTicks;
                return old;
            }
            CompletableFuture<DecodedAudio> future = CompletableFuture.supplyAsync(() -> {
                try {
                    return AudioDecoder.decode(audio.format(), audio.data());
                } catch (Throwable t) {
                    throw new CompletionException(t);
                }
            }, DECODER);
            return new DecodeEntry(future, clientTicks);
        });

        entry.future.whenComplete((decoded, error) -> {
            if (error != null) {
                logOnce("decode-runtime-" + audio.format(), "Decoder accepted " + audio.format() + " but failed while decoding: " + rootMessage(error));
                return;
            }
            READY.add(new StartRequest(audio, key, decoded, generation, epoch));
        });
        return true;
    }

    public static void tryHandleStopPayload(Object payload) {
        if (!HQPayloadView.isStopPayload(payload)) return;
        try {
            UUID source = HQPayloadView.stopSource(payload);
            GENERATIONS.computeIfAbsent(source, ignored -> new AtomicInteger()).incrementAndGet();
            onSoundThread(() -> stopSource(source));
        } catch (Throwable t) {
            logOnce("stop-shape", "Could not read CC:HQ stop packet: " + t);
        }
    }

    public static void tickClient() {
        clientTicks++;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            if (hadLevel) {
                hadLevel = false;
                invalidateSession();
                onSoundThread(CompatAudioManager::stopAllSources);
            }
            return;
        }
        hadLevel = true;

        long gameTime = mc.level.getGameTime();
        ArrayList<StartRequest> deferred = new ArrayList<>();
        StartRequest request;
        while ((request = READY.poll()) != null) {
            if (request.epoch != SESSION_EPOCH.get()) continue;
            AtomicInteger generation = GENERATIONS.get(request.audio.source());
            if (generation == null || generation.get() != request.generation) continue;
            if (request.audio.startTick() > 0 && gameTime < request.audio.startTick()) {
                deferred.add(request);
                continue;
            }
            StartRequest start = request;
            onSoundThread(() -> startSource(start));
        }
        READY.addAll(deferred);

        if ((clientTicks % 200L) == 0L) {
            long cutoff = clientTicks - 600L;
            DECODES.entrySet().removeIf(e -> e.getValue().lastUsedTick < cutoff && e.getValue().future.isDone());
            List<Map.Entry<DecodeKey, DecodeEntry>> completed = DECODES.entrySet().stream()
                    .filter(e -> e.getValue().future.isDone())
                    .sorted(Comparator.comparingLong(e -> e.getValue().lastUsedTick))
                    .toList();
            int excess = completed.size() - MAX_DECODE_CACHE_ENTRIES;
            for (int i = 0; i < excess; i++) {
                Map.Entry<DecodeKey, DecodeEntry> oldest = completed.get(i);
                DECODES.remove(oldest.getKey(), oldest.getValue());
            }
        }

        if ((clientTicks % 2L) == 0L) {
            boolean moved = true;
            if (mc.player != null) {
                double x = mc.player.getX();
                double y = mc.player.getY();
                double z = mc.player.getZ();
                if (!Double.isNaN(lastListenerX)) {
                    double dx = x - lastListenerX;
                    double dy = y - lastListenerY;
                    double dz = z - lastListenerZ;
                    moved = dx * dx + dy * dy + dz * dz >= 0.25D;
                }
                lastListenerX = x;
                lastListenerY = y;
                lastListenerZ = z;
            }
            boolean periodic = (clientTicks % 2L) == 0L;
            if (moved || periodic) {
                onSoundThread(CompatAudioManager::maintainSources);
            } else {
                onSoundThread(CompatAudioManager::cleanupFinishedSources);
            }
        }
    }

    private static void startSource(StartRequest request) {
        BufferRef ref = null;
        int sourceId = 0;
        boolean compatRegistered = false;
        boolean installed = false;
        try {
            if (request.epoch != SESSION_EPOCH.get()) return;
            AtomicInteger generation = GENERATIONS.get(request.audio.source());
            if (generation == null || generation.get() != request.generation) return;

            stopSource(request.audio.source());
            clearAlErrors();

            ref = BUFFERS.get(request.key);
            if (ref == null) {
                int bufferId = AL10.alGenBuffers();
                ByteBuffer bytes = ByteBuffer.allocateDirect(request.decoded.monoPcm16Le().length).order(ByteOrder.nativeOrder());
                bytes.put(request.decoded.monoPcm16Le()).flip();
                AL10.alBufferData(bufferId, AL10.AL_FORMAT_MONO16, bytes, request.decoded.sampleRate());
                checkAl("upload audio buffer");
                ref = new BufferRef(bufferId);
                BUFFERS.put(request.key, ref);
            }
            ref.refs++;

            sourceId = AL10.alGenSources();
            compatRegistered = true;
            EnvironmentSmoother.register(sourceId);
            AL10.alSourcei(sourceId, AL10.AL_BUFFER, ref.bufferId);
            AL10.alSourcei(sourceId, AL10.AL_SOURCE_RELATIVE, AL10.AL_FALSE);
            AL10.alSourcei(sourceId, AL10.AL_LOOPING, AL10.AL_FALSE);
            AL10.alSourcef(sourceId, AL10.AL_PITCH, 1.0F);
            AL10.alSourcef(sourceId, AL10.AL_REFERENCE_DISTANCE, AttenuationBridge.referenceDistance(request.audio));
            AL10.alSourcef(sourceId, AL10.AL_MAX_DISTANCE, AttenuationBridge.maxDistance(request.audio));
            AL10.alSourcef(sourceId, AL10.AL_ROLLOFF_FACTOR, 0.0F);
            AL10.alSource3f(sourceId, AL10.AL_POSITION, request.audio.x(), request.audio.y(), request.audio.z());
            AL10.alSourcef(sourceId, AL10.AL_GAIN, effectiveGain(request.audio));
            checkAl("configure source");

            SoundPhysicsBridge.apply(sourceId, request.audio.source(), request.audio.x(), request.audio.y(), request.audio.z());
            SyncStartCoordinator.play(sourceId, request.audio);
            checkAl("start source");

            ACTIVE.put(request.audio.source(), new ActiveSource(sourceId, request.key, request.audio, request.generation));
            installed = true;
        } catch (Throwable t) {
            logOnce("openal-start", "Failed to start bridged OpenAL source: " + t);
        } finally {
            if (!installed) {
                if (compatRegistered) {
                    try { EnvironmentSmoother.unregister(sourceId); } catch (Throwable ignored) {}
                }
                if (sourceId != 0) {
                    try { AL10.alSourceStop(sourceId); } catch (Throwable ignored) {}
                    try { AL10.alSourcei(sourceId, AL10.AL_BUFFER, 0); } catch (Throwable ignored) {}
                    try { AL10.alDeleteSources(sourceId); } catch (Throwable ignored) {}
                }
                if (ref != null) releaseBuffer(request.key, ref);
            }
        }
    }

    private static void maintainSources() {
        Minecraft mc = Minecraft.getInstance();
        Iterator<Map.Entry<UUID, ActiveSource>> iterator = ACTIVE.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, ActiveSource> entry = iterator.next();
            ActiveSource active = entry.getValue();
            int state = SyncStartCoordinator.sourceState(active.sourceId, AL10.AL_SOURCE_STATE);
            if (state != AL10.AL_PLAYING && state != AL10.AL_PAUSED) {
                destroyActive(active);
                iterator.remove();
                continue;
            }

            float gain = effectiveGain(active.audio);
            if (mc.player != null) {
                double dx = mc.player.getX() - active.audio.x();
                double dy = mc.player.getY() - active.audio.y();
                double dz = mc.player.getZ() - active.audio.z();
                boolean outside = dx * dx + dy * dy + dz * dz
                        > AttenuationBridge.maxDistanceSquared(active.audio);
                Beta9Optimizer.updateDistance(active.sourceId,
                        dx * dx + dy * dy + dz * dz,
                        AttenuationBridge.maxDistanceSquared(active.audio));
                if (outside) {
                    gain = 0.0F;
                }
            }

            Beta10Optimizer.alSourcefStable(active.sourceId, AL10.AL_GAIN, gain);
            Beta10Optimizer.updateAudibility(active.sourceId, gain);
            if (gain > 0.0F) {
                AL10.alSource3f(active.sourceId, AL10.AL_POSITION, active.audio.x(), active.audio.y(), active.audio.z());
                SoundPhysicsBridge.apply(active.sourceId, active.audio.source(), active.audio.x(), active.audio.y(), active.audio.z());
            }
        }
    }

    private static void cleanupFinishedSources() {
        Iterator<Map.Entry<UUID, ActiveSource>> iterator = ACTIVE.entrySet().iterator();
        while (iterator.hasNext()) {
            ActiveSource active = iterator.next().getValue();
            int state = SyncStartCoordinator.sourceState(active.sourceId, AL10.AL_SOURCE_STATE);
            if (state != AL10.AL_PLAYING && state != AL10.AL_PAUSED) {
                destroyActive(active);
                iterator.remove();
            }
        }
    }

    private static float effectiveGain(HQPayloadView.Audio audio) {
        return DistanceBridge.effectiveGain(audio);
    }

    private static void stopSource(UUID source) {
        ActiveSource active = ACTIVE.remove(source);
        if (active != null) destroyActive(active);
    }

    private static void destroyActive(ActiveSource active) {
        EnvironmentSmoother.unregister(active.sourceId);
        try {
            AL10.alSourceStop(active.sourceId);
            AL10.alSourcei(active.sourceId, AL10.AL_BUFFER, 0);
            AL10.alDeleteSources(active.sourceId);
        } finally {
            BufferRef ref = BUFFERS.get(active.key);
            if (ref != null) releaseBuffer(active.key, ref);
        }
    }

    private static void releaseBuffer(DecodeKey key, BufferRef ref) {
        if (--ref.refs <= 0) {
            try { AL10.alDeleteBuffers(ref.bufferId); } catch (Throwable ignored) {}
            BUFFERS.remove(key);
        }
    }

    private static void stopAllSources() {
        for (ActiveSource active : new ArrayList<>(ACTIVE.values())) {
            try { destroyActive(active); } catch (Throwable t) {
                logOnce("stop-all", "Error while stopping compatibility source: " + t);
            }
        }
        ACTIVE.clear();
        for (BufferRef ref : new ArrayList<>(BUFFERS.values())) {
            try { AL10.alDeleteBuffers(ref.bufferId); } catch (Throwable ignored) {}
        }
        BUFFERS.clear();
        Beta11RoomRayCache.clear();
    }

    private static void invalidateSession() {
        SESSION_EPOCH.incrementAndGet();
        READY.clear();
        DECODES.clear();
        GENERATIONS.clear();
        SyncStartCoordinator.clear();
        SoundPhysicsBridge.clearSourceIds();
        lastListenerX = lastListenerY = lastListenerZ = Double.NaN;
    }

    public static void resetForSoundEngine() {
        invalidateSession();
        onSoundThread(CompatAudioManager::stopAllSources);
    }

    public static void pauseCompatSources() {
        onSoundThread(() -> {
            for (ActiveSource active : ACTIVE.values()) {
                if (AL10.alGetSourcei(active.sourceId, AL10.AL_SOURCE_STATE) == AL10.AL_PLAYING) {
                    AL10.alSourcePause(active.sourceId);
                }
            }
        });
    }

    public static void resumeCompatSources() {
        onSoundThread(() -> {
            for (ActiveSource active : ACTIVE.values()) {
                if (AL10.alGetSourcei(active.sourceId, AL10.AL_SOURCE_STATE) == AL10.AL_PAUSED) {
                    AL10.alSourcePlay(active.sourceId);
                }
            }
        });
    }

    public static void stopAllCompatSources() {
        invalidateSession();
        onSoundThread(CompatAudioManager::stopAllSources);
    }

    private static void clearAlErrors() {
        while (AL10.alGetError() != AL10.AL_NO_ERROR) { }
    }

    private static void onSoundThread(Runnable task) {
        try {
            Minecraft mc = Minecraft.getInstance();
            SoundEngine engine = ((SoundManagerAccessor) mc.getSoundManager()).cchqphysics$getSoundEngine();
            SoundEngineExecutor executor = ((SoundEngineAccessor) engine).cchqphysics$getExecutor();
            executor.execute(task);
        } catch (Throwable t) {
            logOnce("sound-thread", "Could not schedule work on Minecraft's sound thread: " + t);
        }
    }

    private static DecodeKey makeDecodeKey(HQPayloadView.Audio audio) {
        return new DecodeKey(audio.format() + ":" + shortHash(audio.data()));
    }

    private static String shortHash(byte[] data) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(data);
            return HexFormat.of().formatHex(hash, 0, 8);
        } catch (Exception e) {
            return Integer.toHexString(Arrays.hashCode(data));
        }
    }

    private static void checkAl(String where) {
        int error = AL10.alGetError();
        if (error != AL10.AL_NO_ERROR) {
            throw new IllegalStateException(where + " OpenAL error 0x" + Integer.toHexString(error));
        }
    }

    static void logOnce(String key, String message) {
        if (LOGGED.add(key)) System.err.println("[CC:HQ Sound Physics Compat] " + message);
    }

    private static String rootMessage(Throwable t) {
        while (t.getCause() != null) t = t.getCause();
        return t.toString();
    }

    private record DecodeKey(String value) { }

    private static final class DecodeEntry {
        final CompletableFuture<DecodedAudio> future;
        volatile long lastUsedTick;

        DecodeEntry(CompletableFuture<DecodedAudio> future, long lastUsedTick) {
            this.future = future;
            this.lastUsedTick = lastUsedTick;
        }
    }

    private static final class BufferRef {
        final int bufferId;
        int refs;

        BufferRef(int bufferId) {
            this.bufferId = bufferId;
        }
    }

    private record ActiveSource(int sourceId, DecodeKey key, HQPayloadView.Audio audio, int generation) { }
    private record StartRequest(HQPayloadView.Audio audio, DecodeKey key, DecodedAudio decoded, int generation, int epoch) { }

    static void beta10OnSoundThread(Runnable task) {
        onSoundThread(task);
    }
}
