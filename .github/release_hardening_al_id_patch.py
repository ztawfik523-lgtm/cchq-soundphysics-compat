from pathlib import Path

p = Path('src/main/java/dev/cchqphysics/compat/audio/CompatAudioManager.java')
s = p.read_text()

def once(old: str, new: str, label: str) -> None:
    global s
    count = s.count(old)
    if count != 1:
        raise SystemExit(f'{label}: expected exactly one match, found {count}')
    s = s.replace(old, new, 1)

once('''                    bufferId = AL10.alGenBuffers();
                    checkAl("allocate audio buffer");
                    bytes = MemoryUtil.memAlloc(request.decoded.monoPcm16Le().length);
''', '''                    bufferId = AL10.alGenBuffers();
                    checkAl("allocate audio buffer");
                    if (bufferId == 0) throw new IllegalStateException("alGenBuffers returned 0");
                    bytes = MemoryUtil.memAlloc(request.decoded.monoPcm16Le().length);
''', 'buffer id guard')

once('''            sourceId = AL10.alGenSources();
            EnvironmentSmoother.register(sourceId);
''', '''            sourceId = AL10.alGenSources();
            checkAl("allocate source");
            if (sourceId == 0) throw new IllegalStateException("alGenSources returned 0");
            EnvironmentSmoother.register(sourceId);
''', 'source id guard')

p.write_text(s)
