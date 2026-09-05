from pathlib import Path

p = Path('src/main/java/dev/cchqphysics/compat/audio/CompatAudioManager.java')
s = p.read_text()

def once(old: str, new: str, label: str) -> None:
    global s
    count = s.count(old)
    if count != 1:
        raise SystemExit(f'{label}: expected exactly one match, found {count}')
    s = s.replace(old, new, 1)

once('''import org.lwjgl.openal.AL10;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
''', '''import org.lwjgl.openal.AL10;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
''', 'memory imports')

once('''            UUID source = HQPayloadView.stopSource(payload);
            GENERATIONS.computeIfAbsent(source, ignored -> new AtomicInteger()).incrementAndGet();
            onSoundThread(() -> stopSource(source));
''', '''            UUID source = HQPayloadView.stopSource(payload);
            AtomicInteger generation = GENERATIONS.get(source);
            if (generation != null) {
                generation.incrementAndGet();
                onSoundThread(() -> stopSource(source));
            }
''', 'native-only stop generation')

once('''        if ((clientTicks % 200L) == 0L) {
''', '''        if ((clientTicks % 20L) == 0L) {
''', 'decode cache trim cadence')

once('''            ref = BUFFERS.get(request.key);
            if (ref == null) {
                int bufferId = AL10.alGenBuffers();
                ByteBuffer bytes = ByteBuffer.allocateDirect(request.decoded.monoPcm16Le().length).order(ByteOrder.nativeOrder());
                bytes.put(request.decoded.monoPcm16Le()).flip();
                AL10.alBufferData(bufferId, AL10.AL_FORMAT_MONO16, bytes, request.decoded.sampleRate());
                checkAl("upload audio buffer");
                ref = new BufferRef(bufferId);
                BUFFERS.put(request.key, ref);
            }
''', '''            ref = BUFFERS.get(request.key);
            if (ref == null) {
                int bufferId = 0;
                ByteBuffer bytes = null;
                try {
                    bufferId = AL10.alGenBuffers();
                    checkAl("allocate audio buffer");
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
''', 'deterministic upload buffer cleanup')

once('''    private static void destroyActive(ActiveSource active) {
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
''', '''    private static void destroyActive(ActiveSource active) {
        try { EnvironmentSmoother.unregister(active.sourceId); }
        catch (Throwable t) { logOnce("source-unregister", "Error while unregistering compatibility source state: " + t); }
        try { AL10.alSourceStop(active.sourceId); } catch (Throwable ignored) {}
        try { AL10.alSourcei(active.sourceId, AL10.AL_BUFFER, 0); } catch (Throwable ignored) {}
        try { AL10.alDeleteSources(active.sourceId); } catch (Throwable ignored) {}
        BufferRef ref = BUFFERS.get(active.key);
        if (ref != null) releaseBuffer(active.key, ref);
    }
''', 'destroy cleanup resilience')

once('''    private static void handoffToNative(UUID source) {
        GENERATIONS.computeIfAbsent(source, ignored -> new AtomicInteger()).incrementAndGet();
        onSoundThread(() -> stopSource(source));
    }
''', '''    private static void handoffToNative(UUID source) {
        AtomicInteger generation = GENERATIONS.get(source);
        if (generation == null) return;
        generation.incrementAndGet();
        onSoundThread(() -> stopSource(source));
    }
''', 'conditional native handoff')

p.write_text(s)
