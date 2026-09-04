from pathlib import Path
p = Path('src/main/java/dev/cchqphysics/compat/audio/Beta10Optimizer.java')
text = p.read_text(encoding='utf-8')
needle = 'import com.sonicether.soundphysics.SoundPhysics;\n'
replacement = needle + 'import dev.cchqphysics.compat.config.ClientConfig;\n'
if 'import dev.cchqphysics.compat.config.ClientConfig;' not in text:
    if text.count(needle) != 1:
        raise SystemExit('unexpected import anchor')
    text = text.replace(needle, replacement, 1)
p.write_text(text, encoding='utf-8')
print('Beta10 ClientConfig import present')
