# trigger: apply world identity lifecycle fix
from pathlib import Path

path = Path('src/main/java/dev/cchqphysics/compat/audio/CompatAudioManager.java')
text = path.read_text(encoding='utf-8')

old_field = '    private static boolean hadLevel;\n'
new_field = '    private static Object trackedLevel;\n'
assert text.count(old_field) == 1, 'expected exactly one hadLevel field'
text = text.replace(old_field, new_field)

old_block = '''        Minecraft mc = Minecraft.getInstance();
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
'''
new_block = '''        Minecraft mc = Minecraft.getInstance();
        Object level = mc.level;
        if (level == null) {
            if (trackedLevel != null) {
                trackedLevel = null;
                invalidateSession();
                onSoundThread(CompatAudioManager::stopAllSources);
            }
            return;
        }
        if (trackedLevel == null) {
            trackedLevel = level;
        } else if (trackedLevel != level) {
            trackedLevel = level;
            invalidateSession();
            onSoundThread(CompatAudioManager::stopAllSources);
        }

        long gameTime = mc.level.getGameTime();
'''
assert text.count(old_block) == 1, 'expected exact tickClient level lifecycle block'
text = text.replace(old_block, new_block)

assert 'hadLevel' not in text, 'old level flag remains'
assert text.count('trackedLevel') == 6, 'unexpected trackedLevel occurrence count'
path.write_text(text, encoding='utf-8')
