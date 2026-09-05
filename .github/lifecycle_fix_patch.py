from pathlib import Path

path = Path('src/main/java/dev/cchqphysics/compat/audio/CompatAudioManager.java')
text = path.read_text()

old = '''    private static void startSource(StartRequest request) {
        BufferRef ref = null;
        int sourceId = 0;
        boolean installed = false;
        try {'''
new = '''    private static void startSource(StartRequest request) {
        BufferRef ref = null;
        int sourceId = 0;
        boolean compatRegistered = false;
        boolean installed = false;
        try {'''
assert text.count(old) == 1, 'startSource declaration anchor mismatch'
text = text.replace(old, new)

old = '''            sourceId = AL10.alGenSources();
            EnvironmentSmoother.register(sourceId);
            AL10.alSourcei(sourceId, AL10.AL_BUFFER, ref.bufferId);'''
new = '''            sourceId = AL10.alGenSources();
            compatRegistered = true;
            EnvironmentSmoother.register(sourceId);
            AL10.alSourcei(sourceId, AL10.AL_BUFFER, ref.bufferId);'''
assert text.count(old) == 1, 'registration anchor mismatch'
text = text.replace(old, new)

old = '''        } finally {
            if (!installed) {
                if (sourceId != 0) {
                    try { AL10.alSourceStop(sourceId); } catch (Throwable ignored) {}
                    try { AL10.alSourcei(sourceId, AL10.AL_BUFFER, 0); } catch (Throwable ignored) {}
                    try { AL10.alDeleteSources(sourceId); } catch (Throwable ignored) {}
                }
                if (ref != null) releaseBuffer(request.key, ref);
            }
        }
    }'''
new = '''        } finally {
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
    }'''
assert text.count(old) == 1, 'failed-start cleanup anchor mismatch'
text = text.replace(old, new)

old = '''    public static void pauseCompatSources() {
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
    }'''
new = '''    public static void pauseCompatSources() {
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
    }'''
assert text.count(old) == 1, 'pause/resume anchor mismatch'
text = text.replace(old, new)

path.write_text(text)
