package dev.cchqphysics.compat.audio;

import dev.cchqphysics.compat.config.ClientConfig;
import dev.cchqphysics.compat.mixin.SoundEngineAccessor;
import dev.cchqphysics.compat.mixin.SoundManagerAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundEngineExecutor;
import org.lwjgl.openal.AL10;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
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
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** Client-owned CC:HQ whole-file playback bridge reconstructed against Beta11 Hotfix3 bytecode. */
public final class CompatAudioManager {
    private static final int MAX_PENDING_DECODE_TASKS = 8;
    private static final ThreadPoolExecutor DECODER = new ThreadPoolExecutor(
            1, 1, 0L, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(MAX_PENDING_DECODE_TASKS),
            r -> {
                Thread t = new Thread(r, "CC-HQ SoundPhysics decoder");
                t.setDaemon(true);
                return t;
            },
            new ThreadPoolExecutor.AbortPolicy());
    private static final ConcurrentLinkedQueue<StartRequest> READY = new ConcurrentLinkedQueue<>();
    private static final ConcurrentHashMap<DecodeKey, DecodeEntry> DECODES = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, AtomicInteger> GENERATIONS = new ConcurrentHashMap<>();
    private static final Set<String> LOGGED = ConcurrentHashMap.newKeySet();
    private static final int MAX_DECODE_CACHE_ENTRIES = 4;
    private static final long MAX_DECODE_CACHE_BYTES = 128L * 1024L * 1024L;
    private static final byte[] EMPTY_AUDIO_DATA = new byte[0];

    private static final Map<UUID, ActiveSource> ACTIVE = new HashMap<>();
    private static final Map<DecodeKey, BufferRef> BUFFERS = new HashMap<>();

    private static long clientTicks;
    private static double lastListenerX = Double.NaN;
    private static double lastListenerY = Double.NaN;
    private static double lastListenerZ = Double.NaN;
    private static final AtomicInteger SESSION_EPOCH = new AtomicInteger();
    private static ClientLevel trackedLevel;

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

        if (audio.format().endsWith("_STREAM") || "PCM_S16LE".equals(audio.format())) {
            handoffToNative(audio.source());
            return false;
        }
        if (!AudioDecoder.canDecode(audio.format(), audio.data())) {
            logOnce("decoder-" + audio.format(), "No compatible decoder for CC:HQ format " + audio.format() + "; that format will use normal CC:HQ playback.");
            handoffToNative(audio.source());
            return false;
        }

