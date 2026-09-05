from pathlib import Path

p = Path('src/main/java/dev/cchqphysics/compat/audio/CompatAudioManager.java')
s = p.read_text()

def once(old: str, new: str, label: str) -> None:
    global s
    count = s.count(old)
    if count != 1:
        raise SystemExit(f'{label}: expected exactly one match, found {count}')
    s = s.replace(old, new, 1)

once('''import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
''', '''import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
''', 'concurrency imports')

once('''    private static final ExecutorService DECODER = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "CC-HQ SoundPhysics decoder");
        t.setDaemon(true);
        return t;
    });
    private static final ConcurrentLinkedQueue<StartRequest> READY = new ConcurrentLinkedQueue<>();
    private static final ConcurrentHashMap<DecodeKey, DecodeEntry> DECODES = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, AtomicInteger> GENERATIONS = new ConcurrentHashMap<>();
    private static final Set<String> LOGGED = ConcurrentHashMap.newKeySet();
    private static final int MAX_DECODE_CACHE_ENTRIES = 4;
''', '''    private static final int MAX_PENDING_DECODE_TASKS = 8;
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
''', 'decoder executor')

once('''        if (audio.format().endsWith("_STREAM") || "PCM_S16LE".equals(audio.format())) return false;
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
''', '''        if (audio.format().endsWith("_STREAM") || "PCM_S16LE".equals(audio.format())) {
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
''', 'decode scheduling')

once('''            List<Map.Entry<DecodeKey, DecodeEntry>> completed = DECODES.entrySet().stream()
                    .filter(e -> e.getValue().future.isDone())
                    .sorted(Comparator.comparingLong(e -> e.getValue().lastUsedTick))
                    .toList();
            int excess = completed.size() - MAX_DECODE_CACHE_ENTRIES;
            for (int i = 0; i < excess; i++) {
                Map.Entry<DecodeKey, DecodeEntry> oldest = completed.get(i);
                DECODES.remove(oldest.getKey(), oldest.getValue());
            }
''', '''            List<Map.Entry<DecodeKey, DecodeEntry>> completed = DECODES.entrySet().stream()
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
''', 'decode cache trim')

once('''        SESSION_EPOCH.incrementAndGet();
        READY.clear();
        DECODES.clear();
''', '''        SESSION_EPOCH.incrementAndGet();
        READY.clear();
        DECODER.getQueue().clear();
        DECODES.clear();
''', 'session decode invalidation')

once('''    private static DecodeKey makeDecodeKey(HQPayloadView.Audio audio) {
''', '''    private static void handoffToNative(UUID source) {
        GENERATIONS.computeIfAbsent(source, ignored -> new AtomicInteger()).incrementAndGet();
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
''', 'hardening helpers')

p.write_text(s)
