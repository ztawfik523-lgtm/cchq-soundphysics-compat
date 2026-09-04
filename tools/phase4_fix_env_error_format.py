from pathlib import Path

path = Path('src/main/java/dev/cchqphysics/compat/audio/EnvironmentSmoother.java')
text = path.read_text()
replacements = {
    '"OpenAL error creating isolated EFX: 0x" + Integer.toHexString(error)': '"OpenAL error creating isolated EFX: " + error',
    '"OpenAL error applying isolated EFX: 0x" + Integer.toHexString(error)': '"OpenAL error applying isolated EFX: " + error',
}
for old, new in replacements.items():
    count = text.count(old)
    if count != 1:
        raise SystemExit(f'expected exactly one occurrence of {old!r}, found {count}')
    text = text.replace(old, new)
path.write_text(text)