        int epoch = SESSION_EPOCH.get();
        DecodeKey key = makeDecodeKey(audio);
        final DecodeEntry entry;
        try {
            entry = DECODES.compute(key, (k, old) -> {
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
        } catch (RejectedExecutionException rejected) {
            logOnce("decode-overload", "Decoder queue is full; excess audio will use normal CC:HQ playback instead of accumulating decode work.");
            handoffToNative(audio.source());
            return false;
        }

        int generation = GENERATIONS.computeIfAbsent(audio.source(), ignored -> new AtomicInteger()).incrementAndGet();
        entry.future.whenComplete((decoded, error) -> {
            if (error != null) {
                DECODES.remove(key, entry);
                logOnce("decode-runtime-" + audio.format(), "Decoder accepted " + audio.format() + " but failed while decoding: " + rootMessage(error));
                return;
            }
            if (epoch != SESSION_EPOCH.get()) return;
            AtomicInteger currentGeneration = GENERATIONS.get(audio.source());
            if (currentGeneration == null || currentGeneration.get() != generation) return;
            READY.add(new StartRequest(playbackMetadata(audio), key, decoded, generation, epoch));
        });
        return true;
    }

    public static void tryHandleStopPayload(Object payload) {
        if (!HQPayloadView.isStopPayload(payload)) return;
        try {
            UUID source = HQPayloadView.stopSource(payload);
            AtomicInteger generation = GENERATIONS.get(source);
            if (generation != null) {
                generation.incrementAndGet();
                onSoundThread(() -> stopSource(source));
            }
        } catch (Throwable t) {
            logOnce("stop-shape", "Could not read CC:HQ stop packet: " + t);
        }
    }

    public static void tickClient() {
        clientTicks++;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            if (trackedLevel != null) {
                trackedLevel = null;
                invalidateSession();
                onSoundThread(CompatAudioManager::stopAllSources);
            }
            return;
        }
        if (trackedLevel != level) {
            if (trackedLevel != null) {
                invalidateSession();
                onSoundThread(CompatAudioManager::stopAllSources);
            }
            trackedLevel = level;
        }

        long gameTime = level.getGameTime();
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

        if ((clientTicks % 20L) == 0L) {
            long cutoff = clientTicks - 600L;
            DECODES.entrySet().removeIf(e -> e.getValue().lastUsedTick < cutoff && e.getValue().future.isDone());
            List<Map.Entry<DecodeKey, DecodeEntry>> completed = DECODES.entrySet().stream()
                    .filter(e -> e.getValue().future.isDone())
                    .sorted(Comparator.comparingLong(e -> e.getValue().lastUsedTick))
                    .toList();
            long completedBytes = 0L;
            for (Map.Entry<DecodeKey, DecodeEntry> item : completed) {
                DecodedAudio decoded = completedDecoded(item.getValue());
                if (decoded != null) completedBytes += decoded.monoPcm16Le().length;
            }
            int remaining = completed.size();
            for (Map.Entry<DecodeKey, DecodeEntry> oldest : completed) {
                if (remaining <= MAX_DECODE_CACHE_ENTRIES && completedBytes <= MAX_DECODE_CACHE_BYTES) break;
                DecodedAudio decoded = completedDecoded(oldest.getValue());
                if (DECODES.remove(oldest.getKey(), oldest.getValue())) {
                    remaining--;
                    if (decoded != null) completedBytes -= decoded.monoPcm16Le().length;
                }
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
        boolean installed = false;
        try {
            if (request.epoch != SESSION_EPOCH.get()) return;
            AtomicInteger generation = GENERATIONS.get(request.audio.source());
            if (generation == null || generation.get() != request.generation) return;

            stopSource(request.audio.source());
            clearAlErrors();

            ref = BUFFERS.get(request.key);
            if (ref == null) {
                int bufferId = 0;
                ByteBuffer bytes = null;
                try {
                    bufferId = AL10.alGenBuffers();
                    checkAl("allocate audio buffer");
                    if (bufferId == 0) throw new IllegalStateException("alGenBuffers returned 0");
                    bytes = MemoryUtil.memAlloc(request.decoded.monoPcm16Le().length);
                    bytes.put(request.decoded.monoPcm16Le()).flip();
                    AL10.alBufferData(bufferId, AL10.AL_FORMAT_MONO16, bytes, request.decoded.sampleRate());
                    checkAl("upload audio buffer");
                    ref = new BufferRef(bufferId);
                    BUFFERS.put(request.key, ref);
                } catch (Throwable t) {
                    if (bufferId != 0) {
                        try { AL10.alDeleteBuffers(bufferId); } catch (Throwable ignored) {}
                    }
                    throw t;
                } finally {
                    if (bytes != null) MemoryUtil.memFree(bytes);
                }
            }
            ref.refs++;

            sourceId = AL10.alGenSources();
            checkAl("allocate source");
            if (sourceId == 0) throw new IllegalStateException("alGenSources returned 0");
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
                if (sourceId != 0) {
                    try { EnvironmentSmoother.unregister(sourceId); } catch (Throwable ignored) {}
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
        try { EnvironmentSmoother.unregister(active.sourceId); }
        catch (Throwable t) { logOnce("source-unregister", "Error while unregistering compatibility source state: " + t); }
        try { AL10.alSourceStop(active.sourceId); } catch (Throwable ignored) {}
        try { AL10.alSourcei(active.sourceId, AL10.AL_BUFFER, 0); } catch (Throwable ignored) {}
        try { AL10.alDeleteSources(active.sourceId); } catch (Throwable ignored) {}
        BufferRef ref = BUFFERS.get(active.key);
        if (ref != null) releaseBuffer(active.key, ref);
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
        DECODER.getQueue().clear();
        DECODES.clear();
        GENERATIONS.clear();
        SyncStartCoordinator.clear();
        SoundPhysicsBridge.clearSourceIds();
        lastListenerX = lastListenerY = lastListenerZ = Double.NaN;
    }

    public static void resetForSoundEngine() {
        invalidateSession();
        onSoundThreadBlocking(CompatAudioManager::stopAllSources);
    }

    public static void pauseCompatSources() {
        onSoundThread(() -> {
            for (ActiveSource active : ACTIVE.values()) {
                if (SyncStartCoordinator.sourceState(active.sourceId, AL10.AL_SOURCE_STATE) == AL10.AL_PLAYING) {
                    AL10.alSourcePause(active.sourceId);
                }
            }
        });
    }

    public static void resumeCompatSources() {
        onSoundThread(() -> {
            for (ActiveSource active : ACTIVE.values()) {
                if (SyncStartCoordinator.sourceState(active.sourceId, AL10.AL_SOURCE_STATE) == AL10.AL_PAUSED) {
                    AL10.alSourcePlay(active.sourceId);
                }
            }
        });
    }

    public static void stopAllCompatSources() {
        invalidateSession();
        onSoundThreadBlocking(CompatAudioManager::stopAllSources);
    }

    private static void clearAlErrors() {
        while (AL10.alGetError() != AL10.AL_NO_ERROR) { }
    }

    private static SoundEngineExecutor soundExecutor() {
        Minecraft mc = Minecraft.getInstance();
        SoundEngine engine = ((SoundManagerAccessor) mc.getSoundManager()).cchqphysics$getSoundEngine();
        return ((SoundEngineAccessor) engine).cchqphysics$getExecutor();
    }

    private static void onSoundThread(Runnable task) {
        try {
            soundExecutor().execute(task);
        } catch (Throwable t) {
            logOnce("sound-thread", "Could not schedule work on Minecraft's sound thread: " + t);
        }
    }

    private static void onSoundThreadBlocking(Runnable task) {
        try {
            soundExecutor().executeBlocking(task);
        } catch (Throwable t) {
            logOnce("sound-thread-blocking", "Could not complete blocking work on Minecraft's sound thread: " + t);
        }
    }

    private static void handoffToNative(UUID source) {
        AtomicInteger generation = GENERATIONS.get(source);
        if (generation == null) return;
        generation.incrementAndGet();
        onSoundThread(() -> stopSource(source));
    }

    private static HQPayloadView.Audio playbackMetadata(HQPayloadView.Audio audio) {
        return new HQPayloadView.Audio(audio.source(), audio.format(), audio.volume(),
                audio.x(), audio.y(), audio.z(), EMPTY_AUDIO_DATA, audio.startTick(),
                audio.syncGroupId(), audio.syncGroupSize());
    }

    private static DecodedAudio completedDecoded(DecodeEntry entry) {
        if (!entry.future.isDone() || entry.future.isCompletedExceptionally() || entry.future.isCancelled()) return null;
        return entry.future.getNow(null);
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
