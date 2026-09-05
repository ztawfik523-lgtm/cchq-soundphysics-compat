from pathlib import Path

path = Path('src/main/java/dev/cchqphysics/compat/audio/CompatAudioManager.java')
text = path.read_text()
old = '''        entry.future.whenComplete((decoded, error) -> {
            if (error != null) {
                logOnce("decode-runtime-" + audio.format(), "Decoder accepted " + audio.format() + " but failed while decoding: " + rootMessage(error));
                return;
            }
            READY.add(new StartRequest(audio, key, decoded, generation, epoch));
        });'''
new = '''        entry.future.whenComplete((decoded, error) -> {
            if (error != null) {
                logOnce("decode-runtime-" + audio.format(), "Decoder accepted " + audio.format() + " but failed while decoding: " + rootMessage(error));
                return;
            }
            if (epoch != SESSION_EPOCH.get()) return;
            AtomicInteger currentGeneration = GENERATIONS.get(audio.source());
            if (currentGeneration == null || currentGeneration.get() != generation) return;
            READY.add(new StartRequest(audio, key, decoded, generation, epoch));
        });'''
assert text.count(old) == 1, 'decode completion anchor mismatch'
text = text.replace(old, new)
path.write_text(text)
